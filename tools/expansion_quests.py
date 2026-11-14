"""Compile authored expedition routes into stable FTB Quests records."""
import json
from pathlib import Path
from quest_layout import mark

GROUP = '3E91144EB73CBA91'


def add_expansion(book, uid):
    source = json.loads((Path(__file__).resolve().parents[1] / 'quests/expansion.json').read_text(encoding='utf-8'))
    for order, route in enumerate(source['chapters'], 20):
        nodes = {n['key']: n for n in route['nodes']}
        depths = {}
        def depth(key):
            if key not in depths:
                parents = [p for p in nodes[key]['parents'] if p in nodes]
                depths[key] = 1 + max((depth(p) for p in parents), default=-1)
            return depths[key]
        for key in nodes:
            depth(key)
        positions = {}
        row = 0
        for level in range(max(depths.values(), default=0) + 1):
            layer = [key for key in nodes if depths[key] == level]
            def parent_center(key):
                points = [positions[p][0] for p in nodes[key]['parents'] if p in positions]
                return sum(points) / len(points) if points else 0
            layer.sort(key=parent_center)
            for start in range(0, len(layer), 8):
                band = layer[start:start+8]
                for column, key in enumerate(band):
                    positions[key] = (column - (len(band)-1)/2, row)
                row += 1
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
