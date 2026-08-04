id: PROC-001
title: Automated spec back-sync for external game repos
status: done
owner: Codex / me-dev:me
blocked_by: none
phase: process
source: architecture review 2026-07-04 (P2.3)
start_gates:
  - selfcheck
  - sync_contract_tests

Context: PIPELINE.md step 7 now requires close-out to sync a completed gap's status
back into the source game bundle (engine-gap-analysis.md, traceability.csv). For
external repos (e.g. D:/Pet/MyTD/spec) this is manual and easy to miss.

Acceptance:
- A deterministic script (e.g. scripts/me-spec-sync.ps1) reads a completed backlog card,
  locates its `source:` bundle (in-repo or external path), and reports which gap/traceability
  rows are stale; optionally applies the status flip.
- Emits one JSON line (runner convention).
- ENGINE_ROADMAP.md and card statuses stay consistent after a /me close-out.

Completed: 2026-08-04

Result: `scripts/me-spec-sync.ps1` resolves relative or external source bundles, reports stale
targeted `EG-*` rows in one JSON result, and applies only those rows with explicit `-Apply`.
External writes additionally require `-AllowExternalWrite`; contract tests cover report-only,
targeted apply, quoted CSV, schema/card errors, path resolution, and exit codes.
