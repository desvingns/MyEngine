# Phase 07 - Entities, Systems, AI Jobs

Status: Planned

## Цель

Ввести entity/system layer and AI job primitives without overbuilding full colony simulation. Нужно подготовить основу для pawns, enemies, towers, projectiles, jobs and path requests.

## Входы

- Phase 05 core
- Phase 06 world/content/save
- `docs/API_SKETCH.md`
- Reference lessons from King under the Mountain, Mountaincore, LibColony, Ashley, gdx-ai

## Work Packages

### 07.1 Entity Identity

- Stable `EntityId`.
- Entity lifecycle.
- Tags/types.
- Serialization boundary.

### 07.2 Components Or Data Records

Минимальный набор:

- position
- health
- movement/path
- inventory placeholder
- tower/attack placeholder
- job actor placeholder

### 07.3 Systems

- System order contract.
- Add/remove entity safety.
- Query mechanism.
- Deterministic iteration.

### 07.4 Path Requests

- Interface for pathfinding.
- Grid path implementation for current map.
- Cache/defer strategy documented.
- Tests for blocked path/no path.

### 07.5 Jobs/Tasks

- Job definition.
- Job assignment.
- Priority.
- Reservation concept.
- Failure reason.
- No full pawn mood/needs yet, just hooks.

### 07.6 Save Integration

- Entities serialized in stable order.
- System transient state handled explicitly.

## Deliverables

- Entity/system layer.
- Pathfinding primitive.
- Job/task primitive.
- Tests and docs.

## Verification

- Deterministic system order test.
- Entity add/remove during tick test.
- Pathfinding edge cases.
- Job assignment priority test.
- Save/load entity roundtrip.

## Acceptance Gates

- System iteration is deterministic.
- Entity save order is stable.
- Jobs are generic enough for colony sim and logistics tasks.
- No Android/render dependency in entity logic.

## Standalone Prompt

```text
Ты выполняешь Phase 07: entities, systems, AI jobs.

Прочитай docs/contracts and reference research for colony/job systems. Реализуй minimal entity/system layer, path request interface, grid pathfinding and generic jobs/tasks. Не реализуй полный RimWorld-like pawn simulation; только reusable primitives.

Run deterministic/entity/path/job/save tests. Update docs and Plane progress.
```

