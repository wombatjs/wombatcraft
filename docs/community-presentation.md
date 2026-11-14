# Community presentation

The official server uses the Forge 1.20.1 build of TAB 5.2.1, pinned by SHA-512 in the lockfile. Its plum/copper header, online count, world and ping accompany a compact sidebar with rotating command tips. `/sb` toggles the sidebar and remembers the choice. Nameplate teams and artificial player-list slots are disabled to avoid overriding gameplay teams. Placeholder updates default to two seconds.

FTB Essentials supplies `/tpa <player>`, `/tpaccept`, `/tpdeny` and `/tpahere <player>`. The pack enables requests and sets a 10-second cooldown and 3-second warmup. Existing-world overrides should be checked at deployment.

`config/wombatcraft/server.json` owns MOTD and 100 advertised slots. The maintenance controller applies these two properties and the 64px server icon after taking its normal backup and stopping the server. It preserves other properties. Slots are not a 100-player load-test result.

The Windows launcher keeps the official server first in servers.dat, retaining other servers and unknown NBT fields, backing up the file, and failing without overwriting unreadable data. The server supplies the styled MOTD over its existing public address.

The Venus sandstone compatibility tag repairs Ad Astra 1.15.21's missing `forge:sandstone/venus_sandstone` reference used by its Mekanism and Immersive Engineering crushing recipes. It does not suppress Mekanism warnings or remove recipes.
