# engine-defense Contract

Status: Accepted (ENG-025 close-out, 2026-08-03)
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
- `DamageTypeContent`
- `ContentRegistry.damageTypes`

## ENG-025 movement and targeting semantics

`EnemyContent.movementMode` is `ground` by default and may be `air`. Ground enemies use the
existing blocker-aware GoalField. Air enemies use a deterministic GoalField that ignores blockers,
so wall placement cannot invalidate their route. `TowerContent.canTargetGround` and
`canTargetAir` default to `true`; content validation requires at least one capability. Target
selection and splash candidates filter by the enemy movement mode before applying the existing
stable priority/entity-id ordering. Balance reports warn when an authored air wave has no
air-capable tower. `SandboxSaveCodec` v21 persists the movement mode; older saves decode it as
ground.

## ENG-011 damage and resistance semantics

ENG-011 uses the approved Option A contract: damage types and percentage resistances are
optional, static content metadata. `damage-types.properties` declares typed damage definitions;
`TowerContent.damageTypeId` points from a tower to one of them, and
`EnemyContent.resists` maps damage-type ids to integer resistance percentages.

For a hit at Manhattan distance `distance`, the authoritative effective-damage formula is:

```text
effectiveDamage = floor(
  baseDamage
  * max(0, 100 - distance * falloffPercent)
  * (100 - resistPercent)
  / 10000
)
```

`baseDamage`, `distance`, `falloffPercent`, and `resistPercent` are integer values;
`resistPercent` is constrained to `0..100`, and an omitted resistance means `0`. The runtime uses
`Long` intermediates and performs one final floor/truncation. Direct hits use `distance=0` and
`falloffPercent=0`. The same formula applies to direct and splash candidates. A result of zero
does not damage health and does not emit a `HitEvent`.

When `damage-types.properties` exists, every tower must declare a non-blank `damageTypeId`; content
validation is bidirectional: tower damage-type references and enemy resistance keys must resolve,
and every declared damage type must be used by at least one tower and one enemy resistance entry.
Legacy packs without the file retain nullable
tower damage types, empty resistance maps, and direct legacy damage behavior.

Damage types and resistances are registry-derived metadata, not dynamic entity state. They are not
serialized; `SandboxSaveCodec.SAVE_VERSION` remains `11`.

The effective-DPS balance matrix is deterministic and reports base towers and upgrade tiers against
all enemies under explicit assumptions: single-target, in-range, no splash, and
`ticks_per_second=20`. Rows sort by tower profile id and then enemy id.

## Test Gates

- Target ordering tests.
- Wave replay tests.
- Damage and status invariant tests.
- Reward/resource integration tests.
- ENG-011 formula, direct/splash resistance, zero-hit, typed-content cross-reference, effective-DPS,
  replay-hash, and save-continuity tests.
