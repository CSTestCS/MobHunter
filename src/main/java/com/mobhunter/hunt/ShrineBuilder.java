package com.mobhunter.hunt;

import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds the unique shrine of a Deity at the end of its trail. Returns the altar position. */
public final class ShrineBuilder {

    public static BlockPos build(ServerLevel level, BlockPos ground, CreatureProfile profile) {
        String id = profile.id().getPath();
        if (id.equals("sheep")) return stormAltar(level, ground);
        if (id.equals("zombified_piglin")) return sunkenPortal(level, ground);
        return eldritchShrine(level, ground, profile);
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    /** Levels a disc: fills below with a base block and clears everything above. */
    private static void platform(ServerLevel level, BlockPos c, int radius, BlockState floor, BlockState edge, int clearHeight) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.5) continue;
                BlockPos col = c.offset(dx, 0, dz);
                for (int y = 0; y < clearHeight; y++) {
                    BlockPos p = col.above(y);
                    if (!level.getBlockState(p).isAir()) set(level, p, Blocks.AIR.defaultBlockState());
                }
                set(level, col.below(), d > radius - 1 ? edge : floor);
                for (int y = 2; y < 6; y++) {
                    BlockPos p = col.below(y);
                    if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) set(level, p, Blocks.COBBLESTONE.defaultBlockState());
                    else break;
                }
            }
        }
    }

    private static void pillar(ServerLevel level, BlockPos base, int height, BlockState body, BlockState top) {
        for (int y = 0; y < height; y++) set(level, base.above(y), body);
        set(level, base.above(height), top);
    }

    // -------------------------------------------------------------------------------- Ram God
    private static BlockPos stormAltar(ServerLevel level, BlockPos c) {
        RandomSource r = level.getRandom();
        platform(level, c, 9, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 16);
        for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
            if (r.nextInt(6) == 0 && dx * dx + dz * dz <= 64) set(level, c.offset(dx, -1, dz), Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            BlockPos base = c.offset((int) Math.round(Math.cos(a) * 7), 0, (int) Math.round(Math.sin(a) * 7));
            int h = 4 + r.nextInt(4);
            pillar(level, base, h, Blocks.STONE_BRICK_WALL.defaultBlockState(), Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState());
            set(level, base.below(), Blocks.COPPER_BLOCK.waxed().unaffected().defaultBlockState());
            set(level, base.offset(0, 0, 0).relative(net.minecraft.core.Direction.Plane.HORIZONTAL.getRandomDirection(r)), Blocks.WOOL.white().defaultBlockState());
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(level, c.offset(dx, 0, dz), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        set(level, c.offset(2, 0, 0), Blocks.CARPET.white().defaultBlockState());
        set(level, c.offset(-2, 0, 0), Blocks.CARPET.white().defaultBlockState());
        set(level, c.offset(0, 0, 2), Blocks.CARPET.white().defaultBlockState());
        set(level, c.offset(0, 0, -2), Blocks.CARPET.white().defaultBlockState());
        BlockPos altar = c.above();
        set(level, altar, ModBlocks.SHRINE_ALTAR.get().defaultBlockState());
        return altar;
    }

    // ----------------------------------------------------------------- Giant Zombie Piglin
    private static BlockPos sunkenPortal(ServerLevel level, BlockPos c) {
        RandomSource r = level.getRandom();
        int R = 14;
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > R) continue;
            BlockPos p = c.offset(dx, -1, dz);
            for (int y = 1; y < 14; y++) if (!level.getBlockState(p.above(y)).isAir()) set(level, p.above(y), Blocks.AIR.defaultBlockState());
            float roll = r.nextFloat();
            BlockState s = d < 4 ? Blocks.NETHERRACK.defaultBlockState()
                    : roll < 0.45f ? Blocks.NETHERRACK.defaultBlockState()
                    : roll < 0.55f ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : roll < 0.6f ? Blocks.GOLD_BLOCK.defaultBlockState()
                    : roll < 0.75f ? Blocks.BLACKSTONE.defaultBlockState() : level.getBlockState(p);
            set(level, p, s);
        }
        // A huge Nether portal lying flat in the ground: 13x13 frame around an 11x11 surface.
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            boolean frame = Math.abs(dx) == 6 || Math.abs(dz) == 6;
            BlockPos p = c.offset(dx, -1, dz);
            set(level, p, frame ? (r.nextInt(4) == 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState())
                    : ModBlocks.SUNKEN_PORTAL.get().defaultBlockState());
            if (!frame) for (int y = 2; y < 16; y++) set(level, p.below(y - 1), Blocks.AIR.defaultBlockState());
        }
        // Broken portal pillars and scattered gold.
        for (int i = 0; i < 4; i++) {
            BlockPos base = c.offset(i < 2 ? -8 : 8, 0, i % 2 == 0 ? -8 : 8);
            pillar(level, base, 2 + r.nextInt(3), Blocks.OBSIDIAN.defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        }
        for (int i = 0; i < 10; i++) {
            BlockPos p = c.offset(r.nextInt(23) - 11, 0, r.nextInt(23) - 11);
            if (Math.abs(p.getX() - c.getX()) > 6 || Math.abs(p.getZ() - c.getZ()) > 6) set(level, p, r.nextBoolean() ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.FIRE.defaultBlockState());
        }
        return c.below();
    }

    // ---------------------------------------------------------------------- generic Deity
    private static BlockPos eldritchShrine(ServerLevel level, BlockPos c, CreatureProfile profile) {
        RandomSource r = level.getRandom();
        BlockState theme = switch (profile.element()) {
            case FIRE -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case ICE -> Blocks.PACKED_ICE.defaultBlockState();
            case THUNDER -> Blocks.COPPER_BLOCK.waxed().unaffected().defaultBlockState();
            case WATER -> Blocks.DARK_PRISMARINE.defaultBlockState();
            case POISON -> Blocks.MOSS_BLOCK.defaultBlockState();
            case BLAST -> Blocks.RED_NETHER_BRICKS.defaultBlockState();
            case VOID -> Blocks.END_STONE_BRICKS.defaultBlockState();
            case WIND -> Blocks.CONCRETE.white().defaultBlockState();
            case EARTH -> Blocks.MUD_BRICKS.defaultBlockState();
            case SONIC -> Blocks.SCULK.defaultBlockState();
            default -> Blocks.BONE_BLOCK.defaultBlockState();
        };
        java.util.List<BlockState> palette = new java.util.ArrayList<>(java.util.List.of(theme));
        BlockState accent = palette.get(0);
        BlockState second = palette.size() > 1 ? palette.get(1) : theme;
        platform(level, c, 10, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 22);
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 <= 100 && r.nextInt(4) == 0) set(level, c.offset(dx, -1, dz), r.nextBoolean() ? accent : theme);
            if (d2 >= 30 && d2 <= 36) set(level, c.offset(dx, -1, dz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            BlockPos base = c.offset((int) Math.round(Math.cos(a) * 8), 0, (int) Math.round(Math.sin(a) * 8));
            int h = 5 + r.nextInt(4);
            for (int y = 0; y < h; y++) set(level, base.above(y), y % 3 == 2 ? accent : Blocks.BLACKSTONE.defaultBlockState());
            set(level, base.above(h), second);
            set(level, base.above(h + 1), Blocks.SOUL_LANTERN.defaultBlockState());
        }
        // An effigy of the creature's concept at the heart of the shrine.
        for (int y = 1; y <= 3; y++) set(level, c.offset(0, y, 4), accent);
        set(level, c.offset(0, 4, 4), second);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) set(level, c.offset(dx, 0, dz), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            BlockPos p = c.offset(i < 2 ? -2 : 2, 0, i % 2 == 0 ? -2 : 2);
            set(level, p, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true)
                    .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
        }
        BlockPos altar = c.above();
        set(level, altar, ModBlocks.SHRINE_ALTAR.get().defaultBlockState());
        return altar;
    }

    private ShrineBuilder() {}
}
