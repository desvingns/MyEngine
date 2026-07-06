id: ENG-021
title: Save slots + autosave policy
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Named slots + rotating autosave (cadence config-driven); atomic write with fallback to the last good autosave on corruption.
- Slot metadata (map id, wave, content version, timestamp) readable without a full load.
- Codec version handled per the PROC-007 matrix (migration test if bumped); Android lifecycle save keeps the existing path.
- Tests: slot isolation, rotation determinism, corrupted-file fallback.
