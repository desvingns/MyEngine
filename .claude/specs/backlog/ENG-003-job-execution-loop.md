id: ENG-003
title: Job execution system (wire JobBoard into the tick loop)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review; JobBoard exists but is never called from the tick loop)

Acceptance:
- Tick loop performs assignment: JobBoard claims by priority with deterministic worker order (sorted entity id); lifecycle claimed -> in-progress -> done/failed advances in-sim.
- JobActor executes move-to (pathfinding), work ticks, completion effects; invalid-target interruption releases the job deterministically.
- Job board + in-flight jobs persist in save (codec bump + migration test).
- Two-workers-competing replay hash scenario; existing JobBoard unit tests extended to integration.
