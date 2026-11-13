package com.mobhunter.hunt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A clue left behind by a hunted creature: tracks, shed material, remains or something worse. */
public class ClueBlock extends Block {
    public enum Kind { TRACKS, SHED, REMAINS, OMINOUS, GORE }

    private static final VoxelShape FLAT = Block.column(16.0, 0.0, 1.0);
    private static final VoxelShape PILE = Block.column(14.0, 0.0, 6.0);
    private static final VoxelShape MOUND = Block.column(16.0, 0.0, 10.0);
    public final Kind kind;

    public ClueBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case TRACKS, GORE -> FLAT;
            case SHED -> PILE;
            case REMAINS, OMINOUS -> MOUND;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.TRACKS || kind == Kind.GORE ? Shapes.empty() : getShape(state, level, pos, context);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (kind == Kind.GORE) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            return HuntManager.investigate(sp, pos) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind == Kind.GORE || random.nextInt(kind == Kind.OMINOUS ? 2 : 5) != 0) return;
        level.addParticle(kind == Kind.OMINOUS ? ParticleTypes.SMOKE : ParticleTypes.MYCELIUM,
                pos.getX() + random.nextDouble(), pos.getY() + 0.3 + random.nextDouble() * 0.4, pos.getZ() + random.nextDouble(), 0, 0.01, 0);
    }
}
