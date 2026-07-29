id: SG-004
title: Android save smoke
status: done
phase: first-game

Acceptance:
- Android shell can trigger save/load in debug. [DONE 2026-07-04]
- Lifecycle pause does not corrupt simulation state. [DONE 2026-07-04; strengthened 2026-07-05 —
  save-format v2 persists the runtime's pending CommandQueue, so this now holds at ANY tick, not
  just a quiescent one. See STATE.md / .ai/handoff.md "SG-004 follow-up".]
- Exact device blocker is documented if no device is available. [DONE 2026-07-04 — on-device Bundle
  round-trip is device-pending; see STATE.md Known Blockers.]

Closed 2026-07-05 (follow-up: sandbox save-format v2, SAVE_VERSION 1->2, pending CommandQueue
persistence). All three acceptance criteria satisfied per me-verifier. Non-blocking low-severity
follow-ups tracked in STATE.md/.ai/handoff.md (delimiter-escaping across the codec's properties
encodings; CommandId-issuance persistence if a real command-submitting UI is added; off-main-thread
save encode; device/emulator round-trip run).
