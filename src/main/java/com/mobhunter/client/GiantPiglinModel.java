package com.mobhunter.client;

import com.mobhunter.core.AnimPose;
import com.mobhunter.core.AnimState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.util.Mth;

/**
 * Upper body of the Giant Zombie Piglin, built from the vanilla zombified piglin mesh but animated
 * with its own huge, telegraphed sword swings.
 */
public class GiantPiglinModel extends EntityModel<GiantPiglinModel.State> {
    public static class State extends DeityRenderState {
        public final BlockModelRenderState blade = new BlockModelRenderState();
        public final BlockModelRenderState grip = new BlockModelRenderState();
        public final BlockModelRenderState pommel = new BlockModelRenderState();
        public final BlockModelRenderState chip = new BlockModelRenderState();
    }

    public final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg;

    public GiantPiglinModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    private static float ease(float p) {
        return p * p * (3 - 2 * p);
    }

    @Override
    public void setupAnim(State s) {
        super.setupAnim(s);
        resetPose();
        rightLeg.visible = false;
        leftLeg.visible = false;
        float t = s.ageInTicks;
        head.yRot = (s.yRot - s.bodyRot) * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        // Idle: hunched, swaying, sword arm held low and forward.
        body.xRot = 0.08f + Mth.sin(t * 0.04f) * 0.03f;
        rightArm.xRot = -0.35f + Mth.sin(t * 0.05f) * 0.05f;
        leftArm.xRot = -1.1f + Mth.sin(t * 0.06f) * 0.08f;
        leftArm.zRot = 0.1f;
        rightArm.zRot = -0.1f;

        float p = s.progress, e = ease(p);
        boolean w = s.phase == AnimState.WINDUP, a = s.phase == AnimState.ACTIVE, r = s.phase == AnimState.RECOVER;
        AnimPose pose = s.pose;
        switch (pose) {
            case GIANT_OVERHEAD -> {
                if (w) {
                    rightArm.xRot = Mth.lerp(e, -0.35f, -3.0f);
                    body.xRot = Mth.lerp(e, 0.08f, -0.25f);
                    leftArm.xRot = Mth.lerp(e, -1.1f, -2.6f);
                } else if (a) {
                    float k = Math.min(1, p * 3);
                    rightArm.xRot = Mth.lerp(k, -3.0f, -0.3f);
                    leftArm.xRot = Mth.lerp(k, -2.6f, -0.5f);
                    body.xRot = Mth.lerp(k, -0.25f, 0.55f);
                } else {
                    rightArm.xRot = Mth.lerp(e, -0.3f, -0.35f);
                    body.xRot = Mth.lerp(e, 0.55f, 0.08f);
                }
            }
            case GIANT_SIDE_SWING -> {
                float twist, armY;
                if (w) {
                    twist = 0.9f * e;
                    armY = 1.2f * e;
                } else if (a) {
                    twist = Mth.lerp(e, 0.9f, -0.9f);
                    armY = Mth.lerp(e, 1.2f, -1.2f);
                } else {
                    twist = -0.9f * (1 - e);
                    armY = -1.2f * (1 - e);
                }
                body.yRot = twist;
                rightArm.yRot = armY + twist;
                rightArm.xRot = -1.5f;
                leftArm.yRot = twist;
            }
            case GIANT_REACH -> {
                if (w) {
                    leftArm.xRot = Mth.lerp(e, -1.1f, -1.7f);
                    leftArm.yRot = -0.3f * e;
                    body.xRot = 0.08f + 0.35f * e;
                } else if (a) {
                    leftArm.xRot = p < 0.65f ? -2.2f : Mth.lerp((p - 0.65f) / 0.35f, -2.8f, -0.8f);
                    body.xRot = p < 0.65f ? 0.05f : Mth.lerp((p - 0.65f) / 0.35f, -0.2f, 0.4f);
                    leftArm.yRot = -0.2f;
                } else {
                    leftArm.xRot = Mth.lerp(e, -0.8f, -1.1f);
                }
            }
            case GIANT_SCOOP -> {
                if (w) {
                    body.xRot = 0.08f + 0.75f * e;
                    rightArm.xRot = Mth.lerp(e, -0.35f, -0.6f);
                    leftArm.xRot = Mth.lerp(e, -1.1f, -0.6f);
                    leftArm.zRot = 0.1f - 0.3f * e;
                } else if (a) {
                    body.xRot = -0.3f;
                    leftArm.xRot = -2.9f;
                    rightArm.xRot = -2.6f;
                } else {
                    body.xRot = Mth.lerp(e, -0.3f, 0.08f);
                    leftArm.xRot = Mth.lerp(e, -1.8f, -1.1f);
                    rightArm.xRot = Mth.lerp(e, -1.8f, -0.35f);
                }
            }
            case GIANT_ROAR, ROAR -> {
                float k = w ? e : a ? (p < 0.8f ? 1 : 1 - (p - 0.8f) * 5) : 0;
                head.xRot -= 0.6f * k;
                body.xRot -= 0.3f * k;
                rightArm.zRot -= 0.9f * k;
                leftArm.zRot += 0.9f * k;
                rightArm.xRot -= 0.6f * k;
                leftArm.xRot += 0.5f * k;
                head.yRot += Mth.sin(t * 2.5f) * 0.05f * k;
            }
            case EMERGE -> {
                // Clawing its way up through the portal.
                float k = 1 - p;
                rightArm.xRot = -2.8f + Mth.sin(t * 0.3f) * 0.3f * k;
                leftArm.xRot = -2.8f + Mth.cos(t * 0.3f) * 0.3f * k;
                body.xRot = 0.4f * k;
            }
            default -> { }
        }
        // The colossal sword is held in both hands, except while it reaches out or roars.
        if (pose != AnimPose.GIANT_REACH && pose != AnimPose.GIANT_ROAR && pose != AnimPose.ROAR && pose != AnimPose.EMERGE) {
            rightArm.yRot -= 0.3f;
            leftArm.xRot = rightArm.xRot;
            leftArm.yRot = rightArm.yRot + 0.9f;
            leftArm.zRot = -rightArm.zRot;
        }
    }
}
