id: PROC-009
title: Android visual smoke gate (screenshot vs golden)
status: backlog
owner: codex
blocked_by: PROC-012
start_gates:
  - baseline_selfcheck_pass
  - android_renderframe_available
  - emulator_lane_pass
phase: process
source: architecture review 2026-07-04 (P4.2)
blocked_by: PROC-012

Context: desktop has a pixel-smoke test; Android still renders ASCII. Once Android
consumes RenderFrame, a device/emulator screenshot gate (mp-fit analogue) catches
visual regressions. MTD-005 delivered the RenderFrame wiring; PROC-012 is the remaining
emulator-lane prerequisite.

Acceptance:
- A script captures an emulator screenshot of a canonical scene and compares it to a
  golden image with a tolerance; emits one JSON line.
- Runs as an optional gate in --feature runs that touch android/** or engine-render/**.
- Golden updates require an explicit handoff note.
