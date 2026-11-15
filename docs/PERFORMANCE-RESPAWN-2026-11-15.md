# Performance and respawn pass — 15 November 2026

Performance and connection stability take precedence over additional content.
The baseline was commit `964ddce59a911467e5304e6928ac268ba38c18cf`.

## Runtime changes

- JITL 2.2.4 stats and essence packets now enqueue their client work through
  Forge's network context and ignore an invalid old player capability during
  respawn. The baseline disconnected immediately after `/kill`; the candidate
  passed two singleplayer and two dedicated-server death/respawn cycles.
- JITL's periodic knowledge synchronization runs once per second, on the END
  player tick, instead of every tick. Explicit knowledge gains still synchronize
  immediately. This reduces that periodic path's calls by 95%; it is not a
  measurement of total network traffic.
- DivineRPG's Aequorea and Ayeraco entity data is registered against the owning
  entity classes instead of the shared Mob class. Client and server must update
  together. A dedicated-server EvilCraft vengeance spirit spawn was tested
  without the previously observed entity-data failure.
- Create, Ars Nouveau, Occultism and AE2 release their static JEI runtime
  references when that runtime becomes unavailable. AE2's existing cleanup is
  preserved. A singleplayer-to-multiplayer switch was tested in one process.
- JEI generates vanilla anvil enchantment and grindstone disenchantment previews
  for vanilla equipment only. Ordinary mod crafting/processing, actual enchanting,
  repair recipes and gameplay remain available. The tradeoff is that modded
  equipment loses those generated preview combinations. Set
  `-Dwombatcraft.fullEnchantmentPreviews=true` to restore the full previews.

All transformers check the expected class shape. Actual Forge runtime log
markers, not just the standalone ASM verifier, confirmed that the patches loaded.

## Measurements and limits

On the same test PC, with a 12 GiB Java heap limit and the same singleplayer test
world, retained heap after an explicit collection fell from 8,441,994 KiB to
6,709,727 KiB: about 1.65 GiB, or 20.5%. Anvil previews fell from 154,959 to 7,053.
After moving that candidate to multiplayer in the same process, retained heap was
7,081,615 KiB rather than another complete recipe index being retained.

The combined candidate with Akashic Tome and JourneyMap retained 6,771,769 KiB
after collection in the private multiplayer test. That is a separate scene, not
a controlled comparison to the singleplayer baseline.

A settled 60-second multiplayer capture with both additions and the duplicate
minimap disabled recorded 88.88 average FPS, 53.32 FPS for the slowest 1%, a
27.66 ms maximum frame, and no frames over 50 ms. This was a stationary loaded
scene, not a moving exploration benchmark. A prior capture overlapped a manual
garbage collection and was excluded. This first combined capture used JourneyMap
6.0.7, which was subsequently rejected because it disables Create's train map.

Settled 60-second singleplayer captures were approximately 88.6 FPS before and
87.2 FPS after, capped at 90. There was no demonstrated steady-state FPS gain.
Both had two frames over 50 ms and none over 100 ms. The candidate capture
included a short menu interval, so these numbers are a sanity check, not a
rigorous comparative benchmark. The established improvement is reduced retained
memory and fixed reproduced disconnects. Recipe indexing and new chunk creation
can still cause startup/travel stalls; this report does not claim every hitch is
eliminated.

Private dedicated-server testing used a loopback-only server and SSH forwarding.
It does not measure public-proxy latency. No production world or player inventory
was used by the death tests.

## Content and overlap

The requested list was compared to the installed lock. Only Akashic Tome and
JourneyMap were missing and compatible. JourneyMap 5.10.3 is pinned because 6.x
disables Create's built-in train map. JourneyMap is client-only; FTB Chunks
remains responsible for claims and its default minimap. JourneyMap's two minimap
presets start disabled, with its full-screen map on F6 (J is used by Journey
Into the Light's stats). Missing new keybinds are
seeded without replacing existing controls; an occupied custom key stays owned
by its existing binding. Explored maps and waypoints are not managed release files.

No duplicate cooking system was added. Actually Additions has no Forge 1.20.1
release and was omitted. The quest book gains 67 authored quests across field
equipment, Tinkers' Construct, Quark and Biomes O' Plenty: 2,807 quests across 90
chapters. Existing quest IDs and authored TARDIS rewards are preserved.

## Server memory finding

WorldEdit 7.2.15 is an administrator-installed jar outside the pack lock. In an
isolated copy it exhausted a 5 GiB heap during platform initialization; the same
server without that jar started and retained about 3.15 GiB after collection.
This implicates WorldEdit's block-state conversion caches in this large pack.
The public installation is unchanged pending the owner's choice about keeping
WorldEdit available continuously or enabling it only for building sessions.
Existing builds and schematics do not require WorldEdit to remain loaded.

## Verification

The final 5.10.3 client completed a fresh dynamic TARDIS interior entry, survival
item pickup, and death/respawn back into the overworld without disconnecting.
Entry retained 20 health and the interior border measured 59,999,968 blocks.
The test positioned the player at the shell threshold using RCON; it was not
a manual walking benchmark. A settled 60-second multiplayer capture after this
cycle averaged 88.67 FPS, with a 46.26 FPS 1% low, a 44.54 ms worst frame, and
zero frames above 50 ms. This stationary capture does not measure exploration
or public-network latency. Retained heap after collection was 6,777,496 KiB.

- 26 launcher/update tests passed, including path restrictions, preservation of
  custom controls, repeat updates, and recovery behavior.
- Static validation passed 318 mod files and 2,807 quests with an acyclic graph.
- ASM verification passed all targeted classes; Forge runtime application markers
  were observed on the combined client.
- The normal release workflow must still pass dedicated-server startup, restart,
  runtime item/quest checks, dimension generation and launcher packaging before
  publication.
