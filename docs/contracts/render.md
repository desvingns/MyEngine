# engine-render Contract

Status: Planned draft  
Owner: renderer and visual snapshots

## Responsibilities

- Camera math.
- Snapshot rendering.
- Sprite/tile batching boundaries.
- Debug overlays.
- Visual test fixtures.

## Non-Responsibilities

- Authoritative simulation mutation, Android lifecycle ownership, content validation, or save
  migration.

## Dependencies

- Depends on `engine-core`, `engine-world`, and `engine-content`.
- May depend on libGDX render APIs.
- Does not depend on Android application code.

## Public Contracts

- `Renderer`
- `CameraState`
- `RenderSnapshot`
- debug draw interfaces.

## Test Gates

- Camera math tests.
- Snapshot-only mutation tests.
- Screenshot or pixel-smoke tests.
- Asset missing/fallback tests.

