# Port from mp (twin pipeline): incremental change-log cursor (sync-state.json pattern)

- status: queued
- origin: mobile-pipeline `.ai/changes/sync-state.json` + `lib/sync.sh` contract
  (see mp AGENTS.md "Change-log + incremental sync"; TWINS.md row
  "agent-skill-log + sync-state.json cursor <-> me .ai/changes (NO cursor yet)")
- staged by: /brain sync-twins (first twin port), 2026-07-05

## Problem
MyEngine's `.ai/changes/agent-skill-log.md` is append-only (good), but every consumer
(adapter selfcheck, retro tooling, a future me sync step) must re-read the WHOLE log
each time — there is no cursor marking "everything up to here is already propagated".
mp solved this with `.ai/changes/sync-state.json`: per-consumer cursors so sync reads
only the diff.

## Evidence
- mp: `.ai/changes/sync-state.json` + `lib/sync.sh` is "the consumer/propagator and
  the only writer of the cursor" (mp AGENTS.md); memory: `change-log-discipline.md`.
- me: `scripts/me-selfcheck.ps1` validates adapters against canon at intake with no
  incremental state; as the log grows this is O(log) every session.

## Proposed change (semantics, not syntax — me side stays PowerShell)
1. Add `.ai/changes/sync-state.json`: `{ "consumers": { "selfcheck": "<last-entry-id>" } }`.
2. `scripts/me-selfcheck.ps1` reads the cursor, validates only adapters touched by
   entries AFTER it, and advances the cursor on success (sole writer).
3. Document the cursor in `docs/agentic/SELF_IMPROVEMENT.md` (Scripts section).

## Target files
`scripts/me-selfcheck.ps1`, `.ai/changes/sync-state.json` (new),
`docs/agentic/SELF_IMPROVEMENT.md`.

## Expected effect
Intake selfcheck cost stays O(new entries) instead of O(whole log); same discipline
as the twin pipeline, easing future mp<->me ports.

## Risks
Low. Cursor desync -> selfcheck can always fall back to a full pass (`-Full` switch).
Rollback: delete the cursor read (keep full pass), file remains inert.

## Required human decision
Apply via `/me --improve` (or `--improve --drain`) — do NOT apply directly.
