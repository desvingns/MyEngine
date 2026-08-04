id: PROC-004
title: Numeric performance budgets for the benchmark gate
status: done
owner: codex
blocked_by: none
phase: process
source: architecture review 2026-07-04 (P3.1)
start_gates:
  - selfcheck
  - benchmark_contract_tests
  - benchmark

Context: scripts/me-benchmark.ps1 emits metrics but has no thresholds, so the perf
gate cannot fail objectively. Telemetry has sim_ms/frame_ms fields that stay null.

Acceptance:
- Budgets (sim_ms per tick, frame_ms, per scenario) live in a versioned config file.
- me-benchmark.ps1 compares metrics to budgets and returns pass/fail + deltas in its JSON.
- me-verifier checks the benchmark verdict when perf-relevant paths changed.
- Telemetry events carry the measured sim_ms/frame_ms so retro can trend them.

## Implementation

- Added versioned `config/performance-budgets.v1.json` thresholds for canonical/kill per-tick
  simulation, goal-field rebuild, spatial-index-1k, belt-transport-100, and optional frame timing.
- `scripts/me-benchmark.ps1` now supports deterministic report fixtures, validates required metrics,
  emits one JSON verdict with deltas, and fails on over-budget or malformed reports.
- `scripts/me-record-run.ps1` records numeric `sim_ms` and `frame_ms`; the pre-push lane includes
  the objective benchmark gate and the canonical verifier rule checks its verdict.

## Verification

- Contract fixtures, full Gradle tests/projects, content validation, replay, save compatibility,
  benchmark, pre-push, selfcheck, required headless inspect, Android `assembleDebug`, and
  `git diff --check` passed.
- JVM benchmark frame timing remains explicitly `not_measured`; supplied frame values are budgeted.
