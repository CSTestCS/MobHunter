package com.mobhunter.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A player's active hunt. Stage 1..3 are clues; the clue position is fixed in X/Z when generated
 * and the block "materialises" when the hunter comes close (so far chunks are never force loaded).
 */
public record HuntState(boolean active, Identifier target, Tier tier, int stage, BlockPos cluePos,
                        ResourceKey<Level> dimension, boolean materialized, Optional<BlockPos> shrinePos) {

    public static final HuntState NONE = new HuntState(false, Identifier.withDefaultNamespace("pig"), Tier.NONE, 0,
            BlockPos.ZERO, Level.OVERWORLD, false, Optional.empty());

    public static final MapCodec<HuntState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("active").forGetter(HuntState::active),
            Identifier.CODEC.fieldOf("target").forGetter(HuntState::target),
            Tier.CODEC.fieldOf("tier").forGetter(HuntState::tier),
            Codec.INT.fieldOf("stage").forGetter(HuntState::stage),
            BlockPos.CODEC.fieldOf("clue").forGetter(HuntState::cluePos),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(HuntState::dimension),
            Codec.BOOL.fieldOf("materialized").forGetter(HuntState::materialized),
            BlockPos.CODEC.optionalFieldOf("shrine").forGetter(HuntState::shrinePos)
    ).apply(i, HuntState::new));

    public HuntState withClue(int stage, BlockPos pos, boolean materialized) {
        return new HuntState(active, target, tier, stage, pos, dimension, materialized, shrinePos);
    }

    public HuntState materialize(BlockPos pos) {
        return new HuntState(active, target, tier, stage, pos, dimension, true, shrinePos);
    }

    public HuntState withShrine(BlockPos pos) {
        return new HuntState(active, target, tier, stage, cluePos, dimension, materialized, Optional.of(pos));
    }
}
