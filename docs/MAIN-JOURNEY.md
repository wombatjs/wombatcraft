# Main Journey and Adventure Journal

Wombatcraft keeps its 3,126 original quests and adds a navigation layer tailored to this pack. The resulting book has 3,142 quests in 117 chapters: the 16 additional quests are optional reading cards, with no material rewards. There are also 91 quest links, which refer to existing quests rather than creating new objectives.

## Player experience

Open **What Should I Do?**, the first chapter in Getting Started. Choose technology, magic, exploration, space, nature, settlement or Convergence. Its 19 linked icons provide a compact selection of goals. The settlement card currently highlights the Warehouse; its prerequisites provide the route into settlement building.

Use **/journey** for live recommendations. It reads your individual progress, prioritizes objectives already underway, and offers up to three other specialties. Click a recommendation to open the original quest, then pin it using FTB Quests' own controls. **/journey magic**, for example, focuses on that specialty. The other accepted paths are survival, technology, exploration, space, nature, settlement and convergence.

If a selected milestone is locked, the command follows its existing dependency graph to find an incomplete, visible prerequisite whose tasks you can start. Completed or excluded objectives are skipped. Suggestions indicate the milestone that the prerequisite leads toward. The command does not complete tasks, claim rewards, change pins or write progression data. Requests have a two-second cooldown.

**Main Journey** presents 72 linked milestones in eight rows:

| Direction | Purpose |
| --- | --- |
| First Steps | Establish a home, tools, storage and navigation |
| The Industrial Age | Choose mechanical power or FE production and start processing |
| Arcane Discovery | Start spellcraft or Botania and magical crafting |
| Into the Unknown | Prepare for dimensional travel, expeditions and Trials |
| Advanced Engineering | Automate storage, manufacturing, farming or a settlement |
| Beyond the Stars | Produce fuel and oxygen, launch, then reach new planets |
| Ancient Powers | Follow boss hunts and advanced renewable resource goals |
| The Convergence | Combine specialist materials and sigils into the Core and Matrix |

These are directions rather than eight enforced stages. A player can specialize while teammates take other paths. Existing source quests retain their original prerequisites and reward rules. The new guide chapters are flexible; existing chapters are not globally changed to flexible progression. The detailed chapters remain the source for recipes, requirements and rewards.

## Progress, rewards and multiplayer

FTB QuestLink objects resolve to the original quest identity. Completing a linked quest counts in its original chapter, Main Journey and the journal wherever that quest appears. There is no duplicate reward pool. Quest progress is keyed to your player UUID. Joining, leaving or changing a faction does not transfer or reset it. The guide reads that same personal quest ledger. See [individual progression](FACTIONS-AND-PERSONAL-PROGRESSION.md) for migration behavior.

The 72-milestone count is an optional completionist measure, not a requirement to unlock every recipe. Research, crafting and the Convergence Matrix keep their existing gameplay requirements. Repeatable work remains available through contracts and Trials. This change does not add a new title system, server-wide achievement system or bespoke graphical journal; the dynamic recommendations appear in chat.

## Authoring and maintenance

`python tools/build_journey.py` rebuilds the two guide files and their catalog without writing any original chapter. `tools/build_quests.py` also invokes this generator after assembling the underlying chapters. Stable IDs derive from semantic keys; linked targets retain their original IDs.

Milestones are curated in `tools/build_journey.py` against exact chapter filenames and titles. Missing or ambiguous titles fail generation. `docs/journey-inventory.json` inventories every original quest, its tasks, dependencies and detected mod namespaces. `docs/journey-build.json` records counts and guide chapter IDs. The companion mod loads its generated milestone catalog from server data resources and refreshes it after resource reload.

Run `python -m unittest discover -s tests` and `python tools/validate.py` after editing. Rebuild the companion mod when its bundled catalog changes. The dedicated-server smoke test checks all quest identities, all 91 runtime links, shared progress, reachable recommendations, completed-objective exclusion, branch filtering and absence of progress mutation. It repeats after restarting the disposable world.

Client-side layout, click navigation and pinning still require a player acceptance pass. Server runtime validation does not substitute for that check.
