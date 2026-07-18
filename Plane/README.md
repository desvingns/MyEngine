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
2. Следующая работа идёт через MyTD backlog specs.
3. `ENG-026` accepted (scoped JVM/build/replay/save-compat proof); следующий backlog item: `ENG-027`.
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

### 2026-07-04 - Signal Garden SG-003

- Status: Done
- Owner: Claude
- Created/changed:
  - `engine-render/src/main/kotlin/dev/myengine/render/PlaceholderRenderSurface.kt` (new; pure
    `project(snapshot: EngineSnapshot, camera: Camera): RenderFrame`; kinds
    `RenderKind{TILE_FLOOR,TILE_WALL,TILE_RESOURCE,CORE,TOWER,ENEMY}`;
    `RenderPrimitive(kind, tile, screen, health?)`; `RenderFrame(primitives, coreHealth, tick)`;
    tiles-then-entities-by-id ordering; unknown types skipped; ENEMY carries health, TOWER null; no
    game/Android imports; no simulation mutation)
  - `engine-render/src/test/kotlin/dev/myengine/render/RenderBoundaryTest.kt` (extended with camera
    pan/zoom cases: pan delta + clamp-to-world, zoom clamp to `[0.5,4]`, scale grows with zoom)
  - `engine-render/src/test/kotlin/dev/myengine/render/PlaceholderRenderSurfaceTest.kt` (new; six
    kinds present, enemy health carried / tower null, screen == `worldToScreen(tile center)`,
    ordering, unknown-type safety, projection purity)
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxRenderNonMutationTest.kt`
    (new; step runtime, capture `stableHash`, project snapshot, assert `stableHash` unchanged)
- Verification:
  - `.\gradlew.bat test` -> pass (full suite incl. new render + sandbox tests)
  - `.\gradlew.bat desktop:run` -> pass (exit 0; renders ASCII map; existing `AsciiRenderer` path
    unaffected)
  - `scripts\me-record-run.ps1` -> pass (events=4)
- Decisions:
  - Reusable, game-agnostic render surface; pure `snapshot -> frame` projection with no
    game/Android imports and no simulation mutation.
  - Deliberately NOT yet launcher-wired (desktop/Android still use `AsciiRenderer`); consuming
    `RenderFrame` in a real launcher with a pixel/screenshot smoke is a tracked follow-up.
  - No ADR (presentation boundary is Experimental; pure projection adds no new dependency
    direction). Pipeline: developer -> tester -> renderer-qa + verifier (both pass) -> docs
    (architect skipped — scope small and well-bounded, like SG-001).
- Next:
  - Implement `.claude/specs/backlog/SG-004-android-save-smoke.md`.

### 2026-07-04 - Signal Garden SG-003 follow-up (RenderFrame consumed in a real desktop launcher + pixel-smoke)

- Status: Done
- Owner: Claude
- Created/changed:
  - `engine-render/src/main/kotlin/dev/myengine/render/RenderPalette.kt` (new; pure, Android-safe —
    NO java.awt/android imports; `data class Rgb(r,g,b).toRgbInt()` + `object RenderPalette` mapping
    each of the six `RenderKind`s to a distinct color plus `background`/`coreHealthText`/`enemyPip`;
    durable shared kind->color mapping for launcher authors)
  - `desktop/src/main/kotlin/dev/myengine/desktop/FrameRasterizer.kt` (new; AWT-only
    `BufferedImage`/`Graphics2D`/`ImageIO`, antialiasing OFF, TYPE_INT_RGB; rasterizes `RenderFrame`
    -> 20px cell per primitive centered on the projected `ScreenPoint`, 4px enemy pip only when
    `health != null`, `core <n>` from `RenderFrame.coreHealth`; `writePng`)
  - `desktop/src/main/kotlin/dev/myengine/desktop/DesktopLauncher.kt` (edit; banner/`hash=`/ASCII
    output preserved and unreordered — canonical `9c495d8ff30fd83d` unchanged; added a debug
    render-smoke block: project via `PlaceholderRenderSurface`, rasterize, write
    `desktop/build/render-smoke.png`, print `png=<path>`)
  - `desktop/build.gradle.kts` (edit; added `libs.kotlin.test.junit5` + `libs.junit.jupiter` test
    deps and `tasks.test { useJUnitPlatform() }`)
  - `desktop/src/test/kotlin/dev/myengine/desktop/FrameRasterizerPixelSmokeTest.kt` (new;
    deterministic headless AWT pixel-smoke — all six kinds' cell-center pixels == their
    `RenderPalette` color, enemy-pip present / tower-pip absent, core-health text region
    non-background, bit-for-bit determinism)
- Verification:
  - `.\gradlew.bat test` -> pass (full suite incl. new `FrameRasterizerPixelSmokeTest` + existing
    `PlaceholderRenderSurfaceTest`/`RenderBoundaryTest`/`SandboxRenderNonMutationTest`)
  - `.\gradlew.bat desktop:run` -> exit 0; `hash=9c495d8ff30fd83d` (canonical, unchanged), ASCII
    map, `png=D:\Pet\MyEngine\desktop\build\render-smoke.png`
  - `.\gradlew.bat android:assembleDebug` -> BUILD SUCCESSFUL (proves `RenderPalette` did not pull
    java.awt into the Android artifact)
- Decisions:
  - Pure headless AWT `BufferedImage` rasterizer (Option A) over a libGDX/GL window (not
    deterministic/headless-testable). Refined by the verified dependency fact `android` ->
    `:games:sandbox` -> `api(project(":engine-render"))`: engine-render compiles into Android, so
    AWT must NOT live in engine-render. Final split: reusable pure `RenderPalette` (RGB ints) in
    engine-render; AWT `FrameRasterizer` + pixel-smoke in the desktop harness.
  - No ADR (engine-render presentation boundary + `EngineSnapshot`/`Camera` Experimental; no new
    module dependency edge; Constitution rule 2 preserved, guarded by `SandboxRenderNonMutationTest`).
  - Closes SG-003 follow-up notes #1 (launcher wiring + pixel-smoke; `render.md` pixel-smoke gate)
    and #2 (durable kind->color mapping / `coreHealth` / nullable-`ENEMY.health` note). Remaining
    open: `Camera.clamped()` [0..width] vs `screenToTile` [0..width-1] (pre-existing); Android shell
    still on `AsciiRenderer` (separate follow-up).
  - Pipeline: architect -> engine-developer -> tester -> runner -> renderer-qa (pass) + verifier
    (pass, all four boundary_checks true) -> docs; one post-review cosmetic fix (debug PNG path
    `desktop\desktop\build\...` -> `desktop\build\render-smoke.png`; re-ran `desktop:run`, hash
    unchanged).
- Next:
  - Implement `.claude/specs/backlog/SG-004-android-save-smoke.md`.

### 2026-07-04 - Signal Garden SG-004 (Android lifecycle save smoke)

- Status: Done
- Owner: Claude
- Created/changed:
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt` (new; pure,
    Android-free holder wrapping `SandboxRuntime` + seed — `start()`/`step()`/`submit()`/
    `stableHash()`/`save()` = `SandboxSaveCodec.encode(state, seed)` / `restore(text)` decode +
    re-parse seed; QUIESCENT-SAVE precondition documented in KDoc: only `state` persisted, NOT the
    command queue or per-tick `SeededRandom(17)`; no android imports; `SAVE_VERSION` stays 1)
  - `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` (edit; thin adapter —
    `onCreate` restores from `savedInstanceState["me_sandbox_save"]` under a greppable
    `DEBUG_SAVE = true` flag else `start()`, preserves banner + tick + hash TextView + `runCatching`
    fold; `onSaveInstanceState` writes `session.save()` to the Bundle; no sim logic/content ids)
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt` (new;
    save/restore roundtrip preserves stableHash, pause/resume determinism == uninterrupted run to the
    same tick, seed roundtrip, independent-runtime, plus future/non-numeric version decode rejection)
  - `docs/contracts/android.md` (edit; Test Gates note — pause/resume smoke JVM-covered +
    device-pending)
- Verification:
  - `.\gradlew.bat test` -> pass (full suite incl. new `SandboxSessionLifecycleTest`)
  - `scripts\me-save-compat.ps1` -> pass
  - `.\gradlew.bat android:assembleDebug` -> pass (thin Activity + Bundle adapter compiles/links)
  - `scripts\me-record-run.ps1` -> pass (events=7)
- Decisions:
  - Pipeline: architect `me-architect` (Option A — pure holder in `games/sandbox` + thin Activity
    adapter) -> developer `me-engine-developer` -> tester `me-tester` (post-review version-rejection
    guard) -> reviewers `me-save-compat-reviewer` (pass) + `me-android-performance` (pass) +
    `me-verifier` (pass, all four boundary_checks true) -> docs `me-docs`.
  - No ADR, no save-format change (`SAVE_VERSION` stays 1; command-queue/RNG persistence deferred to
    a follow-up). Simulation stays Android-free (`SandboxSession` has zero android imports).
  - Device blocker documented (acceptance #3): no device/emulator, so the on-device Bundle
    round-trip is device-pending; device-independent proof is JVM-covered. Follow-ups: persist
    command queue + RNG (SAVE_VERSION v2 + migration), gate `DEBUG_SAVE` on `BuildConfig.DEBUG`,
    off-main-thread encode for larger state, device pause/resume run.
  - Note: the `me-docs` close-out sub-agent was interrupted by a process exit after writing
    STATE/handoff/android.md; the orchestrator completed the handoff NEXT/BLOCKERS/VERIFICATION, this
    Plane entry, and the agent-skill-log entry.
- Next:
  - Implement `.claude/specs/backlog/SG-005-balance-report.md`.

### 2026-07-05 - Signal Garden SG-004 follow-up (sandbox save-format v2 — persist pending CommandQueue)

- Status: Done
- Owner: Claude
- Created/changed:
  - `engine-core/src/main/kotlin/dev/myengine/core/Command.kt` (added
    `CommandQueue.pending(): List<EngineCommand>`, non-destructive snapshot; `drainFor`/
    `commandComparator` unchanged)
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
    (`SandboxRuntime.pendingCommands()`/`submitAll()`; `SandboxSaveCodec.SAVE_VERSION` 1 -> 2;
    `encode()` gained a `pendingCommands` param serialized as a new properties line; `decode()`
    version guard now accepts `1 || 2` (rejects 3+), signature/return type unchanged; new
    `decodePendingCommands(text)`)
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt` (`save()`/`restore()`
    wired to the new codec params; KDoc drops the quiescent-save precondition)
  - `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` (`DEBUG_SAVE` ->
    `BuildConfig.DEBUG`; stale comment updated)
  - `android/build.gradle.kts` (`buildFeatures { buildConfig = true }`)
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt`
    (pending-command save/restore/resume regression test, v1->v2 migration test, CommandId/Tick
    preservation test, retargeted version-rejection tests)
- Verification:
  - `.\gradlew.bat :games:sandbox:test` -> pass
  - full `.\gradlew.bat test` -> pass
  - `scripts\me-save-compat.ps1` -> pass
  - `.\gradlew.bat :android:compileDebugKotlin` -> pass (after pointing a machine-local, gitignored
    `local.properties` at the Android SDK — not a code change)
- Decisions:
  - No ADR: per me-architect, covered by the existing "saves are versioned from v1 and
    migration-aware" invariant; `SandboxSaveCodec` stays Experimental per `docs/API_STABILITY.md`,
    same precedent as the original v1 codec.
  - Closes SG-004 follow-up items #1 (command-queue persistence / quiescent-save precondition
    dropped) and #2 (`DEBUG_SAVE` gated on `BuildConfig.DEBUG`) from the original SG-004 entry above.
    Follow-ups #3 (off-main-thread encode) and #4 (device/emulator round-trip) remain open.
  - Pipeline: architect `me-architect` (no ADR) -> developer `me-engine-developer` -> tester
    `me-tester` -> runner `me-runner` -> reviewers `me-simulation-reviewer` (pass, 2 non-blocking
    mediums), `me-save-compat-reviewer` (pass, 1 non-blocking low), `me-android-performance` (pass, 1
    non-blocking low + 1 pre-existing medium) -> `me-verifier` (pass, all four boundary_checks true;
    all three SG-004 acceptance criteria confirmed satisfied) -> docs `me-docs`.
- Next:
  - Implement `.claude/specs/backlog/SG-005-balance-report.md`.

### 2026-07-05 - Signal Garden SG-005 (balance report deltas)

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolReports.kt` (`BalanceDeltaReport`,
    baseline/changed summaries, deterministic deltas and warnings)
  - `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolsMain.kt` (`balance-report` /
    `balance-delta` CLI aliases)
  - `engine-devtools/src/test/kotlin/dev/myengine/devtools/DevtoolReportsTest.kt` (baseline/changed,
    no-op, warning, invalid-pack, parser-backed JSON, and CLI stdout coverage)
  - `engine-devtools/build.gradle.kts`
  - `gradle/libs.versions.toml`
- Verification:
  - `.\gradlew.bat :engine-devtools:test` -> pass
  - `.\gradlew.bat test` -> pass
  - `scripts\me-content-validate.ps1` -> pass
  - `scripts\me-sim-replay.ps1` -> pass (`9c495d8ff30fd83d`, `83a65da1a7881b2c`)
  - `scripts\me-save-compat.ps1` -> pass
  - `scripts\me-benchmark.ps1` -> pass
  - `scripts\me-record-run.ps1` -> recorded event 10, `reflect_required=true`
  - `scripts\me-retro.ps1` -> pass; wrote `.ai/retro/retro-2026-07-05.md`
  - `.\gradlew.bat -q :engine-devtools:run --args="balance-report"` -> pass; one JSON object with
    enemy/core/resource warnings
- Decisions:
  - Devtools-only Experimental report; no ADR, no content schema/runtime/save/Android change.
  - Thresholds are suspicious-value report metadata, not final balance policy.
- Next:
  - Close or narrow duplicate `.claude/specs/backlog/MTD-001-reward-deposit.md` by reference to SG-002,
    then implement `.claude/specs/backlog/MTD-002-gold-cost-gating.md`.

### 2026-07-05 - MyTD MTD-001/MTD-002 (reward duplicate close + tower cost gating)

- Status: Done
- Owner: Codex
- Created/changed:
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxTowerCostGatingTest.kt` (new;
    affordable spend, unaffordable rejection unchanged/non-negative, rejected placement without
    spend, replay-stable rejection)
  - `.claude/specs/backlog/MTD-001-reward-deposit.md` (status done; duplicate of SG-002)
  - `.claude/specs/backlog/MTD-002-gold-cost-gating.md` (status done)
  - `.claude/specs/ENGINE_ROADMAP.md`
  - `D:/Pet/MyTD/spec/engine-gap-analysis.md`
  - `D:/Pet/MyTD/spec/traceability.csv`
  - `D:/Pet/MyTD/spec/risks.md`
  - `STATE.md`
  - `.ai/handoff.md`
  - `.ai/DIGEST.md`
- Verification:
  - `.\gradlew.bat :games:sandbox:test` -> pass
  - `.\gradlew.bat test` -> pass
  - `scripts\me-content-validate.ps1` -> pass
  - `scripts\me-sim-replay.ps1` -> pass (`9c495d8ff30fd83d`, `83a65da1a7881b2c`)
  - `scripts\me-save-compat.ps1` -> pass
  - `scripts\me-benchmark.ps1` -> pass
- Decisions:
  - MTD-001 is closed by reference to SG-002. MyTD gold is ordinary content data
    (`rewardResource=gold` / `costResource=gold`), not a hardcoded engine concept.
  - MTD-002 needed acceptance coverage, not a production rewrite: `SandboxRuntime.buildTower`
    already gates content-defined tower cost before placement and spends only after successful
    placement.
  - No ADR needed; no dependency direction changed and no reference IP/content was copied.
- Next:
  - Implement `.claude/specs/backlog/MTD-003-tower-upgrade-hook.md`.

### 2026-07-05 - MyTD MTD-003 (tower upgrade hook)

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt`
  - `engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt`
  - `engine-content/src/test/kotlin/dev/myengine/content/ContentPackLoaderTest.kt`
  - `engine-entities/src/main/kotlin/dev/myengine/entities/EntityModel.kt`
  - `engine-render/src/main/kotlin/dev/myengine/render/RenderModel.kt`
  - `games/sandbox/content/sandbox/towers.properties`
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxTowerUpgradeTest.kt`
  - `docs/content-schemas/PROPERTIES_SCHEMA.md`
  - `.claude/specs/backlog/MTD-003-tower-upgrade-hook.md`
  - `.claude/specs/ENGINE_ROADMAP.md`
  - `D:/Pet/MyTD/spec/engine-gap-analysis.md`
  - `D:/Pet/MyTD/spec/traceability.csv`
  - `D:/Pet/MyTD/spec/risks.md`
  - `STATE.md`
  - `.ai/handoff.md`
  - `.ai/DIGEST.md`
- Verification:
  - `.\gradlew.bat :engine-content:test :games:sandbox:test` -> pass
  - `.\gradlew.bat test` -> pass
  - `scripts\me-content-validate.ps1` -> pass
  - `scripts\me-sim-replay.ps1` -> pass (`9c495d8ff30fd83d`, `83a65da1a7881b2c`)
  - `scripts\me-save-compat.ps1` -> pass
  - `scripts\me-benchmark.ps1` -> pass
- Decisions:
  - No ADR: scoped Experimental content/sandbox/save extension, no new dependency edge, no copied
    reference content.
  - Upgrade branch ids are delimiter-safe (`[A-Za-z0-9_-]+`); legal transitions are unupgraded ->
    tier 1, then same-branch `current+1`.
  - `UpgradeTowerCommand` stays beside existing `BuildTowerCommand` for this slice; future refactor
    may move gameplay command DTOs to a neutral command API.
- Next:
  - Implement `.claude/specs/backlog/MTD-004-difficulty-modifiers.md`.

### 2026-07-16 - MyTD MTD-004 (difficulty modifiers)

- Status: Done
- Owner: Codex
- Created/changed:
  - `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt`
  - `engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt`
  - `engine-content/src/main/kotlin/dev/myengine/content/DifficultyScaling.kt`
  - `engine-content/src/test/kotlin/dev/myengine/content/ContentPackLoaderTest.kt`
  - `games/sandbox/content/sandbox/difficulties.properties`
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxDifficultyTest.kt`
  - MTD-004 roadmap/backlog and MyTD gap/traceability/risk docs
- Implementation:
  - `DifficultyContent` and optional difficulty properties are resolved through
    `ContentRegistry.resolveDifficulty`; `BigDecimal` scaling materializes health, count, reward,
    and gold rate deterministically before the first tick.
  - Easy/normal/hard values come from the MyTD balance plan. Setup selection is wired in
    `SandboxGame`/`SandboxSession`; no save-format, Android, or render changes; no ADR.
- Verification:
  - `.\gradlew.bat :engine-content:test :games:sandbox:test` -> pass; `.\gradlew.bat test` -> pass
  - `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`)
  - `scripts\me-sim-replay.ps1` -> pass (`9c495d8ff30fd83d`, `83a65da1a7881b2c`)
  - `scripts\me-save-compat.ps1` -> pass; `scripts\me-benchmark.ps1` -> pass (`sim_ms=429` implementation run)
  - `me-verifier` -> pass with all boundary checks true
- Follow-up:
  - Low, non-blocking: `difficultyId` is not serialized; restore requires the same effective
    difficulty-resolved registry.
- Next:
  - Implement `.claude/specs/backlog/ENG-024-command-dto-relocation.md`.

### 2026-07-16 - MyEngine ENG-024 (command DTO relocation and InputAdapter state fix)

- Status: Done
- Owner: Codex
- Implementation:
  - Approved variant A moved `BuildTowerCommand`/`UpgradeTowerCommand` to
    `engine-core/src/main/kotlin/dev/myengine/core/command/TowerCommands.kt` with `TileCoordinate`.
  - `InputState` no longer owns `nextCommandId`/`selectedTowerId`; `InputUiState` is explicit,
    `CommandId` is caller-owned, and the sandbox performs boundary conversion.
- Verification:
  - Full tests, replay, save-compat, `android:assembleDebug`, and static scan -> pass.
  - Canonical hashes `9c495d8ff30fd83d` and `83a65da1a7881b2c` unchanged; `me-verifier` pass,
    all `boundary_checks` true.
  - Content validation and benchmark: not run by scope.
- Next:
  - Implement the existing `.claude/specs/backlog/ENG-005-map-content-definitions.md` card.

### 2026-07-16 - DX-008 hybrid format and MyEngine ENG-005 (map definitions)

- Status: Done
- Owner: Codex
- Implementation:
  - Accepted `docs/DECISIONS/ADR-0003-content-format-hybrid.md`: flat definitions remain
    `.properties`; nested map assets use additive structured `maps.json`.
  - The sandbox creates `TileWorld`, spawn, and core from `sandbox-canonical`; the fixture preserves
    the previous 64x64 map, spawn `(1,1)`, core `(32,32)`, and `bolt` node `(5,5)=100`.
  - `SandboxSaveCodec` v4 persists map id/content version, validates map/pack/content identity, and
    migrates v1-v3 saves through the sole available map.
- Verification:
  - Full `./gradlew.bat test` -> pass.
  - `scripts\me-content-validate.ps1` -> pass (`validated 2 pack(s)`); replay -> pass with
    `9c495d8ff30fd83d` and `83a65da1a7881b2c` unchanged; save-compat -> pass; benchmark -> pass.
  - Reviewer/verifier verdicts -> pass.
- Follow-ups:
  - Low, non-blocking: Android shell does not yet load packaged maps through `AssetManager`.
  - Low, non-blocking: `BalanceDeltaReport` omits map-local resource-node/geometry metrics.
- Next:
  - Implement `.claude/specs/backlog/ENG-014-win-lose-run-summary.md`.

### 2026-07-16 - MyEngine ENG-014 (win/lose conditions and run summary)

- Status: Done
- Owner: Codex
- Implementation:
  - Map terminal rules provide finite-wave win, no-win/endless mode, mandatory core-health loss,
    and an optional positive leak budget; terminal runs reject commands and freeze summaries.
  - Immutable RunSummary reaches EngineSnapshot; save v5 persists completed runs.
- Verification:
  - Serial full Gradle suite (--no-daemon --max-workers=1), content validation (2 packs),
    canonical/kill replay hashes (9c495d8ff30fd83d, 83a65da1a7881b2c), save-compat, benchmark,
    and Android assemble -> pass. Serial mode followed earlier parallel native-memory exhaustion,
    not an assertion failure.
- Decisions:
  - No ADR: additive data-driven terminal policy, no new dependency edge.
- Next:
  - MTD-005 real render surface and touch input.

### 2026-07-16 - MyTD MTD-005 (real render surface and touch input)

- Status: Done / device-pending limitations documented
- Owner: Codex
- Implementation:
  - Android `SandboxRenderView` consumes immutable `EngineSnapshot` through
    `PlaceholderRenderSurface` and `RenderPalette`, drawing tiles, path, core, tower tiers, enemies,
    and overlay with `Canvas` in tiles -> path -> entities order.
  - `MotionEvent` maps tap, one-finger drag-pan, and pinch to the existing `InputAdapter`.
    The View owns only presentation state; `MyEngineActivity` owns command ids and the callback that
    submits a resulting command, advances one session tick, and invalidates the View.
- Verification:
  - `:engine-render:test` -> pass; `:games:sandbox:test` -> pass; `:android:assembleDebug` -> pass.
  - `scripts\me-sim-replay.ps1` -> pass, canonical `9c495d8ff30fd83d` and kill
    `83a65da1a7881b2c` unchanged at tick 35; `me-verifier` -> pass with all boundary checks true.
- Limitations:
  - No device/emulator smoke was run. Validate tap-build, drag-without-build, pinch, draw order, and
    debug rotate/process recreation with a pending command before calling device acceptance complete.
  - Profile sustained pan/pinch with FrameMetrics/JankStats and Allocation Tracker; the current
    redraw path creates a snapshot/frame and intermediate primitive lists.
- Next:
  - Implement `.claude/specs/backlog/ENG-026-android-surface-renderer-loop.md`.

### 2026-07-16 - MyEngine ENG-026 (Android SurfaceView renderer and Choreographer loop)

- Status: Done / device-performance limitations documented
- Owner: Codex
- Implementation:
  - Android-local `TickScheduler` uses a 20 Hz Choreographer fixed-tick policy; the `SurfaceView`
    renders only the latest immutable `RenderFrame`.
  - Tap/pan/pinch use the existing `InputAdapter`; presentation code queues commands only. The
    Activity retains command-ID ownership.
  - `onPause` cancels ticking and saves pending commands; Bundle restoration resumes with the
    preserved next command ID and replay continuity.
- Verification:
  - `:android:testDebugUnitTest --tests dev.myengine.android.FixedTickFrameLoopTest --rerun-tasks`
    -> pass; `:android:assembleDebug` -> pass.
  - Replay -> pass, canonical `9c495d8ff30fd83d`, kill `83a65da1a7881b2c`; save-compat -> pass.
  - `me-verifier` -> pass. `me-tester` reported no test-file changes.
- Limitations:
  - Device/emulator tap, pan, pinch, pause/recreate-with-pending-command, command-ID, and hash
    continuity checks remain pending.
  - FrameMetrics/JankStats and Allocation Tracker evidence is required before asserting smoothness
    or a frame budget; Android performance review is partial pending these checks.
- Next:
  - Implement `.claude/specs/backlog/ENG-027-hud-data-ui-commands.md`.
