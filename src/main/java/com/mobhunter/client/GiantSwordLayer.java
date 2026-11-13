package com.mobhunter.client;

import com.mobhunter.deity.GiantZombiePiglin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;

/** The Giant Zombie Piglin's sword: built from real, full-size gold blocks. */
public class GiantSwordLayer extends RenderLayer<GiantPiglinModel.State, GiantPiglinModel> {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
    /** Layout in blocks along the blade: z offset, x offset, which part. 0 blade, 1 grip, 2 pommel/accent. */
    private static final int[][] LAYOUT = buildLayout();

    public GiantSwordLayer(RenderLayerParent<GiantPiglinModel.State, GiantPiglinModel> parent) {
        super(parent);
    }

    private static int[][] buildLayout() {
        java.util.List<int[]> l = new java.util.ArrayList<>();
        l.add(new int[]{2, 0, 2});                      // pommel
        l.add(new int[]{1, 0, 1});                      // grip
        l.add(new int[]{0, 0, 1});
        l.add(new int[]{-1, -1, 0});                    // crossguard
        l.add(new int[]{-1, 0, 0});
        l.add(new int[]{-1, 1, 0});
        l.add(new int[]{-1, -2, 2});
        l.add(new int[]{-1, 2, 2});
        for (int z = -2; z >= -12; z--) l.add(new int[]{z, 0, 0}); // blade
        l.add(new int[]{-13, 0, 2});                    // tip
        return l.toArray(new int[0][]);
    }

    public static void extract(BlockModelResolver resolver, GiantPiglinModel.State s) {
        resolver.update(s.blade, Blocks.GOLD_BLOCK.defaultBlockState(), CONTEXT);
        resolver.update(s.grip, Blocks.NETHER_BRICKS.defaultBlockState(), CONTEXT);
        resolver.update(s.pommel, Blocks.RAW_GOLD_BLOCK.defaultBlockState(), CONTEXT);
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector collector, int light, GiantPiglinModel.State s, float yRot, float xRot) {
        if (s.isInvisible) return;
        GiantPiglinModel model = getParentModel();
        ps.pushPose();
        model.root().translateAndRotate(ps);
        model.rightArm.translateAndRotate(ps);
        ps.translate(-1f / 16f, 10f / 16f, 0f);
        float inv = 1f / GiantZombiePiglin.SCALE;
        ps.scale(inv, inv, inv);
        for (int[] b : LAYOUT) {
            BlockModelRenderState block = b[2] == 0 ? s.blade : b[2] == 1 ? s.grip : s.pommel;
            ps.pushPose();
            ps.translate(b[1] - 0.5f, -0.5f, b[0] - 0.5f);
            block.submit(ps, collector, light, OverlayTexture.NO_OVERLAY, s.outlineColor);
            ps.popPose();
        }
        ps.popPose();
    }
}
