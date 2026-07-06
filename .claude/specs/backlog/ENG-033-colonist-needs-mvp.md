id: ENG-033
title: Colonist needs MVP (hunger/rest)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content-defined needs with decay rates + thresholds (validated); threshold crossings enqueue eat/sleep jobs.
- Need-vs-work priority arbitration is deterministic (tested).
- Need levels in save (migration test) and snapshot (HUD bars); 10k-tick determinism soak.
