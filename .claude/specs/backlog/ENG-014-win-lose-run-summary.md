id: ENG-014
title: Win/lose conditions + run summary
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content/scenario-defined end conditions: lose on core-HP/leak-budget exhaustion; win on final wave cleared; endless packs may declare no win condition (ENG-018 interplay).
- Terminal sim state: gameplay commands are rejected after the run ends; same seed produces the same terminal tick + reason (replay hash test).
- Run summary (waves, kills, leaks, resources, ticks) exposed on `EngineSnapshot`; no render types in simulation.
- Save/load of a finished run roundtrips the summary (save-compat gate).
