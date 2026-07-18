id: MTD-003
title: Tower upgrade hook
status: done
phase: first-game
source: D:/Pet/MyTD/spec (EG-003, FR-010, FR-011, FR-015, AC-004, AC-007)

Acceptance:
- Applying an upgrade tier mutates the tower's `AttackComponent` (range/damage/cooldown).
- Spends gold; rejects when unaffordable; gold unchanged on rejection.
- Persists branch + tier on the tower so save/load and replay restore exact stats.
- Covered by unit + save-roundtrip tests.

Resolution (2026-07-05):
- Added content-defined tower upgrade tiers in `towers.properties` as
  `<tower>.upgrade.<branch>.<tier>.(range|damage|cooldownTicks|costResource|costAmount)`.
- Added `UpgradeTowerCommand`; `SandboxRuntime` applies only legal transitions
  (unupgraded -> tier 1, then same-branch next tier), mutates `AttackComponent`, and spends only
  after affordability checks.
- `SandboxSaveCodec` v3 persists tower upgrade branch+tier markers and pending upgrade commands;
  v1/v2 saves with no branch+tier decode as unupgraded towers.
- Covered by `SandboxTowerUpgradeTest`, content loader validation tests, and full gates.
