import errno,hashlib,io,json,sys,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'launcher'))
import updater

class UpdateTests(unittest.TestCase):
 def test_download_fails_over_and_verifies_the_same_hash(self):
  with tempfile.TemporaryDirectory() as tmp:
   target=Path(tmp)/'cached'
   contents=b'complete jar'
   primary='https://primary.example/test.jar'
   fallback='https://mirror.example/test.jar'
   record={'url':primary,'fallback_urls':[fallback],
           'size':len(contents),'sha512':hashlib.sha512(contents).hexdigest()}
   def open_url(request, **options):
    if request.full_url == primary:
     raise ConnectionAbortedError()
    self.assertEqual(request.full_url,fallback)
    return io.BytesIO(contents)
   with patch.object(updater.time,'sleep'),patch.object(
    updater.urllib.request,'urlopen',side_effect=open_url,
   ) as request:
    updater.download(record,target)
   self.assertEqual(request.call_count,4)
   self.assertEqual(target.read_bytes(),contents)

 def test_pack_download_retries_truncated_and_reset_connections(self):
  with tempfile.TemporaryDirectory() as tmp:
   target=Path(tmp)/'cached'
   contents=b'complete jar'
   record={'path':'mods/example.jar','url':'https://example.com/example.jar',
           'size':len(contents),'sha512':hashlib.sha512(contents).hexdigest()}
   with patch.object(updater.time,'sleep'),patch.object(
    updater.urllib.request,'urlopen',
    side_effect=[ConnectionAbortedError(),io.BytesIO(contents[:4]),io.BytesIO(contents)],
   ) as request:
    updater.download(record,target)
   self.assertEqual(request.call_count,3)
   self.assertEqual(target.read_bytes(),contents)
   self.assertFalse(target.with_suffix('.part').exists())
   with patch.object(updater.time,'sleep'),patch.object(
    updater.urllib.request,'urlopen',side_effect=ConnectionAbortedError(),
   ),self.assertRaisesRegex(updater.DownloadInterrupted,'example.jar'):
    updater.download(record,Path(tmp)/'other')

 def test_controls_migrate_once_without_overwriting_custom_bindings(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp)
   instance=root/'game';instance.mkdir()
   stage=root/'stage';stage.mkdir()
   options=instance/'options.txt'
   options.write_bytes(
    b'renderDistance:12\r\nkey_key.ftbchunks.map:key.keyboard.m\r\n'
    b'key_key.cobblemon.summary:key.keyboard.m\r\nkey_iris.keybind.reload:key.keyboard.r\r\n'
    b'key_key.cobblemon.hideparty:key.keyboard.f4\r\nkey_player_custom:key.keyboard.semicolon\r\n'
    b'key_keybinds.combatroll.roll:key.keyboard.r\r\n'
   )
   defaults=stage/'options.txt'
   defaults.write_bytes((Path(__file__).resolve().parents[1]/'pack/options.txt').read_bytes())
   record={'path':'options.txt','seed_only':True,'size':defaults.stat().st_size,'sha512':updater.digest(defaults)}
   updater.activate({'commit':'new'},[record],stage,instance,root/'transaction')
   migrated=options.read_bytes()
   self.assertIn(b'key_key.cobblemon.summary:key.keyboard.comma\r\n',migrated)
   self.assertIn(b'key_iris.keybind.reload:key.keyboard.unknown\r\n',migrated)
   self.assertIn(b'key_key.cobblemon.hideparty:key.keyboard.f4\r\n',migrated)
   self.assertIn(b'key_keybinds.combatroll.roll:key.keyboard.r\r\n',migrated)
   self.assertIn(b'renderDistance:12\r\n',migrated)
   custom=migrated.replace(b'key_iris.keybind.reload:key.keyboard.unknown',b'key_iris.keybind.reload:key.keyboard.r')
   options.write_bytes(custom)
   updater.activate({'commit':'later'},[record],stage,instance,root/'transaction')
   self.assertEqual(options.read_bytes(),custom)

 def test_existing_verified_mod_is_neither_staged_nor_backed_up(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp);instance=root/'game';stage=root/'stage';cache=root/'cache';transaction=root/'transaction'
   installed=instance/'mods/verified.jar'
   installed.parent.mkdir(parents=True);installed.write_bytes(b'verified')
   record={'path':'mods/verified.jar','size':installed.stat().st_size,'sha512':updater.digest(installed),'client':True}
   cache.mkdir()
   archive=cache/'overrides.zip'
   with zipfile.ZipFile(archive,'w'):pass
   manifest={
    'schema':1,'minecraft':'1.20.1','forge':'47.4.28','commit':'a'*40,
    'files':[record],'overrides':{'size':archive.stat().st_size,'sha512':updater.digest(archive),'files':[]},
   }
   archive.rename(cache/(manifest['overrides']['sha512']+'.zip'))
   records=updater.stage(manifest,stage,cache,installed=instance)
   self.assertFalse((stage/record['path']).exists())
   updater.activate(manifest,records,stage,instance,transaction)
   self.assertFalse((transaction/'before'/record['path']).exists())
   self.assertEqual(json.loads((instance/'.wombatcraft.json').read_text())['managed'],[record['path']])
   self.assertEqual(installed.read_bytes(),b'verified')
   installed.write_bytes(b'modified')
   with self.assertRaisesRegex(RuntimeError,'changed during the update'):
    updater.activate(manifest,records,stage,instance,transaction)
   self.assertEqual(installed.read_bytes(),b'modified')

 def test_disk_full_is_distinct_from_other_io_errors(self):
  self.assertTrue(updater.disk_full(OSError(errno.ENOSPC,'disk full')))
  self.assertFalse(updater.disk_full(OSError(errno.EACCES,'access denied')))
  self.assertFalse(updater.disk_full(ValueError('invalid manifest')))

 def test_generated_quest_chapters_are_retired_transactionally(self):
  for fail in [False,True]:
   with self.subTest(fail=fail),tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp);instance=root/'game';stage=root/'stage';prefix='config/ftbquests/quests/chapters/'
    (instance/prefix).mkdir(parents=True);(stage/prefix).mkdir(parents=True)
    old=instance/(prefix+'0123456789ABCDEF.snbt');old.write_text('generated duplicate')
    new=prefix+'wc_create.snbt';(stage/new).write_text('canonical chapter')
    (instance/'world/ftbquests').mkdir(parents=True);progress=instance/'world/ftbquests/team.snbt';progress.write_text('player progress')
    if fail:
     with patch.object(updater.os,'replace',side_effect=OSError('disk failure')),self.assertRaises(OSError):
      updater.activate({'commit':'new'},[{'path':new}],stage,instance,root/'transaction')
     self.assertEqual(old.read_text(),'generated duplicate');self.assertFalse((instance/new).exists())
    else:
     updater.activate({'commit':'new'},[{'path':new}],stage,instance,root/'transaction')
     self.assertFalse(old.exists());self.assertEqual((instance/new).read_text(),'canonical chapter')
     self.assertEqual((root/'transaction/before'/prefix/old.name).read_text(),'generated duplicate')
    self.assertEqual(progress.read_text(),'player progress')
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
