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
| [x] | 15 | [15_domain_systems_sequencing.md](15_domain_systems_sequencing.md) | PROC-003 domain sequencing: flow-field done, colony slice + storyteller ordered with vision-only caveats; successor ENG-010 | Phase 07-08, backlog board | 2026-07-29 |

## Рекомендуемый порядок

1. Оригинальный план Phase 00-14 закрыт.
2. Первый playable Android TD milestone закрыт через `ENG-027`; ручные device/layout/performance
   проверки остаются явно отложенными.
3. `ENG-015` (presentation-side game speed control), `ENG-030` (wave preview + early wave call),
   `ENG-028` (sprite/atlas references in content schema), `ENG-009` (splash damage + shot events),
   and `ENG-020` (spatial index + 1k-entity benchmark) are closed; `ENG-020` was accepted on
   2026-07-29. PROC-003 sequencing adopted 2026-07-29 (see Plane/15): ENG-010 -> ENG-016 ->
   PROC-007 -> ENG-021 -> ENG-029 -> ENG-012 -> ENG-007 -> ENG-018 (done 2026-08-02)
   -> ENG-011 (done 2026-08-02) -> ENG-019 (done 2026-08-02) -> ENG-001 (done 2026-08-02)
   -> ENG-003 (done 2026-08-02) -> ENG-031 (done 2026-08-02) -> ENG-004 (done 2026-08-02)
   -> ENG-032 (done 2026-08-02); ENG-010, ENG-016, PROC-007, ENG-021, ENG-029, ENG-012, ENG-007,
   ENG-018, ENG-011, ENG-019, ENG-001, ENG-003, ENG-031, ENG-004, and ENG-032 are closed.
4. Hardening gaps из `docs/HARDENING_AUDIT.md` закрывать по одному, с тестами и обновлением handoff.

Новые крупные фазы добавлять только после того, как backlog specs перестанут быть достаточно
точным механизмом управления работой.

### Post-Phase-14 feature status

| Status | Feature | Spec | Result | Date |
|---|---|---|---|---|
| [x] | ENG-020 Spatial index + 1k-entity benchmark | [ENG-020](../.claude/specs/done/ENG-020-spatial-index-benchmark.md) | Internal non-persisted grid index for targeting/splash queries plus deterministic machine-readable 1024-enemy benchmark | 2026-07-29 |
| [x] | PROC-013 Spec board hygiene | [PROC-013](../.claude/specs/done/PROC-013-spec-board-hygiene.md) | Variant B migrated 23 cards and wired the board checker into selfcheck | 2026-07-29 |
| [x] | PROC-003 Domain roadmap sequencing | [PROC-003](../.claude/specs/done/PROC-003-domain-roadmap.md) | Plane/15 sequencing adopted; ENG-010 named successor to ENG-020 | 2026-07-29 |
| [x] | ENG-010 Status effects framework | [ENG-010](../.claude/specs/done/ENG-010-status-effects.md) | Content-defined slow/DoT, deterministic lifecycle, movement/damage modifiers, save v9 migration, immutable snapshot tags, and stable replay coverage | 2026-08-01 |
| [x] | ENG-016 Incident execution pipeline + RNG fix | [ENG-016](../.claude/specs/done/ENG-016-incident-execution.md) | Stateful deterministic director, persistent RNG cursor, cadence/pacing/cooldown selection, atomic typed effects, save v10 with v1-v9 migration, and remediation gates | 2026-08-02 |
| [x] | PROC-007 Save migration matrix | [PROC-007](../.claude/specs/done/PROC-007-save-migration-matrix.md) | Checked-in v1-v10 save fixtures, independent stable-hash migration matrix, and save-compat JSON reporting | 2026-08-02 |
| [x] | ENG-021 Save slots + autosave policy | [ENG-021](../.claude/specs/done/ENG-021-save-slots-autosave.md) | Named `slots/`, config-driven rotating `autosave/`, flushed temp + `ATOMIC_MOVE` only, metadata-only inspection, corruption-only fallback, codec v10 and Android Bundle path preserved | 2026-08-02 |
| [x] | ENG-029 Audio event hooks | [ENG-029](../.claude/specs/done/ENG-029-audio-event-hooks.md) | Transient deterministic `GameplayEvent` snapshot feed, optional `sounds.properties` validation, Android `SoundPool` consumer, no save-version/hash change | 2026-08-02 |
| [x] | ENG-012 Boss/elite enemies + wave modifiers | [ENG-012](../.claude/specs/done/ENG-012-boss-elites-wave-modifiers.md) | Data-defined ranks and stat scaling, indexed wave modifiers, deterministic effective spawn state, boss snapshot marker, save v11 migration, replay/save/balance coverage | 2026-08-02 |
| [x] | ENG-007 Multiple spawn points + per-wave routing | [ENG-007](../.claude/specs/done/ENG-007-multi-spawn-wave-routing.md) | Optional validated wave spawn selection, deterministic scheduled/early/incident routing, reserved spawn-id guards, checked-in multi-spawn fixture, replay/save coverage; `SAVE_VERSION` remains 11 | 2026-08-02 |
| [x] | ENG-018 Endless wave generation | [ENG-018](../.claude/specs/done/ENG-018-endless-wave-generation.md) | Validated endless schedule, shared-RNG deterministic generation, count/health/reward growth, no-win validation, and scaling report; `SAVE_VERSION` remains 11 | 2026-08-02 |
| [x] | ENG-011 Enemy armor + damage types | [ENG-011](../.claude/specs/done/ENG-011-armor-damage-types.md) | Option A typed damage content, 0..100 resistances, bidirectional validation, Long/final-floor direct+splash formula, effective-DPS matrix, resist replay hash, and `SAVE_VERSION=11` preserved | 2026-08-02 |
| [x] | ENG-019 Walls + player-placed blockers | [ENG-019](../.claude/specs/done/ENG-019-walls-blocking-buildings.md) | Validated 1x1 wall content, atomic place/remove commands with path rejection and refund, immutable snapshot health, save v12 with v1-v11 migration, forced-corridor replay, and full gates | 2026-08-02 |
| [x] | ENG-001 A* point-to-point pathfinding for agents | [ENG-001](../.claude/specs/done/ENG-001-astar-agent-pathfinding.md) | Deterministic 4-neighbor integer-cost A*, stable tie ordering, API-preserving GridPathfinder delegation, and deterministic AgentPathPlanner repaths; JobBoard/job-actor tick wiring is delivered by ENG-003, with hauling MVP remaining in ENG-004 | 2026-08-02 |
| [x] | ENG-003 Job execution system | [ENG-003](../.claude/specs/done/ENG-003-job-execution-loop.md) | Post-Phase-14/Phase-15 feature close-out: deterministic v13 JobBoard tick execution, lifecycle, pathfinding movement, work ticks, typed resource-delta effects, release semantics, save migration, and replay/full-gate verification | 2026-08-02 |
| [x] | ENG-031 Stockpile zones + designations | [ENG-031](../.claude/specs/done/ENG-031-stockpiles-designations.md) | Accepted Option A: deterministic zone commands/store, validated resource filters, one-shot harvest-node JobBoard jobs, immutable snapshot zone projection, and save v14 with v1-v13 migration; hauling was completed by ENG-004 | 2026-08-02 |
| [x] | ENG-004 First worker agent MVP (hauling) | [ENG-004](../.claude/specs/done/ENG-004-hauling-worker-mvp.md) | Data-defined worker speed/capacity, deterministic source reservations, source-to-stockpile carry/deposit, positioned producer outputs, stockpile contents, and save v15 with v1-v14 migration | 2026-08-02 |
| [x] | ENG-032 Construction system | [ENG-032](../.claude/specs/done/ENG-032-construction-blueprints.md) | Non-blocking blueprints, deterministic sourceId-ordered construction hauling/retry, build jobs, source refunds on cancel, save v16 with v1-v15 migration | 2026-08-02 |

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

### 2026-07-18 - MyEngine ENG-027 (HUD snapshot data and UI command surface)

- Status: Done / manual device-layout-performance limitations documented
- Owner: Codex
- Implementation:
  - `EngineSnapshot` and `RenderFrame` expose an immutable, content-derived HUD block: localized
    labels, resources, wave/countdown/core state, buildable towers with costs/tiers, and selected
    tower damage/kills/upgrades.
  - Android draws build/select/upgrade panels from the snapshot only. A pure density-aware layout
    model provides shared 48 dp row bounds for drawing and hit testing; build and upgrade actions
    flow through `InputAdapter` and the existing caller-owned command queue.
  - Defense accumulates deterministic per-tower actual damage and kills. Save v6 persists those
    metrics; v1-v5 migrate to an empty per-tower metrics map.
  - Tower and tier `displayKey` fields plus the nine `hud.*` localization keys are required and
    covered by content validation.
- Verification:
  - Full Gradle tests -> pass; content validation -> pass (`2` packs); save-compat -> pass;
    `android:assembleDebug` -> pass.
  - Replay -> pass with canonical `9c495d8ff30fd83d` and kill `83a65da1a7881b2c` unchanged.
  - Benchmark -> pass (`canonical=295 ms`, `kill=45 ms`); final verifier accepted every criterion.
- Limitations:
  - Device/emulator build, tower selection, upgrade, and lifecycle smoke remain manual-pending.
  - Check non-default fontScale and long localized labels; profile FrameMetrics/JankStats and
    allocations before asserting UI smoothness or a frame/allocation budget.
  - Harden malformed save v6 handling for a missing `towerMetrics` field and duplicate tower ids.
- Next:
  - Implement `.claude/specs/backlog/ENG-002-flow-field-repath.md`.

### 2026-07-18 - MyEngine ENG-002 (goal-field pathfinding + repath on world change)

- Status: Done
- Owner: Codex
- Implementation:
  - `GoalField` deterministically routes enemies from the core field instead of retaining
    per-enemy precomputed paths; tests pin neighbor/tie order.
  - Placement checks every spawn against the prospective world, rejects a blocked path
    deterministically, reports `occupied_by_enemy`, and rebuilds the field in the same tick after
    each accepted walkability change so live enemies reroute.
  - Save v6 keeps canonical authoritative state: the field is derived after restore and legacy path
    state is canonicalized rather than persisted as cache.
- Verification:
  - Full Gradle tests, replay, save-compat, and benchmark -> pass.
  - Replay hashes: canonical `463d87684ca6cbee`, kill `40c7bda7e3bc1316`, maze golden
    `ed0354584405ec49`; final 64x64 field rebuild metric `4.1904 ms`.
  - Final verifier -> pass; all four acceptance criteria accepted with no findings.
- Notes:
  - The only warning is the pre-existing Gradle 10 deprecation warning from AGP internals.
- Next:
  - Implement `.claude/specs/backlog/ENG-013-tower-sell-refund.md`.

### 2026-07-18 - MyEngine ENG-013 (tower sell/refund)

- Status: Done
- Owner: Codex
- Implementation:
  - `SellTowerCommand` sells a valid tower deterministically. Its refund is content-defined by the
    required inclusive-`0..1` decimal `sellRefundRatio`: base and actually applied sequential tier
    costs are aggregated per resource, then each refund is rounded down independently.
  - Capacity is verified before any mutation. A successful sale frees occupancy, removes the entity
    and its defense metrics, then rebuilds the ENG-002 goal field before enemy movement in the same
    tick. `SandboxTowerSellTest` covers repeated mid-run sell hashing and pending sells retain id,
    tick, actor, and payload through save/restore.
  - `SandboxSaveCodec.SAVE_VERSION` remains `6`; no ADR is required for this additive
    content/sandbox capability.
- Verification:
  - Full Gradle tests, content validation (`2` packs), replay, save-compat, and benchmark -> pass.
  - Replay hashes: canonical `463d87684ca6cbee`, kill `40c7bda7e3bc1316`; benchmark output:
    canonical `458 ms`, kill `88 ms`, 64x64 goal-field rebuild `6.618100 ms`.
  - Required domain reviewers and final verifier -> pass. Initial test/content gate failures from a
    missing test-fixture field and the missing Signal Garden `sellRefundRatio` were repaired before
    the final successful rerun; telemetry reports `retro_due=false`.
- Next:
  - Implement `.claude/specs/backlog/ENG-008-targeting-priorities.md`.

### 2026-07-18 - MyEngine ENG-008 (targeting priority modes)

- Status: Done
- Owner: Codex
- Implementation:
  - Pure `TargetSelector` chooses `first`, `last`, `nearest`, `strongest`, or `weakest` among
    in-range enemies, with entity id as the deterministic final tiebreak.
  - Tower content declares the default; legacy v1 content without `targetingMode` uses `NEAREST`.
    The queued `SetTowerTargetingModeCommand` applies a per-tower override, and the immutable HUD
    exposes the selected mode.
  - Save v7 persists both the tower mode and a pending mode-switch command; v1-v6 saves migrate by
    resolving the content default.
- Verification:
  - Full Gradle tests, content validation (2 packs), replay, save-compat, and benchmark -> pass.
  - Replay hashes: canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`; benchmark output:
    canonical `432 ms`, kill `79 ms`, 64x64 goal-field rebuild `5.3678 ms`.
  - Required domain reviewers and final verifier -> pass.
- Next:
  - Implement `.claude/specs/backlog/ENG-015-game-speed-control.md`.

### 2026-07-18 — MySD repository/spec foundation

- Status: Backlog bridge prepared; gameplay implementation blocked on MySD Gate 1/Gate 2
- Owner: Codex
- Decisions:
  - Accepted `docs/DECISIONS/ADR-0004-composite-build-pinned-engine-revision.md`; PROC-002 is done.
  - Filed ENG-036 for reusable Android-free runtime/session extraction.
  - Filed PROC-015 for hierarchical reference evidence import, clone-strict coverage, traceability,
    and engine-gap dedup.
  - Did not add `mysd` demand to probable gameplay gaps before observed Gate 1 evidence.
- Verification:
  - `scripts/me-selfcheck.ps1` -> pass.
  - Documentation/backlog diff only; no simulation/save/replay/content behavior changed.
- Next:
  - At the time of this historical entry, keep ENG-030 after ENG-015; ENG-030 is now closed.
  - After MySD Gate 2, schedule ENG-036 before the MySD headless vertical slice.

### 2026-07-21 - MyEngine ENG-015 (presentation-side game speed control)

- Status: Done / accepted; device and performance follow-ups remain manual-pending
- Owner: Codex
- Implementation:
  - Android-local `PresentationSpeed` adds `0x`, `1x`, `2x`, and `4x` modes. `FixedTickFrameLoop`
    scales due ticks at the presentation boundary, preserving fixed-tick simulation semantics,
    250 ms frame cap, and no background catch-up after stop/start.
  - `MyEngineActivity` stores speed separately in `Bundle`; `SandboxRenderView` adds four disjoint,
    callback-only speed controls. Speed selection does not create an `EngineCommand` and does not
    enter session save or `SAVE_VERSION`.
- Verification:
  - Selfcheck, full tests, Android unit tests, Android assemble, content validation, replay,
    save-compat, and benchmark -> pass.
  - Replay hashes: canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`; benchmark: canonical
    `328 ms`, kill `66 ms`, rebuild `4.1305 ms`.
  - `me-verifier` -> pass; all `boundary_checks` true. Per-tick trajectory parity, pacing modes,
    pause/restart timing, overflow-safe timestamps, and speed layout bounds are covered.
- Decisions:
  - No ADR: presentation-only Android-local state, no dependency or save-schema change.
- Known follow-ups:
  - Device/instrumentation and FrameMetrics/JankStats evidence remain pending.
  - Extreme `200x600` selected-panel overflow is a manual layout risk; pre-existing `pausedSave`
    rollback risk is outside ENG-015; `0x` idle HUD redraw is an accepted CPU/battery trade-off.
- Next exact action:
  - Select the next backlog item after ENG-030; the current roadmap does not define a unique
    successor.

### 2026-07-21 - MyEngine ENG-030 (wave preview and early wave call)

- Status: Done / accepted; docs close-out completed
- Owner: Codex
- Implementation:
  - A typed `CallWaveEarlyCommand` starts the next wave before its scheduled tick, while the
    immutable HUD projects deterministic next-wave composition and countdown from one projection.
  - `WaveEarlyCallBonus(resourceId, amount)` is optional, content-defined, validated as paired,
    positive, and resource-backed; existing packs remain unchanged because no balance value was
    approved.
  - Calls at/after the scheduled boundary or while enemies are active are rejected without
    authoritative mutation. `SandboxSaveCodec` v8 preserves typed pending commands and migrates
    v1-v7 saves.
- Verification:
  - Full Gradle tests with the JDK 17 fallback, content validation (2 packs), replay, save-compat,
    benchmark, and `android:assembleDebug` -> pass.
  - Replay hashes: canonical `12a65fd2b87593cf`, kill `bb37eefc1903cc77`.
  - Benchmark: canonical `473 ms`, kill `78 ms`, goal-field rebuild `8222800 ns`.
  - Renderer, simulation, save, Android, and final verifier reviews -> pass; exact scheduled-tick
    boundary, active-wave rejection, replay, and mid-countdown save coverage pass.
  - Balance review -> partial: current content packs are valid and contain no hardcoded bonus; its
    schema-documentation gap was closed in this docs close-out, and the optional bonus remains
    unconfigured pending an approved balance value.
- Known risks:
  - Low non-blocking per-snapshot HUD allocation and device profiling follow-up remains open.
  - The existing save delimiter assumption remains documented; no code fix was made in this
    documentation-only close-out.
- Next:
  - Select the next backlog item after ENG-030; the current roadmap does not define a unique
    successor, so backlog sequencing is required before another feature.

### 2026-07-28 - MyEngine ENG-028 (sprite/atlas references in content schema)

- Status: Done / accepted; Android device and PROC-009 golden-image follow-ups remain manual-pending
- Owner: Codex
- Implementation:
  - Added optional pack-relative sprite or minimal-atlas references for tiles, towers, tower tiers,
    enemies, and minimal building definitions with actionable path/key validation.
  - Opaque refs cross the immutable sandbox snapshot into `RenderFrame`; desktop and Android
    consumers resolve available refs and retain deterministic palette fallback otherwise.
  - Added original text atlas placeholder content, focused content/render/desktop/sandbox tests, and
    schema/roadmap/handoff close-out documentation. No save schema or dependency edge changed.
- Verification:
  - Full Gradle tests, projects, focused tests, content validation (2 packs), replay, save-compat,
    benchmark, Android unit/assemble, desktop render smoke, selfcheck, and `git diff --check` -> pass.
  - Replay hashes remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; benchmark
    `sim_ms=341`/`71`, goal-field rebuild `9726600 ns`.
  - Tester initially found a missing building entity projection; local repair closed it and the
    focused render test passed. Conditional reviewer agents were unavailable due thread limit;
    local read-only boundary review passed.
- Known risks:
  - Android AssetManager runtime/device fixture and full PROC-009 screenshot/golden lane remain
    pending. Minimal building entities reuse the generic placeholder entity kind until a distinct
    render kind is justified by gameplay requirements.
- Next:
  - Select the next backlog item after ENG-028; roadmap sequencing is required before another feature.

### 2026-07-28 - MyEngine ENG-009 (splash damage + shot events)

- Status: Done / accepted
- Owner: Codex
- Implementation:
  - Optional tower `splashRadius` and `falloff` fields provide stable entity-id-ordered Manhattan
    AoE. Damage is resolved by the documented integer per-ring rule; default content packs keep no
    unapproved splash balance values.
  - Immutable `ShotEvent` and `HitEvent` source/target/tick lists expose only the latest completed
    tick to snapshot consumers. They are transient presentation data, replaced each tick, and do
    not participate in save encoding or stable hashing; `SandboxSaveCodec.SAVE_VERSION` stays `8`.
  - Balance reporting now exposes splash tower count, radius/falloff totals, and effective
    non-zero-damage Manhattan AoE tiles.
- Verification:
  - Full `./gradlew.bat test` and `./gradlew.bat projects`, content validation (2 packs), replay,
    save-compat, benchmark, and `:android:assembleDebug` -> pass.
  - Replay hashes remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; benchmark:
    canonical `324 ms`, kill `62 ms`, goal-field rebuild `7168200 ns`.
  - Balance review and final verifier -> pass with all boundary checks true.
- Next:
  - Perform backlog sequencing and select the next exact feature; the current roadmap has no
    unique successor after ENG-009.

### 2026-07-29 - ENG-020 (spatial index + 1k-entity benchmark)

- Status: Done / accepted
- Owner: Codex
- Created/changed:
  - `engine-defense/src/main/kotlin/dev/myengine/defense/GridSpatialIndex.kt`
  - `engine-defense/src/main/kotlin/dev/myengine/defense/DefenseRuntime.kt`
  - `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolReports.kt`
  - `engine-devtools/src/main/kotlin/dev/myengine/devtools/DevtoolsMain.kt`
  - `engine-defense/src/test/kotlin/dev/myengine/defense/DefenseRuntimeTest.kt`
  - `engine-defense/src/test/kotlin/dev/myengine/defense/GridSpatialIndexTest.kt`
  - `engine-devtools/src/test/kotlin/dev/myengine/devtools/DevtoolReportsTest.kt`
- Result:
  - Targeting and splash candidate queries use an internal, non-persisted grid index with exact
    post-filters, live entity resolution, stable entity-id ordering, and preserved Manhattan
    semantics.
  - Devtools exposes deterministic machine-readable metrics for 1024 concurrent enemies, 16 towers,
    and 16 queries.
- Verification:
  - Focused tests (14 engine-defense, 16 engine-devtools), full Gradle tests, projects, content
    validation (2 packs), replay, save-compat, benchmark, and `git diff --check` -> pass.
  - Replay hashes remain canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`; benchmark
    `spatial-index-1k` reports `5.3045 ms`.
- Decisions:
  - The index remains an engine-defense implementation detail; no save/content schema, Android,
    render, public API, dependency, or ADR change was introduced.
- Known follow-up:
  - Seeded differential coverage does not yet provide end-to-end `updateTowers` parity across every
    targeting-mode and splash combination; `me-tester` owns this low, non-blocking follow-up.
- Next:
  - Perform backlog sequencing; the roadmap does not define a unique successor after ENG-020.

### 2026-07-29 - PROC-013 (spec board hygiene, Variant B)

- Status: Done / documentation close-out
- Owner: Codex
- DONE:
  - Moved 23 verified-done cards from `.claude/specs/backlog/` to `.claude/specs/done/` and
    reconciled the PROC-013 roadmap row.
  - Added the read-only board checker and wired it into `scripts/me-selfcheck.ps1`.
- DECISIONS:
  - No ADR: no canonical contract or adapter changed. Checker exit 0 means pass; exit 1 means a
    board/roadmap mismatch.
- NEXT:
  - Final commit/push remains blocked by the pre-existing unrelated dirty
    `.ai/retro/retro-2026-07-28.md` unless the user approves/clears it; then perform backlog
    sequencing after ENG-020.
- BLOCKERS:
  - Only the unrelated dirty retro file blocks final commit/push for this close-out.
- VERIFICATION:
  - Developer, tester, runner, and verifier passed; checker/selfcheck and `git diff --check` are
    the documentation close-out checks.

### 2026-07-29 - PROC-003 (domain systems sequencing) / Phase 15

- Status: Done / documentation close-out
- Owner: Claude
- Created/changed:
  - `Plane/15_domain_systems_sequencing.md` (new phase plan: 15.1 flow-field done, 15.2 colony
    slice ordered, 15.3 storyteller incidents)
  - `.claude/specs/done/PROC-003-domain-roadmap.md` (status done, criterion (b) amendment, close
    note; backlog card superseded)
  - `.claude/specs/ENGINE_ROADMAP.md` (PROC-003 row done, adopted chain, demand-tag corrections
    for ENG-012/021/022/029)
  - `STATE.md`, `.ai/handoff.md`, `.ai/DIGEST.md`, `Plane/README.md`
- Verification:
  - Documentation-only change; no engine code. Board checker covers card status/location after
    the move to `done/`.
- Decisions:
  - Adopted chain after ENG-020: ENG-010 -> ENG-016 -> PROC-007 -> ENG-021 -> ENG-029 -> ENG-012
    -> ENG-007 -> ENG-018; unique successor ENG-010 (MyTD FR-007).
  - PROC-003 criterion (b) amended by owner: `vision:*` demand tags count as demand where no
    named game FR exists yet.
  - Demand tags corrected: `mytd` removed from ENG-012/021/022/029 as unbacked by the MyTD bundle.
- Next:
  - Run `/me --feature --next` for ENG-010 (status effects framework).

### 2026-08-01 - ENG-010 (status effects framework)

- Status: Done / accepted
- Owner: Codex
- Created/changed:
  - `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt`
  - `engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt`
  - `engine-content/src/test/kotlin/dev/myengine/content/ContentPackLoaderTest.kt`
  - `engine-entities/src/main/kotlin/dev/myengine/entities/EntityModel.kt`
  - `engine-defense/src/main/kotlin/dev/myengine/defense/DefenseRuntime.kt`
  - `engine-defense/src/test/kotlin/dev/myengine/defense/DefenseRuntimeTest.kt`
  - `engine-render/src/main/kotlin/dev/myengine/render/RenderModel.kt`
  - `engine-render/src/main/kotlin/dev/myengine/render/PlaceholderRenderSurface.kt`
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`
  - `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxStatusEffectTest.kt`
  - save-version expectation updates in Android/sandbox tests
- Result:
  - Added optional data-defined slow and DoT effects with refresh/stack/ignore semantics.
  - Added deterministic entity status state, DoT damage/rewards, slow movement modifiers, v9 save
    migration, and sorted immutable snapshot/render effect tags.
- Verification:
  - Full `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation, replay,
    save-compat, benchmark, `.\gradlew.bat :android:assembleDebug`, and `git diff --check`
    passed. Focused content/defense/render/sandbox tests passed.
  - Save-compat review passed; renderer review passed after defensive tag copying; the simulation
    review's non-enemy DoT metrics finding was fixed with regression coverage. Partial slow uses
    the documented integer-floor rule. Other conditional reviewers/final verifier were unavailable
    after the app subagent-thread limit; device/performance follow-ups remain manual-pending.
- Decisions:
  - No ADR: additive generic status state and existing versioned-save/content boundaries suffice.
  - Default content packs remain unchanged; no balance values or Android simulation logic added.
- Next:
  - Run `/me --feature --next` for ENG-016 (incident execution pipeline + RNG fix).

### 2026-08-02 - MyEngine ENG-016 (incident execution pipeline + RNG fix)

- Status: Done / accepted; documentation close-out completed
- Owner: Codex
- DONE:
  - Closed optional incident content with cadence start/end ticks, pacing threat windows, cooldowns,
    and typed `spawn_wave`, `resource_event`, and `modifier` effects.
  - Confirmed the stateful deterministic director uses a persistent RNG cursor and that the sandbox
    interpreter preflights atomically; repeated resource/modifier effects aggregate via `Long`
    before overflow checks and cross-field validation reports incident field paths.
  - `SandboxSaveCodec` v10 persists incident/RNG/modifier state with v1-v9 migration; moved the
    feature card to `.claude/specs/done/` and synced the schema/roadmap/handoff/state/digest docs.
- DECISIONS:
  - No ADR. Default pack balance and Android production, renderer, and input boundaries remain
    unchanged; this close-out edits documentation only.
- NEXT:
  - Run `/me --feature --next` for PROC-007, then `ENG-021 -> ENG-029 -> ENG-012 -> ENG-007 -> ENG-018`.
- BLOCKERS:
  - No ENG-016 implementation blocker remains. No device proof is claimed; existing device and
    FrameMetrics/JankStats/manual performance follow-ups remain pending.
  - Gradle requires process-local `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.
- VERIFICATION:
  - Full Gradle test/projects/content/replay/save-compat/benchmark/diff-check, focused
    `SandboxIncidentTest`/content tests, and simulation/save reviews passed.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
  - Remediation benchmark: `sim=418 ms`, `kill=85 ms`, `spatial-index-1k=6.1036 ms`,
    `goal-field=10.427 ms`; initial `614/120 ms` values are superseded.

### 2026-08-02 - MyEngine PROC-007 (save migration matrix)

- Status: Done / accepted
- Owner: Codex
- Created/changed:
  - `games/sandbox/src/test/resources/save-fixtures/v1.properties` through `v10.properties`
  - `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSaveMigrationMatrixTest.kt`
  - `scripts/me-save-compat.ps1`
  - `.claude/specs/done/PROC-007-save-migration-matrix.md`
- Result:
  - Every released sandbox save version is checked in and loaded through `SandboxSaveCodec`.
  - The matrix compares each result to an independently constructed canonical state hash and
    repeats decoding to prove deterministic migration.
  - The save-compat JSON now includes `matrix` and runs the matrix test.
- Verification:
  - Focused matrix, full tests, projects, content validation, replay, save-compat, benchmark,
    `git diff --check`, save reviewer, and final verifier -> pass.
  - Replay hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`; matrix passed twice.
- Decisions:
  - No ADR; production codec/save version and Android/render boundaries are unchanged.
- Next:
  - Run `/me --feature --next` for ENG-029 (audio event hooks).

### 2026-08-02 - ENG-021 (save slots + autosave policy)

- Status: Done / accepted
- Owner: Codex
- Created/changed:
  - `.claude/specs/done/ENG-021-save-slots-autosave.md` (moved from `backlog/`)
  - `.claude/specs/ENGINE_ROADMAP.md`
  - `STATE.md`
  - `.ai/handoff.md`
  - `.ai/DIGEST.md`
  - `Plane/README.md`
- Result:
  - Named slots use the separate `slots/` namespace; config-driven rotating autosaves use
    `autosave/`.
  - Writes use a flushed temporary file and `ATOMIC_MOVE` with no non-atomic fallback.
  - Slot metadata is readable without a full load; corruption-only fallback selects the latest
    good autosave. `SandboxSaveCodec.SAVE_VERSION` remains 10 and the Android Bundle path is
    unchanged.
- Verification:
  - Full tests, projects, content validation, replay, save-compat matrix, benchmark, Android
    assemble, focused `SandboxSaveSlotsTest`, and `git diff --check` -> pass.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Benchmark: `sim=473 ms`, `kill=87 ms`, `spatial-index-1k=6.6675 ms`,
  `goal-field rebuild=10459200 ns`.

- Decisions:
  - No ADR. The existing Android Bundle lifecycle path and codec version remain unchanged.
- Blockers:
  - Pre-existing low manual device/emulator Bundle save/restore smoke limitation remains.
- Next:
  - Run `/me --feature --next` for ENG-029 (audio event hooks).

### 2026-08-02 - ENG-012 (boss/elite enemies + wave modifiers)

- Status: Done / accepted; documentation close-out completed
- Owner: Codex
- Created/changed:
  - `engine-content/` enemy rank/scaling and indexed wave-modifier definitions, parser, and tests
  - `engine-defense/` deterministic effective spawn state and persisted enemy combat metadata
  - `engine-entities/` `EnemyComponent` extension with effective speed/reward/rank state
  - `engine-render/` boss marker projection through immutable render data
  - `games/sandbox/` save v11 encoding/decoding with v1-v10 migration fallback and tests
  - `engine-devtools/` effective rank/scaling balance metrics
  - `docs/content-schemas/PROPERTIES_SCHEMA.md`, ENG-012 card, roadmap, state, handoff, and digest
- Result:
  - Elite/boss flags and health/speed/reward scaling are content-defined and validated. Indexed wave
    modifiers cover consecutive authored spawns in list order; scaling uses integer floor for health/
    speed and deterministic half-up rounding for rewards. Existing default content remains unchanged.
- Verification:
  - Full `.\gradlew.bat test`, `.\gradlew.bat projects`, content validation, replay, save-compat
    v1-v11 matrix, benchmark, `:android:assembleDebug`, focused tests, selfcheck, and
    `git diff --check` -> pass.
  - Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
- Decisions:
  - No ADR. Effective enemy state is persisted only when non-default rank/scaling/modifier data needs
    to survive restore; legacy entities preserve canonical hashes and v1-v10 saves migrate to v11.
- Known limitations:
  - Conditional reviewer agents were unavailable after repeated thread timeouts; local simulation,
    render, save, and content boundary review passed. No device/emulator proof is claimed beyond the
    Android assemble gate.
- Next:
  - Run `/me --feature --next` for ENG-011 (enemy armor + damage types), then continue `ENG-018`.

### 2026-08-02 - ENG-007 (multiple spawn points + per-wave routing)

- DONE:
  - `WaveContent.spawnSelection` is optional and supports default/all or pipe-separated named
    spawn ids. Cross-reference/reachability validation, reserved map spawn-id guards, and a
    checked-in multi-spawn fixture are in place.
  - Scheduled, early, and incident waves route in deterministic sorted spawn-id -> authored
    `WaveSpawn` -> instance order. `SandboxSaveCodec.SAVE_VERSION` remains 11.
- DECISIONS:
  - No ADR; the additive content/runtime change preserves the existing save format and replay
    contract.
- NEXT:
  - Run `/me --feature --next` for ENG-018. ENG-011 remains the separate armor + damage types card.
- BLOCKERS:
  - No implementation blocker remains. Only the pre-existing Gradle 10 deprecation warning is
    low-severity; no device/emulator claim is made beyond `assembleDebug`.
- VERIFICATION:
  - Full tests, projects, content validation (2 existing packs plus the checked-in multi-spawn
    fixture), replay, save-compat v1-v11 matrix plus `SandboxMultiSpawnTest`, benchmark,
    `:android:assembleDebug`, and `git diff --check` passed.
  - Replay: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`.
  - Benchmark: `sim_ms=364`, `kill_sim_ms=87`, `goal_field_rebuild_ns=9048200`,
    `spatial_index_1k_ms=5.7197`; tester remediation focused tests: 59 passed; simulation,
    save-compat, and verifier reviews passed.
### 2026-08-02 - ENG-018 (endless wave generation)

- Status: Done / accepted
- Result: Added validated `endless.properties` composition cycles, interval scheduling, content-defined
  count/health/reward growth, shared-RNG deterministic generation, `NO_WIN` enforcement, and the
  `endless-scaling` machine-readable devtools report.
- Verification: Full `gradlew.bat test`, `projects`, content validation, replay, save-compat matrix,
  benchmark, focused generator/loader/defense/sandbox/devtools tests, and `git diff --check` passed.
- Decisions: No ADR; the additive feature keeps the Android-free simulation boundary and
  `SandboxSaveCodec.SAVE_VERSION=11`.
- Limitations: Conditional simulation reviewer and final verifier worker threads timed out; local
  boundary review plus the complete runner gate set passed. No device/emulator claim was made.
- Next: Run `/me --feature --next` for ENG-011 (enemy armor + damage types).

### 2026-08-02 - ENG-011 (enemy armor + damage types)

- Status: Done / accepted; documentation close-out
- Owner: Codex
- Result: Approved Option A adds static typed damage content and enemy percentage resistances with
  deterministic bidirectional validation. `DamageFormula` uses the documented Long-intermediate,
  single-final-floor formula for direct and splash runtime damage; zero damage emits no `HitEvent`.
  Effective-DPS reporting is deterministic under single-target, in-range, no-splash,
  `ticks_per_second=20` assumptions. `SandboxSaveCodec.SAVE_VERSION=11` is unchanged.
- Verification: focused ENG-011 suite passed 24 tests after the `Int.MAX_VALUE` boundary test;
  full tests, projects, content validation, replay, save-compat, benchmark, Android assemble, and
  `git diff --check` passed. Conditional simulation and balance reviewers passed; the low simulation
  finding was resolved. Replay hashes: canonical `e4892bcc18f9d8dc`, kill `a763da4ac32b15b4`,
  resist `3f02607020d48668`. Benchmark: `sim=422 ms`, `kill=63 ms`,
  `goal-field=10743500 ns`, `spatial-index=6.4719 ms`.
- Blockers: no implementation blocker; no device/emulator evidence is claimed.
- Next: Run `/me --feature --next` for ENG-001 (A* point-to-point pathfinding for agents).

### 2026-08-02 - ENG-001 (A* point-to-point pathfinding for agents)

- Status: Done / accepted; documentation close-out completed
- Owner: Codex
- DONE: Added deterministic `engine-world` 4-neighbor uniform integer-cost A* with stable
  `(f, row-major tile index)` open-set ordering, stable neighbors, first predecessor on equal `g`,
  bounds/blocked/no-path handling, and optional occupied-start support. `engine-ai` preserves the
  `PathRequest`/`PathResult` API through A* and adds `AgentPathPlanner` for valid stored paths and
  deterministic repaths after route/world changes. Wave enemies remain on ENG-002 `GoalField`.
- DECISIONS: No ADR. No save/content/render/Android/dependency changes and no game-bundle
  traceability update. Full Movement/job tick integration is intentionally deferred to ENG-003/ENG-004.
- NEXT: Run `/me --feature --next` for ENG-003 (JobBoard wired into tick).
- BLOCKERS: No implementation blocker. The missing full Movement/job tick wiring is intentional;
  colony demand remains vision-only pending the recorded MySD Gate 1 or authored-game-spec trigger.
- VERIFICATION: Focused A* and planner tests, full `test`, `projects`, content validation,
  sim-replay, save-compat, benchmark, and `git diff --check` passed. Replay hashes:
  `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, `3f02607020d48668`. Benchmark: canonical 413 ms,
  kill 102 ms, GoalField rebuild 13099400 ns, spatial index 6.3748 ms. Final verifier passed with
  no findings and all boundary checks true; tie/equal-g and `pathIndex` findings were remediated.

### 2026-08-02 - ENG-003 (JobBoard wired into tick)

- Status: Done / accepted; post-Phase-14/Phase-15 feature close-out. No new phase was created.
- Owner: Codex
- DONE: Wired the Android-free `JobExecutionSystem` into the sandbox fixed-tick pipeline. Positioned
  `JobActorComponent` entities claim jobs in deterministic worker/entity and priority/job-id order;
  lifecycle advances through `CLAIMED -> IN_PROGRESS -> DONE/FAILED`; pathfinding movement, work
  ticks, typed `resource_delta` completion effects, invalid-target/no-path release, reservation
  guards, and same-tick reclaim prevention are covered. `SandboxSaveCodec` v13 persists JobBoard,
  in-flight jobs, actor assignment/progress, and effects with v12 migration to empty job state.
- DECISIONS: No ADR, no game-bundle traceability update, and no plugin/skill/pipeline contract
  change. Approved defaults remain: every positioned `JobActorComponent` is eligible for all job
  types, one work tick per simulation tick, in-world `TilePosition` validation, and deterministic
  release to `OPEN` for invalid/no-path jobs.
- NEXT: Run `/me --feature --next` for ENG-031 (stockpile zones + designations). Colony demand
  remains vision-only pending MySD Gate 1 or an authored colony spec with named FRs.
- BLOCKERS: No implementation blocker. Non-blocking follow-ups are: two-worker replay is not in
  `DevtoolReports.replayInspect`; `scripts/me-save-compat.ps1` does not separately invoke
  `SandboxJobExecutionTest`; and no job-heavy benchmark covers large worker/job counts or
  invalidated paths.
- VERIFICATION: Selfcheck, full test/projects/content validation/replay/save-compat/benchmark,
  `:android:assembleDebug`, and `git diff --check` passed. Focused `JobExecutionSystemTest` and
  `SandboxJobExecutionTest` passed. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`,
  `3f02607020d48668`. Final benchmark: simulation 430 ms, kill 76 ms, spatial index 6.6127 ms.
  Final verifier passed with all boundary checks true.

### 2026-08-02 - ENG-031 (stockpile zones + designations)

- Status: Done / accepted; documentation close-out completed. No new phase was created.
- DONE: Accepted Option A delivers deterministic zone commands/store state, validated stockpile
  resource filters, one-shot harvest-node designation jobs, immutable `EngineSnapshot.zones`, and
  `SandboxSaveCodec` v14 with v1-v13 migration.
- DECISIONS: No ADR or game-bundle traceability update. Colony demand remains vision-only. Hauling
  is now delivered by ENG-004; generic stockpile capacity, depletion/repeated harvest, and actual
  Android `RenderFrame`/view overlay rendering remain follow-up scope. No plugin/skill/pipeline contract changed.
- NEXT: Run `/me --feature --next` for ENG-032 (construction blueprints).
- BLOCKERS: No implementation blocker. Non-blocking follow-ups: claimed/in-progress designation
  removal can leave its job and allow a second job on the same node; generic pending-command
  delimiter escaping remains a pre-existing codec concern while ENG-031 ids are regex-safe;
  RenderFrame/Android view do not consume `snapshot.zones`; per-frame zone snapshot allocations need
  later profiling/consumer work.
- VERIFICATION: Selfcheck, focused ENG-031/remediation tests, full tests/projects, content validation
  (2 packs), replay, save-compat, benchmark, Android assembleDebug, and `git diff --check` passed
  with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. Replay hashes:
  `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, `3f02607020d48668`; benchmark: canonical 413 ms,
  kill 85 ms, spatial index 5.2368 ms, goal rebuild 10958000 ns. Simulation, renderer, save,
  Android, and verifier reviews found no blocker; verifier boundary checks were all true.

### 2026-08-02 - ENG-004 (first worker agent MVP / hauling)

- Status: Done / accepted; documentation close-out completed. No new phase was created.
- DONE: Added optional content-defined workers with speed/capacity, deterministic `HaulSourceStore`
  reservations, typed haul job payload/phase, `InventoryComponent` in-transit carry, speed-budgeted
  source-to-stockpile execution, persisted stockpile contents, and positioned ProducerSystem output
  sources without double-counting global inventory.
- DECISIONS: No ADR or game-bundle traceability update. Generic jobs remain supported through an
  eligibility filter; legacy replay hashes remain unchanged when no worker/haul state is present.
  `SandboxSaveCodec` is v15 with v1-v14 migration. Colony demand remains vision-only; generic
  stockpile capacity/depletion, worker spawn commands, and Android overlay consumption remain follow-up scope.
- NEXT: Run `/me --feature --next` for ENG-032 (construction blueprints).
- BLOCKERS: No implementation blocker. No device/emulator or visual-golden proof is claimed beyond
  the Android assemble gate.
- VERIFICATION: Selfcheck, full `test` (132 sandbox tests plus engine/desktop/Android suites),
  `projects`, content validation, replay, save-compat, benchmark, `:android:assembleDebug`, and
  `git diff --check` passed. Replay hashes: `e4892bcc18f9d8dc`, `a763da4ac32b15b4`.

### 2026-08-02 - ENG-032 (construction blueprints)

- Status: Done / accepted; no new phase was created.
- DONE: Added blueprint/cancel commands, source-aware construction sites, a construction haul
  destination, generic build jobs with content-defined work ticks, and completed-building spawn.
  Blueprints are non-blocking until completion; placement keeps a prospective route-safety guard.
- DECISIONS: User selected refund to each haul's original `HaulSourceStore` and automatic source
  selection in deterministic ascending `sourceId` order with retry. Save v16 migrates v1-v15 with
  empty construction state. No ADR or plugin version bump.
- NEXT: ENG-032 is complete; ENG-033 is the next candidate, subject to the vision-only colony gate.
- VERIFICATION: Full tests/projects/content validation/replay/save-compat/benchmark/selfcheck,
  focused construction tests, Android `assembleDebug`, and `git diff --check` passed. Conditional
  reviewer workers timed out after bounded waits; local boundary review passed.
