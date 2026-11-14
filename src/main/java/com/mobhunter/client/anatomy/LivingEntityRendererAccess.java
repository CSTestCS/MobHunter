package com.mobhunter.client.anatomy;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Hurt-flash overlay so the anatomy flashes red together with the body. */
final class LivingEntityRendererAccess {
    static int overlay(LivingEntityRenderState s) {
        return LivingEntityRenderer.getOverlayCoords(s, 0f);
    }

    private LivingEntityRendererAccess() {}
}
