id: ENG-014
title: Win/lose conditions + run summary
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content/scenario-defined end conditions: lose on core-HP/leak-budget exhaustion; win on final wave cleared; endless packs may declare no win condition (ENG-018 interplay).
- Terminal sim state: gameplay commands are rejected after the run ends; same seed produces the same terminal tick + reason (replay hash test).
- Run summary (waves, kills, leaks, resources, ticks) exposed on `EngineSnapshot`; no render types in simulation.
- Save/load of a finished run roundtrips the summary (save-compat gate).

Implementation:
- `maps.json` now accepts optional `terminalRules`: finite waves win after all configured waves
  spawn and enemies clear; `no_win`/`endless` maps stay active. Core-health exhaustion always loses,
  and an optional positive leak budget adds an earlier deterministic loss.
- `RunState`/immutable `RunSummary` carry terminal status, reason, tick, and waves/kills/leaks/
  resources/ticks. `EngineSnapshot` exposes a render-safe projection; terminal summaries freeze.
- `SandboxSaveCodec` v5 persists terminal state and summary, migrating v1-v4 saves as active runs.

Verification:
- Serial full suite: `.\\gradlew.bat test --no-daemon --max-workers=1` -> pass. Serial execution was
  required only after prior parallel native-memory exhaustion, not an assertion failure.
- `scripts\\me-content-validate.ps1` -> pass (`validated 2 pack(s)`); replay -> pass (canonical
  `9c495d8ff30fd83d`, kill `83a65da1a7881b2c`); save-compat, benchmark, and
  `.\\gradlew.bat android:assembleDebug` -> pass.
