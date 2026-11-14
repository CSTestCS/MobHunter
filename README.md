# MobHunter

A NeoForge mod for **Minecraft 26.3**, inspired by the Monster Hunter games. Every creature in the game gets
**Alpha**, **Monstrous** and **Deity** versions. Each one fights with its own moveset of telegraphed attacks.
Hunt them for materials, then forge weapons and armor that carry their element.

* Minecraft 26.3, NeoForge 26.3.0.103 or newer, Java 25
* Build: `./gradlew build`, then use the jar in `build/libs/mobhunter-<version>.jar`

## The three tiers

| Tier | How it appears | Size & power |
|---|---|---|
| **Alpha** | Whenever a group of a creature spawns naturally there is a 6% chance one of them is an Alpha (at most one per pack) | 1.3x size, about 2x health, a reduced moveset of 3 abilities. Alphas of peaceful creatures are territorial: they attack only when you get close or hit them |
| **Monstrous** | Only through a **Hunt Contract** bought from the Hunt Master | Scaled to roughly 6-7 blocks tall, 260-480 HP, armor, boss bar, full moveset, enrages below 40% HP. Comparable to the Wither or Ender Dragon |
| **Deity** | Only through a Deity Hunt Contract. The trail leads to the Deity's own **shrine** | A unique eldritch god for each creature (see below). 2000 HP, darkens the sky, the full moveset plus Deity powers (meteor rain, void pull, eldritch gaze, summoning Alphas) |

All 95 vanilla creatures have a profile with an archetype, an element, a colour, a signature move set, an
entrance and a Deity epithet: for example *The Weaver Below* (spider), *The Hissing Cataclysm* (creeper) and
*The Moon-Eater* (wolf). Creatures added by other mods get a derived profile automatically, so they can still become Alphas.

### Deities: an eldritch god for every creature
A Deity is not a bigger version of its creature. Every creature has its own god, built around the
*concept* of that creature from living copies of creatures and moving architecture of real blocks. Each
moves in its own way: striding, hovering, or rooted to its shrine. A few examples:

* **The Ram God** (sheep): a living thunderhead with a ram's face and curling horns, orbited by four storm clouds
  that call lightning onto marked spots, pelt the area with hail and blast gales. The clouds are breakable parts.
* **The Giant Zombie Piglin** (zombified piglin): the upper half of a colossus emerging from a nether portal that lies
  flat in the ground, swinging a sword built from real gold blocks. It smashes, slices, grabs and throws hunters,
  hurls boulders and calls zombified piglins through the portal.
* **The Mother of Herds** (cow): a cow of impossible size, crowned by a turning wheel of the whole herd.
* **The Thousand-Eyed Hen** (chicken): an egg-shaped shell of chickens around one vast, staring eye.
* **The Moon-Eater** (wolf): a wolf howling beneath the moon it has half devoured, circled by the eternal pack.
* **The Rotting King** (zombie): enthroned atop a pyramid of his own subjects.
* **The Weaver Below** (spider): a spider on legs that span valleys, before a wheel of webs.
* **The World Shell** (turtle): a whole landscape, with trees and stone, rides on its shell.
* **The Hissing Cataclysm** (creeper): a pillar of fused creepers beneath a halo of TNT.
* **The Glutton Below** (pig): half-sunk in a swamp of its own making, ringed by everything it has eaten.
* ...and so on for every creature: the stampede that never ends (horse), the shoal without end (cod), the hive
  ascendant (bee), the deep dark itself (warden), the cauldron mother (witch), the long silence (enderman)...

Every Deity's shrine is built from the materials of its body, and its awakening describes what rises before you.

## Combat: telegraphs instead of run and melee
Tiered creatures never use plain contact damage. Every hit comes from an ability with three phases:

1. **Wind-up**: the creature holds an anticipation pose (rears up, crouches, swells, twists, lowers its head,
   sinks into the ground...). Particle markers show where the attack will land, yellow at first and red just before it fires.
2. **Active**: the attack happens.
3. **Recovery**: a punish window.

The poses are applied on top of every vanilla model, so even a chicken visibly coils before it pounces. There are
about 75 abilities built from reusable families: melee arcs, charges, leaps, ground-slam shockwaves (jump over the
rings), elemental breath cones, projectile volleys, fang/spike lines, swelling novas, blink strikes, burrow
ambushes, dive bombs, roars that stagger, lingering clouds, tongue pulls, tracking gaze beams, void pulls and meteor rains.

## Hunt contracts
1. Craft a **Hunting Board** (`paper, iron, paper / 3 planks / plank _ plank`). Unemployed villagers become **Hunt Masters**.
2. Buy a contract. Monstrous contracts are available from level 1; Deity contracts unlock at levels 4-5.
3. Use the contract. Green **scoutflies** drift toward the first clue, and the action bar shows the distance and direction.
4. Clue 1: tracks or shed material. Clue 2: half-eaten remains. Clue 3: something ominous, such as prey frozen in fear beside a
   pile of corpses. Right-click each clue to read it and send the scoutflies to the next.
5. Disturbing the final clue makes the monster arrive with an entrance unique to its archetype:
   burrowing up like a warden, diving from the sky, leaping in from afar, bursting from a geyser,
   tearing a rift, striking down as lightning, erupting from fire, assembling from bones, or stepping out of fog.
6. For a Deity, the trail ends at its shrine. Using the altar starts a much longer entrance: the sky darkens,
   lightning rings the shrine and the Deity descends or rises.

Sneak and use a contract to abandon your current hunt.

## Materials and gear
Tiered creatures drop **Hide**, **Claw** and **Gem** materials, tinted in the creature's colour and named after its
body type (Carapace, Plume, Scale, Gel, Plating...). Forge gear on a normal crafting table. **All monster materials
in a recipe must come from the same creature and tier.**

| Item | Pattern (H hide, C claw, G gem, S stick, T string) |
|---|---|
| Great Sword | ` CC` / `HGC` / `SH ` |
| Long Sword | ` HC` / ` CH` / `S  ` |
| Hammer | `HCH` / `HCH` / ` S ` |
| Dual Blades | `C C` / `S S` |
| Hunting Bow | ` CT` / `H T` / ` CT` |
| Helm | `HCH` / `H H` |
| Mail | `H H` / `HGH` / `HHH` |
| Greaves | `HCH` / `H H` / `H H` |
| Boots | `C C` / `H H` |

* Stats scale with tier (Alpha, then Monstrous, then Deity).
* **Weapon specials** (use): Great Sword and Hammer charge while held and release a slash or ground pound. Long Sword has
  Spirit Slash (dash cut). Dual Blades have Demon Flurry.
* **Elements**: weapons apply their creature's element on hit (fire, ice, thunder, water, poison, blast, void, wind, earth, sonic, raw).
* **Hunter's Edge**: hunter weapons deal +35% damage to tiered creatures.
* **Armor** resists its creature's element (15% per piece). Every piece also reduces monster ability damage by 4%.
  A full set from one creature unlocks a skill, such as Fire Resistance for a fire set or a chance to blink away from blows for a void set.

## Commands (operators)
* `/mobhunter summon <creature> <alpha|monstrous|deity>`: spawn a tiered creature with its entrance.
* `/mobhunter contract <creature> <monstrous|deity>`: get a contract for a specific quarry.
* `/mobhunter abandon`: abandon your current hunt.
* `/mobhunter selftest <tier> [creature]`: spawn every creature (or one) at a tier next to dummy targets (for testing).
* `/mobhunter huntstep`: debug helper that walks you through your hunt (starts a contract in your inventory, teleports to the next clue, then investigates it).

## Notes
* The Ender Dragon, villagers and wandering traders cannot be targeted by contracts. Villagers and traders are the
  hunters' clients, and the dragon's AI only works in the End. They can still appear through `/mobhunter summon`.
* Aquatic Monstrous and Deity creatures "swim" through the air when they are out of water.
* Monstrous creatures break leaves and plants that block them (respects the `mob_griefing` gamerule).
