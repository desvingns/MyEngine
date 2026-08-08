# PROC-011 Codex smoke run

- Date: 2026-08-08
- Host: Codex desktop using the repo-local `me-dev:me` adapter
- Invocation: `[$me-dev:me] --feature --next`
- Selected card: `PROC-011` (Codex adapter parity + selfcheck coverage)
- Smoke checks:
  - `powershell.exe -NoProfile -File scripts/tests/me-adapter-parity.tests.ps1` -> pass
  - `powershell.exe -NoProfile -File scripts/me-selfcheck.ps1` -> pass
- Result: Codex `/me` intake and the adapter parity contract completed without changing runtime,
  Android, save, replay, or content behavior.
