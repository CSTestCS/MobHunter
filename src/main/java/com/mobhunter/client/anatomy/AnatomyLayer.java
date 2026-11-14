package com.mobhunter.client.anatomy;

import com.mobhunter.MobHunter;
import com.mobhunter.client.TelegraphData;
import com.mobhunter.client.TelegraphRenderEvents;
import com.mobhunter.core.Tier;
import com.mobhunter.profile.Bestiary;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Draws the custom anatomy of Alpha, Monstrous and Deity creatures on top of their body. */
public class AnatomyLayer extends RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {
    public static final Identifier ATLAS = MobHunter.id("textures/entity/anatomy.png");
    public static final Identifier GLOW = MobHunter.id("textures/entity/anatomy_glow.png");
    private static final RenderType SOLID = RenderTypes.entityCutout(ATLAS);
    private static final RenderType EMISSIVE = RenderTypes.eyes(GLOW);
    private static final java.util.Set<Identifier> FAILED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public AnatomyLayer(RenderLayerParent<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector collector, int light, LivingEntityRenderState state, float yRot, float xRot) {
        TelegraphData d = state.getRenderData(TelegraphRenderEvents.DATA);
        if (d == null || d.tier() == Tier.NONE || d.creature() == null || state.isInvisible) return;
        Bestiary.Entry entry = Bestiary.get(d.creature());
        if (entry == null) return;
        BoxBuffer solid = new BoxBuffer(), glow = new BoxBuffer();
        try {
            Skeleton sk = Skeleton.scan(getParentModel().root(), ps);
            Anatomy.build(sk, entry, d.color(), d.tier(), state.ageInTicks, solid, glow);
        } catch (RuntimeException ex) {
            // Never let an unusual model break rendering: the creature is simply drawn without its growths.
            if (FAILED.add(d.creature())) MobHunter.LOGGER.warn("Could not grow anatomy for {}", d.creature(), ex);
            return;
        }
        int overlay = LivingEntityRendererAccess.overlay(state);
        if (!solid.isEmpty()) collector.submitCustomGeometry(ps, SOLID, (pose, buf) -> solid.write(buf, light, overlay));
        if (!glow.isEmpty()) collector.submitCustomGeometry(ps, EMISSIVE, (pose, buf) -> glow.write(buf, 0xF000F0, OverlayTexture.NO_OVERLAY));
    }
}
