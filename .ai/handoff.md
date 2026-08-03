# MyEngine Handoff

Last updated: 2026-08-03 (ENG-033 close-out; next backlog review)
Owner: Codex

## DONE

- ENG-033 is complete (2026-08-03): added optional content-defined hunger/rest policies,
  deterministic threshold recovery jobs with target-owned effects and stable need-vs-work arbitration,
  immutable HUD need bars, and `SandboxSaveCodec` v17 with v1-v16 migration. The card is in
  `.claude/specs/done/`; MySD TD Gate 1 remains outside the colony evidence boundary.

## ENG-033 close-out

### DONE

- `NeedContent`/`needs.properties` validates decay, threshold, recovery, job type, priority, and HUD
  display keys. `NeedsComponent` is authoritative entity state; `NeedsSystem` decays in entity/need
  order and creates one deterministic job per threshold episode.
- `NeedRecovery` is a typed completion effect targeting the originating colonist, so another worker
  may execute the job without applying recovery to the wrong entity. Sandbox HUD projects sorted
  `HudNeedBar` values through the immutable snapshot path.
- Save v17 appends need levels/trigger counters to entity records and accepts v1-v16 with empty need
  state; canonical legacy replay hashes remain unchanged when no needs state exists.

### DECISIONS

- Need values use a bounded 0..100 scale. Sandbox content uses hunger/rest thresholds at 25 with
  content priorities 100/90; generic jobs with equal priority remain stable by job id.
- No ADR or game-bundle traceability update was needed. No Android production code changed; the
  Android unit assertion was synchronized to v17. No plugin/skill/pipeline contract changed.

### NEXT

- Review the remaining accepted backlog and choose the next `/me --feature --next` candidate.

### BLOCKERS

- No implementation blocker. The delegated scout/architect and conditional reviewer workers timed
  out after bounded waits; local implementation and boundary review completed successfully. No
  device/emulator or visual-golden evidence is claimed beyond `:android:assembleDebug`.

### VERIFICATION

- Full `gradlew test`, `gradlew projects`, content validation, replay, save-compat, benchmark,
  `:android:assembleDebug`, selfcheck, focused content/needs/sandbox tests, and `git diff --check`
  passed. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, `3f02607020d48668`.
- Benchmark: canonical 437 ms, kill 93 ms, GoalField rebuild 7.3769 ms, spatial index 7.7136 ms.

- ENG-019 is done: the 1x1 wall MVP is content-defined and localized with validated cost, health,
  footprint, visual reference, and refund metadata. Render-free place/remove commands perform atomic
  resource and occupancy updates, reject prospective all-spawn path blocks, rebuild routing for
  same-tick movement, expose immutable snapshot health, and persist through save v12 with v1-v11
  migration and pending building-command restoration. The approved balance is 2 bolt, 20 HP, and
  a 50% sell refund. Tower cost validation remains non-negative for compatibility; wall costs are
  strictly positive and zero-cost wall content returns a structured validation error.

- ENG-019 verification: final runner 9/9 passed. Replay hashes are
  `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, and `3f02607020d48668`; save-compat includes the v12
  building fixture and v1-v11 migration matrix. Benchmark evidence includes GoalField 64x64 with
  3,844 reachable tiles and 11,813,800 ns rebuild, plus 1,024 enemies / 16 towers / 16 queries /
  16 shots at 5.201 ms. Canonical and kill scenarios ran 35 ticks at 431 ms and 79 ms. Android
  evidence is assembleDebug only; no device/emulator or frame-budget claim is made. No ADR was
  needed.

- ENG-001 is done: `engine-world` now provides deterministic 4-neighbor uniform integer-cost A*
  with stable open-set/neighbor/predecessor ordering, bounds/blocked/no-path handling, and
  optional occupied-start support. `engine-ai` keeps the `PathRequest`/`PathResult` API through
  A* and adds deterministic `AgentPathPlanner` repaths for valid stored `MovementComponent` paths.
  ENG-003 now provides the full JobBoard/job-actor tick integration; the first worker hauling MVP
  remains ENG-004. Wave enemies remain on ENG-002 `GoalField`.

- ENG-003 is done: the Android-free `JobExecutionSystem` is wired into the sandbox fixed-tick
  pipeline. Positioned `JobActorComponent` entities claim jobs in deterministic worker/entity and
  priority/job-id order; `CLAIMED -> IN_PROGRESS -> DONE/FAILED` lifecycle, pathfinding movement,
  work ticks, typed `resource_delta` completion effects, invalid-target/no-path release, reservation
  guards, and same-tick reclaim prevention are covered. `SandboxSaveCodec` v13 persists JobBoard,
  in-flight jobs, actor assignment/progress, and effects with v12 migration to empty job state.

- ENG-031 is done: accepted Option A adds deterministic zone commands/store state, validated stockpile
  resource filters, one-shot harvest-node designation jobs, immutable `EngineSnapshot.zones`, and
  `SandboxSaveCodec` v14 with v1-v13 migration. ENG-004 now adds hauling and persisted stockpile
  contents; generic stockpile capacity, depletion/repeated harvest, and actual Android overlay
  consumption remain follow-up scope.

- ENG-004 is done: optional `WorkerContent` defines speed/capacity; `HaulSourceStore` reserves
  source quantities by job id; typed haul jobs move `InventoryComponent` carry to validated stockpile
  tiles at a content-defined speed; positioned ProducerSystem outputs become sources; stockpile
  contents and worker carry are included in deterministic state/save v15. Legacy generic jobs and
  replay hashes remain compatible when no new haul state is present.

- ENG-029 is done: the latest completed snapshot tick exposes a deterministic transient immutable
  `GameplayEvent` feed for shot, hit, death, wave-start, build, and sell events. Optional
  `sounds.properties` maps normalized event ids to pack-relative files with validation, and Android
  `SoundPoolPresentationConsumer` consumes the feed with presentation-only volume/mute state.
  `SandboxSaveCodec.SAVE_VERSION` remains 10; events do not enter authoritative saves or stable
  hashes.

- ENG-012 is done: `EnemyContent` supports validated elite/boss rank flags and deterministic
  health/speed/reward scaling; `WaveContent` supports indexed modifiers with consecutive-enemy
  coverage. Effective spawn state is persisted in `EnemyComponent`, bosses are marked in immutable
  snapshots/render primitives, balance reports use effective stats, and sandbox saves are v11 with
  v1-v10 migration fallback.

- ENG-007 is done: optional `WaveContent.spawnSelection` supports default/all or pipe-separated
  named spawn ids with cross-reference/reachability validation. Scheduled, early, and incident
  waves route through deterministic sorted spawn-id -> authored `WaveSpawn` -> instance ordering;
  reserved map spawn ids are guarded and a checked-in multi-spawn fixture covers the content gate.
  `SandboxSaveCodec.SAVE_VERSION` remains 11.

- ENG-011 is done: approved Option A adds static `DamageTypeContent`, tower `damageTypeId`, enemy
  `resists` in the inclusive `0..100` range, and deterministic bidirectional reference validation.
  `DamageFormula` applies the documented Long/final-floor formula to direct and splash damage;
  zero damage emits no `HitEvent`. The deterministic effective-DPS matrix uses single-target,
  in-range, no-splash, `ticks_per_second=20` assumptions. `SAVE_VERSION=11` remains unchanged.
  Resist replay, focused tests, and all runner gates passed.

- PROC-003 is done: `Plane/15_domain_systems_sequencing.md` sequences the domain systems —
  flow-field/pathfinding already done via ENG-002 (MyTD FR-003/FR-009/FR-013), colony slice
  ordered ENG-001 -> ENG-003 -> ENG-031 -> ENG-004 -> ENG-032 -> ENG-033 (ENG-033 now has an
  authored named-FR scope; MySD TD evidence is not used as colony evidence), storyteller incidents = ENG-016
  (vision-only demand plus defect fix F4). Card moved to `.claude/specs/done/`; roadmap row,
  recommended order, and demand tags synced.

- PROC-013 Variant B is done: 23 verified-done cards moved from `backlog/` to `done/`, PROC-013
  roadmap status reconciled, and the read-only board checker wired into `scripts/me-selfcheck.ps1`.
- PROC-002 / ADR-0004 is accepted: separate game repos use Gradle composite builds pinned to a
  full accepted MyEngine SHA, and CI checks out/verifies that same commit. Stable,
  Experimental-with-adapter, and Internal cross-repo rules are explicit.
- MySD foundation backlog bridge filed ENG-036 (Android-free reusable runtime/session extraction)
  and PROC-015 (hierarchical reference state graph + mechanic claims + clone-strict coverage and
  gap dedup). No evidence-driven `mysd` demand was added before Gate 1.

- Phase 00-03 foundation, stack scaffold, and architecture contracts.
- Phase 04 agentic pipeline:
  - `docs/agentic/PIPELINE.md`
  - `docs/agentic/AGENT_CONTRACTS.md`
  - `docs/agentic/SELF_IMPROVEMENT.md`
  - `docs/agentic/SPEC_BOARD.md`
  - Claude/Codex adapter stubs.
- Phase 05 core runtime:
  - fixed ticks, seeded RNG, command queue, events, stable hash, scenario runner.
- Phase 06 world/content/save:
  - tile world, content pack loader/validator, sample pack, sandbox save/load v1.
- Phase 07 entities/AI:
  - stable entity store, components, pathfinding, job board.
- Phase 08 gameplay systems:
  - inventories, producers, waves, enemies, towers, incident director.
- Phase 09 presentation boundary:
  - snapshots, camera, input adapter, desktop ASCII smoke, Android text shell.
- Phase 10 sandbox vertical slice:
  - 64x64 map, tower placement command, waves, enemies, generator, save/replay tests.
- Phase 11 devtools:
  - JSON scenario/balance/content/replay reports and `docs/EDITOR_PLAN.md`.
- Phase 12 game spec pipeline:
  - `docs/GAME_SPEC_PIPELINE.md`
  - `games/signal-garden/spec/` sample bundle.
- Phase 13 self-improvement:
  - telemetry record/retro scripts and synthetic retro.
- Phase 14 hardening:
  - `docs/API_STABILITY.md`
  - `docs/HARDENING_AUDIT.md`
  - `docs/RELEASE_CHECKLIST.md`
  - `games/signal-garden/ROADMAP.md`
  - first five `SG-*` backlog specs.
- Signal Garden `SG-001` (content pack, 2026-07-04):
  - New original content pack `games/signal-garden/content/signal-garden/` (9 `.properties`
    files: manifest, tiles, resources, recipes, towers, enemies, waves, incidents, strings;
    manifest `schemaVersion=1`, `id=signal-garden`).
  - New loader test `engine-content/src/test/kotlin/dev/myengine/content/SignalGardenContentPackTest.kt`
    loads the real on-disk pack via `ContentPackLoader` (resolved as a filesystem `Path`, no
    game-module import) and asserts `isValid`, manifest id, and key cross-references.
  - Ids non-overlapping with sandbox; sandbox content untouched. Pipeline
    developer -> tester -> verifier -> docs; verifier verdict pass, all four boundary_checks true.
  - Gates: `.\gradlew.bat test` -> pass; `engine-devtools:run content-report <abs path>` ->
    `{"pack_id":"signal-garden","valid":true,"errors":[]}`.
- Signal Garden `SG-002` (reward deposit hook, 2026-07-04):
  - `engine-defense/src/main/kotlin/dev/myengine/defense/DefenseRuntime.kt`: added
    `TowerUpdateResult(metrics: DefenseMetrics, rewards: Map<String,Int>)`; `updateTowers` now
    returns it and accumulates `enemy.rewardResource -> +enemy.rewardAmount` (content-derived) on
    kills; removed the old dead reward stub. DefenseRuntime does NOT mutate Inventory.
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
    (`SandboxRuntime.step`): records tower metrics, then deposits `rewards` into `state.inventory`
    in sorted-key order guarded by `canAdd`. No hardcoded resource ids.
  - Tests: `engine-defense/.../DefenseRuntimeTest.kt` updated to the new return type + added
    content-derived resource-conservation and leaked-enemy-yields-no-reward cases;
    `games/sandbox/.../SandboxRewardDepositTest.kt` end-to-end deposit test (delta ==
    enemiesKilled * rewardAmount, content-derived); `games/sandbox/.../SandboxVerticalSliceTest.kt`
    added kill-scenario replay-determinism + reward save-roundtrip cases.
  - Decision: Option A (runtime owns inventory mutation; engine-defense only returns rewards). No
    ADR because `DefenseRuntime` is Experimental per `docs/API_STABILITY.md`. Pipeline
    architect (me-architect) -> developer (me-engine-developer) -> tester (me-tester, one
    runner-caught fix to sandbox scenario tower positions) -> simulation-reviewer (pass) +
    verifier (pass, all four boundary_checks true) -> docs.
  - Gates: `.\gradlew.bat test` -> pass; `scripts\me-sim-replay.ps1` -> pass, hash
    `9c495d8ff30fd83d` UNCHANGED (canonical scenario kills 0 enemies); `scripts\me-save-compat.ps1`
    -> pass; `scripts\me-benchmark.ps1` -> pass.
- Default content gate coverage (2026-07-04):
  - `engine-devtools/.../DevtoolReports.kt`: added `repoRoot()` (walks up to the first ancestor
    holding a `games/` dir), `discoverPackRoots()` (enumerates every `games/<game>/content/<pack>`
    dir), and `contentReportAll()` returning `AggregateContentReport` (one JSON: `valid`,
    `pack_count`, `packs[]` with repo-relative root/pack_id/valid/errors, sorted by forward-slash
    root for cross-OS determinism).
  - `engine-devtools/.../DevtoolsMain.kt`: new `content-report-all` / `content-validate-all`
    command; `content-report`/`content-validate` now resolve a *relative* path arg from `repoRoot()`
    (absolute paths pass through), fixing the prior "File is missing" on module-relative paths.
  - `scripts\me-content-validate.ps1`: rewritten to invoke `content-report-all` and emit exactly
    ONE runner-contract JSON line (`status`/`command`/`exit_code`/`notes`) on pass and fail (fail
    lists invalid pack roots) — satisfies the PIPELINE.md runner-script rule. Empty/no-pack case
    fails loud (`AggregateContentReport.valid` requires a non-empty result set).
  - Test: `engine-devtools/.../DevtoolReportsTest.kt` `contentReportAllCoversEveryGamePack` asserts
    >=2 packs discovered, signal-garden included, and all valid.
  - Pipeline: developer (me-engine-developer) -> tester (me-tester) -> runner (me-runner) ->
    verifier (me-verifier, verdict pass, all four boundary_checks true; low-severity determinism
    nuance fixed by sorting on the emitted forward-slash root) -> docs.
  - Gates: `.\gradlew.bat :engine-devtools:test` -> pass; `engine-devtools:run content-report-all`
    -> `{"valid":true,"pack_count":2,"packs":[sandbox, signal-garden]}`; `content-report
    games/signal-garden/content/signal-garden` (relative) -> valid; `scripts\me-content-validate.ps1`
    -> `{"status":"pass",...,"notes":"validated 2 pack(s)"}`, exit 0.
- SG-002 kill/reward gate hardening (2026-07-04):
  - `games/sandbox/.../SandboxGame.kt`: refactored `runScriptedScenario` to a private
    `(towerPosition, seed)` helper; added public `runScriptedKillScenario()` (pulse tower at (2,2),
    step 35 -> kills). Extracted a pure `internal depositRewards(inventory, rewards): RewardDeposit`
    (sorted-key order, `canAdd`-guarded) and wired it into `SandboxRuntime.step`, which now surfaces
    `lastCommandOrError="reward_dropped:<res>:<amt>"` for any reward that overflows a set capacity.
  - `engine-devtools/.../DevtoolReports.kt` + `DevtoolsMain.kt`: `HeadlessScenarioReport` gained a
    `scenario` name; added `runSandboxKillScenario()` + `runScenarioSuite()`; `scenario`/`balance`
    now emit `{"scenarios":[canonical, kill]}`; `replay-inspect` emits both (with `enemies_killed`).
  - Tests: `DevtoolReportsTest` (kill exercises kills+shots, canonical kills nothing, suite +
    replay-inspect report both); `SandboxVerticalSliceTest.killScenarioApiKillsDeterministicallyAndDiffersFromCanonical`;
    new `SandboxRewardOverflowTest` (pure `depositRewards` add/drop + capacity-bound `step` telemetry).
  - Decision: Option A (additive second scenario), so the baseline hash `9c495d8ff30fd83d` is
    preserved; silent-drop resolved to non-fatal-but-observable telemetry (`lastCommandOrError` is
    not hashed, so replay-safe). No ADR (`DefenseRuntime`/sandbox devtools are Experimental).
    Pipeline: architect -> developer/tester -> runner -> simulation-reviewer (pass) + verifier
    (pass) -> docs.
  - Gates: `.\gradlew.bat test` -> pass; `scripts\me-sim-replay.ps1` + `scripts\me-benchmark.ps1`
    -> pass, canonical `9c495d8ff30fd83d` (kills=0) unchanged + kill `83a65da1a7881b2c`
    (enemies_killed=2, tower_shots=4); `scripts\me-save-compat.ps1` -> pass.
- Signal Garden `SG-003` (placeholder render surface, 2026-07-04):
  - New `engine-render/src/main/kotlin/dev/myengine/render/PlaceholderRenderSurface.kt`: a PURE
    projection `project(snapshot: EngineSnapshot, camera: Camera): RenderFrame`. Kinds
    `RenderKind{TILE_FLOOR,TILE_WALL,TILE_RESOURCE,CORE,TOWER,ENEMY}`;
    `RenderPrimitive(kind, tile, screen, health?)`; `RenderFrame(primitives, coreHealth, tick)`.
    Tiles emitted first (snapshot order), then entities sorted by id; unknown terrain/entity types
    skipped; screen via `camera.worldToScreen(tile center)`; ENEMY carries health, TOWER null. No
    game/Android imports; no simulation mutation — a reusable, game-agnostic engine capability.
  - Tests: `engine-render/.../RenderBoundaryTest.kt` extended with camera pan/zoom cases (pan delta
    + clamp-to-world, zoom clamp to `[0.5,4]`, scale grows with zoom) — camera pan/zoom was
    previously untested; new `engine-render/.../PlaceholderRenderSurfaceTest.kt` (six kinds present,
    enemy health carried / tower null, screen == `worldToScreen(tile center)`,
    tiles-then-entities-by-id ordering, unknown-type safety, projection purity: idempotent + source
    unchanged); new `games/sandbox/.../SandboxRenderNonMutationTest.kt` (step runtime, capture
    `stableHash`, project snapshot, assert `stableHash` unchanged — render path proven
    non-mutating).
  - Decision: reusable game-agnostic render surface, pure `snapshot -> frame` projection, not yet
    launcher-wired (desktop/Android still use `AsciiRenderer`). No ADR (engine-render presentation
    boundary is Experimental; pure projection introduces no new dependency direction).
  - Pipeline: developer `me-engine-developer` (architect skipped — scope small and well-bounded,
    like SG-001) -> tester `me-tester` -> reviewers `me-renderer-qa` (pass: snapshot boundary,
    camera math, coverage all sound) + `me-verifier` (pass: all four boundary_checks true,
    render_snapshot_only confirmed) -> docs.
  - Gates: `.\gradlew.bat test` -> pass (full suite incl. new render + sandbox tests);
    `.\gradlew.bat desktop:run` -> pass (exit 0; ASCII map unaffected); telemetry recorded via
    `scripts\me-record-run.ps1` (events=4).
- Signal Garden `SG-003 follow-up` (RenderFrame consumed in a real desktop launcher + pixel-smoke,
  2026-07-04):
  - New `engine-render/src/main/kotlin/dev/myengine/render/RenderPalette.kt`: pure, Android-safe
    (NO java.awt/android imports — verified by grep + `android:assembleDebug`). `data class
    Rgb(r,g,b)` with `toRgbInt()` (packs 0xRRGGBB); `object RenderPalette` maps each of the six
    `RenderKind`s to a distinct color (floor=dark gray, wall=mid gray, resource=amber, core=cyan,
    tower=blue, enemy=red) plus `background`, `coreHealthText`, `enemyPip`. The durable shared
    kind->color mapping future launcher authors reuse (closes SG-003 follow-up note #2).
  - New `desktop/src/main/kotlin/dev/myengine/desktop/FrameRasterizer.kt`: AWT-only
    (`BufferedImage`/`Graphics2D`/`ImageIO`, antialiasing OFF, TYPE_INT_RGB) rasterizer of
    `RenderFrame` -> a `CELL_SIZE`(20px) quad per primitive centered on the projected `ScreenPoint`,
    an `enemyPip`(4px) marker drawn ONLY when `RenderPrimitive.health != null`, and a `core <n>`
    readout from `RenderFrame.coreHealth` (the CORE primitive carries no health). Consts
    CELL_SIZE/PIP_SIZE/CORE_TEXT_X/CORE_TEXT_Y; `writePng`. AWT is confined to the desktop harness
    (desktop is never built into Android).
  - Edit `desktop/.../DesktopLauncher.kt`: existing banner/`hash=`/ASCII output preserved and
    unreordered (canonical `9c495d8ff30fd83d` unchanged); ADDS a debug render-smoke block — project
    the scenario snapshot via `PlaceholderRenderSurface`, rasterize, write
    `desktop/build/render-smoke.png`, print `png=<path>`.
  - Edit `desktop/build.gradle.kts`: added `testImplementation(libs.kotlin.test.junit5)` +
    `libs.junit.jupiter` and `tasks.test { useJUnitPlatform() }`.
  - New test `desktop/.../FrameRasterizerPixelSmokeTest.kt`: deterministic headless AWT pixel-smoke
    — synthetic snapshot covering all six kinds -> project -> rasterize -> assert each kind's
    cell-center pixel == its `RenderPalette` color (masking getRGB ARGB), enemy-pip present /
    tower-pip absent (health nullability gate), core-health text region non-background, and
    bit-for-bit rasterization determinism. CLOSES the `docs/contracts/render.md` pixel-smoke gate
    that unit tests alone previously met (SG-003 follow-up note #1).
  - Decision: pure headless AWT `BufferedImage` rasterizer (Option A) over a libGDX/GL window (not
    deterministic/headless-testable). Split refined by a verified dependency fact — `android` ->
    `:games:sandbox` -> `api(project(":engine-render"))`, so engine-render compiles transitively
    into Android; therefore AWT must NOT live in engine-render. Final split: reusable pure
    `RenderPalette` (RGB ints) in engine-render; AWT `FrameRasterizer` + pixel-smoke in the desktop
    harness. No ADR (engine-render presentation boundary + `EngineSnapshot`/`Camera` Experimental;
    no new module dependency edge; Constitution rule 2 preserved, guarded by existing
    `SandboxRenderNonMutationTest`).
  - Pipeline: architect `me-architect` -> developer `me-engine-developer` -> tester `me-tester` ->
    runner `me-runner` -> reviewers `me-renderer-qa` (pass) + `me-verifier` (pass, all four
    boundary_checks true) -> docs. Reviewers found only cosmetic/low nits; one post-review cosmetic
    fix applied by the orchestrator (debug PNG path `desktop\desktop\build\...` ->
    `desktop\build\render-smoke.png`; re-ran `desktop:run`, hash unchanged).
  - Gates: `.\gradlew.bat test` -> pass (incl. new `FrameRasterizerPixelSmokeTest` + existing
    `PlaceholderRenderSurfaceTest`/`RenderBoundaryTest`/`SandboxRenderNonMutationTest`);
    `.\gradlew.bat desktop:run` -> exit 0, `hash=9c495d8ff30fd83d` (unchanged), ASCII map,
    `png=D:\Pet\MyEngine\desktop\build\render-smoke.png`; `.\gradlew.bat android:assembleDebug` ->
    BUILD SUCCESSFUL.
- Signal Garden `SG-004` (Android lifecycle save smoke, 2026-07-04):
  - New `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt`: a pure,
    Android-free holder wrapping `SandboxRuntime` + seed. `save()` = `SandboxSaveCodec.encode(state,
    seed)`; `restore(text)` decodes and re-parses the seed; plus `start()`, `step()`, `submit()`,
    `stableHash()`. QUIESCENT-SAVE precondition documented in KDoc: only `state` is persisted, NOT the
    runtime command queue or the per-tick `SeededRandom(17)`, so `save()` is sound at a quiescent tick
    boundary. No android imports; no save-format change (`SAVE_VERSION` stays 1).
  - Edit `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt`: thin adapter. `onCreate`
    restores from `savedInstanceState["me_sandbox_save"]` under a greppable `DEBUG_SAVE = true` flag
    (else `start()`), preserving the banner + tick + hash TextView and the `runCatching` fold;
    `onSaveInstanceState` writes `session.save()` to the Bundle. No sim logic or content ids in the
    Activity.
  - New test `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt`:
    save/restore roundtrip preserves stableHash; pause/resume determinism (resume == uninterrupted run
    to the same tick); seed roundtrip; independent-runtime; PLUS future-version + non-numeric-version
    decode rejection (durable guard for the versioned-save invariant).
  - Decision: Option A (pure holder in `games/sandbox` + thin Activity adapter; command-queue/RNG
    persistence deferred; quiescent-save precondition documented). No ADR (save codec stays v1;
    sandbox game module + Android shell are Experimental; no new module dependency edge). Device path
    (acceptance #3) is device-pending — see BLOCKERS.
  - Pipeline: architect `me-architect` (Option A) -> developer `me-engine-developer` -> tester
    `me-tester` -> reviewers `me-save-compat-reviewer` (pass: roundtrip/versioning/seed sound,
    quiescent limitation documented), `me-android-performance` (pass: lifecycle pairing correct,
    boundary clean, frame budget fine for the tiny sandbox), `me-verifier` (pass, all four
    boundary_checks true) -> docs.
  - Gates: `.\gradlew.bat test` -> pass (full suite incl. `SandboxSessionLifecycleTest` +
    version-rejection tests); `scripts\me-save-compat.ps1` -> pass; `.\gradlew.bat
    android:assembleDebug` -> BUILD SUCCESSFUL (thin adapter compiles + packages); telemetry recorded
    (events=7).
- Signal Garden `SG-004 follow-up` (sandbox save-format v2 — persist the runtime's pending
  `CommandQueue`, 2026-07-05): closes the last recorded SG-004 gap — the quiescent-save precondition
  is DROPPED, `save()` is now sound at ANY tick.
  - `engine-core/src/main/kotlin/dev/myengine/core/Command.kt`: added
    `CommandQueue.pending(): List<EngineCommand>` (non-destructive snapshot; `drainFor`/
    `commandComparator` unchanged).
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`: `SandboxRuntime`
    gained `pendingCommands()`/`submitAll()`; `SandboxSaveCodec.SAVE_VERSION` bumped **1 -> 2**;
    `encode()` gained a `pendingCommands` param serialized as a new `pendingCommands` properties line
    (`type|id|scheduledTick|actorId|stablePayload` per command, `;`-joined); `decode()`'s version
    guard now accepts `1 || 2` (rejects 3+), its signature/return type (`SandboxState`) UNCHANGED —
    this is a real v1->v2 migration, not a breaking bump; new separate `decodePendingCommands(text)`
    reconstructs `BuildTowerCommand` for `type=="build_tower"` or a generic `TextCommand` otherwise.
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt`: `save()` now passes
    `runtime.pendingCommands()` to encode; `restore()` also calls `decodePendingCommands()` and loads
    them via `submitAll()`; KDoc rewritten to DROP the quiescent-save precondition — `save()` is
    documented as sound at ANY tick.
  - `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt`: `DEBUG_SAVE` changed from a
    hardcoded `const val true` to `val DEBUG_SAVE = BuildConfig.DEBUG` (release builds can no longer
    ship this enabled); stale "quiescent tick" comment updated.
  - `android/build.gradle.kts`: added `buildFeatures { buildConfig = true }`.
  - Tests: `games/sandbox/.../SandboxSessionLifecycleTest.kt` updated — new key regression test
    (pending future-tick command still queued at save time round-trips correctly through
    save/restore/resume, matching an uninterrupted run's stableHash); new v1->v2 migration test
    (v1-shaped save with no `pendingCommands` property decodes cleanly to an empty queue); new
    CommandId/Tick preservation test; retargeted the future-version-rejection test to
    `saveVersion=3` (v2 is now valid) and the non-numeric-version test to the new `saveVersion=2`
    baseline. Class KDoc rewritten to describe both quiescent and pending-command save cases.
  - Decision: no ADR needed. me-architect's ruling: covered by the existing "saves are versioned from
    v1 and migration-aware" invariant; `SandboxSaveCodec` stays Experimental per
    `docs/API_STABILITY.md` — same precedent as the original v1 codec, which also shipped without an
    ADR.
  - Pipeline: architect `me-architect` (no ADR) -> developer `me-engine-developer` -> tester
    `me-tester` -> runner `me-runner` -> reviewers `me-simulation-reviewer` (pass, one medium
    non-blocking forward-looking: no persisted CommandId-issuing counter — no production caller mints
    sequential ids today so no current collision risk; one medium non-blocking: no
    engine-core-unit-level determinism test for `pending()`/`submitAll()`, covered end-to-end at the
    sandbox level instead), `me-save-compat-reviewer` (pass, one low: `pendingCommands` encoding
    assumes no `;`/`|`/`:` in command type/actorId/stablePayload — same pre-existing assumption class
    as entities/producers encoding in the same file), `me-android-performance` (pass, one low:
    `DEBUG_SAVE` losing `const val` means release-branch elimination now depends on
    R8/minification rather than compile-time folding — acceptable, trivial branch bodies; one medium
    pre-existing/unchanged: no onPause/onStop/onDestroy persistence path yet, Bundle-only, still
    device-pending) -> `me-verifier` (pass, all four boundary_checks true; confirmed all three SG-004
    acceptance criteria are satisfied: "Android shell can trigger save/load in debug"
    (`BuildConfig.DEBUG` gate + wiring), "Lifecycle pause does not corrupt simulation state" (now
    strictly stronger — sound at any tick, not just quiescent), "Exact device blocker is documented if
    no device is available" (unaffected, already on record)) -> docs `me-docs`.
  - Gates: `.\gradlew.bat :games:sandbox:test` -> pass; full `.\gradlew.bat test` -> pass;
    `scripts\me-save-compat.ps1` -> pass; `.\gradlew.bat :android:compileDebugKotlin` -> pass
    (verified after pointing a machine-local, gitignored `local.properties` at the Android SDK — not
    a code change).
- Signal Garden `SG-005` (balance report deltas, 2026-07-05):
  - `engine-devtools/.../DevtoolReports.kt`: added `BalanceDeltaReport`, `BalancePackSummary`,
    deterministic metric deltas, threshold metadata (`large_percent_delta=0.25`,
    `large_absolute_delta=5.0`), warning generation, and invalid-pack error reporting. Default
    comparison is sandbox baseline content vs `games/signal-garden/content/signal-garden`.
  - `engine-devtools/.../DevtoolsMain.kt`: added `balance-report` / `balance-delta` CLI aliases.
    Existing `scenario` / `balance` suite output is unchanged.
  - Tests in `DevtoolReportsTest`: baseline-vs-changed comparison, parser-backed JSON structure,
    captured CLI stdout parses as one object, no-op copy has no warnings, large enemy/core/resource
    deltas are flagged, and invalid changed-pack errors are returned without throwing. Added
    test-only `kotlinx-serialization-json` via `gradle/libs.versions.toml` and
    `engine-devtools/build.gradle.kts`.
  - Default report warnings today: `enemy_health_total` 32 -> 40, `core_damage_potential` 16 -> 8,
    `reward_total` 8 -> 16.
  - Decision: devtools-only Experimental report; no ADR, no engine runtime change, no content schema
    change, and no balance values changed. Pipeline: architect pass; tester initially found missing
    JSON-parse/CLI coverage, fixed; runner gates pass; verifier pass with all four boundary checks
    true and no findings.
- MyTD `MTD-001` / `MTD-002` (reward deposit duplicate close + tower cost gating, 2026-07-05):
  - `MTD-001` is closed as a duplicate of implemented SG-002. MyTD gold is a content-defined resource
    (`enemy.rewardResource=gold` in a future MyTD pack), not a separate engine branch.
  - `MTD-002` is closed by the existing generic build command flow plus new acceptance tests:
    `SandboxRuntime.buildTower` checks `Inventory.canRemove(tower.costResource, tower.costAmount)`,
    invokes `DefenseRuntime.placeTower` only when affordable, and removes cost only after
    `TowerPlacementResult.Placed`.
  - New `games/sandbox/.../SandboxTowerCostGatingTest.kt` covers affordable spend, unaffordable
    rejection with unchanged non-negative balance, not-buildable rejection without spend, and
    replay-stable rejection.
  - Synced `.claude/specs/backlog/MTD-001-reward-deposit.md`,
    `.claude/specs/backlog/MTD-002-gold-cost-gating.md`, `.claude/specs/ENGINE_ROADMAP.md`, and
    `D:/Pet/MyTD/spec/{engine-gap-analysis.md,traceability.csv,risks.md}`.
  - Decision: no ADR; no new dependency edge, no copied reference content, and no gold-specific
    engine logic. Keep `engine-defense` inventory-free; game/runtime orchestration owns resource
    mutation.

- PROC v0.2.0 pipeline improvements (2026-07-04) — first real `/me --improve`-style batch,
  sourced from an approved architecture review (human gate = plan approval):
  - Canon (`docs/agentic/SELF_IMPROVEMENT.md`, `PIPELINE.md`, `AGENT_CONTRACTS.md`,
    `SPEC_BOARD.md`, `docs/GAME_SPEC_PIPELINE.md`): mandatory telemetry for every run incl.
    failures with new fields (`duration_min`, `malformed_json_count`, `gate_failures`,
    `attributed_agent`); reflect cadence (after any failed run / every 5th event); proposal
    queue `.ai/proposals/` + `--improve --drain`; plugin-versioning rule; memory boundary;
    selfcheck at intake; `.ai/DIGEST.md` intake digest; conditional Reviewer Matrix by
    changed paths; `--upgrade` mode; close-out flips spec card statuses and syncs game-bundle
    traceability; Gap Dedup Rule + demand-counted Engine Roadmap.
  - Scripts: `me-record-run.ps1` (needs_human verdict, new fields, `reflect_required` output),
    `me-retro.ps1` (attribution/gate-failure/retry aggregation).
  - me-dev plugin: `model:` tiers on all agents (haiku `me-runner`; sonnet reviewers/tester/
    docs/reflect/content-schema/balance; inherit architect/developer/gameplay-designer/
    verifier/improve); NEW `me-scout` (cheap `file:line` fact-finder, Scout schema in
    contracts); SKILL.md reworked (intake selfcheck + digest, reviewer matrix, scout
    delegation, close-out telemetry + reflect trigger, `--improve --drain`, `--upgrade`).
  - me-spec plugin: mandatory gap dedup (scan backlog/active/done + `docs/API_STABILITY.md` +
    roadmap before minting a gap); backlog bridge updates `.claude/specs/ENGINE_ROADMAP.md`
    (NEW: aggregated capabilities with demand counts; flags MTD-001 as a duplicate of done
    SG-002 — close by reference, verify the gold-mapping delta only).
  - Deferred (filed as backlog cards, NOT implemented): PROC-001 spec back-sync script,
    PROC-002 multi-repo ADR, PROC-003 domain roadmap (flow-field/jobs/storyteller vs game
    specs), PROC-004 perf budgets, PROC-005 golden replay-hash files, PROC-006 CI/pre-push,
    PROC-007 save migration matrix, PROC-008 playtest bot, PROC-009 Android visual smoke,
    PROC-010 cost telemetry.
  - Versions: both plugins 0.1.0 -> 0.2.0; logged in `.ai/changes/agent-skill-log.md`.
    User should reinstall/update the `myengine` marketplace plugins to refresh the cache.

- ENG-014 is complete: map terminal rules, deterministic terminal rejection, immutable snapshot
  summary, and save v5 persistence. No ADR: additive data-driven capability with no new dependency
  edge.
- MTD-005 is complete/accepted: Android `SandboxRenderView` consumes immutable snapshots through
  `PlaceholderRenderSurface` and draws tiles, path, core, tower tiers, enemies, and overlay with
  `Canvas`/`RenderPalette`. `MotionEvent` tap, drag-pan, and pinch flow through `InputAdapter`;
  `MyEngineActivity` owns command-id issuance and the sole submit -> step -> invalidate callback.
  The View owns presentation state only, so it cannot mutate authoritative simulation state.
  Acceptance is backed by scoped JVM/build/replay proof; the outstanding device smoke and performance
  profile are deliberately recorded as pending, not treated as completed acceptance.
- ENG-026 is complete/accepted: Android-local `TickScheduler` applies a 20 Hz Choreographer policy;
  the `SurfaceView` renders the latest immutable `RenderFrame`; and `InputAdapter` routes input only
  to the command queue. `onPause` cancels/saves pending commands and Bundle restoration preserves the
  next command ID plus replay continuity. JVM/build/replay/save-compat gates pass; device gesture/
  lifecycle smoke and performance profiling remain manual-pending.
- ENG-027 is complete/accepted: `EngineSnapshot`/`RenderFrame` expose immutable, content-derived HUD
  labels and data for resources, wave/countdown/core state, build tower costs/tiers, and selected
  tower stats/upgrades. Android draws build/select/upgrade panels from this snapshot and shares one
  density-aware 48 dp layout model between drawing and hit testing; taps emit `BuildTowerCommand` or
  `UpgradeTowerCommand` through `InputAdapter` and the existing caller-owned queue boundary.
  Defense records deterministic per-tower actual damage/kills, and `SandboxSaveCodec` v6 persists
  them while v1-v5 migrate to an empty metrics map. Content validation now requires tower/tier
  `displayKey` references and all nine `hud.*` string keys. Full gates and final verification pass;
  manual device/layout/performance limitations remain documented below.
- ENG-015 is complete/accepted: Android-local `PresentationSpeed` provides `0x`, `1x`, `2x`, and
  `4x`; `FixedTickFrameLoop` applies presentation pacing to due ticks while preserving fixed-tick
  simulation semantics. `MyEngineActivity` stores speed separately in `Bundle`, and
  `SandboxRenderView` exposes callback-only speed controls without creating engine commands.
  Per-tick trajectory parity, pause/restart timing, overflow-safe timestamps, and speed layout bounds
  pass; no simulation, render-model, or save-schema change was made.
- ENG-030 is complete/accepted: immutable HUD preview exposes deterministic next-wave composition
  and countdown; typed `CallWaveEarlyCommand` starts the next wave before its scheduled tick and
  applies an optional content-defined `resourceId + amount` bonus. Calls at/after the scheduled
  boundary or while enemies are active are deterministic rejections. `SandboxSaveCodec` v8
  restores typed pending commands and migrates v1-v7 saves. Full tests, content validation (2
  packs), replay, save-compat, benchmark, and `android:assembleDebug` pass. Canonical/kill replay
  hashes are `12a65fd2b87593cf`/`bb37eefc1903cc77`; benchmark is `473 ms`/`78 ms`, goal-field
  rebuild `8222800 ns`. The balance review returned partial: current content packs are valid and
  contain no hardcoded bonus; its schema-documentation gap was closed in this docs close-out, and
  the optional bonus remains unconfigured pending an approved balance value.
- ENG-028 is complete/accepted: optional pack-relative sprite/atlas refs are validated for tiles,
  towers, tower tiers, enemies, and minimal building definitions; refs remain opaque through the
  immutable sandbox snapshot and `RenderFrame`; desktop/Android consumers resolve available refs
  and retain deterministic palette fallback for omitted/missing refs. The original text atlas
  placeholder is packaged in the sandbox content tree; no production art, save schema, replay
  hash, or dependency edge changed.
- ENG-009 is complete/accepted: optional tower `splashRadius`/`falloff` resolve deterministic
  entity-id-ordered Manhattan AoE with integer per-ring damage. `ShotEvent` and `HitEvent` expose
  immutable source/target/tick data from only the latest completed tick for presentation consumers;
  they are replaced per tick and intentionally excluded from save and stable-hash state. No content
  pack balance values changed and `SandboxSaveCodec.SAVE_VERSION` remains `8`.
- ENG-020 is complete/accepted: an internal, non-persisted `GridSpatialIndex` supplies targeting and
  splash candidates with exact post-filters, live `EntityStore` resolution, stable entity-id
  ordering, and preserved Manhattan semantics. Devtools exposes deterministic machine-readable
  metrics for 1024 concurrent enemies, 16 towers, and 16 queries; the accepted run measured
  `5.3045 ms`.

## DECISIONS

- ENG-001 needs no ADR: it is an Android-free engine/vision capability with no save, content,
  render, Android, dependency, or game-bundle traceability change. The planner is deliberately
  reusable but not yet wired into a full Movement/job tick system; that belongs to ENG-003/ENG-004.

- ENG-003 needs no ADR, game-bundle traceability update, or plugin/skill/pipeline contract change.
  Approved defaults remain authoritative: every positioned `JobActorComponent` is eligible for all
  job types, one work tick is processed per simulation tick, in-world `TilePosition` is the target
  check, and invalid/no-path jobs return to `OPEN` after deterministic release.

- ENG-029 needs no ADR: simulation remains Android/audio-free and the SoundPool adapter owns audio
  decoding/playback and presentation state. No plugin/skill or canonical agent-contract changed.

- PROC-003 sequencing adopted 2026-07-29 (human-approved): ENG-010 -> ENG-016 -> PROC-007 ->
  ENG-021 -> ENG-029 -> ENG-012 -> ENG-007 -> ENG-018. Unique successor to ENG-020 is ENG-010
  (status effects framework), the only remaining backlog card backed by a named game FR
  (MyTD FR-007). PROC-007 runs before ENG-021 because ENG-010 and ENG-021 bump the save codec.
- PROC-003 acceptance criterion (b) amended by owner decision: `vision:*` demand tags (per
  ENGINE_ROADMAP.md notes) count as legitimate demand where no named game FR exists yet.
- Roadmap demand tags corrected 2026-07-29: `mytd` removed from ENG-012 (2->1), ENG-021 (4->3),
  ENG-022 (2->1), ENG-029 (4->3) as unbacked by the MyTD spec bundle
  (`D:/Pet/MyTD/spec/requirements.md`). SG FR-002 is already satisfied by SG-001 and is not
  demand for ENG-016.

- PROC-013 uses documentation-only Variant B wiring: the checker emits one JSON result, returns
  exit 0 on pass and exit 1 on mismatch, and requires no ADR because no canonical contract or
  adapter changed.
- ADR-0004 chooses composite build plus an exact commit lock instead of early Maven publication.
  A game updates its pin only after an engine commit is accepted and pushed; a game release tag
  contains that exact SHA. Revisit artifact publication only through a later ADR with demonstrated
  multi-consumer value.
- MySD probable families (production buildings/allied units, mobile combat, in-run drafts,
  campaign/energy/sweep, roster/profile progression) are not yet backlog cards. Gate 1 observation
  and normal dedup must precede demand updates or `ENG-037+`.

- ENG-014 is an additive, data-driven terminal-run capability with no new dependency edge, so no
  ADR is needed. Map-owned `terminalRules` select finite-wave victory or no-win/endless behavior;
  core-health loss is mandatory and a positive leak budget is optional. Terminal summaries freeze in
  the authoritative run state, are projected read-only on `EngineSnapshot`, participate in the
  stable hash, and persist through `SandboxSaveCodec` v5 (v1-v4 decode as active runs).

- MTD-005 keeps the Android Canvas/InputController boundary one-way: `SandboxRenderView` holds camera,
  selection, and gesture bookkeeping; it projects read-only snapshots and emits commands only through
  a callback. `MyEngineActivity`, outside `engine-render`, allocates `CommandId` and performs the
  authoritative command-queue submit/step transition. No ADR: this consumes the existing render/input
  boundary without a new engine dependency edge. `docs/contracts/render.md` now makes this durable.

- ENG-027 keeps authoritative state outside presentation: HUD data is projected read-only from the
  runtime snapshot, Android owns only selection/layout state, and all build/upgrade mutations enter
  through existing queued command DTOs. Localization is content-owned and validated at load time.
  Save v6 is required because per-tower metrics affect restored HUD continuity; older saves preserve
  compatibility by decoding with no historical per-tower metrics. No ADR is needed because this is
  an additive Experimental snapshot/content/save extension with no new dependency edge.

- ENG-015 keeps speed strictly in the Android presentation boundary: the loop scales canonical due
  ticks, the HUD owns only transient selection/callback state, and `Bundle` persistence is separate
  from `SandboxSession.save()`. No ADR is needed because there is no dependency-direction or save
  schema change.
- ENG-030 uses the approved Variant A: active-wave rejection is derived from live enemies, while
  the early-call bonus is content-defined as `resourceId + amount`. No ADR is needed because this
  is an additive command/content/HUD/save extension with no new dependency edge. Existing packs
  remain unchanged until an approved balance value exists.
- ENG-028 follows ADR-0003: flat visual definitions stay in `.properties`, the minimal atlas is an
  original text index, and engine-content validates only pack-relative path/key metadata. There is
  no dedicated `RenderKind.BUILDING` yet; minimal building entities reuse the generic placeholder
  entity kind until a gameplay/render requirement justifies a distinct kind.
- ENG-009 uses no ADR: it is an additive content/defense/snapshot capability with no dependency
  direction or save-schema change. Presentation event data remains explicitly transient and never
  becomes authoritative state.
- ENG-020 uses no ADR: the index is an engine-defense implementation detail, is rebuilt for the
  update pass, is not persisted, and introduces no save/content schema, Android/render, public API,
  or dependency-direction change.

- Android remains the only shipping platform; desktop/JVM is a dev harness.
- Simulation modules remain Android/render-free.
- v0.1 content uses external `.properties` files parsed with structured `Properties` APIs.
- Save format starts at v1 in the sandbox codec.
- Agentic pipeline canonical source is `docs/agentic/*`; adapters are thin.
- Self-improvement requires telemetry evidence, a proposal, and a human gate before edits.
- Signal Garden is the first original sample game spec and kickoff target.
- Signal Garden content is original (no clone-IP): own ids/names/numbers, no ADR needed. The
  engine-content loader test resolves the pack as a filesystem `Path` so `engine-content` stays
  game-module-free.
- SG-002 reward deposit uses Option A: engine-defense returns rewards (`TowerUpdateResult`) and the
  game runtime (`SandboxRuntime.step`) owns Inventory mutation. No ADR because `DefenseRuntime` is
  Experimental per `docs/API_STABILITY.md`. Deposits are content-derived (no hardcoded resource
  ids) and applied in sorted-key order guarded by `canAdd` for determinism.
- SG-003 render surface is a reusable, game-agnostic engine capability: `PlaceholderRenderSurface`
  is a pure `snapshot -> RenderFrame` projection with no game/Android imports and no simulation
  mutation, so it stays behind the presentation/snapshot boundary. It is deliberately NOT yet wired
  into the desktop/Android launchers (still `AsciiRenderer`); consuming `RenderFrame` in a real
  launcher with a pixel/screenshot smoke is a tracked follow-up.
- SG-003 follow-up: `RenderFrame` is consumed in a real launcher via a pure headless AWT
  `BufferedImage` rasterizer in the desktop harness. The reusable kind->color mapping is factored
  into a pure Android-safe `RenderPalette` in engine-render (RGB ints, no AWT), and all AWT stays in
  `:desktop` because `android -> :games:sandbox -> engine-render` compiles engine-render into the
  Android artifact. No ADR (Experimental presentation boundary; no new module edge). The Android
  shell keeps `AsciiRenderer` for now, Android real-launcher wiring is a separate follow-up.
- SG-004 Android lifecycle save smoke uses Option A: the save/restore logic lives in a pure,
  Android-free `SandboxSession` holder in `games/sandbox` (`save()`/`restore()` over
  `SandboxSaveCodec`), and the Android `MyEngineActivity` is only a thin Bundle adapter (save on
  `onSaveInstanceState`, restore on `onCreate` under a greppable `DEBUG_SAVE` flag) with no sim logic
  or content ids. Save format stays v1 (only `state`+seed persisted); the command queue and per-tick
  `SeededRandom(17)` are NOT persisted, so `save()` carries a documented quiescent-save precondition
  (deferred: command-queue/RNG persistence would need a v1->v2 migration). No ADR. The
  device-independent proof is JVM-covered by `SandboxSessionLifecycleTest`; the real on-device Bundle
  round-trip is device-pending.
- SG-004 follow-up (2026-07-05) closes the deferred command-queue-persistence item: `SandboxSaveCodec`
  bumps `SAVE_VERSION` 1 -> 2 with a real migration (`decode()` accepts `1 || 2`, rejects 3+; v1 saves
  with no `pendingCommands` property decode to an empty queue) and now persists the runtime's pending
  `CommandQueue` (`CommandQueue.pending()`, `SandboxRuntime.pendingCommands()`/`submitAll()`), so
  `save()` is sound at ANY tick — the quiescent-save precondition from the original SG-004 slice is
  DROPPED. The per-tick incident `SeededRandom(17)` still needs no persistence: confirmed to be a
  fresh instance constructed every tick, not a persistent cursor, so there is nothing to save for it.
  No ADR: per me-architect, this is covered by the existing "saves are versioned from v1 and
  migration-aware" invariant, and `SandboxSaveCodec` stays Experimental per `docs/API_STABILITY.md`
  (same precedent as the original v1 codec). `DEBUG_SAVE` is now gated on `BuildConfig.DEBUG`
  (`android/build.gradle.kts` gained `buildFeatures { buildConfig = true }`), closing SG-004
  follow-up #2 in the same pass.
- SG-005 balance report deltas are a devtools-only, content-driven JSON report over loaded
  `ContentRegistry` values. No ADR: no engine runtime behavior, Android shell, save format, content
  schema, or shipped balance values changed; thresholds are report metadata for suspicious-value
  review, not final game balance policy.
- MyTD gold is represented as ordinary content-defined resources (`costResource`/`rewardResource`),
  not a hardcoded engine economy concept. `MTD-001` is closed by SG-002; `MTD-002` is closed by the
  existing generic build command flow plus `SandboxTowerCostGatingTest`. `engine-defense` remains
  inventory-free.
- MyTD `MTD-004` uses the existing content boundary: difficulty data is materialized into an
  effective registry before simulation, with no per-mode engine branching. No ADR is needed because
  this is an additive, data-driven Experimental capability with no new dependency edge and no
  save/Android/render change.
- MyTD `MTD-003` tower upgrade hook is complete. Upgrade tiers are optional content fields under
  `towers.properties`; `UpgradeTowerCommand` mutates a placed tower's `AttackComponent` through
  legal tier transitions, spends the content-defined tier resource only after affordability checks,
  and `SandboxSaveCodec` v3 persists tower `upgradeBranch`/`upgradeTier` plus pending upgrade
  commands. Sandbox `pulse.upgrade.main.1` is proof content only, not final MyTD balance. All gates
  and reviewers pass.
- MyTD `MTD-004` difficulty modifiers are complete. `DifficultyContent` plus optional
  `difficulties.properties` provide data-only easy/normal/hard multipliers; `ContentRegistry.resolveDifficulty`
  materializes deterministic `BigDecimal` health/count/reward/gold-rate scaling before the first tick.
  `SandboxGame` and `SandboxSession` wire setup selection. Values come from the MyTD balance plan;
  no save-format, Android, or render changes and no ADR. All tests/gates and verifier boundaries pass.
- MyEngine `ENG-024` command DTO relocation and InputAdapter state fix are complete (approved
  variant A). `BuildTowerCommand`/`UpgradeTowerCommand` and `TileCoordinate` now live in the
  render-free `engine-core` command package; `InputState` no longer owns `nextCommandId` or
  `selectedTowerId`; `InputUiState` is explicit; callers supply `CommandId`; sandbox boundary
  conversion remains the adapter seam. `docs/contracts/render.md` was already updated by the
  implementation run and is unchanged in this close-out.
- DX-008 and ENG-005 are complete (2026-07-16). ADR-0003 accepts the hybrid content policy:
  `.properties` remains canonical for flat definitions and additive `maps.json` serves nested maps.
  The sandbox now materializes the canonical 64x64 map, named spawn `(1,1)`, core `(32,32)`, and
  `bolt` resource node `(5,5)=100` from content; loader fixtures cover malformed rows, bounds,
  unknown refs, core count, and blocked spawn paths. `SandboxSaveCodec` v4 persists map id and
  content version, validates map/pack/content identity, and migrates v1-v3 saves via the sole map.
  The canonical replay hashes remain `9c495d8ff30fd83d` and `83a65da1a7881b2c`.
- MyEngine ENG-002 (goal-field pathfinding + repath on world change, 2026-07-18) is complete.
  `GoalField` supplies deterministic core-outward BFS routing to all wave enemies, replacing
  per-enemy precomputed paths. Placement validates every spawn prospectively before mutation and
  returns `occupied_by_enemy` for occupied tiles. Walkability changes rebuild the field in the same
  tick, producing immediate mid-run reroutes. Save v6 derives the field after restore and
  canonicalizes legacy path state instead of serializing cache data. The maze golden hash is
  `ed0354584405ec49`; canonical and kill hashes are `463d87684ca6cbee` and `40c7bda7e3bc1316`.
- MyEngine ENG-013 (tower sell/refund, 2026-07-18) is complete. `SellTowerCommand` has a stable
  positive tower-id payload and deterministic queue ordering. `sellRefundRatio` is a required
  `0..1` inclusive decimal on every tower definition, including sandbox and Signal Garden packs.
  Sale reconstructs only base plus actually applied sequential upgrade-tier spend, aggregates by
  resource, and refunds `floor(spend * ratio)` independently per resource. It rejects insufficient
  refund capacity without mutation; on success it clears occupancy, removes the tower and its
  metrics, rebuilds `GoalField` before enemy movement in that tick, and deposits the refund.
  Pending sells round-trip id/tick/actor/payload through the existing queue encoding. No ADR and no
  save-version change: `SandboxSaveCodec.SAVE_VERSION` remains `6`.
- MyEngine ENG-008 (targeting priority modes, 2026-07-18) is complete. `TargetSelector` is a pure
  selector for `first`, `last`, `nearest`, `strongest`, and `weakest`, resolving ties by entity id.
  Content declares a per-tower default; missing `targetingMode` in a v1 pack defaults to `NEAREST`.
  `SetTowerTargetingModeCommand` is queued and applies the per-tower override at the command
  boundary; immutable HUD tower data exposes the active mode. Save v7 persists tower modes and a
  pending mode-switch command, while v1-v6 migration resolves the content default.

—
## MyTD (2026-07-04)

- `/me-spec --greenfield-game` clone-intake vs `com.vipubstd.games.block.defense` produced an
  original, traceable MyTD bundle at `D:/Pet/MyTD/spec` (13 files, FR-001..FR-016 fully traced).
- Both human gates passed. Mechanics cloned; all names/numbers original; art own-style; reference-IP
  reuse gated behind a dedicated ADR.
- Backlog bridge added: `MTD-001` reward deposit (done/duplicate of SG-002), `MTD-002` gold-cost
  gating (done), `MTD-003` tower upgrade hook (done), `MTD-004` difficulty modifiers (done), `MTD-005` render/input
  surface. Content (EG-006) stays in the MyTD bundle.

## NEXT

Run `/me --feature --next` for ENG-033 (colonist needs MVP), now that its authored named-FR scope
satisfies the Phase 15 colony re-entry trigger. ENG-004 is closed; the earlier commit blocker is resolved: PROC-013 commit 5eaaa78
was pushed.

## BLOCKERS

- ENG-001 has no implementation blocker. Full Movement/job tick integration is intentionally
  deferred to ENG-003/ENG-004, and wave enemies remain on the ENG-002 `GoalField`. Colony demand
  is now eligible through the authored ENG-033 scope; MySD TD evidence remains outside the colony
  evidence boundary.
- ENG-003 has no implementation blocker. Non-blocking follow-ups: add the two-worker replay to
  `DevtoolReports.replayInspect`; invoke `SandboxJobExecutionTest` separately from
  `scripts/me-save-compat.ps1`; and add a job-heavy benchmark for large worker/job counts and
  invalidated paths.
- RESOLVED (2026-07-29): the pre-existing unrelated dirty `.ai/retro/retro-2026-07-28.md` commit
  blocker is cleared; PROC-013 commit 5eaaa78 was pushed.
- ENG-029 has no implementation blocker. No real device/emulator SoundPool playback, volume/mute,
  or frame-metrics evidence was available. The Android reviewer contract was unavailable; local
  Android/content/simulation boundary and gate evidence passed. The initial invalid
  `:android:test --tests` invocation was corrected to `:android:testDebugUnitTest --tests` and is
  not a feature failure.
- ENG-012 has no implementation blocker. Conditional reviewer agents were unavailable after repeated
  thread timeouts; local simulation/render/save/content boundary review passed. No device/emulator
  proof is claimed beyond the Android assemble gate.
- MySD ENG-036 is specified but intentionally not started until the MySD evidence/spec gates choose
  the implementation order. PROC-015 is a backlog process change, not an implemented adapter.

- ENG-030 is accepted. Existing packs intentionally do not configure an early-call bonus because no
  balance value was approved; the data-driven path is covered by synthetic tests. Per-snapshot HUD
  allocation and device profiling remain low, non-blocking follow-ups. The existing save delimiter
  assumption remains documented; no code fix was made in this docs-only close-out.

- ENG-028 Android AssetManager resolution has compile/assemble coverage but no device/instrumented
  runtime fixture; the full PROC-009 screenshot/golden lane remains manual. Conditional domain
  reviewer agents could not be spawned in this run because the agent-thread limit was reached; a
  local read-only boundary review passed, but reviewer-agent evidence should be refreshed when
  capacity is available.

- ENG-009 intentionally leaves all shipped content packs without splash values. Choose a game pack
  balance configuration only through an approved balance change; this is not an engine blocker.

- ENG-011 has no implementation blocker. No device/emulator evidence is claimed; the existing
  manual Android/device and performance follow-ups remain pending where previously recorded.

- ENG-020 has one low, non-blocking test-coverage follow-up: seeded differential tests do not yet
  provide end-to-end `updateTowers` parity across every targeting-mode and splash combination.
  Attribute this to `me-tester`; no production fix is requested in this close-out. The benchmark
  is measurement-only and does not define PROC-004 budget thresholds.

- ENG-015 device/instrumentation verification remains pending: exercise speed taps, lifecycle/
  recreation, Bundle restoration, and no-command behavior on a device/emulator; capture
  FrameMetrics/JankStats and Allocation Tracker evidence. The extreme `200x600` portrait layout has
  a manual selected-panel overflow risk. The pre-existing `pausedSave` rollback risk is outside
  ENG-015 scope. At `0x`, idle HUD redraw continues as an accepted CPU/battery trade-off.
- ENG-027 is accepted with non-blocking manual limitations: run build-tower, tower-selection,
  upgrade, and pause/recreate lifecycle smoke on a device/emulator; check non-default fontScale and
  long localized labels; capture FrameMetrics/JankStats and Allocation Tracker evidence before any
  smoothness or frame/allocation-budget claim. Save v6 malformed-input hardening should explicitly
  define/test missing `towerMetrics` and duplicate tower-id entries; valid v1-v6 migration and
  roundtrip are covered.
- ENG-026 device/performance checks remain pending: on a device/emulator, verify tap, pan, pinch,
  and pause/recreate with a pending command while checking next command ID and replay-hash continuity.
  Capture FrameMetrics/JankStats and Allocation Tracker evidence before claiming a frame budget or
  smoothness target. `me-android-performance` is partial only for these manual checks.
- MTD-005 is accepted but device-pending: no device/emulator smoke verified tap-build after one tick,
  drag-without-build, pinch zoom, tiles -> path -> entities draw order, or debug
  rotation/process recreation with a pending command and the next caller-owned `CommandId`.
- MTD-005 known performance risk: each redraw creates a snapshot/frame and intermediate primitive
  lists. Capture FrameMetrics/JankStats and Allocation Tracker data during sustained pan/pinch before
  claiming frame-budget compliance or smoothness.
- ENG-005 low, non-blocking: the Android module packages sandbox content, but
  `SandboxGame.loadRegistry()` still seeks a filesystem path rather than `AssetManager`; device
  startup/content loading remains unverified.
- ENG-005 low, non-blocking: `BalanceDeltaReport` does not summarize map-local resource-node amounts
  or geometry, so future map-only economy changes will be invisible to balance deltas.
- MTD-004 low, non-blocking follow-up: `difficultyId` is not serialized; save restore requires the
  same effective difficulty-resolved registry.
- RESOLVED (2026-07-05, SG-005): content suspicious-value/balance reporting is covered by the
  devtools `balance-report` / `balance-delta` JSON report.
- SG-004 DEVICE BLOCKER (2026-07-04, acceptance #3): no connected Android device/emulator is
  available here, so the on-device Bundle round-trip (`onSaveInstanceState` outState -> `onCreate`
  savedInstanceState under config-change/process-death) — the real instrumented pause/resume +
  save-directory-access smoke from `docs/contracts/android.md` Test Gates — cannot be executed. The
  device-independent proof (save-at-pause == uninterrupted run, seed roundtrip, versioned-save
  rejection) is JVM-covered by `SandboxSessionLifecycleTest`; `.\gradlew.bat android:assembleDebug`
  is the best available static gate. Closing acceptance #3's device path needs a device/emulator run.
- SG-004 follow-ups (2026-07-04, non-blocking, low, from me-save-compat-reviewer/me-android-performance):
  1. RESOLVED (2026-07-05, SG-004 follow-up): the runtime's pending `CommandQueue` is now persisted
     via a `SAVE_VERSION` v1->v2 migration in `SandboxSaveCodec`, so `save()` is sound outside a
     quiescent tick — see DONE "Signal Garden SG-004 follow-up". The per-tick incident RNG needed no
     persistence (fresh instance every tick, confirmed not a cursor).
  2. RESOLVED (2026-07-05, SG-004 follow-up): `DEBUG_SAVE` is now gated on `BuildConfig.DEBUG`
     (`buildFeatures { buildConfig = true }` added to `android/build.gradle.kts`).
  3. Move the `onSaveInstanceState` encode off the main thread (or cap size) once state grows. Still
     OPEN — not addressed by the SG-004 follow-up.
  4. On a device/emulator, run the real pause/resume + process-death Bundle round-trip (closes
     acceptance #3's device path). Still OPEN/device-pending — unaffected by the SG-004 follow-up.
- SG-004 follow-up new low-severity items (2026-07-05, non-blocking, from
  me-simulation-reviewer/me-save-compat-reviewer/me-android-performance/me-verifier):
  1. No persisted CommandId-issuing counter for the new `pendingCommands` encoding — forward-looking,
     no production caller mints sequential command ids today, no current collision risk. Revisit if a
     real command-submitting UI is added.
  2. No engine-core-unit-level determinism test for `CommandQueue.pending()`/
     `SandboxRuntime.submitAll()` in isolation — covered end-to-end at the sandbox level
     (`SandboxSessionLifecycleTest`) instead.
  3. The new `pendingCommands` properties-line encoding assumes command type/actorId/stablePayload
     contain no `;`/`|`/`:` — same pre-existing delimiter-collision assumption class as the
     entities/producers encodings in the same codec file (not a new risk class, a new field sharing
     it).
- SG-003 follow-ups (2026-07-04, non-blocking, low/info from me-renderer-qa/me-verifier):
  - RESOLVED (2026-07-04, SG-003 follow-up): `RenderFrame` is now consumed in a real launcher —
    `DesktopLauncher` projects via `PlaceholderRenderSurface` and rasterizes through the new headless
    AWT `FrameRasterizer` to `desktop/build/render-smoke.png`; the new deterministic
    `FrameRasterizerPixelSmokeTest` gives all six `RenderKind`s visual coverage, closing the
    `render.md` pixel-smoke gate. See DONE "Signal Garden SG-003 follow-up".
  - RESOLVED (2026-07-04, SG-003 follow-up): the launcher-author doc note is now encoded durably —
    the kind->color mapping lives in `engine-render/.../RenderPalette.kt` (Android-safe), and
    `FrameRasterizer` reads core health from `RenderFrame.coreHealth` (CORE carries no health) and
    draws the enemy pip only when `RenderPrimitive.health != null` (`ENEMY.health` nullable), all
    asserted by the pixel-smoke.
  - Low/pre-existing (NOT introduced by SG-003, still OPEN): `Camera.clamped()` clamps center to
    `[0..width]` inclusive while `screenToTile` clamps to `width-1` — reconcile if precise edge
    framing matters later. Not exercised by the SG-003 follow-up change.
  - Low/deferred (NOT blocking): the Android shell still uses `AsciiRenderer`; wiring the Android
    real launcher to consume `RenderFrame` is a tracked follow-up.
- RESOLVED (2026-07-04): the SG-001 verifier follow-up (default content gate validated only the
  sandbox pack) is closed — the gate now aggregates every `games/*/content/*` pack. See DONE
  "Default content gate coverage" below.
- RESOLVED (2026-07-04): the SG-002 "canonical scenario kills 0 enemies" follow-up is closed — a
  second canonical scenario `runScriptedKillScenario()` (pulse tower at (2,2), 2 kills / 4 shots in
  step 35) is now reported alongside the original by `me-sim-replay`/`me-benchmark`, so the default
  gate exercises kills+rewards. Kill hash `83a65da1a7881b2c`; original canonical hash
  `9c495d8ff30fd83d` unchanged (Option A, additive). See DONE "SG-002 kill/reward gate hardening".
- RESOLVED (2026-07-04): the SG-002 "silent reward drop" follow-up is closed — decision: keep the
  drop non-fatal (a full inventory is a valid game state) but make it observable. `SandboxRuntime.step`
  now surfaces `lastCommandOrError="reward_dropped:<res>:<amt>"` (not hashed, so replay-safe). Logic
  extracted to a pure `depositRewards(...)` helper and covered now by `SandboxRewardOverflowTest`
  (unit add/drop + capacity-bound `step` telemetry) without shipping capacities in default content.

## VERIFICATION

- ENG-003: selfcheck, full `test`, `projects`, content validation, replay, save-compat, benchmark,
  `:android:assembleDebug`, and `git diff --check` passed. Focused `JobExecutionSystemTest` and
  `SandboxJobExecutionTest` passed. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, and
  `3f02607020d48668`. Final benchmark: simulation 430 ms, kill 76 ms, spatial index 6.6127 ms.
  Final verifier passed with all boundary checks true.

- ENG-001: focused `AStarPathfindingTest` and `AgentPathPlannerTest` pass; full
  `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation, replay, save-compat,
  benchmark, and `git diff --check` pass. Replay hashes are `e4892bcc18f9d8dc`,
  `a763da4ac32b15b4`, and `3f02607020d48668`. Benchmark: canonical 413 ms, kill 102 ms,
  GoalField rebuild 13099400 ns, spatial index 6.3748 ms. Final verifier passed with no findings
  and all boundary checks true; tie/equal-g and `pathIndex` review findings were remediated.

- ENG-011 (2026-08-02): focused ENG-011 suite passed 24 tests after the `Int.MAX_VALUE` boundary
  test; full `test`, `projects`, content validation, replay, save-compat, benchmark,
  `:android:assembleDebug`, and `git diff --check` -> pass. Conditional simulation and balance
  reviewers -> pass; the simulation review's low overflow-test finding was resolved. Replay hashes:
  canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`, resist `3f02607020d48668`. Final benchmark:
  `sim=422 ms`, `kill=63 ms`, `goal-field=10743500 ns`, `spatial-index=6.4719 ms`.

- ENG-029 (2026-08-02): selfcheck, 160 focused core/content/sandbox/Android tests, full
  `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation (2 packs), replay,
  save-compat v10 matrix, benchmark, `:android:assembleDebug`, and `git diff --check` -> pass.
  Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`. Benchmark:
  `sim=412 ms`, `kill=83 ms`, `spatial-index-1k=5.62 ms`, `goal-field=9947100 ns`.

- PROC-003 (2026-07-29): documentation-only close-out; no engine code, tests, or gates applicable.
  New `Plane/15_domain_systems_sequencing.md`; PROC-003 card at `.claude/specs/done/` with the
  criterion (b) amendment and close note; ENGINE_ROADMAP.md rows/notes/recommended order synced;
  board status/location consistency covered by the PROC-013 checker.
- PROC-013 (2026-07-29): developer, tester, runner, and verifier passes are recorded; 23 card
  migrations, board checker, selfcheck wiring, checker/selfcheck exit semantics, and no-ADR scope
  were verified. `git diff --check` is required for this docs close-out.

## ENG-001 Close-out (2026-08-02)

### DONE

- Added deterministic `engine-world` A* and `engine-ai` `AgentPathPlanner`; preserved the
  `GridPathfinder` request/result API and covered occupied starts, `pathIndex > 0`, tie/equal-g,
  route/world changes, bounds, blocked cells, and no-path behavior.

### DECISIONS

- No ADR, save/content/render/Android/dependency changes, or game-bundle traceability update.
- Wave enemies stay on ENG-002 `GoalField`; full Movement/job tick integration is deferred to
  ENG-003/ENG-004.

### NEXT

- Run `/me --feature --next` for ENG-003 (JobBoard wired into tick).

### BLOCKERS

- No implementation blocker. The missing full Movement/job tick wiring is an intentional boundary,
  not an ENG-001 defect; colony demand remains vision-only pending its recorded re-entry trigger.

### VERIFICATION

- Focused A* and planner tests, full `test`, `projects`, content-validate, sim-replay, save-compat,
  benchmark, and diff-check passed. Replay hashes: `e4892bcc18f9d8dc` /
  `a763da4ac32b15b4` / `3f02607020d48668`. Metrics: 413 ms / 102 ms / 13099400 ns / 6.3748 ms.
- Final verifier: pass, no findings, all boundary checks true.
- ENG-020 (2026-07-29): focused engine-defense tests (14), focused engine-devtools tests (16), full
  `.\gradlew.bat test`, `.\gradlew.bat projects` with explicit Android Studio `JAVA_HOME`, content
  validation (2 packs), replay, save-compat, benchmark, and `git diff --check` -> pass. Replay
  hashes remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; benchmark:
  `enemy_count=1024`, `tower_count=16`, `query_count=16`, `sim_ms=5.3045`. `me-simulation-reviewer`
  and `me-verifier` -> pass; all boundary checks are true. The low `me-tester` parity follow-up is
  recorded above.
- ENG-009 (2026-07-28): full `.\gradlew.bat test` and `.\gradlew.bat projects`, content validation
  (2 packs), replay, save-compat, benchmark, and `:android:assembleDebug` -> pass. Replay hashes
  remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; benchmark: canonical `324 ms`,
  kill `62 ms`, goal-field rebuild `7168200 ns`. Balance review and `me-verifier` -> pass with all
  boundary checks true. Transient immutable events are excluded from save/stable-hash state; save
  version remains `8`.
- ENG-030 (2026-07-21): full `.\gradlew.bat test` with the JDK 17 fallback, content validation
  (2 packs), replay, save-compat, benchmark, and `android:assembleDebug` -> pass. Replay hashes:
  canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`; benchmark: canonical `473 ms`, kill
  `78 ms`, goal-field rebuild `8222800 ns`. Renderer, simulation, save, Android, and final
  verifier reviews -> pass. Exact scheduled-tick rejection, active-wave rejection, pending
  command identity, and v1-v7 migration are covered. Balance review -> partial: current content
  packs are valid and contain no hardcoded bonus; its schema-documentation gap was closed in this
  docs close-out, and the optional bonus remains unconfigured pending an approved balance value.
- ENG-028 (2026-07-28): `scripts/me-selfcheck.ps1`, full `.\gradlew.bat test`, `.\gradlew.bat projects`,
  focused engine-content/engine-render/games:sandbox/desktop tests, `scripts/me-content-validate.ps1`
  (2 packs), `scripts/me-sim-replay.ps1`, `scripts/me-save-compat.ps1`, `scripts/me-benchmark.ps1`,
  `:android:testDebugUnitTest`, `:android:assembleDebug`, `:desktop:run`, and `git diff --check`
  -> pass. Replay hashes remain canonical `12a65fd2b87593cf`/`bb37eefc1903cc77`; benchmark is
  `sim_ms=341`/`71`, goal-field rebuild `9726600 ns`. Tester initially found and the local repair
  closed the missing `building:marker` -> `RenderFrame` path. Domain reviewer agents were not
  available due the agent-thread limit; local simulation/render/save/Android/content boundary review
  passed. Device/instrumented Android AssetManager and PROC-009 golden screenshot checks remain
  manual-pending.
- ENG-015 (2026-07-21): `scripts/me-selfcheck.ps1`, full `.\gradlew.bat test`,
  `:android:testDebugUnitTest`, `:android:assembleDebug`, content validation, replay, save-compat,
  and benchmark -> pass. Replay hashes: canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`;
  benchmark: canonical `328 ms`, kill `66 ms`, rebuild `4.1305 ms`. `me-verifier` -> pass with
  all `boundary_checks` true. Device/instrumentation and FrameMetrics/JankStats evidence remain
  pending.
- ENG-008 (2026-07-18): full `./gradlew.bat test`, content validation (2 packs), replay,
  save-compat, and benchmark -> pass. Replay hashes: canonical `12a65fd2b87593cf`, kill
  `bb37eefc1903cc77`; benchmark: canonical `432 ms`, kill `79 ms`, 64x64 goal-field rebuild
  `5.3678 ms`. Selector-mode, v1-default, queued-switch/replay, HUD, v1-v6 -> v7 migration, and
  pending-command save coverage pass. Required reviewers and final `me-verifier` -> pass.
- ENG-013 (2026-07-18): final full `./gradlew.bat test` -> pass; content validation -> pass
  (`validated 2 pack(s)`); replay -> pass with canonical `463d87684ca6cbee` and kill
  `40c7bda7e3bc1316`; save-compat -> pass; benchmark -> pass (`canonical=335 ms`, `kill=70 ms`,
  `goal_field_rebuild_ns=6505600`). Required domain reviewers and final `me-verifier` -> pass. The initial
  test/content failures were missing new required `sellRefundRatio` fields in a test fixture and
  the Signal Garden pack; the fields were added and all gates rerun successfully. Telemetry event
  records `retro_due=false`. No ADR: additive data/content behavior, no dependency edge or save
  schema change; `SAVE_VERSION` stays `6`.
- ENG-002 (2026-07-18): full `./gradlew.bat test` -> pass; replay -> pass with canonical
  `463d87684ca6cbee`, kill `40c7bda7e3bc1316`, and maze golden `ed0354584405ec49`; save-compat ->
  pass; benchmark -> pass, including final 64x64 goal-field rebuild metric `4.1904 ms`. Final
  verifier accepted all four acceptance criteria with no findings. The only noted warning is the
  pre-existing Gradle 10 deprecation warning from AGP internals.
- ENG-027 (2026-07-18): final runner -> pass. Full tests pass; content validation passes for 2 packs;
  replay hashes remain canonical `9c495d8ff30fd83d` and kill `83a65da1a7881b2c`; save-compat passes;
  benchmark passes at `canonical=295 ms`, `kill=45 ms`; `android:assembleDebug` passes. Headless HUD,
  content localization, deterministic per-tower metrics, input/render boundary, density-aware layout,
  and save v1-v6 coverage are included. `me-verifier` accepted all criteria and boundary checks.
  Device build/select/upgrade/lifecycle, fontScale/long labels, FrameMetrics/JankStats/allocations,
  and malformed-v6 missing/duplicate `towerMetrics` remain non-blocking manual/hardening work.
- ENG-026 (2026-07-16): `:android:testDebugUnitTest --tests
  dev.myengine.android.FixedTickFrameLoopTest --rerun-tasks` -> pass; `:android:assembleDebug` ->
  pass; replay -> pass with canonical `9c495d8ff30fd83d` and kill `83a65da1a7881b2c`; save-compat
  -> pass. `me-tester` reported no test-file changes; `me-verifier` -> pass. No device/emulator
  smoke or FrameMetrics/JankStats / Allocation Tracker profile was run.

- MTD-005 (2026-07-16): runner -> pass. With Android Studio JBR,
  `:engine-render:test` -> BUILD SUCCESSFUL (17s), `:games:sandbox:test` -> BUILD SUCCESSFUL (17s),
  and `:android:assembleDebug` -> BUILD SUCCESSFUL (22s). `scripts\me-sim-replay.ps1` -> pass at
  tick 35; canonical hash `9c495d8ff30fd83d` and kill hash `83a65da1a7881b2c` are unchanged.
  Content validation, save-compat, and benchmark were not run by scope. `me-verifier` -> pass with
  Android-free simulation, snapshot-only rendering, external content, and versioned saves all true.
  No device smoke or FrameMetrics/JankStats/Allocation Tracker profile was run.

- Plugin validation for `codex-plugins/me-dev` -> pass.
- Plugin validation for `codex-plugins/me-spec` -> pass.
- `.\gradlew.bat projects` -> pass.
- `.\gradlew.bat test` -> pass.
- `scripts\me-content-validate.ps1` -> pass.
- `scripts\me-sim-replay.ps1` -> pass.
- `scripts\me-save-compat.ps1` -> pass.
- `scripts\me-benchmark.ps1` -> pass.
- `scripts\me-record-run.ps1` synthetic event -> pass.
- `scripts\me-retro.ps1` -> pass.
- `.\gradlew.bat desktop:run` -> pass.
- `.\gradlew.bat android:assembleDebug` -> pass.
- SG-001 (2026-07-04): `.\gradlew.bat test` -> pass (incl. `SignalGardenContentPackTest`);
  `engine-devtools:run content-report <abs path to games/signal-garden/content/signal-garden>`
  -> `{"pack_id":"signal-garden","valid":true,"errors":[]}`.
- SG-002 (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl. new `DefenseRuntimeTest`
  conservation + leaked-enemy cases, `SandboxRewardDepositTest`, and `SandboxVerticalSliceTest`
  kill-scenario determinism + reward save-roundtrip cases); `scripts\me-sim-replay.ps1` -> pass,
  hash `9c495d8ff30fd83d` UNCHANGED (canonical scenario kills 0 enemies); `scripts\me-save-compat.ps1`
  -> pass; `scripts\me-benchmark.ps1` -> pass (enemies_killed=0, tower_shots=0 for the canonical
  scenario); `scripts\me-record-run.ps1` -> pass (events=3).
- SG-002 kill/reward gate hardening (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl.
  new `DevtoolReportsTest`, `SandboxVerticalSliceTest`, and `SandboxRewardOverflowTest` cases);
  `scripts\me-sim-replay.ps1` -> pass, `{"scenarios":[canonical 9c495d8ff30fd83d kills=0,
  kill 83a65da1a7881b2c kills=2]}`; `scripts\me-benchmark.ps1` -> pass (kill enemies_killed=2,
  tower_shots=4; canonical 0/0 unchanged); `scripts\me-save-compat.ps1` -> pass;
  `.\gradlew.bat desktop:run` -> canonical hash `9c495d8ff30fd83d` unchanged.
- SG-003 (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl. extended `RenderBoundaryTest`
  camera pan/zoom cases, new `PlaceholderRenderSurfaceTest`, and new `SandboxRenderNonMutationTest`);
  `.\gradlew.bat desktop:run` -> pass (exit 0; ASCII map path unaffected);
  `scripts\me-record-run.ps1` -> pass (events=4).
- SG-003 follow-up (2026-07-04): new files
  `engine-render/src/main/kotlin/dev/myengine/render/RenderPalette.kt`,
  `desktop/src/main/kotlin/dev/myengine/desktop/FrameRasterizer.kt`,
  `desktop/src/test/kotlin/dev/myengine/desktop/FrameRasterizerPixelSmokeTest.kt`; edits to
  `desktop/src/main/kotlin/dev/myengine/desktop/DesktopLauncher.kt` and `desktop/build.gradle.kts`.
  `.\gradlew.bat test` -> pass (full suite incl. new `FrameRasterizerPixelSmokeTest` + existing
  `PlaceholderRenderSurfaceTest`/`RenderBoundaryTest`/`SandboxRenderNonMutationTest`);
  `.\gradlew.bat desktop:run` -> exit 0, `hash=9c495d8ff30fd83d` (canonical, unchanged), ASCII map,
  `png=D:\Pet\MyEngine\desktop\build\render-smoke.png`; `.\gradlew.bat android:assembleDebug` ->
  BUILD SUCCESSFUL (proves `RenderPalette` did not pull java.awt into the Android artifact).

- SG-004 (2026-07-04): `.\gradlew.bat test` -> pass (full suite incl. new
  `SandboxSessionLifecycleTest`: save/restore roundtrip, pause/resume determinism, seed roundtrip,
  independent-runtime, future/non-numeric version rejection); `scripts\me-save-compat.ps1` -> pass;
  `.\gradlew.bat android:assembleDebug` -> pass (thin Activity+Bundle adapter compiles/links);
  `scripts\me-record-run.ps1` -> pass (events=7). On-device Bundle round-trip device-pending (see
  BLOCKERS).
- SG-004 follow-up (2026-07-05, sandbox save-format v2 / pending CommandQueue persistence):
  `.\gradlew.bat :games:sandbox:test` -> pass; full `.\gradlew.bat test` -> pass (incl. updated
  `SandboxSessionLifecycleTest`: pending-command save/restore/resume regression, v1->v2 migration,
  CommandId/Tick preservation, retargeted version-rejection tests); `scripts\me-save-compat.ps1` ->
  pass; `.\gradlew.bat :android:compileDebugKotlin` -> pass (after pointing a machine-local,
  gitignored `local.properties` at the Android SDK — not a code change). All gate/review verdicts:
  pass across the board (me-architect, me-engine-developer, me-tester, me-runner,
  me-simulation-reviewer, me-save-compat-reviewer, me-android-performance, me-verifier), only
  low/non-blocking-medium findings noted (see BLOCKERS "SG-004 follow-up new low-severity items").
- SG-005 (2026-07-05): `.\gradlew.bat :engine-devtools:test` -> pass (incl. parser-backed
  `BalanceDeltaReport` JSON structure and CLI stdout tests); full `.\gradlew.bat test` -> pass;
  `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`); `scripts\me-sim-replay.ps1` ->
  pass (canonical `9c495d8ff30fd83d`, kill `83a65da1a7881b2c`); `scripts\me-save-compat.ps1` ->
  pass; `scripts\me-benchmark.ps1` -> pass; `.\gradlew.bat -q :engine-devtools:run
  --args="balance-report"` -> pass, one JSON object with enemy/core/resource warnings. Final
  verifier verdict pass, all four boundary checks true, no findings. Telemetry recorded via
  `scripts\me-record-run.ps1` (events=9, `reflect_required=false`).
- MTD-001/MTD-002 (2026-07-05): `.\gradlew.bat :games:sandbox:test` -> pass (incl. new
  `SandboxTowerCostGatingTest`); full `.\gradlew.bat test` -> pass;
  `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`); `scripts\me-sim-replay.ps1` ->
  pass (canonical `9c495d8ff30fd83d`, kill `83a65da1a7881b2c`); `scripts\me-save-compat.ps1` ->
  pass; `scripts\me-benchmark.ps1` -> pass; `scripts\me-record-run.ps1` -> recorded event 10 with
  `reflect_required=true`; `scripts\me-retro.ps1` -> pass, wrote
  `.ai/retro/retro-2026-07-05.md` with no failure clusters. Initial bare Gradle invocation failed
  before testing because inherited `JAVA_HOME` was invalid; reruns with the documented Android Studio
  JBR passed.
- MTD-003 (2026-07-05): `.\gradlew.bat :engine-content:test :games:sandbox:test` -> pass (incl.
  `ContentPackLoaderTest` upgrade-tier validation and new `SandboxTowerUpgradeTest`); full
  `.\gradlew.bat test` -> pass; `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`);
  `scripts\me-sim-replay.ps1` -> pass (canonical `9c495d8ff30fd83d`, kill `83a65da1a7881b2c`
  unchanged); `scripts\me-save-compat.ps1` -> pass; `scripts\me-benchmark.ps1` -> pass. Reviews:
  `me-architect` no ADR, `me-tester` pass, `me-simulation-reviewer` pass, `me-save-compat-reviewer`
  pass (two stale KDoc lows fixed), `me-renderer-qa` pass (one low future command-API refactor note),
  `me-balance-simulator` pass, `me-verifier` pass with all four boundary checks true.
  `scripts\me-record-run.ps1` -> recorded event 11 with `reflect_required=false`.
- MTD-004 (2026-07-16): `DifficultyContent` + optional `difficulties.properties`,
  `ContentRegistry.resolveDifficulty`, deterministic `BigDecimal` scaling before tick 0, and
  `SandboxGame`/`SandboxSession` setup wiring; source values from the MyTD balance plan. No
  save-format/Android/render changes; no ADR. `.\gradlew.bat :engine-content:test :games:sandbox:test` ->
  pass; full `.\gradlew.bat test` -> pass; `scripts\me-content-validate.ps1` -> pass
  (`validated 2 pack(s)`); `scripts\me-sim-replay.ps1` -> pass (`9c495d8ff30fd83d`,
  `83a65da1a7881b2c`); `scripts\me-save-compat.ps1` -> pass; `scripts\me-benchmark.ps1` -> pass
  (`sim_ms=429` implementation run). `me-verifier` -> pass with all boundary checks true.

- PROC v0.2.0 (2026-07-04): `scripts\me-selfcheck.ps1` -> pass after all adapter edits;
  `scripts\me-record-run.ps1` (improve event, events=6) -> new fields present in the JSONL and
  `reflect_required` in the output; `scripts\me-retro.ps1` -> pass, wrote
  `.ai/retro/retro-2026-07-04.md` (legacy events without new fields aggregate cleanly);
  `.\gradlew.bat test` -> pass (engine code untouched by this batch).

## ENG-024 VERIFICATION CLOSE-OUT

- Full `./gradlew.bat test`, canonical replay, save-compat, `./gradlew.bat android:assembleDebug`,
  and static scan -> pass.
- Canonical replay hashes `9c495d8ff30fd83d` and `83a65da1a7881b2c` unchanged.
- `me-verifier` -> pass; all `boundary_checks` true.
- Content validation and benchmark were not run by scope.

## DX-008 / ENG-005 VERIFICATION CLOSE-OUT

- `./gradlew.bat test` -> pass (including map loader fixtures, canonical-map parity, and save v4
  migration/restore coverage).
- `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`);
  `scripts\me-sim-replay.ps1` -> pass (`9c495d8ff30fd83d`, `83a65da1a7881b2c` unchanged);
  `scripts\me-save-compat.ps1` -> pass; `scripts\me-benchmark.ps1` -> pass.
- Reviews/verifier -> pass. Low non-blocking Android asset-loading and map-balance-report follow-ups
  are recorded in BLOCKERS.

## ENG-014 VERIFICATION CLOSE-OUT

- Serial full Gradle suite (--no-daemon --max-workers=1) -> pass; serial mode followed earlier
  parallel native-memory exhaustion only, not an assertion failure.
- Content validation (2 packs), replay hashes (9c495d8ff30fd83d, 83a65da1a7881b2c), save-compat,
  benchmark, and Android assemble -> pass.

## NOTES

- The workspace is not a git repository.
- Claude adapters are now installable plugins (`claude-plugins/me-dev`,
  `claude-plugins/me-spec`) shipped via `.claude-plugin/marketplace.json`; verify wiring
  with `scripts\me-selfcheck.ps1`.
- Gradle emits the same Gradle 10 deprecation warning noted earlier; current builds pass.

## ENG-010 Close-out (2026-08-01)

### DONE

- Implemented content-defined `slow` and `dot` status effects with `magnitude`, `durationTicks`,
  and `refresh`/`stack`/`ignore` stacking validation.
- Added deterministic entity effect state, DoT/slow simulation integration, sorted immutable
  snapshot/render tags, and `SandboxSaveCodec` v9 with v1-v8 migration plus v9 effect roundtrip.
- Added loader, defense, save, render, slow replay, and overflow-boundary tests; moved ENG-010 to
  `.claude/specs/done/` and synced the engine roadmap/Plane.

### DECISIONS

- No ADR: additive generic entity state preserves the existing Android-free simulation,
  snapshot-only rendering, external content, and versioned-save boundaries.
- Default packs and balance values remain unchanged; no Android simulation logic was added.

### NEXT

- Run `/me --feature --next` for ENG-016 (incident execution pipeline + RNG fix).

### BLOCKERS

- Save-compat reviewer passed with no findings; renderer review passed after the snapshot tag
  defensive-copy fix. The simulation review's non-enemy DoT metrics finding was fixed and covered
  by a regression test; integer-floor partial slow remains an intentional scope decision with a
  focused test. Other conditional reviewers and final verifier could not all be spawned because
  the app subagent-thread limit was reached.
- Device/emulator smoke and FrameMetrics/JankStats profiling remain manual-pending from earlier
  Android work.

### VERIFICATION

- Passed full `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation, replay,
  save-compat, benchmark, `.\gradlew.bat :android:assembleDebug`, focused tests, and
  `git diff --check`.

## ENG-016 Close-out (2026-08-02)

### DONE

- Closed the incident execution pipeline with optional incident definitions, cadence start/end,
  pacing threat windows, cooldowns, and typed `spawn_wave`, `resource_event`, and `modifier` effects.
- The stateful deterministic director uses a persistent simulation RNG cursor. The atomic sandbox
  interpreter preflights all references, capacity, and arithmetic before mutation; repeated
  resource/modifier effects aggregate via `Long` before overflow checks, and cross-field validation
  reports `ContentValidationError` with the incident field path.
- `SandboxSaveCodec` v10 persists RNG cursor, director state, executions, and active modifiers with
  v1-v9 migration. The card moved to `.claude/specs/done/`; schema, roadmap, Plane, STATE, and DIGEST
  close-out docs are synchronized.

### DECISIONS

- No ADR: the implementation remains inside the existing Android-free simulation, data-driven
  content, stable-hash, and versioned-save boundaries. Default pack balance and Android production,
  renderer, and input boundaries are unchanged.
- Remediation rerun metrics supersede the initial first-run `614/120 ms` values.

### NEXT

- Run `/me --feature --next` for PROC-007, then continue `ENG-021 -> ENG-029 -> ENG-012 -> ENG-007
  -> ENG-018`.

### BLOCKERS

- No ENG-016 implementation blocker remains. No device proof is claimed; existing Android/device,
  FrameMetrics/JankStats, and other manual performance follow-ups remain pending.
- Gradle verification requires process-local `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.

### VERIFICATION

- Full Gradle test/projects/content/replay/save-compat/benchmark/diff-check lanes passed; focused
  `SandboxIncidentTest` and content tests passed; simulation/save reviews passed.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Remediation benchmark: `sim=418 ms`, `kill=85 ms`, `spatial-index-1k=6.1036 ms`,
  `goal-field=10.427 ms`.

## PROC-007 Close-out (2026-08-02)

### DONE

- Added checked-in `games/sandbox/src/test/resources/save-fixtures/v1.properties` through
  `v10.properties` covering every released sandbox save version.
- Added `SandboxSaveMigrationMatrixTest` with an independently constructed canonical state oracle,
  non-default tick/metrics/inventory/producer/entity data, stable-hash comparison, and repeated
  decode determinism checks.
- Extended `scripts/me-save-compat.ps1` to execute the matrix and emit its result in the compact JSON.

### DECISIONS

- No ADR: PROC-007 strengthens the existing test/process gate without changing production save
  encoding, `SandboxSaveCodec.SAVE_VERSION` (10), or migration behavior.

### NEXT

- Run `/me --feature --next` for ENG-029 (audio event hooks), then continue
  `ENG-012 -> ENG-007 -> ENG-018`.

### BLOCKERS

- None for PROC-007. Existing Android/device and FrameMetrics/JankStats follow-ups remain
  manual-pending and were not part of this test-only change.

### VERIFICATION

- Focused matrix, full tests, projects, content validation, replay, save-compat, benchmark, and
  `git diff --check` passed. Save reviewer and final verifier passed with no findings.
- Replay hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`; matrix passed on two runs.

## ENG-021 Close-out (2026-08-02)

### DONE

- Accepted named slots under the separate `slots/` namespace and config-driven rotating autosaves
  under `autosave/`.
- Writes use a flushed temporary file plus `ATOMIC_MOVE`; there is no non-atomic fallback, so a
  failed atomic replacement preserves the previous slot.
- Slot metadata is readable without a full state load. Corruption-only fallback selects the latest
  good autosave; future or incompatible saves are rejected explicitly.
- `SandboxSaveCodec.SAVE_VERSION` remains 10 and the Android Bundle lifecycle save path is
  unchanged.

### DECISIONS

- No ADR: the feature remains at the existing Android-free save boundary with no codec-version,
  content-schema, dependency, or lifecycle-path change.

### NEXT

- Run `/me --feature --next` for ENG-029 (audio event hooks), then continue
  `ENG-012 -> ENG-007 -> ENG-018`.

### BLOCKERS

- No ENG-021 implementation blocker remains. The pre-existing low manual device/emulator Bundle
  save/restore smoke limitation remains; no device proof is claimed.

### VERIFICATION

- Full tests, projects, content validation, replay, save-compat matrix, benchmark, Android
  assemble, focused `SandboxSaveSlotsTest`, and `git diff --check` all passed.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Benchmark: `sim=473 ms`, `kill=87 ms`, `spatial-index-1k=6.6675 ms`,
  `goal-field rebuild=10459200 ns`.

## ENG-029 Close-out (2026-08-02)

### DONE

- Added the transient immutable `GameplayEvent` feed on the latest completed snapshot tick for
  shot, hit, death, wave-start, build, and sell events, with deterministic tick/ordinal ordering.
- Added optional `sounds.properties` event-id -> pack-relative file mappings with normalized-id,
  duplicate, blank-path, root-containment, and regular-file validation.
- Added the Android `SoundPoolPresentationConsumer` with cursor deduplication and presentation-only
  volume/mute state. `SandboxSaveCodec.SAVE_VERSION` remains 10; events are excluded from saves and
  stable hashes.

### DECISIONS

- No ADR: simulation remains Android/audio-free; the Android presentation consumer owns audio
  decoding/playback. No plugin/skill or canonical agent-contract changed.

### NEXT

- Run `/me --feature --next` for ENG-012, then continue `ENG-007 -> ENG-018`.

### BLOCKERS

- No ENG-029 implementation blocker remains. No real device/emulator SoundPool playback, volume/
  mute, or frame-metrics evidence was available. The Android reviewer contract was unavailable;
  local boundary and gate evidence passed.

### VERIFICATION

- Selfcheck, 160 focused core/content/sandbox/Android tests, full `.\gradlew.bat test`,
  `.\gradlew.bat projects`, content validation (2 packs), replay, save-compat v10 matrix,
  benchmark, `:android:assembleDebug`, and `git diff --check` passed.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Benchmark: `sim=412 ms`, `kill=83 ms`, `spatial-index-1k=5.62 ms`,
  `goal-field=9947100 ns`.
- The initial invalid `:android:test --tests` invocation was corrected to
  `:android:testDebugUnitTest --tests`; it is not a feature failure.

## ENG-007 Close-out (2026-08-02)

### DONE

- Added optional `WaveContent.spawnSelection` with default/all or pipe-separated named spawn ids,
  cross-reference/reachability validation, reserved map spawn-id guards, and a checked-in
  multi-spawn fixture.
- Scheduled, early, and incident waves use deterministic sorted spawn-id -> authored `WaveSpawn`
  -> instance ordering. `SandboxSaveCodec.SAVE_VERSION` remains 11.

### DECISIONS

- No ADR: the additive content/runtime routing change preserves the existing save format and
  deterministic replay contract.

### NEXT

- Run `/me --feature --next` for ENG-018. ENG-011 remains the separate armor + damage types card.

### BLOCKERS

- No ENG-007 implementation blocker remains. The only low finding is the pre-existing Gradle 10
  deprecation warning; no device/emulator claim is made beyond `assembleDebug`.

### VERIFICATION

- Runner gates passed: full tests, projects, content validation (2 existing packs plus the checked-in
  multi-spawn fixture), replay, save-compat v1-v11 matrix plus `SandboxMultiSpawnTest`, benchmark,
  `:android:assembleDebug`, and `git diff --check`.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Benchmark: `sim_ms=364`, `kill_sim_ms=87`, `goal_field_rebuild_ns=9048200`,
  `spatial_index_1k_ms=5.7197`. Tester remediation focused tests: 59 passed; simulation,
  save-compat, and verifier reviews passed.
## ENG-018 Close-out (2026-08-02)

### DONE

- Added `endless.properties` parsing/validation for composition cycles, interval, spawn selection,
  and content-defined count/health/reward growth.
- Added deterministic `EndlessWaveGenerator` using the shared simulation RNG and stable generated
  ids, integrated scheduled/early sandbox spawning, effective enemy state, and `NO_WIN` semantics.
- Added `endless-scaling` / `endless-wave-scaling` devtools JSON report and focused tests across
  content, defense, sandbox save/replay, and devtools.

### DECISIONS

- No ADR; no save version bump or Android dependency. `SandboxSaveCodec.SAVE_VERSION` remains 11.
- Existing finite-wave ordering and canonical replay hashes remain unchanged.

### NEXT

- Run `/me --feature --next` for ENG-011 (enemy armor + damage types).

### BLOCKERS

- No implementation blocker. The conditional simulation reviewer and final verifier worker threads
  timed out; local boundary review and full runner evidence cover the feature. No device/emulator
  proof is claimed.

### VERIFICATION

- `gradlew.bat test`, `gradlew.bat projects`, content validation, replay, save-compat matrix,
  benchmark, focused tests, and `git diff --check` passed.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.

## ENG-011 Close-out (2026-08-02)

### DONE

- Approved Option A is complete: static `DamageTypeContent`, tower `damageTypeId`, enemy
  percentage `resists` in `0..100`, and deterministic bidirectional reference validation.
- `DamageFormula` applies
  `floor(baseDamage * max(0,100 - distance*falloffPercent) * (100-resistPercent) / 10000)`
  with `Long` intermediates and one final floor to direct and splash damage; zero damage emits no
  `HitEvent`. Effective-DPS reporting is deterministic under single-target, in-range, no-splash,
  `ticks_per_second=20` assumptions.
- `SandboxSaveCodec.SAVE_VERSION=11` remains unchanged; typed metadata is registry-derived and
  not persisted.

### DECISIONS

- No ADR: the approved additive Option A preserves legacy packs, Android-free simulation, and the
  existing versioned-save boundary.

### NEXT

- Run `/me --feature --next` for ENG-019 (walls + player-placed blockers), the next remaining
  TD-depth item with demand 2.

### BLOCKERS

- No implementation blocker. No device/emulator evidence is claimed; existing manual Android/device
  and performance follow-ups remain pending where previously recorded.

### VERIFICATION

- Focused ENG-011 suite passed 24 tests after the `Int.MAX_VALUE` Long-intermediate boundary test.
- Full tests, projects, content validation, replay, save-compat, benchmark, Android assemble, and
  `git diff --check` passed. Conditional simulation and balance reviewers passed; the simulation
  review's low overflow-test finding was resolved.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`, resist
  `3f02607020d48668`.
- Final benchmark: `sim=422 ms`, `kill=63 ms`, `goal-field=10743500 ns`,
  `spatial-index=6.4719 ms`.

## ENG-031 Close-out (2026-08-02)

### DONE

- Accepted Option A is complete: deterministic zone commands/store state, validated stockpile
  resource filters, one-shot harvest-node designation jobs on the `JobBoard`, immutable snapshot
  zone projection, and `SandboxSaveCodec` v14 with v1-v13 migration.

### DECISIONS

- No ADR and no game-bundle traceability update. Colony demand remains vision-only. Hauling,
  stockpile quantities/capacity, depletion/repeated harvest, and actual Android `RenderFrame`/view
  overlay rendering are deferred to follow-up/ENG-004 scope. No plugin/skill/pipeline contract
  changed.

### NEXT

- Run `/me --feature --next` for ENG-004 (first worker agent MVP / hauling), then ENG-032.

### BLOCKERS

- No implementation blocker. Non-blocking follow-ups: removing a claimed/in-progress designation
  can leave its job and permit a second job on the same node; generic pending-command delimiter
  escaping remains a pre-existing codec concern while current ENG-031 ids are regex-safe;
  `RenderFrame`/Android view do not yet consume `snapshot.zones`; per-frame zone snapshot
  allocations need later profiling/consumer work.

### VERIFICATION

- Selfcheck and focused ENG-031/remediation tests passed. Full runner passed with
  `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`: `gradlew test`, `projects`, content
  validation (2 packs), replay, save-compat, benchmark, `android:assembleDebug`, and
  `git diff --check`. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`,
  `3f02607020d48668`; benchmark: canonical 413 ms, kill 85 ms, spatial index 5.2368 ms,
  goal rebuild 10958000 ns. Simulation, renderer, save, Android, and verifier reviews passed or
  reported only the non-blocking findings above; verifier boundary checks were all true.

## ENG-004 Close-out (2026-08-02)

### DONE

- Added data-defined workers (`speedTilesPerTick`, `capacity`) and `WorkerComponent`; carry uses the
  existing simulation-owned `InventoryComponent` so no Android/render dependency was introduced.
- Added typed haul payload/phase and a deterministic `HaulingSystem`: workers process by EntityId,
  jobs by priority/id, reserve source quantities atomically, pick up, move at content speed, and
  deposit into validated stockpile tiles. Generic `JobExecutionSystem` excludes typed haul jobs
  without changing legacy generic-job behavior.
- Positioned producer outputs are materialized as `producer:<id>` haul sources instead of being
  credited twice to global inventory. Stockpile contents, source reservations, worker carry, producer
  positions, and haul phases are persisted by `SandboxSaveCodec` v15; v1-v14 decode with empty new state.

### DECISIONS

- No ADR, game-bundle traceability update, or plugin/skill/pipeline contract change.
- Legacy stable replay hashes remain unchanged when no new worker/haul state is present. Generic
  stockpile capacity/depletion, worker spawn commands, and Android overlay consumption remain follow-up scope.

### NEXT

- Run `/me --feature --next` for ENG-032 (construction blueprints).

### BLOCKERS

- No implementation blocker. No device/emulator or visual-golden proof is claimed beyond assembleDebug.
- Delegated developer and verifier workers timed out; production/test completion and boundary review
  were completed locally after their bounded retries. Scout and architect contracts passed.

### VERIFICATION

- Focused hauling/content tests and full `.\gradlew.bat test` passed (132 sandbox tests plus
  engine/desktop/Android suites).
- `.\gradlew.bat projects`, content validation, replay, save-compat, benchmark,
  `.\gradlew.bat :android:assembleDebug`, selfcheck, and `git diff --check` passed.
- Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`; benchmark: `sim=559 ms`, `kill=111 ms`,
  GoalField rebuild `7144000 ns`, spatial index `7.7432 ms`.

## ENG-032 close-out

### DONE

- Added `PlaceBlueprintCommand` and `CancelBlueprintCommand`, `ConstructionSiteStore`, and a
  construction-specific haul destination while retaining the existing immediate `PlaceBuilding`
  flow.
- Blueprints stay non-blocking until completion. Placement checks prospective route safety;
  delivery uses ENG-004 hauling; a generic build job applies content-defined `buildWorkTicks`;
  completion spawns the building and rebuilds `GoalField`.
- Source choice is automatic and deterministic: eligible sources are considered in ascending
  `sourceId` order and retried when a source cannot currently satisfy the remaining material.
- Cancellation returns delivered and worker-carried in-transit material to the original
  `HaulSourceStore`, releases reservations, and clears assignments.
- Save format is v16; construction sites and source-aware delivery ledgers roundtrip, while v1-v15
  saves migrate with empty construction state. Pending blueprint/cancel commands roundtrip too.

### DECISIONS

- User-approved refund sink is the original `HaulSourceStore`; global `Inventory` is not used for
  construction refunds. User-approved source selection is automatic sorted `sourceId` selection.
- One existing `BuildingContent` material cost is persisted by source; `buildWorkTicks` is an
  additive optional field defaulting to 1. No multi-material schema, blueprint overlay, or Android
  renderer change is included. No ADR or plugin version bump was needed.

### NEXT

- ENG-032 was complete and ENG-033 was the next candidate at the time of this historical entry;
  ENG-033 is now complete. Inspect the backlog for the next scoped feature.

### BLOCKERS

- No implementation blocker. Developer/tester/reviewer subagent envelopes timed out after bounded
  waits; the orchestrator completed local implementation, tests, gates, and boundary review. No
  device/emulator or visual-golden proof is claimed beyond Android `assembleDebug`. The required
  read-only `me-reflect` retry also timed out; no reflection proposal was applied.

### VERIFICATION

- `gradlew test`, `gradlew projects`, content validation, replay, save-compat, benchmark, selfcheck,
  `:android:assembleDebug`, focused construction tests, and `git diff --check` passed.
- Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`; save compatibility is v16
  with v1-v15 migration.
