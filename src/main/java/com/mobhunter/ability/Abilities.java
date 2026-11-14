package com.mobhunter.ability;

import net.minecraft.world.entity.EntityTypes;
import static com.mobhunter.core.AnimPose.*;

import com.mobhunter.ability.Families.*;
import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/** Registry of every named creature ability. */
public final class Abilities {
    private static final Map<String, Ability> REGISTRY = new HashMap<>();

    public static <T extends Ability> T register(T ability) {
        REGISTRY.put(ability.id, ability);
        return ability;
    }

    public static @Nullable Ability get(String id) {
        return REGISTRY.get(id);
    }

    static {
        // --- melee
        register(new Melee("strike", 2.5f, 55, 0.6f, 1, false, null, 0)).timing(12, 4, 14, 40).damage(5).weight(14).pose(LEAN_BACK);
        register(new Melee("headbutt", 3f, 35, 1.8f, 1, false, null, 0.3f)).timing(18, 4, 18, 90).damage(8).pose(LEAN_BACK);
        register(new Melee("rear_kick", 3f, 50, 1.6f, 1, true, null, 0.2f)).timing(14, 4, 16, 90).damage(7).pose(REAR_UP);
        register(new Melee("peck_flurry", 2.2f, 45, 0.2f, 5, false, null, 0)).timing(12, 20, 14, 100).damage(9).pose(LEAN_BACK);
        register(new Melee("tusk_toss", 2.8f, 40, 0.6f, 1, false, Element.FIRE, 1.1f)).timing(18, 4, 18, 110).damage(8).pose(CHARGE);
        register(new Melee("gold_cleave", 3.5f, 75, 1.2f, 1, false, Element.FIRE, 0)).timing(22, 4, 20, 110).damage(11).pose(SPIN);
        register(new Melee("tail_spin", 3f, 180, 1.2f, 2, false, null, 0)).timing(16, 12, 18, 120).damage(8).pose(SPIN);
        register(new Melee("shell_spin", 3f, 180, 1.4f, 4, false, Element.WATER, 0)).timing(18, 24, 22, 140).damage(10).pose(SPIN);
        register(new Melee("tentacle_sweep", 4f, 180, 1.0f, 3, false, Element.WATER, 0)).timing(18, 18, 18, 120).damage(8).pose(SPIN);
        // --- charges
        register(new Dash("charge", 0.55f, 1, 1.6f, false, false)).timing(24, 22, 26, 140).damage(9);
        register(new Dash("lunge", 0.6f, 1, 0.8f, false, false)).timing(14, 10, 14, 80).damage(6).range(2, 10);
        register(new Dash("stampede", 0.6f, 3, 1.6f, false, false)).timing(26, 60, 30, 260).damage(9);
        register(new Dash("rampage", 0.7f, 4, 1.8f, false, true)).timing(24, 72, 30, 320).damage(10);
        register(new Dash("roll_charge", 0.65f, 2, 1.4f, false, false)).timing(22, 40, 26, 180).damage(8).pose(CROUCH);
        register(new Dash("torpedo", 0.75f, 1, 1.4f, true, false)).timing(22, 20, 24, 140).damage(9);
        // --- leaps
        register(new Leap("leap_slam", 2.5f, 1.2f, 1f)).timing(22, 50, 24, 140).damage(9);
        register(new Leap("pounce", 1.6f, 0.8f, 0.7f)).timing(16, 40, 18, 100).damage(8).range(2, 12);
        register(new Leap("body_slam", 3.2f, 1.6f, 1.5f)).timing(26, 50, 30, 180).damage(12).range(0, 8);
        // --- shockwaves
        register(new Shockwave("stomp_quake", 9, null)).timing(26, 24, 28, 200).damage(9);
        register(new Shockwave("avalanche", 10, Element.ICE)).timing(28, 26, 28, 220).damage(10);
        register(new Shockwave("magma_wave", 10, Element.FIRE)).timing(28, 26, 28, 220).damage(10);
        // --- breaths
        register(new Breath("fire_breath", 9, 30, Element.FIRE, 0)).timing(26, 36, 26, 200).damage(9);
        register(new Breath("frost_breath", 9, 30, Element.ICE, 0)).timing(26, 36, 26, 200).damage(9);
        register(new Breath("rot_breath", 7, 32, Element.POISON, 0)).timing(24, 36, 24, 200).damage(8);
        register(new Breath("water_jet", 13, 12, Element.WATER, 0.25f)).timing(24, 30, 24, 180).damage(9).pose(LEAN_BACK);
        register(new Breath("sonic_screech", 10, 40, Element.SONIC, 0.4f)).timing(22, 24, 22, 200).damage(7).pose(ROAR);
        register(new Breath("venom_spray", 6, 35, Element.POISON, 0)).timing(18, 24, 20, 160).damage(6);
        // --- volleys
        register(new Volley("venom_spit", 3, 10, 1.1f, 0, false, Element.POISON)).timing(16, 9, 16, 110).damage(5);
        register(new Volley("web_snare", 1, 0, 1.0f, 1.5f, true, Element.EARTH)).timing(16, 4, 14, 140).damage(4);
        register(new Volley("feather_volley", 6, 25, 1.2f, 0, false, Element.WIND)).timing(18, 18, 18, 140).damage(4);
        register(new Volley("arrow_volley", 7, 20, 1.6f, 0, false, Element.RAW)).timing(20, 21, 18, 140).damage(4);
        register(new Volley("bolt_volley", 4, 6, 2.0f, 0, false, Element.RAW)).timing(20, 12, 18, 120).damage(6);
        register(new Volley("fireball_volley", 4, 12, 0.9f, 1.5f, false, Element.FIRE)).timing(22, 16, 20, 150).damage(6);
        register(new Volley("fox_fire", 5, 30, 0.7f, 0, false, Element.FIRE)).timing(20, 15, 18, 140).damage(5);
        register(new Volley("egg_barrage", 8, 30, 0.9f, 1.5f, true, Element.FIRE)).timing(20, 24, 20, 160).damage(5);
        register(new Volley("spit_volley", 4, 12, 1.3f, 0, false, Element.WATER)).timing(16, 12, 16, 110).damage(5);
        register(new Volley("snowball_volley", 8, 20, 1.5f, 0, false, Element.ICE)).timing(16, 24, 16, 130).damage(3);
        register(new Volley("potion_volley", 4, 25, 0.8f, 2.5f, true, Element.POISON)).timing(22, 16, 20, 160).damage(6);
        register(new Volley("skull_barrage", 6, 20, 1.1f, 1.8f, false, Element.VOID)).timing(22, 18, 20, 150).damage(8);
        register(new Volley("void_orbs", 3, 30, 0.6f, 1.2f, false, Element.VOID)).timing(22, 12, 20, 140).damage(7);
        register(new Volley("wind_volley", 4, 15, 1.2f, 1.5f, false, Element.WIND)).timing(18, 12, 18, 130).damage(5);
        // --- spikes
        register(new Spikes("ground_spikes", 10, false)).timing(24, 16, 22, 180).damage(8);
        register(new Spikes("bone_spikes", 10, false)).timing(22, 16, 22, 170).damage(8);
        register(new Spikes("root_spikes", 12, false)).timing(24, 18, 22, 180).damage(9);
        register(new Spikes("spike_burst", 0, true)).timing(20, 10, 20, 160).damage(8).pose(INFLATE);
        // --- novas
        register(new Nova("element_nova", 3.5f, null, 0, false)).timing(34, 4, 30, 240).damage(13);
        register(new Nova("chain_blast", 3f, Element.BLAST, 5, false)).timing(36, 22, 30, 260).damage(12);
        register(new Nova("static_fleece", 4f, Element.THUNDER, 0, true)).timing(30, 4, 26, 220).damage(11);
        register(new Nova("levitation_burst", 5f, Element.WIND, 0, false)).timing(28, 4, 26, 220).damage(6);
        // --- misc
        register(new Blink("blink_strike")).timing(16, 6, 18, 120).damage(8);
        register(new Summon("summon_brood", Abilities::selfType, 2, false, Tier.NONE));
        register(new Summon("howl_pack", c -> EntityTypes.WOLF, 3, false, Tier.NONE));
        register(new Summon("summon_vex", c -> EntityTypes.VEX, 3, false, Tier.NONE));
        register(new Summon("grave_rise", Abilities::selfType, 3, true, Tier.NONE));
        register(new Summon("summon_alphas", Abilities::selfType, 2, false, Tier.ALPHA)).cooldown = 700;
        register(new Lightning("lightning_call", 3)).damage(11);
        register(new Burrow("burrow_ambush")).damage(10).cooldown = 220;
        register(new Dive("dive_bomb", null)).damage(10).cooldown = 160;
        register(new Dive("stinger_dive", Element.POISON)).damage(9).cooldown = 160;
        register(new Roar("roar"));
        register(new Cloud("sandstorm", 8, Element.EARTH, true)).damage(6).cooldown = 260;
        register(new Cloud("spore_burst", 7, Element.POISON, false)).damage(7).cooldown = 240;
        register(new Cloud("ink_cloud", 8, Element.WATER, true)).damage(4).cooldown = 260;
        register(new Cloud("mud_wallow", 7, Element.EARTH, false)).damage(5).cooldown = 240;
        register(new Pull("tongue_lash")).damage(6).cooldown = 120;
        register(new Gaze("sonic_gaze", Element.SONIC)).damage(18).cooldown = 260;
        register(new Gaze("eldritch_gaze", null)).damage(16).cooldown = 240;
        register(new VoidPull("void_pull")).damage(16);
        register(new MeteorRain("meteor_rain", 10, null)).damage(10);
        register(new MeteorRain("trident_storm", 7, Element.WATER)).damage(8);
        register(new Gale("gale")).damage(5).cooldown = 160;
    }

    /** The creature a mob "is": a Deity body counts as the creature it is the god of. */
    public static EntityType<?> selfType(Ctx c) {
        if (c.mob instanceof com.mobhunter.deity.RamGod) return EntityTypes.SHEEP;
        if (c.mob instanceof com.mobhunter.deity.GiantZombiePiglin) return EntityTypes.ZOMBIFIED_PIGLIN;
        return c.mob.getType();
    }

    private Abilities() {}
}
