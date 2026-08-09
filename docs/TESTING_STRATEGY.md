# MyEngine Testing Strategy

Status: Draft accepted for Phase 03  
Last updated: 2026-07-02

Testing is part of the engine contract. A feature is not done until the narrowest useful gate exists
and the result is recorded in the phase handoff.

## Current Commands

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat projects
.\gradlew.bat test
.\gradlew.bat desktop:run
.\gradlew.bat android:assembleDebug
```

On Unix-like shells, use `./gradlew` and set equivalent `JAVA_HOME` / `ANDROID_HOME` values.

## Unit Tests

Use JVM unit tests for deterministic algorithms, value objects, validation rules, command ordering,
system ordering, world coordinate math, and save serialization boundaries. Unit tests must not
require Android.

## Deterministic Replay Tests

Replay tests run a scenario with:

- fixed seed;
- fixed content pack versions;
- ordered commands;
- fixed tick count;
- stable system ordering;
- replay hash at known checkpoints.

The replay hash should include enough state to detect behavioral drift without storing whole saves
in every assertion.

## Content Schema Tests

Content tests validate sample packs, reject malformed packs, check cross-references, verify
localization keys, and exercise content migrations. These tests live outside Android.

## Save Compatibility Tests

Save tests include:

- v1 roundtrip tests;
- migration from old fixture to current model;
- content-pack mismatch rejection;
- replay hash preservation for stable scenarios;
- missing or unknown field behavior.

## Simulation Property Tests

Property-style tests should start in `engine-testkit` with small deterministic generators before a
new dependency is added. Useful invariants include resource conservation, no negative inventories,
stable ordering under equivalent commands, and bounded incident budgets.

## Benchmark And Performance Tests

Performance gates measure simulation tick time, allocation pressure, content validation time,
save/load time, and Android frame pacing. `scripts/me-benchmark.ps1` evaluates the versioned
`config/performance-budgets.v1.json` thresholds for the canonical, kill, goal-field, spatial-index,
and belt workloads. The JVM benchmark reports `frame_ms` as `not_measured` until a renderer/device
clock is available; any supplied frame value is evaluated against the configured budget.

## Android Smoke And Device Tests

Android checks should verify startup, lifecycle pause/resume, input forwarding, save directory
access, orientation handling if supported, and basic frame pacing. They belong in Android modules
or device scripts, not simulation modules.

The Windows device lane is:

```powershell
powershell.exe -File scripts/me-android-device-smoke.ps1
```

It uses the scripted `Pixel_5` AVD profile, builds the debug and instrumentation APKs, boots or
reuses an emulator, runs the AndroidX instrumentation smoke headlessly, and emits one compact JSON
result. `status: blocked` / `verdict: blocked` is the expected typed fallback when the SDK, system
image, emulator, or virtualization is unavailable; the JVM/build and contract lanes remain usable.
`BootTimeoutSeconds` bounds device readiness polling, and cleanup stops only an emulator started by
the lane. The deterministic contract is checked with:

```powershell
powershell.exe -File scripts/me-android-device-smoke-test.ps1
```

## Visual And Screenshot Gates

Rendering changes need screenshot or pixel-smoke checks for camera framing, debug overlays, UI
scaling, and text overlap. Visual tests observe snapshots and fixture content.

The Android visual gate is:

```powershell
powershell.exe -File scripts/me-android-visual-smoke.ps1 -AvdName Pixel_5
```

Its canonical contract is Pixel_5 portrait, the default sandbox, seed 7, and tick 0. The
comparator applies app-window semantics by ignoring the top 80 and bottom 120 pixels, uses
per-channel tolerance 8, and allows at most 0.005 differing-pixel ratio. Updating the checked-in
golden requires a non-empty explicit reason. Missing SDK/emulator capability is emitted as typed
`blocked` and is not a pass. The deterministic contract test is
`scripts/tests/me-android-visual-smoke.tests.ps1`.

## Done Gates By Change Type

| Change type | Minimum gate |
|---|---|
| core algorithm | JVM unit test |
| command/tick behavior | replay determinism test |
| content definition | schema validation test |
| save/load behavior | roundtrip and migration test |
| Android lifecycle/input | Android smoke test or documented blocker |
| rendering/camera | screenshot or pixel-smoke test |
| dependency addition | ADR-0002 checklist plus boundary test |
