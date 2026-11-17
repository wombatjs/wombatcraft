# Convergence Arsenal

This expansion gives the production tree personal equipment rewards. It adds 16 items, completes the existing Astralium set and adds nine non-consuming quests. No faction membership or shared progression is required. It does not change upstream weapons, armor, mobs or boss health.

## Armor ladder

| Set | Total armor | Total toughness | Knockback resistance | Full-set benefit |
| --- | ---: | ---: | ---: | --- |
| Aetherium (existing) | 20 | 8 | 0% | 20% less tagged non-player magic damage |
| Astralium | 24 | 12 | 40% | 15% less ordinary non-player damage |
| Convergium | 30 | 16 | 80% | 35% less ordinary non-player damage; 75% less fall damage |

Astralium Boots alone still halve fall damage. Their existing item ID and recipe are retained; existing pairs receive the new material stats. Astralium armor uses durability multiplier 45 (495/720/675/585 by helmet/chest/legs/boots), Convergium uses 80 (880/1280/1200/1040). Both have enchantability 24 and repair with their corresponding ingots. The new gear resists burning as dropped items; this is not player fire immunity.

Set mitigation is applied before the ordinary armor calculation. It requires all four exact pieces and excludes player-attributed attacks, armor-bypassing damage and invulnerability-bypassing damage. It does not protect against void or sacrifice costs and is not magic resistance. Aetherium retains its separate specialist niche. No oxygen, automatic resurrection or creative flight is supplied.

## Weapons and tools

| Item | Baseline benefit |
| --- | --- |
| Aetherium Saber | 12 attack damage, 2 attacks/second; netherite durability and repair |
| Astralium Blade | 18 attack damage, 1.8 attacks/second; netherite durability and repair |
| Convergium Greatblade | 32 attack damage, 1.4 attacks/second, 4096 durability |
| Convergium Longbow | 2.5x arrow base damage, 3072 durability; normal draw, ammunition and enchantment rules |
| Convergium Pickaxe / Axe / Shovel | Mining speed 18, 4096 durability, enchantability 24; tier sorted after netherite |
| Convergence Aegis | Active defensive equipment, usable in either hand |

Attack damage includes the normal player's base 1, before enchantments, effects, critical hits or modded modifiers. These are starting balance values; they do not establish superiority over every configurable Draconic or Mekanism loadout. Tools perform normal single-block actions, preserving ordinary claim checks and drops. Endgame equipment repairs with Convergium. No tool forces a bypass of another mod's block restrictions.

Explicit Better Combat profiles give the Saber a cutlass moveset, the Astralium Blade a sword moveset, the Greatblade a two-handed claymore moveset and the Axe an axe moveset. Claymore combo multipliers are inherited from the installed mod (0.75 / 1 / 1.25), so 32 is the Greatblade's base attribute, not a promise that every swing deals exactly 32 damage. Two-handed combat restricts offhand use under Better Combat's normal rules.

Greatblade use consumes one Field Charge for Strength III and Speed II for 10 seconds, with a 30-second cooldown. Aegis use consumes one Field Charge for eight absorption hearts for 20 seconds and Regeneration II for five seconds, with a 60-second cooldown. Existing stronger vanilla effects follow vanilla merge behavior. Cooldowns belong to the player and item type, so swapping copies cannot bypass them. Creative mode is exempt from charge cost. All activation is server-authoritative; no resources are spent on failed cooldown checks. Effects can also affect normal PvP combat under the server's existing rules; the armor's extra set mitigation excludes PvP.

## Crafting and progression

Craft the Saber from two Aetherium ingots and an Arcane Core. Craft the three new Astralium pieces with ordinary armor patterns. Upgrade the Saber into the Astralium Blade at a smithing table using an Astralium ingot and Netherite Upgrade template.

The Matrix makes **one Convergent Armament** from eight Convergium, two Ancient Cores, two Mechanized Cores, two Arcane Cores, four Stabilized Flux and one Living Relic. Each batch costs 4,000,000 FE, 400,000 mana and 60 seconds. All six specialties contribute through the alloy and Living Relic. The Matrix still requires the existing Convergence Core milestone.

At a smithing table, spend one armament and one Netherite Upgrade template per upgrade:

- Astralium armor to Convergium armor.
- Astralium Blade to Convergium Greatblade.
- Netherite pickaxe, axe or shovel to its Convergium equivalent.
- Bow to Convergium Longbow.

Smithing preserves the base item's NBT, including enchantments, custom name and existing damage. It does not repair equipment for free. Craft the Aegis separately from one armament, one Living Relic and one Totem of Undying.

The Arsenal chapter has nine personal objectives. Main Journey adds Armament, Greatblade and final armor milestones, reaching 75 milestones and 94 total links. Existing quest IDs and rewards remain intact. Gear is tradeable and there are no faction-dependent bonuses.

## Build and acceptance

Authoring lives in `tools/convergence_arsenal.py`; the normal progression builder calls it. Java content is in `ConvergenceArsenal`, with client-only longbow draw predicates in `client/ArsenalClient`. Item models and worn armor currently reuse Minecraft textures; bespoke visual designs and connected-client render acceptance are not included.

`ConvergenceArsenalAudit` runs only under the disposable-server QA flag and verifies armor totals, repairs, full/partial sets, PvP and bypass exclusions, fall multipliers, charge consumption, stack-swap cooldowns, buffs, arrow scaling and all ten smithing upgrades retaining item data. The normal machine audit also processes the Armament recipe. Multiplayer combat feel and long-term survival balance still need player testing.

Restart testing exposed a KubeJS 2001.6.5-build.26 race: `Lazy.forget()` cleared the value while `Lazy.get()` could still observe the cached flag, giving `GeneratedData` a null byte array. `coremods/kubejs_lazy.js` synchronizes those two methods on each Lazy instance and retains existing factories and expiry behavior. `GeneratedDataAudit` verifies the transformed methods and 16,000 concurrent read/invalidate cycles. It does not suppress missing data or return empty recipes on errors.

This is a local expansion, not a deployment. Client and server need the rebuilt companion jar, recipes and quest data together.
