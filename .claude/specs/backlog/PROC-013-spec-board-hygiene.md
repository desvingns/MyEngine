id: PROC-013
title: Spec board hygiene
status: backlog
phase: process
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Verified-done cards (SG-001..005, MTD-001..003) move to `.claude/specs/done/`; ENGINE_ROADMAP statuses reconciled.
- A small script reports card-status vs ENGINE_ROADMAP mismatches as one JSON line; wired into me-selfcheck or the pre-push lane.
- Run after the in-flight MTD-003 close-out work is committed, so moves do not mix with uncommitted edits.
