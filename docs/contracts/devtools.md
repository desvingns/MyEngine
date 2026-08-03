# engine-devtools Contract

Status: Planned draft  
Owner: development tools

## Responsibilities

- Scenario runner.
- Replay inspector.
- Content validation CLI.
- Balance reports.
- Future editor prototypes.

## Non-Responsibilities

- Shipping Android gameplay code, authoritative runtime shortcuts, or hidden state mutations.

## Dependencies

- Depends on `engine-core`, `engine-content`, and `engine-testkit`.
- May depend on desktop/JVM UI or CLI libraries after ADR-0002 review.
- No Android dependency unless a specific Android smoke tool is isolated.

## Public Contracts

- `ScenarioRunner`
- `ScenarioDefinition`
- `ReplayReport`
- `ProceduralMapReport` for deterministic seeded map metadata and ASCII terrain output.
- content validation CLI boundary.

The `procedural-map [seed]` command (also available as `map-generate`) reports a generated sandbox
map without mutating authoritative runtime state.

## Test Gates

- Scenario replay hash tests.
- CLI validation tests.
- Report snapshot tests.
- Tooling dependency boundary tests.
