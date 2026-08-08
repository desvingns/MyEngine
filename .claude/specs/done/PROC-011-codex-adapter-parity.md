id: PROC-011
title: Codex adapter parity audit + selfcheck coverage
status: done
owner: codex
blocked_by: none
phase: process

start_gates:
  - baseline_selfcheck_pass
  - codex_adapter_manifests_parse
  - archive_baseline_excluded
source: engine gap sweep 2026-07-06 (project review; adapters already exist — this is audit + drift coverage)

Acceptance:
- Audit confirms `codex-plugins/me-dev` + `codex-plugins/me-spec` adapters resolve canonical docs and expose the same modes as the claude-plugins counterparts.
- `scripts/me-selfcheck.ps1` drift check extended to codex-plugins + .codex registration; fails on divergence.
- One documented smoke run of a /me mode under Codex CLI recorded in `.ai/runs`.

Completed: 2026-08-08

Close-out:
- Added explicit Claude/Codex mode parity coverage for `/me` and `/me-spec` adapters.
- Extended `scripts/me-selfcheck.ps1` to validate Codex manifests, `.codex` registration, and
  the adapter parity contract test.
- Bumped Codex adapter manifests to `me-dev 0.1.2` and `me-spec 0.1.1`.
- Recorded the Codex smoke run in `.ai/runs/2026-08-08-proc-011-codex-smoke.md`.

Verification:
- `powershell.exe -NoProfile -File scripts/tests/me-adapter-parity.tests.ps1` -> pass.
- `powershell.exe -NoProfile -File scripts/me-selfcheck.ps1` -> pass.
- `.\gradlew.bat test`, `.\gradlew.bat projects`, and `git diff --check` -> pass.
- No runtime, Android, save, replay, or content behavior changed.
