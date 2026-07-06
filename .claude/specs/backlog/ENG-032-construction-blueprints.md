id: ENG-032
title: Construction system (blueprint, haul, build)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Blueprint placed via command with content-defined material costs; hauling (ENG-004) delivers; a build job applies work ticks; the completed building spawns.
- Blueprint blocking behavior pinned by test (non-blocking until built, or a content flag).
- Cancel refunds delivered materials deterministically; save mid-construction roundtrips; replay hash scenario.
