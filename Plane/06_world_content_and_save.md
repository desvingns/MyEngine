# Phase 06 - World, Content And Save v1

Status: Planned

## Цель

Создать foundation для data-driven games: tile world, content pack schemas, validation, localization skeleton, save/load v1 and migration boundary.

## Входы

- Phase 05 core runtime
- `docs/CONTENT_MODEL.md`
- `docs/API_SKETCH.md`

## Work Packages

### 06.1 Tile World

- 2D grid world.
- Terrain type.
- Occupancy.
- Buildability.
- Resource nodes.
- Core/base tile concept.
- Bounds and coordinate APIs.

### 06.2 Content Pack Manifest

- Pack id.
- Version.
- Engine compatibility.
- Locale list.
- Dependencies.
- Checksums if useful later.

### 06.3 Schemas

Draft JSON/YAML schemas:

- tiles
- resources
- entities
- towers
- enemies
- recipes
- waves
- incidents
- research
- strings

### 06.4 Validator

- Validate required fields.
- Validate references.
- Validate duplicate ids.
- Validate version.
- Emit actionable errors.

### 06.5 Save v1

- Save metadata.
- Engine version.
- Content pack id/version.
- Seed.
- Tick.
- World tiles.
- Entities placeholder.
- Resources placeholder.
- Command log pointer or embedded minimal log.

### 06.6 Migration Boundary

- Introduce migration interface.
- Add no-op v1 -> v1 migration.
- Test that unknown future version fails clearly.

## Deliverables

- World module.
- Content module.
- Sample content pack.
- Content validation script/task.
- Save/load roundtrip.
- Save compatibility tests.

## Verification

- Content schema tests.
- Reference integrity tests.
- World buildability tests.
- Save/load roundtrip hash test.
- Unknown version failure test.

## Acceptance Gates

- Content packs are external files, not hardcoded Kotlin-only definitions.
- Every content definition has id and version context.
- Save format version exists from day one.
- Validation errors identify file/id/field.

## Standalone Prompt

```text
Ты выполняешь Phase 06: world, content and save v1.

На базе Phase 05 реализуй tile world, content pack schemas, validator, sample content and save/load v1. Keep entities/gameplay minimal; this phase is about world/content/save foundation.

Run schema/content/save tests. Update docs and Plane progress.
```

