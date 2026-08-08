id: PROC-012
title: Emulator provisioning lane (Gradle managed devices)
status: done
owner: codex
blocked_by: none
start_gates:
  - baseline_selfcheck_pass
  - android_project_buildable
  - virtualization_or_documented_fallback
phase: process
source: engine gap sweep 2026-07-06 (project review; unblocks the SG-004 device-pending acceptance)

Acceptance:
- Gradle managed device (or scripted AVD) definition; one command boots the emulator and runs the Android instrumentation smoke headless, emitting one JSON line.
- Consumed by the PROC-009 screenshot gate and device-lane reruns of the SG-004 save smoke.
- Windows dev box supported; documented fallback when virtualization is unavailable.

Implementation notes:
- `scripts/me-android-device-smoke.ps1` provides the Windows PowerShell lane with a scripted `Pixel_5` AVD, bounded `adb get-state`/boot polling, APK installation, instrumentation execution, owned-emulator cleanup, and one compact JSON result.
- `scripts/me-android-device-smoke-test.ps1` covers deterministic blocked-preflight output, instrumentation discovery, manifest wiring, and Gradle test dependencies.
- Missing SDK, emulator, system image, or virtualization is reported as typed `status: blocked` / `verdict: blocked` with a fallback; JVM/build/contract lanes remain usable.

Verification notes:
- Selfcheck and PROC-012 contract test passed; full Gradle `test`/`projects`, content validation, replay, save compatibility, benchmark (`sim_ms=538`, `frame_ms=not_measured`), headless inspect, `assembleDebug`, and `assembleDebugAndroidTest` passed.
- Actual instrumentation passed on `Pixel_5` / `emulator-5554`; Android reviewer re-review passed after bounded polling replaced the unbounded `adb wait-for-device` path.
- No screenshot golden or frame-metrics claim is made; `me-verifier` timed out twice and the local Android boundary review found no violation. The pre-existing untracked `archive/` baseline was preserved and excluded.
