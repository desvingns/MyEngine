# MyEngine State

Last updated: 2026-07-04  
Active phase: Phase 00-14 complete; Signal Garden SG-002 complete + kill/reward gate hardening, SG-003 next  
Owner of last update: Claude (2026-07-04: added kill-bearing canonical scenario to default sim/benchmark gates; reward-overflow drop now surfaces telemetry)

## Current Status

- Phase 00-03 foundation, stack, and architecture contracts are complete.
- Phase 04 agentic pipeline bootstrap is complete.
- Phase 05-10 engine runtime and sandbox vertical slice are complete.
- Phase 11 devtools and editor direction are complete.
- Phase 12 game spec pipeline and sample Signal Garden spec are complete.
- Phase 13 telemetry/retro self-improvement loop is complete.
- Phase 14 hardening, release checklist, API stability, and first-game kickoff are complete.
- Signal Garden `SG-001` (content pack) is complete: original pack + loader unit test + gates pass.
- Signal Garden `SG-002` (reward deposit hook) is complete: `DefenseRuntime.updateTowers` returns
  `TowerUpdateResult(metrics, rewards)` accumulating content-derived kill rewards (no Inventory
  mutation in engine-defense); `SandboxRuntime.step` deposits rewards into `state.inventory` in
  sorted-key order guarded by `canAdd`. Tests + gates pass.

## Next Exact Action

Implement Signal Garden backlog item `SG-003` (placeholder render surface):

```powershell
Get-Content -Raw games\signal-garden\ROADMAP.md
Get-Content -Raw .claude\specs\backlog\SG-003-render-surface.md
```

Expected next output:

- snapshot renders tiles, core, towers, enemies
- camera pan/zoom remains tested
- rendering does not mutate simulation

## Known Blockers

- No blocking issue for `SG-003`.
- Real renderer, Android lifecycle save smoke, and suspicious-value content reports
  are tracked as first-game/hardening backlog.
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
- `scripts\me-sim-replay.ps1` -> pass; final hash `9c495d8ff30fd83d`.
- `scripts\me-save-compat.ps1` -> pass.
- `scripts\me-benchmark.ps1` -> pass; emits JSON balance metrics.
- `scripts\me-record-run.ps1` synthetic event -> pass.
- `scripts\me-retro.ps1` -> pass; wrote `.ai/retro/retro-2026-07-02.md`.
- `.\gradlew.bat desktop:run` -> pass; prints sandbox hash and ASCII snapshot.
- `.\gradlew.bat android:assembleDebug` -> pass.
- `scripts\me-selfcheck.ps1` -> pass; Claude plugins + repo marketplace wired to canon.

Environment used:

- `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`
- `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`

## MyTD Spec Bundle (2026-07-04)

- `/me-spec` clone-intake vs reference `com.vipubstd.games.block.defense` produced an original,
  traceable `--greenfield-game` bundle at `D:/Pet/MyTD/spec` (mechanics cloned, names/numbers own).
- Both gates passed. Engine gaps bridged to backlog: `MTD-001` reward deposit, `MTD-002` gold-cost
  gating, `MTD-003` tower upgrade hook, `MTD-004` difficulty modifiers, `MTD-005` render/input surface.
- Game content (EG-006) stays in the MyTD bundle; not a MyEngine backlog item.

## Notes

- This directory is still not a git repository.
- Claude workflows now ship as installable plugins via `.claude-plugin/marketplace.json`
  (`me-dev` -> `/me`, `me-spec` -> `/me-spec`); adapters stay thin over `docs/agentic`.
- `D:\Pet\mobile-pipeline` remains a process reference, not a copy source.
- v0.1 content uses `.properties` files until a future ADR justifies another parser/schema stack.
