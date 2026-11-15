# TARDIS Refined migration

The accepted isolated client uses Forge 47.4.28, TARDIS Refined 2.1.7 and Immersive Portals 3.0.7. It retains Embeddium, uses compatibility rendering with one portal layer, and uses ITP threshold teleportation. Oculus is excluded. The user reported successful portal play with minor camera stutter; the PC had approximately 1 GB free RAM. This is not a measured frame-time benchmark or proof of multiplayer stability.

## Lessons carried into the release

- Apply IP dimension ID updates before decoding the next portal packet to prevent `Missing Dimension 70`.
- Include valid resource-pack metadata in the compatibility JAR.
- Keep the removed-player chunk guard on both sides. The Aurorian dedicated-server handler stub belongs only on the dedicated server.
- Hide the BOTI portal entity tooltip and suppress unknown dynamic dimension titles using the tested settings.
- Keep Nether and End portal behavior vanilla.
- Replace NTM item references in the quest book, retaining chapter and existing quest IDs.

## Cutover and recovery

The owner confirmed there are no existing NTM TARDISes. A read-only inspection found no saved `tardis` dimensions and both saved players in the Overworld with no NTM inventory or ender-chest items. No interior conversion is performed. Ordinary worlds and player progress remain in place.

Publish only after the release checks pass. Stop automatic maintenance during preparation. Use the existing maintenance controller to stage verified artifacts, wait for an empty server, save and stop, snapshot the world and configuration, retain the prior managed pack, activate and restart. Preserve an additional migration backup outside the controller's seven-backup rotation. A failed startup restores both the prior pack and its matching world snapshot. Do not restore only the old mod JARs over a world saved with the new pack.

The launcher installs the same immutable release manifest and removes retired managed NTM, NTM-IP and Oculus files. Players must close Minecraft and update before reconnecting. The standalone Refined test profile remains separate.

After cutover verify the actual server mod hashes, first Refined server config, startup log, public status and client manifest. Player acceptance of multiplayer doorway traversal and reconnect is still required; do not call a server startup a completed player test.
