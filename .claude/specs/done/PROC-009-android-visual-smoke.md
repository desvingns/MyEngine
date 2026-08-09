id: PROC-009
title: Android visual smoke gate (screenshot vs golden)
status: done
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

Implementation:
- `AndroidContentPackMaterializer` copies packaged sandbox assets into an app-private,
  path-based content boundary; `MyEngineActivity` accepts the visual-smoke extra and renders
  the deterministic tick-0 frame during a bounded 15-second smoke window.
- `scripts/me-android-visual-smoke.ps1` drives the Pixel_5 AVD through adb, waits for the
  activity and SurfaceView, captures PNG with `screencap -p`, pulls it, and compares it with
  the golden using System.Drawing.
- `scripts/tests/me-android-visual-smoke.tests.ps1` covers the deterministic script contract,
  blocked preflight, explicit golden-update reason, capture/comparison wiring, and defaults.

Visual contract:
- Device: Pixel_5 portrait; content: default sandbox; seed: 7; simulation tick: 0.
- Screenshot semantics are app-window-only via the comparator ignoring the top 80 and bottom
  120 pixels (system chrome boundary).
- Per-channel tolerance is 8; allowed differing-pixel ratio is 0.005.
- Golden replacement requires a non-empty explicit reason in the handoff. A typed `blocked`
  result, including missing-SDK preflight, is not a pass.

Verification (2026-08-09):
- Pixel_5 visual smoke ran twice and passed with difference ratios 0.0007069 and 0.0004784.
- Focused Android gates, full `test`/`projects`, content validation, replay, save compatibility,
  benchmark, selfcheck, required headless inspect, and `git diff --check` passed.
- PROC-012 contract remains typed `blocked` for missing-SDK preflight. Android performance review
  and `me-verifier` finished with a pass; all five boundary checks are true. Low-severity profiling/
  lifecycle and future large-pack materialization follow-ups remain non-blocking.
