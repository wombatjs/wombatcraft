# Wombatcraft presence

Both the launcher and game use application `318787432738717696` and the original copper wombat logo in `wombatcraft-logo-v2.png`. The image is loaded over HTTPS from this repository; no Discord portal upload is required for these configured URLs.

**Download Wombatcraft** links directly to the latest tested Windows launcher ZIP. Extract it and run Wombatcraft.exe; that launcher checks for its own updates and installs the pack. No separate Minecraft account login is implemented here: Minecraft Launcher handles login.

In-game presence rotates every 60 seconds through dimension/biome, deaths/mob kills, world playtime, jumps/health, and weather/time. The client requests authoritative statistics once per minute, and captures display data on the Minecraft client thread. No coordinates, save names, usernames, or private server addresses are published. Launcher presence rotates three short welcome lines. Each card carries the download button.

The launcher migrates the previous default presence once, backing up the old TOML and preserving disabled presence and unrelated server entries. Players can still disable Discord presence. In-game render/Discord acceptance testing must be performed with an actual client; dedicated-server CI cannot validate Discord display.
