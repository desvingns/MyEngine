id: MTD-004
title: Difficulty modifier application
status: backlog
phase: first-game
source: D:/Pet/MyTD/spec (EG-004, FR-012, AC-006)

Acceptance:
- easy/normal/hard multipliers resolve over enemy health, count, reward, and gold rate.
- Applied deterministically at scenario/content setup, before simulation runs.
- Modifier values come only from difficulty content data; no engine-logic branching per mode.
- Same seed + difficulty reproduces the same final hash; covered by tests.
