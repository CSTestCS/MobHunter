package com.mobhunter.core;

import com.mobhunter.ability.Ability;
import com.mobhunter.ability.Ctx;
import com.mobhunter.hunt.EntranceController;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.UUID;
import net.minecraft.server.level.ServerBossEvent;
import org.jspecify.annotations.Nullable;

/** Mutable server-side combat brain of a tiered creature. */
public final class CombatState {
    public final Object2IntOpenHashMap<String> cooldowns = new Object2IntOpenHashMap<>();
    public @Nullable Ability current;
    public @Nullable Ctx ctx;
    public int phase;
    public int phaseTick;
    public int globalCooldown = 40;
    public boolean initialized;
    public boolean enraged;
    public int stuckTicks;
    public @Nullable EntranceController entrance;
    public @Nullable ServerBossEvent bossBar;
    public @Nullable UUID hunter;
    public int idleRoarCooldown;
    public net.minecraft.world.entity.@Nullable LivingEntity target;
}
