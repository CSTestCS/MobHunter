package com.mobhunter.hunt;

import com.mobhunter.core.Tier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** The quarry written on a hunt contract. */
public record ContractData(Identifier target, Tier tier) {
    public static final Codec<ContractData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("target").forGetter(ContractData::target),
            Tier.CODEC.fieldOf("tier").forGetter(ContractData::tier)
    ).apply(i, ContractData::new));
    public static final StreamCodec<ByteBuf, ContractData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ContractData::target,
            Tier.STREAM_CODEC, ContractData::tier,
            ContractData::new);
}
