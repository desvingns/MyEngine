# engine-world Contract

Status: Draft  
Owner: world state

## Responsibilities

- Tile coordinates, world bounds, terrain, occupancy, buildability, and spatial queries.
- World serialization boundaries that persistence can consume later.

## Non-Responsibilities

- Rendering tiles, Android input, pathfinding policy, combat rules, or content file parsing.

## Dependencies

- Depends on `engine-core`.
- No Android, desktop, or render backend dependency.

## Public Contracts

- `World`
- `WorldSize`
- `TilePosition`
- `TileView`
- occupancy and buildability query interfaces.

## Test Gates

- Coordinate math tests.
- Bounds and occupancy tests.
- Serialization boundary tests.
- Deterministic mutation tests once mutable world state exists.

