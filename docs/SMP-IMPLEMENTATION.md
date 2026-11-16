# Wombatcraft SMP implementation

Status: isolated staging branch; no production activation. The companion mod is not in the release manifest.

## Accepted rules

- Factions extend FTB Teams/Chunks, with custom screens, roles, chat and per-chunk policy. Existing ownership remains authoritative.
- PvP is enabled outside spawn and arena safe areas. This does not authorize raiding, stealing or offline destruction.
- The economy uses Lightman's Currency, spawn-only market access, player-priced production and funded demand.
- Individual daily contracts, one substantial weekly collective project, and faction manufacturing orders consume deliveries and track contributions.
- NPC mayor candidates have fixed, visible policies. Terms last four real-world days; mod-specific policies must expire cleanly.
- Arena: own gear, full loot, safe arrival/dropdown, separate consent-based duels, 90-second winner loot collection.
- Arena exit: stand still for 60 seconds; movement, attacking or receiving damage cancels.
- All PvP combat: logging out leaves the character vulnerable for 60 seconds. No duplicate inventory or loot on reconnect/restart.
- Strong dimension protection must cover modded edits, not merely vanilla block interactions.

## Release gates

1. Build and domain tests; bounded data and network payloads; no idle world/entity scans.
2. Disposable server: command permissions, TPA once/duplicate/expired/wrong recipient; existing factions and claims survive migration.
3. Two-client multiplayer: team roles and per-chunk access; forged packets; invitations and role escalation; GUI navigation.
4. Economy: exact-once delivery/payment, faction switching, partial stacks, full inventories, daily/weekly rollover, restart/crash recovery.
5. Elections: one vote/account/term, ties, offline rollover, clock rollback, persistent policy and clean expiry.
6. Combat: own gear, curios, soulbound and grave integration, backpacks with contents, summon attribution, duplicate death events, reconnect at expiry.
7. Arena: tested attack/teleport/edit matrix for installed mods; no production entry until complete.
8. Profile idle and populated operation against the existing baseline. Publish only after behavior and recovery gates pass.

## Delivery tracking

- [x] Staging branch isolated from production auto-update.
- [x] Companion mod compiled and packaged; 22 rule tests pass.
- [ ] TPA acceptance/warmup fix verified.
- [ ] Faction ownership adapter and chunk policy enforcement.
- [ ] Faction UI integration.
- [ ] Spawn-only market and durable contract settlement.
- [ ] Elections and verified mod-specific policies.
- [ ] Arena build, protection and duels.
- [ ] Combat logout inventory lifecycle and recovery.
- [ ] Full multiplayer/performance acceptance.
- [ ] Production deployment.

Pure rule tests are not gameplay acceptance. Unverified features must remain disabled in staging and absent from production.

## Private test evidence (2026-11-16)

- Disposable server `wombatcraft-smp-qa` binds Minecraft and RCON to home-server loopback only. Fresh world, two CPU limit, 4 GiB Java heap. No live world copied or edited.
- First boot with WorldEdit 7.2.15 exhausted the test heap while indexing block states. WorldEdit was moved to the private copy's `disabled-mods`; production is unchanged. A full-modset compatibility pass remains required and the reduced test is not a production performance benchmark.
- Without WorldEdit, the private server started and the first client joined in approximately 32 seconds after clicking connect. One-player sample reported 20 TPS and 21.5 ms mean overworld tick time.
- `/wcsmp audit` passed on the running Forge server: arena world and direct chunk setters reject edits, scoped construction works, exceptions clear the bypass, and the overworld remains editable.
- Faction screen rendered real FTB data in multiplayer. This exposed a nonexistent fallback GUI command, replaced with FTB's own `OpenGUIMessage`. The corrected build successfully opened the native creation screen, created a party, and returned its actual ownership/member count. A chunk was claimed through FTB's map and its private-factory policy was successfully changed through the custom screen.
- FTB's wrapper did not render the injected vanilla map button. A render/input event bridge was added and verified in-game: the button appears at the bottom right and returns to the correct territory page. The saved claim and private-factory policy also survived client reconnect.
- Additional hardening: chunk changes bind to both claim creation time and dimension/chunk coordinates; Builder role removed on leaving a party; delayed UI replies cannot reopen closed screens; contract reservation lookup uses indexed totals rather than scanning old operations.

Not yet accepted: two-player permission/TPA scenarios, full modded arena attack matrix, durable inventory/payout settlement, actual logout-body lifecycle, full duels, mayor policy adapters, populated performance and production rollout.

Two full private clients joined simultaneously, but the second subsequently crashed in native `glfw.dll` on its render thread. The JVM report recorded approximately 1.5 GiB physical memory free; this is a possible contributing factor, not a proven diagnosis. Report: `work/wombatcraft/smp/client2/hs_err_pid27012.log` in the local workspace. No completed two-player acceptance is claimed. Both identities and all test claims belong only to the disposable offline QA world.

Inventory-bearing features remain release blockers. A logout body must account for Curios, backpacks, graves, soulbound rules and crash/restart recovery before activation. Contract payouts likewise require durable settlement before currency or items are granted. The existing state-machine tests do not provide these adapters.

The private clients and QA server were stopped after that milestone. They were restarted for the subsequent civic expansion below. Source is on `smp-factions-staging`; `main` and the production release manifest are unchanged.

## Subsequent civic expansion (2026-11-16, staging)

The companion module now has 34 passing automated tests, including two-stage elections and durable contract settlement. A private multiplayer client submitted a primary vote, delivered 64 stone, received 4.4 copper of credit and collected four Lightman's Currency coins, leaving the fractional balance intact. Restarted election data retained the primary vote and advanced to a three-candidate final. Right-clicking the placed secretary opened that final ballot and successfully recorded a final vote. See [Civic residents](CIVIC-RESIDENTS.md) for placement commands.

This supersedes the earlier statement that no contract settlement or mayor policy adapter exists. Remaining acceptance includes full inventories, populated performance, two-player permissions, policy transitions and the full modset server boot with WorldEdit. Arena duels and combat logout inventory adapters remain unfinished. No production deployment has occurred.

The expanded content validates at 3,007 quests across 100 chapters and 328 locked mod files. Private startup exposed and resolved a Fossils/End's Delight class-compatibility error. Reconnecting the eight-GiB client also reached 99% heap usage before recovering; this is an unresolved performance warning, not release acceptance.

## Published civic expansion

Release `9a790e89c09e98ca3052996332d12cf44e9e5a2b` adds Easy NPC Bundle/Core/Config UI, bringing the upstream lock to 331 files. Full dedicated-server tests now include WorldEdit and pass startup, 3,007 quest references, 30 expansion destinations and restart checks. Tutorial arrival tests and a real-client return/cancellation test passed. The Easy NPC editor opened in multiplayer. Client testing used the launcher's normal ten-GiB allocation; lower-memory and populated stress testing are not claimed.

The public server activated this release from a stopped-world backup on November 16, 2026. Elections and contracts are enabled; per-chunk policy replacement, arena duels and logout-body inventory handling remain disabled or unavailable. Startup health checks passed and an idle sample reported 20 TPS at 20.866 ms mean ticks. The installed local pack was also updated and its 566 managed files verified. Earlier staging-only statements above describe historical milestones.
