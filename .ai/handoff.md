# MyEngine Handoff

Last updated: 2026-07-04 (SG-002 kill/reward gate hardening: kill-bearing canonical scenario + reward-drop telemetry)  
Owner: Claude

## DONE

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

## DECISIONS

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

## MyTD (2026-07-04)

- `/me-spec --greenfield-game` clone-intake vs `com.vipubstd.games.block.defense` produced an
  original, traceable MyTD bundle at `D:/Pet/MyTD/spec` (13 files, FR-001..FR-016 fully traced).
- Both human gates passed. Mechanics cloned; all names/numbers original; art own-style; reference-IP
  reuse gated behind a dedicated ADR.
- Backlog bridge added: `MTD-001` reward deposit, `MTD-002` gold-cost gating, `MTD-003` tower upgrade
  hook, `MTD-004` difficulty modifiers, `MTD-005` render/input surface. Content (EG-006) stays in the
  MyTD bundle.

## NEXT

Implement Signal Garden `SG-003` (placeholder render surface):

```powershell
Get-Content -Raw games\signal-garden\ROADMAP.md
Get-Content -Raw .claude\specs\backlog\SG-003-render-surface.md
```

## BLOCKERS

- None for `SG-003`.
- Known follow-ups: real render surface (now SG-003), Android lifecycle save smoke, content
  suspicious-value report.
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

## NOTES

- The workspace is not a git repository.
- Claude adapters are now installable plugins (`claude-plugins/me-dev`,
  `claude-plugins/me-spec`) shipped via `.claude-plugin/marketplace.json`; verify wiring
  with `scripts\me-selfcheck.ps1`.
- Gradle emits the same Gradle 10 deprecation warning noted earlier; current builds pass.
