"""Build Multiblocked2 projects from Wombatcraft's editable machine files."""
from pathlib import Path

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "quests/convergence-forge"
OUTPUT = ROOT / "pack/config/wombatcraft-mbd2"


def main():
    for name, directory, suffix in (
        ("controller", "multiblock", "mb"),
        ("recipe_type", "recipe_type", "rt"),
    ):
        tag = nbtlib.parse_nbt((SOURCE / f"{name}.snbt").read_text(encoding="utf-8"))
        path = OUTPUT / directory / f"convergence_forge.{suffix}"
        path.parent.mkdir(parents=True, exist_ok=True)
        nbtlib.File(tag, gzipped=False).save(path)


if __name__ == "__main__":
    main()
