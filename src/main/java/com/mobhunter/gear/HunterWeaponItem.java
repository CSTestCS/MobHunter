package com.mobhunter.gear;

import com.mobhunter.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The bestiary's weapons, built from a creature's parts:
 * <ul>
 *     <li>Blade: hold use to gather the creature's element, release for a sweeping elemental slash.</li>
 *     <li>Spear: the vanilla spear lunge with reach, plus a secondary effect of the element.</li>
 * </ul>
 */
public class HunterWeaponItem extends Item {
    public final GearKind kind;

    public HunterWeaponItem(GearKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    private boolean charges() {
        return kind == GearKind.BLADE;
    }

    @Override
    public Component getName(ItemStack stack) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        return src == null ? super.getName(stack) : GearStats.gearName(kind, src);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return charges() ? ItemUseAnimation.TRIDENT : super.getUseAnimation(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return charges() ? 72000 : super.getUseDuration(stack, user);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!charges()) return super.use(level, player, hand);
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!charges()) {
            super.onUseTick(level, user, stack, ticksRemaining);
            return;
        }
        int used = getUseDuration(stack, user) - ticksRemaining;
        if (level instanceof ServerLevel server && used % 4 == 0) {
            MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
            float charge = Math.min(1f, used / 30f);
            server.sendParticles(src != null ? src.element().dust(1.2f) : ParticleTypes.CRIT, user.getX(), user.getY() + 1.2, user.getZ(), 3 + (int) (charge * 4), 0.4, 0.4, 0.4, 0.02);
            if (used == 12 || used == 24 || used == 30) {
                server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.6f + used / 30f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTime) {
        if (!charges()) return super.releaseUsing(stack, level, user, remainingTime);
        if (!(user instanceof Player player) || !(level instanceof ServerLevel server)) return false;
        float charge = Math.min(1f, (getUseDuration(stack, user) - remainingTime) / 30f);
        if (charge < 0.2f) return false;
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (base * (1 + 1.5f * charge));
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        Vec3 dir = flatLook(player);
        for (int i = 1; i <= 3; i++) {
            Vec3 at = player.position().add(dir.scale(i * 1.2));
            hitArea(server, player, stack, src, at, 1.8, damage, 0.5, 0.1);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
            if (src != null) server.sendParticles(src.element().particle(), at.x, at.y + 1, at.z, 8, 0.6, 0.4, 0.6, 0.05);
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
        stack.hurtAndBreak(2, player, player.getUsedItemHand());
        player.getCooldowns().addCooldown(stack, 24);
        return true;
    }

    private static Vec3 flatLook(Player p) {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static void hitArea(ServerLevel level, Player player, ItemStack stack, MonsterSource src, Vec3 at, double radius, float damage, double knock, double launch) {
        AABB box = new AABB(at, at).inflate(radius, 2.5, radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e))) {
            if (e.hurtServer(level, level.damageSources().playerAttack(player), damage)) {
                GearEffects.onWeaponHit(level, player, e, src, stack);
                Vec3 d = e.position().subtract(player.position()).normalize();
                e.push(d.x * knock, launch, d.z * knock);
                e.needsSync = true;
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level) {
            MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
            GearEffects.onWeaponHit(level, attacker, target, src, stack);
            // The spear's secondary effect: a burst of the element at the point of impact.
            if (kind == GearKind.SPEAR && src != null && attacker.getRandom().nextFloat() < 0.35f) {
                level.sendParticles(src.element().dust(2f), target.getX(), target.getY() + 1, target.getZ(), 16, 0.6, 0.6, 0.6, 0.02);
                src.element().afflict(target, src.tier().rank);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (src != null) {
            tooltip.accept(Component.translatable("mobhunter.tooltip.weapon_built", GearEffects.materialName(src), GearEffects.elementName(src)).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("mobhunter.tooltip.weapon_element." + src.element().getSerializedName()).withStyle(ChatFormatting.DARK_GREEN));
        } else {
            tooltip.accept(Component.translatable("item.mobhunter.material.blank").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("mobhunter.tooltip.special." + kind.name).withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("mobhunter.tooltip.hunters_edge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
