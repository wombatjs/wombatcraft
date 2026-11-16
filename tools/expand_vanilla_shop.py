"""Generate a broad supply shop from the recorded vanilla item registry.

Existing buyback offers remain curated. New stock only sells to players.
Run with --registry <item-registry.txt>; output includes a reviewable stock list.
"""
import argparse,json,re,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
CATEGORIES={
 'food':('Food & Farming','bread'),'building':('Stone & Earth','bricks'),
 'decor':('Decoration & Lighting','lantern'),'gear':('Tools & Travel','iron_pickaxe'),
 'utility':('Redstone & Utilities','redstone'),'wood':('Timber & Carpentry','oak_log'),
 'colours':('Colourful Building','blue_concrete'),'plants':('Flowers & Gardens','poppy'),
 'materials':('Materials & Ingredients','iron_ingot'),'nether':('Nether Supplies','netherrack'),
 'end':('End Building','end_stone'),'ocean':('Ocean Supplies','prismarine'),
 'equipment':('Equipment & Clothing','iron_chestplate'),'transport':('Boats & Minecarts','minecart'),
 'records':('Signs & Banners','white_banner')}
EXCLUDE=set('air barrier light bedrock end_portal_frame reinforced_deepslate command_block chain_command_block repeating_command_block command_block_minecart structure_block structure_void jigsaw debug_stick knowledge_book bundle spawner dragon_egg elytra trident nether_star beacon conduit heart_of_the_sea enchanted_golden_apple totem_of_undying ancient_debris netherite_scrap netherite_ingot netherite_block wither_skeleton_skull dragon_head player_head piglin_head echo_shard recovery_compass disc_fragment_5 sniffer_egg turtle_egg sponge wet_sponge shulker_shell potion splash_potion lingering_potion tipped_arrow enchanted_book written_book suspicious_stew filled_map suspicious_sand suspicious_gravel'.split())
EXCLUDE.update('dragon_breath goat_horn firework_star mojang_banner_pattern skull_banner_pattern piglin_banner_pattern'.split())
def category(s):
 if s.endswith(('_boat','_chest_boat','minecart','_raft')):return 'transport'
 if 'banner' in s or 'sign' in s:return 'records'
 if any(s.endswith('_'+x) for x in ['helmet','chestplate','leggings','boots','sword','pickaxe','axe','hoe','shovel']) or s in ['bow','crossbow','shield','fishing_rod','shears']:return 'equipment'
 if any(s.startswith(c+'_') for c in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']) and not s.endswith('_dye'):return 'colours'
 if any(x in s for x in ['prismarine','coral','kelp','sea_pickle','seagrass']):return 'ocean'
 if s.startswith(('end_stone','purpur','chorus')) or s=='end_rod':return 'end'
 if any(x in s for x in ['nether','crimson','warped','soul_','blackstone','basalt','quartz','glowstone','shroomlight','magma','gilded']):return 'nether'
 if any(x in s for x in ['oak','spruce','birch','jungle','acacia','mangrove','cherry','bamboo']) or s in ['stick','ladder','scaffolding']:return 'wood'
 if any(x in s for x in ['sapling','leaves','flower','tulip','rose','fern','azalea','moss','vine','roots','fungus','mushroom','grass','lily','poppy','dandelion','orchid','allium','bluet','daisy','peony','lilac','dead_bush','cactus','spore_blossom','pink_petals']):return 'plants'
 if any(x in s for x in ['cooked','beef','porkchop','chicken','mutton','rabbit','cod','salmon','potato','carrot','beetroot','wheat','seeds','melon','pumpkin','berries','apple','bread','cookie','cake','stew','pie','sugar','egg','honey','milk','bone_meal','cocoa']):return 'food'
 if any(x in s for x in ['stone','granite','diorite','andesite','deepslate','tuff','calcite','dirt','sand','gravel','clay','brick','terracotta','mud','ice','snow','obsidian']):return 'building'
 if any(x in s for x in ['ingot','nugget','raw_','_ore','diamond','emerald','lapis','amethyst','coal','charcoal','flint','leather','feather','string','bone','rotten_flesh','gunpowder','slime_ball','blaze','ender_pearl','ender_eye','ghast_tear','spider_eye','phantom_membrane','paper','book','dye','bowl']):return 'materials'
 if any(x in s for x in ['redstone','piston','hopper','repeater','comparator','observer','dispenser','dropper','lever','button','pressure_plate','rail','tripwire','target','daylight','sculk','tnt','trapped','furnace','smoker','brewing']):return 'utility'
 if any(x in s for x in ['bucket','compass','clock','map','flint_and_steel','name_tag','saddle','lead','spyglass']):return 'gear'
 return 'decor'
def price(s,cat):
 if 'diamond' in s:return 4096 if s=='diamond' else 32768
 if s in ['emerald','ghast_tear','name_tag','saddle','ender_eye']:return 512
 if s in ['ender_pearl','blaze_rod','blaze_powder','phantom_membrane']:return 256
 if s.endswith('_ore') or s in ['raw_iron_block','raw_gold_block','raw_copper_block']:return 2048
 if s in ['gold_block','iron_block','copper_block','emerald_block','lapis_block','redstone_block','coal_block']:return 4096
 if s in ['gold_ingot','raw_gold']:return 256
 if s in ['iron_ingot','raw_iron']:return 128
 if s in ['copper_ingot','raw_copper']:return 64
 if cat=='equipment':return 4096 if s.startswith('golden_') else 1024
 if cat in ['transport','gear']:return 256
 if cat=='materials':return 64
 if cat in ['nether','end','ocean','utility']:return 32
 return 8
def run(registry):
 path=ROOT/'pack/config/lightmanscurrency/PersistentTraders.json';data=json.loads(path.read_text())
 # Keep the original stock and its stable trader IDs. Additional generated traders rebuild deterministically.
 data['Traders']=[t for t in data['Traders'] if not t['ID'].startswith('wombatcraft_supply_')]
 existing={x['SellItem']['ID'] for t in data['Traders'] for x in t['Trades'] if x['TradeType']=='SALE'}
 buybacks={}
 for t in data['Traders']:
  for x in t['Trades']:
   if x['TradeType']=='PURCHASE':
    vals=x['Price']['Value'];assert all(v['Coin']=='lightmanscurrency:coin_copper' for v in vals)
    buybacks[x['SellItem']['ID']]=sum(v['Amount'] for v in vals)/x['SellItem']['Count']
 stock={k:[] for k in CATEGORIES}
 for item in sorted(registry):
  if not item.startswith('minecraft:') or item in existing:continue
  s=item.split(':')[1]
  if s in EXCLUDE or s.startswith(('netherite_','music_disc_')) or s.endswith(('_spawn_egg','_smithing_template','_pottery_sherd','_shulker_box')) or s=='shulker_box' or s in ['axolotl_bucket','tadpole_bucket','salmon_bucket','cod_bucket','pufferfish_bucket','tropical_fish_bucket']:continue
  cat=category(s);unit=max(price(s,cat),math.ceil(buybacks.get(item,0)*8))
  count=1 if cat in ['equipment','gear','transport'] or s.endswith(('_bed','_banner','_banner_pattern','_horse_armor','_stew')) or 'bucket' in s or s in ['cake','beehive','bee_nest','armor_stand','brush','writable_book','rabbit_stew','mushroom_stew','beetroot_soup','carrot_on_a_stick','warped_fungus_on_a_stick'] else 16
  stock[cat].append(dict(TradeType='SALE',SellItem=dict(ID=item,Count=count),Price=dict(type='lightmanscurrency:coins',Chain='main',Value=[dict(Coin='lightmanscurrency:coin_copper',Amount=unit*count)])))
 menu=[]
 for key in ['food','building','decor','gear','utility']:
  title,icon=CATEGORIES[key];menu.append([key,'minecraft:'+icon,title+' | Essentials'])
 for key,offers in stock.items():
  title,icon=CATEGORIES[key]
  for start in range(0,len(offers),54):
   suffix=f'{key}_{start//54+1}';label=title+(' '+str(start//54+1) if len(offers)>54 else '')
   data['Traders'].append(dict(Type='lightmanscurrency:item_trader',ID='wombatcraft_supply_'+suffix,Name='Bram | '+label,OwnerName='Wombatcraft',Trades=offers[start:start+54]))
   menu.append(['supply_'+suffix,'minecraft:'+icon,label])
 menu += [['sell','minecraft:emerald','Bulk Materials Buyback'],['valuables','minecraft:diamond','Valuables Exchange']]
 path.write_text(json.dumps(data,indent=2)+'\n')
 npc=ROOT/'pack/kubejs/server_scripts/wombatcraft_spawn_npcs.js';text=npc.read_text()
 text=re.sub(r'  const categories = \[.*?\];','  const categories = '+json.dumps(menu,indent=4)+';',text,flags=re.S)
 # Paginated selector; native Lightman traders retain their own trade pagination.
 text=text.replace('function wcShopCategories(player) {','function wcShopCategories(player, page) {\n  page = page || 0;')
 text=text.replace('Server Shop").gold(), 5, gui','Server Shop").gold(), 6, gui')
 text=text.replace('gui.slot(0, 0, 8, 4,','gui.slot(0, 0, 8, 5,')
 text=text.replace('categories.forEach((category, index) => {','categories.slice(page * 28, (page + 1) * 28).forEach((category, index) => {')
 text=text.replace('2 + (index % 3) * 2, 1 + Math.floor(index / 3)','1 + (index % 7), 1 + Math.floor(index / 7)')
 text=text.replace("gui.button(4, 4, Item.of('minecraft:arrow')", "if (page > 0) gui.button(1, 5, Item.of('minecraft:arrow'), Text.of('Previous categories'), click => wcShopCategories(player, page - 1));\n    if ((page + 1) * 28 < categories.length) gui.button(7, 5, Item.of('minecraft:arrow'), Text.of('More categories'), click => wcShopCategories(player, page + 1));\n    gui.button(4, 5, Item.of('minecraft:arrow')")
 npc.write_text(text)
 report=dict(added_sale_items=sum(map(len,stock.values())),total_sale_items=len(existing)+sum(map(len,stock.values())),categories=menu,excluded_rare_items=sorted(EXCLUDE),new_stock={k:[t['SellItem']['ID'] for t in v] for k,v in stock.items()})
 (ROOT/'docs/vanilla-shop-stock.json').write_text(json.dumps(report,indent=2)+'\n')
 print('Added',report['added_sale_items'],'supplies;',report['total_sale_items'],'total sale items;',len(menu),'menu entries')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--registry',required=True,type=Path);a=p.parse_args();run(set(a.registry.read_text().splitlines()))
