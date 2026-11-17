"""Reproducible equipment recipes, assets and personal quest objectives."""
import json

NS = 'wombatcraft_smp:'
PARTS = ('helmet', 'chestplate', 'leggings', 'boots')
ITEMS = {
    **{'astralium_'+p: ('Astralium '+p.title(), 'netherite_'+p) for p in PARTS if p != 'boots'},
    **{'convergium_'+p: ('Convergium '+p.title(), 'netherite_'+p) for p in PARTS},
    'aetherium_saber': ('Aetherium Saber', 'diamond_sword'),
    'astralium_blade': ('Astralium Blade', 'netherite_sword'),
    'convergium_greatblade': ('Convergium Greatblade', 'netherite_sword'),
    'convergium_longbow': ('Convergium Longbow', 'bow'),
    'convergium_pickaxe': ('Convergium Pickaxe', 'netherite_pickaxe'),
    'convergium_axe': ('Convergium Axe', 'netherite_axe'),
    'convergium_shovel': ('Convergium Shovel', 'netherite_shovel'),
    'convergence_aegis': ('Convergence Aegis', 'totem_of_undying'),
    'convergent_armament': ('Convergent Armament', 'netherite_upgrade_smithing_template'),
}

def extend_recipes(result):
    def stack(item, count):
        return {'content': {'type':'mbd2:sized','count':count,'ingredient':{'item':item}}}
    result['convergent_armament'] = {
        'type':'wombatcraft:convergence_matrix', 'duration':1200,
        'inputs':{'item':[stack('kubejs:'+name, count) for name,count in (
            ('convergium_ingot',8),('ancient_core',2),('mechanized_core',2),
            ('arcane_core',2),('stabilized_flux',4),('living_relic',1))],
            'forge_energy':[{'content':4000000}], 'botania_mana':[{'content':400000}]},
        'outputs':{'item':[stack(NS+'convergent_armament',1)]}}
    return result

def build(root, save, labels):
    for item,profile in {'aetherium_saber':'cutlass','astralium_blade':'sword','convergium_greatblade':'claymore','convergium_axe':'axe'}.items():
        save('data/wombatcraft_smp/weapon_attributes/'+item+'.json', {'parent':'bettercombat:'+profile})
    patterns = {'helmet':['AAA','A A'], 'chestplate':['A A','AAA','AAA'], 'leggings':['AAA','A A','A A']}
    for item,(title,texture) in ITEMS.items():
        labels['item.wombatcraft_smp.'+item] = title
        model={'parent':'minecraft:item/'+('handheld' if any(item.endswith(x) for x in ('saber','blade','pickaxe','axe','shovel')) else 'generated'), 'textures':{'layer0':'minecraft:item/'+texture}}
        if item=='convergium_longbow':
            # The vanilla parent carries draw transforms/overrides; register bow predicates on the client.
            model={'parent':'minecraft:item/bow'}
        save('assets/wombatcraft_smp/models/item/'+item+'.json',model)
    for part,pattern in patterns.items():
        save('data/wombatcraft_smp/recipes/astralium_'+part+'.json', {'type':'minecraft:crafting_shaped','pattern':pattern,'key':{'A':{'item':'kubejs:astralium_ingot'}},'result':{'item':NS+'astralium_'+part}})
    save('data/wombatcraft_smp/recipes/aetherium_saber.json', {'type':'minecraft:crafting_shaped','pattern':[' A ',' A ',' C '],'key':{'A':{'item':'kubejs:aetherium_ingot'},'C':{'item':'kubejs:arcane_core'}},'result':{'item':NS+'aetherium_saber'}})
    def smith(name,base,addition):
        save('data/wombatcraft_smp/recipes/'+name+'.json',{'type':'minecraft:smithing_transform','template':{'item':'minecraft:netherite_upgrade_smithing_template'},'base':{'item':base},'addition':{'item':addition},'result':{'item':NS+name}})
    smith('astralium_blade',NS+'aetherium_saber','kubejs:astralium_ingot')
    for part in PARTS: smith('convergium_'+part,NS+'astralium_'+part,NS+'convergent_armament')
    for tool in ('pickaxe','axe','shovel'): smith('convergium_'+tool,'minecraft:netherite_'+tool,NS+'convergent_armament')
    smith('convergium_greatblade',NS+'astralium_blade',NS+'convergent_armament')
    smith('convergium_longbow','minecraft:bow',NS+'convergent_armament')
    save('data/wombatcraft_smp/recipes/convergence_aegis.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':NS+'convergent_armament'},{'item':'kubejs:living_relic'},{'item':'minecraft:totem_of_undying'}],'result':{'item':NS+'convergence_aegis'}})

def add_chapter(book,uid):
    nodes=[
        ('saber','A blade worth forging',['aetherium_saber'],[], '12 attack damage and 2 attacks per second before enchantments. Repair with netherite. Craft from Aetherium and an Arcane Core.'),
        ('astral','Astralium expedition armor',['astralium_'+p for p in PARTS],['saber'],'24 armor, 12 toughness and 40% knockback resistance across the full set. Full set reduces ordinary non-player damage by 15%; boots halve fall damage. Repair with Astralium. Armor-bypassing damage and player attacks are excluded from the set bonus.'),
        ('blade','The Astralium edge',['astralium_blade'],['saber'],'18 attack damage and 1.8 attacks per second. Upgrade an Aetherium Saber at a smithing table using an Astralium ingot and Netherite Upgrade template; enchantments, name and damage are retained. Repair with netherite.'),
        ('armament','Give the Matrix a purpose',['convergent_armament'],['astral','blade'],'In the Convergence Matrix, bind 8 Convergium, 2 Ancient Cores, 2 Mechanized Cores, 2 Arcane Cores, 4 Stabilized Flux and 1 Living Relic. Costs 4,000,000 FE, 400,000 mana and 60 seconds. One armament upgrades one piece. The Matrix itself requires the Convergence Core.'),
        ('armor','Convergium: the final set',['convergium_'+p for p in PARTS],['armament'],'Smith each Astralium piece with an armament and Netherite Upgrade template. Full set: 30 armor, 16 toughness, 80% knockback resistance, 35% less ordinary non-player damage and 75% less fall damage. Enchantments are retained. Repair with Convergium. Void, sacrifice and armor-bypassing damage remain dangerous; no oxygen or creative flight is granted.'),
        ('greatblade','The Convergence Greatblade',['convergium_greatblade'],['armament'],'Upgrade your Astralium Blade. 32 base attack damage, 1.4 attacks per second, 4096 durability. Uses the two-handed Better Combat claymore moveset; combo hits have their normal damage multipliers. Use one Field Charge for Strength III and Speed II for 10 seconds; 30-second cooldown. Attacks use normal combat and protection rules.'),
        ('bow','An endgame ranged weapon',['convergium_longbow'],['armament'],'Upgrade a bow with an armament and Netherite Upgrade template. Arrows have 2.5 times base damage; normal draw speed, ammunition, enchantments and friendly-fire rules apply. 3072 durability; repair with Convergium.'),
        ('tools','An industrial toolbelt',['convergium_pickaxe','convergium_axe','convergium_shovel'],['armament'],'Upgrade netherite tools with an armament and Netherite Upgrade template. Mining speed 18, 4096 durability, enchantability 24. Retains enchantments including Fortune or Silk Touch. No area breaking: ordinary block protection and drops apply. Repair with Convergium.'),
        ('aegis','A second wind',['convergence_aegis'],['armament'],'Combine an armament, Living Relic and Totem of Undying. Use one Field Charge for 8 absorption hearts for 20 seconds and Regeneration II for 5 seconds. 60-second cooldown shared by all copies you carry. This is an active survival tool, not an automatic resurrection totem.'),
    ]
    quests=[]
    for i,(key,title,items,parents,description) in enumerate(nodes):
        # Ranged fighters and builders can reach Matrix upgrades without completing the melee route.
        if key=='armament': parents=[]
        elif key=='astral': parents=[]
        elif key=='armor': parents=['armament','astral']
        elif key=='greatblade': parents=['armament','blade']
        quests.append(dict(id=uid('arsenal/'+key),title=title,icon=NS+items[0],x=(i%3)*5,y=(i//3)*5,
            dependencies=[uid('arsenal/'+p) for p in parents]+([uid('workshop/matrix')] if key=='armament' else []), description=[description,'Progress is personal. Trade is welcome. These tasks do not consume equipment or award duplicate gear.'],
            tasks=[dict(id=uid('arsenal/'+key+'/'+item),type='item',item=NS+item,count=1,consume_items=False) for item in items],rewards=[]))
    filename='wc_convergence_arsenal'
    book[filename]=dict(id=uid('chapter/'+filename),filename=filename,title='Convergence: The Arsenal',group='0F095C8E4FA42163',order_index=61,icon=NS+'convergium_greatblade',quests=quests)
