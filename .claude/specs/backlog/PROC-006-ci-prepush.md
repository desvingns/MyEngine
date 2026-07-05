id: PROC-006
title: Pre-push hook or minimal CI for the deterministic gates
status: backlog
phase: process
source: architecture review 2026-07-04 (P3.3)

Context: all gates run only when the orchestrator or user remembers to run them.
A hook/CI catches drift between agent runs.

Acceptance:
- Either a git pre-push hook or a CI workflow runs: gradlew test, me-content-validate,
  me-sim-replay (and me-save-compat when save code changed).
- Failure blocks the push / marks the commit red with the single JSON gate outputs attached.
- Setup is documented in STATE.md or docs/.
