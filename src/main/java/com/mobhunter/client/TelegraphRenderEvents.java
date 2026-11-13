package com.mobhunter.client;

import com.mobhunter.MobHunter;
import com.mobhunter.core.AnimState;
import com.mobhunter.core.Tier;
import com.mobhunter.deity.DeityBoss;
import com.mobhunter.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Applies telegraph poses to every creature and draws the eldritch echoes around generic Deities. */
@EventBusSubscriber(modid = MobHunter.MODID, value = Dist.CLIENT)
public final class TelegraphRenderEvents {
    public static final ContextKey<TelegraphData> DATA = new ContextKey<>(MobHunter.id("telegraph"));
    private static final ContextKey<Boolean> PUSHED = new ContextKey<>(MobHunter.id("telegraph_pushed"));

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier((Class) LivingEntityRenderer.class, (entity, state) -> {
            LivingEntity e = (LivingEntity) entity;
            LivingEntityRenderState s = (LivingEntityRenderState) state;
            Tier tier = e.getExistingDataOrNull(ModAttachments.TIER);
            if (tier == null || tier == Tier.NONE) {
                s.setRenderData(DATA, null);
                return;
            }
            AnimState anim = e.getExistingDataOrNull(ModAttachments.ANIM);
            if (anim == null) anim = AnimState.IDLE;
            float now = e.level().getGameTime() + s.partialTick;
            s.setRenderData(DATA, new TelegraphData(tier, anim.animPose(), anim.phase(), anim.progress(now), e.getBbWidth(), e.getBbHeight(),
                    tier == Tier.DEITY && !(e instanceof DeityBoss)));
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pre(RenderLivingEvent.Pre<?, ?, ?> event) {
        LivingEntityRenderState s = event.getRenderState();
        TelegraphData d = s.getRenderData(DATA);
        if (d == null || d.phase() == AnimState.IDLE_PHASE) {
            s.setRenderData(PUSHED, null);
            return;
        }
        event.getPoseStack().pushPose();
        s.setRenderData(PUSHED, Boolean.TRUE);
        TelegraphPoses.apply(event.getPoseStack(), d, s.bodyRot, s.ageInTicks);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void post(RenderLivingEvent.Post<?, ?, ?> event) {
        LivingEntityRenderState s = event.getRenderState();
        PoseStack ps = event.getPoseStack();
        if (s.getRenderData(PUSHED) != null) {
            ps.popPose();
            s.setRenderData(PUSHED, null);
        }
        TelegraphData d = s.getRenderData(DATA);
        if (d == null || !d.deityEchoes() || s.isInvisible) return;
        // Eldritch echoes: translucent violet afterimages of the Deity drifting around its body.
        LivingEntityRenderer renderer = event.getRenderer();
        Identifier texture = renderer.getTextureLocation(s);
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            float a = s.ageInTicks * 0.015f + i * 2.094f;
            float r = d.width() * 0.75f + 1.5f;
            ps.translate(Math.cos(a) * r, d.height() * (0.15f + 0.1f * (float) Math.sin(s.ageInTicks * 0.04f + i)), Math.sin(a) * r);
            float sc = s.scale * 0.55f;
            ps.scale(sc, sc, sc);
            ps.rotateDegrees(Axis.YP, (180f - s.bodyRot + (float) Math.toDegrees(a) * 0.3f));
            ps.scale(-1, -1, 1);
            ps.translate(0, -1.501f, 0);
            int alpha = (int) (70 + 40 * Math.sin(s.ageInTicks * 0.1f + i));
            int color = (alpha << 24) | 0x9B3DFF;
            event.getSubmitNodeCollector().submitModel(renderer.getModel(), s, ps, RenderTypes.entityTranslucent(texture), 0xF000F0,
                    OverlayTexture.NO_OVERLAY, color, null, 0);
            ps.popPose();
        }
    }

    private TelegraphRenderEvents() {}
}
