import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch


sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'launcher'))
import official_launcher as official


class OfficialLauncherTests(unittest.TestCase):
    def test_registers_one_managed_profile_and_preserves_existing_data(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            vanilla = {'name': 'Vanilla', 'lastVersionId': 'latest-release'}
            original = {'version': 6, 'profiles': {'vanilla': vanilla}, 'settings': {'keepLauncherOpen': True}}
            for name in official.PROFILE_FILES:
                (root / name).write_text(json.dumps(original), encoding='utf-8')

            paths = official.profile_files(root)
            java = root / 'java.exe'
            java.with_name('javaw.exe').touch()
            values = official.profile_values(root / 'instance', '1.20.1-forge-47.4.28', java, 10)
            with patch.object(official, 'launcher_running', return_value=False):
                official.register_profiles(paths, values)
            self.assertTrue(official.profile_matches(paths, values))
            for path in paths:
                data = json.loads(path.read_text(encoding='utf-8'))
                self.assertEqual(data['profiles']['vanilla'], vanilla)
                self.assertEqual(data['settings'], original['settings'])
                self.assertEqual(len(data['profiles']), 2)
                self.assertEqual(data['profiles'][official.PROFILE_KEY]['javaDir'], str(java.with_name('javaw.exe').resolve()))

            before = [path.read_bytes() for path in paths]
            with patch.object(official, 'launcher_running', return_value=True):
                official.register_profiles(paths, values)
                with self.assertRaisesRegex(RuntimeError, 'Close Minecraft Launcher'):
                    official.register_profiles(paths, {**values, 'javaArgs': '-Xmx12G'})
            self.assertEqual([path.read_bytes() for path in paths], before)
