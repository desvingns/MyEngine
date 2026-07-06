id: ENG-001
title: A* point-to-point pathfinding for agents
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review; wave enemies use the ENG-002 goal field)

Acceptance:
- Deterministic A* in engine-world (integer costs, open-set tiebreak by (f, tile index)); differential test vs BFS on uniform-cost maps.
- Stored agent paths invalidated by world changes trigger a deterministic repath.
- Used by Movement for job actors (ENG-003/ENG-004); wave enemies stay on the ENG-002 field.
- Worst-case 64x64 search benchmarked (PROC-004 feed).
