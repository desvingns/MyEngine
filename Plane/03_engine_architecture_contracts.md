# Phase 03 - Engine Architecture Contracts

Status: Done

## Цель

Зафиксировать архитектуру как набор контрактов до массовой реализации. После этапа должно быть понятно, где живёт каждая подсистема, кто от кого зависит, какие API public, какие internal, и какие тесты нужны для каждого контракта.

## Входы

- Phase 00-02 deliverables
- Build scaffold
- `docs/REFERENCE_RESEARCH.md`
- ADR-0001/0002

## Work Packages

### 03.1 Architecture Document

Создать/обновить `docs/ARCHITECTURE.md`:

- module graph;
- runtime flow;
- simulation tick flow;
- render/input flow;
- content loading flow;
- save/load flow;
- test strategy per module;
- non-goals for v1.

### 03.2 API Sketch

Создать `docs/API_SKETCH.md` with draft interfaces:

- `Engine`
- `Simulation`
- `TickScheduler`
- `Command`
- `World`
- `EntityId`
- `System`
- `ContentRegistry`
- `SaveGame`
- `Renderer`
- `InputAdapter`
- `ScenarioRunner`

### 03.3 Content Model Draft

Создать `docs/CONTENT_MODEL.md`:

- content pack manifest;
- tiles;
- entities;
- towers;
- enemies;
- items/resources;
- recipes;
- waves;
- incidents;
- research/upgrades;
- localization;
- versioning and migrations.

### 03.4 Testing Strategy

Создать `docs/TESTING_STRATEGY.md`:

- unit tests;
- deterministic replay tests;
- content schema tests;
- save compatibility tests;
- simulation property tests;
- benchmark/performance tests;
- Android smoke/device tests;
- visual/screenshot gates.

### 03.5 Module Contract Files

Optional but recommended:

```text
docs/contracts/
  core.md
  world.md
  content.md
  entities.md
  ai.md
  logistics.md
  defense.md
  storyteller.md
  render.md
  android.md
  devtools.md
```

## Acceptance Gates

- Dependency graph has no Android dependency inside simulation modules.
- Every planned module has responsibilities, non-responsibilities and tests.
- API sketch explains stable vs experimental contracts.
- Content model includes version fields.
- Testing strategy includes deterministic replay hash.

## Standalone Prompt

```text
Ты выполняешь Phase 03: engine architecture contracts.

Прочитай AGENTS, README, docs/REFERENCE_RESEARCH, ADRs, current scaffold. Создай/обнови:
- docs/ARCHITECTURE.md
- docs/API_SKETCH.md
- docs/CONTENT_MODEL.md
- docs/TESTING_STRATEGY.md
- docs/contracts/*.md if useful

Не углубляйся в реализацию. Сначала контракты. Для каждого API явно укажи module owner, dependencies, test gates and future extension points.

Update Plane/README progress.
```
