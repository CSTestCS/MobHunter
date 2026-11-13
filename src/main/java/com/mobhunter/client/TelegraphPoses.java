package com.mobhunter.client;

import com.mobhunter.core.AnimPose;
import com.mobhunter.core.AnimState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

/**
 * Whole-body anticipation animation applied to <em>any</em> creature's model: rearing, crouching,
 * twisting, swelling... so even vanilla models visibly wind up before every attack.
 */
public final class TelegraphPoses {

    private static float ease(float p) {
        return p * p * (3 - 2 * p);
    }

    public static void apply(PoseStack ps, TelegraphData d, float bodyRot, float age) {
        AnimPose pose = d.pose();
        if (pose == AnimPose.NONE || d.phase() == AnimState.IDLE_PHASE) return;
        float p = d.progress();
        float e = ease(p);
        float half = d.width() * 0.5f;
        float pitch = 0, yaw = 0, sx = 1, sy = 1, sz = 1, tx = 0, ty = 0, pivot = 0;
        boolean windup = d.phase() == AnimState.WINDUP, active = d.phase() == AnimState.ACTIVE, recover = d.phase() == AnimState.RECOVER;
        switch (pose) {
            case REAR_UP -> {
                pivot = -half;
                if (windup) pitch = -32 * e;
                else if (active) pitch = Mth.lerp(Math.min(1, p * 3), -32, 14);
                else pitch = Mth.lerp(e, 14, 0);
            }
            case CROUCH -> {
                if (windup) {
                    sy = 1 - 0.3f * e;
                    sx = sz = 1 + 0.15f * e;
                    pitch = 8 * e;
                } else if (active) {
                    sy = Mth.lerp(e, 1.18f, 1f);
                    sx = sz = Mth.lerp(e, 0.9f, 1f);
                    pitch = -10 * (1 - e);
                } else {
                    sy = 1 - 0.08f * (1 - e);
                }
            }
            case LEAN_BACK -> {
                pivot = -half * 0.5f;
                if (windup) pitch = -20 * e;
                else if (active) pitch = Mth.lerp(Math.min(1, p * 4), -20, 18);
                else pitch = Mth.lerp(e, 18, 0);
            }
            case SPIN -> {
                if (windup) yaw = -50 * e;
                else if (active) yaw = -50 + 410 * e;
                else yaw = 0;
            }
            case SHAKE -> {
                if (windup || active) tx = Mth.sin(age * 2.7f) * 0.05f * Math.max(1, d.width()) * (windup ? e : 1 - p);
            }
            case INFLATE -> {
                if (windup) {
                    float s = 1 + 0.28f * e + Mth.sin(age * 3f) * 0.02f * e;
                    sx = sy = sz = s;
                } else if (active) {
                    float s = Mth.lerp(Math.min(1, p * 3), 1.28f, 0.92f);
                    sx = sy = sz = s;
                } else {
                    sx = sy = sz = Mth.lerp(e, 0.92f, 1f);
                }
            }
            case CHARGE -> {
                if (windup) {
                    pitch = 16 * e;
                    sy = 1 - 0.12f * e;
                    ty = Math.abs(Mth.sin(age * 0.9f)) * 0.06f * d.height() * e;
                } else if (active) {
                    pitch = 16;
                    sz = 1.12f;
                    sy = 0.92f;
                } else {
                    pitch = 16 * (1 - e);
                }
            }
            case ROAR -> {
                pivot = -half * 0.6f;
                if (windup) pitch = -24 * e;
                else if (active) pitch = (p < 0.8f ? -24 : Mth.lerp((p - 0.8f) * 5, -24, 0)) + Mth.sin(age * 3.1f) * 3f;
            }
            case BURROW -> {
                if (windup) ty = -d.height() * 0.85f * e;
                else if (active) ty = -d.height();
                tx = Mth.sin(age * 2.2f) * 0.04f * d.width();
            }
            case EMERGE -> {
                tx = Mth.sin(age * 1.9f) * 0.03f * d.width() * (1 - p);
                pitch = -10 * (1 - p);
            }
            case HOVER -> {
                if (windup) pitch = -18 * e;
                else if (active) pitch = 38;
                else pitch = 38 * (1 - e);
            }
            case FORM -> {
                if (active) {
                    float s = Math.max(0.02f, e);
                    sx = sy = sz = s;
                    yaw = (1 - e) * 180;
                }
            }
            default -> { }
        }
        ps.rotateDegrees(Axis.YP, (-bodyRot));
        ps.translate(tx, ty, 0);
        if (pitch != 0) {
            ps.translate(0, 0, pivot);
            ps.rotateDegrees(Axis.XP, (pitch));
            ps.translate(0, 0, -pivot);
        }
        if (yaw != 0) ps.rotateDegrees(Axis.YP, (yaw));
        if (sx != 1 || sy != 1 || sz != 1) ps.scale(sx, sy, sz);
        ps.rotateDegrees(Axis.YP, (bodyRot));
    }

    private TelegraphPoses() {}
}
