id: DX-006
title: Engine cookbook (agent task recipes)
status: backlog
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `docs/COOKBOOK.md` recipes with exact file lists + gates: add a tower type; add a content field (loader + validation + schema doc + fixture); add a save field (codec bump + migration + PROC-007 matrix); add a tick-loop system; add a snapshot field.
- Each recipe validated against one real historical diff (commit cited).
- Loaded on demand from AGENTS.md intake (not always-loaded, token economy).
