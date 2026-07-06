id: ENG-013
title: Tower sell/refund
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `SellTowerCommand` refunds a content-defined ratio (validated 0..1) of cumulative spend (base + tiers), frees occupancy, deterministic.
- Goal-field invalidation on sell (ENG-002 interplay) is tested.
- Sell mid-run replay hash test; a pending sell command survives save (pending-queue persistence).
