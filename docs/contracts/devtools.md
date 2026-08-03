# engine-devtools Contract

Status: Active draft
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
- `HeadlessStateInspector` and `HeadlessScenarioFactory` for game-owned, bounded inspection.
- content validation CLI boundary.

The `procedural-map [seed]` command (also available as `map-generate`) reports a generated sandbox
map without mutating authoritative runtime state.

The `inspect` command (aliases `state-inspect` and `headless-inspect`) runs a registered factory to
tick N and emits one deterministic JSON object containing an ASCII frame and a state dump with
entities, inventories, defense metrics, and stable hash. The short form is `inspect 35`; the generic
form is `inspect <factory> <scenario> <pack> <ticks> [script] [seed]`. Registered adapters own
command-script parsing and authoritative state access; the inspector excludes wall-clock fields so
identical arguments produce byte-identical output.

## Test Gates

- Scenario replay hash tests.
- CLI validation tests.
- Report snapshot tests.
- Tooling dependency boundary tests.
