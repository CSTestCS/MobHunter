package com.mobhunter.core;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Elemental affinity of a creature. Drives ability visuals, the status inflicted by attacks,
 * weapon element procs and armor resistances.
 */
public enum Element implements StringRepresentable {
    RAW("raw", 0xC8C8C8, ChatFormatting.GRAY),
    FIRE("fire", 0xFF6A1A, ChatFormatting.GOLD),
    ICE("ice", 0x9BE7FF, ChatFormatting.AQUA),
    THUNDER("thunder", 0xFFF04A, ChatFormatting.YELLOW),
    WATER("water", 0x2E7BFF, ChatFormatting.BLUE),
    POISON("poison", 0x7DDA3A, ChatFormatting.GREEN),
    BLAST("blast", 0xFF3A2A, ChatFormatting.RED),
    VOID("void", 0x9B3DFF, ChatFormatting.DARK_PURPLE),
    WIND("wind", 0xE6F7F2, ChatFormatting.WHITE),
    EARTH("earth", 0x9A6B3A, ChatFormatting.DARK_GREEN),
    SONIC("sonic", 0x1FB8B0, ChatFormatting.DARK_AQUA);

    public static final Codec<Element> CODEC = StringRepresentable.fromEnum(Element::values);

    private final String name;
    public final int color;
    public final ChatFormatting chat;

    Element(String name, int color, ChatFormatting chat) {
        this.name = name;
        this.color = color;
        this.chat = chat;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "mobhunter.element." + name;
    }

    public DustParticleOptions dust(float size) {
        return new DustParticleOptions(color, size);
    }

    /** Ambient particle that represents this element. */
    public ParticleOptions particle() {
        return switch (this) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case WATER -> ParticleTypes.SPLASH;
            case POISON -> ParticleTypes.NOXIOUS_GAS;
            case BLAST -> ParticleTypes.SMOKE;
            case VOID -> ParticleTypes.REVERSE_PORTAL;
            case WIND -> ParticleTypes.SMALL_GUST;
            case EARTH -> ParticleTypes.MYCELIUM;
            case SONIC -> ParticleTypes.SCULK_CHARGE_POP;
            default -> ParticleTypes.CRIT;
        };
    }

    /** Status ailment applied by this element on a successful hit. */
    public void afflict(LivingEntity target, int potency) {
        int dur = 40 + potency * 20;
        switch (this) {
            case FIRE -> target.igniteForSeconds(2 + potency);
            case ICE -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur, Math.min(potency, 3)));
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 60));
            }
            case THUNDER -> target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, dur, Math.min(potency, 2)));
            case WATER -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 0));
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, dur, Math.min(potency, 2)));
            case VOID -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, dur / 2, Math.min(potency, 1)));
            case WIND -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10 + potency * 5, 0));
            case EARTH -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, dur / 2, 1));
            case SONIC -> target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dur, 0));
            case BLAST -> { /* blast is burst damage, handled by caller */ }
            default -> { }
        }
    }

    /** Does a vanilla damage source belong to this element (used for armor resistances)? */
    public boolean matches(DamageSource source) {
        return switch (this) {
            case FIRE -> source.is(DamageTypeTags.IS_FIRE);
            case ICE -> source.is(DamageTypeTags.IS_FREEZING);
            case THUNDER -> source.is(DamageTypeTags.IS_LIGHTNING);
            case WATER -> source.is(DamageTypeTags.IS_DROWNING);
            case POISON -> source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
            case BLAST -> source.is(DamageTypeTags.IS_EXPLOSION);
            case VOID -> source.is(DamageTypes.WITHER) || source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.FELL_OUT_OF_WORLD);
            case WIND -> source.is(DamageTypeTags.IS_FALL);
            case EARTH -> source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_STALACTITE) || source.is(DamageTypes.IN_WALL);
            case SONIC -> source.is(DamageTypes.SONIC_BOOM);
            default -> false;
        };
    }
}
