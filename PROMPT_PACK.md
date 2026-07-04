# MyEngine Prompt Pack

Готовый набор промтов для создания `MyEngine`: Android-first 2D game engine / game framework под несколько собственных игр в духе colony sim, tower defense, factory/logistics и minimalist mobile strategy.

Ключевая идея: строить не "универсальный движок на все случаи", а многоразовую игровую платформу под конкретный класс игр. Низкоуровневый runtime можно взять готовый, а собственным сделать именно simulation/content/gameplay engine, dev pipeline, spec pipeline, тестирование, балансировку и self-improvement loop.

## Опорные выводы

- Android - единственная целевая платформа релиза. Desktop target допустим только как dev harness для быстрых симуляционных тестов, отладки и редактора.
- Референсы не должны клонироваться по IP, контенту, названиям или ассетам. Из них берутся системные качества:
  - RimWorld: colony management, pawn needs/mood/jobs, event storyteller, emergent stories, moddable/data-rich systems.
  - Infinitode 2: endless TD, компактный mobile-friendly стиль, глубокие upgrade/research деревья, map editor, run statistics.
  - Mindustry: factory + tower defense + RTS, supply chains, production blocks, waves, map editor, configurable rules.
  - Minimalist Tower Defense: короткие 5-15 минутные мобильные сессии, чистая стратегия без перегруженных меню, много башен и upgrade paths.
- Базовая техническая гипотеза: Kotlin/JVM or Java + libGDX как runtime, поверх него собственный engine layer. Но первый архитектурный промт должен оформить ADR и разрешить агенту выбрать иначе только при сильном обосновании.
- Главный differentiator проекта - agentic self-development: skills, subagents, spec/dev/review/test/verify cycles, telemetry, retrospectives, human-gated improvement propagation.

## Репозитории-референсы

Используй эти проекты как "source of inspiration" и библиотеку инженерных решений. Перед копированием кода, ассетов, схем или крупных фрагментов обязательно делай license ADR: что берём, откуда, под какой лицензией, совместимо ли это с планируемой лицензией `MyEngine`, нужна ли атрибуция, не тащит ли это copyleft на весь проект. По умолчанию забирать идеи, структуры, trade-offs и тестовые подходы, а не код.

### Tier A - самые близкие к MyEngine

- [Anuken/Mindustry](https://github.com/Anuken/Mindustry) - Java, Android/Desktop, automation tower-defense RTS. Что забрать: Gradle multi-target layout, Android/Desktop dev loop, content taxonomy для blocks/items/units/waves/research, map editor/modding mindset, campaign/tech tree, production/logistics + defense integration. Лицензия GPL-3.0: код не копировать без осознанного решения сделать совместимую лицензию.
- [yairm210/Unciv](https://github.com/yairm210/Unciv) - Kotlin, LibGDX, Android/Desktop, moddability-focused 4X. Что забрать: Kotlin/libGDX project organization, data-driven rules/mods, low-end Android UX, localization/civilopedia-style in-game docs, issue/community workflow. Лицензия MPL-2.0: можно изучать, прямое заимствование файлов требует аккуратного соблюдения MPL.
- [rossturner/king-under-the-mountain](https://github.com/rossturner/king-under-the-mountain) - Java 17 + LibGDX colony/settlement sim, MIT. Что забрать: colony/world/job architecture, `core` + `desktop` split, asset packing, simulation-based settlement patterns.
- [rossturner/mountaincore](https://github.com/rossturner/mountaincore) - более поздний colony sim в Java/LibGDX, MIT. Что забрать: production/storage/building systems, large simulation code organization, packaging/release tooling, content scale lessons.
- [mafik/libcolony](https://github.com/mafik/libcolony) - MIT task scheduling library for colony sims. Что забрать: conceptual model для автономных jobs, priority scheduling, anti-micromanagement design, failure/death-spiral prevention. Язык C++/JS, поэтому переносить идеи, не код.

### Tier B - полезные подсистемы и жанровые образцы

- [Anuken/Arc](https://github.com/Anuken/Arc) - Apache-2.0 Java framework used by Mindustry. Что забрать: lightweight game framework patterns, asset/UI/input utility ideas, но не принимать как готовую зависимость без ADR: сам автор предупреждает, что документации нет и проект заточен под Mindustry.
- [00-Evan/shattered-pixel-dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) - Java/libGDX Android/Desktop/iOS roguelike, GPL-3.0. Что забрать: mature mobile-first game structure, procedural content, saves, releases, docs for building/forking, compact UI for complex systems. Код не копировать без GPL-compatible решения.
- [ogzkrt/OTD](https://github.com/ogzkrt/OTD) - MIT Java/libGDX tower defense for Android/Desktop. Что забрать: минимальный TD vertical slice, tower placement, Android emulator gameplay loop, desktop parity.
- [bneukom/heavydefense](https://github.com/bneukom/heavydefense) - Java/libGDX Android tower defense. Что забрать: modes with/without roads, tower upgrades, achievements, simple Android TD scope. Лицензию проверить перед любым кодовым заимствованием.
- [OpenRA/OpenRA](https://github.com/OpenRA/OpenRA) - GPL-3.0 RTS engine with strong modding and Mod SDK. Что забрать: engine-vs-game separation, mod SDK mindset, data-driven rules, command/order model, replay/network determinism ideas. Не Android и не Java; копировать код нельзя без GPL-compatible стратегии.
- [Warzone2100/warzone2100](https://github.com/Warzone2100/warzone2100) - GPL-2.0 RTS with large tech tree, base missions and unit designer. Что забрать: research/tech tree scale, unit customization, campaign/skirmish split, renderer backend abstraction as long-term inspiration.
- [freeciv/freeciv](https://github.com/freeciv/freeciv) - GPL-2.0 empire strategy with mature rules and AI. Что забрать: ruleset/data-driven design, tech/progression, diplomacy/AI documentation, civilopedia-style explainability.

### Libraries and setup references

- [libgdx/gdx-liftoff](https://github.com/libgdx/gdx-liftoff) - modern LibGDX Gradle project generator. Что забрать: initial project shape, Gradle conventions, Android/Desktop modules.
- [libktx/ktx](https://github.com/libktx/ktx) - Kotlin extensions for LibGDX. Что забрать: idiomatic Kotlin wrappers, asset/screen/input utilities, modular dependency approach.
- [libgdx/ashley](https://github.com/libgdx/ashley) - small ECS under the libGDX family. Что забрать: ECS API shape, system ordering, component simplicity, or use as dependency if ADR approves.
- [libgdx/gdx-ai](https://github.com/libgdx/gdx-ai) - Apache-2.0 AI framework: A*, hierarchical pathfinding, behavior trees, state machines, steering. Что забрать: pathfinding/AI primitives and terminology; consider dependency instead of rewriting AI from scratch.

### Agentic-development references

- `D:\Pet\mobile-pipeline` - главный локальный процессный референс. Что забрать: canonical markdown source, thin Claude/Codex adapters, spec/dev split, structured JSON contracts, telemetry, retro, human-gated improvements.
- [swe-agent/swe-agent](https://github.com/swe-agent/swe-agent) - autonomous issue-fixing agent harness. Что забрать: issue-to-patch workflow, tool boundaries, evaluation mindset.
- [OpenHands/openhands](https://github.com/OpenHands/openhands) - open-source software development agent platform. Что забрать: agent workspace/session model, sandboxing concepts, task orchestration and observability.
- [aider-ai/aider](https://github.com/Aider-AI/aider) - terminal AI pair programmer. Что забрать: repo-map, git-native edit loop, automatic lint/test/fix rhythm, self-development discipline.
- [VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents) and [ComposioHQ/awesome-claude-skills](https://github.com/ComposioHQ/awesome-claude-skills) - catalogs of subagents/skills. Что забрать: role taxonomy and packaging ideas, not prompts blindly.

## Как использовать

1. В свежем репозитории сначала дать модели `PROMPT 0`.
2. После получения плана и начального scaffold дать `PROMPT 1`.
3. Затем `PROMPT 2` для agentic pipeline.
4. После этого идти вертикальными срезами через `PROMPT 3`.
5. Для каждого будущего game project использовать `PROMPT 5`.
6. `PROMPT 4` и `PROMPT 6` периодически запускать как обслуживание системы.

Если нужно одним сообщением запустить всё сразу, используй `PROMPT 7`, но лучше дробить: качество будет выше.

---

## PROMPT 0 - Master Bootstrap / Product Definition

```text
Ты - senior game engine architect, Android game developer, build-system engineer и designer of agentic development pipelines.

Контекст:
Мы создаём `MyEngine` в репозитории `D:\Pet\MyEngine`. Это не один игровой проект, а Android-first reusable 2D game engine / framework для нескольких будущих игр. Игры будут в области:
- colony/survival simulation with emergent stories;
- tower defense with waves, towers, upgrades and run statistics;
- factory/logistics/base-defense with supply chains and production blocks;
- minimalist mobile strategy with short readable sessions.

Важный ориентир процесса:
Есть локальный референс `D:\Pet\mobile-pipeline`. Его нужно изучить как пример skills/subagents/spec/dev/selfimprove pipeline: canonical markdown source, thin Claude/Codex adapters, structured JSON contracts, human gates, telemetry, retrospectives, plugin/skill packaging. Не копируй всё слепо; перенеси принципы и адаптируй под game-engine домен.
Также изучи раздел `Репозитории-референсы` из `PROMPT_PACK.md`. Для каждого Tier A проекта выпиши 3-7 решений, которые стоит перенять в `MyEngine`, и 1-3 решения, которые перенимать нельзя или рано. Не копируй код/ассеты без отдельного license ADR.

Непереговорные требования:
1. Shipping platform: Android only.
2. Dev harness может иметь desktop/JVM runner, если это ускоряет тесты, симуляцию, редактор или debug UI.
3. Движок должен обслуживать несколько игр, а не hardcode-ить одну.
4. Game logic должна быть deterministic where practical: fixed tick, seedable RNG, replayable simulations, clear separation simulation/render/input.
5. Контент должен быть data-driven: entities, tiles, towers, recipes, waves, events, research, scenario rules, localization strings.
6. Система должна включать self-development loop для Claude и Codex: skills, subagents, scripts, memory, telemetry, retro, improvement proposal, human gate, propagation.
7. Не создавать клон RimWorld/Infinitode/Mindustry/Block Defense. Создать оригинальную платформу, которая поддерживает похожие классы mechanics.
8. Не начинать с огромной игры. Сначала engine scaffold + один vertical slice, который доказывает архитектуру.

Сначала сделай исследование:
- прочитай текущий репозиторий;
- если доступен `D:\Pet\mobile-pipeline`, изучи его README, AGENTS, docs/ARCHITECTURE, selfimprove и plugin/skill layout;
- изучи Tier A репозитории-референсы из `PROMPT_PACK.md`: Mindustry, Unciv, King under the Mountain, Mountaincore, LibColony;
- точечно изучи Tier B/library/agentic references, если они влияют на stack, ECS, AI/pathfinding, TD vertical slice или agentic workflow;
- создай `docs/REFERENCE_RESEARCH.md`: таблица `repo -> что забираем -> что не забираем -> license risk -> влияние на архитектуру`;
- проверь, какие файлы уже есть, не перетирай пользовательские изменения;
- если выбираешь stack, создай ADR с вариантами и trade-offs.

Ожидаемый результат первого прохода:
1. `README.md` - что такое MyEngine, для каких игр, как запускать.
2. `AGENTS.md` - canonical instructions для Claude/Codex при работе над этим репозиторием.
3. `docs/ARCHITECTURE.md` - модульная архитектура engine + agentic pipeline.
4. `docs/ROADMAP.md` - staged roadmap: scaffold, MVP vertical slice, editor, game specs, self-improve.
5. `docs/DECISIONS/ADR-0001-stack.md` - выбор stack/runtime.
6. `docs/ENGINE_CONSTITUTION.md` - инварианты: deterministic simulation, Android constraints, data-driven content, testing gates, save compatibility, clones allowed / no verbatim IP copying.
7. `.ai/` workspace skeleton: handoff, memory, tasks, changes, proposals.
8. `docs/REFERENCE_RESEARCH.md` - какие решения берём из open-source референсов и какие отклоняем.
9. Начальный layout будущего engine, но без преждевременной реализации больших подсистем.

Критерии готовности:
- Репозиторий можно открыть в новой Claude/Codex сессии, прочитать `AGENTS.md` и понять правила работы.
- Архитектура явно объясняет, где simulation, rendering, content, Android shell, tooling, tests, game modules и agentic pipeline.
- Roadmap даёт первые 5-7 реалистичных итераций.
- Stack choice не является вкусовщиной: есть ADR с альтернативами.
- Self-improvement loop присутствует как first-class subsystem, а не "потом добавим".

Финальный ответ:
- коротко перечисли созданные файлы;
- назови выбранную техническую гипотезу;
- укажи следующий рекомендуемый промт.
```

---

## PROMPT 1 - Engine Architecture Deepening

```text
Ты продолжаешь работу над `MyEngine`. Твоя задача - превратить стартовые документы в исполнимую архитектуру engine.

Сначала прочитай:
- `AGENTS.md`
- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/ENGINE_CONSTITUTION.md`
- `docs/DECISIONS/*.md`
- `docs/REFERENCE_RESEARCH.md`, если есть
- раздел `Репозитории-референсы` из `PROMPT_PACK.md`, если `docs/REFERENCE_RESEARCH.md` ещё не создан
- `.ai/handoff.md`, если есть

Перед детализацией архитектуры сделай короткий reference pass:
- Mindustry: как разделены content, world, blocks, units, campaign, editor/build targets;
- Unciv: как устроены Kotlin/libGDX modules, moddability, data/rules, Android/Desktop UX;
- King under the Mountain / Mountaincore: как организованы colony jobs, production, storage/building, asset pipeline;
- LibColony: какие идеи task scheduling подходят pawn/job subsystem;
- Ashley/gdx-ai/KTX/gdx-liftoff: какие стоит использовать как dependencies или только как API inspiration.

Сформируй архитектуру как набор независимых, тестируемых модулей. Минимальный модульный состав:

1. `engine-core`
   - fixed-step tick loop;
   - deterministic RNG;
   - simulation clock;
   - event bus / command queue;
   - snapshot/replay primitives;
   - no Android dependencies.

2. `engine-world`
   - tile/chunk grid;
   - terrain, occupancy, buildability;
   - spatial index;
   - pathfinding interfaces;
   - fog/visibility hooks if useful later.

3. `engine-ecs` or equivalent component/system layer
   - entities/components/systems;
   - stable IDs;
   - serialization boundaries;
   - system ordering contract.

4. `engine-ai`
   - jobs/tasks;
   - utility scoring or behavior-tree hooks;
   - pawn needs/mood traits as optional package;
   - steering/path request integration.

5. `engine-logistics`
   - items/resources;
   - inventories;
   - production recipes;
   - producers/consumers;
   - conveyor or network abstraction;
   - throughput accounting.

6. `engine-defense`
   - waves;
   - enemies;
   - towers/turrets;
   - projectiles/hit-scan;
   - targeting strategies;
   - status effects;
   - upgrade hooks.

7. `engine-storyteller`
   - event director;
   - threat budget;
   - pacing curves;
   - incidents;
   - difficulty profiles;
   - telemetry hooks for balance.

8. `engine-content`
   - JSON/YAML schema for content packs;
   - validation;
   - localization;
   - content versioning and migrations;
   - sample content pack.

9. `engine-render`
   - camera;
   - tile/sprite rendering;
   - overlays;
   - particles/effects hooks;
   - debug draw;
   - render must observe simulation state, not mutate it.

10. `engine-android`
   - Android launcher/shell;
   - lifecycle;
   - input gestures;
   - save location;
   - performance/frame pacing notes;
   - device smoke tests.

11. `engine-devtools`
   - simulation runner CLI;
   - map/scenario editor plan;
   - content validator;
   - replay inspector;
   - debug panel.

12. `games/sandbox-*`
   - tiny sample game(s) proving engine use without polluting engine modules.

Deliverables:
- update `docs/ARCHITECTURE.md` with module boundaries, dependency graph and non-goals;
- create `docs/API_SKETCH.md` with key public interfaces/classes/data contracts;
- create `docs/CONTENT_MODEL.md` with draft schemas for tiles, entities, towers, recipes, waves, incidents, research;
- create `docs/TESTING_STRATEGY.md` with deterministic replay tests, content schema tests, save compatibility tests, simulation property tests, Android smoke tests and performance budgets;
- create ADRs for any major choice still open.

Architecture constraints:
- Simulation must be testable on JVM without Android.
- Rendering/input must not own game state.
- Content schemas must be forward-compatible: include version fields from day one.
- Save files must be versioned and migration-aware.
- Engine APIs must support multiple games with different content packs and rulesets.
- Avoid premature multiplayer, real-time networking, 3D, physics-heavy gameplay and asset-store-scale editor work in phase 1.

Acceptance:
- A new agent can read docs and know exactly where to add a new tower, pawn job, recipe, wave, incident or content validator.
- There is a clear first vertical slice that touches core/world/content/render/android/tests without exploding scope.
```

---

## PROMPT 2 - Claude/Codex Skills, Subagents and Pipeline

```text
Ты - architect of agentic software development systems. Создай для `MyEngine` dual-harness pipeline для Claude и Codex, вдохновлённый `mobile-pipeline`, но адаптированный под game-engine development.

Сначала прочитай:
- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/ENGINE_CONSTITUTION.md`
- `docs/TESTING_STRATEGY.md`
- локальный `D:\Pet\mobile-pipeline`, если доступен: README, docs/ARCHITECTURE, docs/SPEC-PIPELINE, selfimprove, mp-dev skill.
- раздел `Agentic-development references` из `PROMPT_PACK.md`: `mobile-pipeline`, SWE-agent, OpenHands, aider, Claude subagent/skill catalogs.

Reference extraction:
- из `mobile-pipeline` забери canonical-source/thin-adapter model, JSON contracts, telemetry/retro/human gate;
- из SWE-agent забери issue/spec -> patch -> tests -> report workflow and evaluation mindset;
- из OpenHands забери workspace/session/sandbox/orchestration ideas, но не усложняй локальный MVP облачной платформой;
- из aider забери repo-map/git-native lint-test-fix loop;
- из subagent/skill catalogs забери role taxonomy и packaging conventions, но не копируй промты без адаптации.

Цель:
Сделать систему, в которой engine развивается через управляемые циклы:
- `/me-spec` - создание game/engine spec bundle;
- `/me` - dev pipeline: discuss/spec/feature/bugfix/test/balance/perf/reflect/improve;
- Claude и Codex читают один canonical source, а tool-specific adapters тонкие;
- LLM agents возвращают строгие структурированные payloads;
- deterministic scripts делают то, что не требует LLM;
- self-improvement идёт через telemetry -> retro -> proposal -> human gate -> propagation.

Создай или спроектируй layout:

```text
.ai/
  handoff.md
  memory/
  tasks/
  changes/
  proposals/
  runs/
  retro/

.claude/
  myengine/config.json
  myengine/extras/
  specs/{backlog,active,done}/

.codex/
  agents/
  skills/

claude-plugins/
  me-dev/
  me-spec/

codex-plugins/
  me-dev/
  me-spec/

scripts/
  me-record-run.*
  me-retro.*
  me-content-validate.*
  me-sim-replay.*
  me-benchmark.*
  me-save-compat.*

docs/agentic/
  PIPELINE.md
  AGENT_CONTRACTS.md
  SELF_IMPROVEMENT.md
```

Минимальные agents:
- `me-architect` - read-only brainstorm, architecture options, ADR proposals.
- `me-engine-developer` - implements engine code within approved SPEC.
- `me-gameplay-designer` - mechanics, loops, balancing hypotheses.
- `me-simulation-reviewer` - deterministic sim, system ordering, replay safety.
- `me-android-performance` - frame time, allocations, lifecycle, input, device constraints.
- `me-content-schema-designer` - schemas, validation, migrations.
- `me-balance-simulator` - runs/defines deterministic balance scenarios and metrics.
- `me-renderer-qa` - camera, scaling, visibility, screenshot/pixel checks where applicable.
- `me-save-compat-reviewer` - save schema and migration safety.
- `me-tester` - unit/property/replay/content/save tests, no production code.
- `me-runner` - deterministic scripts/tests/benchmarks, JSON-only.
- `me-verifier` - final gate: feature visible, tests exist, docs updated, no invariant breaks.
- `me-docs` - updates STATE/ROADMAP/DOCUMENTATION/architecture notes.
- `me-reflect` - reads telemetry and lessons, proposes improvements.
- `me-improve` - applies approved pipeline-level improvements in a separate, minimal change.
- `me-game-spec-author` - creates specs for games built on the engine.

Output contracts:
- Every LLM implementation/review/test agent returns exactly one JSON object, no prose.
- Architect may return one `=== BRAINSTORM ===` block.
- Spec author returns markdown files plus a manifest.
- Runner scripts emit one JSON line.
- On invalid JSON, retry once with "return JSON only"; on second failure stop.

Workflows to define:
1. `--discuss <topic>` - read-only options.
2. `--spec <feature>` - creates one or more SPEC markdown files in backlog.
3. `--feature --next` - implements next approved SPEC.
4. `--bugfix <description>` - regression-first bugfix.
5. `--balance <scenario>` - run deterministic balance simulations and file tuning specs.
6. `--perf <scope>` - benchmarks frame/sim/update budget.
7. `--content-validate` - schema/content validation.
8. `--save-compat` - migration checks.
9. `--reflect` - retro from telemetry.
10. `--improve` / `--improve --drain` - gated prompt/agent/system improvements.

Human gates:
- Spec approval before implementation unless consuming already-approved backlog.
- Architecture ADR approval for major stack/module changes.
- Improvement approval before editing skills/agents/templates.
- Final manual checklist before claiming a feature done.

Deliverables:
- Create/update docs describing this pipeline.
- Create initial skill/agent stubs if appropriate.
- Keep canonical text in one place; generated adapters must be thin.
- Add a clear "how to invoke from Claude" and "how to invoke from Codex" section.

Acceptance:
- A future session can use `/me --feature --next` style workflow without re-explaining the process.
- There is an explicit distinction between project-local lessons and pipeline-level improvements.
- No agent is both code writer and final reviewer for the same change.
```

---

## PROMPT 3 - First Engine Vertical Slice

```text
Ты реализуешь первый vertical slice `MyEngine`. Не строй всю игру. Докажи архитектуру end-to-end.

Сначала прочитай:
- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/API_SKETCH.md`
- `docs/CONTENT_MODEL.md`
- `docs/TESTING_STRATEGY.md`
- `docs/REFERENCE_RESEARCH.md`, если есть
- `.ai/handoff.md`

Перед реализацией сделай targeted reference pass:
- Mindustry: minimal loop "resources -> build defense -> waves attack core";
- OTD / Heavydefense: simplest Android/Desktop LibGDX TD implementation shape;
- King under the Mountain / Mountaincore: save/world/job patterns, если vertical slice затрагивает colony/resource production;
- gdx-liftoff/KTX/Ashley/gdx-ai: project setup, Kotlin ergonomics, ECS/pathfinding choices.
Запиши в implementation notes, какие решения взял из референсов и почему.

Вертикальный срез:
Создать маленький playable Android prototype / sandbox на движке:
- карта 64x64 tile grid;
- terrain: floor, wall, resource node, core/base;
- content pack defines tiles, towers, enemies, resources, wave schedule;
- player can pan/zoom and place a basic tower on valid tiles;
- enemies spawn in waves and pathfind toward core;
- tower targets enemies and damages them;
- at least one resource generator or simple production recipe exists, even if UI debug-only;
- deterministic simulation uses fixed ticks and seed;
- save/load stores map + entities + wave state + resources;
- replay/simulation test can rerun a scenario from seed and commands;
- debug overlay shows tick, entity counts, FPS/sim time, selected tile.

Implementation rules:
- Keep simulation independent of rendering and Android lifecycle.
- Rendering reads immutable/presented state or snapshot; it must not mutate simulation.
- User input becomes commands into simulation.
- Content definitions are external files with schemas/validation.
- Use simple placeholder visuals; art polish is not the goal.
- No multiplayer, no monetization, no complex editor in this slice.

Tests/gates:
- unit tests for tick ordering, RNG determinism, pathfinding edge cases, targeting, wave spawn;
- content validation test for sample content;
- save/load roundtrip test;
- replay determinism test: same seed + commands -> same final hash;
- benchmark or smoke metric for 1000 entities / N ticks if feasible;
- Android launch smoke if environment supports it; otherwise document why not run.

Deliverables:
- code scaffold and modules according to architecture;
- sample content pack;
- sample sandbox game;
- scripts or Gradle tasks for test, content validate, replay test, benchmark;
- update README with run commands;
- update STATE/handoff docs.

Acceptance:
- The prototype can be run or at least built on local machine.
- Tests prove deterministic core logic without Android.
- Adding a new tower/enemy/wave should require content + maybe a small system extension, not rewriting the sandbox.
- The final answer states exactly which commands were run and which could not be run.
```

---

## PROMPT 4 - Self-Improvement Loop

```text
Ты улучшаешь self-improvement subsystem для `MyEngine`.

Цель:
Закрыть loop: observe -> reflect -> propose -> human gate -> apply -> propagate.

Сначала прочитай:
- `docs/agentic/SELF_IMPROVEMENT.md`, если есть
- `.ai/memory/*`
- `.ai/runs/*.jsonl`, если есть
- `.ai/retro/*`, если есть
- `D:\Pet\mobile-pipeline\selfimprove`, если доступен, как референс

Создай/улучши:
1. Telemetry schema:
   - run_id, timestamp, workflow, agent, model, verdict, retries;
   - metrics: tests, lint, replay, content_validate, benchmark, frame_ms, sim_ms, allocations, save_compat;
   - note, failure_cluster, changed_files, spec_id.

2. Record script:
   - append-only JSONL;
   - never blocks pipeline;
   - emits one JSON line with `retro_due`.

3. Retro script:
   - deterministic aggregation, no LLM required;
   - pass rates per agent/workflow;
   - top failure clusters;
   - flaky signals;
   - slowest benchmarks;
   - recurring invariant violations.

4. Reflection prompt/agent:
   - reads latest retro + lessons;
   - proposes minimal changes;
   - never edits automatically;
   - separates project-local lesson vs pipeline-level prompt/agent change vs engine architecture issue.

5. Improvement gate:
   - approved proposals become patches;
   - every change logged once in `.ai/changes/agent-skill-log.md`;
   - canonical source updated first, adapters generated/synced after.

6. Lessons:
   - low-volume, durable, append-mostly;
   - store why-facts, traps, preferences, recurring failures;
   - do not store easily derivable code facts.

Acceptance:
- A failed runner/reviewer/verifier step leaves telemetry.
- A retro can be produced without network or LLM.
- The reflection output is a short list: finding -> evidence -> minimal proposed change -> target file -> expected effect.
- No self-improvement change bypasses a human gate.
```

---

## PROMPT 5 - Spec for a New Game Built on MyEngine

```text
Ты - game spec author for games built on `MyEngine`.

Input:
У меня есть идея новой Android game на базе `MyEngine`:
`<PASTE GAME IDEA HERE>`

Сначала прочитай:
- `AGENTS.md`
- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/ENGINE_CONSTITUTION.md`
- `docs/CONTENT_MODEL.md`
- `docs/API_SKETCH.md`
- `docs/TESTING_STRATEGY.md`
- текущую engine capability matrix, если есть

Задача:
Создать traceable spec bundle для игры, не начиная реализацию.

Интервью:
Задавай вопросы decision-tree стилем:
- сначала core fantasy и session shape;
- затем game loop;
- затем карта/мир;
- затем player actions;
- затем enemies/threats;
- затем economy/logistics;
- затем progression/research/upgrades;
- затем content volume;
- затем mobile UX;
- затем monetization/privacy if relevant;
- затем what is explicitly out of scope.

Вопросы задавай по одному или маленькими связанными группами. Для каждого предлагай recommended default, исходя из engine constraints. Если пользователь говорит "достаточно", фиксируй оставшееся как assumptions.

Spec bundle:
Создай `games/<slug>/spec/`:
- `00_manifest.yaml`
- `product-brief.md`
- `requirements.md` with FR IDs
- `user-stories.md` with US IDs
- `acceptance/*.feature`
- `design.md`
- `content-plan.md`
- `engine-gap-analysis.md`
- `balance-plan.md`
- `android-ux.md`
- `nfr.md`
- `risks.md`
- `traceability.csv`

Engine gap policy:
- Если игра требует возможности, которой нет в engine, не хардкодь workaround в игре.
- Запиши gap как engine backlog SPEC candidate:
  - capability needed;
  - why game needs it;
  - minimal engine-level implementation;
  - tests/gates;
  - priority.

Acceptance:
- Spec отделяет reusable engine work от game-specific content/rules.
- У каждой функциональной идеи есть traceability до acceptance criteria.
- First playable milestone умещается в 1-2 недели работы.
- Android controls and session length are specified, not left vague.
```

---

## PROMPT 6 - Architecture / Prompt Pack Review

```text
Ты - skeptical reviewer. Проведи review `MyEngine` architecture and agentic pipeline.

Сначала прочитай:
- `AGENTS.md`
- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/ENGINE_CONSTITUTION.md`
- `docs/API_SKETCH.md`
- `docs/CONTENT_MODEL.md`
- `docs/TESTING_STRATEGY.md`
- `docs/REFERENCE_RESEARCH.md`
- `docs/agentic/*`
- latest `.ai/handoff.md`
- latest `.ai/retro/*`, если есть

Review stance:
Ищи не стиль, а риски:
- architecture that cannot support multiple games;
- simulation/render coupling;
- nondeterminism hidden in systems;
- Android lifecycle/performance blind spots;
- content schema that will break saves;
- agents with too broad responsibilities;
- missing human gates;
- self-improvement that can mutate itself without evidence;
- tests that prove little;
- roadmap steps that are too large;
- accidental cloning/IP risk from references.
- reference research that is superficial: "looked at Mindustry" is not enough; require concrete adopted/rejected decisions and license notes.

Output:
Findings first, ordered by severity:
- `[P0]` blocks the project direction;
- `[P1]` likely causes rework soon;
- `[P2]` quality/maintainability risk;
- `[P3]` nice-to-have.

For every finding include:
- file/section;
- why it matters;
- concrete fix;
- suggested owner/agent;
- test or gate that would prevent recurrence.

Then give:
- open questions;
- recommended next 3 actions;
- whether current system is ready for vertical slice.
```

---

## PROMPT 7 - Single Mega Prompt (Use Only If Needed)

```text
Ты - senior Android game engine architect, Kotlin/libGDX-style game developer, simulation systems designer, and architect of Claude/Codex agentic development pipelines.

Создай в `D:\Pet\MyEngine` Android-first reusable 2D game engine / framework для нескольких будущих игр: colony sim, tower defense, factory/logistics/base-defense, minimalist mobile strategy. Референсы: RimWorld, Infinitode 2, Mindustry, Minimalist Tower Defense. Не клонируй IP, ассеты, названия или точные правила; извлекай системные качества.

Главный фокус проекта - не только engine, но и self-developing development system: skills/subagents/prompts/scripts для Claude и Codex, аналогично `D:\Pet\mobile-pipeline`, с canonical markdown source, thin adapters, structured JSON contracts, spec/dev/review/test/verify workflows, telemetry, retro, human-gated improvements and propagation.

Сначала исследуй:
- текущий repo;
- `D:\Pet\mobile-pipeline` как процессный референс;
- релевантные docs already present.

Создай staged foundation:
1. Repository docs: README, AGENTS, ARCHITECTURE, ENGINE_CONSTITUTION, ROADMAP.
2. ADR for stack/runtime. Preferred hypothesis: Android shipping + JVM desktop dev harness + Kotlin/Java/libGDX runtime, unless ADR strongly rejects it.
3. Reference research: study Tier A/Tier B/library/agentic repos from `PROMPT_PACK.md`; create `docs/REFERENCE_RESEARCH.md` with `repo -> what to borrow -> what not to borrow -> license risk -> architecture impact`.
4. Engine module plan: core fixed tick, world/tile grid, ECS/components, AI/jobs, logistics, defense/waves/towers, storyteller, content schemas, renderer, Android shell, devtools, sample games.
5. Content model: versioned schemas for tiles/entities/towers/enemies/recipes/waves/incidents/research/localization.
6. Testing strategy: deterministic replay, schema validation, save compatibility, unit/property tests, benchmarks, Android smoke.
7. Agentic pipeline: `/me-spec`, `/me`, agents, commands, JSON contracts, human gates, selfimprove scripts.
8. `.ai` coordination workspace.
9. Roadmap to first vertical slice.

Do not build a huge game in the first pass. Build the repository foundation and only minimal scaffold needed to make the next vertical-slice prompt safe.

Acceptance:
- A fresh Claude/Codex session can read `AGENTS.md` and continue correctly.
- The engine has clear module boundaries and non-goals.
- The agentic pipeline is first-class, not a TODO.
- The next command/prompt to implement the first playable vertical slice is obvious.

Final answer:
- list files created/changed;
- summarize decisions;
- list commands run;
- state next recommended prompt.
```

---

## Source Notes

These prompts were shaped around:
- local `D:\Pet\mobile-pipeline` structure: `AGENTS.md`, `README.md`, `docs/ARCHITECTURE.md`, `docs/SPEC-PIPELINE.md`, `selfimprove/*`, `mp-dev` skill;
- RimWorld official/wikis: colony sim, AI storyteller, moods/needs, world/events/modding;
- Google Play listings for Infinitode 2, Mindustry and Minimalist Tower Defense;
- open-source game references: Mindustry, Unciv, Shattered Pixel Dungeon, King under the Mountain, Mountaincore, OTD, Heavydefense, OpenRA, Warzone 2100, Freeciv, LibColony;
- library/setup references: gdx-liftoff, KTX, Ashley, gdx-ai, Arc;
- agentic-development references: SWE-agent, OpenHands, aider, Claude subagent/skill catalogs;
- libGDX official positioning as a Java/OpenGL ES game framework suitable for Android and rapid iteration;
- Android Game Development Kit frame pacing docs as a reminder to keep Android performance/frame timing explicit.
