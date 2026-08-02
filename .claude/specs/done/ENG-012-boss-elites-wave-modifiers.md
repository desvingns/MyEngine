id: ENG-012
title: Boss/elite enemies + wave modifiers
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content: elite/boss flags with stat scaling plus a wave modifier list (hp%, speed%, count); validation + fixtures; no per-game engine branching.
- Deterministic modifier application at spawn; snapshot marks bosses for render emphasis.
- Replay hash + save roundtrip mid-boss-wave; reward scaling visible in the balance report.

## Completion (2026-08-02)

- Added optional data-defined elite/boss flags and validated health/speed/reward scaling on
  `EnemyContent`.
- Added indexed wave modifiers with deterministic consecutive-enemy coverage, effective spawn
  stats, immutable boss snapshot/render markers, and effective balance metrics.
- Added `EnemyComponent` persistence and `SandboxSaveCodec` v11 with v1-v10 migration fallback,
  replay/save continuation coverage, content validation fixtures, and balance/render tests.

Verification: full Gradle tests/projects, content validation (2 packs), replay, save-compat v1-v11
matrix, benchmark, Android assemble, and `git diff --check` passed. Canonical replay hashes remain
`e4892bcc18f9d8dc` and `a763da4ac32b15b4`.
