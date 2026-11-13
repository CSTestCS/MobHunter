package com.mobhunter.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Synced animation snapshot. {@code phase}: 0 idle, 1 wind-up, 2 active, 3 recovery.
 * Progress is derived on the client from the level game time, so only phase changes are sent.
 */
public record AnimState(int pose, int phase, long start, int length) {
    public static final AnimState IDLE = new AnimState(0, 0, 0L, 1);
    public static final int IDLE_PHASE = 0, WINDUP = 1, ACTIVE = 2, RECOVER = 3;

    public static final StreamCodec<ByteBuf, AnimState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimState::pose,
            ByteBufCodecs.VAR_INT, AnimState::phase,
            ByteBufCodecs.VAR_LONG, AnimState::start,
            ByteBufCodecs.VAR_INT, AnimState::length,
            AnimState::new);

    public AnimPose animPose() {
        return AnimPose.byId(pose);
    }

    public float progress(float gameTime) {
        if (length <= 0) return 1f;
        float p = (gameTime - start) / (float) length;
        return p < 0 ? 0 : (p > 1 ? 1 : p);
    }
}
