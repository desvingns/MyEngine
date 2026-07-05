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

