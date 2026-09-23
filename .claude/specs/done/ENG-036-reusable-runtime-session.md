id: ENG-036
title: Reusable Android-free runtime and game session API
status: done
owner: human
blocked_by: none
technical_acceptance: accepted
delivery_status: merged_main
accepted_at: 2026-09-23
merged_at: 2026-09-24
start_gates:
  - human_start_approval
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

# Implementation close-out (2026-08-09)

- Added the Android-free `engine-runtime` module with `GameRuntimeDescriptor`, `GameSession`,
  `GameRuntimeFactory`, typed restore results, and deterministic `QueuedGameSession` dispatch.
- Adapted `SandboxSession` to generic queue ownership while preserving the existing sandbox save
  text API and direct `SandboxRuntime` JVM compatibility path.
- No sandbox save-schema bump was needed: properties encoding remains v22 and replay goldens are
  unchanged. Focused/full tests, content/replay/save/benchmark gates, Android assemble, projects,
  selfcheck, headless inspect, and diff-check passed.
# Implementation status — 2026-09-16

Implementation is staged as part of the user-requested single engine + game batch, but this card is
not accepted or done until the deferred final gates pass.

- Added Android-free `:engine-runtime` depending only on `:engine-core`.
- Added Experimental `GameRuntimeDescriptor`, `GameSession`, immutable identities/save envelope,
  typed operation results, and `DeterministicGameSession` queue/tick ownership. Runtime identity
  keeps structural value semantics and an unmodifiable defensive system-order copy.
- Kept the implemented ID policy honest and explicit: `CALLER_OWNED` is the only advertised value;
  callers provide unique IDs, while duplicate IDs are accepted and ordered by the complete stable
  comparator rather than requiring unpersisted consumed-ID history. Commands scheduled at or before
  the current tick retain the existing catch-up-on-next-tick behavior.
- Treats `Tick(Long.MAX_VALUE)` as the generic terminal clock boundary, so a valid restored maximum
  tick cannot overflow through `Tick.next()`; the Android sandbox input adapter also ignores input
  at that boundary before allocating a command ID or calculating its scheduled tick.
- Adapted `SandboxRuntime` to the generic deterministic base while retaining sandbox-owned state,
  rules, snapshot projection, and text codec payload.
- Kept `SandboxSession.save(): String` as the compatibility facade used by the current Android
  Bundle seam; its legacy non-negative `step` behavior remains intact while large requests are
  chunked through the bounded generic API. Its Unit-returning terminal `submit` remains a silent
  no-op while direct typed runtime submission reports `Rejected`. `SandboxSaveCodec.SAVE_VERSION`
  remains 7.
- Kept simulation, snapshot projection, and replay hashing as independent explicit operations, so
  neither `step()` nor `snapshot()` adds unrelated O(state) work to the hot path.
- Added generic fake-runtime and sandbox-adapter test sources, including typed mismatch/future-save
  outcomes. No tests, builds, linters, selfchecks, replay/save/content gates, or benchmarks have
  been executed yet, per the encompassing batch instruction.

## Source closure — 2026-09-23

- Reviewed the generic queue/tick API, legacy sandbox facade, metadata handling, and Android MAX
  input guard without executing any test or validator. No additional runtime behavior change was
  needed in this closure pass.
- Added typed restore pending-queue/per-tick continuity and malformed metadata test sources.
- Added a version-neutral Java source benchmark with PowerShell orchestration. It compares identical
  warmed 35-tick canonical/kill session batches against the exact pre-extraction baseline, checks
  every golden, retains raw timing samples, and fails above 5% median regression. No fourth existing
  Gradle module was changed. See `docs/contracts/runtime-benchmark.md`.
- At that source-only checkpoint, execution was deferred until the encompassing implementation
  finished. The later final verification results below supersede that execution status; historical
  source-review evidence remains unchanged.

## Current final verification — 2026-09-23

- Technical acceptance: **accepted locally** by the independent root reviewer after the final
  source/result review. The card remains `active` only because the canonical scoped commit/push
  delivery gate is not fulfilled; this is not a missing technical test. Remote publication is not
  authorized. Resolve the local checkpoint from Git history; public APIs remain Experimental.
- The encompassing implementation completed before gate execution started. Engine full tests passed
  184 tests with zero failures, and Android debug assemble passed; retained log:
  `build/reports/eng036-final-20260923-01.log`.
- Replay passed with canonical `12a65fd2b87593cf` and kill `bb37eefc1903cc77`. Save compatibility,
  selfcheck, and content validation (2 packs) passed; logs are retained under
  `build/reports/eng036-final-20260923-me-*.ps1.log`.
- First warmed comparison against exact baseline `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb` failed:
  canonical +36.74%, kill +26.68%, with correct hashes. Preserve
  `build/reports/eng036-runtime-benchmark-20260923-01.json`; this is not acceptance evidence.
- Second separate-process comparison failed canonical +17.33% (kill -0.58%). Diagnostics, matched
  JFR and identical-code A/A were retained; same-code apparent regressions +108.56% / +130.55%
  demonstrated that this host/method could not resolve 5% reliably. Original failures remain
  failures. An independently reviewed, predeclared isolated-classloader paired method uses fixed
  128 warmup pairs / 101 measured pairs / 8 sessions, own-JVM affinity and balanced pair order.
- Controlled paired02 passed absolute A/A calibration but failed A/B kill +8.500% (canonical
  +4.580% passed). Its raw data and fingerprints remain at
  `build/reports/eng036-paired-20260923-02/report.json`. Paired01 failed only at runner startup,
  before timing. No measurements were discarded or thresholds widened.
- A minimal sandbox-only tower/reward/incident helper extraction preserved tick/system order,
  arithmetic, terminal guard and seed, reducing the compiled callback from 433 to 239 bytecodes.
  Independent semantic review passed. The inlining explanation remains a hypothesis. After this
  source change, full engine tests passed 184/0, Android assemble and installDist passed, and replay
  and save compatibility passed again; logs are `build/reports/eng036-after-helper-20260923-01.log`,
  `eng036-after-helper-{replay,save}-20260923.log` and `eng036-after-helper-sandbox-full-20260923.log`.
- The one prescribed post-change paired03 set passed: A/A -3.440% canonical / +0.596% kill obey the
  absolute <=5% bound; A/B -4.094% / -3.761% obey the unchanged <=5% regression gate. Every golden
  and classloader-isolation check passed. Report
  `build/reports/eng036-paired-20260923-03/report.json` SHA-256:
  `fcba53a30178293fde7b7b726e6c6cffe89406a23e3f8a3191408d8efc71a1f8`. See the full chronological
  ledger and all distribution/source identities in `docs/contracts/runtime-benchmark.md`.
- Direct Android-free check passed: runtime Gradle configuration is Kotlin/JVM and declares only
  `api(project(":engine-core"))` as a production project dependency. `rg -n '^import '
  engine-runtime/src/main` returns only core contracts and `java.util.Collections`; a source scan
  for Android/androidx/AWT/Swing and concrete Android/desktop/game/render namespaces has no matches.
- One coordinated engine-run telemetry event and required retro are recorded. Remaining delivery
  actions are the scoped local checkpoint, exact consumer pin/build, and separately authorized
  publication; resolve current commit/pin state from Git/MySD. No production/test changes were
  made during documentation closeout. Headless timing does not claim Android frame-budget proof.

## Merge into main — 2026-09-24

Two independent ENG-036 implementations existed: the 2026-08-09 main variant (`QueuedGameSession`,
`GameRuntimeFactory`) and the MySD-accepted branch `codex/mysd-spec-foundation` (`1174d21`,
`DeterministicGameSession`, typed identities/results, calibrated paired benchmark). The merge keeps
the branch API, which MySD pins and whose acceptance evidence is recorded above, on top of every
Phase 14+ sandbox system from main (jobs, hauling, belts, status effects, research, meta
progression, procedural maps, save v22):

- `SandboxRuntime` extends `DeterministicGameSession`; main's `stepOne` system order is unchanged
  and runs from `advanceOneTick`. `SandboxDescriptor.SYSTEM_ORDER` now lists main's real order.
- `SandboxSession` keeps the branch facade plus main's `startProcedural`. main's
  `SandboxSessionFactory`/`asGameSession` are superseded by `SandboxDescriptor` start/restore.
- main's queued-session unit test is archived (not deleted) at
  `archive/eng036-main-queued-session/GameRuntimeTest.kt.txt` because its API no longer exists.
- Branch tests updated to main semantics: benchmark fixture goldens equal main's canonical/kill
  replay goldens `e4892bcc18f9d8dc` / `a763da4ac32b15b4`; the save-boundary comparison excludes
  only transient per-tick combat events (not persisted by main's design); a version-downgrade case
  uses `SAVE_VERSION`.
- Merged verification: full `gradlew test` 481 tests / 0 failures; content-validate, sim-replay
  and save-compat pass.
