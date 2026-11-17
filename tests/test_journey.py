import copy
import json
import sys
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from build_journey import build, GENERATED, CHAPTERS

class JourneyTest(unittest.TestCase):
    def setUp(self):
        self.book={p.stem:json.loads(p.read_text(encoding='utf-8')) for p in CHAPTERS.glob('*.snbt')}

    def test_original_content_is_unchanged(self):
        before=copy.deepcopy(self.book)
        catalog,inventory=build(self.book)
        self.assertEqual(len(catalog),75)
        self.assertEqual(len(inventory),3135)
        for name in before.keys()-GENERATED:
            self.assertEqual(before[name],self.book[name],name)

    def test_generation_is_deterministic_and_matches_checked_in_files(self):
        before=copy.deepcopy(self.book)
        first=build(self.book)
        self.assertEqual(first,build(self.book))
        self.assertEqual(before,self.book)
        path=ROOT/'mods/wombatcraft-smp/src/main/resources/data/wombatcraft_smp/journey/milestones.json'
        self.assertEqual(first[0],json.loads(path.read_text(encoding='utf-8')))

    def test_links_reuse_existing_identities_without_rewards(self):
        catalog,_=build(self.book)
        original={q['id'] for name,c in self.book.items() if name not in GENERATED for q in c['quests']}
        links=[link for name in GENERATED for link in self.book[name]['quest_links']]
        self.assertEqual(len(links),94)
        self.assertEqual(len({m['quest'] for m in catalog}),75)
        for link in links:
            self.assertIn(link['linked_quest'],original)
            self.assertNotIn('tasks',link)
            self.assertNotIn('rewards',link)
        for name in GENERATED:
            for q in self.book[name]['quests']:
                self.assertFalse(q['rewards'])
                self.assertFalse(q.get('dependencies'))
                self.assertTrue(q['optional'])

    def test_ambiguous_or_removed_source_fails(self):
        quests=self.book['wc_create']['quests']
        original=next(q for q in quests if q['title']=='Water Wheel')
        quests.append(copy.deepcopy(original))
        with self.assertRaises(ValueError):build(self.book)
        quests[:]=[q for q in quests if q['title']!='Water Wheel']
        with self.assertRaises(ValueError):build(self.book)

if __name__=='__main__':unittest.main()
