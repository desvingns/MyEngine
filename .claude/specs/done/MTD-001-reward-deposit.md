id: MTD-001
title: Defense reward deposit into gold
status: done
phase: first-game
source: D:/Pet/MyTD/spec (EG-001, FR-006, AC-003)

Acceptance:
- `DefenseRuntime.updateTowers` deposits `enemy.rewardAmount` into a player gold balance on kill.
- Replaces the current no-op reward branch.
- Deposit is deterministic and covered by a replay/unit test.
- Simulation stays Android/render-free.

Resolution:
- Closed as a duplicate of completed `SG-002` (2026-07-04).
- `DefenseRuntime.updateTowers` returns content-derived kill rewards
  (`enemy.rewardResource`/`enemy.rewardAmount`); `SandboxRuntime.step` deposits them into
  `state.inventory` in deterministic sorted-resource order.
- MyTD's "gold" balance maps to the content-defined reward/cost resource id. A MyTD content pack
  should define that resource as `gold`; no engine hardcoding is required.
- Covered by `DefenseRuntimeTest`, `SandboxRewardDepositTest`, and kill-bearing replay/save tests
  in `SandboxVerticalSliceTest`.
