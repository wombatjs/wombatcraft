"""Build Multiblocked2 projects from Wombatcraft's editable machine files."""
from pathlib import Path

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "quests/convergence-forge"
OUTPUT = ROOT / "pack/config/wombatcraft-mbd2"


def main():
    from convergence_gameplay import build_stations
    build_stations()


if __name__ == "__main__":
    main()
