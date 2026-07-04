id: MTD-001
title: Defense reward deposit into gold
status: backlog
phase: first-game
source: D:/Pet/MyTD/spec (EG-001, FR-006, AC-003)

Acceptance:
- `DefenseRuntime.updateTowers` deposits `enemy.rewardAmount` into a player gold balance on kill.
- Replaces the current no-op reward branch.
- Deposit is deterministic and covered by a replay/unit test.
- Simulation stays Android/render-free.
