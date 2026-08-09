# android Contract

Status: Draft  
Owner: Android shipping shell

## Responsibilities

- Android application entry point.
- Lifecycle handling.
- Touch/gesture input forwarding.
- Save directory and permissions boundary.
- Frame pacing and platform configuration.

## Non-Responsibilities

- Authoritative simulation logic, content validation rules, core persistence model, or desktop tools.

## Dependencies

- May depend on game modules, `engine-render`, and approved Android/libGDX backend dependencies.
- Simulation modules must not depend on `android`.

## Public Contracts

- Activity/application shell.
- Input forwarding adapter.
- Android save-location adapter.
- Android smoke test entry points.

## Test Gates

- Startup smoke test.
- Pause/resume smoke test.
  - SG-004 (2026-07-04): the device-independent proof (save-at-pause == uninterrupted run to the
    same tick, seed roundtrip, versioned-save rejection of future/non-numeric versions) is JVM-covered
    by `games/sandbox/.../SandboxSessionLifecycleTest.kt` against the Android-free `SandboxSession`
    holder. The real on-device Bundle round-trip (`onSaveInstanceState` outState ->
    `onCreate` savedInstanceState under config-change/process-death) is DEVICE-PENDING: no connected
    Android device/emulator is available in this environment, so the instrumented pause/resume +
    save-directory-access smoke cannot be executed here. `android:assembleDebug` is the best available
    static gate (proves the `MyEngineActivity` + Bundle wiring compiles/links).
- Input-to-command mapping test.
- Save directory access test.

The Windows device-lane entry point is `powershell.exe -File scripts/me-android-device-smoke.ps1`.
It runs the AndroidX instrumentation smoke against the scripted `Pixel_5` AVD and emits one compact
JSON result. Missing SDK/emulator/system-image/virtualization is reported as typed `blocked` with a
fallback rather than a false pass; `scripts/me-android-device-smoke-test.ps1` covers that contract.
This lane is device evidence only and does not move simulation authority into Android.

## Visual smoke contract

`AndroidContentPackMaterializer` materializes packaged sandbox assets into an app-private directory
so the existing path-based content boundary remains intact on Android. Visual smoke enables the
activity extra for a bounded 15-second capture window and renders the deterministic tick-0 frame.
The PowerShell lane uses adb `screencap -p`/pull and a System.Drawing comparator. Its canonical
fixture is Pixel_5 portrait, default sandbox, seed 7, tick 0; app-window semantics ignore the top
80 and bottom 120 pixels. Per-channel tolerance is 8 and the allowed difference ratio is 0.005.
Golden updates require an explicit reason. A typed `blocked` preflight result is never considered
a pass. The script contract is covered by `scripts/tests/me-android-visual-smoke.tests.ps1`.
