package com.mobhunter.client;

import com.mobhunter.MobHunter;
import com.mobhunter.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MobHunter.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ELEMENT_ORB.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.RAM_GOD.get(), DeityRenderers.RamGodRenderer::new);
        event.registerEntityRenderer(ModEntities.STORM_CLOUD.get(), DeityRenderers.StormCloudRenderer::new);
        event.registerEntityRenderer(ModEntities.GIANT_ZOMBIE_PIGLIN.get(), DeityRenderers.GiantPiglinRenderer::new);
        event.registerEntityRenderer(ModEntities.ELDRITCH_DEITY.get(), EldritchDeityRenderer::new);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RamGodModel.LAYER, RamGodModel::createLayer);
        event.registerLayerDefinition(StormCloudModel.LAYER, StormCloudModel::createLayer);
    }

    private ClientSetup() {}
}
