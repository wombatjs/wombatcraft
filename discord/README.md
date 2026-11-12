# Wombatcraft on Discord

The launcher shows **In the Wombatcraft Launcher** while you get ready. Minecraft shows activity for the main menu, singleplayer, multiplayer, or the official server. **Get Wombatcraft** opens the latest download. The official server uses its own workshop art. Activity text uses fixed labels so it never publishes a player name, world name, coordinates, or a private server address.

## Developer Portal setup

1. Create an application named **Wombatcraft** in the [Discord Developer Portal](https://discord.com/developers/applications).
2. Under **General Information**, use `wombatcraft.png` for the app icon and copy the public **Application ID**. The app needs only this ID for the Rich Presence integration.
3. Under **Rich Presence → Art Assets**, upload the following PNGs and keep their asset keys exactly as written. [Art asset instructions](https://srpc.fdd-docs.com/customizing/custom-app/)

   | Upload | Asset key | Where it appears |
   | --- | --- | --- |
   | [wombatcraft.png](wombatcraft.png) | `wombatcraft` | Launcher, menu, gameplay, and official server badge |
   | [wombatcraft_frontier.png](wombatcraft_frontier.png) | `wombatcraft_frontier` | Official server card |

4. In [`pack/config/simple-rpc/simple-rpc.toml`](../pack/config/simple-rpc/simple-rpc.toml), set `applicationID` to the copied ID as a quoted string and set `enabled = true` in `[general]`. The Windows launcher bundles the same file, so both use the same Application ID. The release packages the Discord mods and settings once this is enabled. Publish through the normal Wombatcraft release workflow.
5. Test with the Discord desktop app open and activity sharing enabled. Open the launcher, then start Minecraft and check that its status replaces the launcher’s. Check singleplayer and the official server, then close Minecraft and confirm the launcher’s status returns.

The launcher has a **Settings → Discord** toggle after activation. In Minecraft, the **Simple RPC** config under **Mods** controls in-game presence. Wombatcraft keeps each player's game-side preferences when updating the pack.

## Artwork

Both images were made with the built-in ImageGen tool. The prompts below were used for the portal assets.

**Wombatcraft**

> Use case: logo-brand. Create original square 1:1 Discord Rich Presence artwork for Wombatcraft, with the attached existing wombat mascot as the character identity reference. Preserve the rounded ears, dark broad nose, friendly closed eyes and warm chestnut fur. A centered chest-up portrait with a simple copper expedition clasp, a large dark spruce woodland background and a subtle teal lantern glow behind it, copper rim light. Painterly game key art with chunky, readable silhouettes at 64 px; bold warm wombat face fills the center 70 percent, safe padding on all sides so it also works cropped into a circle. Restrained dark spruce, copper and teal palette. Elegant, inviting, one clear focal point, no borders, no words, no lettering, no watermarks, no UI. Output a single square image.

**The Wombatcraft Frontier**

> Use case: stylized-concept. Single square 1:1 Discord Rich Presence artwork for the Wombatcraft multiplayer server. A warm timber and copper workshop with one large illuminated window and a copper waterwheel, nestled in a dark spruce forest by a teal stream. A few grounded voxel-style blocks and an old rail lead into the workshop, evening fog in the treetops, tiny teal glow by the door. Original painterly block-world key art, cohesive with a woodland expedition theme. At 64 pixels it must read immediately as a warmly lit cabin, so one bold building centered at the middle of the square, with breathing room and strong silhouettes. Dark spruce and deep blue-green with copper highlights. No characters, no words, no game or Discord logos, no watermark, no border, no interface.
