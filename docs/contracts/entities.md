# engine-entities Contract

Status: Planned draft  
Owner: entity/component/system runtime

## Responsibilities

- Stable entity IDs.
- Component storage boundaries.
- System registration and ordering.
- Entity persistence hooks.

## Non-Responsibilities

- Rendering component storage directly, Android input, content authoring, or hardcoded game rules.

## Dependencies

- Depends on `engine-core` and usually `engine-world`.
- No Android or render backend dependency.

## Public Contracts

- `EntityId`
- component views.
- system registration interfaces.
- entity snapshot and persistence boundaries.

## Test Gates

- Stable ID tests.
- Component lifecycle tests.
- System ordering tests.
- Save boundary tests.

