package com.mobhunter.profile;

import static com.mobhunter.core.Element.*;
import static com.mobhunter.profile.Archetype.*;
import static com.mobhunter.profile.CreatureProfile.Movement.*;

import com.mobhunter.core.Element;
import com.mobhunter.profile.CreatureProfile.Movement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Profiles for every creature in vanilla Minecraft 26.3. Creatures added by other mods fall back to an
 * automatically derived profile so they can still become Alphas.
 */
public final class CreatureProfiles {
    private static final Map<Identifier, CreatureProfile> PROFILES = new LinkedHashMap<>();
    private static final Map<Identifier, CreatureProfile> FALLBACK = new LinkedHashMap<>();

    static {
        // ---------------------------------------------------------------- beasts
        p("cow", BEAST, EARTH, 0x4A3628, "The Mother of Herds", List.of("stampede", "stomp_quake"));
        p("mooshroom", BEAST, POISON, 0xA81E1E, "The Mycelial Matriarch", List.of("spore_burst", "charge"));
        p("pig", BEAST, EARTH, 0xF0A5A2, "The Glutton Below", List.of("mud_wallow", "charge"));
        p("sheep", BEAST, THUNDER, 0xE8E8E8, "The Ram God", List.of("headbutt", "static_fleece"));
        p("goat", BEAST, ICE, 0xD8CFC0, "The Summit Breaker", List.of("headbutt", "avalanche"));
        p("horse", BEAST, WIND, 0x8A5A2B, "The Endless Gallop", List.of("stampede", "rear_kick"));
        p("donkey", BEAST, EARTH, 0x7A6A58, "The Burden-Bearer", List.of("rear_kick", "stomp_quake"));
        p("mule", BEAST, EARTH, 0x5A4030, "The Stubborn Colossus", List.of("rear_kick", "charge"));
        p("llama", BEAST, WATER, 0xD9C9A3, "The Spitting Peak", List.of("spit_volley", "rear_kick"));
        p("trader_llama", BEAST, WATER, 0x3B67B5, "The Caravan Eternal", List.of("spit_volley", "charge"));
        p("camel", BEAST, EARTH, 0xD7A85F, "The Dune Sovereign", List.of("sandstorm", "stomp_quake"));
        p("camel_husk", BEAST, VOID, 0x8C7A5B, "The Mummified Caravan", List.of("sandstorm", "summon_brood"));
        p("hoglin", BEAST, FIRE, 0xC67B52, "The Crimson Tusk", List.of("charge", "tusk_toss"));
        p("zoglin", BEAST, VOID, 0xB56E7A, "The Rotting Tusk", List.of("rampage", "tusk_toss"), Entrance.INFERNO);
        p("ravager", BEAST, EARTH, 0x5E5650, "The World Trampler", List.of("rampage", "roar"));
        p("sniffer", BEAST, EARTH, 0x9B3A2E, "The Ancient Digger", List.of("burrow_ambush", "spore_burst"), Entrance.BURROW);
        p("panda", BEAST, EARTH, 0xEAEAEA, "The Bamboo Sage", List.of("body_slam", "roar"));
        p("polar_bear", PREDATOR, ICE, 0xF2F2F2, "The Frozen Maw", List.of("frost_breath", "body_slam"));
        p("armadillo", BEAST, EARTH, 0xAE7B6A, "The Rolling Fortress", List.of("roll_charge", "burrow_ambush"), Entrance.BURROW);
        p("rabbit", BEAST, WIND, 0xB5946C, "The Moon Hopper", List.of("pounce", "burrow_ambush"), Entrance.BURROW);
        p("skeleton_horse", SKELETAL, VOID, 0xD9D2C2, "The Pale Rider", List.of("stampede", "lightning_call"), Entrance.THUNDERSTRIKE);
        p("zombie_horse", UNDEAD, POISON, 0x4E6B3E, "The Plague Steed", List.of("stampede", "rot_breath"));
        p("strider", INFERNAL, FIRE, 0x9C3A3A, "The Lava Strider", List.of("magma_wave", "stomp_quake"));
        p("turtle", AQUATIC, WATER, 0x3C9A4A, "The World Shell", List.of("shell_spin", "stomp_quake"));
        // ---------------------------------------------------------------- predators
        p("wolf", PREDATOR, ICE, 0xD7D3CF, "The Moon-Eater", List.of("howl_pack", "frost_breath"));
        p("fox", PREDATOR, FIRE, 0xE37C21, "The Nine-Tailed Ember", List.of("pounce", "fox_fire"));
        p("cat", PREDATOR, VOID, 0x3A3A3A, "The Eye in the Dark", List.of("pounce", "blink_strike"));
        p("ocelot", PREDATOR, POISON, 0xE3BA55, "The Jungle Phantom", List.of("pounce", "venom_spit"));
        p("frog", PREDATOR, POISON, 0xD07444, "The Swamp Tongue", List.of("tongue_lash", "leap_slam"), Entrance.GEYSER);
        p("tadpole", AQUATIC, WATER, 0x6D533D, "The First Spawn", List.of("summon_brood"), Entrance.GEYSER);
        // ---------------------------------------------------------------- avians & flyers
        p("chicken", AVIAN, FIRE, 0xFFFFFF, "The Thousand-Eyed Hen", List.of("egg_barrage", "peck_flurry"), Entrance.SKY_DIVE, GROUND);
        p("parrot", AVIAN, SONIC, 0x1DB33F, "The Echoing Plumage", List.of("sonic_screech", "dive_bomb"), Entrance.SKY_DIVE, FLYING);
        p("bat", AVIAN, SONIC, 0x4A3A2E, "The Night Swarm", List.of("sonic_screech", "summon_brood"), Entrance.SKY_DIVE, FLYING);
        p("phantom", AVIAN, VOID, 0x43518A, "The Insomnia Wyrm", List.of("dive_bomb", "summon_brood"), Entrance.SKY_DIVE, FLYING);
        p("bee", ARTHROPOD, POISON, 0xF5C531, "The Hive Ascendant", List.of("stinger_dive", "summon_brood"), Entrance.SKY_DIVE, FLYING);
        p("allay", ELEMENTAL, SONIC, 0x37C4F2, "The Choir of Lost Things", List.of("sonic_screech", "blink_strike"), Entrance.RIFT, FLYING);
        p("vex", ILLAGER, VOID, 0x8EA0B5, "The Vengeful Host", List.of("blink_strike", "summon_brood"), Entrance.RIFT, FLYING);
        p("ghast", INFERNAL, FIRE, 0xF0F0F0, "The Weeping Sky", List.of("fireball_volley", "sonic_screech"), Entrance.INFERNO, FLYING);
        p("happy_ghast", ELEMENTAL, WATER, 0xF4F4F4, "The Cloud That Smiles", List.of("water_jet", "gale"), Entrance.FOG, FLYING);
        p("blaze", INFERNAL, FIRE, 0xF6B201, "The Undying Flame", List.of("fireball_volley", "fire_breath"), Entrance.INFERNO, FLYING);
        p("breeze", ELEMENTAL, WIND, 0xB7B5EC, "The Howling Vortex", List.of("gale", "wind_volley"), Entrance.FOG, GROUND);
        // ---------------------------------------------------------------- arthropods
        p("spider", ARTHROPOD, POISON, 0x342D27, "The Weaver Below", List.of("web_snare", "venom_spit"));
        p("cave_spider", ARTHROPOD, POISON, 0x0C424E, "The Nest Mother", List.of("venom_spit", "summon_brood"));
        p("silverfish", ARTHROPOD, EARTH, 0x6E6E6E, "The Stone Devourer", List.of("burrow_ambush", "summon_brood"));
        p("endermite", ENDER, VOID, 0x161616, "The Mite of the Void", List.of("blink_strike", "summon_brood"), Entrance.RIFT, GROUND);
        // ---------------------------------------------------------------- undead
        p("zombie", UNDEAD, POISON, 0x00AFAF, "The Rotting King", List.of("grave_rise", "summon_brood"));
        p("husk", UNDEAD, EARTH, 0x797061, "The Desert Mummy", List.of("sandstorm", "summon_brood"));
        p("drowned", UNDEAD, WATER, 0x8FF1D7, "The Drowned Leviathan", List.of("trident_storm", "water_jet"), Entrance.GEYSER);
        p("zombie_villager", UNDEAD, POISON, 0x563C33, "The Hollow Elder", List.of("grave_rise", "rot_breath"));
        p("zombified_piglin", UNDEAD, FIRE, 0xEA9393, "The Giant Zombie Piglin", List.of("gold_cleave", "summon_brood"), Entrance.INFERNO);
        p("giant", UNDEAD, EARTH, 0x00AFAF, "The First Titan", List.of("stomp_quake", "grave_rise"));
        p("zombie_nautilus", UNDEAD, WATER, 0x5C7F6A, "The Drowned Spiral", List.of("torpedo", "ink_cloud"), Entrance.GEYSER, SWIMMING);
        // ---------------------------------------------------------------- skeletal
        p("skeleton", SKELETAL, RAW, 0xC1C1C1, "The Ossuary Lord", List.of("arrow_volley", "bone_spikes"));
        p("stray", SKELETAL, ICE, 0x617677, "The Frostbound Archer", List.of("arrow_volley", "frost_breath"));
        p("bogged", SKELETAL, POISON, 0x8A9A5B, "The Mire Bones", List.of("arrow_volley", "spore_burst"));
        p("parched", SKELETAL, FIRE, 0xC9A76B, "The Sun-Scorched", List.of("arrow_volley", "sandstorm"));
        p("wither_skeleton", SKELETAL, VOID, 0x2A2A2A, "The Charred Executioner", List.of("tail_spin", "bone_spikes"), Entrance.INFERNO);
        p("wither", SKELETAL, VOID, 0x1D1D1D, "The Tri-Skulled Abyss", List.of("skull_barrage", "element_nova"), Entrance.INFERNO, FLYING);
        // ---------------------------------------------------------------- aquatic
        p("cod", AQUATIC, WATER, 0xC6A271, "The Shoal Without End", List.of("torpedo", "summon_brood"), Entrance.GEYSER, SWIMMING);
        p("salmon", AQUATIC, WATER, 0xA00F10, "The Upstream King", List.of("torpedo", "leap_slam"), Entrance.GEYSER, SWIMMING);
        p("pufferfish", AQUATIC, POISON, 0xF6B201, "The Bloated Dread", List.of("element_nova", "venom_spit"), Entrance.GEYSER, SWIMMING);
        p("tropical_fish", AQUATIC, WATER, 0xEF6915, "The Reef Kaleidoscope", List.of("torpedo", "sonic_screech"), Entrance.GEYSER, SWIMMING);
        p("squid", AQUATIC, WATER, 0x223B4D, "The Abyssal Kraken", List.of("ink_cloud", "tentacle_sweep"), Entrance.GEYSER, SWIMMING);
        p("glow_squid", AQUATIC, SONIC, 0x095656, "The Lantern of the Deep", List.of("ink_cloud", "tentacle_sweep"), Entrance.GEYSER, SWIMMING);
        p("dolphin", AQUATIC, WATER, 0x223B4D, "The Tide Sovereign", List.of("torpedo", "water_jet"), Entrance.GEYSER, SWIMMING);
        p("axolotl", AQUATIC, WATER, 0xFBC1E3, "The Regenerating Smile", List.of("water_jet", "summon_brood"), Entrance.GEYSER, SWIMMING);
        p("guardian", AQUATIC, SONIC, 0x5A8272, "The Watching Monument", List.of("eldritch_gaze", "spike_burst"), Entrance.GEYSER, SWIMMING);
        p("elder_guardian", AQUATIC, SONIC, 0xCECCBA, "The Elder Eye", List.of("eldritch_gaze", "spike_burst"), Entrance.GEYSER, SWIMMING);
        p("nautilus", AQUATIC, WATER, 0xE7D4B5, "The Spiral Abyss", List.of("torpedo", "shell_spin"), Entrance.GEYSER, SWIMMING);
        // ---------------------------------------------------------------- slimes
        p("slime", SLIME, POISON, 0x7EBF6E, "The Primordial Ooze", List.of("body_slam", "summon_brood"));
        p("magma_cube", SLIME, FIRE, 0x340000, "The Molten Heart", List.of("magma_wave", "element_nova"), Entrance.INFERNO);
        p("sulfur_cube", SLIME, POISON, 0xD8C640, "The Sulfurous Cauldron", List.of("spore_burst", "element_nova"));
        // ---------------------------------------------------------------- constructs
        p("iron_golem", CONSTRUCT, EARTH, 0xDBCDC1, "The Iron Warden", List.of("ground_spikes", "body_slam"));
        p("snow_golem", CONSTRUCT, ICE, 0xF0F8F8, "The Eternal Winter", List.of("snowball_volley", "avalanche"));
        p("copper_golem", CONSTRUCT, THUNDER, 0xC46B4B, "The Oxidized Colossus", List.of("lightning_call", "stomp_quake"));
        p("creaking", CONSTRUCT, VOID, 0x5F5F5F, "The Heart of the Pale Wood", List.of("root_spikes", "blink_strike"), Entrance.FOG);
        p("shulker", ENDER, VOID, 0x946794, "The Shell of the End", List.of("void_orbs", "levitation_burst"), Entrance.RIFT, IMMOBILE);
        // ---------------------------------------------------------------- ender
        p("enderman", ENDER, VOID, 0x161616, "The Long Silence", List.of("blink_strike", "void_pull"));
        p("ender_dragon", ENDER, VOID, 0x1C1C1C, "The End Itself", List.of("fire_breath", "void_orbs"), Entrance.RIFT, FLYING, false);
        // ---------------------------------------------------------------- illagers / villagers
        p("pillager", ILLAGER, RAW, 0x532F36, "The Raid Eternal", List.of("bolt_volley", "summon_brood"));
        p("vindicator", ILLAGER, RAW, 0x959B9B, "The Executioner", List.of("tail_spin", "charge"));
        p("evoker", ILLAGER, VOID, 0x959B9B, "The Fang Prophet", List.of("ground_spikes", "summon_vex"));
        p("illusioner", ILLAGER, VOID, 0x135A97, "The Thousand Faces", List.of("blink_strike", "arrow_volley"));
        p("witch", ILLAGER, POISON, 0x340000, "The Cauldron Mother", List.of("potion_volley", "lightning_call"), Entrance.THUNDERSTRIKE);
        p("villager", ILLAGER, RAW, 0x563C33, "The Eternal Merchant", List.of("roar"), Entrance.FOG, GROUND, false);
        p("wandering_trader", ILLAGER, RAW, 0x456296, "The Wanderer Beyond", List.of("blink_strike"), Entrance.FOG, GROUND, false);
        // ---------------------------------------------------------------- nether humanoids
        p("piglin", INFERNAL, FIRE, 0x995F40, "The Golden Hunger", List.of("bolt_volley", "gold_cleave"));
        p("piglin_brute", INFERNAL, FIRE, 0x592A10, "The Bastion's Wrath", List.of("gold_cleave", "rampage"));
        // ---------------------------------------------------------------- unique
        p("creeper", BLASTER, BLAST, 0x0DA70B, "The Hissing Cataclysm", List.of("chain_blast", "element_nova"));
        p("warden", SCULK, SONIC, 0x0F4649, "The Deep Dark Itself", List.of("sonic_gaze", "burrow_ambush"));
    }

    private static void p(String path, Archetype a, Element e, int color, String epithet, List<String> sig) {
        p(path, a, e, color, epithet, sig, a.entrance, GROUND, true);
    }

    private static void p(String path, Archetype a, Element e, int color, String epithet, List<String> sig, Entrance entrance) {
        p(path, a, e, color, epithet, sig, entrance, GROUND, true);
    }

    private static void p(String path, Archetype a, Element e, int color, String epithet, List<String> sig, Entrance entrance, Movement m) {
        p(path, a, e, color, epithet, sig, entrance, m, true);
    }

    private static void p(String path, Archetype a, Element e, int color, String epithet, List<String> sig, Entrance entrance, Movement m, boolean contract) {
        Identifier id = Identifier.withDefaultNamespace(path);
        PROFILES.put(id, new CreatureProfile(id, a, e, color, epithet, sig, entrance, m, contract));
    }

    /** Profile for a type, or a derived fallback for modded mobs. Null for non-creatures. */
    public static @Nullable CreatureProfile get(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        // Bespoke Deity bodies belong to the creature they are the god of.
        if (id.getNamespace().equals("mobhunter")) {
            if (id.getPath().equals("ram_god")) id = Identifier.withDefaultNamespace("sheep");
            else if (id.getPath().equals("giant_zombie_piglin")) id = Identifier.withDefaultNamespace("zombified_piglin");
        }
        CreatureProfile p = PROFILES.get(id);
        if (p != null) return p;
        if (type.getCategory() == MobCategory.MISC) return null;
        return FALLBACK.computeIfAbsent(id, k -> {
            Archetype a = switch (type.getCategory()) {
                case MONSTER -> UNDEAD;
                case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> AQUATIC;
                case AMBIENT -> AVIAN;
                default -> BEAST;
            };
            Movement m = a == AQUATIC ? SWIMMING : GROUND;
            String name = k.getPath().replace('_', ' ');
            return new CreatureProfile(k, a, RAW, 0x8F8F8F, "The " + Character.toUpperCase(name.charAt(0)) + name.substring(1) + " Deity",
                    List.of(), a.entrance, m, false);
        });
    }

    public static @Nullable CreatureProfile get(Identifier id) {
        CreatureProfile p = PROFILES.get(id);
        if (p != null) return p;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(CreatureProfiles::get).orElse(null);
    }

    public static @Nullable CreatureProfile get(Mob mob) {
        return get((net.minecraft.world.entity.LivingEntity) mob);
    }

    /** Profile of a living entity; Deity bodies resolve to the creature they are the god of. */
    public static @Nullable CreatureProfile get(net.minecraft.world.entity.LivingEntity e) {
        if (e instanceof com.mobhunter.deity.EldritchDeity d) return get(d.creature());
        return get(e.getType());
    }

    public static Collection<CreatureProfile> all() {
        return PROFILES.values();
    }

    /** Creatures that can be the target of a hunt contract. */
    public static List<CreatureProfile> contractable() {
        List<CreatureProfile> list = new ArrayList<>();
        for (CreatureProfile p : PROFILES.values()) if (p.contract()) list.add(p);
        return list;
    }

    private CreatureProfiles() {}
}
