# Phase 10 - First Playable Vertical Slice

Status: Planned

## Цель

Собрать первый playable sandbox, доказывающий end-to-end architecture: content -> world -> simulation -> commands -> rendering -> Android/Desktop -> save/replay/tests.

## Target Experience

Минимальный сценарий:

- карта 64x64;
- terrain: floor, wall, resource node, core/base;
- player can pan/zoom;
- player can select a tower and place it on valid tiles;
- enemies spawn from wave schedule;
- enemies path toward core;
- towers target/damage enemies;
- at least one resource generator or simple recipe exists;
- core loses health if enemy arrives;
- debug overlay shows sim state;
- save/load works;
- replay test reproduces final state hash.

## Work Packages

### 10.1 Sandbox Content Pack

- tiles
- resources
- one generator/recipe
- one tower
- one enemy
- wave schedule
- strings

### 10.2 Sandbox Ruleset

- win/loss placeholder.
- starting resources.
- build cost.
- core health.
- wave start behavior.

### 10.3 UX Loop

- choose/build tower.
- invalid tile feedback.
- basic HUD.
- debug overlay.

### 10.4 Persistence

- save current sandbox.
- load current sandbox.
- preserve tick, wave, resources, entities, core health.

### 10.5 Replay

- scripted scenario:
  - seed;
  - commands;
  - N ticks;
  - expected hash.

### 10.6 Documentation

- README quick start.
- controls.
- known limitations.
- next systems to build.

## Verification

- `test`
- content validate.
- save/load tests.
- replay deterministic test.
- desktop run.
- Android assemble/run if available.
- manual smoke checklist.

## Acceptance Gates

- It is actually playable as a tiny toy, not just code.
- All core systems touched by vertical slice have tests.
- Adding a second tower/enemy should be content-first.
- Known limitations are documented.

## Standalone Prompt

```text
Ты выполняешь Phase 10: first playable vertical slice.

Собери sandbox game on MyEngine: 64x64 map, core, resources, tower placement, waves, enemies, tower damage, save/load, replay and debug overlay. Use placeholder visuals. Keep scope tiny but end-to-end.

Run all available tests/builds, document manual smoke, update README, STATE, Plane progress.
```

