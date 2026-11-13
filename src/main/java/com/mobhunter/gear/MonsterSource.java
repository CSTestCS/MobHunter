package com.mobhunter.gear;

import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Which creature (and which tier of it) a material or piece of gear came from. */
public record MonsterSource(Identifier creature, Tier tier) {
    public static final Codec<MonsterSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("creature").forGetter(MonsterSource::creature),
            Tier.CODEC.fieldOf("tier").forGetter(MonsterSource::tier)
    ).apply(i, MonsterSource::new));
    public static final StreamCodec<ByteBuf, MonsterSource> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, MonsterSource::creature,
            Tier.STREAM_CODEC, MonsterSource::tier,
            MonsterSource::new);

    public @Nullable CreatureProfile profile() {
        return CreatureProfiles.get(creature);
    }

    public Element element() {
        CreatureProfile p = profile();
        return p == null ? Element.RAW : p.element();
    }

    public int color() {
        CreatureProfile p = profile();
        return p == null ? 0x8F8F8F : p.color();
    }
}
