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
- content validation CLI boundary.

## Test Gates

- Scenario replay hash tests.
- CLI validation tests.
- Report snapshot tests.
- Tooling dependency boundary tests.

