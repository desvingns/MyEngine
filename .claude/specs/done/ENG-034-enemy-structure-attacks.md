id: ENG-034
title: Enemy attacks on structures
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content-flagged behavior: blocked enemies damage the blocking wall/tower (deterministic target pick) instead of idling.
- Destruction frees occupancy and invalidates the goal field (ENG-002) — breach-and-reroute replay hash test.
- Damaged buildings persist in save; structure damage stats appear in the balance report.
