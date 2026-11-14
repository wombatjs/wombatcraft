"""Boot a disposable dedicated server and validate live quest/item/recipe registries."""
import hashlib,json,os,shutil,subprocess,sys,time,urllib.request,zipfile
from pathlib import Path
from portal_patch import PATCH_NAME, COMPAT_PATCH_NAME, build_portal_patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'launcher'));import updater
dest=ROOT/'dist/server-smoke';dest.mkdir(parents=True,exist_ok=True)
lock=json.loads((ROOT/'mod-lock.json').read_text());mods=dest/'mods';mods.mkdir(exist_ok=True)
print('Verifying and downloading pinned server mods...',flush=True)
for r in lock['mods']:
 if not r.get('server',r['project'].get('server_side')!='unsupported'):continue
 f=r['file'];updater.download(dict(path='mods/'+f['filename'],url=f['url'],fallback_urls=r.get('fallback_urls',[]),sha512=f['hashes']['sha512'],size=f['size']),mods/f['filename'])
(mods/PATCH_NAME).write_bytes(build_portal_patch())
(mods/COMPAT_PATCH_NAME).write_bytes(build_portal_patch('runtime-compat'))
print('Mod downloads verified; auditing expedition targets.',flush=True)
# Audit expedition targets against the exact release jars, not an old web wiki.
advancements=set();dimensions=set();structures=set()
for jar in mods.glob('*.jar'):
 with zipfile.ZipFile(jar) as archive:
  for name in archive.namelist():
   parts=name.split('/')
   if len(parts)>=4 and parts[0]=='data' and name.endswith('.json'):
    target=parts[1]+':'+('/'.join(parts[3:]))[:-5]
    if parts[2]=='advancements':advancements.add(target)
    if parts[2]=='dimension':dimensions.add(target)
    if len(parts)>4 and parts[2:4]==['worldgen','structure']:structures.add(parts[1]+':'+('/'.join(parts[4:]))[:-5])
for path in (ROOT/'pack/config/ftbquests/quests/chapters').glob('wc_dimension*.snbt'):
 for q in json.loads(path.read_text())['quests']:
  for task in q['tasks']:
   if task['type']=='advancement' and not task['advancement'].startswith('minecraft:'):
    assert task['advancement'] in advancements, 'Unknown expedition advancement: '+task['advancement']
   if task['type']=='dimension' and not task['dimension'].startswith('minecraft:'):
    assert task['dimension'] in dimensions, 'Unknown expedition dimension: '+task['dimension']
   if task['type']=='structure' and not task['structure'].startswith('minecraft:'):
    assert task['structure'] in structures, 'Unknown expedition structure: '+task['structure']
assert set(json.loads((ROOT/'quests/dimension-inventory.json').read_text())) <= dimensions, 'Dimension inventory differs from release jars'
for name in ['config','kubejs','defaultconfigs']:
 if (ROOT/'pack'/name).exists():shutil.copytree(ROOT/'pack'/name,dest/name,dirs_exist_ok=True,ignore=shutil.ignore_patterns('simple-rpc','oculus.properties'))
version='1.20.1-47.4.28';installer=dest/'installer.jar'
downloaded=False
for origin in ['https://maven.minecraftforge.net','https://files.minecraftforge.net/maven']:
 result=subprocess.run(['curl','--fail','--location','--retry','3','--retry-all-errors','--retry-delay','4','--max-time','120','--user-agent','Wombatcraft/0.1',origin+f'/net/minecraftforge/forge/{version}/forge-{version}-installer.jar','--output',str(installer)],capture_output=True,text=True)
 if result.returncode==0:downloaded=True;break
if not downloaded:raise RuntimeError('Official Forge download failed from both official endpoints')
assert hashlib.sha512(installer.read_bytes()).hexdigest()=='8fd435b269ea75a97340127ab9f745410595ce70e52c39fe1aa70ad00419ae8c4580049d6538c3a24adf9bfc6bc2b42026f2a4cca33015f9f3c5bb8152c1fd35','Forge installer hash mismatch'
with (dest/'install.log').open('w') as log:subprocess.run(['java','-jar',str(installer),'--installServer'],cwd=dest,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=600)
classes=dest/'qa-classes';classes.mkdir(exist_ok=True)
cp=os.pathsep.join(str(p) for folder in ['net/minecraftforge/forge','net/minecraftforge/javafmllanguage','net/minecraftforge/eventbus'] for p in (dest/'libraries'/folder).rglob('*.jar') if not any(k in p.name for k in ['sources','installer','client','extra']))
subprocess.run(['javac','-proc:none','-cp',cp,'-d',str(classes),str(ROOT/'tools/qa/ForgeQuestAudit.java')],check=True)
with zipfile.ZipFile(mods/'forge-quest-audit.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[47,)"\nlicense="MIT"\n[[mods]]\nmodId="forge_quest_audit"\nversion="1.0.0"\ndisplayName="Wombatcraft CI audit"\n')
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
(dest/'eula.txt').write_text('eula=true\n');(dest/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25577\nlevel-name=smoke-world\nonline-mode=true\nview-distance=4\nsimulation-distance=4\nmax-tick-time=180000\n')
args='win_args.txt' if os.name=='nt' else 'unix_args.txt'
for boot in range(2):
 print('Starting dedicated server pass',boot+1,flush=True)
 with (dest/'console.log').open('w') as log:
  process=subprocess.Popen(['java','-Xms1G','-Xmx5G',f'@libraries/net/minecraftforge/forge/{version}/{args}','nogui'],cwd=dest,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT)
  try:
   deadline=time.monotonic()+900
   while time.monotonic()<deadline:
    if process.poll() is not None:raise RuntimeError('Dedicated server exited before startup completed')
    text=(dest/'console.log').read_text(errors='replace')
    if 'Failed to start the minecraft server' in text or 'Mod Loading has failed' in text:raise RuntimeError('Fatal mod loading failure; see console.log')
    if ')! For help, type ' in text and all(marker in text for marker in ('QUEST_AUDIT_COMPLETE','RECIPE_AUDIT_COMPLETE','COMMAND_AUDIT_COMPLETE','PORTAL_CHUNK_AUDIT_COMPLETE')):break
    time.sleep(5)
   else:raise TimeoutError('Dedicated server startup timed out')
   chapters=[json.loads(p.read_text()) for p in (ROOT/'pack/config/ftbquests/quests/chapters').glob('*.snbt')];quests=[q for c in chapters for q in c['quests']]
   actual={line.split('\t')[0].upper() for line in (dest/'quest-runtime-audit.tsv').read_text().splitlines()};expected={q['id'].upper() for q in quests};assert actual==expected,'Runtime quest IDs differ from source: missing='+str(sorted(expected-actual)[:8])+' unexpected='+str(sorted(actual-expected)[:8])
   registry=set((dest/'item-registry.txt').read_text().splitlines());refs={q['icon'] for q in quests if isinstance(q.get('icon'),str)}
   refs|={t['item'] for q in quests for t in q.get('tasks',[]) if t.get('type')=='item' and isinstance(t.get('item'),str)}
   refs|={r['item'] for q in quests for r in q.get('rewards',[]) if r.get('type')=='item' and isinstance(r.get('item'),str)}
   assert not refs-registry,'Missing runtime items: '+str(sorted(refs-registry))
   entities=set((dest/'entity-registry.txt').read_text().splitlines())
   targets={t['entity'] for q in quests for t in q.get('tasks',[]) if t.get('type')=='kill'}
   assert not targets-entities,'Missing runtime entities: '+str(sorted(targets-entities))
   loaded_advancements=set((dest/'advancement-registry.txt').read_text().splitlines())
   requested={t['advancement'] for c in chapters if c['filename'].startswith('wc_dimension_exp_') for q in c['quests'] for t in q['tasks'] if t['type']=='advancement'}
   assert not requested-loaded_advancements,'Unloaded expedition advancements: '+str(sorted(requested-loaded_advancements))
   recipes=set((dest/'recipe-registry.txt').read_text().splitlines());endgame={'kubejs:endgame/'+name for name in ['industrial_sigil','arcane_sigil','voyager_sigil','prosperity_sigil','convergence_core']};assert endgame<=recipes,'Missing endgame recipes'
   assert 'Parsing error loading recipe' not in text,'Recipe parse errors'
   assert 'mekanism:crushing/venus_sandstone_to_venus_sand' in recipes,'Missing Venus sandstone recipe'
   assert 'Incomplete recipe detected: mekanism:crushing/venus_sandstone_to_venus_sand' not in text,'Broken Venus sandstone ingredient'
   assert 'Wombatcraft portal chunk guard applied' in text,'Portal chunk guard did not load'
   assert 'Wombatcraft Aurorian dedicated-server packet guard applied' in text,'Aurorian server packet guard did not load'
   assert 'Wombatcraft Aurorian crafting remainder fix applied' in text,'Aurorian crafting fix did not load'
   assert 'Wombatcraft sequential recipe construction applied' in text,'Recipe construction guard did not load'
   assert 'PORTAL_POLICY_AUDIT_COMPLETE' in text,'Portal policy or serializer synchronization did not load'
   commands=set((dest/'command-registry.txt').read_text().splitlines())
   required_commands={'home','sethome','spawn','tpa','tpaccept','tpdeny','tab','warp','mute','unmute','ftbranks','ftbchunks','lcadmin','spark','chunky','pp','servercore'}
   assert required_commands<=commands,'Missing server commands: '+str(sorted(required_commands-commands))
   # Generate an actual chunk in every new destination on the disposable world.
   # Console commands run in order: the following marker proves setblock returned.
   explored=[]
   expansion=json.loads((ROOT/'quests/expansion.json').read_text(encoding='utf-8'))
   if boot==0:
    for index,d in enumerate(expansion['destinations']):
     if not d['dimension']:continue
     process.stdin.write(('execute in '+d['dimension']+' run forceload add 0 0\n').encode());process.stdin.flush()
     deadline=time.monotonic()+150
     attempt=0
     while time.monotonic()<deadline:
      if process.poll() is not None:raise RuntimeError('Server exited generating '+d['dimension'])
      attempt+=1
      marker='WOMBAT_DIMENSION_QA_'+str(index)+'_'+str(attempt)+'_DONE'
      before=len((dest/'console.log').read_text(errors='replace'))
      process.stdin.write(('execute in '+d['dimension']+' run setblock 0 100 0 minecraft:gold_block\nsay '+marker+'\n').encode());process.stdin.flush()
      while time.monotonic()<deadline:
       segment=(dest/'console.log').read_text(errors='replace')[before:]
       if marker in segment:break
       if process.poll() is not None:raise RuntimeError('Server exited generating '+d['dimension'])
       time.sleep(1)
      segment=(dest/'console.log').read_text(errors='replace')[before:]
      if 'Changed the block at' in segment or 'Could not set the block' in segment:
       process.stdin.write(('execute in '+d['dimension']+' run forceload remove 0 0\n').encode());process.stdin.flush()
       explored.append(d['dimension']);break
      assert 'Unknown dimension' not in segment,'Dimension unavailable: '+d['dimension']
      time.sleep(2)
     else:raise TimeoutError('Dimension generation timed out: '+d['dimension'])
     print('Generated destination',d['dimension'],flush=True)
    (ROOT/'dist/dimension-generation.json').write_text(json.dumps(explored,indent=2))
   (ROOT/'dist/server-validation.json').write_text(json.dumps(dict(quests=len(quests),registry_items=len(registry),recipes=len(recipes),endgame_recipes=sorted(endgame),server_commands=sorted(required_commands),portal_chunk_tracking_verified=True,fresh_world_started=True,restarted_cleanly=boot==1),indent=2))
   print('Dedicated server, quest registry and endgame recipe validation passed.')
  finally:
   if process.poll() is None:
    process.stdin.write(b'stop\n');process.stdin.flush()
    try:process.wait(timeout=120)
    except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=30)
 if boot==0:shutil.copy2(dest/'console.log',dest/'console-first-start.log')
