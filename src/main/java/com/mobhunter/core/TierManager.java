package com.mobhunter.core;

import com.mobhunter.MobHunter;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Turns an ordinary creature into an Alpha, Monstrous or Deity version of itself. */
public final class TierManager {
    private static final Identifier MOD_ID = MobHunter.id("tier");

    public static Tier tier(LivingEntity e) {
        Tier t = e.getExistingDataOrNull(ModAttachments.TIER);
        return t == null ? Tier.NONE : t;
    }

    /** Height (in blocks) each tier aims for, so tiny and huge creatures end up comparable. */
    public static float targetScale(Mob mob, Tier tier) {
        float h = Math.max(0.25f, mob.getType().getDimensions().height());
        float w = Math.max(0.25f, mob.getType().getDimensions().width());
        float size = Math.max(h, w * 0.8f);
        return switch (tier) {
            case ALPHA -> 1.3f;
            case MONSTROUS -> clamp(6.5f / size, 2.2f, 12f);
            case DEITY -> clamp(15f / size, 4f, 16f);
            default -> 1f;
        };
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void applyTier(Mob mob, Tier tier, boolean fullHeal) {
        CreatureProfile profile = CreatureProfiles.get(mob);
        if (profile == null || tier == Tier.NONE) return;
        mob.setData(ModAttachments.TIER, tier);
        mob.syncData(ModAttachments.TIER);

        boolean bespoke = mob instanceof com.mobhunter.deity.DeityBoss;
        float scale = mob instanceof EnderDragon || bespoke ? 1f : targetScale(mob, tier);
        double baseHealth = mob.getAttributeBaseValue(Attributes.MAX_HEALTH);
        double health = switch (tier) {
            case ALPHA -> baseHealth * tier.healthMult + 6;
            case MONSTROUS -> Math.min(480, 260 + baseHealth * 8);
            case DEITY -> Math.min(2000, 1100 + baseHealth * 18);
            default -> baseHealth;
        };
        set(mob, Attributes.MAX_HEALTH, health - baseHealth);
        set(mob, Attributes.SCALE, scale - 1);
        set(mob, Attributes.ARMOR, tier == Tier.DEITY ? 16 : tier == Tier.MONSTROUS ? 8 : 2);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, tier == Tier.DEITY ? 1.0 : tier == Tier.MONSTROUS ? 0.85 : 0.3);
        set(mob, Attributes.STEP_HEIGHT, Math.max(0, scale * 0.45 - 0.4));
        set(mob, Attributes.FOLLOW_RANGE, tier == Tier.ALPHA ? 8 : 48);
        set(mob, Attributes.SAFE_FALL_DISTANCE, scale * 3);
        if (bespoke) {
            mob.setPersistenceRequired();
        } else if (tier.isBoss()) {
            // Huge creatures keep their stride: scale reduces relative speed, so compensate a little.
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(MOD_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            mob.setPersistenceRequired();
        } else {
            set(mob, Attributes.MOVEMENT_SPEED, 0);
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(MOD_ID, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (fullHeal) mob.setHealth(mob.getMaxHealth());
        mob.goalSelector.removeAllGoals(g -> g instanceof PanicGoal);
        // Hunted creepers detonate through their telegraphed moves, never by blowing themselves up.
        mob.goalSelector.removeAllGoals(g -> g instanceof net.minecraft.world.entity.ai.goal.SwellGoal);
        mob.setCustomName(displayName(profile, tier));
        mob.setCustomNameVisible(tier != Tier.ALPHA);
    }

    private static void set(Mob mob, Holder<Attribute> attr, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(MOD_ID);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(MOD_ID, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    public static MutableComponent creatureName(CreatureProfile profile) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(profile.id())
                .map(t -> t.getDescription().copy()).orElse(Component.literal(profile.id().getPath()));
    }

    public static MutableComponent displayName(CreatureProfile profile, Tier tier) {
        if (tier == Tier.DEITY) return Component.literal(profile.epithet()).withStyle(tier.color, net.minecraft.ChatFormatting.BOLD);
        if (tier == Tier.MONSTROUS && profile.entry() != null) {
            // "Monstrous Bull Cow", "Monstrous Crystal-Winged Allay", but "Monstrous Ravager Titan".
            String title = profile.entry().monstrousTitle();
            String creature = creatureName(profile).getString();
            String last = creature.substring(creature.lastIndexOf(' ') + 1).toLowerCase(java.util.Locale.ROOT);
            Component name = title.toLowerCase(java.util.Locale.ROOT).contains(last) ? Component.literal(title)
                    : Component.literal(title + " ").append(creatureName(profile));
            return Component.translatable("mobhunter.tier.monstrous_titled", name).withStyle(tier.color);
        }
        return Component.translatable(tier.translationKey()).append(" ").append(creatureName(profile)).withStyle(tier.color);
    }

    private TierManager() {}
}
