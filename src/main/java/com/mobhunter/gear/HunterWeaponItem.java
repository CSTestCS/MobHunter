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
 * Great Sword / Long Sword / Hammer / Dual Blades forged from monster materials.
 * Each weapon class has a special move on use:
 * <ul>
 *     <li>Great Sword: hold to charge, release for a crushing overhead slash.</li>
 *     <li>Hammer: hold to charge, release for a ground pound that launches foes.</li>
 *     <li>Long Sword: Spirit Slash - a dashing cut through everything in a line.</li>
 *     <li>Dual Blades: Demon Flurry - a whirlwind of cuts around you.</li>
 * </ul>
 */
public class HunterWeaponItem extends Item {
    public final GearKind kind;

    public HunterWeaponItem(GearKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    private boolean charges() {
        return kind == GearKind.GREAT_SWORD || kind == GearKind.HAMMER;
    }

    @Override
    public Component getName(ItemStack stack) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        return src == null ? super.getName(stack) : GearStats.gearName(kind, src);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return charges() ? ItemUseAnimation.TRIDENT : ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return charges() ? 72000 : 0;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (charges()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (level instanceof ServerLevel server) {
            if (kind == GearKind.LONG_SWORD) spiritSlash(server, player, stack);
            else if (kind == GearKind.DUAL_BLADES) demonFlurry(server, player, stack);
        }
        player.getCooldowns().addCooldown(stack, kind == GearKind.LONG_SWORD ? 40 : 55);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        int used = getUseDuration(stack, user) - ticksRemaining;
        if (level instanceof ServerLevel server && used % 4 == 0) {
            float charge = Math.min(1f, used / 30f);
            server.sendParticles(charge >= 1f ? ParticleTypes.FLAME : ParticleTypes.CRIT, user.getX(), user.getY() + 1.2, user.getZ(), 3, 0.4, 0.4, 0.4, 0.02);
            if (used == 12 || used == 24 || used == 30) {
                server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.6f + used / 30f);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTime) {
        if (!(user instanceof Player player) || !(level instanceof ServerLevel server)) return false;
        float charge = Math.min(1f, (getUseDuration(stack, user) - remainingTime) / 30f);
        if (charge < 0.2f) return false;
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (base * (1 + 1.6f * charge));
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (kind == GearKind.GREAT_SWORD) {
            Vec3 at = player.position().add(flatLook(player).scale(2.6));
            hitArea(server, player, stack, src, at, 2.6, damage, 0.6, 0.2);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1, at.z, 3, 0.8, 0.3, 0.8, 0);
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 1, 0, 0, 0, 0);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.6f);
        } else {
            hitArea(server, player, stack, src, player.position(), 3.6, damage * 0.85f, 0.4, 0.8 * charge);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2f, 0.8f);
        }
        stack.hurtAndBreak(2, player, player.getUsedItemHand());
        player.getCooldowns().addCooldown(stack, 20);
        return true;
    }

    private static Vec3 flatLook(Player p) {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private void spiritSlash(ServerLevel level, Player player, ItemStack stack) {
        Vec3 dir = flatLook(player);
        Vec3 from = player.position();
        player.setDeltaMovement(dir.x * 1.6, 0.1, dir.z * 1.6);
        player.needsSync = true;
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4f;
        for (int i = 1; i <= 5; i++) {
            Vec3 p = from.add(dir.scale(i * 1.1));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y + 1, p.z, 1, 0, 0, 0, 0);
            hitArea(level, player, stack, src, p, 1.4, dmg, 0.2, 0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.4f);
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    private void demonFlurry(ServerLevel level, Player player, ItemStack stack) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.9f;
        for (int hit = 0; hit < 4; hit++) {
            hitArea(level, player, stack, src, player.position(), 3.0, dmg, 0.1, 0);
        }
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(a) * 2, player.getY() + 1, player.getZ() + Math.sin(a) * 2, 1, 0, 0, 0, 0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.8f);
        stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
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
        if (attacker.level() instanceof ServerLevel level) GearEffects.onWeaponHit(level, attacker, target, stack.get(ModComponents.MONSTER_SOURCE.get()), stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (src != null) {
            tooltip.accept(Component.translatable("mobhunter.tooltip.element", Component.translatable(src.element().translationKey()).withStyle(src.element().chat)).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("mobhunter.tooltip.weapon_element." + src.element().getSerializedName()).withStyle(ChatFormatting.DARK_GREEN));
        } else {
            tooltip.accept(Component.translatable("item.mobhunter.material.blank").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("mobhunter.tooltip.special." + kind.name).withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("mobhunter.tooltip.hunters_edge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
