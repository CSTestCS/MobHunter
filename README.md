# MobHunter

A NeoForge mod for **Minecraft 26.3**, inspired by the Monster Hunter games and built to the *Alpha, Monstrous
and Deity Bestiary*. Every one of the 88 creatures in the current roster has an **Alpha**, a **Monstrous** and a
**Deity** form, each with its own custom model, its own telegraphed moveset, and its own Blade, Spear and Plate armor.

* Minecraft 26.3, NeoForge 26.3.0.103 or newer, Java 25
* Download: `dist/mobhunter-1.0.0.jar`. Build it yourself with `./gradlew build` (output in `build/libs/`).

## The bestiary
Each creature is defined by the bestiary's **material** (horn, fleece, silk, prismarine, ...), its **element**
(verdant, storm, web, beam, ... 62 elements in all), the **title of its Monstrous form** and the **name of its Deity**:

| Creature | Material | Element | Monstrous form | Deity |
|---|---|---|---|---|
| Cow | horn | verdant | Bull | Bull Deity |
| Sheep | fleece | storm | Ram | Ram Deity |
| Spider | silk | web | World Spider | World Spider Deity |
| Zombified Piglin | rotgold | netherfire | Portal King | Portal King Deity |
| Warden | sculk | resonance | Deepdark Sovereign | Deepdark Sovereign Deity |
| ... | | | | *all 88 creatures* |

Each element belongs to a mechanical family (fire, ice, thunder, water, poison, blast, void, wind, earth, sonic, raw)
that drives the status it inflicts, the resistances of its armor and its full-set skill.

## Tiers and their bodies
Every tier has a **custom model**: generated anatomy is grown onto the creature's skeleton and moves with every
limb. Each creature has its own anatomy drawn from its bestiary material: horns, tusks, spines, plating, crystal
growths, ribs, crests, fins, fungus, foliage, flame vents, claws, manes, shells, crowns, stingers, wings,
tendrils and halos.

| Tier | Body | How it appears |
|---|---|---|
| **Alpha** | Only modestly stronger: thicker reinforced material, brighter markings and a modest version of its defining feature, at 1.3x size | 6% of natural group spawns include one (at most one per pack) |
| **Monstrous** | A hunting-beast evolution: heavily exaggerated structures and a clearly visible glowing core. About 6-7 blocks tall, 260-480 HP, boss bar, enrages below 40% | Only through a **Hunt Contract** |
| **Deity** | A gigantic, fully physical incarnation: an *enormous* defining feature, a blazing heart, a silhouette that still identifies the creature. 15+ blocks tall, up to 2000 HP | Only through a Deity Hunt Contract, at the end of a trail leading to its own **shrine** |

### Special Deities
* **Ram Deity** (sheep): a living thunderhead with a gigantic ram head and curled horns that rolls and surges across
  the sky. Five weapon-clouds orbit it: the **Lightning Cloud** fires controlled bolts (the horns visibly gather charge
  first), the **Hail Cloud** launches dense hail, the **Wind Cloud** carves cutting gusts, the **Thunder Cloud**
  releases sonic shockwaves, and the **Tornado Cloud** lowers a rotating column that drags hunters around. Each cloud
  can be destroyed, which removes its attack.
* **Portal King Deity** (zombified piglin): a gigantic undead piglin halfway out of a huge Nether portal lying flat
  in the ground. The portal pulses purple-red. It carries a colossal chipped sword of real gold blocks in both hands.
  Its strikes send out burning Nether shockwaves, and Nether fire, smoke and gold fragments cling to its corpse. It
  slices, grabs and throws hunters, hurls boulders and calls zombified piglins through the portal.

## Combat: telegraphs instead of run and melee
Tiered creatures never use plain contact damage. Every hit comes from an ability with a **wind-up**, an **active**
phase and a **recovery** phase:

* **Wind-up:** the creature holds an anticipation pose (rears up, crouches, swells, twists, lowers its head, sinks into
  the ground...) while ground markers show where the attack will land, yellow at first and red just before it fires.
* **Active:** the attack happens.
* **Recovery:** a punish window.

There are about 80 abilities: charges, leaps, shockwaves you jump over, breath cones, volleys, spike lines, novas,
blinks, burrow ambushes, dive bombs, roars, clouds, tongue pulls, gaze beams, void pulls and meteor rains.

## Hunt contracts
1. Craft a **Hunting Board** (`paper, iron, paper / 3 planks / plank _ plank`). Unemployed villagers become **Hunt Masters**.
2. Buy a contract. Monstrous contracts are available from level 1; Deity contracts unlock at levels 4-5.
3. Use the contract. Green **scoutflies** lead you to the first clue, and the action bar shows distance and direction.
4. The clues escalate: tracks or shed material, then half-eaten remains, then something ominous (frozen prey beside a
   pile of corpses). Right-click each clue to continue the trail.
5. Disturbing the final clue makes the Monstrous creature arrive with an entrance suited to it: burrowing up, diving
   from the sky, leaping in, bursting from a geyser, tearing a rift, striking as lightning, erupting from fire,
   assembling, or stepping from fog.
6. A Deity's trail ends at its **shrine**. Using the altar begins a much longer entrance.

Sneak and use a contract to abandon your hunt.

## Gear (crafting philosophy)
Slain tiered creatures are carved for three kinds of parts, named for their tier:

| Tier | Example parts (cow) |
|---|---|
| Alpha | Alpha Cow Horn Hide, Alpha Horn Fragment, Alpha Cow Core Shard |
| Monstrous | Bull Horn Hide, Bull Horn, Bull Core |
| Deity | Bull Deity Divine Horn Hide, Bull Deity Horn, Bull Deity Heart-Core |

Forge them on a crafting table. **All parts in a recipe must come from the same creature and tier.**

| Gear | Pattern (H hide, C horn/claw part, G core, S stick) | Example names |
|---|---|---|
| **Blade** | `C` / `C` / `S` | Alpha Hornblade, Bull Hornblade, Bull Deity Hornblade |
| **Spear** | `  C` / ` S ` / `S  ` | Alpha Horn-Spear, Bull Horn-Spear, Bull Deity Horn-Spear |
| **Plate Helm** | `HHH` / `H H` | Bull Hornplate Helm |
| **Plate Mail** | `H H` / `HGH` / `HHH` | Bull Hornplate Mail |
| **Plate Greaves** | `HHH` / `H H` / `H H` | Bull Hornplate Greaves |
| **Plate Boots** | `H H` / `H H` | Bull Hornplate Boots |

* Every creature's gear has its own art at every tier, shaped by its material and coloured by the creature.
  Its Plate armor has its own armor texture, and growths of the creature's anatomy appear on the wearer (horns or a
  crest on the helm, spines or crystals on the mail, claws on the boots).
* **Blade:** hold use to gather the element, release for an elemental slash. **Spear:** the 26.3 spear lunge with
  reach, and hits release a secondary burst of the element.
* Weapons deal +35% damage to tiered creatures. Armor resists its element (15% per piece), and a full set from one
  creature grants a skill.

## Commands (operators)
* `/mobhunter summon <creature> <alpha|monstrous|deity>`: spawn a tiered creature with its entrance.
* `/mobhunter contract <creature> <monstrous|deity>`: get a contract for a specific quarry.
* `/mobhunter abandon`: abandon your current hunt.
* `/mobhunter selftest <tier> [creature]`: spawn creatures next to dummy targets (testing).
* `/mobhunter gearstand <creature> <tier>`: a mannequin wearing that creature's full set, with its gear on display.
* `/mobhunter huntstep`: debug helper that walks you through your hunt.

## Notes
* The Ender Dragon, villagers and wandering traders are not contract targets. They can still appear through
  `/mobhunter summon`.
* Aquatic Monstrous and Deity creatures swim through the air when out of water.
* Monstrous creatures break leaves and plants that block them (respects `mob_griefing`). Monstrous and Deity undead
  do not burn in daylight.
