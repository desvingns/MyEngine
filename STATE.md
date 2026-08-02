# MyEngine State

Last updated: 2026-08-02 (ENG-032 close-out)
Active phase: Phase 00-14 complete; Phase 15 sequencing adopted; Signal Garden SG-001..005 complete; MyTD MTD-001..005 complete; DX-008, ENG-001, ENG-002, ENG-003, ENG-004, ENG-005, ENG-007, ENG-008, ENG-009, ENG-010, ENG-011, ENG-012, ENG-013, ENG-014, ENG-015, ENG-016, ENG-018, ENG-019, ENG-020, ENG-021, ENG-026, ENG-027, ENG-028, ENG-029, ENG-030, ENG-031, PROC-002, PROC-003, PROC-007, and PROC-013 complete; pipeline at v0.2.0; next exact item ENG-032
Owner of last update: Codex (2026-08-02: ENG-032 construction close-out; next candidate ENG-033)

## Current Status

- Phase 00-03 foundation, stack, and architecture contracts are complete.
- Phase 04 agentic pipeline bootstrap is complete.
- Phase 05-10 engine runtime and sandbox vertical slice are complete.
- Phase 11 devtools and editor direction are complete.
- Phase 12 game spec pipeline and sample Signal Garden spec are complete.
- Phase 13 telemetry/retro self-improvement loop is complete.
- Phase 14 hardening, release checklist, API stability, and first-game kickoff are complete.
- PROC-002 is complete through ADR-0004: separate game repositories use a Gradle composite build
  plus a full pinned MyEngine commit SHA; CI checks out and verifies the same revision. Stable APIs
  may be consumed directly, Experimental APIs require a consumer adapter and pin, and Internal APIs
  are not cross-repository dependencies.
- PROC-013 Variant B is complete: the board checker compares card status/location with
  `ENGINE_ROADMAP.md`, emits one JSON result, and is wired into `scripts/me-selfcheck.ps1` with
  exit 0 on pass and exit 1 on mismatch. Twenty-three verified-done cards were migrated to
  `.claude/specs/done/`; no ADR was needed.
- PROC-007 is complete: checked-in v1-v10 sandbox save fixtures, an independent canonical-state
  migration matrix with deterministic stable-hash assertions, and matrix reporting in the save-
  compatibility gate. No production save format or codec version changed.
- ENG-021 is complete: named slots use the separate `slots/` namespace; config-driven rotating
  autosaves use `autosave/`; writes use a flushed temporary file plus `ATOMIC_MOVE` with no
  non-atomic fallback; metadata is readable without a full load; corruption-only fallback selects
  the latest good autosave. `SandboxSaveCodec.SAVE_VERSION` remains 10 and the Android Bundle
  lifecycle path is unchanged. No ADR was needed.
- ENG-029 is complete: the latest completed snapshot tick exposes a transient immutable
  `GameplayEvent` feed for shot, hit, death, wave-start, build, and sell events with deterministic
  tick/ordinal ordering. Optional `sounds.properties` maps event ids to pack-relative files with
  validation; Android `SoundPool` consumes the feed with presentation-only volume/mute state.
  Events do not enter authoritative saves or stable hashes; `SandboxSaveCodec.SAVE_VERSION` remains
  10. No ADR was needed.
- ENG-031 is complete: accepted Option A adds deterministic zone commands/store state, validated
  stockpile resource filters, one-shot harvest-node JobBoard designation jobs, immutable snapshot
  zone projection, and `SandboxSaveCodec` v14 with v1-v13 migration. Colony demand remains
  vision-only; ENG-004 now adds hauling and persisted stockpile contents. Generic stockpile
  capacity, depletion/repeated harvest, and actual Android overlay consumption remain follow-up scope.
- ENG-012 is complete: content-defined elite/boss ranks and health/speed/reward scaling combine with
  indexed wave modifiers at deterministic spawn time. Effective enemy state persists in
  `EnemyComponent`, boss identity crosses the immutable snapshot/render boundary, and
  `SandboxSaveCodec.SAVE_VERSION` is 11 with v1-v10 migration fallback. No ADR was needed.
- ENG-007 is complete: optional `WaveContent.spawnSelection` supports default/all or pipe-separated
  named spawn ids with map cross-reference/reachability validation. Scheduled, early, and incident
  waves route through deterministic sorted spawn-id -> authored `WaveSpawn` -> instance ordering;
  reserved map spawn ids are guarded, and the checked-in multi-spawn fixture covers the content gate.
  `SandboxSaveCodec.SAVE_VERSION` remains 11; no ADR was needed.
- MySD foundation filed ENG-036 for an Android-free reusable runtime/session extraction and
  PROC-015 for a reference-game state-graph/mechanic-claim bridge. Probable gameplay gaps and
  `mysd` demand remain deliberately uncarded until Luna evidence passes Gate 1.
- Signal Garden `SG-001` (content pack) is complete: original pack + loader unit test + gates pass.
- Signal Garden `SG-002` (reward deposit hook) is complete: `DefenseRuntime.updateTowers` returns
  `TowerUpdateResult(metrics, rewards)` accumulating content-derived kill rewards (no Inventory
  mutation in engine-defense); `SandboxRuntime.step` deposits rewards into `state.inventory` in
  sorted-key order guarded by `canAdd`. Tests + gates pass.
- Signal Garden `SG-003` (placeholder render surface) is complete: new
  `engine-render/.../PlaceholderRenderSurface.kt` exposes a PURE
  `project(snapshot: EngineSnapshot, camera: Camera): RenderFrame` (kinds
  `RenderKind{TILE_FLOOR,TILE_WALL,TILE_RESOURCE,CORE,TOWER,ENEMY}`;
  `RenderPrimitive(kind, tile, screen, health?)`; `RenderFrame(primitives, coreHealth, tick)`).
  Tiles emitted first (snapshot order), then entities sorted by id; unknown terrain/entity types
  skipped; screen via `camera.worldToScreen(tile center)`; ENEMY carries health, TOWER null. No
  game/Android imports; no simulation mutation. Reusable, game-agnostic engine capability. Tests +
  gates pass.
- Signal Garden `SG-003 follow-up` (RenderFrame consumed in a real desktop launcher + pixel-smoke)
  is complete: six `RenderKind`s now have visual coverage. NEW pure Android-safe
  `engine-render/.../RenderPalette.kt` (`Rgb(r,g,b).toRgbInt()` + `object RenderPalette`: durable
  shared kind->color mapping — floor/wall/resource/core/tower/enemy + background/coreHealthText/
  enemyPip; NO java.awt/android imports, proven by `android:assembleDebug`). NEW AWT-only
  `desktop/.../FrameRasterizer.kt` (`BufferedImage`/`Graphics2D`/`ImageIO`, antialiasing OFF,
  TYPE_INT_RGB) rasterizes `RenderFrame` -> a 20px cell per primitive centered on the projected
  `ScreenPoint`, a 4px enemy pip drawn ONLY when `RenderPrimitive.health != null`, and a `core <n>`
  readout from `RenderFrame.coreHealth`. `DesktopLauncher` gained a debug render-smoke block
  (project via `PlaceholderRenderSurface`, rasterize, write `desktop/build/render-smoke.png`, print
  `png=<path>`) with the canonical banner/`hash=9c495d8ff30fd83d`/ASCII output preserved and
  unreordered. NEW `desktop/.../FrameRasterizerPixelSmokeTest.kt` (deterministic headless AWT
  pixel-smoke: all six kinds' cell-center pixels == their `RenderPalette` color, enemy-pip present /
  tower-pip absent, core-health text region non-background, bit-for-bit determinism) — closes the
  `docs/contracts/render.md` "Screenshot or pixel-smoke" gate. AWT is confined to the desktop
  harness (`android` -> `:games:sandbox` -> `engine-render`, never `:desktop`). Tests + gates pass.
- Signal Garden `SG-004` (Android lifecycle save smoke) is complete: new pure Android-free
  `games/sandbox/.../SandboxSession.kt` wraps `SandboxRuntime`+seed and exposes `start()`,
  `step()`, `submit()`, `stableHash()`, `save()` (= `SandboxSaveCodec.encode(state, seed)`), and
  `restore(text)` (decode + re-parse seed). QUIESCENT-SAVE precondition documented in KDoc: only
  `state` is persisted, NOT the runtime command queue or the per-tick `SeededRandom(17)`, so `save()`
  is sound at a quiescent tick boundary (`SAVE_VERSION` stays 1; no save-format change; no android
  imports). Thin adapter `android/.../MyEngineActivity.kt`: `onCreate` restores from
  `savedInstanceState["me_sandbox_save"]` under a greppable `DEBUG_SAVE=true` flag (else `start()`),
  preserving the banner + tick + hash TextView and the `runCatching` fold; `onSaveInstanceState`
  writes `session.save()` to the Bundle. No sim logic or content ids in the Activity. Tests
  (`games/sandbox/.../SandboxSessionLifecycleTest.kt`): save/restore roundtrip preserves stableHash;
  pause/resume determinism (resume == uninterrupted run to the same tick); seed roundtrip;
  independent-runtime; plus future-version + non-numeric-version decode rejection (durable
  versioned-save guard). Device-independent proof is JVM-covered; the on-device Bundle round-trip is
  device-pending (see Known Blockers). Tests + `android:assembleDebug` pass.
- Signal Garden `SG-004 follow-up` (sandbox save-format v2 — persist the runtime's pending
  `CommandQueue`, 2026-07-05) is complete: the QUIESCENT-SAVE precondition from the original SG-004
  slice is now DROPPED — `save()` is sound at ANY tick, not only a quiescent one. New
  `CommandQueue.pending(): List<EngineCommand>` (`engine-core/.../Command.kt`) is a non-destructive
  snapshot (`drainFor`/`commandComparator` unchanged). `SandboxRuntime` gained
  `pendingCommands()`/`submitAll()`. `SandboxSaveCodec.SAVE_VERSION` bumped **1 -> 2**: `encode()`
  gained a `pendingCommands` param serialized as a new properties line
  (`type|id|scheduledTick|actorId|stablePayload` per command, `;`-joined); `decode()`'s version guard
  now accepts `1 || 2` (rejects 3+) with its signature/return type (`SandboxState`) UNCHANGED (a real
  v1->v2 migration — v1 saves with no `pendingCommands` property decode cleanly to an empty queue); a
  new separate `decodePendingCommands(text)` reconstructs `BuildTowerCommand` (`type=="build_tower"`)
  or a generic `TextCommand` otherwise. `SandboxSession.save()`/`restore()` (games/sandbox) wired to
  the new codec params; KDoc rewritten to state `save()` is sound at any tick.
  `MyEngineActivity.DEBUG_SAVE` changed from a hardcoded `const val true` to
  `val DEBUG_SAVE = BuildConfig.DEBUG` (`android/build.gradle.kts` gained
  `buildFeatures { buildConfig = true }`) so release builds can no longer ship this enabled — closes
  SG-004 follow-up #2 alongside follow-up #1. No ADR needed (per me-architect: covered by the
  existing "saves are versioned from v1 and migration-aware" invariant; `SandboxSaveCodec` stays
  Experimental per `docs/API_STABILITY.md`, same precedent as the original v1 codec). New/updated
  tests in `SandboxSessionLifecycleTest.kt`: pending future-tick command round-trips through
  save/restore/resume matching an uninterrupted run's stableHash; v1->v2 migration decode test;
  CommandId/Tick preservation test; future-version-rejection retargeted to `saveVersion=3` (v2 is now
  valid); non-numeric-version test retargeted to the new `saveVersion=2` baseline. Pipeline: architect
  `me-architect` (no ADR) -> developer `me-engine-developer` -> tester `me-tester` -> runner
  `me-runner` -> reviewers `me-simulation-reviewer` (pass, 2 non-blocking mediums), `me-save-compat-reviewer`
  (pass, 1 non-blocking low), `me-android-performance` (pass, 1 non-blocking low + 1 pre-existing
  medium) -> `me-verifier` (pass, all four boundary_checks true; confirmed all 3 SG-004 acceptance
  criteria satisfied). Three low-severity, non-blocking follow-ups recorded (see Known Blockers).
- Signal Garden `SG-005` (balance report deltas, 2026-07-05) is complete: `engine-devtools`
  now exposes `DevtoolReports.balanceDeltaReport()` plus CLI aliases `balance-report` and
  `balance-delta`. The default report compares sandbox baseline content to
  `games/signal-garden/content/signal-garden`, stays Android-free/content-driven via
  `ContentPackLoader`, emits machine-readable JSON with threshold metadata, and flags large
  enemy/core/resource deltas. Current default warnings: `enemy_health_total` 32 -> 40,
  `core_damage_potential` 16 -> 8, and `reward_total` 8 -> 16. Tests cover baseline-vs-changed,
  no-op copy/no warnings, large warning categories, invalid changed-pack errors, parser-backed JSON
  structure, and captured CLI stdout. No ADR needed (devtools-only Experimental report; no engine
  runtime or content schema change).
- MyTD `MTD-001` and `MTD-002` (2026-07-05) are complete. `MTD-001` was closed as a duplicate of
  SG-002: MyTD's gold balance maps to content-defined `enemy.rewardResource=gold`, with the existing
  `DefenseRuntime.updateTowers` -> `SandboxRuntime.step` reward deposit path preserving the
  inventory-free defense boundary. `MTD-002` required no production rewrite: `SandboxRuntime.buildTower`
  already gates `BuildTowerCommand` through `tower.costResource`/`tower.costAmount`, calls
  `DefenseRuntime.placeTower` only when affordable, and spends only after
  `TowerPlacementResult.Placed`. New `SandboxTowerCostGatingTest` covers affordable spend,
  unaffordable rejection with unchanged non-negative balance, not-buildable rejection without spend,
  and replay-stable rejection. `.claude/specs/ENGINE_ROADMAP.md`, the MTD backlog cards, and
  `D:/Pet/MyTD/spec` gap/traceability/risk docs are synced. No ADR needed: no new dependency edge,
  no gold-specific engine branch, and no copied reference content.
- MyTD `MTD-003` (tower upgrade hook, 2026-07-05) is complete. `TowerContent` now has optional
  content-defined upgrade tiers parsed from `towers.properties` keys
  `<tower>.upgrade.<branch>.<tier>.(range|damage|cooldownTicks|costResource|costAmount)`, with
  delimiter-safe branch ids and cost-resource validation. New `UpgradeTowerCommand` targets a placed
  tower entity; `SandboxRuntime` applies only legal transitions (unupgraded -> tier 1, then same
  branch/current+1), mutates `AttackComponent(range, damage, cooldownTicks)`, and spends the tier
  resource only after affordability checks. `TowerComponent` persists `upgradeBranch`/`upgradeTier`;
  `SandboxSaveCodec.SAVE_VERSION` is now 3, decodes v1/v2 rows with no branch/tier as unupgraded,
  persists branch+tier on entity rows, and round-trips pending `UpgradeTowerCommand`s. Sandbox sample
  content adds `pulse.upgrade.main.1` as proof content only (not final MyTD balance). New
  `SandboxTowerUpgradeTest` covers stat mutation, spend/reject/no-negative balance, illegal repeated
  tier rejection, v3 save roundtrip, v2-shaped decode, and pending upgrade command lifecycle restore.
  No ADR needed: scoped Experimental content/sandbox/save extension, no new dependency edge, no copied
  reference content. Reviewers pass; one low future refactor remains to eventually move command DTOs
  out of `engine-render` into a neutral command API.
- MyTD `MTD-004` (difficulty modifiers, 2026-07-16) is complete. Added `DifficultyContent` and
  optional `difficulties.properties`; `ContentRegistry.resolveDifficulty` uses deterministic
  `BigDecimal` scaling for enemy health, wave counts, rewards, and gold rate before tick 0. Source
  values are the MyTD balance-plan easy/normal/hard sets (`0.8/0.9/1.2/1.2`, `1/1/1/1`,
  `1.3/1.15/0.9/0.9`). `SandboxGame` and `SandboxSession` wire setup selection. No save-format,
  Android, or render changes; no ADR. Low non-blocking follow-up: `difficultyId` is not serialized,
  so restore requires the same effective difficulty-resolved registry.
- MyEngine `ENG-024` (command DTO relocation and InputAdapter state fix, 2026-07-16) is complete.
  Approved variant A moved command DTOs to `engine-core/.../core/command/TowerCommands.kt` with
  `TileCoordinate`; `InputState` no longer owns `nextCommandId`/`selectedTowerId`, `InputUiState` is
  explicit, `CommandId` is caller-owned, and the sandbox performs boundary conversion. All required
  tests and gates pass; canonical replay hashes remain unchanged.
- DX-008 and MyEngine `ENG-005` (2026-07-16) are complete. ADR-0003 accepts the hybrid content
  format: flat definitions remain `.properties`, while nested maps use additive, Android-compatible
  `maps.json`. The canonical 64x64 sandbox fixture retains spawn `(1,1)`, core `(32,32)`, and the
  `bolt` resource node at `(5,5)=100`; `ContentLoader` validates structural/reachability errors and
  `SandboxGame` materializes the world and routing from the selected map. `SandboxSaveCodec` v4
  persists map id plus content version, validates map/pack/content identity, and migrates v1-v3
  saves through the sole registered map. Full tests, content validation, replay, save-compat, and
  benchmark gates pass; canonical hashes remain `9c495d8ff30fd83d` and `83a65da1a7881b2c`.

- PROC v0.2.0 pipeline improvements (2026-07-04) are complete — the first real pass of the
  improve loop, human-gated via an approved architecture-review plan. Canonical docs
  (`docs/agentic/*`, `docs/GAME_SPEC_PIPELINE.md`) now require: telemetry for EVERY run incl.
  failures (`duration_min`/`malformed_json_count`/`gate_failures`/`attributed_agent` fields),
  reflect after any failed run or every 5th event (`reflect_required` in `me-record-run.ps1`
  output), a proposal queue `.ai/proposals/` + `--improve --drain`, plugin version bumps on
  accepted improvements, selfcheck at intake, a conditional Reviewer Matrix by changed paths,
  the `.ai/DIGEST.md` intake digest, and close-out spec-status/roadmap sync. Roster: all 16
  agents carry `model:` tiers (haiku runner, sonnet reviewers/tester/docs/reflect, inherit for
  architect/developer/verifier/improve/designers); NEW `me-scout` cheap fact-finder. me-spec:
  mandatory gap dedup + demand-counted `.claude/specs/ENGINE_ROADMAP.md` (flags MTD-001 as a
  duplicate of done SG-002). Deferred improvements filed as `PROC-001..010` backlog cards.
  Both plugins 0.1.0 -> 0.2.0 (reinstall from the `myengine` marketplace to refresh the cache).

- MyEngine ENG-014 (win/lose conditions and run summary, 2026-07-16) is complete. Optional map
  terminal rules select finite-wave victory or no-win/endless behavior; terminal RunState freezes an
  immutable summary, EngineSnapshot projects it read-only, and save v5 persists completed runs.
- MyTD MTD-005 (real render surface and touch input, 2026-07-16) is accepted. Android
  `SandboxRenderView` projects immutable snapshots through `PlaceholderRenderSurface` and draws
  tiles, path, core, tower tiers, enemies, and overlay via `Canvas`/`RenderPalette`. `MotionEvent`
  tap, drag-pan, and pinch delegate to `InputAdapter`; the View owns presentation state only, while
  `MyEngineActivity` owns command IDs and the submit -> step -> invalidate callback. This closes the
  backlog acceptance through scoped JVM/build/replay gates, not through an unperformed device smoke.
- MyEngine ENG-026 (Android SurfaceView renderer + Choreographer loop, 2026-07-16) is accepted.
  An Android-local `TickScheduler` applies a 20 Hz Choreographer fixed-tick policy; the `SurfaceView`
  renders the latest immutable `RenderFrame`; and input queues commands only through `InputAdapter`.
  `onPause` cancels ticking and saves pending commands, while Bundle restoration preserves the next
  command ID and replay continuity. JVM/build/replay/save-compat gates pass. Device gesture/lifecycle
  smoke and FrameMetrics/JankStats plus Allocation Tracker profiling remain manual-pending.
- MyEngine ENG-027 (HUD snapshot data + UI command surface, 2026-07-18) is accepted.
  `EngineSnapshot`/`RenderFrame` carry immutable, content-derived HUD data for localized labels,
  resources, wave/countdown/core state, buildable towers, costs, tiers, and placed-tower info.
  Android renders build/select/upgrade panels from that snapshot; 48 dp rows share one pure layout
  model for drawing and hit testing, while build/upgrade taps emit existing commands through
  `InputAdapter` and the caller-owned command queue. Defense records deterministic per-tower actual
  damage and kills; `SandboxSaveCodec` v6 persists those metrics with v1-v5 migration to an empty
  map. Content validation now requires tower/tier `displayKey` references and nine HUD string keys.
  Headless HUD, render/input/layout, defense metrics, content, and save tests pass; all runner gates
  pass with replay hashes unchanged. Device/layout/performance checks remain manual-pending.
- MyEngine ENG-002 (goal-field pathfinding + repath on world change, 2026-07-18) is accepted.
  `GoalField` replaces per-enemy precomputed paths with deterministic core-outward BFS routing;
  `occupied_by_enemy` is surfaced when placement is blocked by an enemy, and all spawns are checked
  prospectively before a world mutation. Tower/wall walkability changes rebuild the field within the
  same tick, so enemies reroute immediately. Save v6 retains canonical authoritative state rather
  than serializing a cache: the field is derived after restore and legacy path state is canonicalized.
  The maze replay golden is `ed0354584405ec49`; canonical and kill hashes are now
  `463d87684ca6cbee` and `40c7bda7e3bc1316`.
- MyEngine ENG-013 (tower sell/refund, 2026-07-18) is accepted. `SellTowerCommand` is a stable
  core command that sells a placed tower at a command boundary. Every tower content definition now
  requires `sellRefundRatio` as a decimal in the inclusive `0..1` range. The runtime reconstructs
  the base and actually applied sequential tier spend, aggregates it by resource, refunds
  `floor(resourceSpend * ratio)` per resource, and first verifies capacity for every refund; a
  capacity rejection leaves inventory, tower, occupancy, and tower metrics unchanged. A successful
  sale clears occupancy, removes the entity and its metrics, then rebuilds the ENG-002 goal field
  before same-tick enemy movement. Pending sell commands round-trip with id, tick, actor, and
  payload; `SandboxSaveCodec.SAVE_VERSION` remains `6`.
- MyEngine ENG-008 (targeting priority modes, 2026-07-18) is accepted. `TargetSelector` is a pure,
  deterministic in-range selector for `FIRST`, `LAST`, `NEAREST`, `STRONGEST`, and `WEAKEST`, with
  entity id as the final tiebreak. Tower content supplies a default (`targetingMode` omitted by a v1
  pack deterministically defaults to `NEAREST`); the queued `SetTowerTargetingModeCommand` applies a
  per-tower override at the runtime command boundary. Immutable HUD tower data exposes the active
  mode. `SandboxSaveCodec` v7 persists tower modes and pending mode-switch commands, and migrates
  v1-v6 saves by resolving the current content default.
- MyEngine ENG-015 (presentation-side game speed control, 2026-07-21) is accepted. Android-local
  `PresentationSpeed` provides `0x`, `1x`, `2x`, and `4x`; `FixedTickFrameLoop` scales due ticks
  without changing authoritative tick semantics, while the HUD exposes callback-only speed controls.
  Speed is restored separately in `Bundle` and does not enter `SandboxSession.save()` or the save
  version. Per-tick trajectory parity, speed layout bounds, pause/restart timing, and overflow-safe
  timestamps are covered; no ADR was needed.
- MyEngine ENG-030 (wave preview + early wave call, 2026-07-21) is accepted. The immutable HUD
  exposes deterministic next-wave composition and countdown; typed `CallWaveEarlyCommand` starts
  the next wave before its scheduled tick and applies an optional content-defined
  `resourceId + amount` bonus. Calls at/after the scheduled boundary or while enemies are active
  are rejected without authoritative mutation. `SandboxSaveCodec` is v8 with typed pending-command
  decode and v1-v7 migration. Full tests, content validation (2 packs), replay, save-compat,
  benchmark, and `android:assembleDebug` pass; canonical/kill hashes are
  `12a65fd2b87593cf`/`bb37eefc1903cc77`; benchmark is `473 ms`/`78 ms`, goal-field rebuild
  `8222800 ns`. No ADR was needed. The balance review returned partial: current content packs are
  valid and contain no hardcoded bonus; its schema-documentation gap was closed in this close-out,
  and the optional bonus remains unconfigured pending an approved balance value.
- MyEngine ENG-028 (sprite/atlas references in content schema, 2026-07-28) is accepted. Optional
  pack-relative sprite or minimal-atlas references are validated for tiles, towers, tower tiers,
  enemies, and minimal building definitions with actionable pack/path/key diagnostics. Opaque refs
  cross the immutable sandbox snapshot into `RenderFrame`; desktop and Android consumers mark
  available assets while omitted/missing refs use deterministic palette fallback. Existing replay
  hashes remain `12a65fd2b87593cf`/`bb37eefc1903cc77`, `SAVE_VERSION` remains `8`, and the full
  tests/content/replay/save-compat/benchmark/Android/desktop gates pass.
- MyEngine ENG-009 (splash damage + shot events, 2026-07-28) is accepted. Optional tower
  `splashRadius`/`falloff` values define stable entity-id-ordered Manhattan AoE under one
  integer-only per-ring damage rule. Immutable `ShotEvent`/`HitEvent` source-target-tick data is
  transient presentation state: it is replaced each completed tick and excluded from saves and the
  stable hash. `SAVE_VERSION` remains `8`; no content-pack balance value was shipped.
- MyEngine ENG-020 (spatial index + 1k-entity benchmark, 2026-07-29) is accepted. An internal,
  non-persisted `GridSpatialIndex` supplies targeting and splash candidates with exact post-filters,
  live `EntityStore` resolution, stable entity-id ordering, and preserved Manhattan semantics.
  Devtools exposes deterministic machine-readable metrics for 1024 concurrent enemies, 16 towers,
  and 16 queries; the accepted run measured `5.3045 ms`.
- MyEngine PROC-003 (domain systems sequencing, 2026-07-29) is done. `Plane/15_domain_systems_sequencing.md`
  records: 15.1 flow-field/pathfinding DONE via ENG-002 (MyTD FR-003/FR-009/FR-013); 15.2 colony
  slice ordered ENG-001 -> ENG-003 -> ENG-031 -> ENG-004 -> ENG-032 with vision-only demand and a
  re-entry trigger (MySD Gate 1 via PROC-015 or an authored colony game spec); 15.3 storyteller =
  ENG-016 (vision-only demand plus defect fix F4). The adopted chain after ENG-020 is ENG-010 ->
  ENG-016 -> PROC-007 -> ENG-021 -> ENG-029 -> ENG-012 -> ENG-007 -> ENG-018, with ENG-010 (status
  effects framework, named MyTD FR-007) as the unique successor. Acceptance criterion (b) was
  amended by owner decision: `vision:*` demand tags count as demand where no named game FR exists
  yet. Roadmap demand tags were corrected: `mytd` removed from ENG-012 (2->1), ENG-021 (4->3),
  ENG-022 (2->1), and ENG-029 (4->3) as unbacked by the MyTD spec bundle.
- MyEngine ENG-001 is complete: `engine-world` provides deterministic 4-neighbor uniform-cost
  integer A* with stable open-set/neighbor/predecessor ordering and boundary handling; `engine-ai`
  preserves the `PathRequest`/`PathResult` API through A* and adds deterministic `AgentPathPlanner`
  repaths for valid stored `MovementComponent` paths. ENG-003 now provides the full JobBoard/job-
  actor tick integration; the first worker hauling MVP remains ENG-004. Wave enemies remain on
  ENG-002 `GoalField`.

## ENG-011 Close-out (2026-08-02)

- DONE: Approved Option A is complete: `DamageTypeContent`, `TowerContent.damageTypeId`, and
  `EnemyContent.resists` are content-driven, resistance values are constrained to `0..100`, and
  bidirectional damage-type validation covers tower references, enemy resistance references, and
  orphan declared types with actionable diagnostics.
- RUNTIME: `DamageFormula` uses
  `floor(baseDamage * max(0,100 - distance*falloffPercent) * (100-resistPercent) / 10000)`
  with `Long` intermediates and one final floor. Direct and splash resistance are applied; zero
  damage produces no `HitEvent`. The effective-DPS matrix is deterministic under single-target,
  in-range, no-splash, `ticks_per_second=20` assumptions.
- PERSISTENCE: `SandboxSaveCodec.SAVE_VERSION=11` is unchanged; typed damage metadata is
  registry-derived and not persisted.
- REPLAY: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`, resist `3f02607020d48668`.
- VERIFICATION: focused ENG-011 suite passed 24 tests after the `Int.MAX_VALUE` boundary test;
  full tests/projects/content/replay/save-compat/benchmark/Android assemble/diff-check passed;
  conditional simulation and balance reviewers passed, and the simulation low finding was resolved.
  Final metrics: `sim=422 ms`, `kill=63 ms`, `goal-field=10743500 ns`, `spatial-index=6.4719 ms`.

## ENG-019 Close-out (2026-08-02)

- DONE: The 1x1 wall MVP is content-defined and localized with validated cost, health, footprint,
  visual reference, and sell-refund metadata. Render-free place/remove commands perform atomic
  resource and occupancy updates, reject prospective all-spawn path blocks, rebuild routing for
  same-tick movement, expose immutable snapshot health, and persist through `SandboxSaveCodec` v12
  with v1-v11 migration and pending building-command restoration.
- DECISIONS: No ADR. The MVP is intentionally 1x1. Approved balance is 2 bolt cost, 20 HP, and a
  50% sell refund. Tower cost validation remains non-negative for compatibility; wall cost
  validation is strictly positive, and zero-cost wall content returns a structured validation error.
- VERIFICATION: Final runner 9/9 passed. Replay hashes are `e4892bcc18f9d8dc`,
  `a763da4ac32b15b4`, and `3f02607020d48668`. Save-compat includes the v12 wall fixture and the
  v1-v11 migration matrix. Benchmark: GoalField 64x64 / 3,844 reachable tiles / 11,813,800 ns
  rebuild; spatial index 1,024 enemies / 16 towers / 16 queries / 16 shots / 5.201 ms; canonical
  and kill scenarios 35 ticks / 431 ms and 79 ms. Android evidence is assembleDebug only; no
  device, emulator, visual-golden, or frame-budget claim is made.

## ENG-001 Close-out (2026-08-02)

- DONE: Added `engine-world` `AStarPathfinder` for deterministic 4-neighbor uniform integer-cost
  A* with `(f, row-major tile index)` open-set ordering, stable neighbor order, first predecessor
  on equal `g`, bounds/blocked/no-path handling, and optional occupied-start support. `engine-ai`
  `GridPathfinder` preserves the `PathRequest`/`PathResult` API while delegating to A*, and
  `AgentPathPlanner` keeps valid stored `MovementComponent` paths and deterministically repaths
  after route/world changes. Focused tests cover `pathIndex > 0` and occupied starts.
- DECISIONS: No ADR. This is an Android-free engine/vision capability; no save, content, render,
  Android, dependency, or game-bundle traceability change was made. Full Movement/job tick
  integration is intentionally deferred to ENG-003/ENG-004, and wave enemies remain on ENG-002's
  `GoalField`.
- NEXT: Run `/me --feature --next` for ENG-003 (JobBoard wired into tick), the next colony-slice
  item after ENG-001.
- BLOCKERS: No ENG-001 implementation blocker. The stored-path planner is not yet wired into a
  full Movement/job tick system; that integration is the planned ENG-003/ENG-004 scope. Colony
  demand remains vision-only until the recorded MySD Gate 1 or authored-game-spec trigger.
- VERIFICATION: `AStarPathfindingTest` and `AgentPathPlannerTest` pass; full `.\gradlew.bat test`,
  `.\gradlew.bat projects`, content-validate, sim-replay, save-compat, benchmark, and
  `git diff --check` pass. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`,
  `3f02607020d48668`. Benchmark: canonical 413 ms, kill 102 ms, GoalField rebuild 13099400 ns,
  spatial index 6.3748 ms. Final verifier passed with no findings and all boundary checks true;
  simulation-reviewer tie/equal-g and `pathIndex` findings were remediated.

## ENG-003 Close-out (2026-08-02)

- DONE: Wired the Android-free `JobExecutionSystem` into the sandbox fixed-tick pipeline. Positioned
  `JobActorComponent` entities claim jobs in deterministic worker/entity and priority/job-id order;
  lifecycle advances through `CLAIMED -> IN_PROGRESS -> DONE/FAILED`; pathfinding movement, work
  ticks, typed `resource_delta` completion effects, invalid-target release, reservation guards, and
  same-tick reclaim prevention are covered. `SandboxSaveCodec` v13 persists the JobBoard, in-flight
  jobs, actor assignment/progress, and effects, with v12 migration to empty job state.
- DECISIONS: No ADR, game-bundle traceability update, or plugin/skill/pipeline contract change.
  Approved defaults remain: every positioned `JobActorComponent` is eligible for all job types, one
  work tick is processed per simulation tick, in-world `TilePosition` is the target check, and
  invalid/no-path jobs return to `OPEN` after deterministic release.
- NEXT: Run `/me --feature --next` for ENG-031 (stockpile zones + designations). Colony demand remains
  vision-only pending MySD Gate 1 or an authored colony spec with named FRs.
- BLOCKERS: No implementation blocker. Three explicitly non-blocking follow-ups remain: the
  two-worker replay is not in `DevtoolReports.replayInspect`; `scripts/me-save-compat.ps1` does not
  separately invoke `SandboxJobExecutionTest`; and no job-heavy benchmark covers large worker/job
  counts or invalidated paths.
- VERIFICATION: Selfcheck, full test/projects/content/replay/save-compat/benchmark, Android assemble,
  and `git diff --check` passed; focused `JobExecutionSystemTest` and `SandboxJobExecutionTest`
  passed. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, `3f02607020d48668`. Final benchmark:
  simulation 430 ms, kill 76 ms, spatial index 6.6127 ms. Verifier passed with all boundary checks
  true.

## ENG-031 Close-out (2026-08-02)

- DONE: Accepted Option A is complete: deterministic zone define/update/remove commands and
  canonical zone state; validated stockpile filters; harvest-node designations that mint at most one
  deterministic `JobBoard` job; immutable `EngineSnapshot.zones`; and save v14 with v1-v13 migration.
- DECISIONS: No ADR and no game-bundle traceability update. The additive Android-free simulation,
  snapshot-only render, content-validation, and versioned-save boundaries remain unchanged. Colony
  demand is vision-only. Hauling, stockpile quantities/capacity, depletion/repeated harvest, and
  actual Android `RenderFrame`/view overlay rendering are deferred to follow-up/ENG-004 scope.
- NEXT: Run `/me --feature --next` for ENG-004 (first worker agent MVP / hauling), then ENG-032.
- BLOCKERS: No implementation blocker. Non-blocking follow-ups are: deleting a claimed/in-progress
  designation can leave its job and allow a second job on the same node; generic pending-command
  delimiter escaping remains a pre-existing codec concern (current ENG-031 ids are regex-safe);
  `RenderFrame`/Android view do not yet consume `snapshot.zones`; and per-frame zone snapshot
  allocations need later profiling/consumer work.
- VERIFICATION: Selfcheck passed. Full runner passed with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`:
  `gradlew test`, `projects`, content validation (2 packs), replay, save-compat, benchmark,
  `android:assembleDebug`, and `git diff --check`. Replay hashes: `e4892bcc18f9d8dc`,
  `a763da4ac32b15b4`, `3f02607020d48668`. Benchmark: canonical 413 ms, kill 85 ms,
  spatial index 5.2368 ms, goal rebuild 10958000 ns. Focused ENG-031 and remediation tests passed;
  simulation reviewer passed with a non-blocking finding, renderer passed, save reviewer passed with
  a low finding, Android review was partial with non-blocking findings, and verifier passed with all
  boundary checks true.

## Next Exact Action

Run `/me --feature --next` for ENG-032 (construction blueprints). ENG-004 has no implementation blocker.

## Known Blockers

- ENG-001 has no implementation blocker. Full Movement/job tick integration is intentionally
  deferred to ENG-003/ENG-004; wave enemies continue to use the ENG-002 `GoalField`. Colony demand
  remains vision-only until MySD Gate 1 or an authored colony game spec supplies named FRs.
- ENG-003 has no implementation blocker. Non-blocking follow-ups are limited to adding the two-worker
  replay to `DevtoolReports.replayInspect`, invoking `SandboxJobExecutionTest` from
  `scripts/me-save-compat.ps1`, and adding a job-heavy benchmark for large worker/job counts and
  invalidated paths.
- ENG-031 has no implementation blocker. Its non-blocking follow-ups are recorded in the ENG-031
  close-out above; colony demand remains vision-only until MySD Gate 1 or an authored colony game
  spec supplies named FRs.
- ENG-011 has no implementation blocker. No device/emulator evidence is claimed; existing manual
  Android/device and performance follow-ups remain unchanged.
- RESOLVED (2026-07-29): the pre-existing unrelated dirty `.ai/retro/retro-2026-07-28.md` commit
  blocker is cleared; PROC-013 commit 5eaaa78 was pushed.
- ENG-030 non-blocking follow-ups: existing content packs intentionally do not configure an
  early-call bonus because no balance value was approved; synthetic tests cover the data-driven
  path. Per-snapshot HUD allocation and device profiling remain follow-up work. The pre-existing
  save delimiter assumption remains documented; no code fix was made in this docs-only close-out.

## ENG-004 Close-out (2026-08-02)

- DONE: Added optional worker content with deterministic speed/capacity, typed haul jobs/phases,
  atomic source reservations, source pickup, speed-budgeted movement, stockpile deposit contents,
  and positioned ProducerSystem outputs as haul sources without double-counting global inventory.
  In-transit carry is persisted in the existing Entity inventory component.
- DECISIONS: No ADR or game-bundle traceability update. Generic jobs remain compatible through an
  eligibility filter; legacy replay hashes are unchanged when no new worker/haul state exists.
  SandboxSaveCodec is v15 with v1-v14 migration. Generic stockpile capacity/depletion, worker
  spawn commands, and Android overlay consumption remain follow-up scope.
- NEXT: Run `/me --feature --next` for ENG-032 (construction blueprints).
- BLOCKERS: No implementation blocker; no device/emulator or visual-golden proof is claimed beyond
  Android assembleDebug.
- VERIFICATION: Full tests, projects, content validation, replay, save-compat, benchmark,
  Android assembleDebug, selfcheck, and git diff --check passed. Replay hashes remain
  `e4892bcc18f9d8dc` and `a763da4ac32b15b4`.
- ENG-028 non-blocking follow-ups: Android AssetManager resolution has compile/assemble coverage but
  no device/instrumented runtime fixture; the full PROC-009 screenshot/golden lane remains manual.
  Conditional domain reviewer agents could not be spawned in this run because the agent-thread limit
  was reached; a local read-only boundary review passed, but reviewer-agent evidence should be
  refreshed when capacity is available.
- ENG-029 non-blocking manual limitations: no real device/emulator SoundPool playback, volume/mute,
  or frame-metrics evidence was available. The Android reviewer contract was unavailable; local
  Android/content/simulation boundary and gate evidence passed. The initial invalid
  `:android:test --tests` invocation was corrected to `:android:testDebugUnitTest --tests` and is
  not a feature failure.
- ENG-012 non-blocking limitation: conditional reviewer agents were unavailable after repeated
  thread timeouts; local simulation/render/save/content boundary review passed. No device/emulator
  proof is claimed beyond the Android assemble gate.
- ENG-009 intentionally ships no splash balance values. Future game packs must choose them through
  an approved balance change; there is no engine blocker from this feature.
- ENG-020 has one low, non-blocking test-coverage follow-up: seeded differential tests do not yet
  provide end-to-end `updateTowers` parity across every targeting-mode and splash combination.
  This is attributed to `me-tester`; no production fix is requested in this close-out. The
  benchmark is measurement-only and does not define PROC-004 budget thresholds.
- ENG-015 is accepted with non-blocking follow-ups: device/instrumentation tap, lifecycle/recreation,
  and Bundle smoke plus FrameMetrics/JankStats/Allocation Tracker evidence remain pending. The
  extreme `200x600` portrait layout has a manual risk of selected-panel overflow. A pre-existing
  `pausedSave` stale-state rollback risk is outside ENG-015 scope. At `0x`, the active Choreographer
  performs idle HUD redraw, an accepted CPU/battery trade-off.
- ENG-027 is accepted with non-blocking manual limitations: on a device/emulator, smoke build-tower,
  tower selection, upgrade, and pause/recreate lifecycle continuity; exercise non-default fontScale
  and long localized labels; capture FrameMetrics/JankStats and Allocation Tracker evidence before
  claiming smoothness or an allocation/frame budget. Save v6 hardening should also add explicit
  rejection tests/policy for a missing `towerMetrics` field and duplicate tower entity ids inside
  that field; current compatibility tests cover valid v1-v6 data.
- ENG-026 remains device/performance-pending: run tap, pan, pinch, and pause/recreate with a
  pending command on a device/emulator, confirming next command ID and replay-hash continuity.
  Capture FrameMetrics/JankStats and Allocation Tracker evidence before claiming a frame budget or
  smoothness target. `me-android-performance` was partial only for these manual checks.
- MTD-005 is accepted but device-pending: no emulator/device smoke has verified tap-build after one
  tick, drag-without-build, pinch zoom, tiles -> path -> entities draw order, or debug
  rotation/process recreation with pending commands.
- MTD-005 known performance risk: each redraw projects a snapshot/frame and creates intermediate
  primitive lists. Profile sustained pan/pinch with FrameMetrics/JankStats and Allocation Tracker
  before asserting a frame budget or smoothness target.
- ENG-005 low, non-blocking: the Android module packages the sandbox content tree, but
  `SandboxGame.loadRegistry()` still seeks a filesystem path rather than `AssetManager`; device
  startup/content loading remains unverified.
- ENG-005 low, non-blocking: `BalanceDeltaReport` does not include map-local resource-node quantities
  or geometry, so future map-only economy changes will not appear in its deltas.
- MTD-004 low, non-blocking follow-up: `difficultyId` is not serialized; restore requires the same
  effective difficulty-resolved registry.
- RESOLVED (2026-07-05, SG-005): suspicious-value balance reporting is now covered by the devtools
  `balance-report` / `balance-delta` JSON report.
- SG-004 DEVICE BLOCKER (2026-07-04, acceptance #3): no connected Android device/emulator is
  available in this environment, so the on-device Bundle round-trip (`onSaveInstanceState` outState
  -> `onCreate` savedInstanceState under config-change/process-death) — the real instrumented
  pause/resume + save-directory-access smoke from `docs/contracts/android.md` Test Gates — CANNOT be
  executed here. The device-independent proof (save-at-pause == uninterrupted run to the same tick,
  seed roundtrip, versioned-save rejection) is JVM-covered by
  `games/sandbox/.../SandboxSessionLifecycleTest.kt` against the Android-free `SandboxSession`;
  `.\gradlew.bat android:assembleDebug` is the best available static gate (proves the
  `MyEngineActivity` + Bundle wiring compiles/links). Closing acceptance #3's device path needs a
  device/emulator run (SG-004 follow-up 4 below).
- SG-004 follow-ups (2026-07-04, non-blocking, low, from me-save-compat-reviewer/me-android-performance):
  1. RESOLVED (2026-07-05, SG-004 follow-up): the runtime's pending `CommandQueue` is now persisted
     (`SAVE_VERSION` v1->v2 migration in `SandboxSaveCodec`), so `save()` is sound outside a quiescent
     tick — the quiescent-save precondition is dropped. (The per-tick incident RNG needed no
     persistence: confirmed to be a fresh `SeededRandom(17)` instance every tick, not a cursor.) See
     DONE "SG-004 follow-up" above.
  2. RESOLVED (2026-07-05, SG-004 follow-up): `DEBUG_SAVE` is now gated on `BuildConfig.DEBUG`
     (`android/build.gradle.kts` gained `buildFeatures { buildConfig = true }`), so the lifecycle save
     path cannot ship enabled in a release build.
  3. Move the `onSaveInstanceState` encode off the main thread (or cap size) once state grows beyond
     the tiny sandbox. Still OPEN (not addressed by the SG-004 follow-up).
  4. On a device/emulator, run the real pause/resume + process-death Bundle round-trip to close
     acceptance #3's device path. Still OPEN/device-pending — unaffected by the SG-004 follow-up.
- SG-004 follow-up new low-severity items (2026-07-05, non-blocking, from
  me-simulation-reviewer/me-save-compat-reviewer/me-android-performance/me-verifier):
  1. No persisted CommandId-issuing counter for the new `pendingCommands` encoding — forward-looking
     only; no production caller mints sequential command ids today, so there is no current collision
     risk. Revisit if/when a real command-submitting UI is added.
  2. No engine-core-unit-level determinism test for `CommandQueue.pending()`/`SandboxRuntime.submitAll()`
     in isolation — covered end-to-end at the sandbox level (`SandboxSessionLifecycleTest`) instead.
  3. The new `pendingCommands` properties-line encoding assumes command type/actorId/stablePayload
     contain no `;`/`|`/`:` — the same pre-existing delimiter-collision assumption class as the
     entities/producers encodings in the same codec file; not a new risk class, just a new field
     sharing it.
- SG-003 follow-ups (2026-07-04, non-blocking, low/info from me-renderer-qa/me-verifier):
  1. RESOLVED (2026-07-04, SG-003 follow-up): `RenderFrame` is now consumed in a real launcher —
     `DesktopLauncher` projects the scenario snapshot via `PlaceholderRenderSurface` and rasterizes
     it through the new headless AWT `FrameRasterizer` to `desktop/build/render-smoke.png`, and the
     new deterministic `FrameRasterizerPixelSmokeTest` gives all six `RenderKind`s visual coverage
     — closing the `docs/contracts/render.md` pixel-smoke gate that unit tests alone previously met.
     (Android shell still uses `AsciiRenderer` — Android real-launcher wiring is a separate
     follow-up, see item 4.)
  2. RESOLVED (2026-07-04, SG-003 follow-up): the durable kind->color mapping now lives in
     `engine-render/.../RenderPalette.kt` (Android-safe, no java.awt/android imports) as the shared
     reference future launcher authors reuse; the note that core health is read from
     `RenderFrame.coreHealth` (CORE primitive carries no health) and that `ENEMY.health` is nullable
     (null-check before drawing a pip/health bar) is now encoded by `FrameRasterizer` (pip drawn
     only when `health != null`; `core <n>` from `coreHealth`) and asserted by the pixel-smoke.
  3. Low/pre-existing (NOT introduced by SG-003, still OPEN): `Camera.clamped()` clamps center to
     `[0..width]` inclusive while `screenToTile` clamps to `width-1` — reconcile if precise edge
     framing matters later. (Not exercised by the SG-003 follow-up change.)
  4. Low/deferred (NOT blocking): the Android shell still uses `AsciiRenderer`; wiring the Android
     real launcher to consume `RenderFrame` is a tracked follow-up.
- RESOLVED (2026-07-04): the default content gate now covers every per-game pack. Added
  `DevtoolReports.contentReportAll()` (discovers every `games/<game>/content/<pack>` root via
  `repoRoot()`+`discoverPackRoots()` and aggregates one JSON), a `content-report-all` devtool
  command, and rewrote `me-content-validate.ps1` to invoke it and emit one runner-contract JSON
  line. `content-report <relative path>` now also resolves from the repo root (previously reported
  "File is missing"). Signal Garden is now in the default gate (`pack_count=2`, both valid).
- RESOLVED (2026-07-04): the default sim/benchmark gate now exercises kills+rewards. Added a second
  canonical scenario `SandboxGame.runScriptedKillScenario()` (pulse tower at (2,2), adjacent to the
  (1,1) enemy spawn, step 35 -> 2 kills / 4 shots) reported alongside the existing one. `balance`
  and `replay-inspect` now emit `{"scenarios":[canonical, kill]}`; the kill scenario has a stable
  hash `83a65da1a7881b2c` with `enemies_killed=2, tower_shots=4`. The original canonical scenario
  (tower (30,32)) and its hash `9c495d8ff30fd83d` are unchanged (Option A: additive, no baseline
  churn).
- RESOLVED (2026-07-04): the capacity-overflow branch in `SandboxRuntime.step` no longer silently
  drops rewards. Decision: a full inventory is a legitimate game state (non-fatal, no exception),
  but a dropped reward must be observable -> the deposit now surfaces
  `lastCommandOrError="reward_dropped:<res>:<amt>"` (not part of `appendHash`, so it cannot perturb
  any replay hash). The deposit logic is extracted to a pure `depositRewards(inventory, rewards)`
  helper and covered now (without shipping capacities in default content) by `SandboxRewardOverflowTest`
  (unit add/drop + a capacity-bound `step` telemetry case).
- Gradle still emits a Gradle 10 deprecation warning from AGP/Gradle internals; builds pass.

## Verification

- ENG-020 (2026-07-29): focused engine-defense tests (14), focused engine-devtools tests (16),
  full `.\gradlew.bat test`, `.\gradlew.bat projects` with explicit Android Studio `JAVA_HOME`,
  content validation (2 packs), replay, save-compat, benchmark, and `git diff --check` -> pass.
  Replay hashes remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; the
  `spatial-index-1k` benchmark reports 1024 concurrent enemies, 16 towers, 16 queries, and
  `5.3045 ms`. `me-simulation-reviewer` and `me-verifier` -> pass; all boundary checks are true.
  The low `me-tester` end-to-end parity follow-up is recorded above.
- ENG-029 (2026-08-02): selfcheck -> pass; focused core/content/sandbox/Android tests (160 tests) ->
  pass; full `.\gradlew.bat test` and `.\gradlew.bat projects` -> pass; content validation
  (2 packs) -> pass; replay -> pass with canonical `e4892bcc18f9d8dc` and kill
  `a763da4ac32b15b4`; save-compat v10 matrix -> pass; benchmark -> pass with `sim=412 ms`,
  `kill=83 ms`, `spatial-index-1k=5.62 ms`, `goal-field=9947100 ns`; `:android:assembleDebug` ->
  pass; `git diff --check` -> pass. The initial invalid `:android:test --tests` invocation was
  corrected to `:android:testDebugUnitTest --tests` and is not a feature failure. The Android
  reviewer contract was unavailable; local boundary/gate evidence passed. No `SAVE_VERSION` bump.
- ENG-009 (2026-07-28): full `.\gradlew.bat test` and `.\gradlew.bat projects`, content validation
  (2 packs), replay, save-compat, benchmark, and `:android:assembleDebug` -> pass. Replay hashes
  remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; benchmark is canonical `324 ms`,
  kill `62 ms`, goal-field rebuild `7168200 ns`. Balance review and final verifier -> pass, with all
  boundary checks true. Events are immutable/transient and excluded from save/stable-hash state;
  `SAVE_VERSION` remains `8`.

- ENG-028 (2026-07-28): selfcheck, full `.\gradlew.bat test`, `.\gradlew.bat projects`, focused
  content/render/sandbox/desktop tests, content validation (2 packs), replay, save-compat, benchmark,
  `:android:testDebugUnitTest`, `:android:assembleDebug`, `:desktop:run`, and `git diff --check` ->
  pass. Replay hashes remain canonical `12a65fd2b87593cf`/`bb37eefc1903cc77`; benchmark is
  `sim_ms=341`/`71`, goal-field rebuild `9726600 ns`. Tester found a building projection gap, which
  was repaired and re-tested. Conditional reviewer agents were unavailable due the agent-thread limit;
  local read-only simulation/render/save/Android/content boundary review passed. Android runtime/device
  AssetManager and PROC-009 golden screenshot checks remain manual-pending.
- ENG-015 (2026-07-21): selfcheck -> pass; full `.\gradlew.bat test`, `:android:testDebugUnitTest`,
  and `:android:assembleDebug` -> pass; content validation, save-compat, replay, and benchmark ->
  pass. Replay hashes: canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`; benchmark:
  canonical `328 ms`, kill `66 ms`, rebuild `4.1305 ms`. `me-verifier` -> pass with all
  `boundary_checks` true. Device/instrumentation and FrameMetrics/JankStats evidence remain pending.
- ENG-008 (2026-07-18): full `./gradlew.bat test` -> pass; content validation -> pass
  (`validated 2 pack(s)`); replay -> pass with canonical `12a65fd2b87593cf` and kill
  `bb37eefc1903cc77`; save-compat -> pass; benchmark -> pass (`canonical=432 ms`, `kill=79 ms`,
  64x64 goal-field rebuild `5.3678 ms`). Required domain reviewers and final `me-verifier` -> pass.
  Coverage includes every selector mode, v1 content fallback to `NEAREST`, queued mid-run switching,
  HUD projection, v1-v6 -> v7 save migration, and pending-command round-trip.
- ENG-013 (2026-07-18): full `./gradlew.bat test` -> pass; content validation -> pass
  (`validated 2 pack(s)`); replay -> pass with canonical `463d87684ca6cbee` and kill
  `40c7bda7e3bc1316`; save-compat -> pass; benchmark -> pass (`canonical=335 ms`, `kill=70 ms`,
  64x64 goal-field rebuild `6.505600 ms`). Required domain reviewers and `me-verifier` -> pass.
  Initial test/content gate failures exposed missing required `sellRefundRatio` fields in a test
  fixture and the Signal Garden pack; both were repaired before the final full gate rerun. No ADR:
  this is an additive content/sandbox command capability with no new dependency edge or save-format
  version change (`SAVE_VERSION` remains `6`).
- ENG-027 (2026-07-18): full `gradlew test` -> pass; content validation -> pass for 2 packs;
  replay -> pass with canonical `9c495d8ff30fd83d` and kill `83a65da1a7881b2c`; save-compat ->
  pass; benchmark -> pass (`canonical=295 ms`, `kill=45 ms`); `android:assembleDebug` -> pass.
  `me-verifier` accepted all criteria and boundary checks. No device/emulator build/select/upgrade/
  lifecycle smoke, fontScale/long-label visual check, or FrameMetrics/JankStats/allocation profile
  was run; malformed v6 missing/duplicate `towerMetrics` hardening remains non-blocking.
- ENG-026: `:android:testDebugUnitTest --tests dev.myengine.android.FixedTickFrameLoopTest
  --rerun-tasks` -> pass; `:android:assembleDebug` -> pass; replay -> pass with canonical
  `9c495d8ff30fd83d` and kill `83a65da1a7881b2c`; save-compat -> pass. `me-tester` reported no
  test-file changes; `me-verifier` -> pass. No device/emulator smoke or FrameMetrics/JankStats /
  Allocation Tracker profile was run.

- MTD-005: `:engine-render:test` -> pass; `:games:sandbox:test` -> pass;
  `:android:assembleDebug` -> pass; `scripts\me-sim-replay.ps1` -> pass with canonical
  `9c495d8ff30fd83d` and kill `83a65da1a7881b2c` unchanged at tick 35. Content validation,
  save-compat, and benchmark were not run by scope. `me-verifier` -> pass, all four boundary checks
  true. No device smoke or performance profile was run.

- ENG-014: serial full Gradle suite (--no-daemon --max-workers=1), content validation (2 packs),
  canonical/kill replay hashes (9c495d8ff30fd83d, 83a65da1a7881b2c), save-compat, benchmark, and
  Android assemble -> pass. Serial mode followed earlier parallel native-memory exhaustion only,
  not an assertion failure.

- `python C:\Users\Admin\.codex\skills\.system\plugin-creator\scripts\validate_plugin.py codex-plugins\me-dev` -> pass.
- `python C:\Users\Admin\.codex\skills\.system\plugin-creator\scripts\validate_plugin.py codex-plugins\me-spec` -> pass.
- `.\gradlew.bat projects` -> pass.
- `.\gradlew.bat test` -> pass.
- `scripts\me-content-validate.ps1` -> pass; now emits one runner-contract JSON line
  (`{"status":"pass",...,"notes":"validated 2 pack(s)"}`) covering all `games/*/content/*` packs.
- Content gate coverage (2026-07-04): `.\gradlew.bat :engine-devtools:test` -> pass (incl. new
  `contentReportAllCoversEveryGamePack`); `engine-devtools:run content-report-all` ->
  `{"valid":true,"pack_count":2,"packs":[sandbox, signal-garden]}`; relative-path
  `content-report games/signal-garden/content/signal-garden` -> valid (was "File is missing").
- SG-001 pack (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl. new
  `SignalGardenContentPackTest`); `engine-devtools:run content-report <abs path to
  games/signal-garden/content/signal-garden>` -> `{"pack_id":"signal-garden","valid":true,"errors":[]}`.
- SG-002 reward deposit (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl. new
  `DefenseRuntimeTest` conservation + leaked-enemy cases, `SandboxRewardDepositTest`, and the
  `SandboxVerticalSliceTest` kill-scenario determinism + reward save-roundtrip cases).
- SG-002 `scripts\me-sim-replay.ps1` -> pass; final hash `9c495d8ff30fd83d` UNCHANGED (the
  canonical scripted scenario kills 0 enemies in its 35-tick window, so the reward-deposit path is
  not exercised by that scenario; this is expected and the hash reference is still correct).
- SG-002 `scripts\me-save-compat.ps1` -> pass.
- SG-002 `scripts\me-benchmark.ps1` -> pass; confirms the canonical scripted scenario
  (tower (30,32), step 35) has enemies_killed=0, tower_shots=0.
- SG-002 telemetry event recorded via `scripts\me-record-run.ps1` (events=3).
- SG-002 kill/reward gate hardening (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl.
  new `DevtoolReportsTest` kill/canonical/suite cases, `SandboxVerticalSliceTest`
  `killScenarioApiKillsDeterministicallyAndDiffersFromCanonical`, and `SandboxRewardOverflowTest`);
  `scripts\me-sim-replay.ps1` -> pass, emits `{"scenarios":[canonical 9c495d8ff30fd83d kills=0,
  kill 83a65da1a7881b2c kills=2]}`; `scripts\me-benchmark.ps1` -> pass, kill scenario
  enemies_killed=2/tower_shots=4, canonical unchanged (0/0); `scripts\me-save-compat.ps1` -> pass;
  `.\gradlew.bat desktop:run` -> prints canonical hash `9c495d8ff30fd83d` (unchanged).
- SG-003 placeholder render surface (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl.
  extended `RenderBoundaryTest` camera pan/zoom cases, new `PlaceholderRenderSurfaceTest` (six kinds
  present, enemy health carried / tower null, screen == `worldToScreen(tile center)`,
  tiles-then-entities-by-id ordering, unknown-type safety, projection purity), and new
  `SandboxRenderNonMutationTest` (stableHash unchanged across a projection of the runtime snapshot));
  `.\gradlew.bat desktop:run` -> pass (exit 0; renders ASCII map; existing `AsciiRenderer` path
  unaffected); telemetry recorded via `scripts\me-record-run.ps1` (events=4).
- SG-003 follow-up (2026-07-04, RenderFrame consumed in a desktop launcher + pixel-smoke): new files
  `engine-render/src/main/kotlin/dev/myengine/render/RenderPalette.kt`,
  `desktop/src/main/kotlin/dev/myengine/desktop/FrameRasterizer.kt`,
  `desktop/src/test/kotlin/dev/myengine/desktop/FrameRasterizerPixelSmokeTest.kt`; edits to
  `desktop/src/main/kotlin/dev/myengine/desktop/DesktopLauncher.kt` (debug render-smoke block) and
  `desktop/build.gradle.kts` (JUnit5 test deps + `useJUnitPlatform()`). `.\gradlew.bat test` -> pass
  (full suite incl. new `FrameRasterizerPixelSmokeTest`); `.\gradlew.bat desktop:run` -> exit 0,
  `hash=9c495d8ff30fd83d` (canonical, unchanged), ASCII map, `png=D:\Pet\MyEngine\desktop\build\
  render-smoke.png`; `.\gradlew.bat android:assembleDebug` -> BUILD SUCCESSFUL (proves
  `RenderPalette` did not pull java.awt into the Android artifact). Pipeline architect ->
  engine-developer -> tester -> runner -> renderer-qa (pass) + verifier (pass, all four
  boundary_checks true) -> docs; one post-review cosmetic fix (debug PNG path corrected from
  `desktop\desktop\build\...` to `desktop\build\render-smoke.png`; re-ran `desktop:run`, hash
  unchanged).
- SG-004 Android lifecycle save smoke (2026-07-04): new
  `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt`,
  `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt`; edit
  `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` (Bundle save/restore under
  `DEBUG_SAVE`). `.\gradlew.bat test` -> pass (full suite incl. `SandboxSessionLifecycleTest`
  save/restore roundtrip, pause/resume determinism, seed roundtrip, independent-runtime, and
  future-version + non-numeric-version decode rejection); `scripts\me-save-compat.ps1` -> pass;
  `.\gradlew.bat android:assembleDebug` -> BUILD SUCCESSFUL (thin adapter compiles + packages);
  telemetry recorded via `scripts\me-record-run.ps1` (events=7). On-device Bundle round-trip is
  device-pending (acceptance #3; see Known Blockers). Pipeline: architect `me-architect` (Option A —
  pure holder in `games/sandbox` + thin Activity adapter; command-queue/RNG persistence deferred; no
  ADR) -> developer `me-engine-developer` -> tester `me-tester` -> reviewers `me-save-compat-reviewer`
  (pass), `me-android-performance` (pass), `me-verifier` (pass, all four boundary_checks true) -> docs.
- SG-004 follow-up (2026-07-05, sandbox save-format v2 / pending CommandQueue persistence): edits to
  `engine-core/src/main/kotlin/dev/myengine/core/Command.kt` (new `CommandQueue.pending()`),
  `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
  (`SAVE_VERSION` 1->2, `pendingCommands` encode/decode, `decodePendingCommands`),
  `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt` (save/restore wiring),
  `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` (`DEBUG_SAVE` ->
  `BuildConfig.DEBUG`), `android/build.gradle.kts` (`buildFeatures { buildConfig = true }`); test edit
  `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt`.
  `.\gradlew.bat :games:sandbox:test` -> pass; full `.\gradlew.bat test` -> pass;
  `scripts\me-save-compat.ps1` -> pass; `.\gradlew.bat :android:compileDebugKotlin` -> pass (verified
  after pointing a machine-local, gitignored `local.properties` at the Android SDK — not a code
  change). Reviews: `me-simulation-reviewer` pass (2 non-blocking mediums — no persisted
  CommandId-issuing counter, no engine-core-unit-level determinism test for pending()/submitAll());
  `me-save-compat-reviewer` pass (1 non-blocking low — delimiter-collision assumption class extended
  to the new pendingCommands field); `me-android-performance` pass (1 non-blocking low — `DEBUG_SAVE`
  losing `const val` means release-branch elimination now depends on R8/minification rather than
  compile-time folding, acceptable for trivial branch bodies; 1 pre-existing/unchanged medium — no
  onPause/onStop/onDestroy persistence path yet, Bundle-only, still device-pending);
  `me-verifier` pass (all four boundary_checks true; confirmed all three SG-004 acceptance criteria
  satisfied, including "Lifecycle pause does not corrupt simulation state" now strictly stronger —
  sound at any tick, not just quiescent).
- SG-005 balance report deltas (2026-07-05): edits to
  `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolReports.kt`
  (`BalanceDeltaReport`, summaries, deterministic deltas/warnings),
  `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolsMain.kt` (`balance-report` /
  `balance-delta` command), `engine-devtools/src/test/kotlin/dev/myengine/devtools/DevtoolReportsTest.kt`
  (baseline/changed, no-op, warning, invalid-pack, parser-backed JSON, CLI stdout tests),
  `engine-devtools/build.gradle.kts`, and `gradle/libs.versions.toml` (test-only
  `kotlinx-serialization-json`). `.\gradlew.bat :engine-devtools:test` -> pass; full
  `.\gradlew.bat test` -> pass; `scripts\me-content-validate.ps1` -> pass; `scripts\me-sim-replay.ps1`
  -> pass (canonical `9c495d8ff30fd83d`, kill `83a65da1a7881b2c`); `scripts\me-save-compat.ps1` ->
  pass; `scripts\me-benchmark.ps1` -> pass; `.\gradlew.bat -q :engine-devtools:run --args="balance-report"`
  -> pass and emits one JSON object with enemy/core/resource warnings. Pipeline: architect
  `me-architect` (pass), tester `me-tester` initially found missing JSON-parse/CLI coverage (fixed),
  runner gates pass, final `me-verifier` pass (all four boundary_checks true; no findings). Telemetry
  recorded via `scripts\me-record-run.ps1` (events=9, `reflect_required=false`).
- MTD-001/MTD-002 (2026-07-05): edits to
  `.claude/specs/backlog/MTD-001-reward-deposit.md` (status done, duplicate resolution),
  `.claude/specs/backlog/MTD-002-gold-cost-gating.md` (status done, generic resource-gate
  resolution), `.claude/specs/ENGINE_ROADMAP.md`, `D:/Pet/MyTD/spec/engine-gap-analysis.md`,
  `D:/Pet/MyTD/spec/traceability.csv`, `D:/Pet/MyTD/spec/risks.md`, and new
  `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxTowerCostGatingTest.kt`.
  `.\gradlew.bat :games:sandbox:test` -> pass; full `.\gradlew.bat test` -> pass;
  `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`);
  `scripts\me-sim-replay.ps1` -> pass (canonical `9c495d8ff30fd83d`, kill
  `83a65da1a7881b2c`); `scripts\me-save-compat.ps1` -> pass; `scripts\me-benchmark.ps1` -> pass.
  `scripts\me-record-run.ps1` -> recorded event 10 with `reflect_required=true`;
  `scripts\me-retro.ps1` -> pass, wrote `.ai/retro/retro-2026-07-05.md` with no failure clusters.
  Initial bare Gradle invocation failed before testing because inherited `JAVA_HOME` was invalid;
  reruns with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` passed.
- MTD-003 tower upgrade hook (2026-07-05): edits to
  `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt` and
  `ContentLoader.kt` (upgrade tier model/parser/reference validation),
  `engine-entities/src/main/kotlin/dev/myengine/entities/EntityModel.kt` (`TowerComponent`
  `upgradeBranch`/`upgradeTier`), `engine-render/src/main/kotlin/dev/myengine/render/RenderModel.kt`
  (`UpgradeTowerCommand`), `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
  (`SandboxRuntime.upgradeTower`, save v3 entity/pending-command decode),
  `games/sandbox/content/sandbox/towers.properties`, `docs/content-schemas/PROPERTIES_SCHEMA.md`,
  tests in `ContentPackLoaderTest`, `SandboxSessionLifecycleTest`, and new
  `SandboxTowerUpgradeTest`, plus MTD roadmap/spec docs. `.\gradlew.bat :engine-content:test
  :games:sandbox:test` -> pass; full `.\gradlew.bat test` -> pass;
  `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`);
  `scripts\me-sim-replay.ps1` -> pass (canonical `9c495d8ff30fd83d`, kill
  `83a65da1a7881b2c` unchanged); `scripts\me-save-compat.ps1` -> pass;
  `scripts\me-benchmark.ps1` -> pass; `scripts\me-record-run.ps1` -> recorded event 11 with
  `reflect_required=false`. Pipeline: scouts -> architect `me-architect` (no ADR; required
  transition validation + delimiter-safe branch ids, both implemented) -> developer/tester
  `me-tester` pass -> runner gates pass -> reviewers `me-simulation-reviewer`, `me-save-compat-reviewer`,
  `me-renderer-qa`, `me-balance-simulator`, `me-verifier` all pass. Non-blocking note:
  `UpgradeTowerCommand` follows existing `BuildTowerCommand` placement in `engine-render`; a future
  command-API module refactor can clean that boundary.
- MTD-004 difficulty modifiers (2026-07-16): added `DifficultyContent`, optional
  `difficulties.properties`, `ContentRegistry.resolveDifficulty`, and deterministic `BigDecimal`
  scaling before the first tick; setup wiring is in `SandboxGame`/`SandboxSession`. Source values
  are from the MyTD balance plan. No save-format/Android/render changes and no ADR. `.\gradlew.bat
  :engine-content:test :games:sandbox:test` -> pass; full `.\gradlew.bat test` -> pass;
  `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`); `scripts\me-sim-replay.ps1` ->
  pass (`9c495d8ff30fd83d`, `83a65da1a7881b2c`); `scripts\me-save-compat.ps1` -> pass;
  `scripts\me-benchmark.ps1` -> pass (`sim_ms=429` implementation run). `me-verifier` -> pass,
  all boundary checks true.
- ENG-024 command DTO relocation (2026-07-16): approved variant A moved `BuildTowerCommand` and
  `UpgradeTowerCommand` to `engine-core/.../core/command/TowerCommands.kt` with `TileCoordinate`;
  `InputState` lost `nextCommandId`/`selectedTowerId`, `InputUiState` became explicit, and callers
  supply `CommandId`; sandbox boundary conversion is retained. Full tests, replay, save-compat,
  `android:assembleDebug`, and static scan passed; canonical hashes `9c495d8ff30fd83d` and
  `83a65da1a7881b2c` unchanged; `me-verifier` passed with all boundary checks true. Content
  validation and benchmark were not run by scope.
- `scripts\me-sim-replay.ps1` -> pass; final hash `9c495d8ff30fd83d`.
- `scripts\me-save-compat.ps1` -> pass.
- `scripts\me-benchmark.ps1` -> pass; emits JSON balance metrics.
- `scripts\me-record-run.ps1` synthetic event -> pass.
- `scripts\me-retro.ps1` -> pass; wrote `.ai/retro/retro-2026-07-02.md`.
- `.\gradlew.bat desktop:run` -> pass; prints sandbox hash and ASCII snapshot.
- `.\gradlew.bat android:assembleDebug` -> pass.
- `scripts\me-selfcheck.ps1` -> pass; Claude plugins + repo marketplace wired to canon.
- PROC v0.2.0 (2026-07-04): `scripts\me-selfcheck.ps1` -> pass after all adapter edits;
  `scripts\me-record-run.ps1` (improve run, events=6) -> new fields present in
  `.ai/runs/telemetry.jsonl` (`duration_min`, `malformed_json_count`, `gate_failures`,
  `attributed_agent`) and output gained `reflect_required`; `scripts\me-retro.ps1` -> pass,
  wrote `.ai/retro/retro-2026-07-04.md` with attribution/gate-failure/retry aggregation
  (legacy events without the new fields aggregate cleanly); `.\gradlew.bat test` -> pass
  (engine code untouched).

Environment used:

- `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`

## ENG-020 Close-out (2026-07-29)

- DONE:
  - Accepted the internal non-persisted `GridSpatialIndex` and deterministic spatial-index-1k
    benchmark; no source, test, save schema, content schema, Android/render, or public API change
    was made by this documentation close-out.
- DECISIONS:
  - Keep the index as an engine-defense implementation detail with live entity resolution and no
    persistence. No ADR is required.
- NEXT:
  - Perform backlog sequencing; the roadmap does not define a unique successor after ENG-020.
- BLOCKERS:
  - Low non-blocking `me-tester` follow-up: add seeded end-to-end `updateTowers` parity coverage
    across all targeting modes and splash combinations when that test expansion is intentionally
    scheduled. The accepted benchmark remains a measurement input for future PROC-004 budgets.
- VERIFICATION:
  - All required gates passed: focused tests 14 + 16, full tests, projects with explicit
    `JAVA_HOME`, content validation, replay, save-compat, benchmark, and `git diff --check`.
  - Replay hashes are unchanged (`canonical=12a65fd2b87593cf`, `kill=bb37eefc1903cc77`);
    benchmark metrics are `1024 enemies / 16 towers / 16 queries / 5.3045 ms`.

## MyTD Spec Bundle (2026-07-04)

- `/me-spec` clone-intake vs reference `com.vipubstd.games.block.defense` produced an original,
  traceable `--greenfield-game` bundle at `D:/Pet/MyTD/spec` (mechanics cloned, names/numbers own).
- Both gates passed. Engine gaps bridged to backlog: `MTD-001` reward deposit (done/duplicate of
  SG-002), `MTD-002` gold-cost gating (done), `MTD-003` tower upgrade hook (done), `MTD-004`
  difficulty modifiers (done), `MTD-005` render/input surface.
- Game content (EG-006) stays in the MyTD bundle; not a MyEngine backlog item.

## Notes

- This directory is still not a git repository.
- Claude workflows now ship as installable plugins via `.claude-plugin/marketplace.json`
  (`me-dev` -> `/me`, `me-spec` -> `/me-spec`); adapters stay thin over `docs/agentic`.
- `D:\Pet\mobile-pipeline` remains a process reference, not a copy source.
- v0.1 content uses `.properties` files until a future ADR justifies another parser/schema stack.

## PROC-013 Close-out (2026-07-29)

- DONE: Variant B migrated 23 verified-done cards from `backlog/` to `done/` and reconciled the
  PROC-013 roadmap status. The board checker is wired into `scripts/me-selfcheck.ps1`.
- BEHAVIOR: The checker emits one compact JSON result; exit 0 means pass and exit 1 means a board
  or roadmap mismatch.
- DECISIONS: No ADR; this is documentation/process wiring with no canonical contract or adapter
  change.
- NEXT: Final commit/push remains blocked by the pre-existing unrelated dirty
  `.ai/retro/retro-2026-07-28.md` unless the user approves/clears it; then perform backlog
  sequencing after ENG-020.
- BLOCKERS: Only the unrelated dirty retro file blocks final commit/push for this close-out.
- VERIFICATION: Developer, tester, runner, and verifier passed; board checker, selfcheck, and
  `git diff --check` verification are recorded for the close-out.

## PROC-003 Close-out (2026-07-29)

- DONE:
  - Domain systems sequencing adopted with human approval and recorded in
    `Plane/15_domain_systems_sequencing.md`: flow-field/pathfinding already done via ENG-002;
    colony slice ordered ENG-001 -> ENG-003 -> ENG-031 -> ENG-004 -> ENG-032; storyteller
    incidents = ENG-016. PROC-003 card moved to `.claude/specs/done/`; roadmap row, recommended
    order, and notes updated.
- DECISIONS:
  - Adopted chain after ENG-020: ENG-010 -> ENG-016 -> PROC-007 -> ENG-021 -> ENG-029 -> ENG-012
    -> ENG-007 -> ENG-018. Unique successor: ENG-010 (status effects framework), the only
    remaining backlog card backed by a named game FR (MyTD FR-007).
  - PROC-003 acceptance criterion (b) amended by owner: `vision:*` demand tags count as demand
    where no named game FR exists yet.
  - PROC-007 scheduled before ENG-021 because ENG-010 and ENG-021 bump the save codec.
  - Demand tags corrected: `mytd` removed from ENG-012/021/022/029 as unbacked by the MyTD spec
    bundle (verified against `D:/Pet/MyTD/spec/requirements.md`).
- NEXT:
  - Run `/me --feature --next` for ENG-010 (status effects framework).
- BLOCKERS:
  - None new. Colony slice and storyteller work stay vision-only: re-entry trigger is MySD Gate 1
    evidence via PROC-015 or an authored colony game spec.
- VERIFICATION:
  - Documentation-only close-out; no engine code changed. Card status/location consistency is
    covered by the PROC-013 board checker.

## ENG-010 Close-out (2026-08-01)

- DONE:
  - Added optional content-defined slow/DoT effects with magnitude, duration, stacking rules,
    tower references, loader validation, and focused fixtures.
  - Added generic `StatusEffectComponent` state to entities and deterministic defense lifecycle:
    old DoT/expire -> tower damage -> effect application -> movement; slow modifiers use capped
    integer percentage math and DoT kills use the existing reward deposit path.
  - Added sorted snapshot/render effect tags and `SandboxSaveCodec` v9 with v1-v8 empty-effect
    migration and v9 active-effect roundtrip.
  - Moved `.claude/specs/backlog/ENG-010-status-effects.md` to
    `.claude/specs/done/ENG-010-status-effects.md`; roadmap and Plane status are synced.
- DECISIONS:
  - No ADR: implementation is additive and preserves Android-free simulation, snapshot-only
    rendering, external content, and versioned saves. Default packs and balance values remain
    unchanged.
- NEXT:
  - Run `/me --feature --next` for ENG-016 (incident execution pipeline + RNG fix).
- BLOCKERS:
  - Save-compat reviewer passed; renderer review passed after the snapshot tag defensive-copy fix.
    The simulation review's non-enemy DoT metrics finding was fixed and covered by a regression
    test; integer-floor partial slow remains an intentional scope decision with a focused test.
    Other conditional reviewers/final verifier could not all be spawned because the app
    subagent-thread limit was reached.
  - Device/emulator smoke and FrameMetrics/JankStats profiling remain manual-pending, consistent
    with earlier Android follow-ups.
- VERIFICATION:
  - Full `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation, replay, save-compat,
    benchmark, `.\gradlew.bat :android:assembleDebug`, focused content/defense/render/sandbox
    tests, and `git diff --check` passed.

## ENG-016 Close-out (2026-08-02)

- DONE:
  - Closed the incident execution pipeline: optional incident content now supports cadence start/end,
    pacing threat windows, cooldowns, and typed `spawn_wave`, `resource_event`, and `modifier` effects.
  - The stateful deterministic director consumes a persistent simulation RNG cursor; the atomic
    sandbox interpreter preflights references, capacity, and overflow before mutation. Repeated
    resource/modifier effects aggregate via `Long` before overflow checks, and cross-field failures
    report `ContentValidationError` diagnostics with the incident field path.
  - `SandboxSaveCodec` v10 persists RNG cursor, director state, executions, and active modifiers with
    v1-v9 migration. The card moved to `.claude/specs/done/`, the properties schema was completed,
    and the roadmap/Plane/DIGEST/handoff are synced.
- DECISIONS:
  - No ADR: the change stays within existing Android-free simulation, data-driven content, stable-hash,
    and versioned-save boundaries. Default pack balance remains unchanged; no renderer/input or
    Android production behavior was changed by this documentation close-out.
  - Remediation rerun metrics supersede the initial first-run `614/120 ms` values.
- NEXT:
  - Run `/me --feature --next` for PROC-007 (save migration matrix), then continue
    `ENG-021 -> ENG-029 -> ENG-012 -> ENG-007 -> ENG-018`.
- BLOCKERS:
  - No ENG-016 implementation blocker remains. No device proof is claimed; existing Android/device,
    FrameMetrics/JankStats, and other manual performance follow-ups remain pending.
  - Gradle requires process-local `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.
- VERIFICATION:
  - Full Gradle test/projects/content/replay/save-compat/benchmark/diff-check lanes passed; focused
    `SandboxIncidentTest` and content tests passed; simulation/save reviews passed.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
  - Remediation benchmark: `sim=418 ms`, `kill=85 ms`, `spatial-index-1k=6.1036 ms`,
    `goal-field=10.427 ms`.

## PROC-007 Close-out (2026-08-02)

- DONE:
  - Added checked-in Java Properties fixtures for every released sandbox save version v1-v10.
  - Added `SandboxSaveMigrationMatrixTest` with an independently constructed canonical state
    oracle, meaningful non-default state fields, stable-hash comparison, and repeated-decode
    determinism assertions.
  - Extended `scripts/me-save-compat.ps1` to run and report the migration matrix.
- DECISIONS:
  - No ADR: this is a test/process gate strengthening existing versioned-save behavior; production
    `SandboxSaveCodec` and `SAVE_VERSION=10` are unchanged.
- NEXT:
  - Run `/me --feature --next` for ENG-021 (save slots + autosave policy).
- BLOCKERS:
  - No PROC-007 implementation blocker. Existing Android/device and FrameMetrics/JankStats
    follow-ups remain manual-pending and are outside this test-only scope.
- VERIFICATION:
  - Focused matrix, full Gradle tests, projects, content validation, replay, save-compat, benchmark,
    `git diff --check`, save-compat review, and final verifier passed.
  - Replay hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`; matrix passed on two runs.

## ENG-021 Close-out (2026-08-02)

- DONE:
  - Accepted named slots under the separate `slots/` namespace and config-driven rotating
    autosaves under `autosave/`.
  - Writes use a flushed temporary file and `ATOMIC_MOVE` with no non-atomic fallback; an
    unsuccessful atomic replacement leaves the previous slot intact.
  - Slot metadata is readable without decoding the authoritative state. Corruption-only fallback
    selects the latest good autosave; incompatible or future saves are rejected rather than
    treated as corruption.
  - `SandboxSaveCodec.SAVE_VERSION` remains 10 and the Android Bundle lifecycle save path is
    unchanged.
- DECISIONS:
  - No ADR: the feature stays at the existing Android-free save boundary and adds no dependency,
    codec-version, content-schema, or lifecycle-path change.
- NEXT:
  - Run `/me --feature --next` for ENG-029 (audio event hooks), then continue
    `ENG-012 -> ENG-007 -> ENG-018`.
- BLOCKERS:
  - No ENG-021 implementation blocker remains. The pre-existing low manual device/emulator
    Bundle save/restore smoke limitation remains; the Android Bundle path was preserved and no
    device proof is claimed.
- VERIFICATION:
  - Full tests, projects, content validation, replay, save-compat matrix, benchmark, Android
    assemble, focused `SandboxSaveSlotsTest`, and `git diff --check` all passed.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Benchmark: `sim=473 ms`, `kill=87 ms`, `spatial-index-1k=6.6675 ms`,
    `goal-field rebuild=10459200 ns`.

## ENG-029 Close-out (2026-08-02)

- DONE:
  - Accepted the transient immutable `GameplayEvent` feed on the latest completed snapshot tick
    for `shot`, `hit`, `death`, `wave-start`, `build`, and `sell`, with deterministic tick/ordinal
    ordering and no authoritative-state ownership.
  - Accepted optional `sounds.properties` event-id -> pack-relative file mappings with validation
    for normalized ids, duplicates, blank paths, root escapes, and missing/non-regular files.
  - Accepted the Android `SoundPool` presentation consumer with cursor deduplication and
    presentation-only volume/mute state. `SandboxSaveCodec.SAVE_VERSION` remains 10; events do not
    affect saves or stable hashes.
- DECISIONS:
  - No ADR and no plugin/skill or canonical agent-contract change. The simulation remains
    Android/audio-free; audio decoding and playback stay in the Android presentation boundary.
- NEXT:
  - Run `/me --feature --next` for ENG-012 (boss/elite enemies + wave modifiers), then continue
    `ENG-007 -> ENG-018`.
- BLOCKERS:
  - No ENG-029 implementation blocker remains. No real device/emulator SoundPool playback,
    volume/mute, or frame-metrics evidence was available. The Android reviewer contract was
    unavailable; local boundary and gate evidence passed.
- VERIFICATION:
  - Selfcheck, 160 focused core/content/sandbox/Android tests, full `.\gradlew.bat test`,
    `.\gradlew.bat projects`, content validation (2 packs), replay, save-compat v10 matrix,
    benchmark, `:android:assembleDebug`, and `git diff --check` passed.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
  - Benchmark: `sim=412 ms`, `kill=83 ms`, `spatial-index-1k=5.62 ms`,
    `goal-field=9947100 ns`.
  - The initial invalid `:android:test --tests` invocation was corrected to
    `:android:testDebugUnitTest --tests`; it is not a feature failure.

## ENG-012 Close-out (2026-08-02)

- DONE:
  - Added validated data-defined elite/boss ranks and enemy health/speed/reward scaling.
  - Added indexed wave modifiers with consecutive authored-spawn coverage and deterministic
    effective stats. Persisted `EnemyComponent` state carries effective movement, core damage,
    reward, and rank data through restore; boss identity crosses the immutable render boundary.
  - Bumped `SandboxSaveCodec.SAVE_VERSION` to 11 with v1-v10 migration fallback and added
    replay/save continuation, content, render, and balance-report coverage.
- DECISIONS:
  - No ADR. Default content remains unchanged; effective state is persisted when rank/scaling or
    wave-modifier data is non-default, while legacy entities preserve canonical hashes.
- NEXT:
  - Run `/me --feature --next` for ENG-011 (enemy armor + damage types), then continue `ENG-018`.
- BLOCKERS:
  - No implementation blocker remains. Conditional reviewer agents were unavailable after repeated
    thread timeouts; local simulation/render/save/content boundary review passed. No device/emulator
    proof is claimed beyond the Android assemble gate.
- VERIFICATION:
  - Selfcheck, full `test`, `projects`, content validation, replay, save-compat v1-v11 matrix,
    benchmark, `:android:assembleDebug`, focused tests, and `git diff --check` passed.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Telemetry recorded event 45; `me-retro.ps1` ran with no proposal auto-applied.

## ENG-007 Close-out (2026-08-02)

- DONE:
  - Added optional `WaveContent.spawnSelection` with default/all and pipe-separated named spawn
    selection, cross-reference/reachability validation, reserved map spawn-id guards, and a
    checked-in multi-spawn fixture.
  - Routed scheduled, early, and incident waves in deterministic sorted spawn-id -> authored
    `WaveSpawn` -> instance order. Save version remains 11.
- DECISIONS:
  - No ADR. The feature is additive at the content/runtime boundary and preserves the existing
    save format and deterministic replay contract.
- NEXT:
  - Run `/me --feature --next` for ENG-018. ENG-011 remains the separate armor + damage types card.
- BLOCKERS:
  - No implementation blocker remains. The only low finding is the pre-existing Gradle 10
    deprecation warning; no device/emulator claim is made beyond `assembleDebug`.
- VERIFICATION:
  - Runner gates passed: full tests, projects, content validation (2 existing packs plus the
    checked-in multi-spawn fixture), replay, save-compat v1-v11 matrix plus `SandboxMultiSpawnTest`,
    benchmark, `:android:assembleDebug`, and `git diff --check`.
  - Canonical replay hashes: `e4892bcc18f9d8dc` / `a763da4ac32b15b4`.
  - Benchmark: `sim_ms=364`, `kill_sim_ms=87`, `goal_field_rebuild_ns=9048200`,
    `spatial_index_1k_ms=5.7197`. Tester remediation focused tests: 59 passed. Simulation,
    save-compat, and verifier reviews passed.
## ENG-018 Close-out (2026-08-02)

- DONE: Added validated `endless.properties` content, deterministic shared-RNG wave generation,
  content-defined count/health/reward growth, no-win validation, overflow-safe effective enemy
  state, and `endless-scaling` JSON output.
- DECISIONS: No ADR; this is an additive Android-free simulation/content feature. Save format stays
  at `SandboxSaveCodec.SAVE_VERSION=11`; generated effective reward/health state is retained on
  entities for mid-wave restore.
- NEXT: Run `/me --feature --next` for ENG-011 (enemy armor + damage types).
- BLOCKERS: No implementation blocker. Conditional simulation reviewer and final verifier workers
  timed out; local boundary review and complete runner gates passed. No device/emulator claim.
- VERIFICATION: Full tests, projects, content validation, replay, save-compat matrix, benchmark,
  focused generator/loader/defense/sandbox/devtools tests, and `git diff --check` passed. Replay
  hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`.

## ENG-032 Close-out (2026-08-02)

- DONE: Added place/cancel blueprint commands, a source-aware construction-site ledger, and
  construction haul destinations using the existing ENG-004 hauling path. Blueprints are
  non-blocking until completion; placement performs prospective route-safety validation, while
  completion occupies the tile, spawns the building, and rebuilds the goal field.
- DECISIONS: Source selection is automatic and deterministic by ascending `sourceId`, with retry
  when the selected source is unavailable. Cancel returns delivered and in-transit materials to
  their original `HaulSourceStore`, releases reservations, and does not credit global Inventory.
  One existing `BuildingContent` material cost is used; optional `buildWorkTicks` defaults to 1.
  No ADR, game-bundle traceability update, or plugin/skill/pipeline contract change was needed.
- NEXT: ENG-032 is complete. ENG-033 is the next candidate, but colony demand remains vision-only
  until MySD Gate 1 or an authored colony game spec supplies named FRs.
- BLOCKERS: No implementation blocker. Conditional reviewer subagents timed out after bounded
  waits; local boundary review and all runner gates passed. No device/emulator or visual-golden
  proof is claimed beyond `:android:assembleDebug`.
- VERIFICATION: Full `gradlew test`, `projects`, content validation, replay, save-compat v1-v16
  checks, benchmark, selfcheck, focused construction tests, Android assembleDebug, and
  `git diff --check` passed. `SandboxSaveCodec` is v16; v1-v15 saves migrate with empty
  construction state.
