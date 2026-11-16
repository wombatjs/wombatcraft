# Everyday utilities and town supplies

Adds Crafting on a Stick, Tool Belt, OpenBlocks Elevator, Torchmaster, Simply Light, Ender Storage, Inventory Essentials, Pocket Storage, Charging Gadgets, Mining Gadgets and GraveStone. Exact upstream files are pinned in `mod-lock.json`. No jar is redistributed in the repository.

Seventy original quests across eleven chapters cover setup, upgrades, practical uses, ownership and death rules. Crafting-on-a-stick uses Ctrl+V; the Tool Belt wheel uses Alt+R. Existing custom bindings are preserved. Inventory Essentials sorting starts unbound to avoid conflicting with the pack's existing inventory controls.

## Multiplayer rules

- Graves can be looted by anyone with permission to break at the grave's location. Sneak pickup is disabled; normal FTB claim protection remains authoritative.
- Arena deaths bypass grave creation and retain ordinary item drops. Outside the arena, a grave is attempted only in air with permission to edit the destination and support block. Failed placement retains ordinary drops. Graves cannot forcibly replace terrain; ghosts are disabled.
- Elevators check FTB interaction permission at both endpoints, require matching colours and have a 64-block range. They cannot be used in the arena or for 90 seconds after player-inflicted damage.
- Ender Pouches are blocked in the arena and during the same PvP lockout. Ender Storage menus close when PvP damage starts. Public colour channels remain deliberately public; private channels require the mod's ownership lock, not just a land claim.
- Mining Gadgets' original block-break events remain intact. Additional guards check light placement, initial render-block placement and neighbouring liquid conversions against FTB edit permission. The arena's existing terrain guard remains in force. Survival recipes above 3x3 mining are removed.
- Mega Torches and Dread Lamps have a 32-block radius. Placement checks the affected chunk claims and rejects overlap with land the placer cannot edit. Natural-spawn-only suppression stays enabled. Feral Flare Lantern light generation is disabled and its recipe is removed because automatic invisible-light writes lack player attribution. Ordinary Simply Light fixtures remain available.

The new guards act on utility use, placement, death or damage. They do not scan worlds, load chunks, or add per-player tick handlers. The small combat map is pruned once per minute and cleared when the server stops. These choices limit added work; they are not a measured performance guarantee.

## Town shop

The shop contains 1,035 distinct purchasable vanilla item IDs, including 893 new supply entries. Thirty category/page entries cover essentials, wood, stone, coloured blocks, gardens, materials, Nether/End building, ocean supplies, equipment and transport. The selector has pagination; Lightman's native trader screens handle payments and stock menus.

Creative/admin items, spawn eggs, smithing templates, pottery sherds, music discs and rare progression rewards are excluded. New stock is sale-only. Existing curated bulk and valuables buybacks remain unchanged; the server does not buy arbitrary farm output. Direct trading opens only inside the configured spawn claim and outside PvP lockout. The detailed stock list is `vanilla-shop-stock.json`; `tools/expand_vanilla_shop.py` regenerates it from an item registry.

## Validation scope

The requested release skips local gameplay testing and does not open Minecraft on the user's PC. Java compilation, JSON/SNBT validation, existing unit checks and the normal GitHub release gates still apply. Two-player claim boundaries, death/grave inventory recovery, UI interaction and loaded-server performance have not been manually verified for these additions. The arena guard is prepared for the arena dimension; unfinished duels/logout bodies are not enabled by this update.
