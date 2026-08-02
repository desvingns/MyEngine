id: ENG-007
title: Multiple spawn points + per-wave routing
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Wave schema gains spawn selection (named ids or all); cross-ref validation against map spawn ids plus per-spawn reachability; fixtures in the content gate.
- DefenseRuntime spawns per routed spawn in deterministic order (sorted spawn id, then index).
- Two-spawn replay hash test; save roundtrip mid multi-spawn wave.

Implementation notes:
- `WaveContent.spawnSelection` is optional and accepts the default/all route or pipe-separated
  named spawn ids; loader validation cross-references map spawn ids and reachability.
- Scheduled, early-called, and incident-triggered waves route through authored `WaveSpawn` entries
  in deterministic sorted spawn-id -> authored-spawn -> instance order. Reserved map spawn ids are
  guarded, and a checked-in multi-spawn fixture covers the content gate.
- `SandboxSaveCodec.SAVE_VERSION` remains 11; no save-version bump or ADR was needed.

Verification notes:
- Runner passed: `.\gradlew.bat test`, `.\gradlew.bat projects`,
  `.\scripts\me-content-validate.ps1` (2 existing packs plus the checked-in multi-spawn fixture),
  `.\scripts\me-sim-replay.ps1`, `.\scripts\me-save-compat.ps1` (v1-v11 matrix plus
  `SandboxMultiSpawnTest`), `.\scripts\me-benchmark.ps1`, `.\gradlew.bat :android:assembleDebug`,
  and `git diff --check`.
- Canonical replay hashes are `e4892bcc18f9d8dc` and `a763da4ac32b15b4`. Benchmark metrics:
  `sim_ms=364`, `kill_sim_ms=87`, `goal_field_rebuild_ns=9048200`, `spatial_index_1k_ms=5.7197`.
- Tester remediation focused tests: 59 passed. Simulation, save-compat, and verifier reviews
  passed; the only low finding is the pre-existing Gradle 10 deprecation warning. No device or
  emulator claim is made beyond `assembleDebug`.
