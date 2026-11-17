"""Add a read-only navigation layer over the existing FTB quest identities."""
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CHAPTERS=ROOT/'pack/config/ftbquests/quests/chapters'
GENERATED={'wc_main_journey','wc_adventure_journal'}
GROUP='000BB10F5887BBEC'


def uid(key):
    return f"{int(hashlib.sha256(('wombatcraft/journey/'+key).encode()).hexdigest()[:16],16)&0x7FFFFFFFFFFFFFFF:016X}"


def source(chapter, title):
    return {'chapter':chapter,'title':title}


STAGES=[
 ('first_steps','The Journey Begins','survival','Build a safe base, learn recipes and protect your supplies. These foundations support every path.',[
  source('00_start_here',t) for t in ['A foothold in Wombatcraft','A place to begin','The first workshop','Below the surface','Keep what matters','A safe return','Make a home together','Mark the Way Home','Recipes at your fingertips']]),
 ('industry','The Industrial Age','technology','Choose a power route and begin processing materials. Mechanical power, FE machines and Convergence research are complementary options.',[
  source('wc_create',t) for t in ['Water Wheel','Mechanical Press','Mechanical Mixer','Brass Ingot']]+[
  source('wc_mekanism',t) for t in ['Metallurgic Infuser','Basic Energy Cube']]+[source('wc_power','Furnator (Starter)'),source('wc_convergence_progression','Resonant Alloy'),source('wc_convergence_workshop','Research Terminal')]),
 ('arcane','Arcane Discovery','magic','Try a spellbook or a living-mana garden. These routes open utility spells, magical processing and trade with engineers.',[
  source('wc_ars_nouveau',t) for t in ['Novice Spell Book','Source Gem','Imbuement Chamber','Source Jar','Enchanting Apparatus']]+[
  source('wc_botania',t) for t in ['Petal Apothecary','Pure Daisy','Mana Pool']]+[source('wc_spellbooks','Inscription Table')]),
 ('unknown','Into the Unknown','exploration','Prepare food, protection and a way home. Dimensional discoveries supply repeatable Trials and rare reagents; you can explore while friends build.',[
  source('wc_twilight_forest','Where the Sun Stands Still'),source('wc_dimension_aether','Into The Aether'),source('wc_dimension_otherside','Into The Otherside'),
  source('wc_dimension_aether','A Scholar Among Clouds'),source('wc_dimension_aether',"The Altar's Craft"),source('wc_dimension_otherside','A Reinforced Echo'),
  source('wc_frontier_trials','The Muster'),source('wc_frontier_expeditions',"The Surveyor's Kit"),source('wc_frontier_expeditions',"The Hunter's Cabin")]),
 ('engineering','Advanced Engineering','technology','Automate work that you repeat. Storage networks, manufacturing, resource crops and settlements are parallel investments, not a checklist every player must finish.',[
  source('wc_applied_energistics',t) for t in ['ME Crafting Terminal','ME Controller','ME Pattern Provider','Molecular Assembler']]+[
  source('wc_mekanism','Digital Miner'),source('wc_create','Precision Mechanism'),source('wc_mystical_agriculture','Inferium Seeds'),source('wc_mystical_agriculture','Supremium Essence'),source('wc_settlement','Warehouse')]),
 ('stars','Beyond the Stars','space','Build fuel and oxygen infrastructure before launching. Planetary materials unlock Stellar Cores, Astralium and renewable late-game production.',[
  source('11_ad_astra',t) for t in ['Materials for space','Pressure-rated parts','Prepare the fuel','Breathing room','Dress for the journey','Assemble a rocket','Footprints on the Moon','The Red Frontier','Beneath a Burning Sky']]),
 ('powers','Ancient Powers','exploration','Pick a hunt or master a renewable resource route. Boss progression supplies magic and industry, while advanced farming remains a valuable specialty.',[
  source('wc_twilight_forest',t) for t in ['Scales in the Courtyard',"The Lich's Tower",'The Fire Swamp']]+[
  source('wc_dimension_aether',t) for t in ['The Stone Sentinel',"A Queen's Challenge"]]+[
  source('wc_botania','Gaia Unbound'),source('wc_spellbooks','The Last King'),source('wc_mystical_agriculture','Awakened Supremium Essence'),source('wc_convergence_progression','Ancient Core')]),
 ('convergence','The Convergence','convergence','Trade discoveries across the six disciplines. Material tiers and the four legacy sigils lead to the Core, a Matrix and your endgame arsenal.',[
  source('wc_convergence_progression',t) for t in ['Aetherium Ingot','Astralium Ingot','Convergium Ingot']]+[
  source('99_convergence',t) for t in ['Industrial Sigil','Arcane Sigil','Voyager Sigil','Prosperity Sigil','Convergence Core']]+[source('wc_convergence_workshop','Build the Convergence Matrix')]+[
  source('wc_convergence_arsenal',t) for t in ['Give the Matrix a purpose','The Convergence Greatblade','Convergium: the final set']]),
]

BRANCHES={
 'technology':('Build an industry','minecraft:redstone','Power, processing and automation free time for bigger projects.'),
 'magic':('Learn magic','minecraft:enchanted_book','Choose spellcraft or mana production; sell supplies to other specialists.'),
 'exploration':('Explore or hunt a boss','minecraft:compass','Follow dimensional arrivals, structures and boss chains at your own pace.'),
 'space':('Prepare for space','ad_astra:tier_1_rocket','Fuel, oxygen and protective equipment come before the launch.'),
 'nature':('Grow a renewable supply','mysticalagriculture:inferium_essence','Resource farming supports both your settlement and Convergence catalysts.'),
 'settlement':('Build a shared home','minecraft:emerald','Make a settlement, organize storage, trade with players and visit /contracts for repeatable work.'),
 'convergence':('Forge your endgame equipment','wombatcraft_smp:convergium_chestplate','Build the Matrix, forge armaments and upgrade your personal armor and weapons. Trade connects all six specialties.'),
}


def branch_for(chapter, fallback):
    if chapter=='wc_mystical_agriculture':return 'nature'
    if chapter=='wc_settlement':return 'settlement'
    return fallback


def build(book):
    original={name:chapter for name,chapter in book.items() if name not in GENERATED}
    index={q['id']:(name,q) for name,c in original.items() for q in c['quests']}
    catalog=[]; notes=[]; links=[]; seen=set()
    for row,(key,title,branch,description,targets) in enumerate(STAGES):
        notes.append({'id':uid('stage/'+key),'title':title,'x':-4,'y':row*5,'icon':'minecraft:map','optional':True,
                      'description':[description,'This row suggests a direction, not a requirement to finish every mod. Linked icons open the original quests and share their progress and rewards. Use /journey for a current recommendation.'],
                      'tasks':[{'id':uid('stage/'+key+'/read'),'type':'checkmark','title':'Read this route'}],'rewards':[]})
        for col,target in enumerate(targets):
            matches=[q for q in original[target['chapter']]['quests'] if q['title']==target['title']]
            if len(matches)!=1:raise ValueError(f'Ambiguous or missing milestone: {target}')
            quest=matches[0]; qid=quest['id']
            if qid in seen:raise ValueError('Duplicate milestone '+qid)
            seen.add(qid)
            links.append({'id':uid('main-link/'+qid),'linked_quest':qid,'x':col*3,'y':row*5,'size':1.15})
            catalog.append({'quest':qid,'title':quest['title'],'chapter':target['chapter'],'chapter_title':original[target['chapter']]['title'],
                            'stage':row,'stage_title':title,'branch':branch_for(target['chapter'],branch),'purpose':description})
    assert len(catalog)==75
    book['wc_main_journey']={'id':uid('chapter/main'),'filename':'wc_main_journey','title':'Main Journey','group':GROUP,'order_index':-20,
        'icon':'minecraft:map','subtitle':['75 milestones across eight directions. Choose a branch; the detailed chapters remain your reference.'],
        'progression_mode':'flexible','autofocus_id':notes[0]['id'],'quests':notes,'quest_links':links}
    intro={'id':uid('journal/intro'),'title':'What should I do next?','x':-4,'y':0,'icon':'minecraft:book','optional':True,
        'description':['Use /journey for your individual progress, one available objective and alternative paths. Click a suggestion to open its original quest.',
            'Use /journey technology, magic, exploration, space, nature, settlement or convergence to focus on a specialty.',
            'Pin the original quest with the quest-book pin control to keep your chosen objective on screen. Pinned tracking remains your choice.',
            'The cards below are a curated map, not a live recommendation screen. Reading a card never grants an item or replaces the linked objective.'],
        'tasks':[{'id':uid('journal/intro/read'),'type':'checkmark','title':'Choose tonight\'s direction'}],'rewards':[]}
    cards=[intro]; journal_links=[]
    for row,(branch,(title,icon,description)) in enumerate(BRANCHES.items(),1):
        cards.append({'id':uid('journal/'+branch),'title':title,'icon':icon,'x':-4,'y':row*4,'optional':True,
            'description':[description,'Run /journey '+branch+' to find an available step for you. These linked goals show entry, development and a long-term target.'],
            'tasks':[{'id':uid('journal/'+branch+'/read'),'type':'checkmark','title':'Read this path'}],'rewards':[]})
        choices=[m for m in catalog if m['branch']==branch]
        chosen=[]
        for position in [0,len(choices)//2,len(choices)-1]:
            m=choices[position]
            if m['quest'] not in [c['quest'] for c in chosen]:chosen.append(m)
        for col,m in enumerate(chosen):
            journal_links.append({'id':uid('journal-link/'+branch+'/'+m['quest']),'linked_quest':m['quest'],'x':col*4,'y':row*4,'size':1.3})
    book['wc_adventure_journal']={'id':uid('chapter/journal'),'filename':'wc_adventure_journal','title':'What Should I Do?','group':GROUP,'order_index':-30,
        'icon':'minecraft:book','subtitle':['Choose a direction here; use /journey for live personal recommendations.'],'progression_mode':'flexible',
        'autofocus_id':intro['id'],'quests':cards,'quest_links':journal_links}
    inventory=[]
    for name,chapter in sorted(original.items()):
        for q in chapter['quests']:
            mods=sorted({str(t.get('item',t.get('entity',t.get('dimension','')))).split(':')[0] for t in q.get('tasks',[]) if ':' in str(t.get('item',t.get('entity',t.get('dimension',''))))})
            inventory.append({'id':q['id'],'chapter':name,'title':q['title'],'tasks':q.get('tasks',[]),'dependencies':q.get('dependencies',[]),'mods':mods,'guided':q['id'] in seen})
    return catalog,inventory


def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')


def export(book):
    catalog,inventory=build(book)
    save(ROOT/'mods/wombatcraft-smp/src/main/resources/data/wombatcraft_smp/journey/milestones.json',catalog)
    save(ROOT/'docs/journey-inventory.json',inventory)
    save(ROOT/'docs/journey-build.json',{'original_quests':len(inventory),'linked_milestones':len(catalog),'journal_links':len(book['wc_adventure_journal']['quest_links']),
         'guide_quests':sum(len(book[n]['quests']) for n in GENERATED),'chapter_ids':{n:book[n]['id'] for n in sorted(GENERATED)}})


def main():
    book={p.stem:json.loads(p.read_text(encoding='utf-8')) for p in CHAPTERS.glob('*.snbt')}
    export(book)
    for name in GENERATED:save(CHAPTERS/(name+'.snbt'),book[name])


if __name__=='__main__':main()
