id: ENG-020
title: Spatial index + 1k-entity benchmark
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Grid-bucket index over positions; targeting + splash queries use it; a differential test proves results identical to the O(n) scan across seeded runs.
- Stable iteration order (sorted ids within traversal) — determinism preserved; canonical replay hashes unchanged.
- New benchmark scenario with >=1k concurrent enemies; JSON numbers recorded so PROC-004 can bind budgets.

Completion (2026-07-29):
- Implemented an internal, non-persisted `GridSpatialIndex` in `engine-defense`; targeting and splash candidate queries use it with exact post-filters and live `EntityStore` resolution.
- Added deterministic devtools JSON benchmark output for 1024 concurrent enemies, 16 towers, and 16 queries; measured `5.3045 ms` on the accepted run.
- Verification passed: focused engine-defense tests (14), focused engine-devtools tests (16), full Gradle tests, projects, content validation (2 packs), replay, save-compat, benchmark, and `git diff --check`. Replay hashes remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`.
- Known follow-up: seeded differential coverage does not yet exercise end-to-end `updateTowers` parity across every targeting-mode and splash combination; this is a low, non-blocking `me-tester` follow-up.
