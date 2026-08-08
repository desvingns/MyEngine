id: PROC-008
title: Headless playtest bot for balance tuning
status: backlog
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
