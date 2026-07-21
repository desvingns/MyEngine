id: ENG-015
title: Game speed control (presentation-side)
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)
completed: 2026-07-21

Acceptance:
- Pacing 0 (pause) / 1x / 2x / 4x implemented as a ticks-per-frame budget in the loop; sim tick semantics untouched.
- Test: same seed + commands at 1x and 4x produce an identical per-tick hash trajectory.
- Speed is presentation state only — never enters sim state or the run save.
- Surfaced via the ENG-027 HUD; the ENG-026 loop honors pacing including background pause.

Implementation:
- Added Android-local `PresentationSpeed` modes `0x`, `1x`, `2x`, and `4x`.
- `FixedTickFrameLoop` scales canonical due ticks at the presentation boundary, keeps the 250 ms frame cap, and drops wall-clock accumulation while paused or after lifecycle restart.
- `MyEngineActivity` restores speed separately in `Bundle`; `SandboxSession`, `SandboxSaveCodec`, and `SAVE_VERSION` remain unchanged.
- `SandboxRenderView` exposes four disjoint speed controls with selected-state drawing and callback-only hit testing; speed taps do not create `EngineCommand` values.

Verification:
- Selfcheck: `scripts/me-selfcheck.ps1` -> pass.
- Full `.\gradlew.bat test`, `:android:testDebugUnitTest`, and `:android:assembleDebug` -> pass.
- Content validation, save-compat, and benchmark -> pass; benchmark: canonical `328 ms`, kill `66 ms`, rebuild `4.1305 ms`.
- Replay -> pass; canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`.
- `me-verifier` -> pass; all `boundary_checks` true. Unit coverage includes pacing modes, pause/restart timing, overflow-safe timestamps, per-tick trajectory parity, and speed-layout bounds.

Known follow-ups:
- Device/instrumentation tap, lifecycle/recreation, and Bundle smoke tests remain pending; FrameMetrics/JankStats and Allocation Tracker evidence is also pending.
- Extreme `200x600` portrait layout has a manual risk that the selected panel can overflow below the viewport; compact-width/accessibility/contrast checks remain manual.
- Pre-existing `pausedSave` stale-state rollback risk in `MyEngineActivity.onResume` is outside ENG-015 scope.
- At `0x`, Choreographer remains active for idle HUD redraw; this is an accepted presentation CPU/battery trade-off.

Decision:
- No ADR: presentation-only Android-local state, with no dependency direction or save-schema change.
