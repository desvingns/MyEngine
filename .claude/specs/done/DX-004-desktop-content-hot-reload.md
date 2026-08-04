id: DX-004
title: Content hot-reload in the desktop launcher
status: done
owner: codex
blocked_by: none
phase: dx
source: engine gap sweep 2026-07-06 (project review)

start_gates:
  - baseline_replay_pass
  - documented_archive_baseline_excluded
  - desktop_smoke_pass

Acceptance:
- DesktopLauncher watches the pack dir; on change it re-validates, then deterministically restarts the scenario with new content + the same seed (no mid-run mutation — sim purity preserved).
- Validation errors surface without crashing; the last-good pack keeps running.
- Reload under 2s on the sample pack; the balance-iteration workflow is documented.

Completed: 2026-08-04
Close-out owner: Codex / me-dev:me
Result: Added the Android-free desktop hot-reload session and recursive WatchService. Content
changes are debounced, validated, and restarted as a new canonical scenario with the same seed;
invalid or partially-written packs report typed errors and preserve the last-good scenario. The
launcher supports opt-in `--watch`, `--pack`, and `--seed` arguments while preserving one-shot
`desktop:run` behavior. Added focused session/watcher tests and desktop balance-iteration docs.
Decision: The watcher swaps only between complete scenario runs; no mid-tick content mutation,
save-schema change, Android change, or new engine runtime API was added.
Verification: Focused/full Gradle tests, `projects`, `desktop:run`, content validation, replay,
save-compat, benchmark, selfcheck, required headless inspect, Android `assembleDebug`, and
`git diff --check` passed. Sample reload measured below 2 seconds.
