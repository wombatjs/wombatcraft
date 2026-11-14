from pathlib import Path
import sys
import tempfile
import unittest
import nbtlib

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'launcher'))
import server_list
import discord_presence


class CommunitySetupTests(unittest.TestCase):
    def test_server_list_preserves_other_servers_and_deduplicates_official(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            entry = lambda name, ip: nbtlib.Compound(name=nbtlib.String(name), ip=nbtlib.String(ip))
            data = nbtlib.File({'servers': nbtlib.List[nbtlib.Compound]([
                entry('Friends', 'friends.example'), entry('Old official', '74.208.117.88'),
                entry('Duplicate', '74.208.117.88:25565')]), 'custom': nbtlib.Int(42)})
            data.save(root / 'servers.dat', gzipped=False)
            server_list.ensure_official(root)
            server_list.ensure_official(root)
            result = nbtlib.load(root / 'servers.dat', gzipped=False)
            self.assertEqual([str(x['ip']) for x in result['servers']], ['74.208.117.88:25565', 'friends.example'])
            self.assertEqual(result['custom'], 42)

    def test_corrupt_server_list_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'servers.dat').write_bytes(b'not-nbt')
            with self.assertRaises(RuntimeError):
                server_list.ensure_official(root)
            self.assertEqual((root / 'servers.dat').read_bytes(), b'not-nbt')

    def test_presence_migration_preserves_opt_out_and_custom_servers(self):
        import tomllib
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'simple-rpc.toml').write_text('[general]\napplicationID="318787432738717696"\nenabled=false\n')
            (root / 'server-entries.toml').write_text('enabled=false\nversion=3\n[[entry]]\nip="friends.example"\n')
            discord_presence.upgrade_game_config(root, ROOT / 'pack/config/simple-rpc')
            current = tomllib.loads((root / 'simple-rpc.toml').read_text())
            self.assertFalse(current['general']['enabled'])
            servers = tomllib.loads((root / 'server-entries.toml').read_text())
            self.assertFalse(servers['enabled'])
            self.assertIn('friends.example', [x['ip'] for x in servers['entry']])
            self.assertTrue((root / '.wombatcraft-v2').exists())
