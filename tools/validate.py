import collections,json,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
chapters=[json.loads(p.read_text(encoding='utf-8')) for p in (ROOT/'pack/config/ftbquests/quests/chapters').glob('*.snbt')]
quests=[q for c in chapters for q in c['quests']];ids=[]
for c in chapters:
 positions=set();ids.append(c['id'])
 for q in c['quests']:
  point=(q['x'],q['y']);assert point not in positions,(c['title'],point);positions.add(point)
  ids.extend([q['id']]+[x['id'] for x in q.get('tasks',[])]+[x['id'] for x in q.get('rewards',[])])
assert len(ids)==len(set(ids)),'Duplicate FTB IDs'
assert all(0<int(i,16)<=0x7FFFFFFFFFFFFFFF for i in ids),'FTB IDs must be positive signed longs'
graph={q['id']:q.get('dependencies',[]) for q in quests};done=set();active=set()
def visit(k):
 assert k in graph,'Missing quest dependency '+k
 assert k not in active,'Cyclic quest dependency '+k
 if k in done:return
 active.add(k)
 for dep in graph[k]:visit(dep)
 active.remove(k);done.add(k)
for k in graph:visit(k)
lock=json.loads((ROOT/'mod-lock.json').read_text());names=[r['file']['filename'] for r in lock['mods']];assert len(names)==len(set(names))
for r in lock['mods']:
 f=r['file'];assert f['url'].startswith('https://');assert re.fullmatch('[a-f0-9]{128}',f['hashes']['sha512']);assert f['size']>0
for p in (ROOT/'pack/packmenu/resources/assets/packmenu/buttons').glob('*.json'):
 b=json.loads(p.read_text());assert b['action'] in ['OPEN_GUI','CONNECT_TO_SERVER','QUIT','OPEN_URL','NONE']
print(f'Validated {len(quests)} quests, {len(chapters)} chapters, {len(names)} mod files; dependency graph is acyclic.')
