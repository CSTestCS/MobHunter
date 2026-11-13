package com.mobhunter.gear;

import com.mobhunter.MobHunter;
import com.mobhunter.ability.Families;
import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import com.mobhunter.core.TierManager;
import com.mobhunter.registry.ModComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/** Elemental weapon procs, armor resistances, Hunter's Edge and full-set skills. */
@EventBusSubscriber(modid = MobHunter.MODID)
public final class GearEffects {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String ARROW_TAG = "mobhunter_arrow_";

    public static @Nullable MonsterSource source(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModComponents.MONSTER_SOURCE.get());
    }

    /** Number of worn hunter armor pieces (and how many match an element). */
    private static int[] armorCount(LivingEntity e, @Nullable Element element) {
        int any = 0, match = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = e.getItemBySlot(slot);
            if (!(s.getItem() instanceof HunterArmorItem)) continue;
            MonsterSource src = source(s);
            if (src == null) continue;
            any++;
            if (element != null && src.element() == element) match++;
        }
        return new int[]{any, match};
    }

    /** Full set: all four pieces from the same creature. Returns the source, or null. */
    public static @Nullable MonsterSource fullSet(LivingEntity e) {
        MonsterSource first = null;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack s = e.getItemBySlot(slot);
            if (!(s.getItem() instanceof HunterArmorItem)) return null;
            MonsterSource src = source(s);
            if (src == null) return null;
            if (first == null) first = src;
            else if (!first.creature().equals(src.creature())) return null;
        }
        return first;
    }

    /** Damage multiplier against a monster ability of the given element. */
    public static float resistance(LivingEntity victim, Element element) {
        int[] c = armorCount(victim, element);
        float mult = 1f - 0.04f * c[0];
        if (element != Element.RAW) mult -= 0.15f * c[1];
        return Math.max(0.2f, mult);
    }

    public static void onWeaponHit(ServerLevel level, LivingEntity attacker, LivingEntity target, @Nullable MonsterSource src, ItemStack stack) {
        if (src == null) return;
        int potency = Math.max(1, src.tier().rank);
        Element e = src.element();
        switch (e) {
            case THUNDER -> {
                if (attacker.getRandom().nextFloat() < 0.12f + potency * 0.04f) {
                    Families.strikeVisual(level, target.position());
                    target.hurtServer(level, level.damageSources().lightningBolt(), 3f * potency);
                }
            }
            case BLAST -> {
                if (attacker.getRandom().nextFloat() < 0.2f) {
                    level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
                    target.hurtServer(level, level.damageSources().explosion(attacker, attacker), 4f * potency);
                }
            }
            case WIND -> {
                Vec3 d = target.position().subtract(attacker.position()).normalize();
                target.push(d.x * 0.5 * potency, 0.2, d.z * 0.5 * potency);
            }
            case EARTH -> target.hurtServer(level, level.damageSources().magic(), 1.5f * potency);
            case RAW -> { }
            default -> {
                if (attacker.getRandom().nextFloat() < 0.45f) e.afflict(target, potency - 1);
            }
        }
        level.sendParticles(e.particle(), target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow) || event.loadedFromDisk()) return;
        if (!(arrow.getOwner() instanceof Player p)) return;
        ItemStack bow = p.getMainHandItem().getItem() instanceof HunterBowItem ? p.getMainHandItem()
                : p.getOffhandItem().getItem() instanceof HunterBowItem ? p.getOffhandItem() : ItemStack.EMPTY;
        MonsterSource src = source(bow);
        if (src == null) return;
        arrow.setBaseDamage(2.0 * GearStats.weaponMult(src.tier()) + src.tier().rank * 0.5);
        arrow.addTag(ARROW_TAG + src.creature().getNamespace() + "__" + src.creature().getPath() + "__" + src.tier().getSerializedName());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        Entity direct = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        // Hunter's Edge: hunter gear bites deeper into tiered creatures.
        boolean tiered = TierManager.tier(victim) != Tier.NONE;
        if (attacker instanceof LivingEntity la && direct == attacker && la.getMainHandItem().getItem() instanceof HunterWeaponItem && tiered) {
            event.setAmount(event.getAmount() * 1.35f);
        }
        if (direct instanceof AbstractArrow arrow) {
            for (String tag : arrow.entityTags()) {
                if (!tag.startsWith(ARROW_TAG)) continue;
                String[] parts = tag.substring(ARROW_TAG.length()).split("__");
                if (parts.length != 3) continue;
                MonsterSource src = new MonsterSource(net.minecraft.resources.Identifier.fromNamespaceAndPath(parts[0], parts[1]), Tier.byName(parts[2]));
                if (tiered) event.setAmount(event.getAmount() * 1.35f);
                if (attacker instanceof LivingEntity la) onWeaponHit(level, la, victim, src, ItemStack.EMPTY);
            }
        }
        // Armor resistances against vanilla elemental damage.
        for (Element e : Element.values()) {
            if (e == Element.RAW || !e.matches(event.getSource())) continue;
            int[] c = armorCount(victim, e);
            if (c[1] > 0) event.setAmount(event.getAmount() * Math.max(0.2f, 1f - 0.18f * c[1]));
            MonsterSource set = fullSet(victim);
            if (set != null && set.element() == e && (e == Element.BLAST || e == Element.WIND || e == Element.THUNDER)) event.setAmount(event.getAmount() * 0.5f);
        }
        // Void set: chance to blink away from a blow.
        MonsterSource set = fullSet(victim);
        if (set != null && set.element() == Element.VOID && attacker != null && victim.getRandom().nextFloat() < 0.15f) {
            for (int i = 0; i < 8; i++) {
                double x = victim.getX() + (victim.getRandom().nextDouble() - 0.5) * 12;
                double z = victim.getZ() + (victim.getRandom().nextDouble() - 0.5) * 12;
                if (victim.randomTeleport(x, victim.getY() + 2, z, true, s -> false)) break;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide() || p.tickCount % 20 != 0) return;
        MonsterSource set = fullSet(p);
        if (set == null) return;
        int amp = set.tier() == Tier.DEITY ? 1 : 0;
        switch (set.element()) {
            case FIRE -> give(p, MobEffects.FIRE_RESISTANCE, 0);
            case ICE -> {
                p.setTicksFrozen(0);
                p.removeEffect(MobEffects.SLOWNESS);
                give(p, MobEffects.RESISTANCE, 0);
            }
            case THUNDER -> give(p, MobEffects.SPEED, amp);
            case WATER -> {
                give(p, MobEffects.WATER_BREATHING, 0);
                if (p.isInWater()) give(p, MobEffects.DOLPHINS_GRACE, 0);
            }
            case POISON -> {
                p.removeEffect(MobEffects.POISON);
                p.removeEffect(MobEffects.WITHER);
                give(p, MobEffects.REGENERATION, 0);
            }
            case VOID, SONIC -> {
                give(p, MobEffects.NIGHT_VISION, 0);
                p.removeEffect(MobEffects.DARKNESS);
                p.removeEffect(MobEffects.BLINDNESS);
            }
            case WIND -> {
                give(p, MobEffects.JUMP_BOOST, amp);
                if (p.fallDistance > 4) give(p, MobEffects.SLOW_FALLING, 0);
            }
            case EARTH -> give(p, MobEffects.RESISTANCE, amp);
            case BLAST -> give(p, MobEffects.HASTE, amp);
            case RAW -> give(p, MobEffects.STRENGTH, amp);
        }
    }

    private static void give(Player p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp) {
        p.addEffect(new MobEffectInstance(effect, effect.value() == MobEffects.NIGHT_VISION.value() ? 260 : 60, amp, true, false, true));
    }

    private GearEffects() {}
}
