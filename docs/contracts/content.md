# engine-content Contract

Status: Draft  
Owner: content model and validation

## Responsibilities

- Content pack manifests.
- Schema versioning.
- Definition loading boundaries.
- Validation and cross-reference checks.
- Localization key checks.
- Content migrations.

## Non-Responsibilities

- Renderer asset lifetime, Android resource packaging, gameplay simulation ownership, or exact
  game-specific balance decisions.

## Dependencies

- Depends on `engine-core`.
- May expose data to world, entities, logistics, defense, render, and games.
- No Android dependency.

## Public Contracts

- `ContentRegistry`
- `ContentDefinition`
- `ContentId`
- `ContentPackRef`
- validation result types.

## Test Gates

- Valid pack test.
- Invalid pack rejection tests.
- Cross-reference tests.
- Localization completeness tests.
- Migration fixture tests.

