"""Boot a disposable dedicated server and validate live quest/item/recipe registries."""
import json,os,shutil,subprocess,sys,time,urllib.request,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'launcher'));import updater
dest=ROOT/'dist/server-smoke';dest.mkdir(parents=True,exist_ok=True)
lock=json.loads((ROOT/'mod-lock.json').read_text());mods=dest/'mods';mods.mkdir(exist_ok=True)
for r in lock['mods']:
 if r['project'].get('server_side')=='unsupported':continue
 f=r['file'];updater.download(dict(url=f['url'],sha512=f['hashes']['sha512'],size=f['size']),mods/f['filename'])
for name in ['config','kubejs','defaultconfigs']:
 if (ROOT/'pack'/name).exists():shutil.copytree(ROOT/'pack'/name,dest/name,dirs_exist_ok=True)
version='1.20.1-47.4.28';installer=dest/'installer.jar'
urllib.request.urlretrieve(f'https://maven.minecraftforge.net/net/minecraftforge/forge/{version}/forge-{version}-installer.jar',installer)
with (dest/'install.log').open('w') as log:subprocess.run(['java','-jar',str(installer),'--installServer'],cwd=dest,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=600)
classes=dest/'qa-classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join(str(p) for folder in ['net/minecraftforge/forge','net/minecraftforge/javafmllanguage','net/minecraftforge/eventbus'] for p in (dest/'libraries'/folder).rglob('*.jar') if not any(k in p.name for k in ['sources','installer','client','extra']))
subprocess.run(['javac','-proc:none','-cp',cp,'-d',str(classes),str(ROOT/'tools/qa/ForgeQuestAudit.java')],check=True)
with zipfile.ZipFile(mods/'forge-quest-audit.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[47,)"\nlicense="MIT"\n[[mods]]\nmodId="forge_quest_audit"\nversion="1.0.0"\ndisplayName="Wombatcraft CI audit"\n')
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
(dest/'eula.txt').write_text('eula=true\n');(dest/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25577\nlevel-name=smoke-world\nonline-mode=true\nview-distance=4\nsimulation-distance=4\nmax-tick-time=180000\n')
args='win_args.txt' if os.name=='nt' else 'unix_args.txt'
with (dest/'console.log').open('w') as log:
 process=subprocess.Popen(['java','-Xms1G','-Xmx5G',f'@libraries/net/minecraftforge/forge/{version}/{args}','nogui'],cwd=dest,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT)
 try:
  deadline=time.monotonic()+900
  while time.monotonic()<deadline:
   if process.poll() is not None:raise RuntimeError('Dedicated server exited before startup completed')
   text=(dest/'console.log').read_text(errors='replace')
   if ')! For help, type ' in text and 'QUEST_AUDIT_COMPLETE' in text and 'RECIPE_AUDIT_COMPLETE' in text:break
   time.sleep(5)
  else:raise TimeoutError('Dedicated server startup timed out')
  chapters=[json.loads(p.read_text()) for p in (ROOT/'pack/config/ftbquests/quests/chapters').glob('*.snbt')];quests=[q for c in chapters for q in c['quests']]
  actual={line.split('\t')[0].upper() for line in (dest/'quest-runtime-audit.tsv').read_text().splitlines()};expected={q['id'].upper() for q in quests};assert actual==expected,'Runtime quest IDs differ from source'
  registry=set((dest/'item-registry.txt').read_text().splitlines());refs={q['icon'] for q in quests if isinstance(q.get('icon'),str)}
  refs|={t['item'] for q in quests for t in q.get('tasks',[]) if t.get('type')=='item' and isinstance(t.get('item'),str)}
  refs|={r['item'] for q in quests for r in q.get('rewards',[]) if r.get('type')=='item' and isinstance(r.get('item'),str)}
  assert not refs-registry,'Missing runtime items: '+str(sorted(refs-registry))
  recipes=set((dest/'recipe-registry.txt').read_text().splitlines());endgame={'kubejs:'+name for name in ['industrial_sigil','arcane_sigil','voyager_sigil','prosperity_sigil','convergence_core']};assert endgame<=recipes,'Missing endgame recipes'
  assert 'Parsing error loading recipe' not in text,'Recipe parse errors'
  (ROOT/'dist/server-validation.json').write_text(json.dumps(dict(quests=len(quests),registry_items=len(registry),recipes=len(recipes),endgame_recipes=sorted(endgame),fresh_world_started=True),indent=2))
  print('Dedicated server, quest registry and endgame recipe validation passed.')
 finally:
  if process.poll() is None:
   process.stdin.write(b'stop\n');process.stdin.flush()
   try:process.wait(timeout=120)
   except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=30)
