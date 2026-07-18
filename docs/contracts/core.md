# engine-core Contract

Status: Draft  
Owner: core runtime

## Responsibilities

- Fixed-step tick scheduling.
- Command queue and command ordering.
- Seeded RNG services.
- Stable system ordering.
- Replay hash inputs.
- Shared lifecycle/outcome value types for deterministic runs (`RunState`, `RunStatus`,
  `TerminalReason`, and immutable `RunSummary`).
- Shared value types that do not belong to a narrower module.

## Non-Responsibilities

- Android lifecycle, rendering, input widgets, save locations, content parsing details, or game
  balance.

## Dependencies

- No Android SDK dependency.
- No render backend dependency.
- May depend only on Kotlin/JVM standard libraries and approved deterministic utilities.

## Public Contracts

- `Engine`
- `Simulation`
- `TickScheduler`
- `Command`
- `EngineSystem`
- `EntityId` until `engine-entities` owns it.
- Run outcome types: an active run has no terminal reason/tick/frozen summary; terminal runs carry
  all three. `RunSummary` resource totals are immutable and stably sorted before hashing.

## Test Gates

- Fixed tick tests.
- Command ordering tests.
- RNG repeatability tests.
- Replay hash drift tests.
- Terminal-state determinism tests.
