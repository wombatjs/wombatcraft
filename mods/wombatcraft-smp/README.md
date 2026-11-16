# Wombatcraft Communities (staging)

Forge 1.20.1 companion module. It is intentionally absent from the production pack manifest.

`gradlew.bat test build` builds a reobfuscated Forge jar and runs the rule tests. Java 17 is required. The wrapper comes from the Forge 47.4.28 MDK. The build pins the currently installed FTB API versions.

Features enabled in the private test server:

- `/faction` opens a server-backed faction screen; FTB's existing team and map screens gain integration points.
- Owner-only Builder role assignments are stored in the existing FTB team extra data.
- Per-chunk private factory, public shop, officer storage and original-FTB presets, bound to claim creation time.
- TPA acceptance receipt/recipient checks retain FTB's teleport warmup and cooldown implementation.
- Immutable arena dimension terrain guards at both world and direct chunk block setters.
- `/wcsmp audit` checks scoped arena editing and terrain protection on the disposable test server.

Implemented but not activated: election persistence and ballots; contract reservation rules and content catalog; duel/exit/logout rule state machines. These do not yet constitute complete economy or combat-log integrations. In particular, no player inventory is moved into a logout body by this version.

Default server configuration is disabled. `chunkPolicies` and `elections` have independent gates. Do not enable elections before implementing and verifying policy effects. Do not publish the jar as a complete faction/arena release.

See `../../docs/SMP-IMPLEMENTATION.md` for the acceptance matrix and remaining work.
