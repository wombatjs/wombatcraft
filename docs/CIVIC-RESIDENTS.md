# Spawn civic residents

Included in public release `9a790e89c09e98ca3052996332d12cf44e9e5a2b` (November 16, 2026).

With the SMP module enabled, an operator standing inside the configured spawn claim can place residents:

```text
/wccivic place secretary
/wccivic place mayor
```

Each resident appears at the operator's feet, facing the operator. Move to the desired position before running the command. `/wccivic remove` removes the nearest civic resident within four blocks, leaving other spawn NPCs alone.

Elowen, the Mayor's Secretary, opens the current primary or final ballot on right-click. Players can review the candidate policies and change their vote before that phase closes. Voting is checked on the server and requires being inside spawn.

The mayor resident displays the current elected candidate's name and shares their policy on right-click. Election results automatically update every loaded mayor resident within ten seconds; unloaded residents update when their chunk loads. Placement is persistent across server restarts. The NPC remains stationary, silent and invulnerable. Its villager model is shared between candidates; the elected identity and policy change.

The election roster contains 50 fixed candidates. Eight run in each primary and three advance to the final ballot. Each phase lasts two real days. The incumbent serves until the final result; the next election starts immediately. Ties follow the published slate order, and an election with no final votes retains the incumbent.

Only tagged civic residents are tracked using weak references. There is no recurring world-wide entity search. Mayor hand-item perks inspect two hand slots per online player every two seconds. Night vision renews above its flashing threshold and can linger up to thirteen seconds after eligibility ends; other perks expire within four seconds.

## Verification

- Gradle tests and build passed with the placement and interaction implementation.
- Both residents were placed in the disposable QA spawn claim; names appeared correctly.
- Right-clicking the secretary opened the final ballot with the three qualified candidates.
- A final vote was accepted through that screen and persisted in the world's election data.
- Advancing only the private world's election epoch past the final deadline elected Brasswick. After restart and chunk load, the existing mayor resident automatically changed from Steward Hearth to `Mayor Brasswick | Town Mayor`, verified through the server's entity data. No replacement NPC command was needed.
- Full-modset dedicated-server startup, dimension generation and restart validation passed, including WorldEdit. The release was deployed with a pre-update backup; the public server reported 20 TPS and approximately 21 ms mean ticks with no players online. This is not a populated stress test.
