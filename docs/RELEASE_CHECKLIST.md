# MyEngine Release Checklist

Status: Phase 14 accepted  
Last updated: 2026-07-02

## v0.1 Checklist

- Version bump in Gradle metadata.
- `.\gradlew.bat test`
- `scripts/me-content-validate.ps1`
- `scripts/me-sim-replay.ps1`
- `scripts/me-save-compat.ps1`
- `scripts/me-benchmark.ps1`
- `.\gradlew.bat desktop:run`
- `.\gradlew.bat android:assembleDebug`
- Update `STATE.md`.
- Update `.ai/handoff.md`.
- Update `Plane/README.md`.
- Review `docs/HARDENING_AUDIT.md`.
- Record known issues.

## Quality Bar

- Replay pass rate: 100% for committed scenarios.
- Content validation: clean.
- Save compatibility: v1 roundtrip clean.
- Headless sim: advisory `sim_ms` recorded.
- Android assemble: pass.
- Frame budget: pending real renderer.
