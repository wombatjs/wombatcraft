# Dimension expansion

This branch stages all fourteen requested exploration additions for Forge 1.20.1. Publication is gated on combined-pack validation; a file being present does not establish compatibility.

| Addition | Pinned file | Expedition quests |
|---|---|---|
| Journey Into the Light [JITL] | JITL - 1.20.1 - 2.2.4.jar | 84 |
| Deep Aether | deep_aether-1.20.1-1.1.7.jar | 20 |
| Dimensional Dungeons | dimdungeons-207-neoforge-1.20.1.jar | 14 |
| DivineRPG (Official) | divinerpg-1.10.7.5.jar | 85 |
| Eternal Starlight Forge | EternalStarlight-0.9.0-hotfix.jar | 63 |
| The Deep Void | the_deep_void-1.99.5-forge-1.20.1.jar | 64 |
| RFTools Dimensions | rftoolsdim-1.20-11.0.11.jar | 25 |
| The Aurorian Reborn | theaurorian-1.0.1-fix.jar | 17 |
| Atlantis | atlantis-9.29.2026-1.20.1-10.1.4-forge.jar | 33 |
| The Lost Cities | lostcities-1.20-7.5.7.jar | 16 |
| Voidscape | Voidscape-1.20.1-1.5.389.jar | 19 |
| Call from the depths - Sculk Dimension | callfromthedepth_-1.22.1-forge-1.20.1.jar | 32 |
| Tropicraft | Tropicraft-9.6.3.jar | 26 |
| Веуоnd thе Еnd | beyond_the_end-1.0.2-forge-1.20.1.jar | 13 |

Aeroblender 1.0.1 is included as the Deep Aether dependency. Exact official download URLs and SHA-512 hashes are in mod-lock.json.

## Quest design

- 511 new expedition quests in 26 chapters, plus 29 new atlas entries. Total: 2,656 quests in 80 chapters.
- All 2,116 previously published quest IDs are retained. The destination inventory now contains 66 data-defined worlds; pocket spaces and internal worlds are distinguished from adventure campaigns.
- The Dimensions group contains arrival, equipment, guardian, discovery and settlement routes. JITL has a departure chapter and one route per realm. DivineRPG separates its major campaigns. Deep Aether extends the existing Aether.
- Advancement objectives follow triggers in the pinned jars. JITL uses item, structure and boss-kill tasks because its jar does not provide a gameplay advancement tree.
- Branch maps use bounded-width layers, distinctive arrival/boss shapes, and chapter-specific item icons.
- Rewards are one-time experience and expedition supplies. Guardian challenges also award two diamonds. Optional self-recorded building projects have no rewards.
- Existing recipes remain available. Factory links include RFTools power reserves and world maintenance, underwater mining and Orichalcum processing, expedition provisions, and reclaimed city workshops.
- Existing terrain is preserved. New Aether features require unexplored chunks; no world reset is part of this expansion.

## Validation

Aurorian Reborn 1.0.1-fix requires two compatibility corrections: its dedicated-server channel references a handler removed by its client-only annotation, and its sickles return null instead of an empty crafting remainder. Small source-controlled Forge patches correct these without modifying the upstream jar. Packet IDs and codecs are preserved; the client retains its normal boss-bar handler. The crafting correction ships to both client and server.

Static graph and layout checks and the local test suite pass. Dedicated-server validation checks real item/entity registries, quest IDs and recipes, boots twice, and generates a chunk in each new static destination. Client boot and in-game checks are separate acceptance steps. A survival playthrough of all campaigns is not claimed.
