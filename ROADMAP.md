# Release checklist

- [x] Create public GitHub repository.
- [x] Preserve the tested unbranded Forge baseline and Fabric backup.
- [x] Measure ATM9 chapter structure, task types and reward distribution at a pinned commit.
- [ ] Replace shallow major-mod chapters with original progression and integrated addon branches.
- [ ] Validate quest dependencies, task IDs, runtime content and rewards.
- [ ] Build and test branded menu with official-server join action.
- [ ] Publish a reproducible client/server release manifest.
- [ ] Create Pterodactyl Forge instance and validate fresh-world startup.
- [ ] Add dedicated ports to the existing public proxy; verify external connections.
- [ ] Install backup, daily maintenance and empty-server update controller; test rollback.
- [ ] Build launcher, test installation/update recovery and launch command.
- [ ] Register Microsoft application and obtain Minecraft API approval (external dependency).
- [ ] Complete licensed-account join, claim protection and multiplayer acceptance tests.

## Quest research

ATM9 reference commit: `255ca23b91cebbb57130074e0bf7b86c266ac2a6`.
67 chapters, 4,034 quests. Major examples: Create 57; AE2 70; Ars Nouveau 130; Botania 144; Blood Magic 51; Mekanism 97 plus reactors 43; PneumaticCraft 129. These are measurements, not quotas. ATM9 includes systems absent here, including extensive GregTech progression.

Structure to learn from: prerequisite-driven subbranches, multiple stages per major system, optional utility paths, varied task types and deliberately paced rewards. Wombatcraft will not reuse ATM text, IDs, layouts or reward tables.
