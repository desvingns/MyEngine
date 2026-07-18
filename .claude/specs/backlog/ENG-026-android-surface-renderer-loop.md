id: ENG-026
title: Android SurfaceView renderer + Choreographer fixed-tick loop
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `MyEngineActivity` replaces the TextView with a SurfaceView/Canvas drawing `RenderFrame` (tiles, towers with tier, enemies, core) via `RenderPalette`; ASCII path retained for tests. Builds on MTD-005 (which stays game/snapshot-side).
- Choreographer loop with fixed-tick accumulator: sim advances at the canonical tick rate regardless of frame rate; render reads the latest immutable snapshot only.
- MotionEvent tap/pan/pinch wired into the existing `InputAdapter`; commands enter the sim only via the command queue.
- Lifecycle: onPause stops ticking and saves via the existing codec (pending commands included); onResume restores with an identical replay hash — robolectric/instrumentation smoke.
- `android:assembleDebug` passes; unblocks PROC-009 (visual smoke gate).

Implementation (2026-07-16):
- `MyEngineActivity` now runs an Android-local 20 Hz fixed-tick policy through a
  Choreographer-backed `TickScheduler`. The `SurfaceView` draws the latest immutable
  `RenderFrame`; it does not own authoritative simulation state.
- Tap/pan/pinch remain presentation input through `InputAdapter`. Input only enqueues commands;
  command IDs remain Activity-owned.
- `onPause` cancels ticking and saves through the existing codec, including pending commands.
  Bundle restoration resumes with the preserved next command ID and replay continuity.

Verification:
- `:android:testDebugUnitTest --tests dev.myengine.android.FixedTickFrameLoopTest --rerun-tasks` -> pass.
- `:android:assembleDebug` -> pass.
- Replay gate -> pass: canonical `9c495d8ff30fd83d`; kill `83a65da1a7881b2c`.
- Save-compat gate -> pass.
- `me-tester` reported no test-file changes; `me-verifier` -> pass. The Android performance review
  is partial only for the manual checks below.

Manual device / performance checks still pending:
- On a device or emulator, verify tap, pan, pinch, and pause/recreate with a pending command while
  confirming the next command ID and replay hash continuity.
- Capture FrameMetrics/JankStats and Allocation Tracker evidence before making a frame-budget or
  smoothness claim.
