id: PROC-008
title: Headless playtest bot for balance tuning
status: done
owner: codex
blocked_by: none
start_gates:
  - baseline_selfcheck_pass
  - baseline_replay_pass
  - baseline_content_validate
phase: process
source: architecture review 2026-07-04 (P4.1)

Context: TD/colony balance needs empirical curves, not spot metrics. me-balance-simulator
currently reads one benchmark report.

Acceptance:
- A deterministic headless bot plays scripted strategies over seeded scenarios
  (N seeds per config) and reports win-rate, leak counts, and difficulty curve per wave.
- Output is one JSON report consumed by me-balance-simulator to propose content-only tuning.
- Fully deterministic given seeds (replayable), engine code untouched by proposals.

Completed: 2026-08-09

Close-out:
- Added `engine-devtools` `PlaytestBot` with the built-in `no-build`, `spawn-tower`, and
  `late-tower` strategies. Each strategy runs the same contiguous seed range and bounded tick
  budget, reporting typed `won`/`lost`/`timeout` outcomes, win rate, leaks, core damage, and
  per-run metrics.
- Added versioned `proc-008-playtest-v1` JSON output with a stable content-derived wave curve
  for enemy count, total health, and total reward. The CLI is available as `playtest` and
  `playtest-bot`; no wall-clock values or proposal mutation are included.
- The implementation remains in `engine-devtools`; sandbox runtime, save format, replay goldens,
  Android, and authored content are unchanged.

Verification:
- `PlaytestBotTest` covers byte-identical repeatability, strategy/seed cardinality, wave curve
  semantics, and bounded timeout behavior.
- Full `:engine-devtools:test`, `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation,
  replay goldens, save-compatibility, benchmark (`performance-budgets-v1`), required headless
  inspect, `:android:assembleDebug`, `me-selfcheck`, and `git diff --check` passed.
- Roster scout/architect/developer/verifier workers timed out after bounded waits; local contract
  and boundary review supplied the final evidence. No ADR or conditional domain reviewer was
  needed because only `engine-devtools` and its tests changed.
