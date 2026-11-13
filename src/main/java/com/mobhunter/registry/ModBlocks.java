package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.hunt.ClueBlock;
import com.mobhunter.hunt.HuntingBoardBlock;
import com.mobhunter.hunt.ShrineAltarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MobHunter.MODID);

    public static final DeferredBlock<HuntingBoardBlock> HUNTING_BOARD = BLOCKS.registerBlock("hunting_board", HuntingBoardBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD).instrument(NoteBlockInstrument.BASS).ignitedByLava());

    private static BlockBehaviour.Properties clue(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.DIRT).strength(0.3f).sound(SoundType.MUD).noOcclusion().pushReaction(PushReaction.POPPED).noLootTable();
    }

    public static final DeferredBlock<ClueBlock> CLUE_TRACKS = BLOCKS.registerBlock("clue_tracks", p -> new ClueBlock(ClueBlock.Kind.TRACKS, p), ModBlocks::clue);
    public static final DeferredBlock<ClueBlock> CLUE_SHED = BLOCKS.registerBlock("clue_shed", p -> new ClueBlock(ClueBlock.Kind.SHED, p), ModBlocks::clue);
    public static final DeferredBlock<ClueBlock> CLUE_REMAINS = BLOCKS.registerBlock("clue_remains", p -> new ClueBlock(ClueBlock.Kind.REMAINS, p),
            p -> clue(p).sound(SoundType.BONE_BLOCK));
    public static final DeferredBlock<ClueBlock> CLUE_OMINOUS = BLOCKS.registerBlock("clue_ominous", p -> new ClueBlock(ClueBlock.Kind.OMINOUS, p),
            p -> clue(p).sound(SoundType.BONE_BLOCK).mapColor(MapColor.COLOR_RED));
    public static final DeferredBlock<ClueBlock> GORE = BLOCKS.registerBlock("gore", p -> new ClueBlock(ClueBlock.Kind.GORE, p),
            p -> clue(p).mapColor(MapColor.COLOR_RED).sound(SoundType.SLIME_BLOCK));
    public static final DeferredBlock<ShrineAltarBlock> SHRINE_ALTAR = BLOCKS.registerBlock("shrine_altar", p -> new ShrineAltarBlock(false, p),
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable().lightLevel(s -> 9).sound(SoundType.ANCIENT_DEBRIS));
    public static final DeferredBlock<ShrineAltarBlock> SUNKEN_PORTAL = BLOCKS.registerBlock("sunken_portal", p -> new ShrineAltarBlock(true, p),
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(-1f, 3600000f).noLootTable().noCollision().lightLevel(s -> s.getValue(ShrineAltarBlock.LIT) ? 13 : 2)
                    .sound(SoundType.GLASS));

    private ModBlocks() {}
}
