# Convergence verification — 2026-11-17

Historical foundation check. The expanded implementation is covered by [gameplay acceptance](VERIFICATION-CONVERGENCE-GAMEPLAY-2026-11-17.md); its completed stations supersede the remaining-work list below.

Local Wombatcraft source changes; no release or public deployment.

## Completed checks

- Companion Forge module: Java 17 build/reobfuscation passed, with all 34 existing Java rule tests passing.
- Python suite: 31 tests passed, including five new progression tests for generated data, reachability, branch requirements, machine capacity and item references.
- Quest validation: 3,106 quests across 114 chapters; unique IDs, valid dependencies, acyclic graph and non-overlapping quest positions.
- Dedicated-server first startup: all 25 progression recipes and eleven KubeJS materials loaded; Java ore/crystal/equipment references resolved through the quest registry check.
- Real Multiblocked2 handlers processed all 16 new Forge recipes plus the three existing recipes. Checks covered exact outputs, consumed inputs, FE and mana debits, refusal without power, and actual item stack limits.
- Server-side armor handler: full set changed a 10-point magic event to 8; incomplete set, generic damage and player-attributed indirect magic remained 10. These checks use two simulated server-player objects, not connected clients or a complete combat encounter.
- Existing three repeatable Trial gateways spawned waves and produced crests. Existing dimension-generation checks passed.
- Clean restart passed with the same item/quest/recipe registries, all 19 Forge processing checks and the four armor-handler cases. The four disabled utility recipes remained absent on both boots after switching removal to exact IDs.

## Bugs found during validation

The industrial Temporal Core initially requested 32 ender pearls in one slot. The real machine test rejected it; the recipe now uses 16. Warp stone outputs were constrained to one item per stack. Astralium's utility recipe was changed to renewable elytra, and Primal production now supplies supremium farmland.

A restart hung while the existing `wombatcraft_utilities.js` output filter instantiated unrelated Quark smithing rune recipes concurrently. Thread dumps showed worker threads spinning in Universal Enchants' category map while the main thread waited for KubeJS recipe removal. The four intended removals now use exact recipe IDs verified against the pinned jars. Runtime validation checks that those four recipes remain absent.

An earlier attempt also hit a KubeJS generated-data null-buffer failure during datapack loading. The smoke runner now recognizes that startup failure promptly. Successful subsequent startup is not proof that every upstream intermittent race has been eliminated.

## Evidence

- `dist/convergence-java-build.log`
- `dist/convergence-tests.log`
- `dist/convergence-smoke-final.log`
- `dist/server-smoke/console-first-start.log` and `console.log`
- `dist/server-validation.json`

These are local generated logs, not committed release artifacts.

## Still required before production acceptance

Connected-client tests for shared FTB quest completion, team changes, item trading, reconnect persistence, JEI display, automation and actual combat. Natural ore generation, Silk Touch/Fortune and client rendering need in-game checks. Textures remain vanilla placeholders. The Arcane Infuser, Dimensional Stabilizer and Research Terminal are design work only; this implementation uses the existing Forge and quest book.
