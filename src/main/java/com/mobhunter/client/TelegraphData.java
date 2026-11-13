package com.mobhunter.client;

import com.mobhunter.core.AnimPose;
import com.mobhunter.core.Tier;

/** Per-frame snapshot of a tiered creature's telegraph animation, attached to its render state. */
public record TelegraphData(Tier tier, AnimPose pose, int phase, float progress, float width, float height, boolean deityEchoes) {
}
