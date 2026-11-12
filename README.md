# Wombatcraft

A shared frontier of industry, spellcraft, exploration and settlement for Minecraft Java 1.20.1 / Forge 47.4.28.

## Development status

The pack contains 276 pinned mod files and 1,604 quests across 37 chapters. Sixteen major-mod chapters now have authored progression branches. The earlier client was tested on an RTX 4070 and its seamless TARDIS doorway was traversed in both directions. Automated release validation boots a fresh Forge world and checks every quest ID, item reference and endgame recipe.

The public server is `74.208.117.88:25565`, hosted in Pterodactyl through the existing proxy. Download **Wombatcraft.mrpack** from [Releases](https://github.com/wombatjs/wombatcraft/releases/latest) and import it into Modrinth App.

The Windows launcher preview uses the purple woodland-sunset identity. Extract its complete ZIP before opening `Wombatcraft.exe`. Pack installation and updates are implemented, but Microsoft sign-in and authenticated game launch remain unavailable until the app registration and Minecraft API approval are completed. Modrinth is the playable route in the meantime.

`pack/` contains distributable configuration, original quests and scripts. `mod-lock.json` pins upstream downloads and hashes; third-party mod binaries are not stored in Git. The earlier Fabric pack remains a separate backup.

## Release contract

Only a validated release for a commit on `main` becomes an update. Client and server use the same release manifest. Existing saves, player data, accounts and local settings are never sourced from Git.

Server updates stage separately, wait for an empty server, take a stopped-world backup and then activate. A daily restart also provides a maintenance window. Failed startup rolls back the pack and its matching world snapshot. Empty-server maintenance runs once for a pending update or daily restart; it does not repeatedly restart an idle server.

The custom launcher uses Microsoft browser authentication. A Wombatcraft app registration and Minecraft API approval are required before public sign-in is enabled. No borrowed launcher client ID, offline authentication or passwords collected by this application.

## Attribution

ATM9's publicly viewable quest structure is a research reference, not redistributed content. Its quests, rewards, scripts and menu assets are All Rights Reserved. Wombatcraft quest text, layouts, branding and reward design are original. Third-party mods retain their respective licenses.
