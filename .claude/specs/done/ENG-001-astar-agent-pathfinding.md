id: ENG-001
title: A* point-to-point pathfinding for agents
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review; wave enemies use the ENG-002 goal field)

Acceptance:
- Deterministic A* in engine-world (integer costs, open-set tiebreak by (f, tile index)); differential test vs BFS on uniform-cost maps.
- Stored agent paths invalidated by world changes trigger a deterministic repath.
- Used by Movement for job actors (ENG-003/ENG-004); wave enemies stay on the ENG-002 field.
- Worst-case 64x64 search benchmarked (PROC-004 feed).

Completion: 2026-08-02

Implementation summary: Added `engine-world` `AStarPathfinder` for deterministic 4-neighbor
uniform integer-cost A* with `(f, row-major tile index)` open-set ordering, stable neighbor order,
first predecessor on equal `g`, bounds/blocked/no-path handling, and optional occupied-start
support. `engine-ai` preserves the `PathRequest`/`PathResult` API through A* and adds deterministic
`AgentPathPlanner` repaths for valid stored `MovementComponent` paths after route/world changes.
Full Movement/job tick integration is intentionally deferred to ENG-003/ENG-004; wave enemies
remain on ENG-002 `GoalField`.

Decisions: No ADR. No save, content, render, Android, dependency, or game-bundle traceability
change was made.

Verification: Focused `AStarPathfindingTest` and `AgentPathPlannerTest` pass; full
`.\gradlew.bat test`, `.\gradlew.bat projects`, content validation, sim-replay, save-compat,
benchmark, and `git diff --check` pass. Replay hashes are `e4892bcc18f9d8dc`,
`a763da4ac32b15b4`, and `3f02607020d48668`. Benchmark: canonical 413 ms, kill 102 ms,
GoalField rebuild 13099400 ns, spatial index 6.3748 ms. Final verifier passed with no findings and
all boundary checks true; tie/equal-g and `pathIndex` findings were remediated.
