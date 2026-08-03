id: DX-002
title: Headless state inspector ("agent eyes")
status: done
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Devtools command runs any registered game/pack scenario to tick N (optional command script) and emits an ASCII frame plus JSON state dump (entities, inventories, defense metrics, hash).
- Byte-identical output for identical args (determinism test); no sandbox-only hardcoding in the inspector path.
- Referenced from AGENTS.md as the default debugging step for agents.

Implementation notes:
- Added `HeadlessScenarioFactory`/`HeadlessStateInspector` with `ServiceLoader` registration and a
  sandbox adapter that owns command parsing and authoritative state projection.
- Added `inspect`, `state-inspect`, and `headless-inspect` CLI aliases with short and generic forms,
  bounded ticks, optional `tick:build_tower:towerId:x:y` scripts, and seed selection.
- Output excludes wall-clock fields and deterministically sorts state collections.

Verification notes:
- Focused inspector tests and confirmed full Gradle tests/projects, content validation, replay,
  save-compat, benchmark, selfcheck, Android `assembleDebug`, and `git diff --check` passed.
- Initial full-test invocation had a transient PowerShell `RemoteException`; the confirmation run
  passed. No device/emulator, visual-golden, or frame-budget evidence is claimed.
