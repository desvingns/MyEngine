id: MTD-002
title: Tower placement gold-cost gating
status: done
phase: first-game
source: D:/Pet/MyTD/spec (EG-002, FR-005, AC-001, AC-002)

Acceptance:
- `placeTower` checks affordability against the player gold balance.
- Rejects placement when unaffordable; gold is unchanged on rejection.
- Spends `tower.cost` on success.
- Gold never goes negative (NFR-006); covered by unit tests.

Resolution:
- Completed by the existing generic tower placement path plus acceptance coverage added
  2026-07-05.
- `SandboxRuntime.buildTower` gates `BuildTowerCommand` with
  `state.inventory.canRemove(tower.costResource, tower.costAmount)`, calls
  `DefenseRuntime.placeTower` only when affordable, and removes the cost only after
  `TowerPlacementResult.Placed`.
- MyTD's "gold" is represented by content data (`tower.costResource=gold` in the future MyTD pack),
  so the reusable engine remains resource-agnostic.
- `SandboxTowerCostGatingTest` covers affordable spend, unaffordable rejection with unchanged
  non-negative balance, placement rejection without spend, and replay-stable rejection.
