package com.mobhunter.core;

/**
 * Whole-body anticipation poses that the client applies on top of any creature's own model animation.
 * Each pose describes a wind-up (telegraph), an active (release) and a recovery shape.
 */
public enum AnimPose {
    NONE,
    /** Rears back onto the hind legs, then crashes forward. */
    REAR_UP,
    /** Squashes low like a coiled spring, then stretches out. */
    CROUCH,
    /** Leans back to gather force, then snaps forward. */
    LEAN_BACK,
    /** Twists the body to one side, then spins. */
    SPIN,
    /** Trembles in place. */
    SHAKE,
    /** Swells up, then deflates. */
    INFLATE,
    /** Head down, body low and stretched: a charge. */
    CHARGE,
    /** Rears back and shakes: roar. */
    ROAR,
    /** Sinks into the ground. */
    BURROW,
    /** Rises from the ground (entrance / burrow eruption). */
    EMERGE,
    /** Tilts back while hovering upward before a dive. */
    HOVER,
    /** Scales up from nothing (formation / materialisation). */
    FORM,
    // --- Giant Zombie Piglin arm poses ---
    GIANT_OVERHEAD,
    GIANT_SIDE_SWING,
    GIANT_REACH,
    GIANT_SCOOP,
    GIANT_ROAR,
    // --- Ram God poses ---
    RAM_INHALE,
    RAM_LOWER_HORNS,
    RAM_CALL_STORM;

    public static AnimPose byId(int id) {
        AnimPose[] v = values();
        return id >= 0 && id < v.length ? v[id] : NONE;
    }
}
