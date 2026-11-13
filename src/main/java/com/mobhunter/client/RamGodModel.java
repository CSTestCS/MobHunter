package com.mobhunter.client;

import com.mobhunter.MobHunter;
import com.mobhunter.core.AnimPose;
import com.mobhunter.core.AnimState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Ram God: a churning mass of storm cloud with a great ram's face pushing out of its front,
 * crowned with curling horns. Rendered at 4x scale.
 */
public class RamGodModel extends EntityModel<DeityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MobHunter.id("ram_god"), "main");
    private final ModelPart cloud;
    private final ModelPart[] puffs = new ModelPart[8];
    private final ModelPart head;
    private final ModelPart jaw;

    public RamGodModel(ModelPart root) {
        super(root);
        this.cloud = root.getChild("cloud");
        for (int i = 0; i < puffs.length; i++) puffs[i] = cloud.getChild("puff" + i);
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition cloud = root.addOrReplaceChild("cloud", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12f, -7f, -10f, 24f, 14f, 20f), PartPose.offset(0f, 10f, 2f));
        // Puffs of cloud billowing around the core.
        float[][] puffs = {
                {-19, -4, -7, 10, 10, 12, 88, 0},  {9, -4, -7, 10, 10, 12, 132, 0},
                {-8, -12, -8, 16, 8, 16, 176, 0},  {-10, -4, 9, 20, 10, 8, 0, 34},
                {-10, 6, -8, 20, 6, 16, 56, 34},   {-16, -10, 2, 8, 6, 8, 128, 34},
                {8, -10, 2, 8, 6, 8, 160, 34},     {-4, -9, 11, 8, 6, 8, 192, 34}};
        for (int i = 0; i < puffs.length; i++) {
            float[] p = puffs[i];
            cloud.addOrReplaceChild("puff" + i, CubeListBuilder.create().texOffs((int) p[6], (int) p[7]).addBox(p[0], p[1], p[2], p[3], p[4], p[5]), PartPose.ZERO);
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 128).addBox(-5f, -6f, -8f, 10f, 10f, 9f)
                        .texOffs(40, 128).addBox(-3f, -2f, -12f, 6f, 6f, 5f)
                        .texOffs(64, 128).addBox(-10f, -5f, -4f, 5f, 2f, 3f)
                        .texOffs(84, 128).addBox(5f, -5f, -4f, 5f, 2f, 3f),
                PartPose.offset(0f, 9f, -9f));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(104, 128).addBox(-2f, 0f, -3f, 4f, 5f, 2f), PartPose.offset(0f, 4f, -8f));
        // Curling horns: each a chain of blocks spiraling back and down around the side of the face.
        PartDefinition rh = head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                .texOffs(0, 192).addBox(-1f, -2f, -2f, 4f, 4f, 6f), PartPose.offsetAndRotation(-5f, -6f, -3f, -0.3f, 0.5f, 0.4f));
        PartDefinition rh2 = rh.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(20, 192).addBox(-2f, -2f, 0f, 4f, 4f, 4f), PartPose.offsetAndRotation(0f, 0f, 4f, 0.9f, 0f, 0f));
        PartDefinition rh3 = rh2.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(36, 192).addBox(-2f, -2f, 0f, 4f, 5f, 4f), PartPose.offsetAndRotation(0f, 0f, 4f, 1.0f, 0f, 0f));
        rh3.addOrReplaceChild("seg4", CubeListBuilder.create().texOffs(52, 192).addBox(-1.5f, -1.5f, 0f, 3f, 3f, 4f), PartPose.offsetAndRotation(0f, 0f, 4f, 1.0f, 0f, 0f));
        PartDefinition lh = head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                .texOffs(0, 208).addBox(-3f, -2f, -2f, 4f, 4f, 6f), PartPose.offsetAndRotation(5f, -6f, -3f, -0.3f, -0.5f, -0.4f));
        PartDefinition lh2 = lh.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(20, 208).addBox(-2f, -2f, 0f, 4f, 4f, 4f), PartPose.offsetAndRotation(0f, 0f, 4f, 0.9f, 0f, 0f));
        PartDefinition lh3 = lh2.addOrReplaceChild("seg3", CubeListBuilder.create().texOffs(36, 208).addBox(-2f, -2f, 0f, 4f, 5f, 4f), PartPose.offsetAndRotation(0f, 0f, 4f, 1.0f, 0f, 0f));
        lh3.addOrReplaceChild("seg4", CubeListBuilder.create().texOffs(52, 208).addBox(-1.5f, -1.5f, 0f, 3f, 3f, 4f), PartPose.offsetAndRotation(0f, 0f, 4f, 1.0f, 0f, 0f));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(DeityRenderState s) {
        super.setupAnim(s);
        resetPose();
        float t = s.ageInTicks;
        // The storm churns constantly.
        for (int i = 0; i < puffs.length; i++) {
            puffs[i].y = Mth.sin(t * 0.06f + i * 1.3f) * 1.2f;
            puffs[i].x = Mth.cos(t * 0.05f + i * 0.7f) * 0.8f;
            float sc = 1f + Mth.sin(t * 0.08f + i) * 0.05f;
            puffs[i].xScale = puffs[i].yScale = puffs[i].zScale = sc;
        }
        cloud.yRot = Mth.sin(t * 0.02f) * 0.05f;
        head.yRot = (s.yRot - s.bodyRot) * Mth.DEG_TO_RAD * 0.5f;
        head.xRot = s.xRot * Mth.DEG_TO_RAD * 0.4f + Mth.sin(t * 0.07f) * 0.04f;
        jaw.xRot = 0.1f + Mth.sin(t * 0.1f) * 0.05f;

        float p = s.progress;
        float e = p * p * (3 - 2 * p);
        boolean windup = s.phase == AnimState.WINDUP, active = s.phase == AnimState.ACTIVE;
        if (s.pose == AnimPose.RAM_INHALE) {
            // Draws breath: head tilts back, the cloud swells.
            if (windup) {
                head.xRot -= 0.5f * e;
                jaw.xRot = 0.1f + 0.6f * e;
                float sw = 1 + 0.18f * e;
                cloud.xScale = cloud.yScale = cloud.zScale = sw;
            } else if (active) {
                head.xRot += 0.3f;
                jaw.xRot = 0.9f;
                float sw = Mth.lerp(Math.min(1, p * 2), 1.18f, 0.95f);
                cloud.xScale = cloud.yScale = cloud.zScale = sw;
            }
        } else if (s.pose == AnimPose.RAM_LOWER_HORNS) {
            if (windup) head.xRot += 0.7f * e;
            else if (active) head.xRot += 0.7f;
            else head.xRot += 0.7f * (1 - e);
            head.z = -9f - (windup ? 2 * e : active ? 2 : 2 * (1 - e));
        } else if (s.pose == AnimPose.RAM_CALL_STORM || s.pose == AnimPose.ROAR) {
            if (windup || active) {
                float k = windup ? e : 1;
                head.xRot -= 0.6f * k;
                jaw.xRot = 0.1f + 0.7f * k;
                for (int i = 0; i < puffs.length; i++) {
                    puffs[i].y -= 2.5f * k;
                    puffs[i].x *= 1 + k;
                }
            }
        }
        if (s.enraged) jaw.xRot += 0.2f;
    }
}
