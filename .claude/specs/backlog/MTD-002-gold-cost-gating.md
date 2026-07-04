id: MTD-002
title: Tower placement gold-cost gating
status: backlog
phase: first-game
source: D:/Pet/MyTD/spec (EG-002, FR-005, AC-001, AC-002)

Acceptance:
- `placeTower` checks affordability against the player gold balance.
- Rejects placement when unaffordable; gold is unchanged on rejection.
- Spends `tower.cost` on success.
- Gold never goes negative (NFR-006); covered by unit tests.
