id: ENG-010
title: Status effects framework
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content-defined effects (slow + DoT minimum): magnitude, duration ticks, stacking rule (refresh/stack/ignore); content-validate fixtures.
- Deterministic apply/expire ordering (sorted entity id, effect id); movement/damage consume the modifiers.
- Effects persist in save (codec bump + migration test); snapshot exposes active-effect tags.
- Slow-tower scenario replay hash test.

Close-out (2026-08-01):
- Added optional `effects.properties` definitions for slow and DoT effects with validated
  magnitude, duration, stacking rule, and tower references.
- Added generic deterministic entity status-effect state, DoT/slow lifecycle in `engine-defense`,
  sorted snapshot/render tags, and `SandboxSaveCodec` v8 -> v9 migration with active-effect
  roundtrip.
- Added focused loader, defense, save, snapshot/render, slow replay, and overflow-boundary tests.
- Full tests, projects, content validation, replay, save-compat, benchmark, Android assemble, and
  `git diff --check` passed. No default balance values or Android simulation logic changed.
