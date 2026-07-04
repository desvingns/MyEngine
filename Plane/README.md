# MyEngine Implementation Plane

Дата создания: 2026-07-02

Эта папка содержит подробный поэтапный план реализации `MyEngine`: Android-first 2D game engine / framework для нескольких будущих игр в области colony sim, tower defense, factory/logistics и minimalist mobile strategy.

План намеренно разбит на отдельные этапы. Каждый этап можно давать Claude/Codex как самостоятельный промт, не загружая весь проект одним огромным заданием. После выполнения этапа обновляй этот README: статус, дату, ссылки на созданные артефакты, команды проверки и следующий рекомендуемый этап.

## Статусы

- `[ ]` Planned - этап ещё не начат.
- `[~]` In progress - этап начат, но не закрыт acceptance gates.
- `[!]` Blocked - есть конкретный блокер.
- `[x]` Done - этап выполнен, проверки задокументированы.
- `[>]` Deferred - осознанно отложено, есть причина и новый входной критерий.

## Прогресс выполнения

| Status | Phase | Файл | Главный результат | Зависимости | Дата завершения |
|---|---:|---|---|---|---|
| [x] | 00 | [00_reference_research.md](00_reference_research.md) | `docs/REFERENCE_RESEARCH.md`, license guardrails, borrowed/rejected decisions | `PROMPT_PACK.md` | 2026-07-02 |
| [x] | 01 | [01_repository_foundation.md](01_repository_foundation.md) | README, AGENTS, constitution, roadmap, `.ai` workspace | Phase 00 | 2026-07-02 |
| [x] | 02 | [02_stack_adr_and_project_scaffold.md](02_stack_adr_and_project_scaffold.md) | ADR-0001, Gradle/libGDX/Kotlin scaffold, build commands | Phase 01 | 2026-07-02 |
| [x] | 03 | [03_engine_architecture_contracts.md](03_engine_architecture_contracts.md) | architecture docs, API sketch, module contracts, dependency graph | Phase 02 | 2026-07-02 |
| [x] | 04 | [04_agentic_pipeline_bootstrap.md](04_agentic_pipeline_bootstrap.md) | `/me`, `/me-spec` design, agent contracts, initial skills/stubs | Phase 01-03 | 2026-07-02 |
| [x] | 05 | [05_core_simulation_runtime.md](05_core_simulation_runtime.md) | deterministic tick loop, command queue, RNG, replay hash | Phase 02-03 | 2026-07-02 |
| [x] | 06 | [06_world_content_and_save.md](06_world_content_and_save.md) | tile world, content schemas, validation, save/load v1 | Phase 05 | 2026-07-02 |
| [x] | 07 | [07_entities_ai_jobs.md](07_entities_ai_jobs.md) | entity model, system ordering primitives, jobs/tasks, path requests | Phase 05-06 | 2026-07-02 |
| [x] | 08 | [08_logistics_defense_storyteller.md](08_logistics_defense_storyteller.md) | resources, recipes, waves, towers, enemies, incident director | Phase 06-07 | 2026-07-02 |
| [x] | 09 | [09_render_android_input.md](09_render_android_input.md) | snapshot boundary, camera, Android shell, input, debug overlay | Phase 05-08 | 2026-07-02 |
| [x] | 10 | [10_vertical_slice_sandbox.md](10_vertical_slice_sandbox.md) | playable sandbox: map, tower placement, waves, save, replay | Phase 05-09 | 2026-07-02 |
| [x] | 11 | [11_devtools_balance_editor.md](11_devtools_balance_editor.md) | sim runner, scenario tests, balance reports, editor plan | Phase 10 | 2026-07-02 |
| [x] | 12 | [12_game_spec_pipeline.md](12_game_spec_pipeline.md) | reusable game-spec bundle workflow and first sample game spec | Phase 04, 10 | 2026-07-02 |
| [x] | 13 | [13_self_improvement_loop.md](13_self_improvement_loop.md) | telemetry, retro, reflection, gated improvements | Phase 04, 10 | 2026-07-02 |
| [x] | 14 | [14_hardening_release_first_game.md](14_hardening_release_first_game.md) | hardening backlog, release discipline, first game kickoff | Phase 10-13 | 2026-07-02 |

## Рекомендуемый порядок

1. Оригинальный план Phase 00-14 закрыт.
2. Следующая работа идёт через Signal Garden backlog specs.
3. Начинать с `.claude/specs/backlog/SG-001-content-pack.md`.
4. Hardening gaps из `docs/HARDENING_AUDIT.md` закрывать по одному, с тестами и обновлением handoff.

Новые крупные фазы добавлять только после того, как backlog specs перестанут быть достаточно
точным механизмом управления работой.

## Глобальные инварианты

- Android - единственная shipping platform. Desktop/JVM нужен как dev harness, тестовый runner и потенциальный редактор.
- Simulation должна быть testable without Android.
- Rendering/input не владеют authoritative game state.
- Все важные решения фиксируются в ADR или docs, а не остаются в чате.
- Контент data-driven: schemas, version fields, validation, migrations from day one.
- Save files versioned from v1.
- Every non-trivial engine behavior has a deterministic test, replay test or scenario test.
- Референсы используются как источник решений, не как источник копипаста.
- GPL/MPL код не копируется без отдельного license ADR.
- Self-improvement changes проходят human gate.

## Definition Of Done Для Любого Этапа

Этап нельзя считать завершённым, пока:

- созданные/изменённые файлы перечислены в финальном отчёте;
- обновлён этот README: status, дата, ссылки на артефакты;
- `STATE.md` или `.ai/handoff.md` обновлены, если они уже существуют;
- проверки запущены или явно указано, почему не могли быть запущены;
- открытые вопросы записаны в `.ai/tasks/` или соответствующий phase-файл;
- следующий этап понятен без перечитывания всего чата.

## Формат Обновления Прогресса

После выполнения этапа добавляй в конец README:

```markdown
## Progress Log

### YYYY-MM-DD - Phase NN

- Status: Done / In progress / Blocked
- Owner: Claude / Codex / Human
- Created/changed:
  - `path`
- Verification:
  - `command` -> pass/fail/not run
- Decisions:
  - ADR or short note
- Next:
  - Phase NN+1 or blocker
```

## Progress Log

### 2026-07-02 - Phase 00

- Status: Done
- Owner: Codex
- Created/changed:
  - `docs/REFERENCE_RESEARCH.md`
  - `docs/DECISIONS/ADR-0000-license-policy.md`
- Verification:
  - `rg -n "Anuken/Mindustry|yairm210/Unciv|rossturner/king-under-the-mountain|rossturner/mountaincore|mafik/libcolony" docs/REFERENCE_RESEARCH.md` -> pass
  - `rg -n "GPL|MPL|unknown|ADR" docs/DECISIONS/ADR-0000-license-policy.md` -> pass
- Decisions:
  - ADR-0000: references are design inputs; direct reuse requires license review and ADR.
- Next:
  - Phase 01 repository foundation.

### 2026-07-02 - Phase 01

- Status: Done
- Owner: Codex
- Created/changed:
  - `README.md`
  - `AGENTS.md`
  - `STATE.md`
  - `DOCUMENTATION.md`
  - `docs/ROADMAP.md`
  - `docs/ENGINE_CONSTITUTION.md`
  - `.ai/handoff.md`
  - `.ai/memory/MEMORY.md`
  - `.ai/memory/reference-policy.md`
  - `.ai/tasks/README.md`
  - `.ai/changes/agent-skill-log.md`
  - `.ai/proposals/README.md`
  - `.ai/runs/README.md`
  - `.ai/retro/README.md`
  - `Plane/README.md`
- Verification:
  - `Test-Path README.md, AGENTS.md, STATE.md, DOCUMENTATION.md, docs/ROADMAP.md, docs/ENGINE_CONSTITUTION.md, .ai/handoff.md, .ai/memory/MEMORY.md, .ai/memory/reference-policy.md, .ai/tasks/README.md, .ai/changes/agent-skill-log.md, .ai/proposals/README.md, .ai/runs/README.md, .ai/retro/README.md` -> pass
  - `rg -n "Phase 02|Get-Content -Raw Plane\\02_stack_adr_and_project_scaffold.md" STATE.md .ai/handoff.md AGENTS.md` -> pass
- Decisions:
  - No production engine code in Phase 01.
  - `.ai` workspace is the cross-session coordination layer.
- Next:
  - Phase 02 stack ADR and project scaffold.

### 2026-07-02 - Phase 02

- Status: Done
- Owner: Codex
- Created/changed:
  - `docs/DECISIONS/ADR-0001-stack.md`
  - `docs/DECISIONS/ADR-0002-dependency-policy.md`
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `gradle.properties`
  - `gradle/libs.versions.toml`
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `engine-core/`
  - `engine-world/`
  - `engine-content/`
  - `engine-testkit/`
  - `games/sandbox/`
  - `desktop/`
  - `android/`
- Verification:
  - `.\gradlew.bat projects` -> pass
  - `.\gradlew.bat test` -> pass
  - `.\gradlew.bat desktop:run` -> pass
  - `.\gradlew.bat android:assembleDebug` -> pass
- Decisions:
  - ADR-0001: Kotlin-first JVM/Android with Gradle Kotlin DSL and libGDX approved.
  - ADR-0002: every new dependency needs license, Android footprint, determinism, and boundary review.
- Next:
  - Phase 03 engine architecture contracts.

### 2026-07-02 - Phase 03

- Status: Done
- Owner: Codex
- Created/changed:
  - `docs/ARCHITECTURE.md`
  - `docs/API_SKETCH.md`
  - `docs/CONTENT_MODEL.md`
  - `docs/TESTING_STRATEGY.md`
  - `docs/contracts/core.md`
  - `docs/contracts/world.md`
  - `docs/contracts/content.md`
  - `docs/contracts/entities.md`
  - `docs/contracts/ai.md`
  - `docs/contracts/logistics.md`
  - `docs/contracts/defense.md`
  - `docs/contracts/storyteller.md`
  - `docs/contracts/render.md`
  - `docs/contracts/android.md`
  - `docs/contracts/devtools.md`
- Verification:
  - `rg -n "engine-core|engine-world|android|replay hash" docs/ARCHITECTURE.md docs/API_SKETCH.md docs/TESTING_STRATEGY.md` -> pass by inspection
  - `.\gradlew.bat test desktop:run android:assembleDebug` -> pass
- Decisions:
  - Contracts keep Android/render dependencies out of authoritative simulation modules.
  - Content starts at schema version 1 and save/replay gates include replay hash checks.
- Next:
  - Phase 04 agentic pipeline bootstrap.

### 2026-07-02 - Phase 04

- Status: Done
- Owner: Codex
- Created/changed:
  - `docs/agentic/PIPELINE.md`
  - `docs/agentic/AGENT_CONTRACTS.md`
  - `docs/agentic/SELF_IMPROVEMENT.md`
  - `docs/agentic/SPEC_BOARD.md`
  - `claude-plugins/me-dev/`
  - `claude-plugins/me-spec/`
  - `codex-plugins/me-dev/`
  - `codex-plugins/me-spec/`
  - `.claude/`
  - `.codex/`
  - `scripts/me-*.ps1`
- Verification:
  - `python C:\Users\Admin\.codex\skills\.system\plugin-creator\scripts\validate_plugin.py codex-plugins\me-dev` -> pass
  - `python C:\Users\Admin\.codex\skills\.system\plugin-creator\scripts\validate_plugin.py codex-plugins\me-spec` -> pass
- Decisions:
  - Canonical pipeline source is `docs/agentic/*`; adapters stay thin.
- Next:
  - Phase 05 core simulation runtime.

### 2026-07-02 - Phase 05

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-core/src/main/kotlin/dev/myengine/core/`
  - `engine-core/src/test/kotlin/dev/myengine/core/CoreRuntimeTest.kt`
- Verification:
  - `.\gradlew.bat test` -> pass
- Decisions:
  - Core replay hash uses deterministic `StableHash`; randomness goes through `SeededRandom`.
- Next:
  - Phase 06 world, content and save v1.

### 2026-07-02 - Phase 06

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-world/src/main/kotlin/dev/myengine/world/`
  - `engine-content/src/main/kotlin/dev/myengine/content/`
  - `docs/content-schemas/PROPERTIES_SCHEMA.md`
  - `games/sandbox/content/sandbox/`
  - sandbox save/load codec
- Verification:
  - `.\gradlew.bat test` -> pass
  - `scripts\me-content-validate.ps1` -> pass
  - `scripts\me-save-compat.ps1` -> pass
- Decisions:
  - v0.1 content packs use external `.properties` files and structured `Properties` parsing.
- Next:
  - Phase 07 entities, systems, AI jobs.

### 2026-07-02 - Phase 07

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-entities/`
  - `engine-ai/`
- Verification:
  - `.\gradlew.bat test` -> pass
- Decisions:
  - Minimal entity/component records and job/path primitives were added without full colony scope.
- Next:
  - Phase 08 logistics, defense and storyteller.

### 2026-07-02 - Phase 08

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-logistics/`
  - `engine-defense/`
  - `engine-storyteller/`
- Verification:
  - `.\gradlew.bat test` -> pass
  - `scripts\me-sim-replay.ps1` -> pass
- Decisions:
  - Defense systems are driven by content definitions; sample names stay in sample content.
- Next:
  - Phase 09 rendering, Android shell and input.

### 2026-07-02 - Phase 09

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-render/`
  - `desktop/src/main/kotlin/dev/myengine/desktop/DesktopLauncher.kt`
  - `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt`
- Verification:
  - `.\gradlew.bat test` -> pass
  - `.\gradlew.bat desktop:run` -> pass
  - `.\gradlew.bat android:assembleDebug` -> pass
- Decisions:
  - Rendering/input use snapshots and commands; no simulation mutation from presentation.
- Next:
  - Phase 10 first playable vertical slice.

### 2026-07-02 - Phase 10

- Status: Done
- Owner: Codex
- Created/changed:
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxVerticalSliceTest.kt`
  - `games/sandbox/README.md`
- Verification:
  - `.\gradlew.bat test` -> pass
  - `.\gradlew.bat desktop:run` -> pass; final hash `9c495d8ff30fd83d`
- Decisions:
  - The first slice is a tiny toy with ASCII/text presentation and deterministic tests.
- Next:
  - Phase 11 devtools, balance runner and editor direction.

### 2026-07-02 - Phase 11

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-devtools/`
  - `docs/EDITOR_PLAN.md`
- Verification:
  - `scripts\me-content-validate.ps1` -> pass
  - `scripts\me-benchmark.ps1` -> pass
  - `scripts\me-sim-replay.ps1` -> pass
- Decisions:
  - Editor scope is constrained to future desktop/dev tools until save/replay/content gates harden.
- Next:
  - Phase 12 game spec pipeline.

### 2026-07-02 - Phase 12

- Status: Done
- Owner: Codex
- Created/changed:
  - `docs/GAME_SPEC_PIPELINE.md`
  - `games/signal-garden/spec/`
- Verification:
  - Traceability exists in `games/signal-garden/spec/traceability.csv` -> pass by inspection
- Decisions:
  - Signal Garden is the first original sample game spec.
- Next:
  - Phase 13 self-improvement loop.

### 2026-07-02 - Phase 13

- Status: Done
- Owner: Codex
- Created/changed:
  - `scripts/me-record-run.ps1`
  - `scripts/me-retro.ps1`
  - `.ai/retro/retro-2026-07-02.md`
- Verification:
  - `scripts\me-record-run.ps1 ...` -> pass
  - `scripts\me-retro.ps1` -> pass
- Decisions:
  - Reflection can propose improvements but cannot edit files without a human gate.
- Next:
  - Phase 14 hardening, release discipline and first game kickoff.

### 2026-07-02 - Phase 14

- Status: Done
- Owner: Codex
- Created/changed:
  - `docs/API_STABILITY.md`
  - `docs/HARDENING_AUDIT.md`
  - `docs/RELEASE_CHECKLIST.md`
  - `games/signal-garden/ROADMAP.md`
  - `.claude/specs/backlog/SG-*.md`
  - `STATE.md`
  - `.ai/handoff.md`
  - `Plane/README.md`
- Verification:
  - `.\gradlew.bat projects` -> pass
  - `.\gradlew.bat test` -> pass
  - `.\gradlew.bat desktop:run` -> pass
  - `.\gradlew.bat android:assembleDebug` -> pass
- Decisions:
  - The original phase plan is complete; next work is Signal Garden `SG-001`.
- Next:
  - Implement `.claude/specs/backlog/SG-001-content-pack.md`.

### 2026-07-04 - Signal Garden SG-001

- Status: Done
- Owner: Claude
- Created/changed:
  - `games/signal-garden/content/signal-garden/` (9 `.properties` files: manifest, tiles,
    resources, recipes, towers, enemies, waves, incidents, strings; `schemaVersion=1`,
    `id=signal-garden`)
  - `engine-content/src/test/kotlin/dev/myengine/content/SignalGardenContentPackTest.kt`
- Verification:
  - `.\gradlew.bat test` -> pass (full suite incl. new `SignalGardenContentPackTest`)
  - `engine-devtools:run content-report <abs path to games/signal-garden/content/signal-garden>`
    -> `{"pack_id":"signal-garden","valid":true,"errors":[]}`
- Decisions:
  - Content is original (own ids/names/numbers), no clone-IP, no ADR needed.
  - Loader test resolves the pack as a filesystem `Path` so `engine-content` stays
    game-module-free.
  - Follow-up (low): default `scripts\me-content-validate.ps1` validates only the sandbox pack;
    extend it to iterate every `games/*/content/*` root so future packs are gated by default.
- Next:
  - Implement `.claude/specs/backlog/SG-002-reward-deposit.md`.

### 2026-07-04 - Signal Garden SG-002

- Status: Done
- Owner: Claude
- Created/changed:
  - `engine-defense/src/main/kotlin/dev/myengine/defense/DefenseRuntime.kt` (added
    `TowerUpdateResult(metrics, rewards)`; `updateTowers` returns it and accumulates
    content-derived kill rewards; removed dead reward stub; no Inventory mutation)
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
    (`SandboxRuntime.step` deposits rewards into `state.inventory` in sorted-key order guarded
    by `canAdd`; no hardcoded resource ids)
  - `engine-defense/src/test/kotlin/dev/myengine/defense/DefenseRuntimeTest.kt` (new return
    type + content-derived conservation + leaked-enemy-yields-no-reward cases)
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxRewardDepositTest.kt`
    (end-to-end deposit test)
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxVerticalSliceTest.kt`
    (kill-scenario replay-determinism + reward save-roundtrip cases)
- Verification:
  - `.\gradlew.bat test` -> pass (full suite incl. all new SG-002 tests)
  - `scripts\me-sim-replay.ps1` -> pass; final hash `9c495d8ff30fd83d` UNCHANGED
  - `scripts\me-save-compat.ps1` -> pass
  - `scripts\me-benchmark.ps1` -> pass (canonical scenario enemies_killed=0, tower_shots=0)
  - `scripts\me-record-run.ps1` -> pass (events=3)
- Decisions:
  - Option A: engine-defense returns rewards (`TowerUpdateResult`); the game runtime owns
    Inventory mutation. No ADR because `DefenseRuntime` is Experimental per
    `docs/API_STABILITY.md`.
  - Replay hash unchanged because the canonical scripted scenario kills 0 enemies in its
    35-tick window, so the reward-deposit path is not exercised by that scenario (expected;
    hash references remain correct).
- Next:
  - Implement `.claude/specs/backlog/SG-003-render-surface.md`.

### 2026-07-04 - SG-002 kill/reward gate hardening

- Status: Done
- Owner: Claude
- Created/changed:
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt` (private
    `runScriptedScenario(towerPosition, seed)` helper + public `runScriptedKillScenario()` at (2,2);
    pure `depositRewards(...)` helper; `SandboxRuntime.step` surfaces
    `lastCommandOrError="reward_dropped:..."` on capacity overflow)
  - `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolReports.kt` (scenario name on
    `HeadlessScenarioReport`; `runSandboxKillScenario()`; `runScenarioSuite()`; `replayInspect`
    reports both scenarios)
  - `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolsMain.kt` (`scenario`/`balance`
    -> scenario suite)
  - `engine-devtools/src/test/kotlin/dev/myengine/devtools/DevtoolReportsTest.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxVerticalSliceTest.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxRewardOverflowTest.kt` (new)
- Verification:
  - `.\gradlew.bat test` -> pass (full suite incl. new devtools/sandbox cases)
  - `scripts\me-sim-replay.ps1` -> pass; `{"scenarios":[canonical 9c495d8ff30fd83d kills=0,
    kill 83a65da1a7881b2c kills=2]}`
  - `scripts\me-benchmark.ps1` -> pass; kill scenario enemies_killed=2 / tower_shots=4, canonical 0/0
  - `scripts\me-save-compat.ps1` -> pass
  - `.\gradlew.bat desktop:run` -> canonical hash `9c495d8ff30fd83d` unchanged
- Decisions:
  - Option A (additive second scenario), so the baseline hash `9c495d8ff30fd83d` is preserved
    rather than rewritten.
  - Silent reward-drop resolved to non-fatal-but-observable telemetry; `lastCommandOrError` is not
    part of `appendHash`, so it is replay-safe. Overflow branch covered now via a capacity-bound
    unit/integration test without shipping capacities in default content.
  - Closes both SG-002 reviewer follow-ups (kill-bearing default gate; silent-drop decision).
- Next:
  - Implement `.claude/specs/backlog/SG-003-render-surface.md`.
