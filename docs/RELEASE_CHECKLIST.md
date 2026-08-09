# MyEngine Release Checklist

Status: PROC-014 accepted
Last updated: 2026-08-09

## v0.1 Checklist

- Version bump in Gradle metadata.
- `.\gradlew.bat test`
- `scripts/me-content-validate.ps1`
- `scripts/me-sim-replay.ps1`
- `scripts/me-save-compat.ps1`
- `scripts/me-benchmark.ps1`
- `.\gradlew.bat desktop:run`
- `.\gradlew.bat android:assembleDebug`
- `powershell.exe -File scripts/me-android-release.ps1 -KeystorePropertiesPath keystore.properties`
- Update `STATE.md`.
- Update `.ai/handoff.md`.
- Update `Plane/README.md`.
- Review `docs/HARDENING_AUDIT.md`.
- Record known issues.

## Quality Bar

- Replay pass rate: 100% for committed scenarios.
- Content validation: clean.
- Save compatibility: v1 roundtrip clean.
- Headless sim: `scripts/me-benchmark.ps1` passes the versioned `performance-budgets-v1`
  thresholds and records canonical `sim_ms` plus any supplied `frame_ms`.
- Android assemble: pass.
- Release lane: signed `bundleRelease`/`assembleRelease`, R8 content smoke, AAB byte size, and
  cold-start samples are recorded in the `proc-014-android-release-v1` report.
- Frame budget: the JVM gate reports `not_measured` until a real renderer/device feed exists;
  supplied frame measurements are checked against the 16.67ms budget.
