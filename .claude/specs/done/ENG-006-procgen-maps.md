id: ENG-006
title: Seeded procedural map generation
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Deterministic generator: map seed + content-defined params produce an ENG-005 MapDefinition; same seed yields an identical map (hash test).
- Generated maps are always valid: spawn->core connectivity guaranteed or deterministically regenerated (bounded attempts).
- Devtools dumps a generated map as ASCII (DX-002 tie-in); replays embed the map seed.

Implementation: `ProceduralMapGenerator` derives typed parameters from validated `MapContent` and
`TileContent`, retries bounded seeded layouts, and falls back to a deterministic corridor. Sandbox
procedural sessions reuse the existing map id/seed save fields; devtools expose `procedural-map
[seed]` (alias `map-generate`) with JSON metadata and ASCII output.

Verification: focused content, sandbox save/reload, and devtools tests; full Gradle tests/projects,
content validation, replay, save compatibility, benchmark, Android `assembleDebug`, selfcheck, and
`git diff --check` passed.
