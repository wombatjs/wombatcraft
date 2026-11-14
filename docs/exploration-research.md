# Wombatcraft exploration review

Researched 13 November 2026 for Forge 1.20.1. These are recommendations, not newly installed mods. File compatibility is not a successful multiplayer test.

## Recommendation

Start with **Journey Into the Light** as the candidate for a substantial new campaign, **Deep Aether** to enrich a world already in the pack, and **Dimensional Dungeons** for repeatable group expeditions. Test them individually before combining them. Keep DivineRPG as an alternative campaign rather than adding both large RPG systems at once.

| Candidate | Reason to explore | Verified version evidence | Integration assessment |
|---|---|---|---|
| Journey Into the Light | Its project advertises eight dimensions, over 100 mobs, dungeons and a large equipment progression. Best candidate here for the requested long adventure arc. | [Forge 1.20.1 build 2.1.7](https://www.curseforge.com/minecraft/mc-mods/journey-into-the-light-mod/files/5724279); [project](https://www.curseforge.com/minecraft/mc-mods/journey-into-the-light-mod) | Validate the actual 1.20.1 dimension/boss roster before authoring its campaign. Test weapons and armour against Wombatcraft's magic and late technology; advertised content alone does not establish balance. |
| Deep Aether | Expands the existing Aether with biomes, Skyjade and Stratus equipment, and additional discoveries. Stratus offers movement benefits, giving the material hunt a practical reward. | [1.20.1 build 1.1.7](https://www.curseforge.com/minecraft/mc-mods/deep-aether/files/6839617); [features and dependencies](https://www.curseforge.com/minecraft/mc-mods/deep-aether) | It enriches the Aether rather than adding another separate dimension. Requires Aeroblender; new world generation needs unexplored chunks. Verify compatibility with the installed Aether version. |
| Dimensional Dungeons | Renewable generated dungeons with combat, puzzles and loot. Restricted building prevents many simple shortcuts. | [Forge 1.20.1 build 202](https://www.curseforge.com/minecraft/mc-mods/dimensional-dungeons/files/6420829); [gameplay description](https://www.curseforge.com/minecraft/mc-mods/dimensional-dungeons) | Strong multiplayer expedition candidate. Check flight, teleport spells, TARDIS travel and claims against dungeon restrictions. Tune loot to make repeated expeditions worthwhile without handing out factory progression. This is one dungeon dimension, not many distinct worlds. |
| DivineRPG | Project advertises eight worlds and a broad mob, boss and equipment progression. Another substantial campaign option. | [Forge-tagged 1.10.7.5 in the 1.20.1 file list](https://www.curseforge.com/minecraft/mc-mods/official-divinerpg/files/all?page=1&pageSize=20&version=1.20.1); [project](https://www.curseforge.com/minecraft/mc-mods/official-divinerpg) | Larger balance commitment. Do not assume the newer NeoForge-labelled file is the right Forge build. Compare this against JITL in isolated tests before choosing the campaign. |
| Eternal Starlight Forge | A visually distinct realm with a Gatekeeper entrance and a guided boss hunt using the Seeking Eye and Starlight Story. | [Forge 1.20.1 backport, 0.9.0 hotfix](https://www.curseforge.com/minecraft/mc-mods/eternal-starlight-forge) | Promising later addition. The backport author explicitly warns that bugs and crashes can occur; given this pack's performance history, it needs focused generation, combat and multiplayer travel testing first. |

## What was already installed

The audited client jars contain **38 data-defined dimensions**, including the three vanilla worlds, six orbital spaces, pocket realms and one internal Graveyard world. That is not 38 fully developed adventure campaigns. The atlas also covers runtime-created TARDIS interiors and AE2 spatial storage. TARDIS interiors have individual identifiers; AE2's code defines `ae2:spatial_storage`, rather than supplying another dimension JSON in this jar.

Aether, Blue Skies, Twilight Forest, Undergarden, Deeper and Darker, Bumblezone, Ad Astra and Stargate Journey were already present. Alex's Caves, Cataclysm, When Dungeons Arise, Incendium and Nullscape also already expand exploration; adding them again would not solve the discovery problem. Stargate worlds are useful destinations and outpost sites, but this review does not claim every one has a unique boss campaign.

The exact inventory is in `quests/dimension-inventory.json`; the player-facing destination routes are in `docs/dimension-atlas.json`.

## Implemented quest organisation

- Added **Dimensions**, an atlas of all 38 data-defined worlds plus TARDIS interiors and AE2 spatial storage, and distinct expedition chapters.
- Preserved all 1,968 existing quest IDs, including the survey quests moved out of Exploration.
- Added 148 entries: 40 field-guide entries and 108 arrival, advancement or expedition objectives. The book now contains 2,116 quests across 54 chapters.
- Adventure branches use actual arrival and advancement detection for bosses, materials, discovery, royal trading and equipment progression. Optional station/outpost commissioning is explicitly self-recorded and gives no reward.
- Rewards support further travel with food and experience; bosses still supply their own exclusive loot. No farmable repeatable quest rewards or free progression-skipping machinery were added.
- Orbit and pocket-space entries are clearly distinguished from adventure worlds. The Graveyard's `past` definition is documented without an impossible mandatory arrival task. Existing vanilla, magic and technology chapters remain available for their main progression.

## Giving industry a purpose

Current concrete links include Ad Astra's rocket/material chain, oxygenated orbital stations, supplied Stargate outposts, Bumblezone's long royal trading chain, and Blood Magic's dungeon materials. These connect expeditions to fuel, power, farming and logistics without rewriting existing essential recipes.

For a future campaign integration, make factory output useful through expedition consumables and optional commissions, then turn earned boss materials into useful upgrades. Test damage and travel bypasses with midgame and late-game equipment. A long list of portals alone will not provide that progression.

## Validation scope

Local validation checks quest IDs, dependencies, positions and item references. Release validation boots a disposable dedicated server twice, verifies loaded quest IDs and item/recipe registries, and checks new expedition advancement/dimension targets against the exact release jars. Neither is a survival playthrough of every quest or a performance benchmark of the recommended mods.
