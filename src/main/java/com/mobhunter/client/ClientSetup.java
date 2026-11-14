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
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.world.entity.EntityType<?> type : event.getEntityTypes()) {
            if (type == ModEntities.RAM_GOD.get() || type == ModEntities.STORM_CLOUD.get()) continue;
            if (event.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer lr) {
                lr.addLayer(new com.mobhunter.client.anatomy.AnatomyLayer(lr));
            }
        }
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer r = event.getPlayerRenderer(skin);
            if (r != null) r.addLayer(new com.mobhunter.client.anatomy.ArmorAnatomyLayer(r));
        }
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RamGodModel.LAYER, RamGodModel::createLayer);
        event.registerLayerDefinition(StormCloudModel.LAYER, StormCloudModel::createLayer);
    }

    private ClientSetup() {}
}
