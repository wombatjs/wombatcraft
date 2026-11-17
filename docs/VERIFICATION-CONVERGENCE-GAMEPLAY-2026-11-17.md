# Convergence gameplay acceptance — 2026-11-17

The expanded local implementation passed its final dedicated-server startup and clean restart. No public release or live deployment was performed.

## Implemented and exercised

- Four processing stations: Forge, Infuser, Stabilizer and Matrix. All 47 progression recipes registered. All 31 new machine recipes plus the original three Forge recipes processed through the actual Multiblocked2 handlers.
- Exact ingredient, FE and mana consumption; correct output stacks; actual item stack limits; no processing without resources. Blocked product slots kept both ingredients and power intact.
- Top-face Forge item capabilities exposed six insertable input slots and two extractable output slots on every station. Internal test storage was cleared before recipe execution.
- Research Terminal: two simulated server-player objects shared an actual FTB effective-team ledger. Repeated and renamed evidence was accepted without consuming inventory. Switching to an independent effective team did not copy knowledge; rejoining restored access. FTB serialization and the next server boot preserved discoveries.
- Terminal action checks rejected an unknown button, a different player, excessive distance and a removed terminal. The implementation also binds the menu to its original Level instance; cross-dimension validation is source-checked, not separately exercised by this fixture.
- Field Projector: no charge means no effect; a charged use grants water breathing and night vision and consumes exactly one charge; an immediate second use consumes nothing because of cooldown.
- Astralium Boots halve the fall multiplier; ordinary diamond boots receive no bonus.
- Prospector detects the placed nearby ore, spends one durability and rejects repeated scans during cooldown.
- Ore loot: normal crystal drop, Silk Touch ore drop and Fortune III outputs within the expected range.
- Aetherium full-set bonus: 10-point damage events became 8 for every one of 24 selected vanilla/Ars/Iron's Spellbooks/Mana and Artifice/Cataclysm types. Player-attributed events remained 10 for every type. Partial sets and generic damage were unchanged. Blood Magic ritual/sacrifice, Botania relic damage and Heartstop stayed at 10.
- Existing three gateway Trials, quest/item/recipe references, portal checks and dimension-generation checks passed.

These are real server-side objects and APIs, but simulated players and direct damage-handler events. They do not establish a complete connected-client combat or invitation-flow playtest.

## Build checks

- Java 17 Forge build and reobfuscation passed; 34 Java rule regression tests passed.
- All 34 Python tests passed, including station-bootstrap reachability, ambiguous-input rejection, all-six-branches dependency checks, resource references, generated-file consistency and independent station model/type checks.
- Quest validation passed for 3,126 quests across 115 chapters, including IDs, dependency cycles and layout overlap.
- Trial regeneration preserved the new station translations. General reward generation excludes both Convergence chapters from automatic material rewards.
- `git diff --check` passed.

## Bugs and design hazards addressed

The foundation's oversized pearl stack was corrected earlier. This expansion separates accelerated and normal recipes by station so a subset match cannot select the wrong process. It preserves the Forge's original recipe type while giving each new station its own definition, model and recipe category. Station dependencies were checked alongside material dependencies to avoid requiring a machine to produce its own unlock material.

The existing broad recipe-output removal was replaced with verified exact IDs after a startup hang in Quark/Universal Enchants recipe construction. The four disabled utility recipes remain absent in both final boots. Trial generation no longer overwrites the new machine translations. Team evidence uses item identity so renamed trade goods are valid.

## Evidence and limits

Local generated evidence:

- `dist/convergence-gameplay-java.log`
- `dist/convergence-gameplay-tests.log`
- `dist/convergence-gameplay-release-check.log`
- `dist/server-smoke/console-first-start.log` and `console.log`
- `dist/server-validation.json`

The cached disposable world was reused. Natural ore distribution in newly generated terrain was not visually sampled. No connected-client UI/rendering pass, real FTB invite/join/leave flow, complete pipe-network build, or long-term multiplayer economy playthrough was performed. The terminal screen and tooltips compile; those client-facing checks remain necessary before production acceptance. Models reuse existing textures.

Both client and server require the new module and pack overrides. Existing prototype jobs moved from the Forge to the Infuser/Stabilizer must be moved in established factories before rollout.
