# Quest rewards

Wombatcraft follows the reward pattern observed in [ATM9's quest book](https://github.com/AllTheMods/ATM-9/tree/255ca23b91cebbb57130074e0bf7b86c266ac2a6/config/ftbquests/quests): small crafting supplies throughout a branch, useful equipment at milestones, and stronger prizes for substantial achievements. ATM9's Create chapter supplies a wrench, andesite and cogwheels; its Ars Nouveau and AE2 tables provide modest batches of materials. Wombatcraft's rewards are authored separately for this pack's progression and installed items.

- 1,881 quests now have item rewards, compared with 820 previously. The 841 XP-only quests now also provide supplies. Some small steps and reading/manual objectives remain unrewarded.
- 60 milestones have explicit prizes: workshop tools, utility upgrades, crafting materials, capture supplies, consumable protection, and exploration relics.
- Routine rewards are small replenishments. Route endpoints provide larger bundles. Major bosses and Convergence achievements award stronger utility items.
- No random loot tables, commands, new mods or repeatable currency payouts are introduced. Manual building/project checkmarks receive no new rewards.
- Quest IDs, objectives, dependencies, layout and existing reward IDs are preserved. Previously claimed slots stay claimed; newly added slots are available once on already-completed quests.

`quests/rewards.json` is the editable source. `tools/quest_rewards.py` applies it at the end of the quest compiler, validates items against the live registry, and emits `docs/quest-rewards.json`. Rebuilding quests preserves the reward pass.
