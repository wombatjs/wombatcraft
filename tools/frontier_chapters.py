"""Compile the authored Trials, Forge and new side routes into FTB Quests."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GROUPS = {
    "magic": "065A019E212476CF",
    "exploration": "00AA8F374274B5F3",
    "convergence": "0F095C8E4FA42163",
}


def add_frontier_chapters(book, uid, registry):
    source = json.loads((ROOT / "quests/frontier_chapters.json").read_text(encoding="utf-8"))
    for order, chapter in enumerate(source, 60):
        filename = "wc_frontier_" + chapter["key"]
        quests = []
        for node in chapter["quests"]:
            key = f"frontier/{chapter['key']}/{node['key']}"
            tasks = []
            items = {node.get("icon", chapter["icon"])}
            for index, (kind, target, count) in enumerate(node.get("tasks", [])):
                task = {"id": uid(f"{key}/task/{index}"), "type": kind}
                if kind == "checkmark":
                    task["title"] = target
                else:
                    task[kind] = target
                    if kind == "item":
                        task.update(count=count, consume_items=False)
                        items.add(target)
                tasks.append(task)
            rewards = [
                {"id": uid(f"{key}/reward/{index}"), "type": "item", "item": item, "count": count}
                for index, (item, count) in enumerate(node["rewards"])
            ]
            items.update(reward["item"] for reward in rewards)
            missing = items - registry
            assert not missing, f"{key} references missing items: {missing}"
            x, y = node["position"]
            quests.append({
                "id": uid(key), "title": node["title"], "icon": node.get("icon", chapter["icon"]),
                "description": node["description"], "subtitle": node.get("subtitle", ""),
                "x": x, "y": y, "shape": node.get("shape", "circle"),
                "size": 1.3 if node.get("shape") in ("hexagon", "diamond") else 1.05,
                "optional": True,
                "dependencies": [uid(f"frontier/{parent if '/' in parent else chapter['key'] + '/' + parent}")
                                 for parent in node["parents"]],
                "tasks": tasks, "rewards": rewards,
            })
        book[filename] = {
            "id": uid(f"chapter/{filename}"), "filename": filename, "title": chapter["title"],
            "group": GROUPS[chapter["group"]], "icon": chapter["icon"], "order_index": order,
            "default_quest_shape": "circle", "autofocus_id": quests[0]["id"], "quests": quests,
        }
