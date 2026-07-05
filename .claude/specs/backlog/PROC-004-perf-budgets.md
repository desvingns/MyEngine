id: PROC-004
title: Numeric performance budgets for the benchmark gate
status: backlog
phase: process
source: architecture review 2026-07-04 (P3.1)

Context: scripts/me-benchmark.ps1 emits metrics but has no thresholds, so the perf
gate cannot fail objectively. Telemetry has sim_ms/frame_ms fields that stay null.

Acceptance:
- Budgets (sim_ms per tick, frame_ms, per scenario) live in a versioned config file.
- me-benchmark.ps1 compares metrics to budgets and returns pass/fail + deltas in its JSON.
- me-verifier checks the benchmark verdict when perf-relevant paths changed.
- Telemetry events carry the measured sim_ms/frame_ms so retro can trend them.
