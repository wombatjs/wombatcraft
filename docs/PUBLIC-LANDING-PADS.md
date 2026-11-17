# Public landing pads

Implemented in the Wombatcraft companion mod for Forge 1.20.1, TARDIS Refined 2.1.7 and Refined Flight 1.0.2. Both client and server need the updated companion JAR. This is a local implementation; it does not automatically publish a pack release or update the live server.

## Player flow

1. Place a Refined landing pad. It starts hidden and belongs to its placer.
2. Right-click the pad with an empty main hand. Its owner or an administrator can save a name and toggle public visibility. Claim permissions still apply.
3. Open the TARDIS monitor and choose **Public Landing Pads**, immediately below **Waypoints**.
4. Search by name, owner or dimension, optionally filter to available pads, and choose **Set destination**. An unloaded destination offers **Check & set**.
5. Fly using the normal console controls. The Landing Pad upgrade and the TARDIS's dimension discovery/restrictions apply.

Existing pads acquire an owner on their first permitted empty-hand interaction. Players who cannot edit the claimed area cannot adopt its pads. Names accept 1–40 plain characters. Duplicate display names are permitted; the owner, dimension and coordinate tooltip distinguish them.

Hidden pads are unlisted, not protected against coordinate travel. Hiding a pad rejects a new directory departure but does not strand a TARDIS already travelling to it. Breaking/replacing the block invalidates its previous identity. Command placement and other non-player creation remain unlisted until adopted.

## Availability and flight

The server is authoritative. The directory refreshes every five seconds and immediately after an action. Browsing does not load chunks. Explicit destination checks may load the pad and doorway chunks, without permanent tickets.

| Status | Meaning |
| --- | --- |
| Available | Clear shell space and doorway, with no incoming reservation |
| Incoming | A travelling TARDIS has reserved this pad |
| Occupied | A Refined exterior occupies the pad |
| Blocked | Shell space, doorway, world border or build-height checks fail |
| Unverified | The destination chunk or dimension is unavailable for a current check |
| Removed | A previously registered block is missing |
| Moving pad unsupported | A Valkyrien Skies shipyard position; it cannot be published or selected |

Takeoff reserves a pad synchronously. A second TARDIS is refused before departure. Choosing a pad while already in flight reserves it immediately. Arrival rechecks clearance and uses the exact pad location, rather than Refined's nearby fallback. An unavailable arrival remains in flight and asks the pilot to choose another destination. Coordinate travel to a registered pad uses the same guards. Exterior flight checks its landing before restoring the pilot/removing its flight state, and final shell placement has a further guard.

Retargeting, completed/cancelled flight state and missing TARDIS dimensions release stale reservations. Occupancy comes from actual shell blocks, so it changes when an exterior is placed or removed, including exterior flight. Incoming reservations and pad metadata are saved with the world. The active-flight state is checked after restart. No timed lease can expire under an actively travelling ship and allow a competing arrival.

Arrivals align to the pad's facing so the checked doorway is the doorway used for landing. Players and mobs do not count as parked TARDISes or reserve pads; in particular, the exterior-flight pilot must not block their own arrival.

Only owner/admin requests within eight blocks can save settings. Names and packet lengths are bounded, writes check a revision, and console requests require the player to stand near the current console inside a TARDIS. The registry is capped at 4,096 pad records.

## Files and verification

- `PadRegistry.java`: world SavedData, pad UUIDs, ownership, selected targets and reservations.
- `PublicPads.java`: block interactions, permission checks, availability and flight integration.
- `PadNetwork.java`: bounded server-authoritative requests and snapshots.
- `client/PublicPadsScreen.java`: settings and paginated directory.
- `coremods/public_pads.js`: version-specific monitor, flight, final placement, removal and exterior-flight hooks.
- `PublicPadsAudit.java`: opt-in disposable-server regression fixture.
- `client/PublicPadsPreview.java`: opt-in offline rendering and actual monitor insertion check.

Build from `mods/wombatcraft-smp` with Java 17: `gradlew.bat test build`.

After the existing disposable server has been provisioned with `tools/smoke_server.py`, run `python tools/test_public_pads.py`. It uses only the loopback server at `dist/server-smoke`, runs twice and checks persisted state after restart. `python tools/preview_public_pads.py` uses the installed Forge libraries, an isolated minimal client directory and an offline test identity to render six fixture screenshots. Neither test reads account tokens or connects to the live server.

Real player acceptance should cover empty-hand interaction in survival, keyboard/controller-scale menu use, two players travelling from separate TARDISes, exterior-flight landing, and reconnect. Moving ship pads are deliberately unsupported in this version.
