id: ENG-029
title: Audio event hooks (snapshot event feed)
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Deterministic gameplay event feed on the snapshot (shot, hit, death, wave-start, build, sell) with tick + type; same seed produces the same event log (test). Shares the ENG-009 event plumbing.
- Content maps event types to sound refs; validation checks files exist.
- Android playback (SoundPool) consumes the feed; sim has no audio dependency; volume/mute is presentation state only.

Close-out (2026-08-02): Implemented the transient immutable `GameplayEvent` feed on the latest
completed snapshot tick for shot, hit, death, wave-start, build, and sell events, with deterministic
tick/ordinal ordering. Optional `sounds.properties` maps event ids to pack-relative files; validation
rejects unknown or duplicate ids, missing files, and paths escaping the pack root. Android's
`SoundPoolPresentationConsumer` consumes the feed with presentation-only volume/mute state. No
`SandboxSaveCodec.SAVE_VERSION` bump or authoritative state/hash change was made.

Verification: selfcheck, 160 focused core/content/sandbox/Android tests, full `./gradlew.bat test`
and `./gradlew.bat projects`, content validation for 2 packs, replay, save-compat v10 matrix,
benchmark, `android:assembleDebug`, and `git diff --check` passed. Replay hashes are canonical
`e4892bcc18f9d8dc` and kill `a763da4ac32b15b4`; benchmark is `sim=412 ms`, `kill=83 ms`,
`spatial-index-1k=5.62 ms`, `goal-field=9947100 ns`. The initial invalid `:android:test --tests`
invocation was corrected to `:android:testDebugUnitTest --tests` and is not a feature failure.

Manual limitations: no real device/emulator SoundPool playback, volume/mute, or frame-metrics
evidence was available. The Android reviewer contract was unavailable; local Android/content/
simulation boundary and gate evidence passed.
