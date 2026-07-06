id: ENG-009
title: Splash damage + shot events
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content: optional splashRadius + falloff per tower; validation rejects invalid values.
- Deterministic AoE resolution (stable target order, integer math rule pinned by test).
- Snapshot carries shot/hit events (source, target, tick) so render can animate projectiles; sim stays render-free; event log determinism test.
- Golden replay hashes re-baselined via handoff note; balance report covers splash towers.
