# Village population — 2026-11-17

The user reported villages that are empty on arrival and villages losing residents, and explicitly requested no testing. This change has not been compiled, tested, released or deployed. Investigation used only existing configuration, pinned mod classes/templates, logs and a read-only copy of saved Overworld region/entity files.

## Findings and uncertainty

- Production has `spawn-npcs=true`. ServerCore dynamic population adjustment, entity activation optimization and breeding caps are disabled.
- The saved Overworld snapshot contains 147 vanilla villagers, 14 iron golems and 52 guards. Several fully generated villages have substantial populations (29–45 residents in or near their boundaries). Therefore a pack-wide failure to generate villagers is not supported by the snapshot.
- Many zero-population structure records are only structure starts in ungenerated chunks. They cannot be counted as empty, visited villages. The CTOV and Towns and Towers starts examined contain vanilla villager template pieces.
- Guard Villagers' effective configuration permits guard arrows to hurt villagers, guards and iron golems. Patrols are disabled. These are confirmed settings that can weaken village survival, not proof of how every missing villager died.
- No coordinates identifying a particular empty village were supplied. The precise original cause for each observed village remains unconfirmed; this adds population recovery rather than claiming a universal generation bug was found.

The local saved-world inspection files are under ignored `dist/village-audit/`. They are diagnostic snapshots, not world modifications or gameplay tests.

## Changes

`pack/config/guardvillagers-common.toml` disables friendly-arrow damage and enables regular guard patrols. Other settings retain the mod's defaults.

`VillagePopulation` adds gradual immigration to actual generated villages recognized by the vanilla village structure tag, or CTOV / Towns and Towers village IDs. It supports newly visited and previously generated villages without resetting their terrain.

- At most two arrivals per village per minute of server ticks, and eight per 24,000 game ticks.
- Target population is the smaller of the actual bed count and `villageResidentTarget` (default 24). Existing larger populations are not culled.
- The whole structure plus a 16-block margin must already have loaded chunks and loaded entities. The code reads resident structure-start/reference data and does not force-load or generate terrain.
- A non-spectating player must be visiting; arrivals occur only during daytime with `doMobSpawning` enabled.
- The village must have at least two beds and a vacant bed with a safe nearby landing spot. New residents have ordinary AI, biome-appropriate appearance and persistence; no preset trades or professions are imposed.
- Existing villagers retain names, professions, trades and homes. Zombie villagers count toward population so recoverable residents are not immediately replaced.
- No arrivals during an active raid, in zombie/abandoned template villages, into claimed beds/landing chunks, around nearby monsters or onto detected hazards. Oversized structures above 256 blocks on either horizontal axis are skipped to bound work.
- Arrival budgets and cooldowns are saved per dimension and structure start; visiting with multiple players or restarting does not reset the daily budget.
- Arrivals are logged with dimension, village coordinates and population change.

This is independent of faction progression. Player-built breeding/trading halls outside generated village structures do not trigger immigration. Claimed natural villages are not automatically restocked into claimed homes.

`villageSettlers` defaults to true in the companion server config, including when Forge supplies missing keys to an existing world's config. Setting it false disables immigration. These defaults are also in `pack/defaultconfigs/wombatcraft_smp-server.toml`.

## Delivery boundary

Source and configuration changes only. No test suite, gameplay fixture, dedicated-server startup, build, live summons, world edits or production restart was run for this change. The companion jar must be built and released with the configuration before these changes affect the server. Runtime behavior and survival balance remain unverified at the user's request.
