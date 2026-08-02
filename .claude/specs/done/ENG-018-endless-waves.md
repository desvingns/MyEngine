id: ENG-018
title: Endless wave generation
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content-defined endless params (composition cycle, hp/count/reward growth); no hardcoded curve in engine code.
- Generation consumes the sim RNG stream (no fresh RNG); wave-N composition reproducible in isolation and in replay.
- Endless packs declare no win condition (ENG-014 interplay); scaling table dumpable via devtools for PROC-008 consumption.

completion: 2026-08-02
verification:
- Added validated `endless.properties` content with composition cycle, interval, and count/health/reward growth.
- Generation consumes the shared simulation RNG, preserves deterministic IDs/order, and keeps finite waves compatible.
- Endless schedules remain `NO_WIN`; `endless-scaling` emits deterministic machine-readable rows for tooling.
- Full tests/projects, content validation, replay, save-compat matrix, benchmark, and `git diff --check` passed.
- Focused generator, loader, defense, sandbox save/replay, and devtools report tests passed.
decisions:
- No ADR; the feature is additive, Android-free, and keeps `SandboxSaveCodec.SAVE_VERSION` at 11.
- Generated effective enemy state is retained on entities so mid-wave save/restore does not fall back to base rewards.
known_limitations:
- Conditional simulation reviewer and final verifier agent threads timed out; local boundary review plus full gates passed.
