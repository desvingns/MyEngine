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

Performance gates should measure simulation tick time, allocation pressure, content validation
time, save/load time, and Android frame pacing. Early benchmarks are advisory; release gates become
strict only after the vertical slice exists.

## Android Smoke And Device Tests

Android checks should verify startup, lifecycle pause/resume, input forwarding, save directory
access, orientation handling if supported, and basic frame pacing. They belong in Android modules
or device scripts, not simulation modules.

## Visual And Screenshot Gates

Rendering changes need screenshot or pixel-smoke checks for camera framing, debug overlays, UI
scaling, and text overlap. Visual tests observe snapshots and fixture content.

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

