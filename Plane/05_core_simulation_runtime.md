# Phase 05 - Core Simulation Runtime

Status: Planned

## Цель

Реализовать deterministic engine core: fixed-step simulation, seedable RNG, commands, event queue, system ordering, snapshot/replay primitives and state hash.

## Входы

- Phase 02-03 scaffold and contracts
- `docs/API_SKETCH.md`
- `docs/TESTING_STRATEGY.md`

## Work Packages

### 05.1 Time And Tick

- `Tick`
- `TickRate`
- accumulator-independent simulation API
- no rendering dependency
- deterministic ordering

### 05.2 RNG

- Seeded RNG wrapper.
- No direct use of platform random inside simulation.
- Ability to fork named streams if needed.

### 05.3 Command Queue

- Commands have stable type/id/tick.
- Commands are serializable.
- Input becomes commands, not direct state mutation.
- Commands are processed in deterministic order.

### 05.4 Events

- Internal simulation events.
- Event log for debug/replay.
- Distinguish authoritative state vs presentation events.

### 05.5 Replay Primitive

- Initial seed + content version + command stream -> final state hash.
- Simple `ScenarioRunner`.
- Testkit helpers.

### 05.6 State Hash

- Deterministic hash over relevant state.
- Exclude transient render/UI fields.
- Useful failure diffs in tests.

## Deliverables

- Core runtime classes/interfaces.
- Unit tests.
- Replay determinism test.
- Minimal scenario runner.
- Docs updates if API changed.

## Verification

- Unit tests for tick order.
- Unit tests for command ordering.
- RNG reproducibility tests.
- Replay test: same seed + commands -> same hash.
- Negative test: changed command -> different hash.

## Acceptance Gates

- Core module has no Android/libGDX rendering dependency unless ADR explicitly allows tiny runtime-free types.
- All simulation randomness goes through engine RNG.
- Commands are serializable or have documented serialization plan.
- Replay tests pass.

## Standalone Prompt

```text
Ты выполняешь Phase 05: core simulation runtime.

Прочитай docs/API_SKETCH, docs/ARCHITECTURE, docs/TESTING_STRATEGY. Реализуй deterministic fixed-step core: Tick, Simulation, CommandQueue, EventLog, seeded RNG, ScenarioRunner, state hash and tests.

Do not implement world, rendering, Android UI or gameplay systems beyond tiny test fixtures. Keep this phase narrow.

Run tests and update docs/Plane progress.
```

