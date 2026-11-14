package com.mobhunter.profile;

import com.mobhunter.core.Element;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Data from the Alpha, Monstrous and Deity bestiary: for every creature its defining material,
 * its element, the title of its Monstrous form, the name of its Deity, and the anatomy that its
 * custom models grow (the first feature is the Deity's enormous defining feature).
 * Generated from the bestiary document.
 */
public final class Bestiary {
    /** Anatomical structures grown by tiered creatures. */
    public enum Feature { HORNS, TUSKS, SPIKES, PLATES, CRYSTALS, BONES, FEATHERS, FINS, FUNGUS, FOLIAGE, FLAMES, CLAWS, MANE, SHELL, CROWN, STINGER, WINGS, TENDRILS, HALO }

    /**
     * @param material      the creature's defining material word ("horn", "fleece"...)
     * @param elementName   the bestiary element ("verdant", "storm"...)
     * @param family        the mechanical element family that drives effects and resistances
     * @param elementColor  colour of the element (cores, crystals, particles)
     * @param monstrousTitle title of the Monstrous form ("Bull")
     * @param deityName     name of the Deity ("Bull Deity")
     */
    public record Entry(String id, String material, String elementName, Element family, int elementColor, String monstrousTitle,
                        String deityName, List<Feature> features) {
        /** "Horn" / "Siege Horn" */
        public String materialTitle() {
            StringBuilder b = new StringBuilder();
            for (String w : material.split(" ")) {
                if (!b.isEmpty()) b.append(' ');
                b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
            return b.toString();
        }

        /** "Hornblade" / "Siege Hornblade" */
        public String bladeWord() {
            return materialTitle() + "blade";
        }

        public String spearWord() {
            return materialTitle() + "-Spear";
        }

        public String plateWord() {
            return materialTitle() + "plate";
        }

        public String elementTitle() {
            return Character.toUpperCase(elementName.charAt(0)) + elementName.substring(1);
        }
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<>();

    private static void e(String id, String material, String element, Element family, int color, String title, String deity, Feature... features) {
        ENTRIES.put(id, new Entry(id, material, element, family, color, title, deity, List.of(features)));
    }

    static {
        e("allay", "resonant", "sound", Element.SONIC, 0x9AF0FF, "Crystal-Winged", "Crystal-Winged Deity", Feature.WINGS, Feature.CRYSTALS, Feature.HALO);
        e("armadillo", "scute", "earth", Element.EARTH, 0x8B6A3E, "Burrow", "Burrow Deity", Feature.SHELL, Feature.PLATES, Feature.CLAWS);
        e("axolotl", "gill", "water", Element.WATER, 0x3A7AFF, "Aquatic", "Aquatic Deity", Feature.FINS, Feature.TENDRILS, Feature.CLAWS);
        e("bat", "echo", "sound", Element.SONIC, 0x9AF0FF, "Cavern", "Cavern Deity", Feature.WINGS, Feature.CLAWS, Feature.SPIKES);
        e("camel", "dune", "sand", Element.EARTH, 0xE0C98A, "Caravan", "Caravan Deity", Feature.SHELL, Feature.PLATES, Feature.CROWN);
        e("camel_husk", "tomb", "dust", Element.EARTH, 0xC9B27C, "Desert Undead", "Desert Undead Deity", Feature.BONES, Feature.TENDRILS, Feature.CROWN);
        e("cat", "stalker", "night", Element.VOID, 0x2A2A4A, "Shadow", "Shadow Deity", Feature.CLAWS, Feature.SPIKES, Feature.MANE);
        e("chicken", "plume", "sky", Element.WIND, 0x9AD0FF, "Rooster", "Rooster Deity", Feature.FEATHERS, Feature.WINGS, Feature.CLAWS);
        e("cod", "scale", "water", Element.WATER, 0x3A7AFF, "Shoal", "Shoal Deity", Feature.FINS, Feature.PLATES, Feature.SPIKES);
        e("copper_golem", "copper", "electricity", Element.THUNDER, 0x4FD8E8, "Clockwork", "Clockwork Deity", Feature.SPIKES, Feature.PLATES, Feature.CRYSTALS);
        e("cow", "horn", "verdant", Element.EARTH, 0x5ACB3A, "Bull", "Bull Deity", Feature.HORNS, Feature.PLATES, Feature.FOLIAGE);
        e("donkey", "pack", "mountain", Element.EARTH, 0x9A9A9A, "Courier", "Courier Deity", Feature.SHELL, Feature.PLATES, Feature.MANE);
        e("frog", "marsh", "water", Element.WATER, 0x3A7AFF, "Leap", "Leap Deity", Feature.FINS, Feature.FOLIAGE, Feature.CLAWS);
        e("glow_squid", "lumen", "light", Element.THUNDER, 0xFFF6B0, "Abyss", "Abyss Deity", Feature.TENDRILS, Feature.CRYSTALS, Feature.HALO);
        e("happy_ghast", "cloud", "wind", Element.WIND, 0xE6F7F2, "Sky", "Sky Deity", Feature.WINGS, Feature.TENDRILS, Feature.HALO);
        e("horse", "mane", "sun", Element.FIRE, 0xFFD23A, "Warhorse", "Warhorse Deity", Feature.MANE, Feature.PLATES, Feature.HALO);
        e("mooshroom", "mycelium", "spores", Element.POISON, 0xC04A4A, "Fungal", "Fungal Deity", Feature.FUNGUS, Feature.HORNS, Feature.FOLIAGE);
        e("mule", "trail", "road", Element.EARTH, 0xA0866A, "Warpack", "Warpack Deity", Feature.SHELL, Feature.PLATES, Feature.MANE);
        e("ocelot", "jungle", "emerald", Element.EARTH, 0x2FCB5A, "Panther", "Panther Deity", Feature.CLAWS, Feature.MANE, Feature.CRYSTALS);
        e("parrot", "feather", "rainbow", Element.SONIC, 0xFF5FBF, "Roc", "Roc Deity", Feature.FEATHERS, Feature.WINGS, Feature.CROWN);
        e("pig", "tusk", "earth", Element.EARTH, 0x8B6A3E, "Boar", "Boar Deity", Feature.TUSKS, Feature.PLATES, Feature.SPIKES);
        e("rabbit", "spring", "moon", Element.WIND, 0xE6E8FF, "Hare", "Hare Deity", Feature.HALO, Feature.CLAWS, Feature.CRYSTALS);
        e("salmon", "river", "water", Element.WATER, 0x3A7AFF, "Leviathan", "Leviathan Deity", Feature.FINS, Feature.PLATES, Feature.SPIKES);
        e("sheep", "fleece", "storm", Element.THUNDER, 0xB8C8FF, "Ram", "Ram Deity", Feature.HORNS, Feature.MANE, Feature.CRYSTALS);
        e("skeleton_horse", "soulbone", "soul-fire", Element.FIRE, 0x3AD9E0, "Gravecharger", "Gravecharger Deity", Feature.BONES, Feature.FLAMES, Feature.MANE);
        e("sniffer", "ancient", "plants", Element.EARTH, 0x4CAF50, "Root Titan", "Root Titan Deity", Feature.FOLIAGE, Feature.FUNGUS, Feature.PLATES);
        e("snow_golem", "frost", "ice", Element.ICE, 0x8FD3FF, "Glacier", "Glacier Deity", Feature.CRYSTALS, Feature.SPIKES, Feature.CROWN);
        e("squid", "ink", "abyss", Element.VOID, 0x1B2A6B, "Kraken", "Kraken Deity", Feature.TENDRILS, Feature.SPIKES, Feature.FINS);
        e("strider", "magma", "lava", Element.FIRE, 0xFF4A00, "Netherwalker", "Netherwalker Deity", Feature.FLAMES, Feature.SPIKES, Feature.PLATES);
        e("sulfur_cube", "brimstone", "explosion", Element.BLAST, 0xFF5A2A, "Sulfur", "Sulfur Deity", Feature.CRYSTALS, Feature.FLAMES, Feature.SPIKES);
        e("tadpole", "pond", "water", Element.WATER, 0x3A7AFF, "Primordial", "Primordial Deity", Feature.FINS, Feature.TENDRILS, Feature.HALO);
        e("tropical_fish", "coral", "prism", Element.SONIC, 0xFF7AD9, "Reef", "Reef Deity", Feature.FINS, Feature.CRYSTALS, Feature.SPIKES);
        e("turtle", "shell", "ocean", Element.WATER, 0x2A6BD9, "Worldshell", "Worldshell Deity", Feature.SHELL, Feature.FOLIAGE, Feature.PLATES);
        e("villager", "emerald", "protection", Element.EARTH, 0x3FBF7F, "Village Warden", "Village Warden Deity", Feature.PLATES, Feature.CROWN, Feature.CRYSTALS);
        e("wandering_trader", "caravan", "fortune", Element.RAW, 0xF2C94C, "Merchant", "Merchant Deity", Feature.MANE, Feature.CROWN, Feature.CRYSTALS);
        e("zombie_horse", "rotbone", "undead", Element.VOID, 0x5A7A4A, "Death Courier", "Death Courier Deity", Feature.BONES, Feature.TENDRILS, Feature.SPIKES);
        e("bee", "honey", "venom", Element.POISON, 0xC8E02A, "Hiveguard", "Hiveguard Deity", Feature.STINGER, Feature.WINGS, Feature.PLATES);
        e("cave_spider", "web", "poison", Element.POISON, 0x5DBB2A, "Widow", "Widow Deity", Feature.SPIKES, Feature.CLAWS, Feature.TENDRILS);
        e("dolphin", "wave", "tide", Element.WATER, 0x3A9AD9, "Ocean Herald", "Ocean Herald Deity", Feature.FINS, Feature.SPIKES, Feature.PLATES);
        e("drowned", "coral", "brine", Element.WATER, 0x2F8C8C, "Leviathan Drowned", "Leviathan Drowned Deity", Feature.CRYSTALS, Feature.TENDRILS, Feature.CROWN);
        e("enderman", "ender", "void", Element.VOID, 0x2A0A4A, "Riftwalker", "Riftwalker Deity", Feature.CRYSTALS, Feature.SPIKES, Feature.HALO);
        e("fox", "brush", "fire", Element.FIRE, 0xFF7A1A, "Ember Fox", "Ember Fox Deity", Feature.MANE, Feature.FLAMES, Feature.CLAWS);
        e("goat", "horn", "wind", Element.WIND, 0xE6F7F2, "Mountain Ram", "Mountain Ram Deity", Feature.HORNS, Feature.MANE, Feature.PLATES);
        e("iron_golem", "iron", "strength", Element.RAW, 0xD0C8B8, "Fortress", "Fortress Deity", Feature.PLATES, Feature.SPIKES, Feature.FOLIAGE);
        e("llama", "pack", "trade", Element.RAW, 0x5ACB5A, "Caravan Beast", "Caravan Beast Deity", Feature.MANE, Feature.SHELL, Feature.CROWN);
        e("nautilus", "spiral", "pressure", Element.WATER, 0x1E4F8C, "Leviathan", "Leviathan Deity", Feature.SHELL, Feature.SPIKES, Feature.TENDRILS);
        e("panda", "bamboo", "shockwave", Element.BLAST, 0xF0F0F0, "Celestial Panda", "Celestial Panda Deity", Feature.SPIKES, Feature.HALO, Feature.PLATES);
        e("piglin", "gold", "fire", Element.FIRE, 0xFF7A1A, "Nether King", "Nether King Deity", Feature.CROWN, Feature.PLATES, Feature.TUSKS);
        e("polar_bear", "frost", "ice", Element.ICE, 0x8FD3FF, "Mountain Bear", "Mountain Bear Deity", Feature.CRYSTALS, Feature.MANE, Feature.CLAWS);
        e("pufferfish", "spine", "venom", Element.POISON, 0xC8E02A, "Poisoned Pearl", "Poisoned Pearl Deity", Feature.SPIKES, Feature.FINS, Feature.CRYSTALS);
        e("spider", "silk", "web", Element.EARTH, 0xDADADA, "World Spider", "World Spider Deity", Feature.SPIKES, Feature.TENDRILS, Feature.CLAWS);
        e("trader_llama", "trade", "fortune", Element.RAW, 0xF2C94C, "Celestial Pack", "Celestial Pack Deity", Feature.MANE, Feature.CROWN, Feature.SHELL);
        e("wolf", "fang", "moonlight", Element.VOID, 0xC8D2FF, "Moon Wolf", "Moon Wolf Deity", Feature.MANE, Feature.CLAWS, Feature.HALO);
        e("zombie_nautilus", "grave spiral", "necrosis", Element.POISON, 0x4F6B4A, "Drowned Spiral", "Drowned Spiral Deity", Feature.SHELL, Feature.BONES, Feature.TENDRILS);
        e("zombified_piglin", "rotgold", "netherfire", Element.FIRE, 0xFF3A2A, "Portal King", "Portal King Deity", Feature.TUSKS, Feature.PLATES, Feature.FLAMES);
        e("blaze", "blaze rod", "fire", Element.FIRE, 0xFF7A1A, "Solar", "Solar Deity", Feature.FLAMES, Feature.SPIKES, Feature.HALO);
        e("bogged", "bogbone", "roots", Element.EARTH, 0x6A8A3A, "Mire Revenant", "Mire Revenant Deity", Feature.FOLIAGE, Feature.FUNGUS, Feature.BONES);
        e("breeze", "windcore", "wind", Element.WIND, 0xE6F7F2, "Tempest", "Tempest Deity", Feature.WINGS, Feature.SPIKES, Feature.HALO);
        e("creaking", "heartwood", "wood", Element.EARTH, 0x8A6A40, "Ancient Creaking", "Ancient Creaking Deity", Feature.FOLIAGE, Feature.SPIKES, Feature.CROWN);
        e("creeper", "blast", "explosion", Element.BLAST, 0xFF5A2A, "World Creeper", "World Creeper Deity", Feature.SPIKES, Feature.CRYSTALS, Feature.PLATES);
        e("elder_guardian", "prismarine", "beam", Element.SONIC, 0x55E0D0, "Temple Sovereign", "Temple Sovereign Deity", Feature.SPIKES, Feature.CRYSTALS, Feature.PLATES);
        e("endermite", "rift", "burrow", Element.EARTH, 0x7A5A3A, "Void Devourer", "Void Devourer Deity", Feature.SPIKES, Feature.CRYSTALS, Feature.CLAWS);
        e("evoker", "arcane", "magic", Element.VOID, 0xB050E0, "Arch-Evoker", "Arch-Evoker Deity", Feature.HALO, Feature.CROWN, Feature.MANE);
        e("ghast", "ghastfire", "fire", Element.FIRE, 0xFF7A1A, "Nether Moon", "Nether Moon Deity", Feature.TENDRILS, Feature.FLAMES, Feature.CROWN);
        e("guardian", "prismarine", "beam", Element.SONIC, 0x55E0D0, "Prismarine Sun", "Prismarine Sun Deity", Feature.SPIKES, Feature.CRYSTALS, Feature.HALO);
        e("hoglin", "tusk", "charge", Element.RAW, 0xC8643C, "Nether Boar", "Nether Boar Deity", Feature.TUSKS, Feature.MANE, Feature.SPIKES);
        e("husk", "sandbone", "dust", Element.EARTH, 0xC9B27C, "Desert Revenant", "Desert Revenant Deity", Feature.BONES, Feature.TENDRILS, Feature.CROWN);
        e("magma_cube", "magma", "lava", Element.FIRE, 0xFF4A00, "Nether Core", "Nether Core Deity", Feature.FLAMES, Feature.CRYSTALS, Feature.SPIKES);
        e("parched", "drought", "drought", Element.FIRE, 0xD9A441, "Wasteland Sovereign", "Wasteland Sovereign Deity", Feature.BONES, Feature.HALO, Feature.SPIKES);
        e("phantom", "nightwing", "shadow", Element.VOID, 0x3A2A5A, "Night Reaper", "Night Reaper Deity", Feature.WINGS, Feature.SPIKES, Feature.TENDRILS);
        e("piglin_brute", "brute gold", "fire", Element.FIRE, 0xFF7A1A, "Warlord", "Warlord Deity", Feature.PLATES, Feature.TUSKS, Feature.SPIKES);
        e("pillager", "raider", "siege", Element.BLAST, 0x8A7A6A, "Illager Warlord", "Illager Warlord Deity", Feature.PLATES, Feature.CROWN, Feature.SPIKES);
        e("ravager", "siege horn", "charge", Element.RAW, 0xC8643C, "Ravager Titan", "Ravager Titan Deity", Feature.HORNS, Feature.PLATES, Feature.SPIKES);
        e("shulker", "shell", "levitation", Element.WIND, 0xD6B3FF, "End Bastion", "End Bastion Deity", Feature.SHELL, Feature.SPIKES, Feature.CRYSTALS);
        e("silverfish", "silverfang", "burrow", Element.EARTH, 0x7A5A3A, "Stone Devourer", "Stone Devourer Deity", Feature.SPIKES, Feature.PLATES, Feature.CLAWS);
        e("skeleton", "bone", "soul-fire", Element.FIRE, 0x3AD9E0, "Bone King", "Bone King Deity", Feature.BONES, Feature.CROWN, Feature.FLAMES);
        e("slime", "gel", "bounce", Element.WIND, 0x7ED957, "Primordial Ooze", "Primordial Ooze Deity", Feature.CRYSTALS, Feature.TENDRILS, Feature.SPIKES);
        e("stray", "glacierbone", "frost", Element.ICE, 0xA8E8FF, "Frozen Bone King", "Frozen Bone King Deity", Feature.CRYSTALS, Feature.BONES, Feature.CROWN);
        e("vex", "soulwing", "phase", Element.VOID, 0x9FB8D0, "Vex Archon", "Vex Archon Deity", Feature.WINGS, Feature.HALO, Feature.SPIKES);
        e("vindicator", "blacksteel", "melee", Element.RAW, 0xB0B0B0, "Executioner", "Executioner Deity", Feature.PLATES, Feature.SPIKES, Feature.CROWN);
        e("warden", "sculk", "resonance", Element.SONIC, 0x1FA8A0, "Deepdark Sovereign", "Deepdark Sovereign Deity", Feature.HORNS, Feature.TENDRILS, Feature.CRYSTALS);
        e("witch", "hex", "alchemy", Element.POISON, 0x8E3FBF, "Witch Queen", "Witch Queen Deity", Feature.CROWN, Feature.FUNGUS, Feature.CRYSTALS);
        e("wither_skeleton", "witherbone", "soul-fire", Element.FIRE, 0x3AD9E0, "Witherbone Tyrant", "Witherbone Tyrant Deity", Feature.BONES, Feature.FLAMES, Feature.SPIKES);
        e("zoglin", "deathtusk", "undead", Element.VOID, 0x5A7A4A, "Apocalypse Boar", "Apocalypse Boar Deity", Feature.TUSKS, Feature.BONES, Feature.SPIKES);
        e("zombie", "rotiron", "undead", Element.VOID, 0x5A7A4A, "Undying King", "Undying King Deity", Feature.CROWN, Feature.PLATES, Feature.TENDRILS);
        e("zombie_villager", "plague", "decay", Element.POISON, 0x6B7A2E, "Plague Elder", "Plague Elder Deity", Feature.FUNGUS, Feature.TENDRILS, Feature.CROWN);
        e("ender_dragon", "dragonbone", "ender", Element.VOID, 0x6B2FB3, "World Dragon", "World Dragon Deity", Feature.SPIKES, Feature.HORNS, Feature.CRYSTALS);
        e("wither", "witherbone", "soul-fire", Element.FIRE, 0x3AD9E0, "Wither Sovereign", "Wither Sovereign Deity", Feature.BONES, Feature.FLAMES, Feature.CROWN);
    }

    public static @Nullable Entry get(Identifier id) {
        return id.getNamespace().equals("minecraft") ? ENTRIES.get(id.getPath()) : null;
    }

    public static @Nullable Entry get(String path) {
        return ENTRIES.get(path);
    }

    public static java.util.Collection<Entry> all() {
        return ENTRIES.values();
    }

    private Bestiary() {}
}
