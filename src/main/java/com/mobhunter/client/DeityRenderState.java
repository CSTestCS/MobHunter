package com.mobhunter.client;

import com.mobhunter.core.AnimPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state shared by the bespoke Deities. */
public class DeityRenderState extends LivingEntityRenderState {
    public AnimPose pose = AnimPose.NONE;
    public int phase;
    public float progress;
    public float charge;
    public boolean enraged;
}
