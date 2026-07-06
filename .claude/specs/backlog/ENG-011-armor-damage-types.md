id: ENG-011
title: Enemy armor + damage types
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Damage types on towers, armor/resist table on enemies, cross-ref validated in both directions with fixtures.
- Damage formula documented in `docs/contracts/defense.md`; integer rounding pinned by test.
- Balance report shows effective DPS per tower vs enemy; resist scenario replay hash test.
