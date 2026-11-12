import hashlib,json,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'launcher'))
import updater

class UpdateTests(unittest.TestCase):
 def test_reject_paths_that_can_touch_saves_or_escape(self):
  for path in ['../world/level.dat','saves/world/level.dat','C:/mods/a.jar','mods/../../secret','mods\\a.jar','/mods/a.jar','server.properties']:
   with self.subTest(path=path),self.assertRaises(ValueError):updater.safe_path(Path('instance'),path)
 def test_update_preserves_world_settings_and_unmanaged_files(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp);instance=root/'game';stage=root/'stage';instance.mkdir();(instance/'saves').mkdir();(instance/'saves/world.dat').write_text('player world');(instance/'options.txt').write_text('my settings');(instance/'mods').mkdir();(instance/'mods/personal.jar').write_text('personal');(instance/'mods/old.jar').write_text('old');(instance/'.wombatcraft.json').write_text(json.dumps({'managed':['mods/old.jar'],'commit':'old'}));(stage/'mods').mkdir(parents=True);(stage/'mods/new.jar').write_text('new');(stage/'options.txt').write_text('defaults')
   updater.activate({'commit':'new'},[{'path':'mods/new.jar'},{'path':'options.txt','seed_only':True}],stage,instance,root/'transaction')
   self.assertEqual((instance/'saves/world.dat').read_text(),'player world');self.assertEqual((instance/'options.txt').read_text(),'my settings');self.assertTrue((instance/'mods/personal.jar').exists());self.assertFalse((instance/'mods/old.jar').exists());self.assertEqual((instance/'mods/new.jar').read_text(),'new')
 def test_failed_activation_restores_previous_pack(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp);instance=root/'game';stage=root/'stage';(instance/'config').mkdir(parents=True);(stage/'config').mkdir(parents=True)
   (instance/'config/a.txt').write_text('old');(stage/'config/a.txt').write_text('new');(stage/'config/b.txt').write_text('new b');old={'commit':'old','managed':['config/a.txt']};(instance/'.wombatcraft.json').write_text(json.dumps(old))
   real=updater.os.replace
   def fail(source,target):
    if str(target).endswith('b.txt'):raise OSError('simulated disk failure')
    return real(source,target)
   with patch.object(updater.os,'replace',side_effect=fail),self.assertRaises(OSError):updater.activate({'commit':'new'},[{'path':'config/a.txt'},{'path':'config/b.txt'}],stage,instance,root/'transaction')
   self.assertEqual((instance/'config/a.txt').read_text(),'old');self.assertFalse((instance/'config/b.txt').exists());self.assertEqual(json.loads((instance/'.wombatcraft.json').read_text()),old)
 def test_symlink_escape_is_rejected(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp);instance=root/'game';instance.mkdir();outside=root/'outside';outside.mkdir()
   try:(instance/'mods').symlink_to(outside,target_is_directory=True)
   except OSError:self.skipTest('Symlinks unavailable')
   with self.assertRaises(ValueError):updater.safe_path(instance,'mods/secret.jar')

if __name__=='__main__':unittest.main()
