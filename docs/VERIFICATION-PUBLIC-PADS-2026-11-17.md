# Public landing pads verification — 2026-11-17

Implemented and built locally. No production deployment, pack publication, account changes or changes to the running player's Minecraft instance were performed.

## Build

- Java 17 / Forge 1.20.1-47.4.28: `gradlew.bat test build` passed.
- All 34 existing Java unit tests passed. New integration behavior is tested by the opt-in runtime fixture, not by those pre-existing rule tests.
- Build dependency pins the exact Refined 2.1.7 Modrinth artifact (`TwZf1vY4`); runtime metadata requires Refined 2.1.7.
- Artifact: `mods/wombatcraft-smp/build/libs/wombatcraft-smp-0.1.0-staging.jar`.
- SHA-256: `c9a7012cec3e8e761badcbe8025cb83195edcdc900239532b5b476af55df6e96`.
- `git diff --check` passed; Python test-driver compilation passed.

## Dedicated-server tests

The disposable full-pack loopback server completed two boots with clean shutdown and persisted ownership/name/visibility verification. A further single boot passed after the pad-facing fix. Logs are under `dist/public-pads-before-orientation-boot-1.log`, `dist/public-pads-before-orientation-boot-2.log`, `dist/public-pads-two-boot-test.log`, `dist/public-pads-boot-1.log` and `dist/public-pads-final-test.log`.

Runtime assertions passed for:

- Hidden-by-default registration, UUID replacement and serialization of metadata/reservations.
- Atomic competing reservations and stale-reservation release.
- Actual Refined shell occupancy and removal, headroom and doorway obstructions.
- The transformed exact-arrival method returning no fallback for blocked pads.
- Forced arrival and direct exterior placement refusing a blocked pad.
- Arrival orientation matching the pad's checked doorway.
- Non-owner editing and outside-TARDIS directory access rejection, invalid/oversized/formatting-code names.
- Block removal unregistering the old pad identity.
- Two real Refined interior capabilities: ship A takes off and reserves; ship B cannot steal the destination; A materializes; B cannot overwrite it; A can depart its own pad; retargeting releases its reservation and the pad becomes available.

The final small change removes entity-overlap checks so the exterior-flight pilot does not obstruct its own landing. It was source-reviewed and rebuilt with all Java tests passing after the last dedicated-server run. The physical block, shell and reservation checks remain unchanged. A human exterior-flight landing has not been tested.

## Client tests

An isolated, minimal offline Forge client loaded the real Refined and Refined Flight integrations and exited cleanly. It used an offline test identity and did not read account tokens or connect to a server.

- The actual transformed `MonitorScreen.createSelectionList` method executed and placed **Public Landing Pads** at index 3, directly after Waypoints. Its constructor's world-dependent shell renderer was bypassed only in this offline fixture.
- Directory, editable settings and read-only settings rendered at 480×360 and 320×240 GUI sizes. All six framebuffer captures were generated; the visual inspection covered the directory at both sizes and compact editable/read-only settings.
- Long names truncate without hiding availability, disabled destinations remain visibly disabled, and compact settings retain the name, visibility, owner, status and save/done controls.
- Captures: `dist/public-pads-client/screenshots/public-pads-0.png` through `public-pads-5.png`.
- Logs: `dist/public-pads-client.log` and `dist/public-pads-preview-test.log`.

The first preview attempt stopped before the fixture ran. The next exposed a fixture-only missing-world error in Refined's monitor constructor; the offline fixture now executes the list method without constructing a world-dependent shell renderer. The succeeding run passed and generated all six captures.

## Remaining acceptance

No human multiplayer session has exercised empty-hand pad settings, networked menu clicks, exterior flight, or reconnect with this build. Moving ship pads are unsupported. The tests establish block/flight behavior and real client rendering, not full player acceptance. The live server and launcher-managed client still require an ordinary coordinated pack release to receive this feature.
