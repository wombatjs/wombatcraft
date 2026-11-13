"""Build immutable, hash-addressed release artifacts without redistributing mod jars."""
import hashlib,json,subprocess,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'launcher'))
from discord_presence import application_id
from portal_patch import PATCH_NAME, build_portal_patch

OUT=ROOT/'dist';OUT.mkdir(exist_ok=True)
if subprocess.check_output(['git','-C',str(ROOT),'status','--porcelain'],text=True).strip():raise SystemExit('Commit source changes before building an immutable release.')
sha=subprocess.check_output(['git','-C',str(ROOT),'rev-parse','HEAD'],text=True).strip()
base=f'https://github.com/wombatjs/wombatcraft/releases/download/build-{sha}/'
lock=json.loads((ROOT/'mod-lock.json').read_text())
discord_enabled=bool(application_id(ROOT/'pack/config/simple-rpc/simple-rpc.toml'))
# The client also hosts integrated singleplayer servers. Include server-side
# world generation and AI libraries there; upstream client-side metadata only
# describes a dedicated multiplayer client, not a complete singleplayer pack.
files=[]
for record in lock['mods']:
 if record.get('requires_discord_app') and not discord_enabled:continue
 f=record['file'];files.append(dict(path='mods/'+f['filename'],url=f['url'],sha512=f['hashes']['sha512'],size=f['size'],client=record.get('client',True),server=record.get('server',record['project'].get('server_side')!='unsupported')))
for f in lock.get('shaderpacks',[]):
 files.append(dict(path='shaderpacks/'+f['filename'],url=f['url'],sha512=f['hashes']['sha512'],size=f['size'],client=True,server=False))
overrides=[]
client_defaults={'options.txt','config/oculus.properties','config/simple-rpc/simple-rpc.toml','config/simple-rpc/server-entries.toml'}
client_only=client_defaults | {'config/jeioptimizations.properties'}
with zipfile.ZipFile(OUT/'overrides.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted((ROOT/'pack').rglob('*')):
  if not p.is_file():continue
  name=p.relative_to(ROOT/'pack').as_posix()
  if name.startswith('config/simple-rpc/') and not discord_enabled:continue
  data=p.read_bytes();z.writestr(name,data)
  overrides.append(dict(path=name,sha512=hashlib.sha512(data).hexdigest(),size=len(data),client=True,server=not name.startswith(('packmenu/','resourcepacks/','kubejs/assets/')) and name not in client_only,seed_only=name in client_defaults))
 patch_path='mods/'+PATCH_NAME
 patch=build_portal_patch();z.writestr(patch_path,patch)
 overrides.append(dict(path=patch_path,sha512=hashlib.sha512(patch).hexdigest(),size=len(patch),client=False,server=True,seed_only=False))
archive=(OUT/'overrides.zip').read_bytes()
manifest=dict(schema=1,name='Wombatcraft',commit=sha,minecraft='1.20.1',forge='47.4.28',server_address='74.208.117.88:25565',files=files,overrides=dict(url=base+'overrides.zip',sha512=hashlib.sha512(archive).hexdigest(),size=len(archive),files=overrides))
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
index=dict(formatVersion=1,game='minecraft',versionId=sha[:12],name='Wombatcraft',summary='Industry, spellcraft and a shared frontier.',files=[dict(path=f['path'],hashes={'sha512':f['sha512']},downloads=[f['url']],fileSize=f['size'],env={'client':'required' if f['client'] else 'unsupported','server':'required' if f['server'] else 'unsupported'}) for f in files],dependencies={'minecraft':'1.20.1','forge':'47.4.28'})
with zipfile.ZipFile(OUT/'Wombatcraft.mrpack','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('modrinth.index.json',json.dumps(index,indent=2))
 for override in overrides:
  name=override['path']
  if name==patch_path:z.writestr('server-overrides/'+name,patch);continue
  directory='client-overrides' if not override['server'] else 'overrides'
  z.write(ROOT/'pack'/name,directory+'/'+name)
print(json.dumps(dict(commit=sha,mods=sum(f['path'].startswith('mods/') for f in files),shaderpacks=len(lock.get('shaderpacks',[])),overrides=len(overrides))))
