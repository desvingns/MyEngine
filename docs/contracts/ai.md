# engine-ai Contract

Status: Planned draft  
Owner: jobs, tasks, and path requests

## Responsibilities

- Job queues.
- Task assignment.
- Cancellation and retry rules.
- Path request contracts.
- Deterministic local decision hooks.

## Non-Responsibilities

- Rendering path previews, Android gestures, full behavior-tree dependency choice, or game-specific
  colony rules.

## Dependencies

- Depends on `engine-core`, `engine-world`, and `engine-entities`.
- May later wrap approved pathfinding helpers after ADR-0002 review.
- No Android dependency.

## Public Contracts

- `Job`
- `Task`
- `WorkerCapability`
- `PathRequest`
- assignment result types.

## Test Gates

- Assignment determinism tests.
- Cancellation tests.
- Starvation and priority scenario tests.
- Path request boundary tests.

