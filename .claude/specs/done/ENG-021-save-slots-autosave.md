id: ENG-021
title: Save slots + autosave policy
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Named slots + rotating autosave (cadence config-driven); atomic write with fallback to the last good autosave on corruption.
- Slot metadata (map id, wave, content version, timestamp) readable without a full load.
- Codec version handled per the PROC-007 matrix (migration test if bumped); Android lifecycle save keeps the existing path.
- Tests: slot isolation, rotation determinism, corrupted-file fallback.

Close-out (2026-08-02): Implemented named slots under the separate `slots/` namespace and
config-driven rotating autosaves under `autosave/`. Writes use a flushed temporary file and
`ATOMIC_MOVE` with no non-atomic fallback. Slot metadata is inspectable without decoding the
authoritative state, and fallback is limited to corruption, selecting the latest good autosave.
`SandboxSaveCodec.SAVE_VERSION` remains 10 and the Android Bundle lifecycle path is unchanged.
No ADR was needed.
