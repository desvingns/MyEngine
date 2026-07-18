# ADR-0004: Composite Build With A Pinned Engine Revision

Status: Accepted
Date: 2026-07-18
Decision id: PROC-002

## Context

Games live in repositories separate from MyEngine. A game must consume engine source locally and
in CI without depending on an unpublished artifact or whichever local `main` happens to contain.
MyEngine also distinguishes Stable, Experimental, and Internal APIs in
`docs/API_STABILITY.md`, but previously had no cross-repository compatibility rule.

The first concrete consumer is MySD. It needs engine changes to land independently while keeping
every game revision reproducible and reviewable.

## Options Considered

### Gradle composite build without a revision lock

This gives fast local source navigation and project substitution, but two machines can build the
same game commit against different MyEngine states. CI cannot reproduce a local failure reliably.

### Publish every engine change to a local/remote Maven repository

Published artifacts provide conventional dependency metadata and immutable versions, but introduce
release infrastructure, credentials or local-publish steps before the engine has a stable release
cadence. Source-level debugging and coordinated early API work become slower.

### Gradle composite build plus exact engine commit lock

The game declares normal `dev.myengine:<module>:<version>` dependencies and includes a checked-out
MyEngine build. A repository lock file records the engine repository and full commit SHA. Local
builds resolve a configured checkout; CI checks out the exact SHA before running the same Gradle
tasks.

## Decision

Adopt the composite build plus exact commit lock.

- Each game commits a machine-readable engine lock with repository URL, full commit SHA, engine
  version, and compatibility policy.
- Local builds accept an explicit Gradle property/environment path and may default to a documented
  sibling checkout. The local checkout must resolve to the locked commit before a release or fit
  verdict; day-to-day engine development may use a different checkout only when the diff is
  explicit.
- CI reads the lock, checks out MyEngine at that exact SHA inside the game workspace, verifies
  `rev-parse HEAD`, and runs the same tests/assemble tasks through the composite build.
- A game updates the lock only to an accepted, pushed MyEngine commit whose relevant unit,
  replay, save-compatibility, content, benchmark, and Android gates have passed.
- Stable APIs may be consumed directly according to `docs/API_STABILITY.md`.
- Experimental APIs require a small consumer-owned adapter, an explicit usage note, and a pinned
  commit. Breaking Experimental changes require coordinated adapter and lock updates but do not
  receive semver compatibility guarantees.
- Internal APIs are not cross-repository dependencies.
- MyEngine tags mark reviewed Stable API compatibility checkpoints, not every Experimental feature
  commit. A game release tag contains its lock and therefore the exact MyEngine SHA.

## Compatibility Workflow

1. Implement and accept a MyEngine change through the canonical engine pipeline.
2. Push the accepted engine commit.
3. Update the consumer lock and any Experimental adapter.
4. Run consumer unit/replay/save/content/Android gates.
5. Review the lock diff together with behavior and migration impact.
6. Commit the consumer update.

If MyEngine later has multiple stable consumers and artifact publication reduces real build cost,
revisit Maven publication in a new ADR. Do not silently combine both mechanisms.

## Consequences

- Game commits are reproducible without publishing early engine artifacts.
- Local source debugging remains direct.
- Cross-repository changes require two intentional commits and a lock update.
- A stale/unavailable SHA fails early instead of falling back to a different engine.
- Experimental API risk is visible at the game adapter boundary.
