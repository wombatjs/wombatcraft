"""Compile original, authored progression lanes into FTB Quests chapters.

The content registry and names are build inputs, never inferred from ATM files.
Existing addon chapters remain until they receive an authored replacement.
"""
import argparse,collections,hashlib,json,re,shutil
from pathlib import Path
from quest_content import refine_chapters
from dimension_quests import add_dimensions
from quest_layout import chapter_key, lay_out_atlas, lay_out_lanes
from quest_rewards import apply_rewards
ROOT=Path(__file__).resolve().parents[1]
def uid(s):return f"{int(hashlib.sha256(('wombatcraft/'+s).encode()).hexdigest()[:16],16) & 0x7FFFFFFFFFFFFFFF:016X}"
def save(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def build(registry,names):
 dest=ROOT/'pack/config/ftbquests/quests';chapters=dest/'chapters'
 groups=json.loads((dest/'chapter_groups.snbt').read_text())['chapter_groups']
 gids={g['title']:g['id'] for g in groups}
 lanes=collections.defaultdict(list)
 for line in (ROOT/'quests/progression.txt').read_text(encoding='utf-8').splitlines():
  if not line.strip() or line.startswith('#'):continue
  title,ns,group,lane,parent,items,note=line.split('|')
  expand=lambda item:item if ':' in item else ns+':'+item
  lanes[title].append(dict(ns=ns,group=group,title=lane,parent=None if parent=='-' else expand(parent),items=list(map(expand,items.split())),note=note))
 missing=sorted({i for ls in lanes.values() for l in ls for i in l['items'] if i not in registry})
 if missing:raise ValueError('Unknown authored items:\n'+'\n'.join(missing))
 # Avoid carrying shallow duplicate chapters for these major systems.
 replace=set(lanes)|{'AE2'}
 for p in chapters.glob('*.snbt'):p.unlink()
 for p in (ROOT/'quests/baseline/chapters').glob('*.snbt'):shutil.copy2(p,chapters/p.name)
 old_goals={}
 ns_to_title={lane['ns']:title for title,ls in lanes.items() for lane in ls}
 # Exact original names are resolved below; unrelated mixed addon chapters stay intact.
 for p in chapters.glob('*.snbt'):
  d=json.loads(p.read_text(encoding='utf-8'))
  if d['title'] in replace:
   for q in d['quests']:
    task=q['tasks'][0];old_goals[q['id']]=(d['title'],task.get('item'))
   p.unlink()
  else:
   # Retire shallow mixed-chapter introductions once their whole mod has an
   # authored chapter. Redirect outside prerequisites to the matching goal.
   by_id={q['id']:q for q in d['quests']};children=collections.defaultdict(list)
   for q in d['quests']:
    for dep in q.get('dependencies',[]):children[dep].append(q['id'])
   removed=set()
   for root in d['quests']:
    if root.get('dependencies') or not all(t.get('type')=='checkmark' for t in root.get('tasks',[])):continue
    component=set();todo=[root['id']]
    while todo:
     key=todo.pop()
     if key in component:continue
     component.add(key);todo.extend(children[key])
    goals=[t for key in component for t in by_id[key].get('tasks',[]) if t.get('type')!='checkmark']
    targets={ns_to_title.get(t.get('item','').split(':')[0]) for t in goals}
    if not goals or None in targets or len(targets)!=1 or any(t.get('type')!='item' for t in goals):continue
    target=targets.pop();removed.update(component)
    for key in component:
     old_goals[key]=(target,next((t.get('item') for t in by_id[key].get('tasks',[]) if t.get('type')=='item'),None))
   if removed:d['quests']=[q for q in d['quests'] if q['id'] not in removed];save(p,d)
 reports=[]
 for order,(title,ls) in enumerate(lanes.items(),10):
  items={i for l in ls for i in l['items']};quest_ids={i:uid(title+'/'+i) for i in items};seen=set();qs=[]
  for row,lane in enumerate(ls):
   if lane['parent'] and lane['parent'] not in items:raise ValueError('Missing parent '+lane['parent'])
   prev=quest_ids.get(lane['parent']);lane_quests=[]
   for col,item in enumerate(lane['items']):
    if item in seen:raise ValueError('Duplicate authored goal '+item)
    seen.add(item);label=names.get(item,item.split(':')[1].replace('_',' ').title())
    desc=[lane['note']] if col==0 else []
    # Rewards are deliberately modest and cannot bypass the next technology tier.
    xp=10+min(row,8)*5
    rewards=[dict(id=uid(title+'/'+item+'/xp'),type='xp',xp=xp)] if col%3==0 else []
    if col==len(lane['items'])-1:
     supplies=[('minecraft:iron_ingot',8),('minecraft:redstone',16),('minecraft:gold_ingot',4),('minecraft:quartz',8)] if lane['group']=='Technology' else [('minecraft:lapis_lazuli',12),('minecraft:book',4),('minecraft:amethyst_shard',8),('minecraft:golden_carrot',4)]
     supply,count=supplies[row%len(supplies)]
     rewards.append(dict(id=uid(title+'/'+item+'/supplies'),type='item',item=supply,count=count))
     if not rewards or rewards[0]['type']!='xp':rewards.insert(0,dict(id=uid(title+'/'+item+'/xp'),type='xp',xp=xp+20))
    q=dict(id=quest_ids[item],title=label,subtitle=lane['title'] if col==0 else '',description=desc,icon=item,x=col*2.0,y=row*3.0,shape='hexagon' if col==len(lane['items'])-1 else 'circle',dependencies=[prev] if prev else [],tasks=[dict(id=uid(title+'/'+item+'/task'),type='item',item=item,count=1,consume_items=False)],rewards=rewards)
    qs.append(q);lane_quests.append(q);prev=q['id']
  filename=chapter_key(title)+'.snbt'
  chapter=dict(id=uid('chapter/'+title),title=title,group=gids[ls[0]['group']],order_index=order,default_quest_shape='circle',quests=qs)
  lay_out_lanes(chapter,ls)
  save(chapters/filename,chapter)
  reports.append(dict(chapter=title,quests=len(qs),lanes=len(ls)))
 # Player-facing starter copy stays in the world, with technical notes in README.
 p=chapters/'00_start_here.snbt';d=json.loads(p.read_text())
 for q in d['quests']:
  if q['title']=='Choose your direction':
   q['title']='A foothold in Wombatcraft';q['description']=['Raise a workshop, study the arcane, chart distant lands or build a thriving settlement. Each path has its own discoveries.','', 'Their greatest works meet at the Convergence. Begin with a shelter and follow the branch that calls to you.']
 save(p,d)
 data=json.loads((dest/'data.snbt').read_text());data['title']='Wombatcraft';save(dest/'data.snbt',data)
 replacement_goals={};chapter_heads={}
 for p in chapters.glob('wc_*.snbt'):
  d=json.loads(p.read_text());chapter_heads[d['title']]=d['quests'][0]['id']
  for q in d['quests']:replacement_goals[q['tasks'][0]['item']]=q['id']
 book={}
 for p in chapters.glob('*.snbt'):
  d=json.loads(p.read_text())
  d['title']=d['title'].replace(' & ',' and ')
  for q in d['quests']:
   q['title']=q['title'].replace(' & ',' and ')
   clean=[]
   for text in q.get('description',[]):
    text=re.sub(r'Goal: obtain .*?Keep it in your inventory until the quest records completion\.?','',text)
    text=re.sub(r'This is a manual (?:test objective|objective|project milestone)\..*','',text)
    text=re.sub(r'Follow the connected milestones from left to right\..*','',text)
    text=text.replace('Mark this project after checking the build in game.','').replace('Use JEI to inspect the exact recipe. These ingredients are consumed by crafting; quest detection keeps the resulting item.','')
    text=text.replace('This is the first endgame recipe draft; difficulty and economy balance need a survival playthrough.','Four disciplines, one enduring achievement.')
    text=text.replace(' & ',' and ')
    if text.strip():clean.append(text.strip())
   q['description']=clean
   if q['title']=='Make a home together':q['description']=['Found a team and claim land for your settlement. Invite trusted companions and protect the stores that sustain your home.','', 'Build a market with Lightman\'s Currency. Set a price, stock a shop and trade with fellow travellers.']
   if q['title']=='A different timeship':q['title']='The Type 40 field guide';q['description']=['Some forgotten vessels are far larger within than their weathered shells suggest. Study the Type 40 Manual before seeking a ship of your own.']
   if q['title']=='A bigger interior':q['title']='Across the threshold';q['description']=['Open the doors and step into your vessel. Leave the outside world behind, then return through the same doorway.']
   if q['title']=='A traveling observatory':q['title']='Beyond the familiar';q['description']=['Prepare a vessel for a journey, reach an unfamiliar destination and bring your discoveries safely home.']
   if q['title']=='Loot for everyone':q['description']=['The marked chests hold a separate share for each adventurer. Gather your spoils and leave the container for those who follow.']
   if q['title']=='A shared settlement':q['description']=['Welcome a companion into your team, secure a home and complete a trade. A settlement becomes stronger when its members can rely on one another.']
   fixed=[]
   for dep in q.get('dependencies',[]):
    if dep in old_goals:
     title,item=old_goals[dep];title='Applied Energistics' if title=='AE2' else title
     dep=replacement_goals.get(item,chapter_heads[title])
    fixed.append(dep)
   q['dependencies']=fixed
  d['filename']=p.stem
  if p.stem.startswith(('20_','21_','22_','23_','24_','25_')):
   lay_out_atlas(d)
  book[p.stem]=d
 refine_chapters(book,uid)
 destinations=add_dimensions(book,uid)
 apply_rewards(book,uid,registry)
 save(ROOT/'docs/dimension-atlas.json',destinations)
 for filename,d in book.items():save(chapters/(filename+'.snbt'),d)
 for entry in reports:entry['quests']=len(book[chapter_key(entry['chapter'])]['quests'])
 allq=[q for chapter in book.values() for q in chapter['quests']]
 qids={q['id'] for q in allq};assert len(qids)==len(allq),'Duplicate quest IDs'
 for q in allq:
  assert all(dep in qids for dep in q.get('dependencies',[])),q['title']
 report=dict(authored_chapters=reports,total_quests=len(allq),total_chapters=len(book))
 save(ROOT/'docs/quest-build.json',report);print(json.dumps(report,indent=2))

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--registry',required=True,type=Path);p.add_argument('--index',required=True,type=Path);a=p.parse_args()
 build(set(a.registry.read_text().splitlines()),json.loads(a.index.read_text())['items'])
