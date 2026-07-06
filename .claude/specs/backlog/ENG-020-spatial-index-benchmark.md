id: ENG-020
title: Spatial index + 1k-entity benchmark
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Grid-bucket index over positions; targeting + splash queries use it; a differential test proves results identical to the O(n) scan across seeded runs.
- Stable iteration order (sorted ids within traversal) — determinism preserved; canonical replay hashes unchanged.
- New benchmark scenario with >=1k concurrent enemies; JSON numbers recorded so PROC-004 can bind budgets.
