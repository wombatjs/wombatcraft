# Wombatcraft

A shared frontier of industry, spellcraft, exploration and settlement for Minecraft Java 1.20.1 / Forge 47.4.28.

## Development status

The pack-specific [Convergence progression foundation](docs/CONVERGENCE.md) adds six disciplines, four material tiers, 47 progression recipes, four processing stations, individual research, two quest chapters, Resonant Ore and expedition equipment. See its acceptance notes before release.

The pack contains 344 pinned upstream mod files and 3,142 quests across 117 chapters, plus Wombatcraft's generated compatibility and community modules. The quest book includes workshop projects, boss hunts, planetary landings, supernatural progression and archaeology. Automated release validation boots a Forge world and checks quest IDs, item references, endgame recipes and server commands, then repeats the check after a clean restart.

The public server is `74.208.117.88:25565`, hosted in Pterodactyl through the existing proxy. Download the Windows launcher from [Releases](https://github.com/wombatjs/wombatcraft/releases/latest).

Extract the complete Windows launcher ZIP before opening `Wombatcraft.exe`. The launcher checks GitHub Releases at startup and restarts into a newer launcher after verifying the download and testing that it opens. You can also check from **About → Check for launcher updates**. Older launcher ZIPs need a one-time manual replacement to gain this feature.

Click **Play** in Wombatcraft to check and install the current pack. Wombatcraft prepares its Java Edition installation and opens the official Minecraft Launcher. Sign in there and select **Wombatcraft** to play. If Minecraft Launcher is new, open Java Edition once to complete setup before returning to Wombatcraft.

`pack/` contains distributable configuration, original quests and scripts. `mod-lock.json` pins upstream downloads and hashes; third-party mod binaries are not stored in Git. The earlier Fabric pack remains a separate backup.

The [Main Journey and Adventure Journal](docs/MAIN-JOURNEY.md) preserve all 3,126 original quests, add 72 linked milestones and provide personal next objectives through `/journey`.

## Graphics and multiplayer

Embeddium, ImmediatelyFast and Dynamic FPS are included. TARDIS Refined uses the tested Immersive Portals compatibility renderer with see-through doors and direct threshold traversal. Oculus and the bundled shader pack are excluded from this release to match the accepted portal setup.

Open the FTB Chunks map with **M**. Cobblemon uses **,** for the party summary, **'** to hide the party display, and **R** to send out or recall Pokémon. Press **F10** to open the shader menu. Shader reload and toggle shortcuts can be assigned in Controls. Earlier installations may still have JourneyMap waypoints in the instance's `journeymap` folder.

Members can set three homes with `/sethome`. Use `/home`, `/spawn`, `/tpa` and `/warp` to get around. FTB Chunks manages claims and Lightman’s Currency provides coins and player shops. Titles unlock through advancements, and Cosmetic Armor lets players change the appearance of their armor.

Use `/faction` for faction members and shared land, `/townhall` for the town ballot, and `/contracts` for orders. Each has a separate pause-menu button. Quest and research progress belongs to each player, independently of factions. The election roster rotates 50 candidates through eight-candidate primaries and three-candidate finals. `/contracts` opens daily, community and factory orders at spawn. Staff can place the [mayor and secretary](docs/CIVIC-RESIDENTS.md).

Easy NPC provides configurable residents. Staff set the first-visit tutorial entrance with `/settutorialspawn`; players return with `/tutorial`. See [tutorial arrivals](docs/TUTORIAL-SPAWN.md). Arena duels and combat-logout bodies remain unavailable; their unfinished adapters are not activated in this release.

Staff can use `/spark` for profiling and `/pp` for PrismProtect block history and rollback. The Moderator rank grants chat moderation. Assign it through FTB Ranks when a staff member needs it.

See the [Discord Rich Presence setup](discord/README.md) for the launcher and Minecraft activity cards and their artwork.

Everyday utilities, lootable gravestones and the expanded vanilla market are documented in [Utility and market update](docs/UTILITY-MARKET.md).

Use `/moonblood` for hybrid progression. A level 5 werewolf can drink a Moonblood Draught to gain night sight and feed on hostile mobs in wolf form. A Sunward Charm held in either hand protects against the hybrid's sunlight damage. A Severing Tonic removes the bond and preserves werewolf levels. Server admins can tune Moonblood in the `[moonblood]` section of the world's `serverconfig/wombatcraft_smp-server.toml`.

## Release contract

Only a validated release for a commit on `main` becomes an update. Client and server use the same pack manifest. The launcher installs executable updates in a separate folder under its application data, so the original shortcut keeps working and the instance, saves and settings stay in place.

Server updates stage separately, wait for an empty server, take a stopped-world backup and then activate. A daily restart also provides a maintenance window. Failed startup rolls back the pack and its matching world snapshot. Empty-server maintenance runs once for a pending update or daily restart; it does not repeatedly restart an idle server.

Microsoft sign-in and game authentication are handled by the official Minecraft Launcher. Wombatcraft installs the pinned pack and registers its Java Edition profile.

## Attribution

ATM9's publicly viewable quest structure is a research reference, not redistributed content. Its quests, rewards, scripts and menu assets are All Rights Reserved. Wombatcraft quest text, layouts, branding and reward design are original. Third-party mods retain their respective licenses.

Downloads use the Windows certificate trust system, resume interrupted transfers, and verify the published size and SHA-512 before activation. Cached files are reused across updates. If the primary Modrinth host fails, the installer tries its alternate host and remembers that route for the current installation. Play verifies managed files and repairs missing or corrupted downloads.
