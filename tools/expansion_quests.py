"""Compile authored expedition routes into stable FTB Quests records."""
import json
from pathlib import Path
from quest_layout import mark

GROUP = '3E91144EB73CBA91'


def add_expansion(book, uid):
    source = json.loads((Path(__file__).resolve().parents[1] / 'quests/expansion.json').read_text(encoding='utf-8'))
    for order, route in enumerate(source['chapters'], 20):
        nodes = {n['key']: n for n in route['nodes']}
        children = {key: [] for key in nodes}
        roots = []
        for key, node in nodes.items():
            local = [p for p in node['parents'] if p in nodes]
            if local:
                children[local[0]].append(key)
            else:
                roots.append(key)
        # Each subtree has its own strip; siblings spread out below a shared
        # parent. Long routes grow down the page instead of crossing other lanes.
        widths = {}
        def width(key):
            widths[key] = max(1, sum(width(k) for k in children[key]))
            return widths[key]
        for root in roots:
            width(root)
        positions = {}
        def place(key, left, depth):
            positions[key] = (left + (widths[key] - 1) / 2, depth)
            cursor = left
            for child in children[key]:
                place(child, cursor, depth + 1)
                cursor += widths[child]
        # Independent research lanes are arranged in bands of at most 8 leaves.
        left = 0
        top = 0
        bottom = 0
        for root in roots:
            if left and left + widths[root] > 8:
                left = 0
                top = bottom + 2
            place(root, left, top)
            bottom = max(bottom, max(y for x, y in positions.values()))
            left += widths[root] + 1
        chapter = dict(filename=route['key'], id=uid('chapter/' + route['key']),
                       title=route['title'], icon=route['icon'], group=GROUP,
                       order_index=order, default_quest_shape='circle', quests=[])
        for node in route['nodes']:
            key = 'expedition/' + node['key']
            kind = node['kind']
            task = dict(id=uid(key + '/task'), type=kind)
            if kind == 'checkmark':
                task['title'] = 'Record the completed project'
            elif kind == 'kill':
                task.update(entity=node['target'], value=node.get('count', 1))
            else:
                task[kind] = node['target']
                if kind == 'item':
                    task['count'] = node.get('count', 1)
            quest = dict(id=uid(key), title=node['title'], description=[node['note']],
                         icon=node.get('icon') or route['icon'], tasks=[task], optional=True,
                         dependencies=[uid('expedition/' + p) for p in node['parents']])
            challenge = node.get('challenge', False)
            x, y = positions[node['key']]
            mark(quest, x * 2.5, y * 2.5,
                 'diamond' if kind == 'dimension' else 'hexagon' if challenge else 'rsquare' if kind == 'checkmark' else 'circle',
                 1.3 if challenge or kind == 'dimension' else 1)
            if kind != 'checkmark':
                quest['rewards'] = [dict(id=uid(key + '/xp'), type='xp', xp=250 if challenge else 60)]
                reward = 'minecraft:diamond' if challenge else 'minecraft:golden_carrot' if kind in ('dimension', 'structure') else 'minecraft:experience_bottle'
                quest['rewards'].append(dict(id=uid(key + '/supplies'), type='item', item=reward, count=2 if challenge else 4 if kind == 'dimension' else 1))
            chapter['quests'].append(quest)
        book[route['key']] = chapter
    return [(d['title'], d['dimension'], d['chapter'], d['description'], book[d['chapter']]['icon']) for d in source['destinations']]
