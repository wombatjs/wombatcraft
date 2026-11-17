"""Pack-specific production loop, stations and renewable specialist outputs."""
import json
from pathlib import Path
import nbtlib

ROOT = Path(__file__).resolve().parents[1]
STATIONS = {
    'convergence_forge': ('Convergence Forge', 'create:brass_block', 'minecraft:crying_obsidian'),
    'arcane_infuser': ('Arcane Infuser', 'botania:manasteel_block', 'minecraft:amethyst_block'),
    'dimensional_stabilizer': ('Dimensional Stabilizer', 'ad_astra:ostrum_block', 'minecraft:crying_obsidian'),
    'convergence_matrix': ('Convergence Matrix', 'minecraft:netherite_block', 'minecraft:beacon'),
}
EXTRA_ITEMS = {
    'infused_catalyst': ('Infused Catalyst', 'minecraft:item/blaze_powder'),
    'stabilized_flux': ('Stabilized Flux', 'minecraft:item/ender_pearl'),
    'field_charge': ('Field Charge', 'minecraft:item/prismarine_crystals'),
}
ARCANE_DAMAGE = ['minecraft:magic', 'minecraft:indirect_magic']
ARCANE_DAMAGE += ['ars_nouveau:'+name for name in ('crush','flare','frost','spell','windshear')]
ARCANE_DAMAGE += ['irons_spellbooks:'+name+'_magic' for name in ('blood','eldritch','ender','evocation','fire','holy','ice','lightning','nature')]
ARCANE_DAMAGE += ['mna:spell_'+name for name in ('briars','conflagrate','disperse','frost','lightning')]
ARCANE_DAMAGE += ['cataclysm:'+name for name in ('abyssal_burn','flame_strike','maledictio_magicae')]


def recipe_type(station):
    return 'wombatcraft:' + ('convergence_forging' if station == 'convergence_forge' else station)


def extend_recipes(result):
    def craft(name, output, items, count=1):
        result[name] = {'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': i} for i in items], 'result': {'item': output, 'count': count}}

    def process(name, station, output, items, count=1, fe=400000, mana=24000, ticks=300):
        def stack(i, n):
            return {'content': {'type': 'mbd2:sized', 'count': n, 'ingredient': {'item': i}}}
        result[name] = {'type': recipe_type(station), 'duration': ticks,
                        'inputs': {'item': [stack(i, n) for i, n in items], 'forge_energy': [{'content': fe}], 'botania_mana': [{'content': mana}]},
                        'outputs': {'item': [stack(output, count)]}}

    # Existing tiers stay in the Forge so neither new station creates a bootstrap cycle.
    for name in ('living_relic', 'arcane_industry', 'primal_harvest', 'aetherium_storage'):
        result[name]['type'] = recipe_type('arcane_infuser')
    for name in ('ancient_core_industrial', 'temporal_core_industrial', 'stellar_industry', 'temporal_travel', 'astralium_travel'):
        result[name]['type'] = recipe_type('dimensional_stabilizer')

    craft('research_terminal', 'wombatcraft_smp:research_terminal', ['kubejs:resonant_alloy'] * 4 + ['minecraft:book', 'minecraft:glass', 'minecraft:redstone'])
    craft('arcane_infuser', 'wombatcraft:arcane_infuser', ['kubejs:aetherium_ingot'] * 4 + ['kubejs:arcane_core', 'botania:mana_pool', 'ars_nouveau:source_gem', 'create:precision_mechanism'])
    craft('dimensional_stabilizer', 'wombatcraft:dimensional_stabilizer', ['kubejs:astralium_ingot'] * 4 + ['kubejs:temporal_core', 'ad_astra:ostrum_ingot', 'minecraft:ender_eye', 'ae2:engineering_processor'])
    craft('convergence_matrix', 'wombatcraft:convergence_matrix', ['kubejs:convergence_core'] + ['kubejs:convergium_ingot'] * 4 + ['kubejs:stellar_core', 'kubejs:ancient_core', 'kubejs:primal_core', 'kubejs:temporal_core'])
    craft('field_projector', 'wombatcraft_smp:field_projector', ['kubejs:aetherium_ingot'] * 3 + ['kubejs:primal_core', 'minecraft:glass', 'minecraft:compass'])
    craft('astralium_boots', 'wombatcraft_smp:astralium_boots', ['kubejs:astralium_ingot'] * 4 + ['minecraft:feather', 'kubejs:temporal_core'])
    craft('resonant_prospector', 'wombatcraft_smp:resonant_prospector', ['kubejs:resonant_alloy'] * 4 + ['minecraft:compass', 'minecraft:amethyst_shard'])

    infuser = 'arcane_infuser'
    stabilizer = 'dimensional_stabilizer'
    matrix = 'convergence_matrix'
    process('infused_catalyst', infuser, 'kubejs:infused_catalyst', [('kubejs:arcane_core', 1), ('kubejs:primal_core', 1), ('ars_nouveau:source_gem', 8)], 8)
    process('stabilized_flux', stabilizer, 'kubejs:stabilized_flux', [('kubejs:temporal_core', 1), ('kubejs:stellar_core', 1), ('minecraft:ender_pearl', 8)], 8, 800000, 48000, 400)
    process('field_charge', infuser, 'kubejs:field_charge', [('kubejs:infused_catalyst', 1), ('minecraft:prismarine_crystals', 8), ('minecraft:glowstone_dust', 8)], 8, 200000, 12000, 200)
    process('infused_circuits', infuser, 'mekanism:advanced_control_circuit', [('kubejs:mechanized_core', 1), ('kubejs:infused_catalyst', 1), ('minecraft:iron_ingot', 8), ('minecraft:redstone', 16)], 8)
    process('infused_terrasteel', infuser, 'botania:terrasteel_ingot', [('kubejs:infused_catalyst', 2), ('botania:manasteel_ingot', 4), ('botania:mana_diamond', 4), ('botania:mana_pearl', 4)], 4, 1200000, 500000, 600)
    process('primal_renewal', infuser, 'mysticalagriculture:supremium_essence', [('kubejs:primal_core', 1), ('mysticalagriculture:imperium_essence', 8), ('kubejs:infused_catalyst', 1)], 4, 800000, 60000, 400)
    process('resonant_recrystallization', infuser, 'wombatcraft_smp:resonant_crystal', [('kubejs:infused_catalyst', 1), ('minecraft:amethyst_shard', 16), ('minecraft:redstone', 16)], 16, 300000, 18000, 300)
    process('desh_reconstitution', stabilizer, 'ad_astra:desh_ingot', [('kubejs:stellar_core', 1), ('kubejs:stabilized_flux', 1), ('minecraft:iron_ingot', 16)], 16, 800000, 48000, 400)
    process('ostrum_reconstitution', stabilizer, 'ad_astra:ostrum_ingot', [('kubejs:stellar_core', 1), ('kubejs:stabilized_flux', 2), ('ad_astra:desh_ingot', 16)], 16, 1200000, 60000, 500)
    process('overclocked_processors', infuser, 'ae2:engineering_processor', [('kubejs:mechanized_core', 1), ('kubejs:stabilized_flux', 1), ('ae2:printed_silicon', 8), ('minecraft:diamond', 8), ('minecraft:redstone', 8)], 8, 400000, 24000, 50)
    process('overclocked_astralium', stabilizer, 'kubejs:astralium_ingot', [('kubejs:aetherium_ingot', 4), ('kubejs:stellar_core', 1), ('kubejs:mechanized_core', 1), ('kubejs:stabilized_flux', 1)], 4, 1200000, 60000, 75)
    # The final Core becomes infrastructure. All benefits keep buying specialist consumables.
    process('matrix_industry', matrix, 'mekanism:pellet_polonium', [('kubejs:mechanized_core', 2), ('kubejs:stabilized_flux', 4), ('mekanism:ingot_uranium', 16)], 4, 3000000, 180000, 800)
    process('matrix_arcane', matrix, 'botania:gaia_ingot', [('kubejs:arcane_core', 2), ('kubejs:ancient_core', 1), ('kubejs:infused_catalyst', 4), ('botania:terrasteel_ingot', 4)], 4, 3000000, 300000, 800)
    process('matrix_nature', matrix, 'mysticalagriculture:awakened_supremium_essence', [('kubejs:primal_core', 2), ('kubejs:infused_catalyst', 4), ('mysticalagriculture:supremium_essence', 16)], 4, 3000000, 180000, 800)
    process('matrix_expedition', matrix, 'minecraft:nether_star', [('kubejs:ancient_core', 2), ('kubejs:stellar_core', 1), ('kubejs:temporal_core', 1), ('kubejs:stabilized_flux', 4)], 4, 3000000, 180000, 800)
    return result


def build_stations():
    base = ROOT/'quests/convergence-forge'
    target = ROOT/'pack/config/wombatcraft-mbd2'
    models = ROOT/'pack/kubejs/assets/wombatcraft/models/block'
    language = ROOT/'pack/kubejs/assets/wombatcraft/lang/en_us.json'
    labels = json.loads(language.read_text())
    for key, (title, frame, cap) in STATIONS.items():
        controller = (base/'controller.snbt').read_text()
        recipe = (base/'recipe_type.snbt').read_text()
        if key != 'convergence_forge':
            controller = controller.replace('wombatcraft:convergence_forging', recipe_type(key)).replace('wombatcraft:convergence_forge', 'wombatcraft:'+key).replace('Convergence Forge', title).replace('create:brass_block', frame).replace('minecraft:crying_obsidian', cap)
            controller = controller.replace('wombatcraft:block/convergence_forge', 'wombatcraft:block/'+key)
            recipe = recipe.replace('wombatcraft:convergence_forging', recipe_type(key))
            for suffix, text in [('controller', controller), ('recipe_type', recipe)]:
                path = ROOT/'quests/convergence-machines'/key/(suffix+'.snbt')
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text)
            textures = {'arcane_infuser': ('amethyst_block', 'enchanting_table_top'), 'dimensional_stabilizer': ('polished_deepslate', 'respawn_anchor_top'), 'convergence_matrix': ('polished_blackstone_bricks', 'lodestone_top')}
            side, top = textures[key]
            (models/(key+'.json')).write_text(json.dumps({'parent': 'minecraft:block/orientable', 'textures': {'side': 'minecraft:block/'+side, 'front': 'minecraft:block/respawn_anchor_side4', 'top': 'minecraft:block/'+top}}, indent=2)+'\n')
        for text, directory, suffix in [(controller, 'multiblock', 'mb'), (recipe, 'recipe_type', 'rt')]:
            path = target/directory/(key+'.'+suffix)
            path.parent.mkdir(parents=True, exist_ok=True)
            nbtlib.File(nbtlib.parse_nbt(text), gzipped=False).save(path)
        labels['block.wombatcraft.'+key] = title
        labels['item.wombatcraft.'+key] = title
        labels[recipe_type(key).replace(':', '.')] = title
    language.write_text(json.dumps(labels, indent=2)+'\n')


def add_workshop_chapter(book, uid):
    filename = 'wc_convergence_workshop'
    nodes = [
        ('terminal', 'Research Terminal', 'wombatcraft_smp:research_terminal', [], 'Carry discoveries to the terminal and press Record. Knowledge belongs to you and survives replacing the terminal or changing factions. Record your own carried evidence; recipes and materials remain tradeable.'),
        ('prospector', 'Read the stone', 'wombatcraft_smp:resonant_prospector', ['terminal'], 'Use the prospector to locate Resonant Ore in an 8-block cube. It checks loaded terrain only, has 128 scans and a five-second cooldown. Amethyst crafting remains an alternative for established worlds.'),
        ('forge', 'Build the first workshop', 'wombatcraft:convergence_forge', ['terminal'], 'All four processing stations use a six-block wall. Facing the controller, build the bottom row: frame, controller, frame. The top row is cap, frame, cap. For this Forge use brass blocks as frames and crying obsidian as caps. Supply FE and aim a Botania Mana Spreader at the controller. Its six input slots and two output slots accept item pipes.'),
        ('infuser', 'Arcane Infuser', 'wombatcraft:arcane_infuser', ['forge'], 'After your first Aetherium batch, build an Infuser. Use the same wall pattern with manasteel block frames and amethyst block caps. This station makes catalysts, Living Relics, farmland, mana metals, storage crystals and expedition supplies. Both FE and mana are required.'),
        ('catalyst', 'A market for magic and nature', 'kubejs:infused_catalyst', ['infuser'], 'One Arcane Core, one Primal Core and eight source gems produce eight catalysts. Engineers and explorers consume them repeatedly. Set up a player shop or supply a teammate; catalysts are consumed, not permanent upgrades.'),
        ('projector', 'Carry an expedition field', 'wombatcraft_smp:field_projector', ['infuser'], 'Right-click with a Field Charge in your inventory for three minutes of water breathing and night vision. Each use spends one charge. The ten-second cooldown prevents repeated activation. It does not supply space oxygen, flight or combat invulnerability.'),
        ('charge', 'Keep expeditions supplied', 'kubejs:field_charge', ['catalyst'], 'Infuse a catalyst, prismarine crystals and glowstone dust into eight Field Charges. Keep charges in your inventory; a projector with no charges does nothing in survival.'),
        ('stabilizer', 'Dimensional Stabilizer', 'wombatcraft:dimensional_stabilizer', ['infuser'], 'Craft this controller after reaching Astralium. Build its wall with ostrum block frames and crying obsidian caps. It handles renewable Ancient and Temporal Cores, planetary metals and travel equipment. Existing TARDIS travel is welcome but no ship ownership or ship parts are consumed.'),
        ('flux', 'Time becomes a consumable', 'kubejs:stabilized_flux', ['stabilizer'], 'Combine Temporal and Stellar Cores with ender pearls for eight flux. Flux enables accelerated batches and keeps the space and dimension specialists useful after initial exploration.'),
        ('boots', 'A controlled landing', 'wombatcraft_smp:astralium_boots', ['stabilizer'], 'Diamond-level boots halve the fall damage multiplier. They do not prevent all fall damage. Wearing them replaces Aetherium Boots, so you lose the four-piece arcane protection bonus. Choose equipment for the expedition.'),
        ('flight', 'Renewable wings', 'minecraft:elytra', ['flux'], 'Forge four Astralium, one Stellar Core, sixteen phantom membranes and thirty-two feathers in the Stabilizer. End ships are a discovery route rather than a permanent supply limit. This also supplies the Voyager Sigil.'),
        ('overclock', 'Pay for throughput', 'ae2:engineering_processor', ['flux'], 'The Infuser makes an eight-processor batch in 50 ticks with one flux, twice the FE and twice the mana of the 200-tick Forge recipe. The Stabilizer similarly reduces Astralium from 300 to 75 ticks. Yield stays unchanged; faster factories buy more power and flux.'),
        ('metals', 'Keep the colonies supplied', 'ad_astra:ostrum_ingot', ['flux'], 'Stellar Cores and flux turn iron into desh, then desh into ostrum. Calorite has its own costly reconstitution recipe. The first Stellar Cores still require all three planetary metals; automation follows discovery or trade.'),
        ('harvest', 'Expand the farms', 'mysticalagriculture:supremium_farmland', ['catalyst'], 'Primal Cores turn dirt and wheat into four supremium farmland blocks. The Infuser can also make four supremium essence from one Primal Core, eight imperium essence and a catalyst. Crops and intermediate essence remain inputs; this is not free essence duplication.'),
        ('relic', 'Living history', 'kubejs:living_relic', ['infuser'], 'Bring Ancient and Primal Cores to the Infuser. This single component binds nature and adventure for the six-slot final alloy recipe.'),
        ('matrix', 'Build the Convergence Matrix', 'wombatcraft:convergence_matrix', ['stabilizer', 'relic'], 'The final Core is infrastructure: spend it on a Convergence Matrix. Use netherite block frames and beacon caps in the same six-block wall. Both caps are ordinary beacon blocks; no pyramid is required for the machine. Stock FE, mana, specialist cores, catalysts and flux.'),
        ('matrix_industry', 'Industrial continuity', 'mekanism:pellet_polonium', ['matrix'], 'The Matrix makes four polonium pellets from Mechanized Cores, flux and uranium. The first Core still requires the original antimatter-era sigil; this is post-mastery production, not an early nuclear skip.'),
        ('matrix_arcane', 'Arcane continuity', 'botania:gaia_ingot', ['matrix'], 'The Matrix turns Arcane and Ancient Cores, catalysts and terrasteel into Gaia ingots. The first Convergence Core still requires the original magic and boss progression. Repeated infrastructure can now use industrial supply.'),
        ('matrix_nature', 'Nature continuity', 'mysticalagriculture:awakened_supremium_essence', ['matrix'], 'Primal Cores, catalysts and supremium supply awakened essence after the original awakening milestone. Nature remains a recurring supplier to the server economy.'),
        ('matrix_expedition', 'Expedition continuity', 'minecraft:nether_star', ['matrix'], 'Ancient, Stellar and Temporal Cores plus flux produce four Nether Stars. This closes a renewable supply loop at a substantial FE, mana and specialist-material cost. Keep the Matrix fed by several workshops.'),
    ]
    quests=[]
    for index,(key,title,item,parents,description) in enumerate(nodes):
        quests.append(dict(id=uid('workshop/'+key), title=title, icon=item, x=(index%4)*4, y=(index//4)*4,
                           description=[description], dependencies=[uid('workshop/'+p) for p in parents],
                           tasks=[dict(id=uid('workshop/'+key+'/task'), type='item', item=item, count=1, consume_items=False)], rewards=[]))
    book[filename]=dict(id=uid('chapter/'+filename), filename=filename, title='Convergence: Workshops and Expeditions', group='0F095C8E4FA42163', order_index=60, icon='wombatcraft_smp:research_terminal', quests=quests)
