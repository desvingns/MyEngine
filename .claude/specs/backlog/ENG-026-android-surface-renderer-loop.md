id: ENG-026
title: Android SurfaceView renderer + Choreographer fixed-tick loop
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `MyEngineActivity` replaces the TextView with a SurfaceView/Canvas drawing `RenderFrame` (tiles, towers with tier, enemies, core) via `RenderPalette`; ASCII path retained for tests. Builds on MTD-005 (which stays game/snapshot-side).
- Choreographer loop with fixed-tick accumulator: sim advances at the canonical tick rate regardless of frame rate; render reads the latest immutable snapshot only.
- MotionEvent tap/pan/pinch wired into the existing `InputAdapter`; commands enter the sim only via the command queue.
- Lifecycle: onPause stops ticking and saves via the existing codec (pending commands included); onResume restores with an identical replay hash — robolectric/instrumentation smoke.
- `android:assembleDebug` passes; unblocks PROC-009 (visual smoke gate).
