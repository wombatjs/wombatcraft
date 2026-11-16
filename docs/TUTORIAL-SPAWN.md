# Tutorial arrivals

Stand on the exact safe landing spot, facing the direction new players should face, and run as an operator:

```text
/settutorialspawn
```

New accounts joining this world for the first time will arrive there once. Accounts already present in the world's player data are recognized as returning players when this feature is installed. Joining before a tutorial point is configured still counts as a first visit; setting a point later does not relocate that player.

Every player can return using:

```text
/tutorial
```

The command takes five seconds, cancels on movement or damage, and cannot be used while riding, sleeping, inside the arena or within 60 seconds of player combat. It does not change world spawn, beds, respawn anchors or death respawn locations. The target dimension, coordinates and facing persist in the world's saved data. If the landing becomes obstructed, travel is refused instead of placing the player inside blocks. Reset it by standing somewhere safe and repeating `/settutorialspawn`.

The location is intentionally not chosen during installation: the server owner places it after building the tutorial.

## Easy NPC

Easy NPC Bundle, Core and Config UI are pinned to Forge 1.20.1 version 7.19.0 and shipped to clients and the server. The bundle alone is not sufficient. NPC creation/configuration is available through the mod's spawn eggs, configuration wand and `/easy_npc` command tree. Only authorized administrators should configure NPC actions.

The existing mayor and secretary remain persistent civic villagers with automatic election bindings. Installing Easy NPC does not replace or erase existing spawn NPCs.
