"""Progression integrity: real ingredient IDs, reachable tiers, and six disciplines."""
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'tools'))
from build_convergence_progression import ITEMS, recipes
from convergence_gameplay import STATIONS, recipe_type


def inputs(recipe):
    if recipe['type'] == 'minecraft:crafting_shapeless':
        return [i['item'] for i in recipe['ingredients']]
    return [i['content']['ingredient']['item'] for i in recipe['inputs']['item']]


def output(recipe):
    if recipe['type'] == 'minecraft:crafting_shapeless':
        return recipe['result']['item']
    return recipe['outputs']['item'][0]['content']['ingredient']['item']


class ConvergenceProgressionTest(unittest.TestCase):
    def test_generated_files_match_authoring_source(self):
        for name, recipe in recipes().items():
            actual = json.loads((ROOT/'pack/kubejs/data/wombatcraft/recipes/progression'/f'{name}.json').read_text())
            self.assertEqual(recipe, actual, name)

    def test_machine_capacity_and_no_finite_loot(self):
        for name, recipe in recipes().items():
            self.assertNotIn('minecraft:dragon_egg', inputs(recipe), name)
            if 'inputs' in recipe:
                self.assertLessEqual(len(recipe['inputs']['item']), 6, name)
                self.assertLessEqual(recipe['inputs']['forge_energy'][0]['content'], 4000000, name)
                self.assertLessEqual(recipe['inputs']['botania_mana'][0]['content'], 1000000, name)
                for stack in recipe['inputs']['item'] + recipe['outputs']['item']:
                    self.assertTrue(0 < stack['content']['count'] <= 64, name)

    def test_custom_progression_has_no_deadlock(self):
        custom = {'kubejs:'+name for name in ITEMS}
        reached = set()
        for _ in range(len(custom)):
            for recipe in recipes().values():
                if (set(inputs(recipe)) & custom) <= reached:
                    reached.add(output(recipe))
        self.assertFalse(custom - reached, custom - reached)

    def test_every_branch_is_required_and_has_a_utility_sink(self):
        data = recipes()
        # Follow the principal recipes, not the deliberately more expensive alternatives.
        producers = {output(r): r for n, r in data.items() if not n.endswith('_industrial') and output(r).startswith('kubejs:')}
        def ancestors(item):
            return {item} | set().union(*(ancestors(i) for i in inputs(producers[item]))) if item in producers else {item}
        required = ancestors('kubejs:convergium_ingot')
        for name in ('mechanized', 'arcane', 'stellar', 'ancient', 'primal', 'temporal'):
            core = 'kubejs:'+name+'_core'
            self.assertIn(core, required)
            self.assertTrue(any(core in inputs(r) and not output(r).startswith('kubejs:') for r in data.values()), name)

    def test_registry_references_when_local_runtime_available(self):
        registry = ROOT/'dist/server-smoke/item-registry.txt'
        if not registry.exists():
            self.skipTest('Exact release registry is checked by the dedicated server smoke test')
        known = set(registry.read_text().splitlines()) | {'kubejs:'+name for name in ITEMS}
        known |= {'wombatcraft:'+name for name in STATIONS}
        known |= {'wombatcraft_smp:'+name for name in ('research_terminal','field_projector','resonant_prospector','astralium_boots')}
        for name, recipe in recipes().items():
            self.assertFalse((set(inputs(recipe)) | {output(recipe)}) - known, name)

    def test_stations_do_not_lock_their_own_materials(self):
        custom = {'kubejs:'+name for name in ITEMS} | {'wombatcraft:'+name for name in STATIONS if name != 'convergence_forge'}
        available = {'wombatcraft:convergence_forge'}
        data = recipes()
        for _ in range(len(custom)+1):
            for r in data.values():
                requirements = set(inputs(r)) & custom
                if 'inputs' in r:
                    requirements.add('wombatcraft:'+next(k for k in STATIONS if recipe_type(k)==r['type']))
                if requirements <= available:
                    available.add(output(r))
        self.assertFalse(custom - available, custom - available)

    def test_no_ambiguous_machine_inputs(self):
        from collections import Counter
        data = [(n,r) for n,r in recipes().items() if 'inputs' in r]
        for i,(left,a) in enumerate(data):
            ca = Counter({x['content']['ingredient']['item']:x['content']['count'] for x in a['inputs']['item']})
            for right,b in data[i+1:]:
                if a['type'] != b['type']: continue
                cb = Counter({x['content']['ingredient']['item']:x['content']['count'] for x in b['inputs']['item']})
                self.assertFalse(ca <= cb or cb <= ca, (left,right))

    def test_station_definitions_use_their_own_models_and_recipe_types(self):
        import nbtlib
        for key in STATIONS:
            path=ROOT/'pack/config/wombatcraft-mbd2/multiblock'/(key+'.mb')
            definition=nbtlib.load(path,gzipped=False)['definition']
            self.assertEqual(str(definition['id']),'wombatcraft:'+key)
            self.assertEqual(str(definition['stateMachine']['root']['renderer']['value']['modelLocation']),'wombatcraft:block/'+key)
            self.assertEqual(str(definition['recipeLogicSettings']['recipeType']),recipe_type(key))


if __name__ == '__main__':
    unittest.main()
