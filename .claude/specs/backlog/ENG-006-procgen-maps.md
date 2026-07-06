id: ENG-006
title: Seeded procedural map generation
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Deterministic generator: map seed + content-defined params produce an ENG-005 MapDefinition; same seed yields an identical map (hash test).
- Generated maps are always valid: spawn->core connectivity guaranteed or deterministically regenerated (bounded attempts).
- Devtools dumps a generated map as ASCII (DX-002 tie-in); replays embed the map seed.
