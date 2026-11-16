"""Original small-tool routes. Item goals use the pinned jars' registry names."""
def chapters():
    result=[]
    def chapter(key,title,icon,entries,group='starter'):
        nodes=[]
        for i,(item,name,text,reward) in enumerate(entries):
            # Three parallel trails return to a shared entrance. Room for readable connectors.
            lane=(i-1)%3 if i else 1;stage=(i-1)//3+1 if i else 0
            parent=[] if i==0 else [str(i-3) if i>3 else '0']
            nodes.append(dict(key=str(i),title=name,icon=item or icon,position=[stage*3,(lane-1)*4],parents=parent,
                description=[text],tasks=[['item',item,1]] if item else [['checkmark',name,1]],
                rewards=reward,shape='hexagon' if i==0 else 'circle'))
        result.append(dict(key=key,title=title,icon=icon,group=group,quests=nodes))
    chapter('pocket_workshop','The Pocket Workshop','crafting_on_a_stick:crafting_table',[
      ('crafting_on_a_stick:crafting_table','A workshop on the road','Combine a crafting table and a stick. Right-click the tool to craft without placing a workstation.',[['minecraft:oak_log',8]]),
      (None,'Keep a hand free','Equip a workstation in its Curios slot. Open it with Ctrl+V, or choose your own shortcut in Controls. Use the item directly if that shortcut is already assigned.',[['minecraft:leather',3]]),
      ('crafting_on_a_stick:stonecutter','Cut to measure','Carry a stonecutter for stairs and slabs. Check its recipes before cutting a whole stack; some conversions save materials.',[['minecraft:stone',32]]),
      ('crafting_on_a_stick:anvil','Repairs in the field','A portable anvil still uses the normal repair and experience rules. Bring spare materials before a long expedition.',[['minecraft:iron_ingot',4]]),
      ('crafting_on_a_stick:grindstone','A clean edge','Use a grindstone to remove unwanted enchantments. Check the output carefully: useful enchantments are removed too.',[['minecraft:experience_bottle',4]]),
      ('crafting_on_a_stick:smithing_table','A travelling smith','Smithing follows the normal template and ingredient requirements. The handle makes the station portable, not the upgrade free.',[['minecraft:coal',8]]),
      ('crafting_on_a_stick:loom','Your company colours','Weave a banner for your settlement. A loom on a stick lets you work while travelling between market stalls and home.',[['minecraft:white_wool',8]]),
      ('crafting_on_a_stick:cartography_table','Chart the frontier','Expand or copy a vanilla map at your portable cartography table. Keep a spare chart of important routes.',[['minecraft:paper',16]])])
    chapter('tool_belt','A Place for Every Tool','toolbelt:belt',[
      ('toolbelt:belt','Wear your toolkit','A Tool Belt keeps tools together without filling your hotbar. Start with the tools you reach for most often.',[['minecraft:leather',4]]),
      (None,'Draw the right tool','Use Alt+R to open the tool wheel. Add a pickaxe and a wrench, then practise switching between them. Change the shortcut if you prefer.',[['minecraft:iron_ingot',3]]),
      ('toolbelt:pouch','Room to grow','Craft a Belt Pouch. Combine it with the belt in an anvil to add capacity; follow the next upgrade cost shown by the anvil.',[['minecraft:string',8]]),
      (None,'The builder’s belt','Put a building tool, measuring aid or another frequently used non-stackable item in the belt. Leave fragile or irreplaceable tools at home when heading into combat.',[['minecraft:scaffolding',16]]),
      (None,'One belt, one loadout','Arrange a complete set of tools for a project. The belt and its contents are part of your carried equipment: a lootable grave is not a private vault.',[['minecraft:cooked_beef',8]])])
    chapter('elevators','Between Floors','elevatorid:elevator_white',[
      ('elevatorid:elevator_white','The first landing','Craft two elevators of the same colour. Place them directly above one another with clear landing space.',[['minecraft:white_wool',8]]),
      (None,'Up and down','Stand on an elevator and jump to ascend; sneak to descend. Matching floors must be within 64 blocks. The destination must already be loaded.',[['minecraft:ladder',16]]),
      ('elevatorid:elevator_blue','Colour your routes','Use matching colours to separate different elevator routes in the same shaft.',[['minecraft:blue_dye',8]]),
      (None,'A safe arrival','Keep feet and head space clear above each landing. Check both directions before inviting visitors to use a shaft.',[['minecraft:lantern',4]]),
      (None,'Open to invited guests','Elevators follow interaction permissions at both ends. They cannot be used in the arena or during the 90-second PvP lockout.',[['minecraft:oak_sign',6]])])
    chapter('torchmaster','A Quiet Homestead','torchmaster:megatorch',[
      ('torchmaster:megatorch','Keep the night at bay','A Mega Torch suppresses natural hostile spawning within 32 blocks in each direction. It does not form a wall: wandering monsters can still approach.',[['minecraft:torch',32]]),
      (None,'Respect your neighbours','Keep the torch’s protected radius away from claims you cannot edit. Placement is refused when its range overlaps such land.',[['minecraft:oak_fence',16]]),
      (None,'Keep the farm running','Natural spawning and spawner-driven farms are different. Mega Torches are configured to leave normal spawner operation alone.',[['minecraft:iron_bars',8]]),
      ('torchmaster:dreadlamp','A different kind of quiet','Dread Lamps suppress natural passive spawning. Keep them away from areas where you want animals to appear.',[['minecraft:glowstone_dust',8]]),
      (None,'Light with intention','Use ordinary lamps for illumination. Feral Flare Lanterns are unavailable here because their automatic invisible lights can reach across property boundaries.',[['minecraft:lantern',4]])])
    chapter('simply_light','Light for the Workshop','simplylight:lightbulb',[
      ('simplylight:lightbulb','A brighter bench','Fit a light bulb above a crafting bench or machine. These lights need no fuel supply.',[['minecraft:glass',16]]),
      ('simplylight:wall_lamp','Fixtures and fittings','Mount fixtures on walls, floors or ceilings. Keep walkways clear and work surfaces well lit.',[['minecraft:iron_ingot',4]]),
      ('simplylight:illuminant_panel','Recessed lighting','Panels and slabs make neat inset lighting. Place a sample first to settle on the orientation.',[['minecraft:smooth_stone',16]]),
      ('simplylight:edge_light','Follow the corridor','Edge lights shape themselves around neighbouring blocks when placed. Temporary guide blocks help form a clean route.',[['minecraft:glass',16]]),
      ('simplylight:illuminant_slab','A low-profile glow','Fit luminous slabs into a floor or ceiling. Pair them with ordinary building materials to mark a safe route.',[['minecraft:stone_slab',16]]),
      ('simplylight:illuminant_block','Switchable lighting','Try a redstone-controlled light block. The inverted version turns off when powered.',[['minecraft:redstone',8]]),
      ('simplylight:rodlamp','Finishing touches','Use rods to mark entrances, workstations and stairwells. A consistent lighting scheme makes a busy factory easier to navigate.',[['minecraft:quartz',8]])])
    chapter('ender_storage','Shared Frequencies','enderstorage:ender_chest',[
      ('enderstorage:ender_chest','A distant storehouse','Matching colour frequencies share an inventory. Public frequencies are shared with other players, so do not store valuables there by accident.',[['minecraft:white_dye',8]]),
      (None,'Set your own frequency','Change the three colour bands on a chest with dyes. Set a second chest to the same pattern and move a small test stack through it.',[['minecraft:red_dye',8],['minecraft:blue_dye',8]]),
      (None,'Lock before loading','Use a diamond on a chest’s latch to make its frequency private to you. Confirm the ownership information before adding valuable cargo.',[['minecraft:obsidian',4]]),
      ('enderstorage:ender_pouch','Supplies on the road','Link an Ender Pouch to a chest by using it on that chest. Keep food, torches and building supplies close while travelling.',[['minecraft:cooked_beef',8]]),
      ('enderstorage:ender_tank','A liquid connection','Matching Ender Tanks share fluid storage. Use separate frequencies for different fluids and check ownership before connecting pipes.',[['minecraft:bucket',2]]),
      (None,'A factory handoff','Connect a deliberately shared frequency to a controlled input chest. Agree on its purpose with your teammates; public channels are not protected by the chest’s surrounding claim.',[['minecraft:hopper',1]]),
      (None,'Travel light, fight fairly','Ender Pouches cannot be opened in the arena or for 90 seconds after PvP damage. Remote storage must not become a way to hide combat loot.',[['minecraft:golden_carrot',4]])], 'technology')
    chapter('inventory_essentials','An Orderly Inventory','minecraft:chest',[
      (None,'Know your pockets','Inventory Essentials adds transfer shortcuts. Check its entries in Controls before changing a carefully arranged inventory.',[['minecraft:chest',2]]),
      (None,'One item at a time','Use the configured single-transfer shortcut to move one item rather than a whole stack. Practise with inexpensive materials.',[['minecraft:coal',8]]),
      (None,'Clear the cobblestone','Use the all-of-type transfer shortcut to move matching stacks. Confirm which inventory is receiving the items.',[['minecraft:barrel',2]]),
      (None,'Sweep the slots','Try transfer-on-hover with a small chest. Avoid assigning the same gesture to multiple inventory mods.',[['minecraft:bread',8]]),
      (None,'Sort on your terms','Inventory Essentials sorting starts unbound to avoid competing with the existing sorting controls. Assign it in Controls only if you want to use it.',[['minecraft:item_frame',4]])])
    chapter('pocket_storage','Bulk Supplies','pocketstorage:psu_1',[
      ('pocketstorage:psu_1','A pocket quarry','Pocket Storage holds large quantities of a limited number of item types. Start with expendable building blocks.',[['minecraft:cobblestone',32]]),
      (None,'Choose what to collect','Set the unit up with the materials you want it to absorb. Check its pickup and overflow behaviour before carrying valuable drops.',[['minecraft:coal',8]]),
      ('pocketstorage:psu_2','A longer expedition','Upgrade to Tier 2 for more capacity. Check the recipe output and your stored contents before completing an upgrade.',[['minecraft:iron_ingot',4]]),
      (None,'Overflow is not a backup','Pocket Storage can void excess matching items. Keep scarce materials out of automatic overflow paths and check the unit before a long mining session.',[['minecraft:chest',2]]),
      ('pocketstorage:psu_3','Supply the project','Tier 3 is a useful companion for a large building job. Keep distinct materials organised so you can see what still needs collecting.',[['minecraft:glass',32]]),
      ('pocketstorage:psu_4','The travelling warehouse','Tier 4 completes the storage line. Large capacity does not protect the contents from loss or other players looting your grave.',[['minecraft:golden_carrot',8]])])
    chapter('charging_gadgets','Power Before the Grid','charginggadgets:charging_station',[
      ('charginggadgets:charging_station','A simple charging bench','The Charging Station burns ordinary furnace fuel to charge Forge Energy equipment. Start with a little fuel and one tool.',[['minecraft:coal',8]]),
      (None,'Watch the meter','Insert a compatible tool and confirm its energy increases. Keep enough charge for the work ahead.',[['minecraft:copper_ingot',8]]),
      (None,'Fuel with a purpose','The station is a convenient early charger. Gather fuel sustainably rather than assuming powered tools run for free.',[['minecraft:charcoal',8]]),
      (None,'Ready for the road','Charge a gadget before departing, then pack food and spare conventional tools. A depleted battery should not strand you.',[['minecraft:cooked_beef',8]]),
      (None,'Connect to industry','As your workshop grows, compare this fuel-powered charger with the charging options in Mekanism, Thermal and Powah.',[['minecraft:redstone',8]])], 'technology')
    chapter('mining_gadgets','The Surveyor’s Laser','mininggadgets:mininggadget_simple',[
      ('mininggadgets:mininggadget_simple','A powered pick','Craft a Mining Gadget and charge it in an FE-compatible charger. Mine only where you have permission.',[['minecraft:coal',16]]),
      ('mininggadgets:modificationtable','The upgrade bench','A Modification Table fits upgrade modules to your gadget. Each module changes its abilities or operating costs.',[['minecraft:redstone',8]]),
      ('mininggadgets:upgrade_empty','A blank canvas','Blank Upgrade Modules are the basis for specialised upgrades. Plan a loadout rather than installing everything immediately.',[['minecraft:iron_ingot',4]]),
      ('mininggadgets:upgrade_battery_1','A longer shift','A battery upgrade raises capacity. It still needs charging before the extra capacity is useful.',[['minecraft:copper_ingot',8]]),
      ('mininggadgets:upgrade_size_1','Three by three','The 3x3 module opens wider tunnels. Larger 5x5 and 7x7 upgrades are unavailable to keep mining bursts modest on the shared server.',[['minecraft:torch',32]]),
      ('mininggadgets:upgrade_efficiency_1','A sharper beam','Efficiency speeds up mining while increasing energy use. Watch battery consumption during sustained work.',[['minecraft:coal',16]]),
      ('mininggadgets:upgrade_magnet','Gather the spoils','The magnet upgrade gathers mined drops. Leave room in your inventory for valuable finds.',[['minecraft:leather',4]]),
      ('mininggadgets:upgrade_void_junk','Keep only what matters','Set the junk filter carefully. Test it on common stone before including materials you cannot easily replace.',[['minecraft:barrel',2]]),
      ('mininggadgets:upgrade_light_placer','Leave a lit passage','The light-placement upgrade illuminates your route where you have building permission. It cannot place lights inside protected land or the arena.',[['minecraft:glowstone_dust',8]]),
      ('mininggadgets:upgrade_freezing','Tame the hazards','The freezing upgrade changes nearby liquid blocks only where you can build. Treat unknown liquids cautiously and keep an escape route.',[['minecraft:bucket',1]]),
      ('mininggadgets:upgrade_fortune_1','A richer seam','Fortune improves suitable drops. Compare its energy cost with the value of the material you are mining.',[['minecraft:lapis_lazuli',16]]),
      ('mininggadgets:upgrade_silk','Bring it home whole','Silk Touch preserves suitable blocks. Choose it or Fortune according to your task and verify the installed upgrade settings.',[['minecraft:experience_bottle',8]])], 'technology')
    chapter('graves','What Remains','gravestone:gravestone',[
      (None,'A marker on the trail','Outside the arena, death leaves a grave when a safe, permitted space is available. Graves are lootable by anyone who has permission to break the block at that location.',[['minecraft:bread',8]]),
      (None,'Read the record','An obituary records the death location when a grave is attempted. Read the dimension as well as the coordinates before travelling back. You do not need to die to complete this guide.',[['minecraft:compass',1]]),
      (None,'Recover carefully','Break the grave to recover its contents. Make room in your inventory first. Sneaking over a grave does not retrieve it, so claim permissions cannot be bypassed that way.',[['minecraft:chest',2]]),
      (None,'Land still has an owner','A grave cannot overwrite protected terrain. If placement is blocked by another claim, the world border or a lack of space, items remain ordinary drops. Ask the landowner for access when necessary.',[['minecraft:torch',16]]),
      (None,'The arena keeps its stakes','Arena deaths leave ordinary loose drops rather than graves. Whoever collects those drops keeps them. Equipment stored in carried belts and storage items is still at risk.',[['minecraft:cooked_beef',8]])])
    return result
