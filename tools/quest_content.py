"""Authored milestones and chapter maps for the Wombatcraft quest book."""

from quest_layout import lay_out_authored_map, mark


# Each entry names the existing item quests that open a playable objective.
MAJOR_MILESTONES = {
    "wc_create": [
        ("Workshop by Water", "water_wheel", "create:fan_processing", ["encased_fan", "belt_connector"],
         "Set a fan behind water and send a batch of gravel through the airflow. Catch the washed output at the end of the belt.",
         "minecraft:gravel", 32),
        ("A Field in Motion", "mechanical_harvester", "create:contraption_actors", ["mechanical_harvester", "portable_storage_interface"],
         "Set a harvester moving with storage on the contraption. Stop the machine, align the interfaces and check that crops reach the stationary chest.",
         "minecraft:wheat_seeds", 24),
        ("The Switching Yard", "mechanical_arm", "create:arm_many_targets", ["mechanical_arm", "depot"],
         "Use a mechanical arm to service several depots. Leave one station open for overflow and keep the output accessible from the aisle.",
         "create:brass_ingot", 8),
        ("The Long Haul", "track_station", "create:long_train", ["track_station", "schedule", "track_signal"],
         "Assemble a long train on a straight siding. Make room for every carriage before testing the signal and scheduling a round trip.",
         "create:track", 32),
    ],
    "wc_applied_energistics": [
        ("A Network With Room to Grow", "fluix_smart_cable", "ae2:main/network2", ["controller", "fluix_smart_cable"],
         "Bring several devices online together and read the channels on a smart cable. Leave spare channels on the trunk for your next workshop.",
         "ae2:fluix_glass_cable", 16),
        ("The Pattern Library", "crafting_monitor", None, ["pattern_provider", "molecular_assembler", "1k_crafting_storage"],
         "Encode a crafting pattern and a processing pattern. Request each from a terminal and check that both jobs finish without manual transfers.",
         "ae2:blank_pattern", 8),
        ("A Room in a Cell", "spatial_io_port", "ae2:main/spatial_explorer", ["spatial_pylon", "spatial_io_port", "spatial_storage_cell_2"],
         "Use a small empty test chamber to learn spatial transfer. Align the pylons and bring enough power to bring the room back.",
         "ae2:fluix_crystal", 16),
    ],
    "wc_ars_nouveau": [
        ("A Shortcut Home", "warp_scroll", "ars_nouveau:create_portal", ["enchanting_apparatus", "source_jar"],
         "Bind a warp scroll to a familiar destination and use it with a portal frame and source. Check the route before trusting it with a long expedition.",
         "ars_nouveau:source_gem", 12),
        ("The Wilden Hunt", "ritual_wilden_summon", "ars_nouveau:wilden_tribute", ["ritual_wilden_summon"],
         "Carry a healing spell and make room to move around the chimera. Its tribute opens the path to the archmage's book.",
         "minecraft:golden_carrot", 12),
        ("The Tower Keeps Working", "spell_turret", None, ["spell_turret", "source_jar", "spell_sensor"],
         "Run a harmless spell from a turret, trigger it from a sensor and confirm that it stops when you cut the signal.",
         "ars_nouveau:source_gem", 16),
    ],
    "wc_botania": [
        ("An Open Gateway", "alfheim_portal", "botania:main/elf_portal_open", ["alfheim_portal", "mana_pool"],
         "Set up the elven gateway with charged mana pools and pylons. Open it and send a small offering through before stocking the exchange.",
         "botania:livingrock", 24),
        ("Gaia Unbound", "gaia_ingot", "botania:challenge/gaia_guardian_hardmode", ["gaia_ingot"],
         "Clear the ritual arena, bring a Gaia Spirit Ingot and keep a supply of food and mana on hand for the stronger guardian.",
         "minecraft:experience_bottle", 16),
    ],
    "wc_mekanism": [
        ("Fivefold Commissioning", "chemical_crystallizer", None,
         ["chemical_dissolution_chamber", "chemical_washer", "chemical_crystallizer"],
         "Run a batch from raw ore to finished ingots. Check the clean and dirty slurry tanks before connecting the line to bulk storage.",
         "mekanism:alloy_infused", 16),
        ("A Reserve for the Grid", "basic_induction_cell", None, ["basic_induction_cell", "basic_induction_provider", "induction_port"],
         "Build an induction matrix with separate input and output ports. Stop generation and confirm that the reserve can carry your running machines.",
         "mekanism:energy_tablet", 4),
    ],
    "wc_occultism": [
        ("A Working Circle", "dictionary_of_spirits", None, ["chalk_white", "sacrificial_bowl", "book_of_binding_bound_foliot"],
         "Draw the circle for your first bound spirit, check the markings against the Dictionary of Spirits and complete the summoning with the right binding.",
         "minecraft:candle", 12),
    ],
}


def add_milestone(chapter, uid, title, icon, note, position, dependencies, task_type, target, reward, count, optional=True):
    key = chapter["filename"] + "/milestone/" + title
    task = {"id": uid(key + "/task"), "type": task_type}
    if task_type == "checkmark":
        task["title"] = "Complete the build"
    elif task_type == "kill":
        task.update(entity=target, value=1)
    else:
        task[task_type] = target
    quest = {
        "id": uid(key), "title": title, "description": [note], "icon": icon,
        "dependencies": list(dict.fromkeys(dependencies)), "tasks": [task],
        "optional": optional,
        "rewards": [
            {"id": uid(key + "/supplies"), "type": "item", "item": reward, "count": count},
            {"id": uid(key + "/xp"), "type": "xp", "xp": 40 if optional else 60},
        ],
    }
    mark(quest, *position)
    chapter["quests"].append(quest)
    return quest["id"]


def by_item(chapter):
    return {task["item"]: q for q in chapter["quests"] for task in q["tasks"] if task["type"] == "item"}


def add_major_milestones(chapters, uid):
    for filename, milestones in MAJOR_MILESTONES.items():
        chapter = chapters[filename]
        items = by_item(chapter)
        ns = next(iter(items)).split(":")[0]
        y = max(q["y"] for q in chapter["quests"]) + 2.6
        for index, (title, icon, advancement, prerequisites, note, reward, count) in enumerate(milestones):
            add_milestone(
                chapter, uid, title, f"{ns}:{icon}", note, ((index - (len(milestones)-1)/2) * 3.1, y, "diamond", 1.42),
                [items[f"{ns}:{item}"]["id"] for item in prerequisites],
                "advancement" if advancement else "checkmark", advancement, reward, count,
            )
    spellbooks = chapters["wc_spellbooks"]
    spell_items = by_item(spellbooks)
    y = max(q["y"] for q in spellbooks["quests"]) + 2.6
    for i, (title, entity, gate, note) in enumerate([
        ("The Last King", "dead_king", "dead_king_phylactery",
         "Follow the Wayward Compass into the catacombs. Save mana for the later phases and keep a clear route around the chamber."),
        ("Fire in the Citadel", "pyromancer", "scroll_forge",
         "Bring fire resistance and a spell that works at range. Clear space around the arena before committing to the fight."),
        ("A Cold Reception", "cryomancer", "inscription_table",
         "Carry healing and a movement spell. Fight on firm ground and leave room to sidestep frost attacks."),
    ]):
        prerequisite = spell_items[f"irons_spellbooks:{gate}"]
        add_milestone(spellbooks, uid, title, prerequisite["icon"], note, ((i-1)*3.25, y, "diamond", 1.45),
                      [prerequisite["id"]], "kill", f"irons_spellbooks:{entity}", "minecraft:golden_carrot", 10)


def add_twilight_chapter(chapters, uid):
    filename = "wc_twilight_forest"
    group = chapters["22_exploration"]["group"]
    chapter = {"filename": filename, "id": uid("chapter/Twilight Forest"), "title": "Twilight Forest",
               "group": group, "order_index": 12, "default_quest_shape": "circle", "quests": []}
    intro = add_milestone(chapter, uid, "Where the Sun Stands Still", "minecraft:oak_sapling",
        "Ring a shallow pool with flowers and throw in a diamond. Bring food, torches and a map before stepping through.",
        (0, -5.5, "octagon", 1.65), [], "dimension", "twilightforest:twilight_forest", "minecraft:torch", 32, False)
    chapter.update(icon="twilightforest:magic_map", autofocus_id=intro)
    nodes = [
        ("naga", "Scales in the Courtyard", "naga_trophy", "Hunt the Naga in its walled courtyard. Keep the serpent in view as it breaks through the stone.", (0, -3.3), ()),
        ("lich", "The Lich's Tower", "lich_trophy", "Climb the tower with room in your pack. Return the Lich's projectiles to break its shields.", (0, -1.1), ("naga",)),
        ("labyrinth", "Beneath the Swamp", "maze_map", "Descend into the labyrinth. Defeat the Minoshroom and eat its Meef Stroganoff to clear the swamp's curse.", (-3.8, 1.2), ("lich",)),
        ("hydra", "The Fire Swamp", "hydra_trophy", "Face the Hydra in the fire swamp. Attack the open mouths and move between volleys.", (-3.8, 3.6), ("labyrinth",)),
        ("knights", "Beneath the Dark Forest", "knight_phantom_trophy", "Bring a trophy to the stronghold pedestal. Search the lower rooms for the Knight Phantoms.", (0, 1.8), ("lich",)),
        ("ur_ghast", "Above the Dark Tower", "ur_ghast_trophy", "Scale the Dark Tower and use its ghast traps to bring the Ur-Ghast into reach.", (0, 4.2), ("knights",)),
        ("yeti", "Under the Snow", "alpha_yeti_trophy", "Find the Yeti's lair in the snowy forest. Keep away from its grab and bring a ranged weapon.", (3.8, 1.2), ("lich",)),
        ("glacier", "The Palace on the Glacier", "snow_queen_trophy", "Follow the towers of the Aurora Palace upward and defeat the Snow Queen above the glacier.", (3.8, 3.6), ("yeti",)),
        ("troll", "Into the Highlands", "magic_beans", "Search the troll caves for magic beans and plant one in Uberous Soil. The beanstalk climbs to the giants.", (0, 7.3), ("hydra", "ur_ghast", "glacier")),
    ]
    quest_ids = {}
    for advancement, title, icon, note, (x, y), parents in nodes:
        quest_ids[advancement] = add_milestone(chapter, uid, title, "twilightforest:"+icon, note,
            (x, y, "hexagon" if advancement in {"hydra", "ur_ghast", "glacier", "troll"} else "diamond", 1.35),
            [quest_ids[parent] for parent in parents] or [intro], "advancement", "twilightforest:progress_"+advancement,
            "minecraft:golden_carrot", 8, False)
    chapters[filename] = chapter


def refine_chapters(chapters, uid):
    starter = chapters["00_start_here"]
    lay_out_authored_map(starter, {
        "A foothold in Wombatcraft": (0, 0, "octagon", 1.75),
        "A place to begin": (-2.15, -1.65, "rsquare", 1.18),
        "The first workshop": (-4.0, -1.65, "rsquare", 1.18),
        "Below the surface": (-5.85, -1.65, "diamond", 1.28),
        "Keep what matters": (-2.2, 1.65, "rsquare", 1.18),
        "A safe return": (2.2, 1.65, "hexagon", 1.35),
        "Recipes at your fingertips": (0, -2.55, "circle", 1.12),
        "Make a home together": (4.1, 1.65, "hexagon", 1.28),
        "The doorway test": (2.2, -1.65, "circle", 1.0),
    })
    start_quests = {q["title"]: q for q in starter["quests"]}
    start_quests["The doorway test"]["title"] = "Mark the Way Home"
    start_quests["The doorway test"]["icon"] = "minecraft:compass"
    start_quests["The doorway test"]["description"] = [
        "Open the FTB Chunks map and mark your camp. Carry a bed and food when you travel beyond familiar ground."
    ]
    start_quests["The doorway test"]["tasks"][0]["title"] = "Mark your camp"
    for title, prior in (("The first workshop", "A place to begin"), ("Below the surface", "The first workshop"),
                         ("Make a home together", "A safe return")):
        start_quests[title]["dependencies"] = [start_quests[prior]["id"]]
    start_quests["A foothold in Wombatcraft"]["description"] = [
        "Find a place to sleep and put down a workbench. Then take whichever path calls to you.",
        "Build a workshop with Create, learn spellcraft in Ars Nouveau, or follow the expeditions beyond the Overworld."
    ]
    start_quests["Make a home together"]["description"] = [
        "Open the FTB Chunks map and claim your home. Invite companions to your team before sharing tools and storage.",
        "When you have goods to spare, use Lightman's Currency to stock a shop and set a price."
    ]

    space = chapters["11_ad_astra"]
    lay_out_authored_map(space, {
        "Ad Astra": (0, -5.0, "octagon", 1.55),
        "Materials for space": (0, -3.2, "rsquare", 1.12),
        "Power the workshop": (-2.0, -1.6, "circle", 1.02),
        "Pressure-rated parts": (0, -1.6, "rsquare", 1.1),
        "Prepare the fuel": (-2.0, 0.15, "circle", 1.02),
        "Breathing room": (2.0, -1.6, "circle", 1.02),
        "Dress for the journey": (2.0, 0.15, "rsquare", 1.1),
        "Assemble a rocket": (0, 0.15, "rsquare", 1.2),
        "First launch": (0, 2.0, "hexagon", 1.45),
        "Lunar industry": (-1.55, 5.35, "circle", 1.0),
        "Beyond the Moon": (-3.2, 5.35, "hexagon", 1.35),
        "Martian materials": (-1.55, 9.3, "circle", 1.0),
        "A wider system": (-3.2, 9.3, "hexagon", 1.35),
        "Extreme materials": (-1.55, 13.3, "circle", 1.0),
        "The farthest journey": (-3.2, 13.3, "hexagon", 1.35),
    })
    parts = {q["title"]: q for q in space["quests"]}
    for title, prior in (("Pressure-rated parts", "Power the workshop"), ("Prepare the fuel", "Pressure-rated parts"),
                         ("Breathing room", "Pressure-rated parts"), ("Assemble a rocket", "Pressure-rated parts")):
        parts[title]["dependencies"] = [parts[prior]["id"]]
    parts["First launch"]["dependencies"] = [parts[t]["id"] for t in ("Assemble a rocket", "Dress for the journey", "Prepare the fuel")]
    for planet, title, launcher, y, note in [
        ("moon", "Footprints on the Moon", "First launch", 4.0,
         "Fill your tanks and take spare fuel. Land on the Moon, then mark the lander before searching for desh."),
        ("mars", "The Red Frontier", "Beyond the Moon", 8.0,
         "Land on Mars with charged equipment and spare oxygen. Bring ostrum back to the workshop."),
        ("venus", "Beneath a Burning Sky", "A wider system", 12.0,
         "Wear protection rated for Venus. Check oxygen and thermal protection before leaving the lander."),
        ("glacio", "A Light in the Ice", "The farthest journey", 16.0,
         "Travel to Glacio with enough fuel for the return. Mark a sheltered landing site before exploring the ice."),
    ]:
        add_milestone(space, uid, title, parts[launcher]["icon"], note, (0, y, "diamond", 1.4),
            [parts[launcher]["id"]], "dimension" if planet == "glacio" else "advancement",
            "ad_astra:"+planet, "minecraft:golden_carrot", 8, False)

    projects = chapters["26_projects"]
    lay_out_authored_map(projects, {
        "A workshop that runs itself": (-3.5, -1.7, "hexagon", 1.45),
        "A home worth returning to": (-3.5, 1.7, "hexagon", 1.45),
        "A shared settlement": (3.5, 1.7, "hexagon", 1.45),
        "Beyond the familiar": (3.5, -1.7, "hexagon", 1.45),
    })
    for quest, icon in zip(projects["quests"], ("create:mechanical_press", "minecraft:lantern",
                                                "minecraft:spyglass", "minecraft:bell"), strict=True):
        quest["icon"] = icon
    projects["icon"] = "minecraft:lantern"
    lay_out_authored_map(chapters["01_tardis"], {
        "Tardis Mod": (0, -2, "octagon", 1.4),
        "The Type 40 field guide": (0, 0, "rsquare", 1.15),
        "Across the threshold": (-2.5, -0.5, "diamond", 1.2),
        "Read the ship's personality": (0, 1.7, "circle", 1.0),
        "Prepare a repair kit": (0, 3.4, "rsquare", 1.08),
        "A sonic of your own": (0, 5.1, "rsquare", 1.13),
        "Maintain the engine": (-2.25, 6.75, "hexagon", 1.2),
        "Choose your exterior": (2.25, 6.75, "hexagon", 1.2),
    })

    convergence = chapters["99_convergence"]
    lay_out_authored_map(convergence, {
        "Convergence Core": (0, 0, "octagon", 1.95),
        "Industrial Sigil": (-3.45, 0, "hexagon", 1.38),
        "Arcane Sigil": (0, -3.15, "hexagon", 1.38),
        "Voyager Sigil": (3.45, 0, "hexagon", 1.38),
        "Prosperity Sigil": (0, 3.15, "hexagon", 1.38),
    })
    for q in convergence["quests"]:
        if q["title"] == "Prosperity Sigil":
            q["description"] = ["Bring the yield of a thriving settlement to the forge. Earn or exchange coin and prepare the rare harvests required for the sigil."]
        if q["title"] == "Convergence Core":
            q["rewards"] = [{"id": uid("convergence/celebration"), "type": "item", "item": "minecraft:firework_rocket", "count": 32}]
    add_major_milestones(chapters, uid)
    add_twilight_chapter(chapters, uid)
    for chapter, art, x, y in [
        ("00_start_here", "frontier", -1.0, -4.65),
        ("wc_create", "workshop", 0, -2.65),
        ("wc_applied_energistics", "network", 0, -2.65),
        ("wc_ars_nouveau", "arcane", 0, -2.65),
        ("wc_twilight_forest", "twilight", 0, -7.9),
        ("11_ad_astra", "space", 0, -7.5),
        ("99_convergence", "convergence", 0, -5.6),
    ]:
        chapters[chapter]["images"] = [{
            "x": x, "y": y, "width": 7.8, "height": 1.3,
            "image": f"wombatcraft:textures/questbook/{art}.png",
        }]
