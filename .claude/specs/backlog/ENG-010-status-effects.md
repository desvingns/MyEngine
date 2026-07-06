id: ENG-010
title: Status effects framework
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content-defined effects (slow + DoT minimum): magnitude, duration ticks, stacking rule (refresh/stack/ignore); content-validate fixtures.
- Deterministic apply/expire ordering (sorted entity id, effect id); movement/damage consume the modifiers.
- Effects persist in save (codec bump + migration test); snapshot exposes active-effect tags.
- Slow-tower scenario replay hash test.
