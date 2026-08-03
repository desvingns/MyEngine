id: DX-006
title: Engine cookbook (agent task recipes)
status: done
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `docs/COOKBOOK.md` recipes with exact file lists + gates: add a tower type; add a content field (loader + validation + schema doc + fixture); add a save field (codec bump + migration + PROC-007 matrix); add a tick-loop system; add a snapshot field.
- Each recipe validated against one real historical diff (commit cited).
- Loaded on demand from AGENTS.md intake (not always-loaded, token economy).

Completed: 2026-08-03
Result: Added `docs/COOKBOOK.md` with five scoped recipes, exact file lists, required gates, and
historical commit references (`492fb03`, `3fceabf`, `d281fed`, `e0c600e`, and `270e667`). Added an on-demand
AGENTS.md intake link without expanding the always-loaded checklist. No ADR or human gate was needed.
