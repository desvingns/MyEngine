id: MTD-004
title: Difficulty modifier application
status: done
phase: first-game
source: D:/Pet/MyTD/spec (EG-004, FR-012, AC-006)

Acceptance:
- easy/normal/hard multipliers resolve over enemy health, count, reward, and gold rate.
- Applied deterministically at scenario/content setup, before simulation runs.
- Modifier values come only from difficulty content data; no engine-logic branching per mode.
- Same seed + difficulty reproduces the same final hash; covered by tests.

Resolution (2026-07-16):
- Added `DifficultyContent` and optional `difficulties.properties` fields for `healthMult`,
  `countMult`, `rewardMult`, and `goldRateMult`. MyTD balance-plan source values are easy
  `0.8/0.9/1.2/1.2`, normal `1/1/1/1`, and hard `1.3/1.15/0.9/0.9` (health/count/reward/gold rate).
- Added `ContentRegistry.resolveDifficulty`; `BigDecimal` scaling materializes effective enemy
  health, wave counts, and payout deterministically before the first tick.
- Wired difficulty selection through `SandboxGame` and `SandboxSession`. No save-format, Android,
  or render changes; no ADR required.

Evidence (2026-07-16):
- `.\gradlew.bat :engine-content:test :games:sandbox:test` -> pass; `.\gradlew.bat test` -> pass.
- Content validation -> pass (`validated 2 pack(s)`); replay -> pass
  (`9c495d8ff30fd83d`, `83a65da1a7881b2c`); save-compat -> pass; benchmark -> pass
  (`sim_ms=429` implementation run).
- `me-verifier` -> pass; all boundary checks true.

Follow-up:
- Low, non-blocking: `difficultyId` is not serialized; restore requires the same effective
  difficulty-resolved registry.
