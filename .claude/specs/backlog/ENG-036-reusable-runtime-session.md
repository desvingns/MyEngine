id: ENG-036
title: Reusable Android-free runtime and game session API
status: backlog
owner: human
phase: engine
source: MySD foundation gap analysis 2026-07-18 (repository evidence)
requirements:
  - ENG-036-R1
  - ENG-036-R2
  - ENG-036-R3
  - ENG-036-R4
  - ENG-036-R5
acceptance:
  - engine_runtime_tests
  - sandbox_replay_goldens
  - sandbox_save_compat_v1_v7
  - content_validation
  - benchmark
gates:
  - tests
  - replay
  - save_compat
  - content_validate
  - benchmark
  - android_free_static_check

# Context

`SandboxRuntime`, `SandboxSession`, and the lifecycle save orchestration currently live in
`games/sandbox`, while `docs/API_STABILITY.md` marks the runtime surface Experimental. A separate
game cannot depend on `games:sandbox`: doing so would make sandbox game rules, content loading, and
save encoding part of the reusable engine API.

MySD needs an Android-free session seam before any headless vertical slice. The extraction must
preserve accepted sandbox replay hashes and v1-v7 save migrations.

# Requirements

## ENG-036-R1 — Module boundary

Add an Android-free `engine-runtime` module. It may depend on stable engine-core abstractions and
immutable snapshot/save contracts, but not on `android/**`, `desktop/**`, or any `games/**`
module.

## ENG-036-R2 — Public lifecycle API

Provide Experimental public contracts named `GameRuntimeDescriptor` and `GameSession`. The
surface supports:

- immutable descriptor identity, tick rate, content-pack ID/version, and save schema identity;
- `submit(command)` with an explicit accepted/rejected result;
- `step(ticks)` with positive bounded tick count;
- immutable `snapshot()`;
- versioned `save()`;
- `restore(...)` through a descriptor/factory that validates content and save compatibility.

The API must not expose sandbox state, Android Bundle/View types, wall-clock time, renderer-owned
state, or persistence encoding details.

## ENG-036-R3 — Deterministic ownership

The session owns authoritative runtime orchestration, pending command order, next command ID policy
where applicable, stable system ordering, and seed/RNG state required for replay continuity.
Rendering and input can only consume snapshots and submit commands.

## ENG-036-R4 — Sandbox migration

Refactor shared lifecycle/orchestration out of `games/sandbox`. Sandbox remains the owner of
sandbox-specific state, rules, content selection, and codec payload fields, but implements/adapts
the generic session contract. No consumer needs a dependency on `:games:sandbox`.

## ENG-036-R5 — Compatibility and performance

- Existing sandbox scripted scenarios retain their accepted per-tick/final replay hashes.
- Existing v1-v7 sandbox saves restore through their current migrations and round-trip without
  losing pending commands, terminal state, content/map identity, upgrade/targeting state, or
  deterministic continuity.
- Descriptor/content mismatch and unsupported future save versions fail explicitly.
- The same benchmark scenarios report runtime/session overhead and do not regress median simulation
  time by more than 5% without an accepted explanation.

# Content schema

The generic module defines identity/version contracts, not a new universal game-content format.
Concrete games continue to own their versioned content schema. `GameRuntimeDescriptor` references
stable content-pack ID/version values already validated by the concrete loader.

# Deterministic ordering

For one tick: drain eligible commands in the existing stable order, run concrete systems in the
descriptor/session-defined stable order, produce an immutable snapshot, and update replay-hashable
state. Restore reproduces the pending queue and the next deterministic action.

# Save/replay impact

The extraction should not require a sandbox save-version bump when serialized data is unchanged.
If implementation changes persisted shape, add the next version plus v1-v7 migration tests and
document the exact reason. Replay golden changes are blockers unless a separately accepted behavior
change explains them.

# Dependency order

1. Add module and pure contracts.
2. Add contract/unit tests with a tiny fake runtime.
3. Adapt sandbox runtime/session without behavior change.
4. Run replay/save/content gates and benchmark.
5. Update API stability and cookbook/docs.
6. Only then allow MySD to pin the accepted commit.

# Gherkin acceptance

```gherkin
@ENG-036-AC1
Scenario: A game session runs without Android or sandbox dependencies
  Given a tiny deterministic game descriptor and content identity
  When a command is submitted and the session steps one tick
  Then the immutable snapshot reflects the command
  And the engine-runtime classpath contains no Android or games module

@ENG-036-AC2
Scenario: Restore preserves deterministic continuity
  Given the same descriptor, content version, seed, and pending commands
  When one session continues uninterrupted
  And another session is saved and restored before continuing
  Then their per-tick hash trajectories and snapshots are equal

@ENG-036-AC3
Scenario: Sandbox remains compatible after extraction
  Given the accepted sandbox replay scenarios and save fixtures from v1 through v7
  When sandbox uses the reusable session API
  Then replay goldens remain unchanged
  And every supported save migrates and round-trips

@ENG-036-AC4
Scenario: Incompatible restore fails explicitly
  Given a save with a different content identity or unsupported future schema version
  When restore is requested
  Then the session returns a typed incompatibility result
  And no partial runtime is exposed
```

# Split rule

If implementation requires changes in more than three existing modules or roughly twelve
production files, split sandbox adaptation and generic persistence into follow-up cards before code.
