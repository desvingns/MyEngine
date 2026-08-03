# MyEngine Engine Roadmap

Aggregated engine gaps across all games, with demand counts. Rules:
`docs/agentic/SPEC_BOARD.md` (Engine Roadmap, Gap Dedup Rule). A gap demanded by
two or more games outranks single-game gaps of the same severity.

Updated by: the `/me-spec` backlog bridge (new gaps / new demand) and `/me`
close-out (status changes).

Last updated: 2026-08-03 (ENG-025 close-out)

## Capabilities

| Capability | Cards | Demanded by | Demand | Status |
|---|---|---|---:|---|
| Cross-repo composite build + pinned engine revision | PROC-002 / ADR-0004 | process, mysd | - | **done** (2026-07-18; consumer lock pins a full accepted SHA, CI checks out the same commit, Stable/Experimental/Internal usage is explicit) |
| Reusable Android-free runtime/session API | ENG-036 | mysd | 1 | backlog (extract generic descriptor/session orchestration from games/sandbox; preserve replay hashes and v1-v7 save migrations) |
| Defense kill-reward deposit into player resources | SG-002, MTD-001 | signal-garden, mytd | 2 | **done** (SG-002 implemented 2026-07-04; MTD-001 closed 2026-07-05 as duplicate; MyTD gold maps to content-defined `rewardResource`) |
| Render surface + palette (snapshot -> RenderFrame) | SG-003 (+follow-up), MTD-005 | signal-garden, mytd | 2 | **done** (MTD-005 accepted 2026-07-16: Android Canvas consumes immutable RenderFrame, MotionEvent uses InputAdapter, scoped JVM/build/replay gates pass; device smoke and performance profiling remain manual-pending) |
| Content pack authoring/validation (game pack) | SG-001 | signal-garden | 1 | done (2026-07-04) |
| Android lifecycle save smoke (incl. any-tick save via pending-CommandQueue persistence) | SG-004 (+follow-up) | signal-garden | 1 | **done** (SG-004 implemented 2026-07-04; follow-up closed 2026-07-05; `SandboxSaveCodec` v1->v2, quiescent-save precondition dropped) |
| Balance report with suspicious-value checks | SG-005 | signal-garden | 1 | **done** (SG-005 implemented 2026-07-05: devtools `balance-report`/`balance-delta` JSON compares baseline vs changed content and flags enemy/core/resource deltas) |
| Gold cost gating in placeTower | MTD-002 | mytd | 1 | **done** (2026-07-05; existing generic `tower.costResource`/`costAmount` gate verified with `SandboxTowerCostGatingTest`) |
| Tower upgrade hook | MTD-003 | mytd | 1 | **done** (2026-07-05; content-defined upgrade tiers, `UpgradeTowerCommand`, deterministic spend/reject, save v3 branch+tier persistence) |
| Difficulty modifiers | MTD-004 | mytd | 1 | **done** (2026-07-16; data-defined `DifficultyContent`, deterministic pre-tick materialization, easy/normal/hard values from the MyTD balance plan) |
| Command DTO relocation out of engine-render + InputAdapter state fix | ENG-024 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; DTOs in `engine-core` command package, explicit `InputUiState`, caller-owned `CommandId`; full test/replay/save-compat/Android/static gates pass; hashes unchanged) |
| Map definitions in content packs (size, terrain, spawns, core) | ENG-005 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; DX-008 hybrid JSON map asset, validated `maps.json`, data-driven sandbox world/routing, v4 map/content save identity, canonical hashes unchanged) |
| Win/lose conditions + run summary | ENG-014 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; map-defined terminal rules, immutable snapshot summary, save v5 terminal-state persistence, all gates pass) |
| Android SurfaceView renderer + Choreographer fixed-tick loop | ENG-026 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; 20 Hz Android-local Choreographer policy, immutable RenderFrame SurfaceView, command-queue-only input, pause/save/Bundle command-ID restoration; JVM/build/replay/save-compat gates pass, device/performance checks manual-pending) |
| HUD snapshot data + UI command surface | ENG-027 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-18; content-derived immutable HUD snapshot, Android build/select/upgrade panels through `InputAdapter`, deterministic per-tower damage/kills, save v6 metrics persistence; full gates pass, device/layout/performance checks manual-pending) |
| Goal-field pathfinding + repath on world change (mazing) | ENG-002 | mytd, vision:td, vision:mindustry-like | 3 | **done** (2026-07-18; deterministic `GoalField`, prospective all-spawn placement rejection, same-tick reroute, v6-derived field/legacy canonicalization; replay/save/benchmark gates pass) |
| Multiple spawn points + per-wave routing | ENG-007 | vision:td, vision:mindustry-like | 2 | **done** (2026-08-02; optional validated wave spawn selection, deterministic scheduled/early/incident routing, multi-spawn fixture, replay/save coverage; `SAVE_VERSION` remains 11) |
| Targeting priority modes | ENG-008 | mytd, vision:td | 2 | **done** (2026-07-18; pure deterministic first/last/nearest/strongest/weakest selector with entity-id tiebreak, content default + queued per-tower override, HUD projection, and save v7 migration) |
| Splash damage + shot events | ENG-009 | mytd, vision:td, vision:mindustry-like | 3 | **done** (2026-07-28; stable entity-id-ordered Manhattan AoE, integer per-ring falloff, and transient immutable source/target/tick shot-hit events; save v8 and replay hashes unchanged) |
| Status effects framework | ENG-010 | mytd, vision:td, vision:mindustry-like | 3 | **done** (2026-08-01; content-defined slow/DoT, deterministic lifecycle, movement/damage modifiers, save v9 migration, immutable snapshot tags, and stable slow replay coverage) |
| Enemy armor + damage types | ENG-011 | vision:td, vision:mindustry-like | 2 | **done** (2026-08-02; Option A typed damage content, 0..100 percentage resistances, bidirectional validation, deterministic Long/floor formula, direct/splash runtime, effective-DPS matrix, resist replay hash, and `SAVE_VERSION=11` preserved) |
| Flying enemies | ENG-025 | vision:td | 1 | **done** (2026-08-03; ground/air movement modes, blocker-ignoring air routes, tower capability filters, air-coverage balance warning, v21 save migration, mixed-wave replay/leak coverage) |
| Boss/elite enemies + wave modifiers | ENG-012 | vision:td | 1 | **done** (2026-08-02; data-defined elite/boss scaling, indexed wave modifiers, deterministic effective spawn state, boss snapshot marker, save v11 migration, replay/save/balance coverage) |
| Tower sell/refund | ENG-013 | mytd, vision:td | 2 | **done** (2026-07-18; required validated `sellRefundRatio`, deterministic cumulative per-resource floor refund, atomic capacity rejection, occupancy/metrics cleanup, same-tick goal-field rebuild, and pending-sell save coverage; `SAVE_VERSION` remains 6) |
| Game speed control (presentation-side) | ENG-015 | mytd, vision:td, vision:rimworld-like | 3 | **done** (2026-07-21; Android-local 0x/1x/2x/4x pacing and HUD controls preserve fixed-tick simulation, per-tick trajectory parity, save/replay boundaries, and all verifier boundary checks; device/instrumentation and FrameMetrics/JankStats remain pending) |
| Endless wave generation | ENG-018 | vision:td, vision:mindustry-like | 2 | **done** (2026-08-02; content-defined endless schedule, deterministic RNG generation, no-win validation, and scaling-table report) |
| Walls + player-placed blockers | ENG-019 | vision:td, vision:mindustry-like | 2 | **done** (2026-08-02; validated 1x1 wall content, atomic place/remove commands with prospective path rejection and refund, immutable snapshot health, save v12/v1-v11 migration, forced-corridor replay and full gates) |
| Spatial index + 1k-entity benchmark | ENG-020 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-29; internal non-persisted grid index powers targeting/splash candidate queries, and the deterministic 1024-enemy benchmark reports `5.3045 ms`) |
| Save slots + autosave policy | ENG-021 | vision:td, vision:rimworld-like, vision:mindustry-like | 3 | **done** (2026-08-02; named `slots/`, rotating `autosave/`, atomic writes, metadata-only inspection, corruption-only fallback, codec v10 and Android Bundle path preserved) |
| Meta-progression store | ENG-022 | vision:td | 1 | backlog |
| Sprite/atlas references in content schema | ENG-028 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-28; validated opaque refs for tiles/towers/tower tiers/enemies/buildings, pack-relative file/atlas-key checks, deterministic palette fallback, desktop/Android consumers, and replay/save boundaries unchanged) |
| Audio event hooks (snapshot event feed) | ENG-029 | vision:td, vision:rimworld-like, vision:mindustry-like | 3 | **done** (2026-08-02; transient deterministic `GameplayEvent` feed, optional `sounds.properties` file validation, Android `SoundPool` consumer, no save-version/hash change) |
| Wave preview + early wave call | ENG-030 | mytd, vision:td | 2 | **done** (2026-07-21; typed early-call command, deterministic HUD composition/countdown, content-defined bonus validation, SAVE_VERSION v8 migration, replay/save/gate verification pass; balance review partial: current packs valid/no hardcoded bonus, schema gap closed in docs close-out, optional bonus unconfigured pending approved balance value) |
| A* point-to-point pathfinding for agents | ENG-001 | vision:rimworld-like, vision:mindustry-like | 2 | **done** (2026-08-02; deterministic 4-neighbor integer-cost A*, stable tie/neighbor/predecessor ordering, API-preserving GridPathfinder delegation, AgentPathPlanner repaths, focused/full gates pass; JobBoard/job-actor tick wiring is delivered by ENG-003, with hauling MVP remaining in ENG-004) |
| Job execution system (JobBoard wired into tick) | ENG-003 | vision:rimworld-like, vision:mindustry-like | 2 | **done** (2026-08-02; deterministic v13 job execution with worker assignment/lifecycle, pathfinding movement, work ticks, typed resource-delta effects, invalid-target release, save migration, and replay/full-gate verification) |
| First worker agent MVP (hauling) | ENG-004 | vision:rimworld-like, vision:mindustry-like | 2 | **done** (2026-08-02; data-defined worker speed/capacity, deterministic source reservations, source-to-stockpile carry/deposit, positioned producer outputs, stockpile contents, v15 save with v1-v14 migration, and full gates pass) |
| Stockpile zones + designations | ENG-031 | vision:rimworld-like, vision:mindustry-like | 2 | **done** (2026-08-02; accepted Option A: deterministic zone commands/store, validated resource filters, one-shot harvest-node JobBoard jobs, immutable snapshot projection, v14 save with v1-v13 migration; hauling, quantities/capacity, depletion/repeated harvest, and Android overlay consumption deferred) |
| Construction system (blueprint, haul, build) | ENG-032 | vision:rimworld-like, vision:mindustry-like | 2 | **done** (2026-08-02; non-blocking blueprints, deterministic sourceId-ordered hauling/retry, build jobs, source refunds on cancel, save v16 with v1-v15 migration, and full gates pass) |
| Colonist needs MVP (hunger/rest) | ENG-033 | authored-colony-scope | 1 | **done** (2026-08-03; content-defined decay/thresholds, deterministic eat/sleep jobs and arbitration, immutable HUD bars, save v17 with v1-v16 migration, 10k determinism coverage) |
| Incident execution pipeline + RNG fix | ENG-016 | vision:rimworld-like, vision:mindustry-like, vision:td | 3 | **done** (2026-08-02; stateful deterministic director with persistent RNG cursor, cadence/pacing/cooldown selection, atomic typed spawn-wave/resource-event/modifier interpreter, v10 save with v1-v9 migration, remediation replay/save/overflow diagnostics and benchmark gates pass) |
| Research/tech tree + unlock gating | ENG-017 | vision:mindustry-like, vision:td | 2 | **done** (2026-08-03; optional validated `tech-tree.json`, deterministic atomic research and unlock gating, immutable snapshot, save v18/v1-v17 migration, replay/save/content/full-gate verification) |
| Seeded procedural map generation | ENG-006 | vision:rimworld-like, vision:mindustry-like, vision:td | 3 | **done** (2026-08-03; bounded deterministic generator from validated content params, guaranteed spawn-to-core path, ASCII devtools report, seed-preserving sandbox save/reload) |
| Enemy attacks on structures | ENG-034 | vision:mindustry-like | 1 | **done** (2026-08-03; content-flagged blocked enemy attacks use stable adjacent-structure selection, lethal occupancy/GoalField invalidation, persisted building health, and balance-report structure metrics) |
| Resource extractor building | ENG-035 | vision:mindustry-like, vision:rimworld-like | 2 | **done** (2026-08-03; deterministic finite/infinite extractor nodes, output-only ProducerSystem path, ENG-004 haul-source output, and save v19/v1-v18 migration; ENG-023 belt transport remains separate) |
| Conveyor transport MVP | ENG-023 | vision:mindustry-like | 1 | **done** (2026-08-03; deterministic straight/corner belts, content-defined ticks-per-cell, producer/core/entity endpoints, v20 save migration, replay and 100-belt benchmark) |
| New-game scaffolder script | DX-001 | agent pipeline | - | backlog |
| Headless state inspector (agent eyes) | DX-002 | agent pipeline | - | **done** (2026-08-03; provider-based deterministic ASCII/JSON inspector, sandbox ServiceLoader adapter, optional command scripts, focused/full gates) |
| Replay divergence bisector | DX-003 | agent pipeline | - | backlog |
| Desktop content hot-reload | DX-004 | agent pipeline | - | backlog |
| Schema-docs drift gate | DX-005 | agent pipeline | - | **done** (2026-08-03; deterministic ContentLoader/properties-schema drift report, bidirectional fixtures, selfcheck and pre-push wiring) |
| Engine cookbook (agent task recipes) | DX-006 | agent pipeline | - | **done** (2026-08-03; `docs/COOKBOOK.md` has five on-demand recipes with exact file lists, gates, and historical commit references; `AGENTS.md` links it without adding it to always-loaded intake) |
| Fuzz tests for ContentLoader + SaveCodec | DX-007 | agent pipeline | - | backlog |
| ADR: JSON vs properties content format | DX-008 | agent pipeline | - | **done** (2026-07-16; ADR-0003 accepts `.properties` for flat definitions and JSON for nested assets) |
| Spec board backsync | PROC-001 | process | - | backlog |
| Domain roadmap sequencing | PROC-003 | process | - | **done** (2026-07-29; Plane/15 sequences flow-field [done], colony slice, storyteller; vision:* accepted as demand by owner amendment; successor ENG-010 adopted) |
| Performance budgets | PROC-004 | process | - | backlog |
| Golden replay hashes | PROC-005 | process | - | backlog |
| CI pre-push lane | PROC-006 | process | - | **done** (2026-08-03; `.githooks/pre-push` runs tests, content validation, replay, save compatibility, schema drift and emits one blocking JSON result) |
| Save migration matrix | PROC-007 | process | - | **done** (2026-08-02; checked-in v1-v10 fixtures, independent stable-hash migration matrix, and save-compat JSON result) |
| Playtest bot | PROC-008 | process | - | backlog |
| Android visual smoke | PROC-009 | process | - | backlog |
| Cost telemetry | PROC-010 | process | - | backlog |
| Codex adapter parity audit + selfcheck coverage | PROC-011 | process | - | backlog |
| Emulator provisioning lane (managed devices) | PROC-012 | process | - | backlog |
| Spec board hygiene | PROC-013 | process | - | **done** (2026-07-29; Variant B migrated 23 cards and wired board checker into selfcheck) |
| Android release build lane | PROC-014 | process | - | backlog |
| Reference-game evidence bridge for me-spec | PROC-015 | process, mysd | - | backlog (state-graph.v1 + mechanic claims, clone-strict coverage, traceability, and gap dedup) |

## Known duplicates

- **MTD-001 duplicates SG-002** (reward deposit): SG-002 shipped in `DefenseRuntime`
  (TowerUpdateResult -> inventory deposit). MTD-001 is closed by reference; MyTD's gold balance is
  a content-defined resource, not a separate engine concept.

## Notes

- Statuses in `.claude/specs/backlog/*.md` cards may lag reality (SG-001..003 cards
  still said `backlog` after implementation). Close-out now flips card status;
  see `docs/agentic/PIPELINE.md` step 7. (SG-001..003 statuses flipped to done in the
  2026-07-06 review; directory moves deferred to PROC-013.)
- `vision:*` tags (2026-07-06 gap sweep) mark demand from the target-genre vision
  (RimWorld-like / Infinitode+BlockDefense TD / Mindustry-like) rather than an existing
  game repo; each vision direction counts as 1 in Demand.
- The 2026-07-06 gap sweep (ENG-001..035, DX-001..008, PROC-011..014; see
  `docs/reviews/2026-07-06-project-and-backlog-review.md`) materializes the sequencing
  PROC-003 asks for; PROC-003 may be closed by reference once the recommended order
  below is adopted.
- Demand tags for ENG-012/021/022/029 corrected 2026-07-29: the `mytd` tag was unbacked
  by the MyTD spec bundle (verified against `D:/Pet/MyTD/spec/requirements.md`).

## Recommended order (2026-07-06 review)

1. Human: commit the in-flight MTD-003 close-out changes; then run PROC-013 (board hygiene).
2. MTD-004 -> ENG-024 -> ENG-005 -> ENG-014 -> MTD-005 -> ENG-026 -> ENG-027 =
   **first playable Android TD milestone** (complete 2026-07-18; manual device/layout/performance
   evidence remains explicitly pending).
3. P1 opener: ENG-002 (done) -> ENG-013 (done) -> ENG-008 (done) -> ENG-015 (done) -> ENG-030
   (done 2026-07-21) -> ENG-028 (done 2026-07-28) -> ENG-009 (done 2026-07-28) -> ENG-020
   (done 2026-07-29). PROC-003 sequencing adopted 2026-07-29: ENG-010 -> ENG-016 (done 2026-08-02)
   -> PROC-007 (done 2026-08-02) -> ENG-021 (done 2026-08-02) -> ENG-029 (done 2026-08-02)
   -> ENG-012 (done 2026-08-02) -> ENG-007 (done 2026-08-02) -> ENG-018 (done 2026-08-02)
   -> ENG-011 (done 2026-08-02) -> ENG-019 (done 2026-08-02) -> ENG-001 (done 2026-08-02)
   -> ENG-003 (done 2026-08-02) -> ENG-031 (done 2026-08-02) -> ENG-004 (done 2026-08-02)
   -> ENG-032 (done 2026-08-02) -> ENG-033 (done 2026-08-03) -> ENG-017 (done 2026-08-03)
   -> ENG-025 (done 2026-08-03).
   ENG-001, ENG-003, ENG-031, ENG-004, ENG-032, ENG-033, ENG-017, and ENG-025 are complete. MySD TD evidence is
   not used as colony evidence.
4. DX-008 is done: use its hybrid-format ADR for ENG-017/ENG-028 schema work. DX-002, DX-006,
   DX-005, and PROC-006 are complete (2026-08-03). Post-DX-006 recommendation: review the
   remaining accepted backlog and assign owner, blocked_by, and start gates before selecting another
   card; ENG-036 and PROC-015 remain explicitly human-owned. ENG-025 is now closed.
5. MySD foundation: PROC-002 / ADR-0004 is done. MySD Gate 1/relaxed Gate 2 are accepted for the
   TD reference bundle, but no colony demand is inferred from that bundle. ENG-033 is unlocked by
   its separate authored scope; future MySD demand still bridges only through PROC-015 semantics.

## Deliberately not carded (2026-07-06, bounded scope)

Power grid, fluid transport, unit production/control (Mindustry deep-end);
temperature/roofing, animals, trading (RimWorld deep-end); achievements/quests,
tutorials (game-side, not engine). Revisit when a game spec demands them.

MySD currently has probable families around production buildings, allied mobile units, in-run
drafts, campaign/energy/sweep, and roster/profile progression. They remain deliberately uncarded
until each family has accepted evidence and deduplication; do not increment `mysd` demand from the
public listing or from the TD Gate 1 bundle alone.
