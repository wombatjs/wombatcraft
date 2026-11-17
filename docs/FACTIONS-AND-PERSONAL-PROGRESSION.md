# Factions, contracts and individual progression

Factions manage membership, invitations, allies, chat and shared territory. Their menu has only Faction, Members and Territory tabs. The claim map and territory policies continue to use the faction's FTB team.

The pause menu now has separate **Factions**, **Contracts**, **Town Hall** and **Journey** buttons. Commands provide the same entry points:

| Entry | Purpose |
| --- | --- |
| `/faction` | Faction members and land |
| `/contracts` | Browse orders and personal earnings |
| `/townhall` | Mayor, policies and election ballot |
| `/journey` | Individual quest progress and recommended goals |

Contracts and elections open their own screens, with separate data payloads and action routing. Contracts can be browsed anywhere; delivery and coin collection still require the contract desk at spawn. Voting retains the spawn requirement. Civic NPCs open the appropriate screen directly.

Factory orders are weekly personal industrial orders, available without a faction. Their internal legacy category remains `FACTION` for compatibility with contract definitions and mayor bonus rules, but their ownership key is `industrial/<player UUID>`. Joining, leaving or changing factions does not refresh those orders. Existing unpaid earnings and transaction records are retained. Old shared factory-order rows remain archived; the new personal rows start separately, without an automatic payout. Public community projects remain shared town activities, with earnings paid to each contributor.

## Individual gameplay progress

FTB quest tasks, completions, rewards, recommendations and Convergence research now belong to a player's UUID. Faction changes do not grant, reset or transfer progress. Item detection uses the carrying player's own journal. Quest updates and automatic rewards target that player rather than everyone in the faction.

This does not add recipe locks. Players can still trade materials and use shared workshops, but each player records their own carried research evidence and completes their own quests. Existing quest IDs, links, task definitions and reward identities remain stable; relevant descriptions now explain individual ownership.

The companion mod applies a narrow compatibility patch to the pinned FTB Quests 2001.4.22 integration: player lookup, login, client ownership, inventory detection, reward context and update recipients use personal records. FTB's quest transfer on team creation/join/leave is disabled. FTB Teams and Chunks themselves retain faction ownership for land and permissions. Both client and server need the updated companion JAR; menu network protocol is version 2.

## Existing-world migration

On the first server start after this change, every known player, including offline members, receives their existing legacy shared quest progress and research in their personal record. Because the old save records shared completion rather than who performed each task, that historical shared progress is retained for the members who already had access to it.

The migration merges progress, existing reward-claim records, repeatable cooldowns and completion counters, preserving locks and reward blocks. It does not issue rewards or consume items. Personal saves are written before the world migration marker is committed. A retry merges the same records without resetting claims or awarding anything. Original faction records are retained. Later faction joins do not copy progress.

The implementation uses FTB's existing save format. Research lives in the permanent personal player-team record, not the active faction. New task screens bind to the placing player's UUID. Legacy faction-owned task screens retain their old owner record; replace those screens to bind them to an individual journal.

## Verification

The dedicated-server fixture tests two players in one faction, individual item detection, ownership checks, faction switching, personal research serialization, legacy completion/claim migration, migration idempotence, independent menu payloads, menu packet serialization and solo contract access. Full smoke validation also checks existing land protections, utility guards, quests, recipes and Convergence machines, then repeats after restart.

Client-side button layout, click navigation, and actual invitations with connected players still require in-game acceptance. Synthetic server fixtures are not a multiplayer playthrough. These changes have not been deployed to the public server.
