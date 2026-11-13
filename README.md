# Wombatcraft

A shared frontier of industry, spellcraft, exploration and settlement for Minecraft Java 1.20.1 / Forge 47.4.28.

## Development status

The pack contains 285 pinned mod files and 1,968 quests across 43 chapters. Discord Rich Presence adds two client mods when activated. The quest book includes optional workshop projects, boss hunts, planetary landings and a Twilight Forest campaign. Automated release validation boots a fresh Forge world and checks quest IDs, item references, endgame recipes and server commands, then repeats the check after a clean restart.

The public server is `74.208.117.88:25565`, hosted in Pterodactyl through the existing proxy. Download the Windows launcher from [Releases](https://github.com/wombatjs/wombatcraft/releases/latest).

Extract the complete Windows launcher ZIP before opening `Wombatcraft.exe`. The launcher checks GitHub Releases at startup and restarts into a newer launcher after verifying the download and testing that it opens. You can also check from **About → Check for launcher updates**. Older launcher ZIPs need a one-time manual replacement to gain this feature.

Click **Play** in Wombatcraft to check and install the current pack. Wombatcraft prepares its Java Edition installation and opens the official Minecraft Launcher. Sign in there and select **Wombatcraft** to play. If Minecraft Launcher is new, open Java Edition once to complete setup before returning to Wombatcraft.

`pack/` contains distributable configuration, original quests and scripts. `mod-lock.json` pins upstream downloads and hashes; third-party mod binaries are not stored in Git. The earlier Fabric pack remains a separate backup.

## Graphics and multiplayer

Embeddium, ImmediatelyFast and Dynamic FPS are included. To enable Complementary Reimagined, open **Options → Video Settings → Shader Packs** and turn shaders on. Shaders are off by default.

Open the FTB Chunks map with **M**. Cobblemon uses **,** for the party summary, **'** to hide the party display, and **R** to send out or recall Pokémon. Press **F10** to open the shader menu. Shader reload and toggle shortcuts can be assigned in Controls. Earlier installations may still have JourneyMap waypoints in the instance's `journeymap` folder.

Members can set three homes with `/sethome`. Use `/home`, `/spawn`, `/tpa` and `/warp` to get around. FTB Chunks manages claims and Lightman’s Currency provides coins and player shops. Titles unlock through advancements, and Cosmetic Armor lets players change the appearance of their armor.

Staff can use `/spark` for profiling and `/pp` for PrismProtect block history and rollback. The Moderator rank grants chat moderation. Assign it through FTB Ranks when a staff member needs it.

See the [Discord Rich Presence setup](discord/README.md) for the launcher and Minecraft activity cards and their artwork.

## Release contract

Only a validated release for a commit on `main` becomes an update. Client and server use the same pack manifest. The launcher installs executable updates in a separate folder under its application data, so the original shortcut keeps working and the instance, saves and settings stay in place.

Server updates stage separately, wait for an empty server, take a stopped-world backup and then activate. A daily restart also provides a maintenance window. Failed startup rolls back the pack and its matching world snapshot. Empty-server maintenance runs once for a pending update or daily restart; it does not repeatedly restart an idle server.

Microsoft sign-in and game authentication are handled by the official Minecraft Launcher. Wombatcraft installs the pinned pack and registers its Java Edition profile.

## Attribution

ATM9's publicly viewable quest structure is a research reference, not redistributed content. Its quests, rewards, scripts and menu assets are All Rights Reserved. Wombatcraft quest text, layouts, branding and reward design are original. Third-party mods retain their respective licenses.

Downloads use the Windows certificate trust system, resume interrupted transfers, and verify the published size and SHA-512 before activation. Cached files are reused across updates. If the primary Modrinth host fails, the installer tries its alternate host and remembers that route for the current installation. Play verifies managed files and repairs missing or corrupted downloads.
