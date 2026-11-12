"""Verified staging and recoverable activation shared by client and server tooling."""
import concurrent.futures,hashlib,json,os,shutil,urllib.request,zipfile
from pathlib import Path,PurePosixPath

RELEASE='https://github.com/wombatjs/wombatcraft/releases/latest/download/manifest.json'
ALLOWED={'mods','config','kubejs','resourcepacks','packmenu','defaultconfigs'}

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

def stage(m,stage_dir,cache,side='client',progress=lambda s:None):
 validate_manifest(m);stage_dir.mkdir(parents=True,exist_ok=True);cache.mkdir(parents=True,exist_ok=True)
 files=[f for f in m['files'] if f[side]]
 def one(f):
  blob=cache/f['sha512'];download(f,blob);target=safe_path(stage_dir,f['path']);target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(blob,target)
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
  if entry['existed']:
   target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(backup,target)
  elif target.exists():target.unlink()
 old=transaction/'previous-manifest.json';manifest=instance/'.wombatcraft.json'
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
  if new.get(name,{}).get('seed_only') and target.exists():continue
  existed=target.is_file()
  if existed:
   backup=safe_path(transaction/'before',name);backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(target,backup)
  entries.append(dict(path=name,existed=existed))
 journal=transaction/'journal.json';journal.write_text(json.dumps({'entries':entries}))
 try:
  for entry in entries:
   name=entry['path'];target=safe_path(instance,name)
   if name in new:
    target.parent.mkdir(parents=True,exist_ok=True);temp=target.with_name(target.name+'.wombatcraft-new');shutil.copy2(safe_path(stage_dir,name),temp);os.replace(temp,target)
   elif target.exists():target.unlink()
  temp=instance/'.wombatcraft-new.json';temp.write_text(json.dumps({'commit':m['commit'],'managed':list(new)},indent=2));os.replace(temp,current)
  journal.unlink()
 except BaseException:
  recover(instance,transaction);raise
