package com.mobhunter.client;

import com.mobhunter.MobHunter;
import com.mobhunter.core.AnimState;
import com.mobhunter.core.CombatState;
import com.mobhunter.deity.GiantZombiePiglin;
import com.mobhunter.deity.RamGod;
import com.mobhunter.deity.StormCloud;
import com.mobhunter.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Renderers of the bespoke Deities. */
public final class DeityRenderers {

    static void fill(LivingEntity e, DeityRenderState s, float partialTicks) {
        AnimState a = e.getExistingDataOrNull(ModAttachments.ANIM);
        if (a == null) a = AnimState.IDLE;
        s.pose = a.animPose();
        s.phase = a.phase();
        s.progress = a.progress(e.level().getGameTime() + partialTicks);
    }

    // ------------------------------------------------------------------------------ Ram God
    public static class RamGodRenderer extends MobRenderer<RamGod, DeityRenderState, RamGodModel> {
        private static final Identifier TEXTURE = MobHunter.id("textures/entity/ram_god.png");
        private static final RenderType EYES = RenderTypes.eyes(MobHunter.id("textures/entity/ram_god_eyes.png"));

        public RamGodRenderer(EntityRendererProvider.Context context) {
            super(context, new RamGodModel(context.bakeLayer(RamGodModel.LAYER)), 0f);
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return EYES;
                }
            });
        }

        @Override
        public DeityRenderState createRenderState() {
            return new DeityRenderState();
        }

        @Override
        public void extractRenderState(RamGod e, DeityRenderState s, float partialTicks) {
            super.extractRenderState(e, s, partialTicks);
            fill(e, s, partialTicks);
            CombatState c = e.getExistingDataOrNull(ModAttachments.COMBAT);
            s.enraged = e.getHealth() < e.getMaxHealth() * 0.4f;
        }

        @Override
        protected void scale(DeityRenderState s, PoseStack ps) {
            float form = s.pose == com.mobhunter.core.AnimPose.FORM && s.phase == AnimState.ACTIVE ? Math.max(0.05f, s.progress) : 1f;
            ps.scale(4f * form, 4f * form, 4f * form);
        }

        @Override
        public Identifier getTextureLocation(DeityRenderState s) {
            return TEXTURE;
        }

        @Override
        protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(RamGod e, float partialTicks) {
            return e.getBoundingBox().inflate(4);
        }
    }

    // --------------------------------------------------------------------------- Storm cloud
    public static class StormCloudRenderer extends MobRenderer<StormCloud, DeityRenderState, StormCloudModel> {
        private static final Identifier TEXTURE = MobHunter.id("textures/entity/storm_cloud.png");

        public StormCloudRenderer(EntityRendererProvider.Context context) {
            super(context, new StormCloudModel(context.bakeLayer(StormCloudModel.LAYER)), 0f);
        }

        @Override
        public DeityRenderState createRenderState() {
            return new DeityRenderState();
        }

        @Override
        public void extractRenderState(StormCloud e, DeityRenderState s, float partialTicks) {
            super.extractRenderState(e, s, partialTicks);
            s.charge = e.charge();
        }

        @Override
        protected void scale(DeityRenderState s, PoseStack ps) {
            ps.scale(2.2f, 2.2f, 2.2f);
        }

        @Override
        protected float getWhiteOverlayProgress(DeityRenderState s) {
            // Flashes brighter as it charges a strike.
            float flicker = (float) Math.abs(Math.sin(s.ageInTicks * 0.8f));
            return s.charge > 0.05f ? Math.min(1f, s.charge * (0.5f + 0.5f * flicker)) : 0f;
        }

        @Override
        public Identifier getTextureLocation(DeityRenderState s) {
            return TEXTURE;
        }
    }

    // ------------------------------------------------------------------- Giant Zombie Piglin
    public static class GiantPiglinRenderer extends MobRenderer<GiantZombiePiglin, GiantPiglinModel.State, GiantPiglinModel> {
        private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");
        private final net.minecraft.client.renderer.block.BlockModelResolver blocks;

        public GiantPiglinRenderer(EntityRendererProvider.Context context) {
            super(context, new GiantPiglinModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIFIED_PIGLIN)), 0f);
            this.blocks = context.getBlockModelResolver();
            addLayer(new GiantSwordLayer(this));
        }

        @Override
        public GiantPiglinModel.State createRenderState() {
            return new GiantPiglinModel.State();
        }

        @Override
        public void extractRenderState(GiantZombiePiglin e, GiantPiglinModel.State s, float partialTicks) {
            super.extractRenderState(e, s, partialTicks);
            fill(e, s, partialTicks);
            GiantSwordLayer.extract(blocks, s);
        }

        @Override
        protected void scale(GiantPiglinModel.State s, PoseStack ps) {
            ps.scale(GiantZombiePiglin.SCALE, GiantZombiePiglin.SCALE, GiantZombiePiglin.SCALE);
            // Only the upper body rises above the portal: drop the waist to the portal plane.
            ps.translate(0f, 0.75f, 0f);
        }

        @Override
        public Identifier getTextureLocation(GiantPiglinModel.State s) {
            return TEXTURE;
        }

        @Override
        protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(GiantZombiePiglin e, float partialTicks) {
            return e.getBoundingBox().inflate(14, 4, 14);
        }

        @Override
        protected boolean shouldShowName(GiantZombiePiglin entity, double distSq) {
            return false;
        }
    }

    private DeityRenderers() {}
}
