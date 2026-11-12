import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile


sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'launcher'))
import launcher_update as update


class LauncherUpdateTests(unittest.TestCase):
    def test_reject_unsafe_archive_before_extracting(self):
        for names in (
            ('Wombatcraft/../settings.json',),
            ('Wombatcraft/aux.txt',),
            ('Wombatcraft/_internal/file.dll', 'Wombatcraft/_internal/FILE.dll'),
            ('Wombatcraft/_internal/file.dll:stream',),
        ):
            with self.subTest(names=names), tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                archive = root / 'test.zip'
                with zipfile.ZipFile(archive, 'w') as bundle:
                    for name in names:
                        bundle.writestr(name, b'test')
                with self.assertRaises(ValueError):
                    update._extract(archive, root)
                self.assertFalse((root / 'Wombatcraft').exists())

    def test_reject_mismatched_asset_checksum(self):
        asset = {
            'browser_download_url': 'https://github.com/test.zip',
            'size': 3,
            'digest': 'sha256:' + '0' * 64,
        }
        response = io.BytesIO(b'zip')
        response.geturl = lambda: asset['browser_download_url']
        with patch.object(update, '_request', return_value=response):
            with self.assertRaisesRegex(ValueError, 'checksum'):
                update._download(asset, io.BytesIO())

    def test_failed_restart_preserves_previous_version(self):
        previous = update.Build('a' * 40, 4)
        candidate = update.Build('b' * 40, 5)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            active = root / 'active.json'
            active.write_text(json.dumps(previous.metadata()), encoding='utf-8')
            with patch.object(update, 'start', side_effect=OSError('start failed')):
                with self.assertRaises(OSError):
                    update.activate(root, candidate, root / candidate.commit / update.EXE_NAME)
            self.assertEqual(update.read_build(active), previous)

    def test_old_shortcut_follows_installed_update_but_newer_build_stays(self):
        installed = update.Build('b' * 40, 5)
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'active.json').write_text(json.dumps(installed.metadata()), encoding='utf-8')
            slot = root / installed.commit
            (slot / '_internal').mkdir(parents=True)
            (slot / '_internal' / update.BUILD_FILE).write_text(json.dumps(installed.metadata()), encoding='utf-8')
            executable = slot / update.EXE_NAME
            executable.touch()
            self.assertEqual(update.active_executable(root, update.Build('a' * 40, 4)), executable)
            self.assertIsNone(update.active_executable(root, update.Build('c' * 40, 6)))
