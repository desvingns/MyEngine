id: PROC-009
title: Android visual smoke gate (screenshot vs golden)
status: backlog
phase: process
source: architecture review 2026-07-04 (P4.2)
blocked_by: Android RenderFrame wiring (MTD-005 / SG follow-ups)

Context: desktop has a pixel-smoke test; Android still renders ASCII. Once Android
consumes RenderFrame, a device/emulator screenshot gate (mp-fit analogue) catches
visual regressions.

Acceptance:
- A script captures an emulator screenshot of a canonical scene and compares it to a
  golden image with a tolerance; emits one JSON line.
- Runs as an optional gate in --feature runs that touch android/** or engine-render/**.
- Golden updates require an explicit handoff note.
