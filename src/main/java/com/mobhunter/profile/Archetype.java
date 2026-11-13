package com.mobhunter.profile;

import java.util.List;

/**
 * Broad body plan of a creature. Supplies the default moveset, material names and the
 * flavour text of the clues it leaves behind.
 */
public enum Archetype {
    BEAST("hide", "horn", "heart", Entrance.CRASH_LANDING,
            List.of("strike", "charge", "leap_slam", "stomp_quake", "tail_spin", "roar"),
            new String[]{"Enormous tracks are pressed deep into the earth here.", "Tufts of shed hide cling to the ground, still warm.", "A half-eaten carcass lies here, gnawed to the bone.", "Terrified animals huddle beside a pile of picked-clean bones. Something is coming for them."}),
    PREDATOR("pelt", "fang", "heart", Entrance.CRASH_LANDING,
            List.of("strike", "pounce", "leap_slam", "tail_spin", "roar", "rampage"),
            new String[]{"Paw prints the size of shields circle this spot.", "Claw marks score the ground, and a clump of shed pelt.", "A savaged kill, dragged and half buried.", "A den of torn prey. Fresh blood. It is hunting right now."}),
    AVIAN("plume", "talon", "wing_heart", Entrance.SKY_DIVE,
            List.of("strike", "dive_bomb", "gale", "feather_volley", "roar"),
            new String[]{"Huge three-toed tracks and scattered down.", "An enormous moulted feather, still twitching in the wind.", "A nest of crushed bones and shell fragments.", "Prey lies frozen beneath circling shadows. It is watching from above."}),
    ARTHROPOD("carapace", "mandible", "venom_sac", Entrance.BURROW,
            List.of("strike", "leap_slam", "web_snare", "venom_spit", "burrow_ambush", "summon_brood"),
            new String[]{"Rows of skittering leg marks crisscross the dirt.", "A shed exoskeleton, split down the back.", "Cocooned husks of drained prey hang here.", "A writhing nest. Dozens of silk-wrapped bodies. The ground is moving."}),
    UNDEAD("rotten_hide", "grave_claw", "black_heart", Entrance.BURROW,
            List.of("strike", "lunge", "rot_breath", "burrow_ambush", "summon_brood", "roar"),
            new String[]{"Dragging footprints, and the smell of the grave.", "Strips of rotting flesh and torn cloth.", "A fresh grave, dug open from the inside.", "A pile of corpses, stacked like a feast. It means to eat them."}),
    SKELETAL("bone", "marrow_spike", "soul_core", Entrance.ASSEMBLE,
            List.of("strike", "arrow_volley", "bone_spikes", "tail_spin", "summon_brood"),
            new String[]{"Splintered bone fragments scattered in a line.", "A cracked rib longer than your arm.", "An ossuary of picked bones arranged in rings.", "A mound of skulls, every one facing you."}),
    AQUATIC("scale", "fin", "pearl", Entrance.GEYSER,
            List.of("strike", "torpedo", "water_jet", "ink_cloud", "tail_spin"),
            new String[]{"A wet trail of slime and fish scales.", "Shed scales glinting like coins.", "A gutted catch, left to rot on the shore.", "Beached prey gasping in a ring. The water churns."}),
    SLIME("gel", "gel_core", "nucleus", Entrance.ASSEMBLE,
            List.of("leap_slam", "body_slam", "element_nova", "summon_brood", "strike"),
            new String[]{"A glistening trail of slime.", "A wobbling glob that broke away from something huge.", "Prey half-dissolved inside hardened gel.", "Everything here is coated, dissolving. The puddles are joining together."}),
    CONSTRUCT("plating", "gear", "core", Entrance.ASSEMBLE,
            List.of("strike", "stomp_quake", "charge", "ground_spikes", "roar"),
            new String[]{"Heavy, square footprints crush the grass.", "A fallen scrap of plating, still humming.", "Shattered stone and twisted scrap, torn apart.", "A graveyard of broken things. Something is rebuilding itself."}),
    ENDER("void_hide", "void_shard", "void_eye", Entrance.RIFT,
            List.of("strike", "blink_strike", "void_orbs", "void_pull", "roar"),
            new String[]{"Footprints that appear, vanish, and reappear.", "Purple motes drift from a scorched patch of ground.", "Prey turned inside out, floating a hand above the soil.", "Space itself is torn here. It stares back."}),
    ILLAGER("cloth", "talisman", "dark_sigil", Entrance.FOG,
            List.of("strike", "bolt_volley", "ground_spikes", "summon_brood", "blink_strike"),
            new String[]{"Boot prints of a raiding party... but one set is far too large.", "A torn banner, scorched at the edges.", "A looted wagon and the remains of its owners.", "Captives bound in a ring around a ritual circle."}),
    INFERNAL("cinder_hide", "ember_horn", "ember_heart", Entrance.INFERNO,
            List.of("strike", "fire_breath", "fireball_volley", "element_nova", "charge"),
            new String[]{"Scorched footprints, still smouldering.", "Charred scales flaking ash.", "A burnt carcass, cooked from the inside.", "Blackened prey in a ring of fire. The heat is unbearable."}),
    ELEMENTAL("essence", "shard", "storm_heart", Entrance.THUNDERSTRIKE,
            List.of("strike", "gale", "lightning_call", "element_nova", "dive_bomb"),
            new String[]{"Scoured ground where something touched down.", "Crackling residue hangs in the air.", "Prey flung far and broken.", "The sky darkens. Animals cower in a crater."}),
    SCULK("sculk_hide", "echo_horn", "echo_heart", Entrance.BURROW,
            List.of("strike", "sonic_gaze", "stomp_quake", "burrow_ambush", "roar"),
            new String[]{"Sculk spreads in giant footprints.", "A shed sculk tendril, still pulsing.", "Prey frozen mid-scream.", "Total silence. Hundreds of tendrils point the same way."}),
    BLASTER("blast_hide", "fuse", "blast_core", Entrance.THUNDERSTRIKE,
            List.of("element_nova", "leap_slam", "strike", "lightning_call", "summon_brood"),
            new String[]{"Scorched craters dot the landscape in a trail.", "Flakes of green hide and gunpowder residue.", "A crater filled with what used to be prey.", "Hissing. Everywhere, hissing."});

    public final String hidePart;
    public final String clawPart;
    public final String gemPart;
    public final Entrance entrance;
    public final List<String> abilities;
    private final String[] clues;

    Archetype(String hidePart, String clawPart, String gemPart, Entrance entrance, List<String> abilities, String[] clues) {
        this.hidePart = hidePart;
        this.clawPart = clawPart;
        this.gemPart = gemPart;
        this.entrance = entrance;
        this.abilities = abilities;
        this.clues = clues;
    }

    /** @param stage 1 = tracks, 2 = shed material, 3 = remains, 4 = ominous */
    public String clueText(int stage) {
        return clues[Math.max(0, Math.min(3, stage - 1))];
    }
}
