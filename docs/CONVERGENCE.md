# Wombatcraft Convergence gameplay

Convergence is Wombatcraft's pack-specific progression backbone. It uses the existing Forge 1.20.1 companion module, KubeJS, Multiblocked2, Botania mana and FTB Teams/Quests. It is not intended to run independently of this pack.

## The complete loop

1. Establish a Create workshop and discover amethyst. Craft Resonant Alloy, a Research Terminal and optionally a Resonant Prospector.
2. Specialize in industry, magic, space, adventure, nature or dimensions. Craft and trade the six cores. No personal recipe lock prevents a specialist from supplying another player.
3. Build the Convergence Forge and combine arcane and dimensional discoveries into Aetherium. Build the Arcane Infuser for catalysts, equipment supplies, Living Relics and hybrid production.
4. Combine space exploration with industry to make Astralium. Build the Dimensional Stabilizer for flux, renewable planetary metals, dimensional cores and elytra.
5. Feed flux back into industry. Accelerated recipes retain their normal yield and raw inputs, run four times faster, and consume twice the direct FE/mana plus flux. They run in different stations from their ordinary counterparts so a machine cannot choose the wrong recipe.
6. Combine all six disciplines into Convergium. Finish the existing four advanced sigils and craft the Convergence Core without consuming a dragon egg.
7. Spend the Core on a Convergence Matrix. This produces post-mastery polonium, Gaia ingots, awakened essence and Nether Stars while continually consuming specialist cores, catalysts, flux and ordinary resources.
8. Forge Convergent Armaments in the Matrix and upgrade your personal weapons, armor and tools. See [the Arsenal](CONVERGENCE-ARSENAL.md) for equipment statistics, abilities and recipes.

The first Matrix still requires the pack's original antimatter, advanced magic/boss, planetary and awakened-agriculture milestones. Matrix production replaces repeated late-game grind after that investment. It does not erase initial progression. Additional matrices remain expensive cooperative projects.

## Branches and recurring trade

| Branch | Initial evidence | Recurring products and customers |
| --- | --- | --- |
| Industry | Create precision mechanism, Mekanism advanced circuit, AE2 engineering processor | Mechanized Cores for processors, Astralium and Matrix polonium |
| Magic | Botania mana diamond/manasteel and Ars source gem | Arcane Cores and Infused Catalysts for metal processing, equipment supplies and Matrix Gaia ingots |
| Space | Ad Astra desh, ostrum and calorite | Stellar Cores for Astralium, planetary-metal reconstitution, flux and Matrix expeditions |
| Adventure | Repeatable Thornwood Trial and Wither | Ancient Cores for beacons, Living Relics and Matrix supplies |
| Nature | Mystical Agriculture supremium, honey and crops | Primal Cores for catalysts, farmland, essence production, Living Relics and Matrix agriculture |
| Dimensions | Repeatable High Sky and Echo Trials | Temporal Cores and Stabilized Flux for travel, overclocking and Matrix supplies |

TARDIS Refined remains an optional transport system. No ship is consumed, ownership is changed, or ship API is hooked. Traded discoveries are valid. The terminal records possessed evidence; it does not claim to prove where a player traveled.

## Stations

All four processing stations use the existing six-block wall shape. Viewed from the front:

```
CAP    FRAME       CAP
FRAME  CONTROLLER  FRAME
```

| Station | Frame / cap blocks | Unlock and purpose |
| --- | --- | --- |
| Convergence Forge | Brass / crying obsidian | Existing controller recipe; foundational alloy tiers, basic industry and original Trial recipes |
| Arcane Infuser | Manasteel / amethyst | Aetherium controller; magic/nature catalysts, Living Relics, mana metals, farmland, essence, field charges and accelerated processors |
| Dimensional Stabilizer | Ostrum / crying obsidian | Astralium controller; dimensional cores, flux, planetary metals, elytra, warp stones and accelerated Astralium |
| Convergence Matrix | Netherite / beacon | Convergence Core controller; four post-mastery production routes |

Each wall needs three frame blocks and two caps. Beacon caps do not require a beacon pyramid. All processing stations require both FE and Botania mana. Aim a Mana Spreader at the controller. Each controller stores up to 4,000,000 FE and 1,000,000 mana, with six ingredient slots and two product slots. Pipes can insert inputs and extract products; full outputs prevent starting a batch without consuming ingredients or power. Recipe displays use the corresponding station recipe types in JEI.

The Research Terminal is a separate, single-block station. It does not require power or a block entity containing unique player data.

## Materials and production budgets

| Tier | Normal batch | FE / mana | Time |
| --- | --- | --- | --- |
| Resonant Alloy | 4 from andesite alloy, copper, redstone and amethyst; 8 with Resonant Crystal instead of amethyst | Crafting | Instant |
| Aetherium | 4 from four Resonant Alloy plus Arcane and Temporal Cores | 200,000 / 12,000 | 10s |
| Astralium | 4 from four Aetherium plus Stellar and Mechanized Cores | 600,000 / 30,000 | 15s |
| Convergium | 4 from four Astralium, a Living Relic and Mechanized/Arcane/Stellar/Temporal Cores | 2,000,000 / 120,000 | 30s |

A Living Relic packages Ancient and Primal Cores into one slot. This keeps the final alloy within six inputs without omitting a discipline.

Eight Infused Catalysts cost one Arcane Core, one Primal Core, eight source gems, 400,000 FE and 24,000 mana. Eight Stabilized Flux cost Temporal and Stellar Cores, eight pearls, 800,000 FE and 48,000 mana. These are consumed per batch, not permanent speed tokens.

One flux accelerates eight engineering processors from 200 to 50 ticks in the Infuser, or four Astralium from 300 to 75 ticks in the Stabilizer. Direct energy and mana double. Factories trade ongoing operating cost for throughput rather than multiplying outputs for free.

Each Matrix batch takes 40 seconds and 3,000,000 FE. Gaia ingots cost 300,000 mana; the other three batches cost 180,000 mana. All consume specialist inputs. Resource conversion is intentional, but no recipe generates materials without inputs. Server-economy balance still needs real survival data; machine correctness alone does not establish perfect balance across 344 mods.

## Equipment and exploration

- **Resonant Prospector:** scans an eight-block cube for Resonant Ore in already loaded terrain. Five-second cooldown, 128 uses. Reports deposit coordinates. It neither generates chunks nor breaks terrain.
- **Aetherium armor:** diamond defense, durability, enchantability and diamond repair. All four pieces reduce tagged non-player magic damage by 20%. The tag covers 24 explicit damage types from vanilla, Ars Nouveau, Iron's Spellbooks, Mana and Artifice and selected Cataclysm attacks. Player-attributed damage, Blood Magic sacrifice/ritual costs, Botania relic penalties and Heartstop are excluded.
- **Astralium Boots:** four armor and three toughness; halve the fall-damage multiplier. They now belong to the full Astralium expedition set and repair with Astralium. Swapping them into an Aetherium set sacrifices that set's arcane bonus.
- **Expedition Field Projector:** consumes one carried Field Charge for three minutes of water breathing and night vision. Ten-second cooldown. No free effect without a charge in survival. It does not replace Ad Astra oxygen equipment or grant flight or invulnerability.
- **Renewable elytra:** four Astralium, a Stellar Core, sixteen phantom membranes and thirty-two feathers in the Stabilizer. This also supplies the legacy Voyager Sigil without exhausting End ships.

Resonant Ore generates in new eligible Overworld chunks between Y 0 and 80, with four attempts at veins up to four blocks. Terrain determines actual placement. Iron-tier tools are required. Normal drops, Silk Touch and Fortune are supported. The amethyst route remains valid in established worlds; Infuser recrystallization provides a renewable crystal source later.

## Individual research and quests

The terminal has fourteen discoveries, live server-synchronized status and production-route tooltips. Press **Record carried discoveries** to record items in your inventory. Items are not consumed and no items or currency are awarded. Renamed trade goods are accepted by item identity.

Knowledge is stored in your personal FTB player record and saved by FTB. Faction membership does not share or reset research. Existing members receive a one-time copy of legacy shared discoveries during migration. Destroying or replacing a terminal does not destroy research. Recording is idempotent. The server validates the actor, button, distance, original dimension and terminal block on every action.

The terminal ledger supplements FTB Quests; it is not a second roster or a claim that quest completion and terminal registration are the same event. Recipes use physical ingredient/controller gates and remain available for trade without private research restrictions.

The three Convergence chapters contain 42 quests: 13 progression milestones, 20 workshop/expedition objectives and nine Arsenal objectives. They are non-consuming, have stable IDs, and have no material rewards. The general reward compiler preserves that policy. Including the navigation chapters, the full pack now has 3,151 quests in 118 chapters.

## Implementation and rebuild

There are 48 progression recipes: 16 crafting recipes and 32 processing recipes across four stations. The original three Forge recipes remain, giving 35 machine recipes under runtime audit. The Arsenal also adds 15 equipment crafting/smithing recipes in the companion mod. There are fourteen KubeJS materials/components, plus Java ore, crystal, armor, tools, armament and terminal content.

- `tools/build_convergence_progression.py`: items, base recipes, equipment data, chapters and generated machine audit.
- `tools/convergence_gameplay.py`: station definitions, extended production routes and workshop chapter.
- `mods/wombatcraft-smp/.../ConvergenceContent.java`, `ConvergenceTools.java`, `ConvergenceResearch.java`: registered content, server-side utility behavior and personal ledger.
- `mods/wombatcraft-smp/.../client/ResearchScreen.java`: terminal display; changes require a rebuilt client jar.

Run `python tools/build_convergence_progression.py`, the companion module's `gradlew.bat --no-daemon test build`, `python -m unittest discover -s tests -v`, `python tools/validate.py`, then `python tools/smoke_server.py`. The standalone machine builder also regenerates all four stations.

The runtime audit uses a disposable server and simulated server players. It checks actual recipe handlers, stack limits, blocked outputs, pipe directions, research sharing/isolation/save/restart, terminal action validation, charged abilities, cooldowns, armor, ore loot and the existing Trials. It does not substitute for connected-client UI, combat, automation-network or survival playtesting.

## Release boundary

See [gameplay acceptance results](VERIFICATION-CONVERGENCE-GAMEPLAY-2026-11-17.md) for verified behavior and the remaining client/playtest limits.

No live deployment or public release is performed by this change. Both client and server need the updated companion jar, startup scripts, machine definitions and recipe data; `/reload` alone is insufficient. Existing item and quest IDs are retained. Several existing prototype recipes now belong to the Infuser or Stabilizer, so old Forge factories must move those jobs. Drain in-flight machines and back up before upgrading an established world.

Models and icons reuse existing Minecraft/mod textures. Client rendering, natural ore discovery, actual FTB invite/join/leave flows with connected players, and long-term economy balance still require in-game acceptance. The Java team fixture exercises effective-team changes and persistence, not the full invitation UI.
