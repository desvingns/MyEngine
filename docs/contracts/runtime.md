# Engine Runtime Contract

Status: ENG-036 technically accepted locally; Experimental API; not published
Last updated: 2026-09-23

## Boundary

`engine-runtime` is an Android-free, game-agnostic module that depends only on `engine-core`.
Android, desktop, renderer, and concrete `games/**` modules may consume its public contracts; the
module itself imports none of them.

The API is Experimental. Cross-repository consumers pin an accepted MyEngine commit and keep a
small game-owned adapter around it.

## Public lifecycle

- `GameRuntimeDescriptor<S>` owns immutable runtime, tick-rate, validated content-pack, save-schema,
  stable-system-order, and command-ID-policy identity.
- `GameRuntimeIdentity` compares structurally and holds an unmodifiable defensive copy of the
  declared system order, so equality/hash semantics cannot drift after construction.
- `start(seed)` returns `SessionStartResult<S>`.
- `restore(VersionedGameSave)` returns `SessionRestoreResult<S>` and never exposes a partial session.
- `GameSession<S>` exposes typed submit, step, snapshot, and save results.
- `VersionedGameSave.payload` is opaque to `engine-runtime`; the concrete game owns its codec and
  migrations.

## Deterministic ownership

`DeterministicGameSession<S>` owns:

1. pending commands and their insertion-order snapshot for persistence;
2. `CommandQueue`'s stable eligible-command drain order;
3. the session seed;
4. positive bounded multi-tick requests;
5. one exact concrete advance per authoritative tick;
6. runtime/content/save identity in the outer save envelope.

The current Experimental contract supports `CommandIdPolicy.CALLER_OWNED` only. The game/application
boundary must allocate unique IDs. The generic queue intentionally does not persist consumed-ID
history or reject duplicate IDs; if a caller supplies duplicates, both commands remain accepted and
the complete comparator `(scheduledTick, id, type, actorId, stablePayload)` gives them a deterministic
drain order. Commands submitted at or before the current tick intentionally catch up on the next
tick because the queue drains every command whose `scheduledTick <= nextTick`; this preserves the
existing sandbox behavior and prevents late input from becoming permanently stranded. A future
session-owned allocator would require an explicit allocation API and persisted cursor before it can
be advertised here.

Concrete adapters may explicitly use the protected `drainCommandsAtCurrentTick()` seam for
command-only input boundaries. It drains only already-due commands in canonical order, leaves
future commands pending, and never advances the authoritative clock. The adapter applies the
drained commands immediately under its serialized ownership, without running time-based systems.
This keeps paused controls responsive and prevents rapid input from accelerating simulation.
Terminal sessions do not drain; sandbox does not opt in and its established behavior is unchanged.

`Tick(Long.MAX_VALUE)` is the final representable session tick and therefore a generic terminal
boundary. A restored session at that tick does not call `Tick.next()`, rejects later submissions as
terminal, and reports a zero-advance terminal step instead of overflowing the clock.

Concrete games own authoritative state, deterministic system implementation/order, immutable
snapshot projection, stable hashing, content selection, and payload encoding. Rendering and input
may only consume snapshots and submit commands.

`step()` intentionally does not project a snapshot or calculate a replay hash. `snapshot()` also
does not calculate a hash: projection and replay hashing are independent explicit operations. This
keeps both the simulation and render hot paths free from unrelated O(state) work; callers request
`snapshot()` and/or `stableHash()` only when each value is actually needed.

## Restore compatibility

The generic boundary rejects runtime ID, content-pack ID/version, save-schema ID, and unsupported
schema versions as typed `SessionRestoreResult.Incompatible` values. Malformed concrete payloads
return `SessionRestoreResult.InvalidSave`.

The sandbox adapter wraps its existing text codec inside `VersionedGameSave` without changing the
payload fields or save version. Legacy v1-v7 payload migration remains game-owned. Its
Unit-returning `SandboxSession.submit` facade preserves the old terminal no-op behavior; callers of
the typed runtime API receive `SessionSubmitResult.Rejected` for the same attempt.

## Acceptance status

Per the encompassing engine + game batch decision, no gates ran during implementation. The later
final pass completed 184 engine tests with zero failures, Android assemble, replay, save-compat,
selfcheck, and content validation (2 packs). After the sandbox-only callback helper extraction,
full tests/assemble/installDist and replay/save compatibility passed again. Independent semantic,
API and final evidence reviews accepted ENG-036 locally on 2026-09-23.

Direct static review confirmed Kotlin/JVM configuration, only `:engine-core` as a production
project dependency, core/JDK imports only, and no Android/androidx/AWT/Swing or concrete
Android/desktop/game/render namespace references in `engine-runtime/src/main`.

The controlled post-change paired03 comparison passed its absolute A/A calibration and <=5% A/B
regression gates. Earlier failures, noise evidence, the failed pre-helper paired02 result, and final
source/distribution identities remain in [runtime-benchmark.md](runtime-benchmark.md). The older
one-shot balance report alone is not sufficient evidence for that gate, and headless timing does
not establish Android rendering budgets. Scoped commit/publication and the consumer pin remain
pending; local acceptance does not change the Experimental stability label.
