package com.mobhunter.ability;

import com.mobhunter.core.AnimPose;

/**
 * A telegraphed attack. Every ability goes through three phases:
 * <ol>
 *     <li><b>wind-up</b>: the creature holds an anticipation pose and markers show where the hit will land,</li>
 *     <li><b>active</b>: the attack happens,</li>
 *     <li><b>recovery</b>: a punish window where the creature is vulnerable.</li>
 * </ol>
 */
public abstract class Ability {
    public final String id;
    public int windup = 20;
    public int active = 10;
    public int recovery = 20;
    public int cooldown = 120;
    /** Usable when the target is within [minRange, maxRange] + body size. */
    public float minRange = 0f;
    public float maxRange = 4f;
    public AnimPose pose = AnimPose.LEAN_BACK;
    public float damage = 5f;
    public int weight = 10;
    /** When true, the creature keeps walking toward the target during the wind-up. */
    public boolean moveDuringWindup = false;
    /** Fraction of the wind-up at which the aim stops following the target. */
    public float lockAt = 0.7f;

    protected Ability(String id) {
        this.id = id;
    }

    public Ability timing(int windup, int active, int recovery, int cooldown) {
        this.windup = windup;
        this.active = active;
        this.recovery = recovery;
        this.cooldown = cooldown;
        return this;
    }

    public Ability range(float min, float max) {
        this.minRange = min;
        this.maxRange = max;
        return this;
    }

    public Ability pose(AnimPose pose) {
        this.pose = pose;
        return this;
    }

    public Ability damage(float damage) {
        this.damage = damage;
        return this;
    }

    public Ability weight(int w) {
        this.weight = w;
        return this;
    }

    public boolean canUse(Ctx c, double dist) {
        double s = c.size() * 0.5;
        return dist >= minRange + s * 0.5 && dist <= maxRange + s * 1.5;
    }

    /** Called every tick of the wind-up. Draw telegraphs here. */
    public void telegraph(Ctx c, int tick, float progress) {}

    public void begin(Ctx c) {}

    public void activeTick(Ctx c, int tick) {}

    public void end(Ctx c) {}
}
