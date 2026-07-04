# Phase 08 - Logistics, Defense And Storyteller Slice

Status: Planned

## Цель

Создать первые gameplay systems, которые доказывают жанровой диапазон `MyEngine`: resources/recipes/logistics, waves/enemies/towers, and simple event director. Не игра целиком, а reusable systems.

## Входы

- Phase 05-07 runtime/world/entities
- Content schemas
- Reference research: Mindustry, Infinitode-like TD, OTD, Heavydefense

## Work Packages

### 08.1 Resources And Inventories

- Resource definitions from content.
- Simple inventory component or storage.
- Add/remove/query.
- Capacity optional.

### 08.2 Recipes And Producers

- Recipe content schema.
- Producer system.
- Input/output resources.
- Production time in ticks.
- Deterministic accounting.

### 08.3 Waves

- Wave schedule content.
- Spawn points.
- Enemy composition.
- Wave state.

### 08.4 Enemies

- Enemy definition.
- Health/speed/reward.
- Path to core/base.
- Damage core on arrival.

### 08.5 Towers

- Tower definition.
- Placement validation.
- Range.
- Targeting strategy.
- Damage/cooldown.
- Upgrade hook.

### 08.6 Projectiles Or Hitscan

Start simple:

- hitscan damage or basic projectile.
- Deterministic collision.
- Visual representation can be later.

### 08.7 Storyteller / Incident Director

Minimal:

- incident content definition;
- threat budget;
- pacing curve placeholder;
- deterministic incident selection by seed;
- hooks for future colony events.

## Deliverables

- Logistics systems.
- Defense systems.
- Storyteller minimal system.
- Sample content.
- Scenario tests.

## Verification

- Recipe production tests.
- Resource conservation tests.
- Wave spawn tests.
- Tower targeting tests.
- Enemy path/core damage tests.
- Incident deterministic selection test.
- Replay hash test including systems.

## Acceptance Gates

- Systems are content-driven.
- No hardcoded tower/enemy/resource names except sample content.
- Balance numbers live in content.
- Systems remain deterministic.
- First vertical slice can be assembled from these systems.

## Standalone Prompt

```text
Ты выполняешь Phase 08: logistics, defense and storyteller slice.

Используя Phase 05-07, реализуй reusable systems for resources, recipes, producers, waves, enemies, towers and minimal storyteller. Всё data-driven через content definitions. Keep visuals out unless needed for tests.

Run scenario/replay tests and update Plane progress.
```

