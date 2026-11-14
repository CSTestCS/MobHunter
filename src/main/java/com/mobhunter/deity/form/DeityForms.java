package com.mobhunter.deity.form;

import static com.mobhunter.deity.form.DeityForm.Layout.*;
import static com.mobhunter.deity.form.DeityForm.Movement.*;
import static net.minecraft.world.entity.EntityTypes.*;

import com.mobhunter.deity.form.DeityForm.Layout;
import com.mobhunter.deity.form.DeityForm.Movement;
import com.mobhunter.deity.form.DeityForm.Part;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Pufferfish;

import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Deity of every creature. Each is an eldritch god built around the creature's <em>concept</em>:
 * the herd, the hunger, the hive, the grave, the storm... assembled from copies of creatures and
 * living architecture.
 */
public final class DeityForms {
    private static final Map<Identifier, DeityForm> FORMS = new HashMap<>();

    // ------------------------------------------------------------------ builders
    static Part c(EntityType<?> t, Layout l, int n, float r, float s, float y, float spin) {
        return new Part(t, null, l, n, r, s, y, spin, 1, null);
    }

    static Part c(EntityType<?> t, Layout l, int n, float r, float s, float y, float spin, Consumer<Entity> setup) {
        return new Part(t, null, l, n, r, s, y, spin, 1, setup);
    }

    static Part core(EntityType<?> t, float s) {
        return c(t, SINGLE, 1, 0, s, 0, 0);
    }

    static Part core(EntityType<?> t, float s, float y) {
        return c(t, SINGLE, 1, 0, s, y, 0);
    }

    static Part core(EntityType<?> t, float s, float y, Consumer<Entity> setup) {
        return c(t, SINGLE, 1, 0, s, y, 0, setup);
    }

    static Part b(Block block, Layout l, int n, float r, float size, float y, float spin, int len) {
        return new Part(null, block.defaultBlockState(), l, n, r, size, y, spin, len, null);
    }

    static Part b(BlockState state, Layout l, int n, float r, float size, float y, float spin, int len) {
        return new Part(null, state, l, n, r, size, y, spin, len, null);
    }

    static void f(String id, String concept, Movement m, float w, float h, Part... parts) {
        FORMS.put(Identifier.withDefaultNamespace(id), new DeityForm(concept, m, w, h, List.of(parts)));
    }

    static final Consumer<Entity> PUFFED = e -> { if (e instanceof Pufferfish p) p.setPuffState(2); };

    static Consumer<Entity> size(int s) {
        return e -> { if (e instanceof AbstractCubeMob m) m.setSize(s, false); };
    }

    static {
        // ======================================================================== beasts
        f("cow", "a cow of impossible size, crowned by a turning wheel of the whole herd", GROUND, 7, 11,
                core(COW, 8), c(COW, WHEEL, 12, 9, 2.2f, 12, 0.6f), b(Blocks.BONE_BLOCK, SPIRES, 6, 9, 1.2f, 0, 0.2f, 5));
        f("mooshroom", "a fungal matriarch whose spores have become a forest", GROUND, 7, 11,
                core(MOOSHROOM, 8), b(Blocks.RED_MUSHROOM_BLOCK, CROWN, 7, 3.5f, 2f, 11, 0.4f, 2),
                b(Blocks.BROWN_MUSHROOM_BLOCK, TENTACLES, 8, 4, 1.2f, 6, 1f, 7), c(MOOSHROOM, SWARM, 8, 9, 1.5f, 6, 1.2f));
        f("pig", "a bottomless glutton, half-sunk in a swamp of its own making, mouth ringed with what it has eaten", ANCHORED, 9, 7,
                core(PIG, 11, -4), c(PIG, RING, 10, 9, 2.2f, 1, 1.5f), b(Blocks.MUD, TENTACLES, 10, 5, 1.3f, 5, 0.6f, 6),
                b(Blocks.HAY_BLOCK, SWARM, 14, 11, 1.2f, 5, 1.1f, 1));
        f("sheep", "a living thunderhead with the face of a ram", FLOAT, 8, 6, core(SHEEP, 4));
        f("goat", "the mountain itself, rearing its horned head out of an avalanche", GROUND, 8, 13,
                core(GOAT, 9, 2), b(Blocks.PACKED_ICE, SPIRES, 8, 6, 1.5f, 0, 0.3f, 6), b(Blocks.STONE, PYRAMID, 4, 4, 1.6f, 0, 0, 3),
                b(Blocks.SNOW_BLOCK, SWARM, 20, 10, 0.8f, 8, 2f, 1));
        f("horse", "a stampede that never stops: horses galloping forever around a heart of storm", FLOAT, 6, 10,
                core(HORSE, 5, 4), c(HORSE, RING, 10, 8, 2.4f, 1, 6f), c(HORSE, RING, 8, 12, 2f, 5, -4f),
                b(Blocks.WOOL.white(), HELIX, 3, 3, 1f, 0, 8f, 14));
        f("donkey", "a patient colossus bearing a tower of every burden ever placed upon a back", GROUND, 7, 14,
                core(DONKEY, 8), b(Blocks.BARREL, STACK, 1, 0, 2.5f, 11, 0.2f, 5), b(Blocks.CHEST, SWARM, 10, 7, 1.4f, 10, 0.8f, 1),
                b(Blocks.IRON_CHAIN, TENTACLES, 6, 3, 1f, 11, 0.5f, 8));
        f("mule", "a stubborn colossus that has dragged its chains across the world", GROUND, 7, 12,
                core(MULE, 8), b(Blocks.IRON_CHAIN, LEGS, 8, 4, 1.2f, 6, 0.2f, 9), b(Blocks.ANVIL, SWARM, 6, 9, 1.4f, 4, 0.9f, 1));
        f("llama", "a totem of llamas reaching to the sky, spitting judgement", ANCHORED, 6, 18,
                c(LLAMA, STACK, 5, 0, 3.6f, 0, 0.6f), c(LLAMA, RING, 8, 8, 1.8f, 0, -2f), b(Blocks.WOOL.red(), CROWN, 8, 4, 1.5f, 17, 0.8f, 1));
        f("trader_llama", "an endless caravan, marching in a circle that never reaches its destination", FLOAT, 6, 10,
                core(TRADER_LLAMA, 6, 3), c(TRADER_LLAMA, RING, 12, 10, 1.8f, 1, 2f), c(WANDERING_TRADER, RING, 6, 10, 1.5f, 4, 2f),
                b(Blocks.EMERALD_BLOCK, SWARM, 16, 8, 0.8f, 7, 2f, 1));
        f("camel", "a sovereign of the dunes carrying twin sandstone obelisks within a sandstorm", GROUND, 8, 13,
                core(CAMEL, 7), b(Blocks.SANDSTONE, SPIRES, 2, 1.6f, 1.6f, 11, 0, 5), b(Blocks.SAND, HELIX, 4, 9, 0.9f, 0, 5f, 16));
        f("camel_husk", "a mummified caravan risen from beneath the desert, bound in grave wrappings", GROUND, 8, 13,
                core(CAMEL_HUSK, 7), c(HUSK, RING, 8, 9, 2f, 0, 1.5f), b(Blocks.WOOL.white(), TENTACLES, 8, 3, 0.8f, 9, 0.5f, 10),
                b(Blocks.BONE_BLOCK, SPIRES, 6, 11, 1.2f, 0, 0, 6));
        f("hoglin", "a crimson beast whose tusks have grown into a forest of bone", GROUND, 8, 9,
                core(HOGLIN, 6), b(Blocks.BONE_BLOCK, LEGS, 6, 3, 1.2f, 6, 0.3f, 8), b(Blocks.CRIMSON_STEM, CROWN, 6, 3, 1.5f, 9, 0.5f, 3),
                b(Blocks.SHROOMLIGHT, SWARM, 10, 8, 0.8f, 6, 2f, 1));
        f("zoglin", "rot given hooves: a beast that is mostly the corpses of its herd", GROUND, 8, 9,
                core(ZOGLIN, 6), c(ZOGLIN, SWARM, 8, 7, 1.5f, 3, 1.4f), b(Blocks.NETHER_WART_BLOCK, TENTACLES, 8, 3, 1f, 7, 0.9f, 7));
        f("ravager", "the end of every village, walking on pillars of ruin with an army riding its back", GROUND, 9, 14,
                core(RAVAGER, 5, 3), b(Blocks.COBBLESTONE, LEGS, 4, 4, 1.8f, 4, 0, 4), c(PILLAGER, RING, 8, 3.5f, 1.3f, 10, 1f),
                b(Blocks.DARK_OAK_LOG, SPIRES, 6, 9, 1.2f, 0, 0, 7));
        f("sniffer", "the ancient digger, its back an overgrown garden of extinct flowers", GROUND, 9, 10,
                core(SNIFFER, 4.5f), b(Blocks.MOSS_BLOCK, CROWN, 10, 3, 1.4f, 8, 0.2f, 1), b(Blocks.TORCHFLOWER, SWARM, 14, 8, 1.4f, 7, 1f, 1),
                b(Blocks.ROOTED_DIRT, TENTACLES, 8, 3, 1f, 3, 0.4f, 7));
        f("panda", "a meditating sage enthroned in a halo of bamboo", ANCHORED, 7, 10,
                core(PANDA, 7), b(Blocks.BAMBOO_BLOCK, WHEEL, 20, 9, 1.2f, 9, 0.5f, 1), b(Blocks.BAMBOO_BLOCK, SPIRES, 8, 10, 1f, 0, -0.3f, 9));
        f("polar_bear", "a maw of glacial ice that swallows the cold itself", GROUND, 8, 10,
                core(POLAR_BEAR, 6.5f), b(Blocks.PACKED_ICE, CROWN, 8, 3.5f, 1.6f, 9, 0.5f, 3), b(Blocks.BLUE_ICE, LEGS, 8, 4, 1.2f, 4, 0, 6),
                b(Blocks.SNOW_BLOCK, SWARM, 20, 9, 0.6f, 5, 3f, 1));
        f("armadillo", "a rolling fortress: a sphere of armoured bodies that never stops turning", GROUND, 8, 8,
                c(ARMADILLO, SPHERE, 30, 4.5f, 2.2f, 4.5f, 3f), core(ARMADILLO, 5, 2));
        f("rabbit", "the hopper on the moon, who brings the moon down with it", GROUND, 6, 14,
                core(RABBIT, 14), b(Blocks.QUARTZ_BLOCK, SPHERE, 40, 4, 1f, 16, 0.4f, 1), c(RABBIT, SWARM, 16, 9, 2f, 4, 2f));
        f("skeleton_horse", "the pale rider and its steed, trailing the souls of everything it outran", FLOAT, 6, 12,
                core(SKELETON_HORSE, 6), core(SKELETON, 6, 6), b(Blocks.SOUL_LANTERN, SWARM, 14, 7, 1f, 6, 2f, 1),
                b(Blocks.BONE_BLOCK, TENTACLES, 6, 2, 0.8f, 4, 1f, 9));
        f("zombie_horse", "a plague steed and its rider, its hoofprints sprouting rot", GROUND, 6, 12,
                core(ZOMBIE_HORSE, 6), core(ZOMBIE, 6, 6), b(Blocks.MOSS_BLOCK, TENTACLES, 8, 2, 1f, 3, 0.8f, 8),
                b(Blocks.SLIME_BLOCK, SWARM, 10, 7, 0.8f, 5, 1.5f, 1));
        f("strider", "a strider on stilts of living magma, wading through a sea of fire", GROUND, 6, 22,
                core(STRIDER, 6, 12), b(Blocks.MAGMA_BLOCK, LEGS, 6, 3, 1.6f, 12, 0.2f, 11), b(Blocks.SHROOMLIGHT, SWARM, 12, 8, 0.8f, 14, 1.5f, 1));
        f("turtle", "the world turtle: a whole land rides on its shell", GROUND, 12, 12,
                core(TURTLE, 9), b(Blocks.GRASS_BLOCK, CROWN, 12, 3, 1.6f, 4, 0.1f, 1), b(Blocks.OAK_LOG, SPIRES, 3, 2, 1.2f, 5, 0.1f, 5),
                b(Blocks.OAK_LEAVES, SPHERE, 24, 3, 1.4f, 11, 0.1f, 1), b(Blocks.STONE, PYRAMID, 3, 3, 1.4f, 5, 0.1f, 2));
        // ======================================================================== predators
        f("wolf", "the moon-eater: a wolf howling at the moon it has half devoured, leading the eternal pack", GROUND, 7, 12,
                core(WOLF, 9), b(Blocks.QUARTZ_BLOCK, SPHERE, 50, 5, 1f, 18, 0.2f, 1), c(WOLF, RING, 10, 10, 2.4f, 0, 3f));
        f("fox", "a nine-tailed fox whose tails burn with the fire it stole from the sun", GROUND, 7, 10,
                core(FOX, 9), b(Blocks.TERRACOTTA, TENTACLES, 9, 2, 1.2f, 4, 0.6f, 10), b(Blocks.SHROOMLIGHT, CROWN, 9, 7, 1f, 10, 0.6f, 1));
        f("cat", "the eye in the dark: a black cat with a thousand watching eyes around it", GROUND, 6, 9,
                core(CAT, 9), b(Blocks.OCHRE_FROGLIGHT, SPHERE, 40, 7, 0.6f, 5, 0.3f, 1), b(Blocks.OBSIDIAN, WHEEL, 16, 8, 1f, 8, -0.4f, 1));
        f("ocelot", "the jungle phantom: a hunter seen only as a blur of afterimages", GROUND, 6, 8,
                core(OCELOT, 9), c(OCELOT, SWARM, 10, 7, 4f, 1, 5f), b(Blocks.JUNGLE_LEAVES, TENTACLES, 10, 3, 1.2f, 7, 0.8f, 6));
        f("frog", "the swamp tongue: a frog-god whose tongues reach in every direction", ANCHORED, 8, 8,
                core(FROG, 12), b(Blocks.WOOL.pink(), TENTACLES, 6, 2, 1f, 4, 1.2f, 12), b(Blocks.LILY_PAD, RING, 16, 9, 2f, 0.1f, 0.3f, 1),
                c(TADPOLE, SWARM, 20, 9, 3f, 2, 2f));
        f("tadpole", "the first spawn: a churning cloud of every life that was ever born in water", FLOAT, 6, 6,
                c(TADPOLE, SPHERE, 40, 4, 4f, 3, 2f), b(Blocks.FROGSPAWN, SWARM, 20, 6, 1.5f, 3, 1f, 1), core(FROG, 5, 1));
        // ======================================================================== avians & flyers
        f("chicken", "the thousand-eyed hen: an egg-shaped shell of chickens around one vast, staring eye", FLOAT, 6, 9,
                c(CHICKEN, SPHERE, 36, 5, 3f, 5, 1.2f), b(Blocks.OCHRE_FROGLIGHT, SINGLE, 1, 0, 4f, 3, 0.5f, 1), core(CHICKEN, 6, 1));
        f("parrot", "a prism of plumage that repeats every sound ever made, louder", FLOAT, 6, 8,
                core(PARROT, 10), c(PARROT, SWARM, 24, 8, 2.5f, 4, 3f), b(Blocks.WOOL.lime(), WHEEL, 12, 7, 1f, 6, 1f, 1),
                b(Blocks.WOOL.red(), WHEEL, 12, 8, 1f, 6, -1f, 1));
        f("bat", "the night swarm: a cloud of wings in the shape of a bat", FLOAT, 7, 7,
                core(BAT, 10), c(BAT, SWARM, 60, 8, 2f, 3, 4f));
        f("phantom", "the insomnia wyrm: a serpent of phantoms coiling through the sky", FLOAT, 6, 6,
                core(PHANTOM, 5), c(PHANTOM, HELIX, 14, 6, 2.5f, 0, 2.5f), b(Blocks.WOOL.blue(), SWARM, 12, 9, 0.6f, 4, 2f, 1));
        f("bee", "the hive ascendant: a queen enthroned in a cathedral of honeycomb", FLOAT, 7, 9,
                core(BEE, 9), b(Blocks.HONEYCOMB_BLOCK, CROWN, 10, 4, 1.4f, 8, 0.6f, 2), b(Blocks.BEE_NEST, RING, 6, 7, 1.6f, 2, 0.8f, 1),
                c(BEE, SWARM, 40, 9, 1.6f, 4, 4f));
        f("allay", "a choir of lost things, singing the songs of everything ever dropped", FLOAT, 6, 8,
                core(ALLAY, 8), c(ALLAY, WHEEL, 12, 7, 2.5f, 5, 1.2f), b(Blocks.NOTE_BLOCK, SWARM, 12, 7, 1f, 4, 1.5f, 1),
                b(Blocks.JUKEBOX, RING, 4, 4, 1.2f, 0, 1f, 1));
        f("vex", "the vengeful host: a storm of spirits around a heart of iron", FLOAT, 6, 8,
                core(VEX, 9), c(VEX, SWARM, 36, 9, 2.5f, 4, 5f), b(Blocks.IRON_BARS, CAGE, 12, 4, 1f, 0, 1f, 7));
        f("ghast", "the weeping sky: a ghast whose tears have frozen into hanging spires", FLOAT, 8, 8,
                core(GHAST, 2.2f), b(Blocks.BLUE_ICE, TENTACLES, 12, 4, 1f, 0, 0.3f, 12), c(GHAST, RING, 4, 12, 0.8f, 4, 1f));
        f("happy_ghast", "the cloud that smiles, carrying a sky-village in its embrace", FLOAT, 8, 8,
                core(HAPPY_GHAST, 2.2f), c(HAPPY_GHAST, RING, 4, 12, 0.7f, 4, 0.8f), b(Blocks.WOOL.white(), SPHERE, 30, 8, 1.5f, 4, 0.4f, 1),
                b(Blocks.OAK_PLANKS, CROWN, 8, 3, 1.2f, 9, 0.3f, 1));
        f("blaze", "the undying flame: a column of rods and fire that is its own sun", FLOAT, 6, 12,
                core(BLAZE, 7, 2), b(Blocks.SHROOMLIGHT, HELIX, 3, 4, 1f, 0, 6f, 14), c(BLAZE, RING, 6, 8, 2f, 5, 3f),
                b(Blocks.MAGMA_BLOCK, SWARM, 12, 6, 0.8f, 6, 3f, 1));
        f("breeze", "the howling vortex: wind made flesh, spinning breezes into a tornado", GROUND, 6, 16,
                c(BREEZE, HELIX, 12, 4, 3f, 0, 9f), b(Blocks.WOOL.white(), HELIX, 3, 6, 1f, 0, 7f, 18), core(BREEZE, 6, 6));
        // ======================================================================== arthropods
        f("spider", "the weaver below: a spider on legs that span valleys, spinning the dark", GROUND, 10, 10,
                core(SPIDER, 6, 5), b(Blocks.COAL_BLOCK, LEGS, 8, 3, 1.4f, 6, 0.1f, 12), b(Blocks.COBWEB, WHEEL, 24, 9, 1.4f, 9, 0.3f, 1),
                c(CAVE_SPIDER, SWARM, 10, 9, 1.5f, 2, 1.5f));
        f("cave_spider", "the nest mother: her brood hangs in a cage of silk around her", ANCHORED, 8, 9,
                core(CAVE_SPIDER, 8, 2), b(Blocks.COBWEB, CAGE, 16, 6, 1.2f, 0, 0.5f, 8), c(CAVE_SPIDER, SWARM, 24, 8, 2f, 4, 2f));
        f("silverfish", "the stone devourer: a worm of a thousand silverfish that eats mountains", GROUND, 6, 14,
                c(SILVERFISH, HELIX, 24, 4, 5f, 0, 2f), b(Blocks.STONE, SWARM, 16, 7, 1f, 5, 2f, 1), core(SILVERFISH, 9));
        f("endermite", "the mite of the void: a parasite chewing holes in the world", GROUND, 6, 10,
                c(ENDERMITE, HELIX, 18, 4, 5f, 0, 3f), b(Blocks.END_STONE, SWARM, 14, 7, 1f, 5, 2f, 1), b(Blocks.OBSIDIAN, RING, 10, 6, 1f, 0, 1f, 1));
        // ======================================================================== undead
        f("zombie", "the rotting king, enthroned on a pyramid of his subjects", ANCHORED, 10, 14,
                c(ZOMBIE, PYRAMID, 5, 7, 2.4f, 0, 0), core(ZOMBIE, 5, 7), b(Blocks.GOLD_BLOCK, CROWN, 6, 1.6f, 0.9f, 16, 0.5f, 1));
        f("husk", "the desert mummy, unwinding endless grave wrappings across the sand", GROUND, 7, 13,
                core(HUSK, 6.5f), b(Blocks.WOOL.white(), TENTACLES, 10, 2, 0.9f, 8, 0.6f, 12), b(Blocks.SANDSTONE, SPIRES, 6, 9, 1.4f, 0, 0, 6));
        f("drowned", "the drowned leviathan, crowned in coral, trailing kelp and the bodies of sailors", FLOAT, 7, 13,
                core(DROWNED, 6.5f), b(Blocks.DRIED_KELP_BLOCK, TENTACLES, 10, 2, 0.9f, 4, 0.8f, 12), b(Blocks.FIRE_CORAL_BLOCK, CROWN, 6, 1.6f, 1f, 13, 0.5f, 1),
                c(DROWNED, SWARM, 8, 9, 2f, 2, 1.5f));
        f("zombie_villager", "the hollow elder, ringing the bells of a village that is no more", GROUND, 7, 13,
                core(ZOMBIE_VILLAGER, 6.5f), c(ZOMBIE_VILLAGER, RING, 8, 8, 2f, 0, 1f), b(Blocks.LANTERN, SWARM, 14, 7, 1f, 8, 1.5f, 1));
        f("zombified_piglin", "the giant beneath the portal", ANCHORED, 7, 12, core(ZOMBIFIED_PIGLIN, 9));
        f("giant", "the first titan, older than the world, with mountains growing on his shoulders", GROUND, 9, 24,
                core(GIANT, 2), b(Blocks.MOSSY_COBBLESTONE, CROWN, 8, 3, 2f, 22, 0.2f, 3), b(Blocks.STONE, SPIRES, 6, 10, 1.6f, 0, 0, 8));
        f("zombie_nautilus", "a drowned spiral of shells that rings like a dirge", FLOAT, 6, 10,
                core(ZOMBIE_NAUTILUS, 7), c(ZOMBIE_NAUTILUS, HELIX, 12, 5, 2f, 0, 2f), b(Blocks.DARK_PRISMARINE, SWARM, 10, 7, 1f, 4, 1f, 1));
        // ======================================================================== skeletal
        f("skeleton", "the ossuary lord: a wheel of skeletons behind a throne of bone", ANCHORED, 7, 13,
                core(SKELETON, 6), c(SKELETON, WHEEL, 14, 9, 2.4f, 11, 0.8f), b(Blocks.BONE_BLOCK, SPIRES, 8, 7, 1.2f, 0, 0, 8));
        f("stray", "the frostbound archer, wreathed in a blizzard of arrows of ice", GROUND, 7, 13,
                core(STRAY, 6), b(Blocks.PACKED_ICE, CROWN, 8, 2, 1f, 12, 0.8f, 2), c(STRAY, SWARM, 10, 9, 2f, 3, 1.5f),
                b(Blocks.SNOW_BLOCK, SWARM, 24, 10, 0.5f, 6, 4f, 1));
        f("bogged", "the mire bones, a skeleton grown through with fungus and rot", GROUND, 7, 13,
                core(BOGGED, 6), b(Blocks.MOSS_BLOCK, TENTACLES, 10, 2, 1f, 7, 0.5f, 9), b(Blocks.BROWN_MUSHROOM_BLOCK, SWARM, 12, 7, 1f, 6, 1f, 1));
        f("parched", "the sun-scorched archer, carrying a dying sun on its back", GROUND, 7, 14,
                core(PARCHED, 6), b(Blocks.GLOWSTONE, WHEEL, 20, 4, 1.4f, 12, 1.5f, 1), b(Blocks.SANDSTONE, SPIRES, 6, 9, 1.4f, 0, 0, 6));
        f("wither_skeleton", "the charred executioner, standing in a cage of black iron", GROUND, 7, 15,
                core(WITHER_SKELETON, 5.5f), b(Blocks.IRON_BARS, CAGE, 14, 5, 1.2f, 0, 0.6f, 10), b(Blocks.BLACKSTONE, SPIRES, 6, 9, 1.4f, 0, 0, 7));
        f("wither", "the tri-skulled abyss, ringed by a halo of screaming skulls", FLOAT, 8, 10,
                core(WITHER, 3), c(WITHER_SKULL, WHEEL, 12, 7, 3f, 6, 2f), b(Blocks.SOUL_SAND, SWARM, 14, 8, 1f, 4, 2f, 1));
        // ======================================================================== aquatic
        f("cod", "the shoal without end: a school so vast it moves as one great fish", FLOAT, 7, 6,
                c(COD, SWARM, 70, 7, 3f, 3, 3f), core(COD, 14, 2));
        f("salmon", "the upstream king, leaping eternally up a waterfall of its kin", FLOAT, 6, 12,
                c(SALMON, HELIX, 20, 4, 3.5f, 0, 4f), core(SALMON, 14, 6));
        f("pufferfish", "the bloated dread, swollen to bursting with poison spines", FLOAT, 8, 8,
                core(PUFFERFISH, 14, 1, PUFFED), b(Blocks.POINTED_DRIPSTONE, SPHERE, 40, 5.5f, 1.4f, 4, 0.6f, 1),
                c(PUFFERFISH, SWARM, 12, 9, 2.5f, 3, 2f, PUFFED));
        f("tropical_fish", "the reef kaleidoscope: a coral reef that swims", FLOAT, 7, 8,
                c(TROPICAL_FISH, SPHERE, 40, 5, 3f, 4, 1.5f), b(Blocks.BRAIN_CORAL_BLOCK, SWARM, 12, 6, 1f, 4, 1f, 1),
                b(Blocks.FIRE_CORAL_BLOCK, SWARM, 12, 7, 1f, 4, -1f, 1), core(TROPICAL_FISH, 12, 2));
        f("squid", "the abyssal kraken, its arms long enough to drag ships under", FLOAT, 8, 10,
                core(SQUID, 8, 3), b(Blocks.DARK_PRISMARINE, TENTACLES, 10, 3, 1.2f, 3, 1f, 14));
        f("glow_squid", "the lantern of the deep, a beacon that lures sailors to the bottom", FLOAT, 8, 10,
                core(GLOW_SQUID, 8, 3), b(Blocks.SEA_LANTERN, TENTACLES, 8, 3, 1f, 3, 1f, 12), b(Blocks.SEA_LANTERN, SWARM, 16, 9, 0.6f, 5, 2f, 1));
        f("dolphin", "the tide sovereign, a wheel of leaping dolphins rolling across the sea", FLOAT, 7, 8,
                core(DOLPHIN, 8, 2), c(DOLPHIN, WHEEL, 12, 8, 2.6f, 6, 3f), b(Blocks.PRISMARINE, SWARM, 10, 6, 1f, 4, 1f, 1));
        f("axolotl", "the regenerating smile, which grows back everything it loses", GROUND, 7, 6,
                core(AXOLOTL, 12), b(Blocks.WOOL.pink(), TENTACLES, 6, 2, 1f, 4, 0.8f, 7), c(AXOLOTL, SWARM, 14, 8, 2f, 2, 2f));
        f("guardian", "the watching monument: a temple with an eye", ANCHORED, 8, 10,
                core(GUARDIAN, 5, 3), b(Blocks.PRISMARINE_BRICKS, SPIRES, 8, 7, 1.6f, 0, 0, 8), b(Blocks.SEA_LANTERN, CROWN, 8, 7, 1f, 13, 0, 1),
                c(GUARDIAN, RING, 6, 10, 1.5f, 6, 1f));
        f("elder_guardian", "the elder eye, encircled by every guardian it has ever sent", FLOAT, 8, 10,
                core(ELDER_GUARDIAN, 3.5f, 2), c(GUARDIAN, SPHERE, 18, 8, 1.4f, 4, 0.6f), b(Blocks.SEA_LANTERN, WHEEL, 16, 9, 1f, 5, 0.4f, 1));
        f("nautilus", "the spiral abyss, a shell that coils down into the sea floor forever", FLOAT, 6, 10,
                core(NAUTILUS, 8, 3), c(NAUTILUS, HELIX, 14, 5, 2f, 0, 1.5f), b(Blocks.PRISMARINE, HELIX, 2, 7, 1f, 0, 1.5f, 16));
        // ======================================================================== slimes
        f("slime", "the primordial ooze, containing every slime there will ever be", GROUND, 9, 9,
                core(SLIME, 2.5f, 0, size(4)), c(SLIME, SWARM, 14, 7, 1.5f, 4, 1f, size(2)), b(Blocks.SLIME_BLOCK, SPHERE, 20, 6, 1f, 4, 0.5f, 1));
        f("magma_cube", "the molten heart of the nether, pulsing like a furnace", GROUND, 9, 9,
                core(MAGMA_CUBE, 2.5f, 0, size(4)), c(MAGMA_CUBE, RING, 8, 8, 1.5f, 0, 2f, size(2)), b(Blocks.MAGMA_BLOCK, SPIRES, 6, 10, 1.4f, 0, 0, 5));
        f("sulfur_cube", "the sulfurous cauldron, boiling with every reaction at once", GROUND, 9, 9,
                core(SULFUR_CUBE, 2.5f, 0, size(4)), b(Blocks.CAULDRON, RING, 8, 8, 1.6f, 0, 1f, 1), c(SULFUR_CUBE, SWARM, 10, 7, 1.5f, 4, 1.5f, size(2)));
        // ======================================================================== constructs
        f("iron_golem", "the iron warden: a fortress that walks to protect what it loves", GROUND, 8, 16,
                core(IRON_GOLEM, 5.5f), b(Blocks.IRON_BLOCK, SPIRES, 4, 6, 1.6f, 0, 0, 9), b(Blocks.IRON_BARS, WHEEL, 18, 8, 1.2f, 12, 0.4f, 1),
                b(Blocks.POPPY, CROWN, 10, 2, 1.5f, 16, 0.5f, 1));
        f("snow_golem", "the eternal winter: a tower of snow golems wearing burning lanterns", ANCHORED, 6, 18,
                c(SNOW_GOLEM, STACK, 4, 0, 5f, 0, 0.8f), b(Blocks.JACK_O_LANTERN, CROWN, 8, 4, 1.4f, 18, 1f, 1), b(Blocks.SNOW_BLOCK, SWARM, 30, 10, 0.6f, 8, 4f, 1));
        f("copper_golem", "the oxidized colossus that conducts every storm", GROUND, 7, 12,
                core(COPPER_GOLEM, 9), b(Blocks.LIGHTNING_ROD.waxed().unaffected(), CROWN, 8, 3, 1.4f, 10, 1f, 2),
                b(Blocks.COPPER_BLOCK.waxed().oxidized(), SPIRES, 6, 8, 1.4f, 0, 0, 6));
        f("creaking", "the heart of the pale wood: a forest that has learned to stand up", GROUND, 8, 18,
                core(CREAKING, 6), b(Blocks.PALE_OAK_LOG, TENTACLES, 10, 2, 1.2f, 12, 0.4f, 9), b(Blocks.CREAKING_HEART, SINGLE, 1, 0, 2f, 9, 1f, 1),
                b(Blocks.PALE_OAK_LOG, SPIRES, 8, 8, 1.4f, 0, 0, 10));
        f("shulker", "the shell of the end: an orb of shulkers that opens like an eye", ANCHORED, 8, 10,
                c(SHULKER, SPHERE, 26, 5, 2f, 5, 0.5f), core(SHULKER, 4, 3), b(Blocks.PURPUR_PILLAR, SPIRES, 6, 9, 1.4f, 0, 0, 6));
        // ======================================================================== ender
        f("enderman", "the long silence: so tall it scrapes the void, holding pieces of the world it has stolen", GROUND, 5, 26,
                core(ENDERMAN, 8.5f), b(Blocks.GRASS_BLOCK, SWARM, 6, 6, 1.6f, 14, 1f, 1), b(Blocks.TNT, SWARM, 4, 7, 1.6f, 16, -1f, 1),
                b(Blocks.CRYING_OBSIDIAN, WHEEL, 18, 6, 1.2f, 20, 0.6f, 1));
        f("ender_dragon", "the end itself", FLOAT, 10, 8, core(ENDERMAN, 6), c(END_CRYSTAL, RING, 8, 10, 2f, 4, 1f));
        // ======================================================================== illagers
        f("pillager", "the raid eternal: a war banner carried by a crossbow army that never disbands", GROUND, 7, 13,
                core(PILLAGER, 6.5f), c(PILLAGER, RING, 12, 9, 2f, 0, 2f), b(Blocks.DARK_OAK_LOG, SPIRES, 4, 4, 1.2f, 0, 0, 12),
                b(Blocks.WOOL.black(), CROWN, 4, 4, 1.4f, 12, 0, 2));
        f("vindicator", "the executioner, whose cage of iron holds the condemned", GROUND, 7, 14,
                core(VINDICATOR, 6.5f), b(Blocks.IRON_BARS, CAGE, 14, 5, 1.2f, 0, 0.5f, 11), b(Blocks.WOOL.red(), SWARM, 10, 7, 0.6f, 4, 1f, 1));
        f("evoker", "the fang prophet, preaching in a circle of jaws", ANCHORED, 7, 13,
                core(EVOKER, 6.5f), c(EVOKER_FANGS, RING, 16, 8, 3f, 0, 1f), c(VEX, SWARM, 14, 8, 2f, 6, 3f),
                b(Blocks.EMERALD_BLOCK, CROWN, 6, 2, 0.9f, 14, 1f, 1));
        f("illusioner", "the thousand faces: which one is real?", GROUND, 7, 13,
                core(ILLUSIONER, 6.5f), c(ILLUSIONER, RING, 10, 9, 4f, 0, 1.5f), c(ILLUSIONER, WHEEL, 8, 8, 2f, 10, -1f));
        f("witch", "the cauldron mother, brewing the world in her pot", ANCHORED, 8, 14,
                core(WITCH, 6, 4), b(Blocks.CAULDRON, SINGLE, 1, 0, 7f, 0, 0, 1), b(Blocks.BREWING_STAND, SWARM, 10, 8, 1.4f, 7, 1.5f, 1),
                b(Blocks.SLIME_BLOCK, SWARM, 8, 6, 0.6f, 6, -2f, 1));
        f("villager", "the eternal merchant, who has sold everything to everyone", GROUND, 7, 13,
                core(VILLAGER, 6.5f), b(Blocks.EMERALD_BLOCK, WHEEL, 16, 8, 1.2f, 9, 0.8f, 1), c(VILLAGER, RING, 8, 9, 1.5f, 0, 1f));
        f("wandering_trader", "the wanderer beyond, followed by caravans from worlds that no longer exist", GROUND, 7, 13,
                core(WANDERING_TRADER, 6.5f), c(TRADER_LLAMA, RING, 8, 9, 2f, 0, 2f), b(Blocks.EMERALD_BLOCK, SWARM, 12, 7, 0.8f, 7, 1.5f, 1));
        // ======================================================================== nether
        f("piglin", "the golden hunger, wrapped in an orbit of everything gold it has ever hoarded", GROUND, 7, 13,
                core(PIGLIN, 6.5f), b(Blocks.GOLD_BLOCK, SWARM, 24, 8, 1f, 7, 2f, 1), b(Blocks.GILDED_BLACKSTONE, CROWN, 6, 2, 1f, 13, 0.8f, 2));
        f("piglin_brute", "the bastion's wrath: a fortress with a brute's face", GROUND, 8, 14,
                core(PIGLIN_BRUTE, 6.5f), b(Blocks.BLACKSTONE, SPIRES, 8, 6, 1.6f, 0, 0, 9), b(Blocks.GILDED_BLACKSTONE, CROWN, 8, 3, 1.4f, 13, 0.4f, 1),
                c(PIGLIN, RING, 8, 10, 2f, 0, 1.5f));
        // ======================================================================== unique
        f("creeper", "the hissing cataclysm: a pillar of fused, charged creepers under a halo of dynamite", GROUND, 6, 18,
                c(CREEPER, STACK, 4, 0, 4f, 0, 0.5f), b(Blocks.TNT, WHEEL, 16, 7, 1.4f, 13, 1.5f, 1), b(Blocks.TNT, SWARM, 12, 8, 1f, 6, 2f, 1));
        f("warden", "the deep dark itself: a warden grown into the sculk of the entire underground", GROUND, 8, 14,
                core(WARDEN, 4.5f), b(Blocks.SCULK, TENTACLES, 12, 3, 1.2f, 9, 0.5f, 10), b(Blocks.SCULK_SHRIEKER, CROWN, 8, 3, 1.2f, 13, 0.5f, 1),
                b(Blocks.SCULK_SENSOR, SWARM, 14, 9, 1f, 6, 1.5f, 1));
    }

    public static @Nullable DeityForm get(Identifier creature) {
        return FORMS.get(creature);
    }

    /** Fallback for creatures added by other mods: a swarm of itself around a crown of crying obsidian. */
    public static DeityForm fallback(EntityType<?> type) {
        List<Part> parts = new ArrayList<>();
        parts.add(core(type, 6));
        parts.add(c(type, SWARM, 14, 8, 1.5f, 4, 2f));
        parts.add(b(Blocks.CRYING_OBSIDIAN, WHEEL, 16, 8, 1.2f, 8, 0.6f, 1));
        return new DeityForm("an eldritch reflection of itself", GROUND, 6, 10, parts);
    }

    private DeityForms() {}
}
