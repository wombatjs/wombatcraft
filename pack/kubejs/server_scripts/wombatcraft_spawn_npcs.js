const WCTraderCache = Java.loadClass('io.github.lightman314.lightmanscurrency.common.data.types.TraderDataCache');
const WCMarketGuard = Java.loadClass('net.wombatcraft.smp.UtilityGuard');
const WCSpawnResidents = {
  guide: {name: 'Mara | Welcome Guide', profession: 'librarian', icon: 'minecraft:compass', entries: [
    ['minecraft:book', 'Your first steps', 'Open the quest book, gather supplies, then choose a branch. Technology, magic and exploration all meet in the Convergence.'],
    ['minecraft:oak_door', 'Make a home', 'Use /sethome outside spawn to mark your base, then /home to return. Use /spawn to visit the town.'],
    ['minecraft:shield', 'Protect your settlement', 'Open the FTB Chunks map with M to claim your base. Spawn belongs to the town; build your home beyond its boundary.']
  ]},
  market: {name: 'Bram | Market Steward', profession: 'toolsmith', icon: 'minecraft:gold_ingot', entries: [
    ['minecraft:gold_ingot', 'The town economy', 'Trade with other players using Lightman\'s Currency coins. Check a trader\'s price and stock before confirming a purchase.'],
    ['minecraft:chest', 'Open a shop', 'Craft a Lightman\'s Currency trader, stock it, and set your prices. Your shop can trade while you are away.'],
    ['minecraft:paper', 'Find a stall', 'Visit the market stalls around spawn. Ask a town administrator about a stall location before placing a trader in the protected town.']
  ]},
  travel: {name: 'Pip | Wayfinder', profession: 'cartographer', icon: 'minecraft:ender_pearl', entries: [
    ['minecraft:ender_pearl', 'Visit a friend', 'Send /tpa <player>. They can use /tpaccept or /tpdeny. Teleport requests always need the other player\'s consent.'],
    ['minecraft:compass', 'Mark your discoveries', 'JourneyMap opens with F6. Mark a waypoint for your home and important discoveries. FTB Chunks remains on M for claims.'],
    ['minecraft:ender_eye', 'Beyond the Overworld', 'The Dimensions quest category charts the destinations and their challenges. Prepare equipment, food and a way home before leaving.']
  ]},
  quests: {name: 'Iris | Quest Archivist', profession: 'librarian', icon: 'minecraft:enchanted_book', entries: [
    ['minecraft:book', 'Choose your path', 'Begin with the field equipment chapter, then follow the branches that interest you. Claim completed rewards in the quest book.'],
    ['minecraft:nether_star', 'The Convergence', 'Build the Convergence Forge and unite the industrial, arcane, prosperity and voyager paths. The quest book records the materials you will need.'],
    ['minecraft:iron_sword', 'Wombatcraft Trials', 'Find the Trials quests for encounter requirements and rewards. Prepare before summoning a challenge, and keep encounters away from spawn.']
  ]}
};
function wcResidentMenu(player, role) {
  const resident = WCSpawnResidents[role];
  if (!resident) return;
  if (role === 'market') { wcMarketMenu(player); return; }
  player.openChestGUI(Text.of(resident.name).gold(), 3, gui => {
    gui.playerSlots = false;
    gui.slot(0, 0, 8, 2, slot => { slot.item = Item.of('minecraft:gray_stained_glass_pane').withName(Text.of(' ')); });
    if (role === 'guide' || role === 'quests') gui.button(4, 0, Item.of('ftbquests:book'), Text.of('Open the Quest Book').aqua(), click => {
      player.closeMenu();
      player.server.runCommandSilent('execute as ' + player.uuid + ' run ftbquests open_book');
    });
    resident.entries.forEach((entry, index) => {
      gui.button(2 + index * 2, 1, Item.of(entry[0]), Text.of(entry[1]).yellow(), click => {
        player.closeMenu();
        player.tell(Text.of('\n' + entry[1] + '\n').gold().append(Text.of(entry[2] + '\n').white()));
      });
    });
    gui.button(4, 2, Item.of('minecraft:barrier'), Text.of('Close').red(), click => player.closeMenu());
  });
}
ItemEvents.entityInteracted(event => {
  const tags = event.target.getTags();
  if (!tags.contains('wombatcraft_spawn_npc')) return;
  if (String(event.hand) !== 'MAIN_HAND') { event.cancel(); return; }
  Object.keys(WCSpawnResidents).forEach(role => {
    if (tags.contains('wc_npc_' + role)) wcResidentMenu(event.player, role);
  });
  event.cancel();
});
ServerEvents.commandRegistry(event => {
  const C = event.commands;
  const root = C.literal('wcnpc');
  const place = C.literal('place').requires(source => source.hasPermission(2));
  const menu = C.literal('menu');
  Object.keys(WCSpawnResidents).forEach(role => {
    menu.then(C.literal(role).executes(ctx => { wcResidentMenu(ctx.source.player, role); return 1; }));
    place.then(C.literal(role).executes(ctx => {
      const player = ctx.source.player;
      const resident = WCSpawnResidents[role];
      const nbt = '{Tags:["wombatcraft_spawn_npc","wc_npc_' + role + '"],NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b,CustomNameVisible:1b,CustomName:\'' + JSON.stringify({text:resident.name,color:'gold'}) + '\',VillagerData:{type:"minecraft:plains",profession:"minecraft:' + resident.profession + '",level:5}}';
      return ctx.source.server.runCommandSilent('execute as ' + player.uuid + ' at @s run summon minecraft:villager ~ ~ ~ ' + nbt);
    }));
  });
  root.then(place).then(menu);
  root.then(C.literal('remove').requires(source => source.hasPermission(2)).executes(ctx => {
    const player = ctx.source.player;
    let nearest = null;
    let distance = 9.0001;
    for (const entity of player.level.getEntitiesWithin(player.getBoundingBox().inflate(3))) {
      if (!entity.getTags().contains('wombatcraft_spawn_npc')) continue;
      const d = player.distanceToEntitySqr(entity);
      if (d < distance) { nearest = entity; distance = d; }
    }
    if (nearest == null) { player.tell(Text.of('No town resident within three blocks.').yellow()); return 0; }
    nearest.discard();
    player.tell(Text.of('Town resident removed.').green());
    return 1;
  }));
  event.register(root);
});

// Use Lightman's native trader menus for payments, escrow and inventory checks.
function wcOpenMarket(player, kind) {
  if (!WCMarketGuard.market(player)) { player.tell(Text.of('Visit the Town Market at spawn after leaving combat to trade.').yellow()); return; }
  const cache = WCTraderCache.TYPE.get(false);
  const trader = cache == null ? null : (kind === 'auctions' ? cache.getAuctionHouse() : cache.getTrader('wombatcraft_' + kind));
  if (trader == null) { player.tell(Text.of('The market is unavailable. Please tell a town administrator.').red()); return; }
  trader.openTraderMenu(player);
}
function wcMarketMenu(player) {
  player.openChestGUI(Text.of('Bram | Town Market').gold(), 3, gui => {
    gui.playerSlots = false;
    gui.slot(0, 0, 8, 2, slot => { slot.item = Item.of('minecraft:gray_stained_glass_pane').withName(Text.of(' ')); });
    gui.button(4, 0, Item.of('minecraft:paper'), Text.of('Town Contracts | Daily, community & factory orders').aqua(), click => {
      player.closeMenu();
      player.server.runCommandSilent('execute as ' + player.uuid + ' run contracts');
    });
    gui.button(1, 1, Item.of('minecraft:gold_ingot'), Text.of('Auction House | Browse, bid & sell').yellow(), click => wcOpenMarket(player, 'auctions'));
    gui.button(3, 1, Item.of('minecraft:chest'), Text.of('Server Shop | Browse categories').yellow(), click => wcShopCategories(player));
    gui.button(5, 1, Item.of('minecraft:emerald'), Text.of('Sell Counter | Materials & valuables').yellow(), click => wcSellMenu(player));
    gui.button(7, 1, Item.of('minecraft:book'), Text.of('Trading guide').yellow(), click => {
      player.closeMenu();
      player.tell(Text.of('Town Market\n').gold().append(Text.of('Auction House: browse listings and bid. Open its storage/create-auction tab to list your own items or collect completed sales. Coins use Lightman\'s Currency, in its wallets or the trader coin slots. The Server Shop offers everyday supplies; the Sell Counter buys bulk materials and selected valuables. You can auction valuable finds to other players. Successful auctions charge a 5% fee. Check every quantity and price before trading.').white()));
    });
    gui.button(4, 2, Item.of('minecraft:barrier'), Text.of('Close').red(), click => player.closeMenu());
  });
}

function wcShopCategories(player, page) {
  page = page || 0;
  const categories = [
    [
        "food",
        "minecraft:bread",
        "Food & Farming | Essentials"
    ],
    [
        "building",
        "minecraft:bricks",
        "Stone & Earth | Essentials"
    ],
    [
        "decor",
        "minecraft:lantern",
        "Decoration & Lighting | Essentials"
    ],
    [
        "gear",
        "minecraft:iron_pickaxe",
        "Tools & Travel | Essentials"
    ],
    [
        "utility",
        "minecraft:redstone",
        "Redstone & Utilities | Essentials"
    ],
    [
        "supply_food_1",
        "minecraft:bread",
        "Food & Farming"
    ],
    [
        "supply_building_1",
        "minecraft:bricks",
        "Stone & Earth 1"
    ],
    [
        "supply_building_2",
        "minecraft:bricks",
        "Stone & Earth 2"
    ],
    [
        "supply_decor_1",
        "minecraft:lantern",
        "Decoration & Lighting 1"
    ],
    [
        "supply_decor_2",
        "minecraft:lantern",
        "Decoration & Lighting 2"
    ],
    [
        "supply_gear_1",
        "minecraft:iron_pickaxe",
        "Tools & Travel"
    ],
    [
        "supply_utility_1",
        "minecraft:redstone",
        "Redstone & Utilities"
    ],
    [
        "supply_wood_1",
        "minecraft:oak_log",
        "Timber & Carpentry 1"
    ],
    [
        "supply_wood_2",
        "minecraft:oak_log",
        "Timber & Carpentry 2"
    ],
    [
        "supply_wood_3",
        "minecraft:oak_log",
        "Timber & Carpentry 3"
    ],
    [
        "supply_colours_1",
        "minecraft:blue_concrete",
        "Colourful Building 1"
    ],
    [
        "supply_colours_2",
        "minecraft:blue_concrete",
        "Colourful Building 2"
    ],
    [
        "supply_colours_3",
        "minecraft:blue_concrete",
        "Colourful Building 3"
    ],
    [
        "supply_colours_4",
        "minecraft:blue_concrete",
        "Colourful Building 4"
    ],
    [
        "supply_plants_1",
        "minecraft:poppy",
        "Flowers & Gardens"
    ],
    [
        "supply_materials_1",
        "minecraft:iron_ingot",
        "Materials & Ingredients"
    ],
    [
        "supply_nether_1",
        "minecraft:netherrack",
        "Nether Supplies 1"
    ],
    [
        "supply_nether_2",
        "minecraft:netherrack",
        "Nether Supplies 2"
    ],
    [
        "supply_end_1",
        "minecraft:end_stone",
        "End Building"
    ],
    [
        "supply_ocean_1",
        "minecraft:prismarine",
        "Ocean Supplies"
    ],
    [
        "supply_equipment_1",
        "minecraft:iron_chestplate",
        "Equipment & Clothing"
    ],
    [
        "supply_transport_1",
        "minecraft:minecart",
        "Boats & Minecarts"
    ],
    [
        "supply_records_1",
        "minecraft:white_banner",
        "Signs & Banners"
    ],
    [
        "sell",
        "minecraft:emerald",
        "Bulk Materials Buyback"
    ],
    [
        "valuables",
        "minecraft:diamond",
        "Valuables Exchange"
    ]
];
  player.openChestGUI(Text.of("Bram | Server Shop").gold(), 6, gui => {
    gui.playerSlots = false;
    gui.slot(0, 0, 8, 5, slot => { slot.item = Item.of('minecraft:gray_stained_glass_pane').withName(Text.of(' ')); });
    categories.slice(page * 28, (page + 1) * 28).forEach((category, index) => {
      gui.button(1 + (index % 7), 1 + Math.floor(index / 7), Item.of(category[1]), Text.of(category[2]).yellow(), click => wcOpenMarket(player, category[0]));
    });
    if (page > 0) gui.button(1, 5, Item.of('minecraft:arrow'), Text.of('Previous categories'), click => wcShopCategories(player, page - 1));
    if ((page + 1) * 28 < categories.length) gui.button(7, 5, Item.of('minecraft:arrow'), Text.of('More categories'), click => wcShopCategories(player, page + 1));
    gui.button(4, 5, Item.of('minecraft:arrow'), Text.of('Back to the Town Market').aqua(), click => wcMarketMenu(player));
  });
}

function wcSellMenu(player) {
  player.openChestGUI(Text.of('Bram | Sell Counter').gold(), 3, gui => {
    gui.playerSlots = false;
    gui.slot(0, 0, 8, 2, slot => { slot.item = Item.of('minecraft:gray_stained_glass_pane').withName(Text.of(' ')); });
    gui.button(3, 1, Item.of('minecraft:cobblestone'), Text.of('Bulk Materials').yellow(), click => wcOpenMarket(player, 'sell'));
    gui.button(5, 1, Item.of('minecraft:diamond'), Text.of('Valuables Exchange').aqua(), click => wcOpenMarket(player, 'valuables'));
    gui.button(4, 2, Item.of('minecraft:arrow'), Text.of('Back to the Town Market').aqua(), click => wcMarketMenu(player));
  });
}
