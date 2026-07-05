# MyEngine Engine Roadmap

Aggregated engine gaps across all games, with demand counts. Rules:
`docs/agentic/SPEC_BOARD.md` (Engine Roadmap, Gap Dedup Rule). A gap demanded by
two or more games outranks single-game gaps of the same severity.

Updated by: the `/me-spec` backlog bridge (new gaps / new demand) and `/me`
close-out (status changes).

Last updated: 2026-07-05

## Capabilities

| Capability | Cards | Demanded by | Demand | Status |
|---|---|---|---:|---|
| Defense kill-reward deposit into player resources | SG-002, MTD-001 | signal-garden, mytd | 2 | **done** (SG-002 implemented 2026-07-04; MTD-001 closed 2026-07-05 as duplicate; MyTD gold maps to content-defined `rewardResource`) |
| Render surface + palette (snapshot -> RenderFrame) | SG-003 (+follow-up), MTD-005 | signal-garden, mytd | 2 | **partial** (desktop rasterizer done; MTD-005 adds touch input + Android wiring, still backlog) |
| Content pack authoring/validation (game pack) | SG-001 | signal-garden | 1 | done (2026-07-04) |
| Android lifecycle save smoke (incl. any-tick save via pending-CommandQueue persistence) | SG-004 (+follow-up) | signal-garden | 1 | **done** (SG-004 implemented 2026-07-04; follow-up closed 2026-07-05; `SandboxSaveCodec` v1->v2, quiescent-save precondition dropped) |
| Balance report with suspicious-value checks | SG-005 | signal-garden | 1 | **done** (SG-005 implemented 2026-07-05: devtools `balance-report`/`balance-delta` JSON compares baseline vs changed content and flags enemy/core/resource deltas) |
| Gold cost gating in placeTower | MTD-002 | mytd | 1 | **done** (2026-07-05; existing generic `tower.costResource`/`costAmount` gate verified with `SandboxTowerCostGatingTest`) |
| Tower upgrade hook | MTD-003 | mytd | 1 | backlog |
| Difficulty modifiers | MTD-004 | mytd | 1 | backlog |

## Known duplicates

- **MTD-001 duplicates SG-002** (reward deposit): SG-002 shipped in `DefenseRuntime`
  (TowerUpdateResult -> inventory deposit). MTD-001 is closed by reference; MyTD's gold balance is
  a content-defined resource, not a separate engine concept.

## Notes

- Statuses in `.claude/specs/backlog/*.md` cards may lag reality (SG-001..003 cards
  still said `backlog` after implementation). Close-out now flips card status;
  see `docs/agentic/PIPELINE.md` step 7.
