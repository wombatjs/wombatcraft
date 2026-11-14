import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class DimensionQuestTests(unittest.TestCase):
    def test_every_packaged_dimension_has_an_atlas_entry(self):
        inventory = json.loads((ROOT / 'quests/dimension-inventory.json').read_text())
        atlas = json.loads((ROOT / 'docs/dimension-atlas.json').read_text())
        self.assertEqual(set(inventory), {d['dimension'] for d in atlas if d['dimension']})
        for destination in atlas:
            self.assertTrue((ROOT / 'pack/config/ftbquests/quests/chapters' / (destination['chapter']+'.snbt')).is_file())

    def test_adventure_destinations_have_real_arrival_tasks(self):
        atlas = json.loads((ROOT / 'docs/dimension-atlas.json').read_text())
        chapters = [json.loads(p.read_text()) for p in (ROOT / 'pack/config/ftbquests/quests/chapters').glob('*.snbt')]
        tasks = [t for c in chapters for q in c['quests'] for t in q['tasks']]
        arrivals = {t['dimension'] for t in tasks if t['type']=='dimension'}
        # Earlier Ad Astra milestones use the mod's arrival advancements.
        arrivals |= {t['advancement'] for t in tasks if t['type']=='advancement' and t['advancement'] in {'ad_astra:moon','ad_astra:mars','ad_astra:venus'}}
        required = {d['dimension'] for d in atlas if d['dimension']} - {'minecraft:overworld','graveyard:past'}
        self.assertLessEqual(required, arrivals)
        for c in chapters:
            if c['filename'].startswith('wc_dimension') or c['filename'] in {'wc_twilight_forest','11_ad_astra','28_pocket_worlds','01_tardis'}:
                self.assertEqual(c['group'], '3E91144EB73CBA91')
