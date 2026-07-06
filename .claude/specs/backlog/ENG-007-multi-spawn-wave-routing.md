id: ENG-007
title: Multiple spawn points + per-wave routing
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Wave schema gains spawn selection (named ids or all); cross-ref validation against map spawn ids plus per-spawn reachability; fixtures in the content gate.
- DefenseRuntime spawns per routed spawn in deterministic order (sorted spawn id, then index).
- Two-spawn replay hash test; save roundtrip mid multi-spawn wave.
