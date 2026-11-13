package com.mobhunter.deity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** A bespoke Deity entity with its own body, movement and moveset. */
public interface DeityBoss {
    void setHome(BlockPos pos);

    /** Called by the entrance choreography once the Deity has fully manifested. */
    void onEntranceFormed(ServerLevel level);
}
