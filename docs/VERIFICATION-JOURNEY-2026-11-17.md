# Guided journey verification — 2026-11-17

Scope: the pack-specific Main Journey, Adventure Journal and `/journey` recommendation service. These changes are local; no public server deployment or release was performed.

## Automated checks

- Python suite: 38 tests passed, including generator determinism, missing/ambiguous milestone rejection, all 3,126 original quests preserved, and absence of duplicate task/reward objects on links.
- Quest validation: 3,142 quests, 117 chapters, 344 pinned mod files; unique positive IDs, non-overlapping guide nodes, valid link targets and an acyclic dependency graph.
- Companion mod: Gradle `test build` passed, including 34 existing unit tests.
- Dedicated server: loaded the exact expected quest IDs and all 91 expected link-to-target pairs with FTB Quests 2001.4.22 and Forge 47.4.28.
- Clean restart passed: both server boots completed the journey audit, existing Convergence gameplay audits and registry checks; the smoke process exited successfully after shutdown.
- An isolated, unsaved FTB TeamData fixture received reachable recommendations; filtering to magic retained only that path, and a completed objective disappeared from subsequent suggestions.
- Serialized fixture progress was identical before and after requesting recommendations.
- Repeatedly completing recommended fixture objectives reached all 72 milestones in 196 steps, including source prerequisites. This proves dependency reachability in the loaded quest graph, not the time or difficulty of actually obtaining every item.
- Each QuestLink resolved to the original Quest instance. A completed source quest remained completed when accessed through its link. QuestLink's own progress method returns zero because it is a navigation wrapper; the installed client QuestLinkButton passes the resolved Quest to QuestButton for display.

The first full-server run caught a Forge runtime method-name incompatibility when reading quest titles. The service now reads explicit pack titles using `getRawTitle`; the corrected server audit passed.

## Acceptance limits

The test uses a disposable, cached smoke world and synthetic progression data, not a fresh human playthrough. No real player quest save was migrated or edited. Client layout, click-to-open behavior and pinning still need an in-game acceptance pass. Existing upstream mod warnings are not a claim that the entire pack is bug-free.

Runtime evidence is written to `dist/journey-build.log`, `dist/journey-smoke.log`, `dist/server-smoke/console-first-start.log`, `dist/server-smoke/console.log`, `dist/server-smoke/quest-link-runtime-audit.tsv` and `dist/server-validation.json`. The server smoke test requires the journey audit on both startup and restart.
