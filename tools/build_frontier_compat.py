"""Supply replacements for optional integrations absent from Wombatcraft."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "pack/kubejs/data"

def save(namespace, name, data):
    path = ROOT / namespace / (name + ".json")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")

def loot(namespace, name, items, rolls=1, first_win=False):
    entries = [{"type": "minecraft:item", "name": item} for item in items]
    if first_win:
        for entry in entries:
            entry["conditions"] = [{"condition": "rctmod:defeat_count", "count": 1}]
    save(namespace, "loot_tables/" + name, {"pools": [{"rolls": rolls, "entries": entries}]})

def main():
    # The two badge providers are alternatives in RCT's own trainer rewards.
    # Keep both empty since neither provider is installed. League quests give supplies.
    for family in ("cobblebadges", "pkgbadges"):
        for step in range(1, 9):
            save("rctmod", f"loot_tables/generic/unique/{family}_{step}", {"pools": []})
    loot("rctmod", "generic/rare/trs", ["cobblemon:exp_candy_s", "cobblemon:great_ball", "cobblemon:revive"])
    loot("rctmod", "generic/unique/tms", ["cobblemon:exp_candy_l", "cobblemon:rare_candy"], first_win=True)

    loot("idas", "chests/labyrinth/if_labyrinth_tomb", [
        "idas:disc_fragment_slither", "minecraft:diamond", "minecraft:dune_armor_trim_smithing_template",
        "create:experience_nugget", "minecraft:totem_of_undying", "minecraft:golden_carrot"
    ], 4)
    loot("idas", "chests/haunted_manor/if_haunted_manor", [
        "minecraft:bone", "minecraft:cobweb", "minecraft:golden_apple",
        "minecraft:pumpkin_seeds", "minecraft:book", "supplementaries:rope"
    ], 4)
    root = "chests/dread_citadel/"
    loot("idas", root + "dread_citadel", ["minecraft:diamond_chestplate", "minecraft:diamond_pickaxe", "minecraft:golden_apple"], 2)
    loot("idas", root + "dread_citadel_library", ["minecraft:book", "minecraft:map", "minecraft:paper", "quark:permafrost"], 3)
    loot("idas", root + "dread_citadel_throne", ["minecraft:diamond_sword", "minecraft:diamond_helmet", "minecraft:golden_apple"], 2)
    save("idas", "integrated_structure_spawners/dread_citadel", {"mobs": [
        {"name": "minecraft:stray", "weight": 15},
        {"name": "minecraft:skeleton", "weight": 10},
    ]})

    # Optional biome mods can be absent. Some of the built-in camp structures
    # also refer to alternate spellings of these tags.
    for tag, biome in (
        ("byg_redwood_biomes", "byg:redwood_thicket"),
        ("bygmohogany_biomes", "byg:tropical_rainforest"),
    ):
        save("idas", "tags/worldgen/biome/has_structure/" + tag, {
            "replace": True, "values": [{"id": biome, "required": False}]
        })
    for alias, tag in (
        ("bopmahogany_biomes", "bopmohogany_biomes"),
        ("bygmahogany_biomes", "bygmohogany_biomes"),
        ("bygredwood_biomes", "byg_redwood_biomes"),
    ):
        save("idas", "tags/worldgen/biome/has_structure/" + alias, {
            "values": ["#idas:has_structure/" + tag]
        })

if __name__ == "__main__":
    main()
