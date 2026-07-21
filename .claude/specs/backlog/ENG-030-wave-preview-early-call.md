id: ENG-030
title: Wave preview + early wave call
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Snapshot HUD block exposes next-wave composition + countdown ticks.
- `CallWaveEarly` command starts the next wave immediately with a content-defined bonus; deterministic; rejected while a wave is active (rule tested).
- Early-call-at-fixed-tick replay hash test; save mid-countdown roundtrips.

Implementation:
- Added typed `CallWaveEarlyCommand`, deterministic next-wave composition/countdown projection,
  and optional content-defined `WaveEarlyCallBonus(resourceId, amount)` validation.
- Accepted early calls spawn the next wave once and deposit the configured bonus; calls at/after
  the scheduled tick or while a wave is active are deterministic no-ops.
- `SandboxSaveCodec` is v8 with typed pending-command decode and v1-v7 migration coverage.

Verification:
- Full Gradle tests, content validation (2 packs), replay, save-compat, benchmark, and
  `android:assembleDebug` passed with the JDK 17 fallback.
- Replay hashes: canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`.
- Benchmark: canonical `473 ms`, kill `78 ms`, goal-field rebuild `8222800 ns`.
- Renderer, simulation, save, Android, and final verifier reviews passed; exact scheduled-tick
  rejection and mid-countdown save roundtrip are covered. The balance review returned partial:
  current content packs are valid and contain no hardcoded bonus; its schema-documentation gap was
  closed in this docs close-out, and the optional bonus remains unconfigured pending an approved
  balance value.

Decisions and risks:
- Variant A was approved: active-wave rejection is derived from live enemies; no ADR is needed.
- Existing packs intentionally do not configure an early-call bonus because no balance value was
  approved; the data-driven path is covered by synthetic tests.
- Low non-blocking follow-ups remain for per-snapshot HUD allocation/device profiling, and the
  existing save delimiter assumption remains documented without a code change in close-out.
