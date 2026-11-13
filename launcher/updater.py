"""Verified staging and recoverable activation shared by client and server tooling."""
import concurrent.futures,errno,hashlib,json,os,shutil,urllib.request,zipfile
from pathlib import Path,PurePosixPath

RELEASE='https://github.com/wombatjs/wombatcraft/releases/latest/download/manifest.json'
ALLOWED={'mods','config','kubejs','resourcepacks','packmenu','defaultconfigs','shaderpacks'}

# Earlier releases seeded these bindings or left the mods' defaults in place.
OLD_KEYS = {
 'key_key.ftbchunks.map': ('f7',),
 'key_key.cobblemon.summary': ('m',),
 'key_key.cobblemon.hideparty': ('o',),
 'key_iris.keybind.reload': ('r',),
 'key_iris.keybind.toggleShaders': ('k',),
 'key_iris.keybind.shaderPackSelection': ('o',),
 'key_key.mute_microphone': ('m',),
 'key_key.drawMahoujin': ('m',),
 'key_key.structurize.mirror': ('m',),
 'key_keybinds.combatroll.roll': ('r',),
 'key_key.irons_spellbooks.spell_wheel': ('r',),
 'key_key.irons_spellbooks.spellbook_cast': ('v',),
 'key_key.immersiveengineering.magnetEquip': ('s',),
 'key_key.titles.openTitleSelection': ('t',),
 'key_key.travelersbackpack.inventory': ('b',),
 'key_key.occultism.backpack': ('b',),
 'key_key.toms_storage.open_terminal': ('b',),
 'key_key.lightmanscurrency.open_wallet': ('v',),
 'key_key.mekanism.legs_mode': ('j',),
 'key_key.ars_elemental.open_pouch': ('j',),
}

def migrate_controls(current, defaults):
 desired = dict(line.split(b':', 1) for line in defaults.splitlines() if line.startswith(b'key_') and b':' in line)
 bindings = dict(line.split(b':', 1) for line in current.splitlines() if line.startswith(b'key_') and b':' in line)
 changes = {}
 # Free M before attempting to move the map onto it.
 for key in sorted(OLD_KEYS, key=lambda key: key == 'key_key.ftbchunks.map'):
  name = key.encode()
  target = desired.get(name)
  previous = bindings.get(name)
  if not target or previous not in {b'key.keyboard.' + old.encode() for old in OLD_KEYS[key]}:
   continue
  if target != b'key.keyboard.unknown' and target in bindings.values():
   continue
  changes[name] = bindings[name] = target
 result = []
 for line in current.splitlines(keepends=True):
  name = line.partition(b':')[0]
  if name in changes:
   ending = line[len(line.rstrip(b'\r\n')):]
   line = name + b':' + changes[name] + ending
  result.append(line)
 return b''.join(result)

def safe_path(root,name):
 p=PurePosixPath(name)
 if not name or '\\' in name or ':' in name or p.is_absolute() or '..' in p.parts:raise ValueError('Unsafe release path')
 if p.parts[0] not in ALLOWED and name!='options.txt':raise ValueError('Unmanaged release path: '+name)
 result=root.joinpath(*p.parts)
 if not result.resolve().is_relative_to(root.resolve()):raise ValueError('Release path escapes instance')
 return result

def digest(path):
 h=hashlib.sha512()
 with path.open('rb') as f:
  for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
 return h.hexdigest()

def matches(path, record):
 return (
  'size' in record and 'sha512' in record and path.is_file()
  and path.stat().st_size == record['size'] and digest(path) == record['sha512']
 )

def disk_full(error):
 return isinstance(error,OSError) and (
  error.errno == errno.ENOSPC or getattr(error,'winerror',None) in {39,112}
 )

def fetch_json(url):
 if not url.startswith('https://'):raise ValueError('HTTPS is required')
 with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Wombatcraft/0.1'}),timeout=45) as r:return json.load(r)

def download(record,target):
 if target.exists() and target.stat().st_size==record['size'] and digest(target)==record['sha512']:return
 if not record['url'].startswith('https://'):raise ValueError('HTTPS is required')
 target.parent.mkdir(parents=True,exist_ok=True);part=target.with_suffix(target.suffix+'.part')
 try:
  with urllib.request.urlopen(urllib.request.Request(record['url'],headers={'User-Agent':'Wombatcraft/0.1'}),timeout=90) as r,part.open('wb') as out:
   size=0
   while chunk:=r.read(1024*1024):
    size+=len(chunk)
    if size>record['size']:raise ValueError('Download exceeded declared size')
    out.write(chunk)
  if size!=record['size'] or digest(part)!=record['sha512']:raise ValueError('Download verification failed: '+target.name)
  os.replace(part,target)
 finally:
  if part.exists():part.unlink()

def validate_manifest(m):
 if m.get('schema')!=1 or m.get('minecraft')!='1.20.1' or m.get('forge')!='47.4.28':raise ValueError('Unsupported release format or game version')
 if len(m.get('commit',''))!=40 or any(c not in '0123456789abcdef' for c in m['commit']):raise ValueError('Invalid commit')
 names=[]
 for f in m['files']+m['overrides']['files']:
  safe_path(Path('instance'),f['path']);names.append(f['path'].casefold())
  if len(f['sha512'])!=128 or f['size']<0:raise ValueError('Invalid file metadata')
 if len(names)!=len(set(names)):raise ValueError('Duplicate release path')

def stage(m,stage_dir,cache,side='client',progress=lambda s:None,installed=None):
 validate_manifest(m);stage_dir.mkdir(parents=True,exist_ok=True);cache.mkdir(parents=True,exist_ok=True)
 files=[f for f in m['files'] if f[side]]
 def one(f):
  if installed is not None and matches(safe_path(installed,f['path']),f):return
  target=safe_path(stage_dir,f['path'])
  if matches(target,f):return
  blob=cache/f['sha512'];download(f,blob)
  target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(blob,target)
 with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
  for n,_ in enumerate(pool.map(one,files),1):progress(f'Preparing mods {n} / {len(files)}')
 archive=cache/(m['overrides']['sha512']+'.zip');download(m['overrides'],archive)
 with zipfile.ZipFile(archive) as z:
  for f in m['overrides']['files']:
   if not f[side]:continue
   data=z.read(f['path'])
   if len(data)!=f['size'] or hashlib.sha512(data).hexdigest()!=f['sha512']:raise ValueError('Override verification failed')
   target=safe_path(stage_dir,f['path']);target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
 return [f for f in files+m['overrides']['files'] if f[side]]

def recover(instance,transaction):
 journal=transaction/'journal.json'
 if not journal.exists():return
 state=json.loads(journal.read_text())
 for entry in reversed(state['entries']):
  target=safe_path(instance,entry['path']);backup=safe_path(transaction/'before',entry['path'])
  target.with_name(target.name+'.wombatcraft-new').unlink(missing_ok=True)
  if entry['existed']:
   target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(backup,target)
  elif target.exists():target.unlink()
 old=transaction/'previous-manifest.json';manifest=instance/'.wombatcraft.json'
 (instance/'.wombatcraft-new.json').unlink(missing_ok=True)
 if old.exists():shutil.copy2(old,manifest)
 elif manifest.exists():manifest.unlink()
 journal.unlink()

def activate(m,records,stage_dir,instance,transaction):
 instance.mkdir(parents=True,exist_ok=True);transaction.mkdir(parents=True,exist_ok=True)
 recover(instance,transaction)
 current=instance/'.wombatcraft.json';old=json.loads(current.read_text()) if current.exists() else {'managed':[]}
 if current.exists():shutil.copy2(current,transaction/'previous-manifest.json')
 elif (transaction/'previous-manifest.json').exists():(transaction/'previous-manifest.json').unlink()
 new={f['path']:f for f in records}
 options = instance/'options.txt'
 seed = new.get('options.txt')
 migrated_options = None
 controls_migrated = old.get('controls_migrated', False)
 defaults = stage_dir/'options.txt'
 if seed and not controls_migrated and matches(defaults, seed):
  if options.is_file():
   original = options.read_bytes()
   migrated = migrate_controls(original, defaults.read_bytes())
   if migrated != original:
    migrated_options = migrated
  controls_migrated = True
 # FTB rewrites chapters on shutdown. This one directory is pack-owned;
 # journal obsolete generated chapters too, so updates cannot duplicate them.
 # Player/team completion data lives in world/, which is never managed here.
 quest_prefix='config/ftbquests/quests/chapters/'
 retired=set()
 if any(name.startswith(quest_prefix) for name in new):
  folder=safe_path(instance,quest_prefix)
  if folder.exists():retired={p.relative_to(instance).as_posix() for p in folder.glob('*.snbt') if p.is_file()}
 names=sorted(set(old.get('managed',[]))|set(new)|retired)
 entries=[]
 for name in names:
  target=safe_path(instance,name)
  record=new.get(name)
  if record is not None:
   if record.get('seed_only') and target.exists():
    if name == 'options.txt' and migrated_options is not None:
     entries.append(dict(path=name,existed=True))
    continue
   if matches(target,record):continue
   staged=safe_path(stage_dir,name)
   if not staged.is_file() or ('sha512' in record and not matches(staged,record)):
    raise RuntimeError('A pack file changed during the update. Close Minecraft and retry.')
  existed=target.is_file()
  if not existed and record is None:continue
  entries.append(dict(path=name,existed=existed))
 for entry in entries:
  if entry['existed']:
   name=entry['path'];target=safe_path(instance,name)
   backup=safe_path(transaction/'before',name);backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(target,backup)
 journal=transaction/'journal.json';pending=transaction/'journal.tmp'
 try:
  pending.write_text(json.dumps({'entries':entries}))
  os.replace(pending,journal)
 finally:
  pending.unlink(missing_ok=True)
 try:
  for entry in entries:
   name=entry['path'];target=safe_path(instance,name)
   if name in new:
    target.parent.mkdir(parents=True,exist_ok=True);temp=target.with_name(target.name+'.wombatcraft-new')
    if name == 'options.txt' and migrated_options is not None:
     temp.write_bytes(migrated_options)
    else:
     shutil.copy2(safe_path(stage_dir,name),temp)
    os.replace(temp,target)
   elif target.exists():target.unlink()
  installed={'commit':m['commit'],'managed':list(new)}
  if controls_migrated:
   installed['controls_migrated']=True
  temp=instance/'.wombatcraft-new.json';temp.write_text(json.dumps(installed,indent=2));os.replace(temp,current)
  journal.unlink()
 except BaseException:
  recover(instance,transaction);raise
