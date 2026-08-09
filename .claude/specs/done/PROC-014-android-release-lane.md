id: PROC-014
title: Android release build lane
status: done
owner: codex
blocked_by: none
start_gates:
  - baseline_selfcheck_pass
  - android_project_buildable
  - release_variant_policy_defined
phase: process
source: engine gap sweep 2026-07-06 (project review; games ship exclusively on Android)

Acceptance:
- `bundleRelease` works: signing via untracked keystore properties; R8 rules keep sim + content loading intact, proven by a release-variant scenario smoke test.
- versionCode/versionName automation policy + per-game applicationId strategy documented.
- Release AAB size + cold-start metrics recorded (feeds PROC-004/PROC-010).

Implementation:
- `android/build.gradle.kts` reads ignored keystore properties, validates release signing before
  packaging, enables R8/resource shrinking, and accepts version/application-id overrides.
- `android/proguard-rules.pro` keeps Android, simulation, content, render, and sandbox entry points;
  `scripts/me-android-release.ps1` validates AAB content, runs signed release APK cold starts, and
  writes `proc-014-android-release-v1` JSON reports without emitting secrets.
- `docs/ANDROID_RELEASE.md` defines the signing, version, application-id, R8, and measurement policy.

Verification (2026-08-09):
- Signed `bundleRelease`/`assembleRelease` passed using an ignored local properties file and the
  existing local debug keystore; AAB size was 697589 bytes and required sandbox assets were present.
- Pixel_5 release content smoke passed with cold-start samples 995/656/642 ms (min 642, median 656,
  max 995); SurfaceView was present after each start.
- Release contract, full Gradle test/projects, content validation, replay, save compatibility,
  benchmark, selfcheck, headless inspect, Android assembleDebug, and diff-check passed.
