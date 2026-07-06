id: ENG-015
title: Game speed control (presentation-side)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Pacing 0 (pause) / 1x / 2x / 4x implemented as a ticks-per-frame budget in the loop; sim tick semantics untouched.
- Test: same seed + commands at 1x and 4x produce an identical per-tick hash trajectory.
- Speed is presentation state only — never enters sim state or the run save.
- Surfaced via the ENG-027 HUD; the ENG-026 loop honors pacing including background pause.
