id: DX-005
title: Schema-docs drift gate
status: backlog
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Script diffs keys parsed by ContentLoader vs keys documented in `docs/content-schemas/PROPERTIES_SCHEMA.md`; fails with an actionable list in both directions (code-not-doc, doc-not-code).
- One JSON line output (runner rule); wired into the PROC-006 pre-push lane and me-selfcheck.
- Fixtures prove both failure directions.
