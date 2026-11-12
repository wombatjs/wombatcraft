"""Build immutable, hash-addressed release artifacts without redistributing mod jars."""
import hashlib,json,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'dist';OUT.mkdir(exist_ok=True)
if subprocess.check_output(['git','-C',str(ROOT),'status','--porcelain'],text=True).strip():raise SystemExit('Commit source changes before building an immutable release.')
sha=subprocess.check_output(['git','-C',str(ROOT),'rev-parse','HEAD'],text=True).strip()
base=f'https://github.com/wombatjs/wombatcraft/releases/download/build-{sha}/'
lock=json.loads((ROOT/'mod-lock.json').read_text())
files=[]
for record in lock['mods']:
 f=record['file'];files.append(dict(path='mods/'+f['filename'],url=f['url'],sha512=f['hashes']['sha512'],size=f['size'],client=record['project'].get('client_side')!='unsupported',server=record['project'].get('server_side')!='unsupported'))
overrides=[]
with zipfile.ZipFile(OUT/'overrides.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted((ROOT/'pack').rglob('*')):
  if not p.is_file():continue
  name=p.relative_to(ROOT/'pack').as_posix();data=p.read_bytes();z.writestr(name,data)
  overrides.append(dict(path=name,sha512=hashlib.sha512(data).hexdigest(),size=len(data),client=True,server=not name.startswith(('packmenu/','resourcepacks/')) and name!='options.txt',seed_only=name=='options.txt'))
archive=(OUT/'overrides.zip').read_bytes()
manifest=dict(schema=1,name='Wombatcraft',commit=sha,minecraft='1.20.1',forge='47.4.28',server_address='74.208.117.88:25565',files=files,overrides=dict(url=base+'overrides.zip',sha512=hashlib.sha512(archive).hexdigest(),size=len(archive),files=overrides))
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
index=dict(formatVersion=1,game='minecraft',versionId=sha[:12],name='Wombatcraft',summary='Industry, spellcraft and a shared frontier.',files=[dict(path=f['path'],hashes={'sha512':f['sha512']},downloads=[f['url']],fileSize=f['size'],env={'client':'required' if f['client'] else 'unsupported','server':'required' if f['server'] else 'unsupported'}) for f in files],dependencies={'minecraft':'1.20.1','forge':'47.4.28'})
with zipfile.ZipFile(OUT/'Wombatcraft.mrpack','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('modrinth.index.json',json.dumps(index,indent=2))
 for p in (ROOT/'pack').rglob('*'):
  if p.is_file():z.write(p,'overrides/'+p.relative_to(ROOT/'pack').as_posix())
print(json.dumps(dict(commit=sha,mods=len(files),overrides=len(overrides))))
