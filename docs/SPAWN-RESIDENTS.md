# Spawn safe zone and residents

The dedicated `WombatcraftSpawn` FTB server team defines the safe zone (team `2de87e97-dce4-41d0-b02d-c24a395997a1`). Keep the castle and paths inside its claimed chunks. Adding or removing chunks changes the boundary automatically; ordinary player claims are unaffected.

The server guard rejects hostile entity joins (including disk loads), removes hostiles that enter within one second without loot, cancels living damage and knockback inside, and prevents players inside from attacking targets outside. Fire visuals are cleared once per second. FTB retains ownership/build, interaction, PvP, explosion and mob-griefing permissions. Vanilla spawn-protection remains disabled so it cannot conflict with FTB ownership. This does not restore hunger or remove every status effect, and administrative commands are not a security boundary.

## Place residents

Stand on the floor at the desired position and run one command as an operator:

- `/wcnpc place guide` — Mara, Welcome Guide: first steps, homes and claims.
- `/wcnpc place market` — Bram, Market Steward: player auctions, server supplies and trading help.
- `/wcnpc place travel` — Pip, Wayfinder: teleport requests, maps and dimension expeditions.
- `/wcnpc place quests` — Iris, Quest Archivist: progression, Convergence and Trials.

Right-click a resident to open their menu. They are persistent, silent, invulnerable villagers with AI disabled and Carry On pickup blocked. No extra NPC mod is required. `/wcnpc remove` removes only the closest tagged resident within three blocks; placement/removal require operator permission level 2. `/wcnpc menu <role>` opens the corresponding menu without placing another resident.

## Market

Bram opens Lightman's Currency's native shared Auction House. Players can browse/bid, create listings and retrieve items or proceeds using its storage tabs. The mod handles escrow, ownership, inventory capacity and payment. Server supplies use category-specific persistent creative traders with the same coin currency and native trade interface.

The shop has 194 offers across seven categories: Food & Farming (23), Building Materials (27), Decoration & Lighting (33), Tools & Travel (25), Redstone & Utilities (34), Sell Counter (6), and Valuables Exchange (46). Stock includes crops, food, eight wood types, building stone, all sixteen dyes, lighting, iron tools, transport supplies, storage and redstone components. It does not sell diamond/netherite equipment, enchanted equipment, modded machines or endgame rewards.

Prices live in `pack/config/lightmanscurrency/PersistentTraders.json`, with IDs `wombatcraft_food`, `wombatcraft_building`, `wombatcraft_decor`, `wombatcraft_gear`, `wombatcraft_utility`, `wombatcraft_sell`, and `wombatcraft_valuables`. Player auctions remain player-priced. Put coins in a wallet or the native trader's coin slots to pay. Closing a trader returns unused loose coins. The sell counter pays 1 copper for 32 oak logs or 64 cobblestone/rotten flesh/sugar cane/bamboo; 64 wheat pays 2 copper. Buy prices exceed buyback prices.

Back up the persistent-traders file before adding other traders. Do not enable LC Admin Mode for ordinary players. Pricing is deliberately modest; review earnings as automated farms grow.

## Performance and deployment

All economy and spawn files are dedicated-server overrides, not singleplayer/client additions. The guard performs one loaded-overworld sweep per second, caches entity type classifications and never loads chunks. Residents have no pathfinding AI. A startup-script change needs a server restart; NPC server-script edits can be reloaded with KubeJS. Back up and retire the old live-only `spawn_claim_mobs.js` when deploying the new guard.

## Valuables and currency sinks

The exchange buys the following bundles for copper coins:

| Valuable | Quantity | Copper |
|---|---:|---:|
| Diamond | 1 | 24 |
| Emerald | 32 | 1 |
| Gold Ingot | 1 | 4 |
| Iron Ingot | 4 | 4 |
| Copper Ingot | 16 | 1 |
| Lapis Lazuli | 16 | 4 |
| Amethyst Shard | 16 | 2 |
| Quartz | 16 | 4 |
| Nautilus Shell | 1 | 16 |
| Heart Of The Sea | 1 | 64 |
| Echo Shard | 1 | 16 |
| Rabbit Foot | 1 | 8 |
| Netherite Scrap | 1 | 64 |
| Nether Star (minecraft) | 1 | 128 |
| Shulker Shell (minecraft) | 1 | 8 |
| Wither Skeleton Skull (minecraft) | 1 | 32 |
| Dragon Breath (minecraft) | 16 | 4 |
| Ghast Tear (minecraft) | 4 | 8 |
| Blaze Rod (minecraft) | 8 | 4 |
| Ender Pearl (minecraft) | 16 | 2 |
| Prismarine Crystals (minecraft) | 16 | 2 |
| Sponge (minecraft) | 1 | 8 |
| Ancient Debris (minecraft) | 1 | 64 |
| Netherite Ingot (minecraft) | 1 | 256 |
| Scute (minecraft) | 4 | 4 |
| Totem Of Undying (minecraft) | 1 | 8 |
| Zanite Gemstone (aether) | 4 | 4 |
| Ambrosium Shard (aether) | 16 | 2 |
| Skyjade (deep_aether) | 4 | 8 |
| Stratus Ingot (deep_aether) | 1 | 32 |
| Carminite (twilightforest) | 4 | 8 |
| Fiery Blood (twilightforest) | 1 | 16 |
| Fiery Tears (twilightforest) | 1 | 16 |
| Knightmetal Ingot (twilightforest) | 4 | 8 |
| Steeleaf Ingot (twilightforest) | 4 | 4 |
| Ironwood Ingot (twilightforest) | 8 | 2 |
| Manasteel Ingot (botania) | 4 | 4 |
| Elementium Ingot (botania) | 4 | 8 |
| Terrasteel Ingot (botania) | 1 | 24 |
| Mana Pearl (botania) | 16 | 2 |
| Source Gem (ars_nouveau) | 16 | 2 |
| Certus Quartz Crystal (ae2) | 16 | 2 |
| Fluix Crystal (ae2) | 16 | 4 |
| Fluorite Gem (mekanism) | 16 | 2 |
| Zinc Ingot (create) | 16 | 2 |
| Brass Ingot (create) | 16 | 2 |

The server shop removes spent coins. Successful player auctions charge a 5% fee, removed from circulation rather than deposited into a server tax account. There is no listing fee. Players may have ten active auctions, lasting up to seven days. Buyback creates money; shop spending and auction fees are the sinks. These are initial prices, not a claim of immunity to every modded farm or recipe loop.

Valuables prices are deliberately below the cost of directly recoverable materials from the server shop (for example, clocks and iron equipment). Review money generation as automation grows.

Emeralds use a conservative bulk price because discounted villager trades can turn purchased logs, sticks or paper into emeralds. The normal and one-item discounted stick trade cannot return more copper than the server log bundle costs.

Resource-to-coin minting is disabled on the server so it cannot bypass the buyback economy. Coin exchange, wallets and player trading remain available. Coin melting remains disabled.

The expanded exchange accepts 26 vanilla valuables and 20 modded materials from eight mods. Farmable drops and industrial materials use conservative bundle prices. High-demand loot can still earn more through player auctions. No new ticking handlers or mods are added.
