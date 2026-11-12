# Wombatcraft

A shared frontier of industry, spellcraft, exploration and settlement for Minecraft Java 1.20.1 / Forge 47.4.28.

## Development status

The pack contains 285 pinned mod files and 1,936 quests across 42 chapters. Twenty-three major-mod chapters have authored progression branches. Automated release validation boots a fresh Forge world and checks quest IDs, item references, endgame recipes and server commands, then repeats the check after a clean restart.

The public server is `74.208.117.88:25565`, hosted in Pterodactyl through the existing proxy. Download the Windows launcher from [Releases](https://github.com/wombatjs/wombatcraft/releases/latest).

Extract the complete Windows launcher ZIP before opening `Wombatcraft.exe`. The launcher checks GitHub Releases at startup and restarts into a newer launcher after verifying the download and testing that it opens. You can also check from **About → Check for launcher updates**. Older launcher ZIPs need a one-time manual replacement to gain this feature.

Click **Play** in Wombatcraft to check and install the current pack. If you're signed out, Wombatcraft prepares its Java Edition installation and opens the official Minecraft Launcher. Sign in there and select **Wombatcraft** to play. If Minecraft Launcher is new, open Java Edition once to complete setup before returning to Wombatcraft. Direct sign-in inside Wombatcraft still needs Minecraft API approval and a licensed account test.

`pack/` contains distributable configuration, original quests and scripts. `mod-lock.json` pins upstream downloads and hashes; third-party mod binaries are not stored in Git. The earlier Fabric pack remains a separate backup.

## Graphics and multiplayer

Embeddium, ImmediatelyFast and Dynamic FPS are included. To enable Complementary Reimagined, open **Options → Video Settings → Shader Packs** and turn shaders on. Shaders are off by default.

Members can set three homes with `/sethome`. Use `/home`, `/spawn`, `/tpa` and `/warp` to get around. FTB Chunks manages claims and Lightman’s Currency provides coins and player shops. Titles unlock through advancements, and Cosmetic Armor lets players change the appearance of their armor.

Staff can use `/spark` for profiling and `/pp` for PrismProtect block history and rollback. The Moderator rank grants chat moderation. Assign it through FTB Ranks when a staff member needs it.

## Release contract

Only a validated release for a commit on `main` becomes an update. Client and server use the same pack manifest. The launcher installs executable updates in a separate folder under its application data, so the original shortcut keeps working and the instance, saves and settings stay in place.

Server updates stage separately, wait for an empty server, take a stopped-world backup and then activate. A daily restart also provides a maintenance window. Failed startup rolls back the pack and its matching world snapshot. Empty-server maintenance runs once for a pending update or daily restart; it does not repeatedly restart an idle server.

The custom launcher uses Microsoft browser authentication. A Wombatcraft app registration and Minecraft API approval are required before public sign-in is enabled. No borrowed launcher client ID, offline authentication or passwords collected by this application.

## Attribution

ATM9's publicly viewable quest structure is a research reference, not redistributed content. Its quests, rewards, scripts and menu assets are All Rights Reserved. Wombatcraft quest text, layouts, branding and reward design are original. Third-party mods retain their respective licenses.
