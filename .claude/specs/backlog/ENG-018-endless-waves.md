id: ENG-018
title: Endless wave generation
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content-defined endless params (composition cycle, hp/count/reward growth); no hardcoded curve in engine code.
- Generation consumes the sim RNG stream (no fresh RNG); wave-N composition reproducible in isolation and in replay.
- Endless packs declare no win condition (ENG-014 interplay); scaling table dumpable via devtools for PROC-008 consumption.
