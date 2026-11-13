package com.mobhunter;

import com.mobhunter.deity.DeityAbilities;
import com.mobhunter.registry.ModAttachments;
import com.mobhunter.registry.ModBlocks;
import com.mobhunter.registry.ModComponents;
import com.mobhunter.registry.ModEntities;
import com.mobhunter.registry.ModItems;
import com.mobhunter.registry.ModRecipes;
import com.mobhunter.registry.ModTabs;
import com.mobhunter.registry.ModVillagers;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * MobHunter: Monster Hunter inspired Alpha, Monstrous and Deity versions of every creature.
 */
@Mod(MobHunter.MODID)
public class MobHunter {
    public static final String MODID = "mobhunter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MobHunter(IEventBus modBus, ModContainer container) {
        ModAttachments.ATTACHMENTS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.LOOT_FUNCTIONS.register(modBus);
        ModVillagers.POI.register(modBus);
        ModVillagers.PROFESSIONS.register(modBus);
        ModTabs.TABS.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
        DeityAbilities.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
