"""Arrange the quest book around the routes players can take."""

import collections
import re


def chapter_key(title):
    return "wc_" + re.sub(r"[^a-z0-9]+", "_", title.lower())


def mark(quest, x, y, shape=None, size=None):
    quest.update(x=round(x, 2), y=round(y, 2))
    if shape:
        quest["shape"] = shape
    if size:
        quest["size"] = size


def lay_out_lanes(chapter, lanes):
    """Draw a foundation across the top and keep descendant routes together."""
    by_item = {q["tasks"][0]["item"]: q for q in chapter["quests"]}
    owners = {item: index for index, lane in enumerate(lanes) for item in lane["items"]}
    children = collections.defaultdict(list)
    for index, lane in enumerate(lanes):
        children[owners.get(lane["parent"])].append(index)

    def branch(index, side, row):
        lane = lanes[index]
        quests = [by_item[item] for item in lane["items"]]
        for step, quest in enumerate(quests):
            offset = step % 6
            band = step // 6
            if band % 2:
                offset = 5 - offset
            x = side * (1.8 + offset * 1.55)
            y = row + band * 1.55
            if step == 0:
                mark(quest, x, y, "rsquare", 1.22)
                quest["subtitle"] = lane["title"]
            elif step == len(quests) - 1:
                mark(quest, x, y, "hexagon", 1.24)
            else:
                mark(quest, x, y, "circle", 0.9)
        next_row = row + (1.55 if len(quests) > 6 else 0) + 2.35
        for child in children[index]:
            next_row = branch(child, side, next_row)
        return next_row

    roots = children[None]
    foundation = [by_item[item] for item in lanes[roots[0]]["items"]]
    center = (len(foundation) - 1) / 2
    for step, quest in enumerate(foundation):
        mark(quest, (step - center) * 1.65, 0, "octagon" if step == 0 else "rsquare",
             1.5 if step == 0 else 1.05)
    chapter["icon"] = foundation[0]["icon"]
    chapter["autofocus_id"] = foundation[0]["id"]
    rows = {-1: 3.0, 1: 3.0}
    for index in children[roots[0]] + roots[1:]:
        side = min(rows, key=rows.get)
        rows[side] = branch(index, side, rows[side])


def lay_out_atlas(chapter):
    """Fit the small addon routes into a navigable atlas of separate clusters."""
    rows = collections.defaultdict(list)
    for quest in chapter["quests"]:
        rows[quest["y"]].append(quest)
    columns = 3 if len(rows) >= 12 else 2
    for index, (_, route) in enumerate(sorted(rows.items())):
        col, row = index % columns, index // columns
        center_x = (col - (columns - 1) / 2) * 10.0
        center_y = row * 4.7
        route.sort(key=lambda q: q["x"])
        for step, quest in enumerate(route):
            x = center_x if step == 0 else center_x + (step - len(route) / 2) * 1.55
            y = center_y if step == 0 else center_y + 1.8
            mark(quest, x, y, "octagon" if step == 0 else "rsquare" if step == len(route) - 1 else "circle",
                 1.28 if step == 0 else 0.94)
    chapter["autofocus_id"] = min(chapter["quests"], key=lambda q: (q["y"], abs(q["x"])))["id"]


def lay_out_authored_map(chapter, positions):
    quests = {quest["title"]: quest for quest in chapter["quests"]}
    for title, position in positions.items():
        mark(quests[title], *position)
    chapter["icon"] = quests[next(iter(positions))]["icon"]
    chapter["autofocus_id"] = quests[next(iter(positions))]["id"]
