package com.mobhunter.registry;

import com.google.common.collect.ImmutableSet;
import com.mobhunter.MobHunter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Hunt Master villager and its job site, the Hunting Board. */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MobHunter.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, MobHunter.MODID);

    public static final ResourceKey<PoiType> HUNTING_BOARD_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, MobHunter.id("hunting_board"));

    public static final Supplier<PoiType> HUNTING_BOARD = POI.register("hunting_board",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.HUNTING_BOARD.get().getStateDefinition().getPossibleStates()), 1, 1));

    private static ResourceKey<TradeSet> trades(int level) {
        return ResourceKey.create(Registries.TRADE_SET, MobHunter.id("hunt_master/level_" + level));
    }

    public static final Supplier<VillagerProfession> HUNT_MASTER = PROFESSIONS.register("hunt_master", () -> new VillagerProfession(
            Component.translatable("entity.mobhunter.villager.hunt_master"),
            p -> p.is(HUNTING_BOARD_POI),
            p -> p.is(HUNTING_BOARD_POI),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_WEAPONSMITH,
            Int2ObjectMap.ofEntries(
                    Int2ObjectMap.entry(1, trades(1)),
                    Int2ObjectMap.entry(2, trades(2)),
                    Int2ObjectMap.entry(3, trades(3)),
                    Int2ObjectMap.entry(4, trades(4)),
                    Int2ObjectMap.entry(5, trades(5)))));

    private ModVillagers() {}
}
