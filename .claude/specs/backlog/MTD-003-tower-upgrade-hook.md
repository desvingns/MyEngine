id: MTD-003
title: Tower upgrade hook
status: backlog
phase: first-game
source: D:/Pet/MyTD/spec (EG-003, FR-010, FR-011, FR-015, AC-004, AC-007)

Acceptance:
- Applying an upgrade tier mutates the tower's `AttackComponent` (range/damage/cooldown).
- Spends gold; rejects when unaffordable; gold unchanged on rejection.
- Persists branch + tier on the tower so save/load and replay restore exact stats.
- Covered by unit + save-roundtrip tests.
