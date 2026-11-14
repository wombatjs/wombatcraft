# Wombatcraft Trials and Convergence Forge

The six Forge 1.20.1 additions bring 75 quests across six chapters. Trials and the Forge are under Convergence. Mana and Artifice is under Combat and Magic. Cobblemon League, Ruins and Expeditions, and The Frontier in Focus are under Exploration.

## Trials and Forge

Open trial pearls in clear terrain, at least 48 blocks from another active gate. Each of the three encounters has three waves, a timed round and a completion crest. The crests are shared drops. Teammates can use FTB Teams to share quest progress. Keep the crests until the quest records all three.

Build the Forge in a vertical plane with its controller facing out. Read the rows from above to below:

    Crying Obsidian   Brass Block   Crying Obsidian
    Brass Block       Controller    Brass Block

Connect a cable to the controller and bind a Mana Spreader to it. The Forge requires both stored FE and mana before a recipe starts.

| Product | Inputs | FE | Mana |
| --- | --- | ---: | ---: |
| 4 Convergence Alloy | 8 brass ingots, 1 terrasteel, 2 ostrum | 200,000 | 12,000 |
| 1 Convergence Lens | 2 alloy, 8 ambrosium, 2 mana diamonds | 400,000 | 30,000 |
| 1 Convergence Keystone | 1 lens, 3 distinct crests, 2 calorite, 1 Gaia ingot | 1,000,000 | 75,000 |

A lens can also become four Warp Stones. A Keystone can become a beacon. JEI shows the pearl and Forge recipes.

## Maintenance

- Quest source is `quests/frontier_chapters.json`. Run the existing quest compiler to regenerate the six chapters. Their authored rewards are retained by the general reward pass.
- `tools/build_trials.py` generates the gateway definitions and their entry recipes. `tools/build_convergence_forge.py` compiles the two editable SNBT project files into the machine files. `tools/build_frontier_compat.py` supplies loot tables for optional integrations whose mods are absent.
- Multiblocked2 is pinned to 1.0.39. Version 1.0.40 loads a client-only KubeJS renderer on dedicated servers.
- The server smoke test assembles the Forge in a disposable world, runs its three recipes, checks input consumption, and spawns and clears all three trial encounters to check crest drops. New IDAS structures appear in freshly generated terrain.
