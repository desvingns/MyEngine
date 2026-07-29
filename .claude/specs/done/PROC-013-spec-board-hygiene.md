id: PROC-013
title: Spec board hygiene
status: done
phase: process
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Verified-done cards (SG-001..005, MTD-001..003) move to `.claude/specs/done/`; ENGINE_ROADMAP statuses reconciled.
- A small script reports card-status vs ENGINE_ROADMAP mismatches as one JSON line; wired into me-selfcheck or the pre-push lane.
- Run after the in-flight MTD-003 close-out work is committed, so moves do not mix with uncommitted edits.

## Completion

- Variant B completed 2026-07-29: migrated 23 verified-done cards from `backlog/` to `done/`.
- Added the read-only board checker and wired it into `me-selfcheck`; the checker emits one JSON
  result and uses exit 0 for a clean board or exit 1 for mismatches.
- Developer, tester, runner, and verifier passes were recorded; no ADR was needed.
