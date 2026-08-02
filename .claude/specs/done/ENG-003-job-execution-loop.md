id: ENG-003
title: Job execution system (wire JobBoard into the tick loop)
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review; JobBoard exists but is never called from the tick loop)

Acceptance:
- Tick loop performs assignment: JobBoard claims by priority with deterministic worker order (sorted entity id); lifecycle claimed -> in-progress -> done/failed advances in-sim.
- JobActor executes move-to (pathfinding), work ticks, completion effects; invalid-target interruption releases the job deterministically.
- Job board + in-flight jobs persist in save (codec bump + migration test).
- Two-workers-competing replay hash scenario; existing JobBoard unit tests extended to integration.

Implementation decisions (human-approved 2026-08-02):
- Worker discovery: every entity with `JobActorComponent` and a position is eligible for all job
  types; one work tick is processed per simulation tick.
- Target validation: a job target is valid when its `TilePosition` is inside the world; the
  existing pathfinder determines reachability, and no-path is treated as an interruption.
- Invalid target/no-path policy: return the job to `OPEN`, clear worker assignment/reservation and
  movement, and prohibit reclaiming the same job in the same tick.
- Completion effects: use a typed `resource_delta` effect (`resourceId` + amount) delivered through
  a deterministic sink; sandbox applies resource deltas in sorted resource-id order.

Completion: 2026-08-02

Implementation summary: Wired the Android-free `JobExecutionSystem` into the sandbox fixed-tick
pipeline. Workers are discovered from positioned `JobActorComponent` entities, jobs are claimed in
deterministic worker/entity and priority/job-id order, and the lifecycle advances through
`CLAIMED -> IN_PROGRESS -> DONE/FAILED`. Movement uses the existing agent path planner, work ticks
and typed `resource_delta` completion effects are persisted/applied deterministically, and invalid
targets or no-path interruptions release assignment/reservation without same-tick reclaim. The
authoritative `JobBoard`, in-flight jobs, actor assignment/progress, and effects are persisted in
`SandboxSaveCodec` v13 with v12 migration to empty job state.

Decisions: No ADR, game-bundle traceability update, or plugin/skill/pipeline contract change was
needed. The approved defaults remain: every positioned `JobActorComponent` is eligible for all job
types, one work tick is processed per simulation tick, in-world `TilePosition` is the target-validity
check, and invalid/no-path jobs return to `OPEN` after deterministic release.

Verification: Selfcheck, full tests/projects/content validation/replay/save-compat/benchmark,
`android:assembleDebug`, and `git diff --check` passed. Focused `JobExecutionSystemTest` and
`SandboxJobExecutionTest` passed; replay hashes are `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, and
`3f02607020d48668`. Final benchmark: simulation 430 ms, kill 76 ms, spatial index 6.6127 ms;
the verifier passed with all boundary checks true. Non-blocking follow-ups: the two-worker replay
is not in `DevtoolReports.replayInspect`, `me-save-compat.ps1` does not separately invoke
`SandboxJobExecutionTest`, and no job-heavy benchmark covers large worker/job counts or invalidated
paths.
