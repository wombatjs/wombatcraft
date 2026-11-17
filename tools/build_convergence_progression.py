"""Author the first Convergence progression slice; no upstream recipes are removed."""
import hashlib
import json
from pathlib import Path
from convergence_gameplay import EXTRA_ITEMS, STATIONS, ARCANE_DAMAGE, build_stations, extend_recipes, recipe_type, add_workshop_chapter

ROOT = Path(__file__).resolve().parents[1]
PREFIX = 'wombatcraft:progression/'
ITEMS = {
    'resonant_alloy': ('Resonant Alloy', 'minecraft:item/iron_ingot'),
    'aetherium_ingot': ('Aetherium Ingot', 'minecraft:item/gold_ingot'),
    'astralium_ingot': ('Astralium Ingot', 'minecraft:item/netherite_ingot'),
    'convergium_ingot': ('Convergium Ingot', 'minecraft:item/copper_ingot'),
    'mechanized_core': ('Mechanized Core', 'minecraft:item/clock'),
    'arcane_core': ('Arcane Core', 'minecraft:item/amethyst_shard'),
    'stellar_core': ('Stellar Core', 'minecraft:item/firework_star'),
    'ancient_core': ('Ancient Core', 'minecraft:item/nautilus_shell'),
    'primal_core': ('Primal Core', 'minecraft:item/heart_of_the_sea'),
    'temporal_core': ('Temporal Core', 'minecraft:item/ender_eye'),
    'living_relic': ('Living Relic', 'minecraft:item/totem_of_undying'),
}
ITEMS.update(EXTRA_ITEMS)


def recipes():
    result = {}

    def craft(name, output, ingredients, count=1):
        result[name] = {'type': 'minecraft:crafting_shapeless',
                        'ingredients': [{'item': i} for i in ingredients],
                        'result': {'item': output, 'count': count}}

    def forge(name, output, inputs, count=1, fe=200000, mana=12000, ticks=200):
        def stack(item, amount):
            return {'content': {'type': 'mbd2:sized', 'count': amount, 'ingredient': {'item': item}}}
        result[name] = {'type': 'wombatcraft:convergence_forging', 'duration': ticks,
                        'inputs': {'item': [stack(i, n) for i, n in inputs],
                                   'forge_energy': [{'content': fe}], 'botania_mana': [{'content': mana}]},
                        'outputs': {'item': [stack(output, count)]}}

    craft('resonant_alloy', 'kubejs:resonant_alloy', ['create:andesite_alloy', 'minecraft:copper_ingot', 'minecraft:redstone', 'minecraft:amethyst_shard'], 4)
    craft('mechanized_core', 'kubejs:mechanized_core', ['kubejs:resonant_alloy', 'create:precision_mechanism', 'mekanism:advanced_control_circuit', 'ae2:engineering_processor'])
    craft('arcane_core', 'kubejs:arcane_core', ['kubejs:resonant_alloy', 'botania:mana_diamond', 'ars_nouveau:source_gem', 'botania:manasteel_ingot'])
    craft('stellar_core', 'kubejs:stellar_core', ['kubejs:resonant_alloy', 'ad_astra:ostrum_ingot', 'ad_astra:desh_ingot', 'ad_astra:calorite_ingot'])
    craft('ancient_core', 'kubejs:ancient_core', ['kubejs:resonant_alloy', 'kubejs:trial_twilight_crest', 'minecraft:nether_star'])
    craft('primal_core', 'kubejs:primal_core', ['kubejs:resonant_alloy', 'mysticalagriculture:supremium_essence', 'minecraft:honey_bottle', 'minecraft:golden_carrot'])
    craft('temporal_core', 'kubejs:temporal_core', ['kubejs:resonant_alloy', 'kubejs:trial_aether_crest', 'kubejs:trial_otherside_crest', 'minecraft:ender_pearl'])
    forge('aetherium', 'kubejs:aetherium_ingot', [('kubejs:resonant_alloy', 4), ('kubejs:arcane_core', 1), ('kubejs:temporal_core', 1)], 4)
    forge('astralium', 'kubejs:astralium_ingot', [('kubejs:aetherium_ingot', 4), ('kubejs:stellar_core', 1), ('kubejs:mechanized_core', 1)], 4, 600000, 30000, 300)
    forge('living_relic', 'kubejs:living_relic', [('kubejs:ancient_core', 1), ('kubejs:primal_core', 1)], 1, 400000, 24000, 240)
    forge('convergium', 'kubejs:convergium_ingot', [('kubejs:astralium_ingot', 4), ('kubejs:living_relic', 1), ('kubejs:mechanized_core', 1), ('kubejs:arcane_core', 1), ('kubejs:stellar_core', 1), ('kubejs:temporal_core', 1)], 4, 2000000, 120000, 600)
    # Costly, renewable alternatives keep repeat production independent of trial farming.
    forge('ancient_core_industrial', 'kubejs:ancient_core', [('kubejs:aetherium_ingot', 8), ('minecraft:nether_star', 4), ('botania:terrasteel_ingot', 4)], 1, 1000000, 60000, 600)
    forge('temporal_core_industrial', 'kubejs:temporal_core', [('kubejs:aetherium_ingot', 8), ('minecraft:ender_pearl', 16), ('botania:mana_diamond', 8)], 1, 1000000, 60000, 600)
    craft('resonant_circuits', 'mekanism:basic_control_circuit', ['kubejs:resonant_alloy', 'minecraft:redstone', 'minecraft:redstone'], 2)
    craft('resonant_tracks', 'minecraft:powered_rail', ['kubejs:resonant_alloy', 'minecraft:gold_ingot', 'minecraft:stick'], 12)
    forge('mechanized_processors', 'ae2:engineering_processor', [('kubejs:mechanized_core', 1), ('ae2:printed_silicon', 8), ('minecraft:diamond', 8), ('minecraft:redstone', 8)], 8)
    forge('arcane_industry', 'botania:manasteel_ingot', [('kubejs:arcane_core', 1), ('minecraft:iron_ingot', 32)], 32)
    forge('stellar_industry', 'ad_astra:calorite_ingot', [('kubejs:stellar_core', 1), ('ad_astra:calorite_ingot', 4), ('kubejs:aetherium_ingot', 8)], 8, 800000, 40000, 400)
    forge('primal_harvest', 'mysticalagriculture:supremium_farmland', [('kubejs:primal_core', 1), ('minecraft:dirt', 4), ('minecraft:wheat', 32)], 4)
    forge('ancient_beacon', 'minecraft:beacon', [('kubejs:ancient_core', 1), ('minecraft:obsidian', 3), ('minecraft:glass', 5)])
    forge('temporal_travel', 'waystones:warp_stone', [('kubejs:temporal_core', 1), ('minecraft:ender_pearl', 4), ('minecraft:amethyst_shard', 8)])
    forge('aetherium_storage', 'ae2:fluix_crystal', [('kubejs:aetherium_ingot', 1), ('ae2:certus_quartz_crystal', 16), ('minecraft:redstone', 16)], 32)
    forge('astralium_travel', 'minecraft:elytra', [('kubejs:astralium_ingot', 4), ('kubejs:stellar_core', 1), ('minecraft:phantom_membrane', 16), ('minecraft:feather', 32)], 1, 1000000, 60000, 600)
    forge('convergium_automation', 'create:precision_mechanism', [('kubejs:convergium_ingot', 1), ('minecraft:gold_ingot', 16), ('create:cogwheel', 16)], 16, 1000000, 60000, 400)
    forge('convergium_storage', 'ae2:singularity', [('kubejs:convergium_ingot', 4), ('ae2:fluix_crystal', 64)], 1, 2000000, 120000, 600)
    return extend_recipes(result)


def add_chapter(book, uid):
    filename = 'wc_convergence_progression'
    nodes = [
        ('resonant_alloy', [], 0, 0, 'Start with Create and an amethyst discovery. Resonant Alloy supports circuits and railways. Choose a specialty or trade with another workshop.'),
        ('mechanized_core', ['resonant_alloy'], -6, 3, 'Industry supplies circuits, precision mechanisms and processors. Spend Mechanized Cores on processor batches in the Forge.'),
        ('arcane_core', ['resonant_alloy'], -3, 3, 'Combine Botania and Ars Nouveau. Arcane Cores support bulk manasteel production and Aetherium.'),
        ('stellar_core', ['resonant_alloy'], 0, 3, 'Explore the Ad Astra planets for desh, ostrum and calorite. Stellar Cores support Astralium and renewable calorite production.'),
        ('ancient_core', ['resonant_alloy'], 3, 3, 'Win the repeatable Thornwood Trial and defeat a Wither. Ancient Cores become beacons or Living Relics. An expensive Dimensional Stabilizer alternative supports repeat production.'),
        ('primal_core', ['resonant_alloy'], 6, 3, 'Grow Mystical Agriculture essence and maintain a farm. Primal Cores supply supremium farmland or bind ancient discoveries into Living Relics.'),
        ('temporal_core', ['resonant_alloy'], 9, 3, 'Complete the repeatable High Sky and Echo Trials. Temporal Cores support travel and Aetherium; expensive industrial production becomes available after building the Dimensional Stabilizer.'),
        ('aetherium_ingot', ['arcane_core', 'temporal_core'], -3, 6, 'Combine four Resonant Alloy with Arcane and Temporal Cores in the Convergence Forge. Supply 200,000 FE and 12,000 mana. Aetherium supports storage components and repeatable core production.'),
        ('astralium_ingot', ['aetherium_ingot', 'stellar_core', 'mechanized_core'], 0, 9, 'Combine four Aetherium with Stellar and Mechanized Cores. Supply 600,000 FE and 30,000 mana. Astralium can produce elytra from renewable materials, supplying explorers and the Voyager Sigil without exhausting End ships.'),
        ('living_relic', ['ancient_core', 'primal_core'], 6, 6, 'Fuse Ancient and Primal Cores with 400,000 FE and 24,000 mana. This compact reagent brings adventure and nature into the final alloy.'),
        ('convergium_ingot', ['astralium_ingot', 'living_relic'], 3, 12, 'All six disciplines meet here. Forge four Astralium, a Living Relic, and one each of the Mechanized, Arcane, Stellar and Temporal Cores with 2,000,000 FE and 120,000 mana. Spend Convergium on automation, singularities or the Convergence Core.'),
    ]
    quests = []
    for key, parents, x, y, description in nodes:
        quests.append(dict(id=uid('progression/'+key), title=ITEMS[key][0], icon='kubejs:'+key,
                           x=x, y=y, description=[description, 'Trade is welcome: these tasks detect items without consuming them. Quest progress belongs to you, even when you join or leave a faction. Recipes remain tradeable.'],
                           dependencies=[uid('progression/'+p) for p in parents],
                           tasks=[dict(id=uid('progression/'+key+'/task'), type='item', item='kubejs:'+key, count=1, consume_items=False)], rewards=[]))
    book[filename] = dict(id=uid('chapter/'+filename), filename=filename, title='The Convergence: Six Disciplines',
                          group='0F095C8E4FA42163', order_index=59, icon='kubejs:resonant_alloy', quests=quests)
    quests.append(dict(id=uid('progression/ore'), title='Resonant deposits', icon='wombatcraft_smp:resonant_ore', x=-3, y=0,
                       description=['Look for violet Resonant Ore in newly explored Overworld stone between Y 0 and 80. Mine with an iron pickaxe or better. Its crystals double the alloy yield. Existing worlds can use amethyst instead; no reset is needed.'],
                       tasks=[dict(id=uid('progression/ore/task'), type='item', item='wombatcraft_smp:resonant_crystal', count=1, consume_items=False)], rewards=[]))
    quests.append(dict(id=uid('progression/armor'), title='Arcane field equipment', icon='wombatcraft_smp:aetherium_chestplate', x=-6, y=9,
                       dependencies=[uid('progression/aetherium_ingot')],
                       description=['Craft all four Aetherium armor pieces using the usual armor patterns. A complete set reduces magic and indirect magic damage from non-player sources by 20%. Defense and durability match diamond armor; repair with diamonds. Astralium Boots trade the full-set bonus for reduced fall damage.'],
                       tasks=[dict(id=uid('progression/armor/'+p), type='item', item='wombatcraft_smp:aetherium_'+p, count=1, consume_items=False) for p in ('helmet', 'chestplate', 'leggings', 'boots')], rewards=[]))
    if '99_convergence' in book:
        for quest in book['99_convergence']['quests']:
            if quest['title'] == 'Convergence Core':
                quest['description'] = ['Combine the four legacy sigils with awakened cores, Nether Stars and Convergium. All six disciplines contribute to the alloy; specialists can trade their work.']
    add_workshop_chapter(book, uid)


def main():
    startup = ['// Generated by tools/build_convergence_progression.py; restart both client and server.', 'StartupEvents.registry("item", event => {']
    for key, (title, texture) in ITEMS.items():
        startup.append(f'  event.create("{key}").displayName("{title}").texture("{texture}").glow(true);')
    startup.append('});\n')
    (ROOT/'pack/kubejs/startup_scripts/convergence_progression.js').write_text('\n'.join(startup), encoding='utf-8')
    folder = ROOT/'pack/kubejs/data/wombatcraft/recipes/progression'
    folder.mkdir(parents=True, exist_ok=True)
    for name, recipe in recipes().items():
        (folder/(name+'.json')).write_text(json.dumps(recipe, indent=2)+'\n', encoding='utf-8')
    legacy = ROOT/'pack/config/ftbquests/quests/chapters/99_convergence.snbt'
    book = {'99_convergence': json.loads(legacy.read_text(encoding='utf-8'))}
    def uid(s):
        return f"{int(hashlib.sha256(('wombatcraft/'+s).encode()).hexdigest()[:16],16) & 0x7FFFFFFFFFFFFFFF:016X}"
    add_chapter(book, uid)
    for name, chapter in book.items():
        (ROOT/'pack/config/ftbquests/quests/chapters'/(name+'.snbt')).write_text(json.dumps(chapter, indent=2)+'\n', encoding='utf-8')
    build_equipment()
    build_stations()
    build_machine_audit()


def build_machine_audit():
    path = ROOT/'tools/qa/FrontierAudit.java'
    text = path.read_text(encoding='utf-8')
    begin = '        // BEGIN GENERATED PROGRESSION AUDIT'
    end = '        // END GENERATED PROGRESSION AUDIT'
    lines = [begin]
    for name, recipe in recipes().items():
        if 'inputs' not in recipe:
            continue
        ins = recipe['inputs']
        out = recipe['outputs']['item'][0]['content']
        items = ','.join(json.dumps(x['content']['ingredient']['item']) for x in ins['item'])
        counts = ','.join(str(x['content']['count']) for x in ins['item'])
        machine = next(k for k in STATIONS if recipe_type(k) == recipe['type'])
        lines.extend([f'        // {name}', f'        if (machineId.equals("{machine}"))',
                      f'        forge(logic, inputs, products, energy, mana, set, get, charge, fillMana, empty, {recipe["duration"]}, {ins["forge_energy"][0]["content"]}, {ins["botania_mana"][0]["content"]},',
                      f'            "{out["ingredient"]["item"]}", {out["count"]}, new String[]{{{items}}}, new int[]{{{counts}}});'])
    lines.append(end)
    start = text.index(begin)
    finish = text.index(end, start) + len(end)
    path.write_text(text[:start]+'\n'.join(lines)+text[finish:], encoding='utf-8')


def build_equipment():
    root = ROOT/'mods/wombatcraft-smp/src/main/resources'
    def save(path, value):
        target = root/path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(value, indent=2)+'\n', encoding='utf-8')
    ns = 'wombatcraft_smp'
    save(f'assets/{ns}/blockstates/resonant_ore.json', {'variants': {'': {'model': ns+':block/resonant_ore'}}})
    save(f'assets/{ns}/models/block/resonant_ore.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'minecraft:block/amethyst_block'}})
    save(f'assets/{ns}/models/item/resonant_ore.json', {'parent': ns+':block/resonant_ore'})
    save(f'assets/{ns}/models/item/resonant_crystal.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/amethyst_shard'}})
    labels = {f'block.{ns}.resonant_ore': 'Resonant Ore', f'item.{ns}.resonant_crystal': 'Resonant Crystal'}
    patterns = {'helmet': ['AAA', 'A A'], 'chestplate': ['A A', 'AAA', 'AAA'], 'leggings': ['AAA', 'A A', 'A A'], 'boots': ['A A', 'A A']}
    for part, pattern in patterns.items():
        name = 'aetherium_'+part
        labels[f'item.{ns}.{name}'] = 'Aetherium '+part.title()
        save(f'assets/{ns}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/diamond_'+part}})
        save(f'data/{ns}/recipes/{name}.json', {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {'A': {'item': 'kubejs:aetherium_ingot'}}, 'result': {'item': ns+':'+name}})
    labels[f'block.{ns}.research_terminal'] = 'Research Terminal'
    save(f'assets/{ns}/blockstates/research_terminal.json', {'variants': {'': {'model': ns+':block/research_terminal'}}})
    save(f'assets/{ns}/models/block/research_terminal.json', {'parent': 'minecraft:block/orientable', 'textures': {'side': 'minecraft:block/lodestone_side', 'front': 'minecraft:block/bookshelf', 'top': 'minecraft:block/enchanting_table_top'}})
    save(f'assets/{ns}/models/item/research_terminal.json', {'parent': ns+':block/research_terminal'})
    save(f'data/{ns}/loot_tables/blocks/research_terminal.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'conditions': [{'condition': 'minecraft:survives_explosion'}], 'entries': [{'type': 'minecraft:item', 'name': ns+':research_terminal'}]}]})
    for key, title, texture in [('astralium_boots', 'Astralium Boots', 'diamond_boots'), ('field_projector', 'Expedition Field Projector', 'spyglass'), ('resonant_prospector', 'Resonant Prospector', 'compass')]:
        labels[f'item.{ns}.{key}'] = title
        save(f'assets/{ns}/models/item/{key}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'minecraft:item/'+texture}})
    save(f'assets/{ns}/lang/en_us.json', labels)
    save(f'data/{ns}/tags/damage_type/arcane_damage.json', {'replace': False, 'values': ARCANE_DAMAGE})
    save('data/minecraft/tags/blocks/mineable/pickaxe.json', {'replace': False, 'values': [ns+':resonant_ore', ns+':research_terminal']})
    save('data/minecraft/tags/blocks/needs_iron_tool.json', {'replace': False, 'values': [ns+':resonant_ore']})
    save(f'data/{ns}/loot_tables/blocks/resonant_ore.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'name': ns+':resonant_ore', 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'enchantments': [{'enchantment': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}]},
        {'type': 'minecraft:item', 'name': ns+':resonant_crystal', 'functions': [{'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}, {'function': 'minecraft:explosion_decay'}]}
    ]}]}]})
    save(f'data/{ns}/worldgen/configured_feature/resonant_ore.json', {'type': 'minecraft:ore', 'config': {'size': 4, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:stone_ore_replaceables'}, 'state': {'Name': ns+':resonant_ore'}}]}})
    save(f'data/{ns}/worldgen/placed_feature/resonant_ore.json', {'feature': ns+':resonant_ore', 'placement': [
        {'type': 'minecraft:count', 'count': 4}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 0}, 'max_inclusive': {'absolute': 80}}}, {'type': 'minecraft:biome'}]})
    save(f'data/{ns}/forge/biome_modifier/resonant_ore.json', {'type': 'forge:add_features', 'biomes': '#minecraft:is_overworld', 'features': ns+':resonant_ore', 'step': 'underground_ores'})
    save(f'data/{ns}/recipes/resonant_alloy_from_crystal.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': ns+':resonant_crystal'}, {'item': 'create:andesite_alloy'}, {'item': 'minecraft:copper_ingot'}, {'item': 'minecraft:redstone'}], 'result': {'item': 'kubejs:resonant_alloy', 'count': 8}})


if __name__ == '__main__':
    main()
