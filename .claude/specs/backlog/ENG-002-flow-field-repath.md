id: ENG-002
title: Goal-field pathfinding + repath on world change (mazing)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- A single goal field (BFS/Dijkstra from core) replaces per-enemy precomputed paths for wave enemies; deterministic neighbor/tie order pinned by test.
- Walkability changes (place/destroy tower or wall) rebuild the field in the same tick; enemies reroute mid-run — mazing scenario replay-hash test.
- Placement that would leave any spawn without a path to core is rejected deterministically (path-block check).
- Save mid-reroute roundtrips identically (save-compat gate); 64x64 field rebuild cost benchmarked (feeds PROC-004).
