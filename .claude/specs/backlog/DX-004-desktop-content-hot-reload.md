id: DX-004
title: Content hot-reload in the desktop launcher
status: backlog
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- DesktopLauncher watches the pack dir; on change it re-validates, then deterministically restarts the scenario with new content + the same seed (no mid-run mutation — sim purity preserved).
- Validation errors surface without crashing; the last-good pack keeps running.
- Reload under 2s on the sample pack; the balance-iteration workflow is documented.
