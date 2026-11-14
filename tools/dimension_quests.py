"""Destination-led expeditions. Existing quest IDs survive chapter moves."""
from quest_layout import mark, lay_out_atlas

GROUP = "3E91144EB73CBA91"

# Exact advancement IDs verified against the installed Forge 1.20.1 jars.
# Each lane branches independently from arrival; rewards never bypass its loot.
EXPEDITIONS = {
    "aether": ("The Aether", "aether:the_aether", "aether:zanite_gemstone", "Build a glowstone gateway and pour water into its frame. The islands hold three dungeon tiers, enchanted minerals and moa companions.", [
        [("A Scholar Among Clouds", "aether:read_lore", "Read the Book of Lore before testing unfamiliar tools."), ("The Altar's Craft", "aether:craft_altar", "Build an altar to work with the sky's minerals."), ("Weightless Wealth", "aether:enchanted_gravitite", "Refine gravitite at the altar; bring the result home for your next equipment project.")],
        [("The Stone Sentinel", "aether:bronze_dungeon", "Defeat the Slider in a bronze dungeon; ordinary weapon swings are not the answer."), ("A Queen's Challenge", "aether:silver_dungeon", "Earn the right to challenge the Valkyrie Queen and defeat her."), ("Fire Above the Clouds", "aether:gold_dungeon", "Challenge the Sun Spirit. Watch its projectiles and use the arena to your advantage.")],
        [("A Nest of Your Own", "aether:obtain_egg", "Find a moa egg and bring it safely back to camp."), ("Wings for the Expedition", "aether:incubate_moa", "Hatch a moa with an incubator. Raise your travelling companion before setting out again.")],
    ]),
    "everbright": ("Everbright", "blue_skies:everbright", "minecraft:blue_ice", "Seek a Gatekeeper and consult the Blue Journal. Carry a Zeal Lighter to open the Everbright portal; local tools matter in this cold realm.", [
        [("A Foothold in the Frost", "blue_skies:everbright/get_wood", "Gather local timber and begin a sheltered workshop."), ("Ventium Expedition", "blue_skies:everbright/get_ventium", "Search below the frozen surface for ventium."), ("Falsite Reserves", "blue_skies:everbright/get_falsite", "Bring falsite back from the mines for local equipment.")],
        [("The Summoner's Tower", "blue_skies:everbright/kill_summoner", "Gather the keys, enter the blinding dungeon and defeat the Summoner."), ("Beneath the Roots", "blue_skies:everbright/enter_nature_dungeon", "Follow the next keys to the nature dungeon."), ("The Starlit Crusher", "blue_skies:everbright/kill_starlit_crusher", "Defeat the Starlit Crusher; prepare the right local tool before entering its chamber.")],
    ]),
    "everdawn": ("Everdawn", "blue_skies:everdawn", "minecraft:red_sand", "Find the Gatekeeper's Everdawn portal and light it with a Zeal Lighter. This warmer sister realm has its own materials, alchemy and dungeon guardians.", [
        [("Timber in the Dawn", "blue_skies:everdawn/get_wood", "Gather local wood for a second expedition workshop."), ("Horizonite Veins", "blue_skies:everdawn/get_horizonite", "Find horizonite and bring a sample back to camp."), ("A Forge of the Dawn", "blue_skies:everdawn/craft_horizonite_forge", "Build the horizonite forge for the materials found here.")],
        [("The Alchemist", "blue_skies:everdawn/kill_alchemist", "Enter the blinding dungeon with its keys and defeat the Alchemist."), ("The Poison Dungeon", "blue_skies:everdawn/enter_poison_dungeon", "Prepare for poison and find the next dungeon."), ("The Arachnarch", "blue_skies:everdawn/kill_arachnarch", "Defeat the Arachnarch and claim the spoils of the dawn.")],
        [("The Alchemy Table", "blue_skies:everdawn/use_alchemy_table", "Put the alchemy table to work with local materials."), ("An Alchemist's Fortune", "blue_skies:everdawn/alchemy_gold", "Complete the gold transmutation recorded in the Blue Journal.")],
    ]),
    "undergarden": ("The Undergarden", "undergarden:undergarden", "undergarden:catalyst", "Craft a Catalyst and awaken a stone-brick portal. Beneath the surface lie strange crops, Stoneborn traders and forgotten metal guarded in the catacombs.", [
        [("Ores Beneath the Roots", "undergarden:undergarden/mine_ore", "Mine a local ore and begin a supply cache."), ("Cloggrum Field Armour", "undergarden:undergarden/cloggrum_armor", "Assemble the cloggrum armour set before the deeper expedition."), ("A Geologist's Collection", "undergarden:undergarden/all_ore_blocks", "Collect the realm's ore blocks for a geological display.")],
        [("The Catacombs", "undergarden:undergarden/catacombs", "Descend into the catacombs with lighting, food and a marked route out."), ("The Forgotten Guardian", "undergarden:undergarden/kill_forgotten_guardian", "Defeat the Forgotten Guardian in its ancient halls."), ("Forgotten Metal", "undergarden:undergarden/forgotten_ingot", "Obtain a forgotten ingot for the tools beyond cloggrum."), ("Tools of a Lost Age", "undergarden:undergarden/forgotten_tools", "Complete the forgotten tool collection.")],
        [("Trade With the Stoneborn", "undergarden:undergarden/stoneborn_trade", "Complete a trade with a Stoneborn."), ("A Garden Underground", "undergarden:undergarden/plant_gloomgourd", "Plant a gloomgourd and establish a renewable food supply."), ("A Harvest for Home", "undergarden:undergarden/stack_of_gloomgourds", "Bring together a full stack of gloomgourds.")],
    ]),
    "otherside": ("The Otherside", "deeperdarker:otherside", "minecraft:echo_shard", "Seek an Ancient City and the Warden's Heart. Open its great frame and descend with spare light, protection and room for echo-rich discoveries.", [
        [("The Ancient Temple", "deeperdarker:main/find_ancient_temple", "Find the Ancient Temple beyond the portal."), ("A Reinforced Echo", "deeperdarker:main/obtain_reinforced_echo_shard", "Recover a reinforced echo shard for deeper crafting."), ("The Sonorous Staff", "deeperdarker:main/obtain_sonorous_staff", "Create the sonorous staff from your expedition's spoils.")],
        [("Survey of the Otherside", "deeperdarker:main/explore_otherside", "Visit the Otherside's biomes; map a safe route between them."), ("Predators of the Sculk", "deeperdarker:main/kill_all_sculk_mobs", "Defeat every sculk creature required by the realm's hunting record."), ("Armour of the Deep", "deeperdarker:main/warden_armor", "Assemble the Warden armour set.")],
    ]),
    "bumblezone": ("The Bumblezone", "the_bumblezone:the_bumblezone", "minecraft:honeycomb", "Throw an ender pearl into a bee nest or hive. The honeycomb world holds royal trades, ancient structures and trials; treat its inhabitants carefully.", [
        [("A Compass Through Honey", "the_bumblezone:tools/craft_honey_compass", "Craft a Honey Compass to guide the expedition."), ("An Audience With Royalty", "the_bumblezone:structures/enter_throne_pillar", "Find a Throne Pillar and approach the Bee Queen."), ("The Queen's First Request", "the_bumblezone:the_queens_desire/the_beginning", "Begin the Queen's Desire trading journey."), ("The Queen's Long Commission", "the_bumblezone:the_queens_desire/journeys_end", "Complete the Queen's Desire chain. Let your farms and workshop supply her requests.")],
        [("The Cell Maze", "the_bumblezone:structures/enter_cell_maze", "Find a Cell Maze and explore its passages."), ("The Sempiternal Sanctum", "the_bumblezone:structures/enter_sempiternal_sanctum", "Seek a Sempiternal Sanctum and investigate its trial."), ("A Complete Honey Atlas", "the_bumblezone:structures/enter_all_structures", "Discover every structure in the Bumblezone's exploration record.")],
        [("A Shield of Crystal", "the_bumblezone:honey_crystal_shield/obtain_honey_crystal_shield", "Make a Honey Crystal Shield for the next expedition."), ("A Stinger's Reach", "the_bumblezone:stinger_spear/obtain_stinger_spear", "Obtain a Stinger Spear and learn its reach."), ("A Returning Traveller", "the_bumblezone:teleportation/teleport_out_of_bumblezone", "Return from the hive world with your discoveries.")],
    ]),
}


def add_dimensions(book, uid):
    def chapter(key, title, icon, order):
        c = dict(filename=key, id=uid('chapter/'+key), title=title, icon=icon,
                 group=GROUP, order_index=order, default_quest_shape='circle', quests=[])
        book[key] = c
        return c

    def quest(c, title, note, kind, target, x, y, deps=(), icon=None, reward=True):
        key = c['filename']+'/'+title
        task = dict(id=uid(key+'/task'), type=kind)
        task['title' if kind=='checkmark' else kind] = 'Record the expedition' if kind=='checkmark' else target
        q = dict(id=uid(key), title=title, description=[note], icon=icon or c.get('icon','minecraft:compass'),
                 tasks=[task], dependencies=list(deps), optional=True)
        mark(q,x,y,'diamond' if kind=='dimension' else 'hexagon' if kind=='advancement' else 'rsquare',1.2)
        if reward:
            q['rewards']=[dict(id=uid(key+'/xp'),type='xp',xp=75),
                          dict(id=uid(key+'/food'),type='item',item='minecraft:golden_carrot',count=4)]
        c['quests'].append(q)
        return q['id']

    atlas=chapter('wc_dimensions_atlas','Atlas of Dimensions','minecraft:compass',0)
    destinations=[]
    for index,(key,(title,dimension,icon,entry,lanes)) in enumerate(EXPEDITIONS.items()):
        c=chapter('wc_dimension_'+key,title,icon,index+2)
        arrival=quest(c,'Into '+title,entry,'dimension',dimension,0,0)
        c['autofocus_id']=arrival
        for lane,steps in enumerate(lanes):
            prior=arrival
            for step,(name,adv,note) in enumerate(steps):
                prior=quest(c,name,note,'advancement',adv,(lane-(len(lanes)-1)/2)*4,3+step*2.2,[prior])
        destinations.append((title,dimension,c['filename'],entry,icon))

    # Extract whole existing branches, including their root, without changing IDs.
    routes={'aether':'aether','blue_skies':'everbright','deeperdarker':'otherside',
            'undergarden':'undergarden','the_bumblezone':'bumblezone'}
    source=book['22_exploration']
    for ns,key in routes.items():
        selected={q['id'] for q in source['quests'] if any(str(t.get(t['type'],'')).startswith(ns+':') for t in q['tasks'])}
        for q in source['quests']:
            if q['id'] in selected:
                selected.update(q.get('dependencies',[]))
        moved=[q for q in source['quests'] if q['id'] in selected]
        source['quests']=[q for q in source['quests'] if q['id'] not in selected]
        for q in moved:
            dest=book['wc_dimension_'+('everdawn' if ns=='blue_skies' and 'Everdawn' in q['title'] else key)]
            q['optional']=True
            q['subtitle']='Field survey'
            if q['tasks'][0]['type']=='checkmark':
                q['description']=[EXPEDITIONS[key][3]]
            mark(q,8,3+sum(1 for v in dest['quests'] if v['x']==8)*2.2,'circle',1)
            dest['quests'].append(q)

    for key in ['11_ad_astra','01_tardis','28_pocket_worlds','wc_twilight_forest']:
        book[key]['group']=GROUP
    book['11_ad_astra']['order_index']=12
    book['wc_twilight_forest']['order_index']=1
    book['01_tardis']['order_index']=19
    book['28_pocket_worlds']['order_index']=18
    destinations.append(('Twilight Forest','twilightforest:twilight_forest','wc_twilight_forest',
                         'Follow the courtyard, tower, swamp and glacier expeditions in Twilight Forest. Defeating each guardian opens the next reaches of the forest.','twilightforest:magic_map'))

    worlds=chapter('wc_dimension_stargates','Through the Stargates','minecraft:ender_eye',13)
    intro=quest(worlds,'The Address Book','Record discovered gate addresses and test the return connection before moving equipment. A powered gate network can link your distant outposts.','advancement','sgjourney:adventure/use_stargate',0,0)
    for i,(key,note) in enumerate([
        ('abydos','Survey the desert and record its gate address.'),('chulak','Establish a safe return point on Chulak.'),
        ('cavum_tenebrae','Bring light and survey the dark terrain.'),('rima','Scout a landing area before shipping machinery.'),
        ('tollan','Survey Tollan and add it to your gate network.'),('lantea','Prepare for an ocean expedition before leaving the gate.'),
        ('athos','Mark a safe campsite on Athos.'),('unitas','Scout Unitas and record a route back.'),
        ('destiny','Prepare for a distant journey and confirm a return route before departure.'),
    ]):
        title=key.replace('_',' ').title()
        visit=quest(worlds,title,note,'dimension','sgjourney:'+key,(i%3-1)*5,3+(i//3)*5,[intro])
        quest(worlds,title+' Field Station','Survey the surroundings, mark the return gate and establish a stocked shelter. Complete this field record after testing a round trip.','checkmark',None,(i%3-1)*5,5+(i//3)*5,[visit],reward=False)
        destinations.append((title,'sgjourney:'+key,worlds['filename'],note,'minecraft:ender_eye'))

    space=book['11_ad_astra']
    for i,(key,resource) in enumerate([('moon','desh'),('mars','ostrum'),('venus','calorite'),('mercury',None),('glacio',None)]):
        title=key.title()
        destinations.append((title,'ad_astra:'+key,space['filename'],
            'Follow the rocket tiers in Ad Astra. Pack oxygen, fuel and suitable protection. '+('Bring '+resource+' home to advance the next rocket tier.' if resource else 'Survey the terrain and establish a supplied expedition camp.'),'minecraft:firework_rocket'))
    quest(space,'Mercury Survey','Land on Mercury with suitable protection, oxygen and fuel for the return journey.','dimension','ad_astra:mercury',4,12)
    orbits=chapter('wc_dimension_orbits','Orbital Stations','minecraft:glass',14)
    for i,key in enumerate(['earth','moon','mars','mercury','venus','glacio']):
        title=key.title()+' Orbit'
        visit=quest(orbits,title,'Reach this orbit through Ad Astra navigation. Carry oxygen and a return plan; orbit is station space, not a second planetary surface.','dimension','ad_astra:'+key+'_orbit',(i%3-1)*5,(i//3)*5)
        quest(orbits,'Station: '+key.title(),'Build a sealed, oxygenated station with power and emergency supplies. Check its life support and make a successful return trip before recording the commission.','checkmark',None,(i%3-1)*5,2+(i//3)*5,[visit],reward=False)
        destinations.append((title,'ad_astra:'+key+'_orbit',orbits['filename'],'Optional orbital station space. Open Orbital Stations for arrival and life-support projects.','minecraft:glass'))

    vanilla=chapter('wc_dimension_vanilla','Nether and End Expeditions','minecraft:ender_eye',10)
    for lane,(title,dim,entry,steps) in enumerate([
        ('Nether','minecraft:the_nether','Build an obsidian portal. Bring fire protection, spare blocks and a marked path home. Incendium adds further ruins to explore.',[
            ('The Fortress','minecraft:nether/find_fortress','Find a Nether fortress and secure a route through its corridors.'),
            ('Blaze Supply','minecraft:nether/obtain_blaze_rod','Recover a blaze rod for brewing and the journey to the End.'),
            ('Ancient Debris','minecraft:nether/obtain_ancient_debris','Recover ancient debris for netherite equipment.'),
            ('The Wither','minecraft:nether/summon_wither','Prepare an arena away from your home before summoning the Wither.'),
            ('Beacon Commission','minecraft:nether/create_beacon','Build a beacon from the Nether Star and commission it at your base.')]),
        ('End','minecraft:the_end','Find a stronghold and open its gateway. Carry spare blocks and a return plan before facing the dragon.',[
            ('The Dragon','minecraft:end/kill_dragon','Defeat the Ender Dragon and open the outer-island route.'),
            ('Beyond the Gateway','minecraft:end/enter_end_gateway','Travel through an End gateway to the outer islands.'),
            ('An End City','minecraft:end/find_end_city','Find an End city among Nullscape terrain.'),
            ('Wings for the Return','minecraft:end/elytra','Recover an elytra from an End ship.'),
            ('The Dragon Reborn','minecraft:end/respawn_dragon','Prepare End crystals and summon the dragon again.')]),
    ]):
        prior=quest(vanilla,'Into the '+title,entry,'dimension',dim,lane*5,0)
        for j,(name,adv,note) in enumerate(steps):
            prior=quest(vanilla,name,note,'advancement',adv,lane*5,3+j*2.2,[prior])
        destinations.append((title,dim,vanilla['filename'],entry,'minecraft:obsidian' if lane==0 else 'minecraft:ender_eye'))

    pockets=book['28_pocket_worlds']
    y=max(q['y'] for q in pockets['quests'])+3
    for i,(key,title,note) in enumerate([
        ('dungeon_pockets','Dungeon Pockets','Explore a dimensional dungeon through a rift. Watch each room before taking another door; the wrong turn can lead to Limbo.'),
        ('personal_pockets','Personal Pockets','Create a personal pocket through Dimensional Doors and establish a quiet workshop.'),
        ('public_pockets','Shared Pockets','Create a shared pocket and test its doorway from both sides before storing valuables.'),
        ('limbo','Limbo','A dangerous place beyond the doors. If you arrive here, concentrate on finding a way home; this journey is optional.'),
    ]):
        quest(pockets,title,note,'dimension','dimdoors:'+key,(i%2)*5,y+(i//2)*3)
        destinations.append((title,'dimdoors:'+key,pockets['filename'],note,'minecraft:iron_door'))

    special=chapter('wc_dimension_sanctums','Sanctums and Pocket Realms','minecraft:ender_chest',17)
    for i,(title,dimension,note) in enumerate([
        ('Endless Realm','bloodmagic:dungeon','Follow the Edge of the Hidden Realm ritual in Sanguine Scientiem. Explore the dungeon for materials that advance Blood Magic.'),
        ('Pocket Dimension','irons_spellbooks:pocket_dimension','Follow the Pocket Dimension spell in the spellbook. Establish a retreat and test the return before leaving equipment inside.'),
        ('Reality Marble','mahoutsukai:reality_marble','Study Reality Marble in the Mahou Tsukai guide. This is a magical ability space, not a conventional expedition world.'),
    ]):
        visit=quest(special,title,note,'dimension',dimension,(i-1)*5,0)
        quest(special,title+' Return Journey','Prepare supplies, use the realm through its own magic and return safely. Record the journey after bringing your discoveries home.','checkmark',None,(i-1)*5,3,[visit],reward=False)
        destinations.append((title,dimension,special['filename'],note,'minecraft:ender_chest'))
    # These are catalogue entries, not impossible arrival requirements. Dynamic
    # interiors have per-world identifiers; internal worlds may be inaccessible.
    for title,chapter_name,note,icon in [
        ('TARDIS Interiors','01_tardis','Each vessel has its own interior. Follow the TARDIS branch to enter, maintain and pilot yours.','minecraft:clock'),
        ('Spatial Storage','wc_applied_energistics','Spatial cells hold isolated spaces. Follow A Room in a Cell in Applied Energistics; test a small powered chamber before moving valuable machinery.','ae2:spatial_storage_cell_2'),
        ('The Past','wc_dimension_sanctums','A realm recorded by The Graveyard. It is not treated as an ordinary portal destination in this expedition atlas; no visit is required.','minecraft:written_book'),
    ]:
        destinations.append((title,'graveyard:past' if title=='The Past' else None,chapter_name,note,icon))
    destinations.insert(0,('Overworld','minecraft:overworld','22_exploration','Your home and departure point. Follow Exploration for Alex\'s Caves, Cataclysm ruins, settlements and underground expeditions.','minecraft:grass_block'))
    # Relocate the remaining destination-specific survey branches as well.
    for ns,target in [('sgjourney','wc_dimension_stargates'), ('twilightforest','wc_twilight_forest'),
                      ('incendium','wc_dimension_vanilla'), ('nullscape','wc_dimension_vanilla'),
                      ('betterfortresses','wc_dimension_vanilla'), ('betterend','wc_dimension_vanilla')]:
        selected={q['id'] for q in source['quests'] if any(str(t.get(t['type'],'')).startswith(ns+':') for t in q['tasks'])}
        changed=True
        while changed:
            before=len(selected)
            for q in source['quests']:
                if q['id'] in selected:
                    selected.update(q.get('dependencies',[]))
            changed=len(selected)!=before
        dest=book[target]
        row=max(q['y'] for q in dest['quests'])+3
        for q in [v for v in source['quests'] if v['id'] in selected]:
            mark(q,0,row,'circle',1)
            q['optional']=True
            row+=2
            dest['quests'].append(q)
        source['quests']=[q for q in source['quests'] if q['id'] not in selected]
    # Rebuild atlas rows from dependency roots, not its previously laid-out Y
    # coordinates (several separate routes share the same displayed row).
    remaining={q['id']:q for q in source['quests']}
    def root(q):
        parents=[d for d in q.get('dependencies',[]) if d in remaining]
        return root(remaining[parents[0]]) if parents else q['id']
    roots=list(dict.fromkeys(root(q) for q in source['quests']))
    for q in source['quests']:
        mark(q,0 if q['id']==root(q) else 1+source['quests'].index(q),roots.index(root(q)))
    lay_out_atlas(source)
    for i,(title,dimension,target,note,icon) in enumerate(destinations):
        qid=quest(atlas,title,note+'\n\nQuest route: '+book[target]['title']+'.',
                  'checkmark',None,(i%5-2)*3.5,(i//5)*2.5,icon=icon,reward=False)
        # Reading the atlas is optional; expeditions detect real arrivals separately.
        q=atlas['quests'][-1]
        q['tasks'][0]['title']='Read the field guide'
        q['subtitle']='Field guide'
    return [dict(title=t,dimension=d,chapter=c,description=n) for t,d,c,n,icon in destinations]
