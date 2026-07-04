# engine-defense Contract

Status: Planned draft  
Owner: waves, towers, enemies, and combat

## Responsibilities

- Wave schedules.
- Tower targeting contracts.
- Enemy combat state.
- Damage, status effects, and rewards.

## Non-Responsibilities

- Campaign design, exact reference-game mechanics, renderer effects, or Android input handling.

## Dependencies

- Depends on `engine-core`, `engine-world`, `engine-entities`, and `engine-logistics`.
- Consumes content definitions from `engine-content`.

## Public Contracts

- `WaveDefinition`
- `TowerDefinition`
- `EnemyDefinition`
- `TargetingRule`
- `DamageEvent`

## Test Gates

- Target ordering tests.
- Wave replay tests.
- Damage and status invariant tests.
- Reward/resource integration tests.

