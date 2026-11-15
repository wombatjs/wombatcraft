# Performance update — November 15, 2026

This release promotes the Performance Lab build accepted in gameplay testing.

- Adds pinned Saturn, Noisium, BadOptimizations, MemoryLeakFix and Radium Re-Reforged builds. Includes the existing ServerCore build in the client for singleplayer as well as on the dedicated server.
- Disables Spark's background profiler on clients while retaining server profiling.
- Disables JEI tooltip indexing; item names, mod names and tags remain searchable.
- Uses the tested renderer, culling and portal settings, with conservative Radium and BadOptimizations exceptions for Immersive Portals.
- Seeds new installations with six-chunk rendering, five-chunk simulation and a 90 FPS cap. Existing personal controls, video options and launcher RAM allocations are preserved.
- Repairs Titles synchronization when connecting after another world session.
- Includes the live TARDIS interior lifecycle repair: independent interior borders and refreshed Forge world ticking for newly created interiors.

The settled 60-second local frame capture averaged 89 FPS, with a 49.61 FPS 1% low and no frames over 100 ms. These are measurements from the test PC, not a guarantee for every machine. The user accepted this build for release.

Repeated world/server switches in the same Minecraft process can still retain recipe-viewer memory. Restart Minecraft between sessions if memory builds up. An experimental JEI runtime cleanup patch is deliberately excluded because it has not completed gameplay testing.

Publication requires the existing quest/config validation, launcher tests, launcher build and dedicated-server smoke test. Server activation follows the normal empty-server maintenance process with a saved-world backup and rollback protection.
