package com.mobhunter.hunt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Heart of a Deity's shrine (and, for the Giant Zombie Piglin, the surface of its sunken portal).
 * Disturbing it calls the Deity.
 */
public class ShrineAltarBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public final boolean portal;

    public ShrineAltarBlock(boolean portal, Properties properties) {
        super(properties);
        this.portal = portal;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LIT)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) return HuntManager.disturbShrine(sp, pos) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean lit = state.getValue(LIT);
        if (portal) {
            if (lit) {
                for (int i = 0; i < 3; i++)
                    level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0, 0.6, 0);
            } else if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
            }
        } else {
            level.addParticle(lit ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + 1.1, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
        }
    }
}
