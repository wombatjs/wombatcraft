"""Release wiring must retain the pack's existing protection patches."""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'mods/wombatcraft-smp/src/main/resources'

class PersonalProgressReleaseTest(unittest.TestCase):
    def test_all_companion_coremods_are_registered(self):
        manifest=json.loads((RES/'META-INF/coremods.json').read_text())
        scripts={p.relative_to(RES).as_posix() for p in (RES/'coremods').glob('*.js')}
        self.assertEqual(set(manifest.values()),scripts)
        self.assertIn('wombatcraft_personal_progress',manifest)
        for name in ('faction_policy','rooms_guard','utility_guards','fossil_food','arena_terrain'):
            self.assertIn('wombatcraft_'+name,manifest)

    def test_research_quest_copy_describes_personal_ownership(self):
        chapters=ROOT/'pack/config/ftbquests/quests/chapters'
        workshop=json.loads((chapters/'wc_convergence_workshop.snbt').read_text())
        terminal=next(q for q in workshop['quests'] if q['title']=='Research Terminal')
        text=' '.join(terminal['description'])
        self.assertIn('Knowledge belongs to you',text)
        self.assertNotIn('current FTB team',text)

if __name__=='__main__':unittest.main()
