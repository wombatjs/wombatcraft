# Wombatcraft Communities

Forge 1.20.1 companion module, packaged into both client and server release overrides.

`gradlew.bat test build` builds a reobfuscated Forge jar and runs the rule tests. Java 17 is required. The wrapper comes from the Forge 47.4.28 MDK. The build pins the currently installed FTB API versions.

Features enabled in the private test server:

- `/faction` opens a server-backed faction screen; FTB's existing team and map screens gain integration points.
- Owner-only Builder role assignments are stored in the existing FTB team extra data.
- Per-chunk private factory, public shop, officer storage and original-FTB presets, bound to claim creation time.
- TPA acceptance receipt/recipient checks retain FTB's teleport warmup and cooldown implementation.
- Immutable arena dimension terrain guards at both world and direct chunk block setters.
- `/wcsmp audit` checks scoped arena editing and terrain protection on the disposable test server.

The release also includes two-stage elections, 50 candidates, mayor policies, spawn residents, durable contract deliveries/payouts, and first-join tutorial arrivals. See the civic and tutorial guides in `../../docs` for commands.

Pack defaults enable the community UI, elections, contracts and TPA guard. Per-chunk policy replacement remains disabled pending two-player acceptance. Duel/exit/logout state machines do not yet have complete gameplay adapters: no player inventory is moved into a logout body, and arena duels are unavailable.

See `../../docs/SMP-IMPLEMENTATION.md` for the acceptance matrix and remaining work.
