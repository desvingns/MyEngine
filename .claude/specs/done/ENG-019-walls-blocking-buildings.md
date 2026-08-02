id: ENG-019
title: Walls + player-placed blockers
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content type for non-tower buildings (wall MVP): cost, HP, footprint; validation + l10n.
- Place/remove via commands with cost, occupancy, and path-block rejection (ENG-002); refund follows ENG-013 rules.
- Walls have Health, appear in the snapshot, persist in save; forced-corridor mazing replay hash test.

Completion: 2026-08-02

Implementation summary: Added a data-defined 1x1 wall MVP with validated cost, health, footprint,
localization, visual reference, and sell-refund metadata; render-free place/remove commands; atomic
resource and occupancy updates; prospective all-spawn path validation and same-tick rerouting;
immutable snapshot health; and `SandboxSaveCodec` v12 persistence with v1-v11 migration and pending
building-command restoration.

Decisions: No ADR. The MVP is intentionally limited to 1x1 walls. Approved balance is 2 bolt cost,
20 HP, and a 50% sell refund. Tower cost validation remains non-negative for compatibility while
wall cost validation is strictly positive and reports zero-cost content as a structured validation
error.

Verification: Final runner 9/9 passed (tests, projects, content validation, replay, save-compat,
benchmark, Android assemble, and diff-check). Replay hashes are canonical
`e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`, and resist `3f02607020d48668`. Save compatibility
includes the v12 wall fixture with health, occupancy, and a pending place-building command plus
the v1-v11 migration matrix. Benchmark evidence: 64x64 GoalField, 3,844 reachable tiles,
11,813,800 ns rebuild; 1,024 enemies, 16 towers, 16 queries, 16 shots, 5.201 ms spatial-index
run; canonical scenario 35 ticks / 431 ms and kill scenario 35 ticks / 79 ms. Android evidence
is `:android:assembleDebug` only; no device, emulator, visual-golden, or frame-budget claim is made.
