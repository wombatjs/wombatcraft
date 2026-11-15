import json
from pathlib import Path
import sys
import tomllib
import unittest
import io
import zipfile

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from portal_patch import build_portal_patch

class RefinedReleaseTests(unittest.TestCase):
    def test_retired_mods_cannot_ship(self):
        lock=json.loads((ROOT/'mod-lock.json').read_text(encoding='utf-8'))
        ids={m['modId'] for r in lock['mods'] for m in r.get('metadata',{}).get('mods',[])}
        self.assertIn('tardis_refined',ids)
        self.assertFalse(ids & {'tardis','ntm_ip','oculus'})
        for p in (ROOT/'pack/config/ftbquests/quests/chapters').glob('*.snbt'):
            self.assertNotIn('tardis:',p.read_text(encoding='utf-8'))

    def test_client_patch_keeps_real_aurorian_handler(self):
        with zipfile.ZipFile(io.BytesIO(build_portal_patch('portal-client'))) as z:
            core=json.loads(z.read('META-INF/coremods.json'))
            self.assertEqual(set(core),{'portalChunkTracking'})
            self.assertFalse(any('aurorian' in n for n in z.namelist()))
        with zipfile.ZipFile(io.BytesIO(build_portal_patch())) as z:
            self.assertIn('coremods/aurorian_server_network.js',z.namelist())

    def test_tested_portal_settings_and_dimension_patch(self):
        portal=json.loads((ROOT/'pack/config/immersive_portals.json').read_text())
        self.assertTrue(portal['compatibilityRenderMode'])
        self.assertEqual(portal['maxPortalLayer'],1)
        self.assertEqual(portal['netherPortalMode'],'vanilla')
        cfg=tomllib.loads((ROOT/'pack/defaultconfigs/tardis_refined-server.toml').read_text())
        self.assertEqual(cfg['compatibility']['immersive_portals']['teleportation_mode'],'ITP')
        with zipfile.ZipFile(io.BytesIO(build_portal_patch('refined-dimension-sync'))) as z:
            self.assertEqual(json.loads(z.read('pack.mcmeta'))['pack']['pack_format'],15)
            for script in json.loads(z.read('META-INF/coremods.json')).values():
                self.assertIn(script,z.namelist())
