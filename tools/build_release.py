"""Build immutable, hash-addressed release artifacts without redistributing mod jars."""
import hashlib,json,subprocess,sys,zipfile,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'launcher'))
from discord_presence import application_id
from updater import MAP_DEFAULTS
from portal_patch import PATCH_NAME, COMPAT_PATCH_NAME, REFINED_PATCH_NAME, CLIENT_PATCH_NAME, build_portal_patch

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
 if 'fallback_urls' in record:
  files[-1]['fallback_urls']=record['fallback_urls']
for f in lock.get('shaderpacks',[]):
 files.append(dict(path='shaderpacks/'+f['filename'],url=f['url'],sha512=f['hashes']['sha512'],size=f['size'],client=True,server=False))
overrides=[]
generated={}
smp_root=ROOT/'mods/wombatcraft-smp'
wrapper=str(smp_root/('gradlew.bat' if os.name=='nt' else 'gradlew'))
subprocess.run(([wrapper] if os.name=='nt' else ['bash',wrapper])+['--no-daemon','test','build'],cwd=smp_root,check=True)
smp_jar=smp_root/'build/libs/wombatcraft-smp-0.1.0-staging.jar'
if not smp_jar.is_file():raise SystemExit('Communities module was not built')
client_defaults={'options.txt','config/oculus.properties','config/simple-rpc/simple-rpc.toml','config/simple-rpc/server-entries.toml'}
client_defaults |= MAP_DEFAULTS
client_only=client_defaults | {'config/jeioptimizations.properties','config/modernfix-mixins.properties',
 'config/spark/config.json','config/jei/client/default/jei-client.ini',
 'config/embeddium-options.json','config/entityculling.json','config/immediatelyfast.json',
 'config/badoptimizations.txt'}
server_only={'config/lightmanscurrency-common.txt','config/lightmanscurrency-server.txt','config/lightmanscurrency/PersistentTraders.json','kubejs/startup_scripts/wombatcraft_spawn_guard.js','kubejs/server_scripts/wombatcraft_spawn_npcs.js'}
with zipfile.ZipFile(OUT/'overrides.zip','w',zipfile.ZIP_DEFLATED) as z:
 name='mods/'+smp_jar.name;data=smp_jar.read_bytes();generated[name]=data;z.writestr(name,data)
 overrides.append(dict(path=name,sha512=hashlib.sha512(data).hexdigest(),size=len(data),client=True,server=True,seed_only=False))
 for p in sorted((ROOT/'pack').rglob('*')):
  if not p.is_file():continue
  name=p.relative_to(ROOT/'pack').as_posix()
  if name.startswith('config/simple-rpc/') and not discord_enabled:continue
  data=p.read_bytes();z.writestr(name,data)
  overrides.append(dict(path=name,sha512=hashlib.sha512(data).hexdigest(),size=len(data),client=name not in server_only and not name.startswith(('config/tab/','config/wombatcraft/')),server=not name.startswith(('packmenu/','resourcepacks/','kubejs/assets/','kubejs/client_scripts/')) and name not in client_only,seed_only=name in client_defaults))
 patch_path='mods/'+PATCH_NAME
 patch=build_portal_patch();z.writestr(patch_path,patch)
 overrides.append(dict(path=patch_path,sha512=hashlib.sha512(patch).hexdigest(),size=len(patch),client=False,server=True,seed_only=False))
 aurorian_path='mods/'+COMPAT_PATCH_NAME
 aurorian_patch=build_portal_patch('runtime-compat');z.writestr(aurorian_path,aurorian_patch)
 overrides.append(dict(path=aurorian_path,sha512=hashlib.sha512(aurorian_patch).hexdigest(),size=len(aurorian_patch),client=True,server=True,seed_only=False))
 for filename,folder,server in [(REFINED_PATCH_NAME,'refined-dimension-sync',True),(CLIENT_PATCH_NAME,'portal-client',False)]:
  name='mods/'+filename;data=build_portal_patch(folder);generated[name]=data;z.writestr(name,data)
  overrides.append(dict(path=name,sha512=hashlib.sha512(data).hexdigest(),size=len(data),client=True,server=server,seed_only=False))
archive=(OUT/'overrides.zip').read_bytes()
manifest=dict(schema=1,name='Wombatcraft',commit=sha,minecraft='1.20.1',forge='47.4.28',server_address='74.208.117.88:25565',files=files,overrides=dict(url=base+'overrides.zip',sha512=hashlib.sha512(archive).hexdigest(),size=len(archive),files=overrides))
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
index=dict(formatVersion=1,game='minecraft',versionId=sha[:12],name='Wombatcraft',summary='Industry, spellcraft and a shared frontier.',files=[dict(path=f['path'],hashes={'sha512':f['sha512']},downloads=[f['url']],fileSize=f['size'],env={'client':'required' if f['client'] else 'unsupported','server':'required' if f['server'] else 'unsupported'}) for f in files],dependencies={'minecraft':'1.20.1','forge':'47.4.28'})
with zipfile.ZipFile(OUT/'Wombatcraft.mrpack','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('modrinth.index.json',json.dumps(index,indent=2))
 for override in overrides:
  name=override['path']
  if name==patch_path:z.writestr('server-overrides/'+name,patch);continue
  if name==aurorian_path:z.writestr('overrides/'+name,aurorian_patch);continue
  directory='server-overrides' if not override['client'] else ('client-overrides' if not override['server'] else 'overrides')
  if name in generated:z.writestr(directory+'/'+name,generated[name]);continue
  z.write(ROOT/'pack'/name,directory+'/'+name)
print(json.dumps(dict(commit=sha,mods=sum(f['path'].startswith('mods/') for f in files),shaderpacks=len(lock.get('shaderpacks',[])),overrides=len(overrides))))
