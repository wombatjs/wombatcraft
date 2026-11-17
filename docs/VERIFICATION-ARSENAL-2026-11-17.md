# Arsenal acceptance — 2026-11-17

Implemented locally against the existing 344-mod Forge 1.20.1 pack. No production deployment or release was performed.

- Companion module: `gradlew.bat --no-daemon test build` passed; 34 Java unit tests.
- Python suite: all 40 tests passed.
- Quest validation: 3,151 quests, 118 chapters; all identities, dependencies and layout checks passed.
- Full disposable dedicated server: startup, gameplay audit, destination generation, clean shutdown, restart and repeated gameplay audit passed.
- Journey runtime: 75 milestones, 94 links; recommendation simulation reached every milestone without modifying progress during recommendation reads.
- All 16 new Arsenal items registered; equipment recipes loaded. The new Matrix armament recipe passed actual processing and capacity/output checks alongside the other 34 machine recipes.
- Armor runtime: Astralium 24 armor / 12 toughness; Convergium 30 / 16. Repair ingredients, full-set mitigation, partial-set rejection, PvP exclusion, bypass exclusion and fall multipliers passed.
- Abilities: missing charges reject activation; successful use consumes one charge; swapping item copies cannot bypass cooldowns; Greatblade and Aegis grant their documented effects.
- Longbow: arrow base damage multiplier verified as 2.5.
- All ten smithing transformations matched their inputs and preserved custom name, enchantments and existing damage.
- Four explicit Better Combat movesets loaded; Greatblade resolved as two-handed.
- KubeJS concurrency regression: transformed get/forget methods verified synchronized; 16,000 parallel generated-resource reads/invalidation cycles passed on each successful server boot.

The first test run passed gameplay but failed its restart with a KubeJS `GeneratedData` null-byte-array error. Inspection of the pinned jar identified unsynchronized `Lazy.get()` / `forget()` state. A per-instance synchronization coremod was added, preserving all pre-existing coremod registrations. The final complete two-boot run passed with this fix. Original failure evidence is retained locally at `dist/arsenal-failed-restart.log`.

Final generated quest data allows ranged/tool progression directly from the Matrix; melee completion is not required for the Armament objective. The final Arsenal chapter was copied into the disposable server before its successful restart and matches the source file hash.

Limits: no connected-client rendering or human combat playtest was performed. Models reuse Minecraft textures. Balance against every configurable upstream endgame loadout, survival acquisition time and multiplayer combat feel remain player acceptance work. These checks establish functioning equipment and progression, not perfect balance or bespoke visual art.
