# TARDIS room growth and real-world flight

Pinned Forge 1.20.1 addons: TARDIS Refined: Rooms 1.2.0 and TARDIS Refined: Flight 1.0.2. TARDIS Refined remains 2.1.7 and Immersive Portals is retained.

Rooms: grow selected rooms and corridors with a sonic screwdriver at growth nodes. Expansion is free; players supply their own machines. The interface supports items, fluids, energy, redstone, Mekanism chemicals and AE2 networks. Automatic hub simplification is enabled. The operator-approved one-time corridor migration removes generated chunks outside the central console reserve, including their contents, after a stopped-server backup. The Eye of Harmony and hub regenerate with the room-growth layout; ship metadata, upgrades and the central console reserve remain. New terrain has no procedural ARS corridors. Ten quests explain growth, furnishing, transfers and renovation.

Room growth/removal checks the affected chunk footprint against FTB edit permission, including adjacent passage panels. Interface changes and room renaming also check claims. Checks run on actions without loading chunks. Arena actions are rejected. Empty removable-block exemptions prevent configured container exemptions; the upstream explicit Remove all confirmation can still destroy contents. Do not confirm it unless intending to discard those contents.

Flight: unlock Flight Control System first; fuel and five efficiency upgrades remain enabled. F7 exits flight, F9 requests Local Hop, and Free Fall starts unbound to avoid Q/drop conflicts. Six quests cover upgrades, fuel and piloting. Starting flight and local hops are blocked during the existing 90-second PvP lockout and in the arena; local-hop destinations check claims. This is not a blanket ban on ordinary aerial travel over claimed land.

Both mods use official CurseForge download URLs with pinned hashes. No new permanent chunk loaders or forced chunks are configured. Flight and room creation can still generate terrain/load chunks; no measured performance claim is made. The release pipeline checks server startup, integration class loading and quest references. Player piloting, portal rendering and two-player permissions require gameplay verification.

Sources:
- https://www.curseforge.com/minecraft/mc-mods/tardis-refined-rooms/files/9540058
- https://github.com/LeonusDH/TardisRefined-Rooms/tree/1.20.1
- https://www.curseforge.com/minecraft/mc-mods/tardis-refined-flight/files/8945950
