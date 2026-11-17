# Personal progression and menu separation — 2026-11-17

Implemented locally; not deployed or released. Behavior and migration details are in [Factions and individual progression](FACTIONS-AND-PERSONAL-PROGRESSION.md).

Validation:

- Gradle `test build`: passed; 34 Java unit tests.
- Python suite: 40 tests passed, including preservation of every existing companion compatibility/protection registration.
- Quest validation: 3,142 quests across 117 chapters, all link targets valid, no dependency cycles or duplicate IDs.
- Dedicated-server startup and clean restart both passed with the pinned pack. Both emitted `PERSONAL_PROGRESSION_AUDIT_COMPLETE isolated=true faction_switch=true login=true migration=true menus=true solo_contracts=true`.
- Two synthetic players in the same faction resolve to distinct personal quest records. Completing an objective does not complete it for the other player. Leaving/rejoining and the FTB team-change callback keep the same personal record.
- Actual FTB inventory detection credited the carrying player's Resonant Alloy task and left the other player's task at zero. The fixture resets both task progress and completion flags so this check remains meaningful after restart.
- Migration retained an old completion and an existing reward claim; running it again produced identical serialized progress. The legacy source record remains intact.
- Personal research stayed isolated within a faction and survived serialization and restart (`restored=true`).
- Faction, contract and town-hall snapshots contain only their own feature data. Menu packet serialization round-tripped correctly; a solo player could read orders.
- Existing room/claim guards, utility guards, portal policy, quest links, all 47 Convergence progression recipes, four stations and Trials passed the full smoke suite.

During implementation, a manifest replacement temporarily omitted existing coremods. The original entries were restored, the redundant compatibility patch was removed, and a regression test now checks registration of every companion coremod. The final smoke runs include all existing protection patches.

Evidence: `dist/personal-build.log`, `dist/personal-smoke.log`, `dist/server-smoke/console-first-start.log`, `dist/server-smoke/console.log`, and `dist/server-validation.json`.

The tests use a disposable cached world and synthetic players. Client button layout, clicking, live invitations and a full multiplayer playthrough remain unverified. Legacy faction-owned task screens need replacement to bind to an individual player; migration does not guess which member should own a shared physical screen.
