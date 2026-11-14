"""Generate the authored gateway encounters and their matching pearl recipes."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "pack/kubejs/data/wombatcraft"
TRIALS = {
    "twilight": {
        "name": "Trial of the Thornwood",
        "color": "#8BBB6C",
        "key": ["twilightforest:ironwood_ingot", "twilightforest:fiery_ingot"],
        "waves": [
            {"twilightforest:kobold": 5, "twilightforest:skeleton_druid": 2},
            {"twilightforest:redcap": 4, "twilightforest:redcap_sapper": 2, "twilightforest:wraith": 2},
            {"twilightforest:wraith": 4, "twilightforest:skeleton_druid": 3},
        ],
        "supplies": "minecraft:arrow",
    },
    "aether": {
        "name": "Trial of the High Sky",
        "color": "#8DD6ED",
        "key": ["aether:ambrosium_shard", "aether:enchanted_gravitite"],
        "waves": [
            {"aether:cockatrice": 4, "aether:sentry": 2},
            {"aether:cockatrice": 4, "aether:sentry": 4},
            {"aether:cockatrice": 4, "aether:sentry": 5},
        ],
        "supplies": "minecraft:golden_carrot",
    },
    "otherside": {
        "name": "Trial of the Echo",
        "color": "#68CBC7",
        "key": ["deeperdarker:resonarium_plate", "deeperdarker:reinforced_echo_shard"],
        "waves": [
            {"deeperdarker:shattered": 4, "deeperdarker:sculk_centipede": 3},
            {"deeperdarker:sculk_snapper": 3, "deeperdarker:shattered": 4},
            {"deeperdarker:stalker": 2, "deeperdarker:sculk_snapper": 4, "deeperdarker:sculk_centipede": 2},
        ],
        "supplies": "minecraft:golden_carrot",
    },
}


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8")


def reward(item, count):
    return {"type": "gateways:stack", "stack": {"item": item, "count": count}}


def main():
    translations = dict.fromkeys(["block.wombatcraft.convergence_forge",
                                  "item.wombatcraft.convergence_forge", "wombatcraft.convergence_forging"], "Convergence Forge")
    for key, trial in TRIALS.items():
        name = f"trial_{key}"
        translations[f"wombatcraft.{name}"] = trial["name"]
        waves = [
            {
                "entities": [{"entity": entity, "count": count} for entity, count in enemies.items()],
                "modifiers": [
                    {"attribute": "generic.max_health", "operation": "MULTIPLY_TOTAL", "value": 0.20 * index}
                ] if index else [],
                "rewards": [reward(trial["supplies"], 4)] if index == 1 else [],
                "max_wave_time": 2400,
                "setup_time": 120,
            }
            for index, enemies in enumerate(trial["waves"])
        ]
        write(DATA / "gateways" / f"{name}.json", {
            "size": "medium",
            "color": trial["color"],
            "waves": waves,
            "rewards": [reward(f"kubejs:{name}_crest", 1)],
            "rules": {
                "player_damage_only": True,
                "spacing": 48,
                "spawn_range": 8,
                "leash_range": 28,
                "remove_mobs_on_failure": True,
            },
        })
        outer, center = trial["key"]
        write(DATA / "recipes/trials" / f"{name}.json", {
            "type": "gateways:gate_recipe",
            "group": "wombatcraft_trials",
            "pattern": [" O ", "EKE", " O "],
            "key": {"O": {"item": outer}, "E": {"item": "minecraft:ender_pearl"}, "K": {"item": center}},
            "result": {"item": "gateways:gate_pearl"},
            "gateway": f"wombatcraft:{name}",
        })
    write(ROOT / "pack/kubejs/assets/wombatcraft/lang/en_us.json", translations)


if __name__ == "__main__":
    main()
