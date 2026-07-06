id: ENG-016
title: Incident execution pipeline + RNG fix
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review; SandboxGame.kt:143 allocates IncidentDirector per tick with fresh SeededRandom(17), result discarded)

Acceptance:
- Selection moves off the per-tick fresh `SeededRandom(17)` onto the sim RNG stream, on a content-defined cadence (pacing window + cooldowns); the dead per-tick call is removed.
- Selected incidents EXECUTE via a content-declared effect interpreter (spawn wave, resource event, modifier).
- Full replay-hash coverage of selection + effects; no-incident packs remain valid (content fixture).
- Cooldown/pacing state persists in save (codec bump + migration).
