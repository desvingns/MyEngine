id: DX-005
title: Schema-docs drift gate
status: done
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Script diffs keys parsed by ContentLoader vs keys documented in `docs/content-schemas/PROPERTIES_SCHEMA.md`; fails with an actionable list in both directions (code-not-doc, doc-not-code).
- One JSON line output (runner rule); wired into the PROC-006 pre-push lane and me-selfcheck.
- Fixtures prove both failure directions.

Completed: 2026-08-03
Result: Added the deterministic `me-schema-docs-drift.ps1` gate, aligned the properties schema
documentation for status effects and legacy indexed fields, added bidirectional drift fixtures,
and wired the gate into selfcheck and the full PROC-006 pre-push lane. No engine/runtime/save or
Android behavior changed.
