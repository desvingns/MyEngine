id: ENG-030
title: Wave preview + early wave call
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Snapshot HUD block exposes next-wave composition + countdown ticks.
- `CallWaveEarly` command starts the next wave immediately with a content-defined bonus; deterministic; rejected while a wave is active (rule tested).
- Early-call-at-fixed-tick replay hash test; save mid-countdown roundtrips.
