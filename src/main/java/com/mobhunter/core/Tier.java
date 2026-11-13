package com.mobhunter.core;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;

/** The three hunted tiers (plus NONE for ordinary creatures). */
public enum Tier implements StringRepresentable {
    NONE("none", ChatFormatting.WHITE, 1f, 1f, 1f, 0),
    ALPHA("alpha", ChatFormatting.GOLD, 2.2f, 1.3f, 1.0f, 1),
    MONSTROUS("monstrous", ChatFormatting.RED, 1f, 2.2f, 1.8f, 2),
    DEITY("deity", ChatFormatting.DARK_PURPLE, 1f, 3.6f, 2.8f, 3);

    public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);
    public static final StreamCodec<ByteBuf, Tier> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Tier::ordinal);

    private final String name;
    public final ChatFormatting color;
    /** Health multiplier used for Alphas (Monstrous/Deity use absolute health pools). */
    public final float healthMult;
    /** Multiplier applied to all ability damage. */
    public final float damageMult;
    /** Multiplier applied to gear crafted from this tier. */
    public final float gearMult;
    public final int rank;

    Tier(String name, ChatFormatting color, float healthMult, float damageMult, float gearMult, int rank) {
        this.name = name;
        this.color = color;
        this.healthMult = healthMult;
        this.damageMult = damageMult;
        this.gearMult = gearMult;
        this.rank = rank;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String translationKey() {
        return "mobhunter.tier." + name;
    }

    public boolean isBoss() {
        return this == MONSTROUS || this == DEITY;
    }

    public BossEvent.BossBarColor barColor() {
        return this == DEITY ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED;
    }

    /** Base cooldown between two abilities. */
    public int globalCooldown() {
        return switch (this) {
            case ALPHA -> 70;
            case MONSTROUS -> 34;
            case DEITY -> 22;
            default -> 100;
        };
    }

    public static Tier byName(String s) {
        for (Tier t : values()) if (t.name.equalsIgnoreCase(s)) return t;
        return NONE;
    }
}
