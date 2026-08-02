id: ENG-011
title: Enemy armor + damage types
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Damage types on towers, armor/resist table on enemies, cross-ref validated in both directions with fixtures.
- Damage formula documented in `docs/contracts/defense.md`; integer rounding pinned by test.
- Balance report shows effective DPS per tower vs enemy; resist scenario replay hash test.

Close-out (2026-08-02):
- Approved scope: Option A — additive static damage types and percentage resistances; legacy packs remain valid.
- Content contract: `DamageTypeContent` and `ContentRegistry.damageTypes`; nullable
  `TowerContent.damageTypeId`; `EnemyContent.resists` from `resist.<damageTypeId>=<percent>`;
  resistance range `0..100`; deterministic bidirectional reference validation including orphan
  declared types, with actionable file/id/field diagnostics.
- Runtime contract: `DamageFormula` evaluates
  `floor(baseDamage * max(0,100 - distance*falloffPercent) * (100-resistPercent) / 10000)`
  with `Long` intermediates and one final floor. Direct and splash damage use the same formula;
  zero damage changes no health and emits no `HitEvent`. Upgrade tiers inherit the tower damage type.
- Balance contract: effective-DPS matrix covers base towers and upgrade tiers against every enemy,
  sorted by tower profile id then enemy id, under single-target, in-range, no-splash assumptions and
  `ticks_per_second=20`.
- Persistence: `SandboxSaveCodec.SAVE_VERSION=11` is unchanged; damage types and resistances are
  registry-derived metadata and are not persisted.
- Evidence: focused ENG-011 suite — 24 tests passed after the `Int.MAX_VALUE` Long-intermediate
  boundary test; full runner gates passed for tests, projects, content validation, replay,
  save-compat, benchmark, Android assemble, and diff-check. Conditional simulation and balance
  reviewers passed; the simulation review's low overflow-test finding was resolved by that boundary
  test. No device/emulator evidence is claimed.
- Replay goldens: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`, resist
  `3f02607020d48668`.
- Final benchmark: `sim=422 ms`, `kill=63 ms`, `goal-field=10743500 ns`,
  `spatial-index=6.4719 ms`.
- Next: `ENG-019` (walls + player-placed blockers), selected as the next remaining TD-depth item
  with demand 2 after ENG-011; no implementation blocker remains.
