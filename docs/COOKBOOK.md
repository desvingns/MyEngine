# MyEngine Cookbook

This is an on-demand task guide for common engine changes. It is intentionally not part of the
always-loaded intake set: read the recipe that matches the change, then confirm the active spec,
the current save version, and the current module contract before editing.

## Shared rules

- Keep authoritative simulation Android-free. Android may consume snapshots and submit commands;
  it must not own simulation state.
- Keep authored values in content packs. Loader validation, schema documentation, and at least one
  real-pack fixture must move together.
- Keep deterministic ordering explicit: sort ids, use stable tie-breakers, and do not use wall-clock
  values or unordered map iteration in authoritative state.
- Any persisted field needs a version bump, migrations from every supported prior version, and a
  save-compatibility fixture/matrix entry.
- Add focused tests before broad gates. Do not weaken or delete existing assertions.

## Recipe 1 — Add a tower type

Use this when a new tower changes authored stats or behavior but should use the existing command,
entity, targeting, and render boundaries.

Why: tower identity and balance belong to content; the runtime should consume a validated definition
instead of branching on a hardcoded id. A bespoke runtime branch is only justified by a new mechanic,
which should be a separately scoped feature.

### Files to inspect or change

- `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt` — definition fields and
  defaults.
- `engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt` — parsing and cross-reference
  validation.
- `engine-content/src/test/kotlin/dev/myengine/content/ContentPackLoaderTest.kt` — valid/invalid
  content coverage.
- `engine-defense/src/main/kotlin/dev/myengine/defense/DefenseRuntime.kt` — only if the tower needs a
  new generic behavior hook; keep inventory mutation outside this module.
- `engine-defense/src/test/kotlin/dev/myengine/defense/DefenseRuntimeTest.kt` — deterministic combat
  or reward behavior.
- `games/sandbox/content/sandbox/towers.properties` — authored tower and tier values.
- `games/sandbox/content/sandbox/strings.properties` — user-facing id/name strings when needed.
- `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt` — command/runtime wiring
  only when the generic path cannot consume the definition.
- `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxTowerCostGatingTest.kt` and the
  narrowest relevant sandbox test — placement, upgrade, targeting, or replay behavior.

### Procedure and gates

1. Add the definition and pack values with stable ids; do not duplicate a runtime constant.
2. Add loader validation and a test for both the new valid reference and the relevant invalid case.
3. Reuse the generic command/entity/targeting path. Add a behavior hook only if the mechanic is not
   representable by existing data.
4. Run `.\gradlew.bat :engine-content:test :engine-defense:test :games:sandbox:test`.
5. Run `powershell.exe -File scripts\me-content-validate.ps1` and
   `powershell.exe -File scripts\me-sim-replay.ps1`.
6. If the tower changes saves or snapshots, also run the applicable save-compat and renderer gates.

### Historical validation

Commit `492fb03` (`Initial commit`) demonstrates the real base path: it introduced `TowerContent`,
the tower parser and loader tests, `games/sandbox/content/sandbox/towers.properties`, the sandbox
runtime, and tower-cost tests together. Treat the commit as a file-list example, not as code or
asset to copy.

## Recipe 2 — Add a content field

Use this when an authored property is missing from a validated content definition.

Why: a field is a contract, not just a parser edit. Keeping definition, loader, schema, fixture,
and validation test in one change prevents packs from silently drifting from the documented format.

### Files to inspect or change

- `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt` — typed field and
  compatibility default, if the field is optional.
- `engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt` — parse, range checks,
  required/optional semantics, and cross-references.
- `engine-content/src/test/kotlin/dev/myengine/content/ContentPackLoaderTest.kt` — valid value,
  default behavior, and invalid-value diagnostics.
- `docs/content-schemas/PROPERTIES_SCHEMA.md` — key, type, default, range, and example.
- `games/sandbox/content/sandbox/waves.properties` — real-pack value for the canonical
  `WaveContent.spawnSelection` field example.
- `games/sandbox/src/test/resources/content-fixtures/multi-spawn/waves.properties` — minimal
  checked-in fixture value for the same field.
- `games/signal-garden/content/signal-garden/towers.properties` — second real pack when a shared
  tower field is being added; omit it for a sandbox-only field.

### Procedure and gates

1. Decide whether the field is required or optional and document the compatibility default.
2. Parse it once into the typed definition; validate bounds and references at load time.
3. Put at least one valid value and one invalid-value test in the loader suite. Keep fixture ids
   stable and avoid copying values from reference games.
4. Run `.\gradlew.bat :engine-content:test :engine-devtools:test`.
5. Run `powershell.exe -File scripts\me-content-validate.ps1`; it must validate every discovered
   game pack, not only the sandbox pack.
6. If the field affects authoritative state, run replay and the relevant simulation tests.

### Historical validation

Commit `3fceabf` (`feat(engine): add per-wave multi-spawn routing`) demonstrates the complete
content-field pattern: it added `WaveContent.spawnSelection`, loader parsing and cross-reference
validation, `docs/content-schemas/PROPERTIES_SCHEMA.md`, loader tests, and the checked-in fixture
pack under `games/sandbox/src/test/resources/content-fixtures/multi-spawn/`. Use the same
definition-loader-schema-fixture-test chain for new fields.

## Recipe 3 — Add a save field

Use this when authoritative state must survive save/load.

Why: adding a property to the current serializer is not backward-compatible by itself. The codec
version and migration matrix make the state transition explicit and keep old saves replay-safe.

### Files to inspect or change

- `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt` — authoritative codec,
  `SAVE_VERSION`, encode/decode, and migration defaults.
- `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt` — lifecycle/save
  wiring when the public session path needs an update.
- `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSaveMigrationMatrixTest.kt` —
  every supported version, round-trip, and future-version rejection.
- `.claude/specs/done/PROC-007-save-migration-matrix.md` — matrix acceptance and stable-hash
  contract; inspect it, do not edit it for a feature field.
- `games/sandbox/src/test/resources/save-fixtures/v21.properties` — baseline fixture retained for
  the current `SAVE_VERSION=22` migration boundary.
- `games/sandbox/src/test/resources/save-fixtures/v22.properties` — newest checked-in fixture after
  bumping the codec to v22.
- `scripts/me-save-compat.ps1` — only when the supported-version matrix or gate invocation changes.
- `android/src/test/kotlin/dev/myengine/android/FixedTickFrameLoopTest.kt` — update only if an
  Android-facing save-version assertion is intentionally part of the contract.

### Procedure and gates

1. Bump `SAVE_VERSION` exactly once and write the new field in a stable order.
2. Decode every supported older version with a documented default; never infer missing state from
   wall-clock time or unordered data.
3. Add the newest fixture and extend the PROC-007 migration matrix. Preserve stable hashes when the
   new state is absent from legacy saves; the gate must report `matrix=pass`.
4. Run `.\gradlew.bat :games:sandbox:test`.
5. Run `powershell.exe -File scripts\me-save-compat.ps1` and
   `powershell.exe -File scripts\me-sim-replay.ps1`.
6. Run `.\gradlew.bat test` and `.\gradlew.bat projects`, plus
   `.\gradlew.bat :android:assembleDebug` when Android save wiring changed.

### Historical validation

Commit `d281fed` (`feat(engine): add player-placed wall blockers`) demonstrates a save-field rollout:
it changed `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`,
`games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSaveMigrationMatrixTest.kt`,
added `games/sandbox/src/test/resources/save-fixtures/v12.properties`, and updated
`scripts/me-save-compat.ps1`. Follow the same versioned migration and fixture pattern; do not copy
the feature's schema or values.

## Recipe 4 — Add a tick-loop system

Use this when a simulation capability needs deterministic per-tick execution.

Why: the fixed-tick loop is the authoritative ordering boundary. A system should be Android-free,
receive state through explicit inputs, and run in a documented position relative to command drain,
movement, combat, persistence, and terminal evaluation.

### Files to inspect or change

- `engine-ai/src/main/kotlin/dev/myengine/ai/JobExecutionSystem.kt` — pure/system-owned behavior
  and stable iteration order; use the same module boundary for an AI-domain system.
- `engine-ai/src/test/kotlin/dev/myengine/ai/JobExecutionSystemTest.kt` — ordering, edge cases,
  and deterministic repeated-run coverage.
- `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt` — construct the system
  and call it at the approved fixed-tick boundary.
- `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxJobExecutionTest.kt` — end-to-end
  state transition and replay coverage.
- `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSaveMigrationMatrixTest.kt` —
  only when the system introduces persisted state.
- `android/src/test/kotlin/dev/myengine/android/FixedTickFrameLoopTest.kt` — only for Android loop
  integration, never for authoritative system logic.

### Procedure and gates

1. Define the system inputs/outputs and its exact order in `SandboxGame.step`.
2. Iterate entities/jobs/ids in stable order and avoid presentation callbacks or wall-clock reads.
3. Add unit tests for ordering and no-op/invalid cases, then an end-to-end sandbox test.
4. Add replay and save coverage when the system changes authoritative state.
5. Run `.\gradlew.bat :engine-ai:test :games:sandbox:test`, then
   `powershell.exe -File scripts\me-sim-replay.ps1` and the applicable save/content gates.

### Historical validation

Commit `e0c600e` (`feat: wire job execution into tick loop`) demonstrates the pattern: it added
`engine-ai/src/main/kotlin/dev/myengine/ai/JobExecutionSystem.kt`, updated `Jobs.kt`, added
`JobExecutionSystemTest.kt`, wired `SandboxGame.kt`, and added `SandboxJobExecutionTest.kt`. The
system remained Android-free while the sandbox owned composition and fixed-tick placement.

## Recipe 5 — Add a snapshot field

Use this when a renderer, HUD, devtool, or other presentation consumer needs new state.

Why: snapshots are the one-way presentation boundary. Add the field to immutable projection data;
do not let a renderer reach into mutable simulation state or make the Android layer authoritative.

### Files to inspect or change

- `engine-render/src/main/kotlin/dev/myengine/render/RenderModel.kt` — immutable snapshot/render
  DTO, stable ordering, and defensive copies where collections cross the boundary.
- `engine-render/src/test/kotlin/dev/myengine/render/RenderBoundaryTest.kt` — immutability and
  Android-free boundary assertions.
- `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt` — project authoritative
  state into the field in deterministic order.
- `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxHudSnapshotTest.kt` — exact
  values, ordering, and repeated-run determinism.
- `android/src/main/kotlin/dev/myengine/android/SandboxRenderView.kt` — consume the field only when
  Android UI/rendering needs it.
- `android/src/test/kotlin/dev/myengine/android/SandboxHudLayoutModelTest.kt` — Android consumer
  coverage when the view/layout behavior changes.

### Procedure and gates

1. Add the smallest immutable field and define empty/legacy behavior.
2. Project it from authoritative state; sort collections and copy mutable inputs.
3. Keep input as commands and keep rendering read-only. Do not add simulation logic to the view.
4. Run `.\gradlew.bat :engine-render:test :games:sandbox:test` and Android tests when the Android
   consumer changes.
5. Run `.\gradlew.bat :android:assembleDebug`, `powershell.exe -File scripts\me-sim-replay.ps1`,
   and `git diff --check`.

### Historical validation

Commit `270e667` (`feat: add HUD snapshot and UI commands`) demonstrates the field path: it changed
`engine-render/src/main/kotlin/dev/myengine/render/RenderModel.kt`,
`engine-render/src/test/kotlin/dev/myengine/render/RenderBoundaryTest.kt`,
`games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`,
`games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxHudSnapshotTest.kt`, and the
Android `SandboxRenderView.kt` plus its layout test.

## Recipe 6 — Create a new game with the scaffolder

Use this when a new game needs a safe repository entry point with a module, a minimal data-driven
pack, a deterministic replay example, and the initial spec bundle.

Why: the scaffolder makes the game/module/spec boundary explicit from the first generated commit.
It provides a runnable deterministic starter without claiming gameplay, engine, save, or Android
behavior for the new game.

### Invocation

From the repository root, run:

```powershell
powershell.exe -NoProfile -File scripts\me-new-game.ps1 -Slug <lower-kebab-slug>
```

Use `-Root <repo-root>` only when the target repository root is intentionally supplied; the
generator defaults to the repository containing the script.

### Exact files and generated artifacts

The invocation updates only the root `settings.gradle.kts` wiring and creates exactly these 28
files under `games/<slug>` (hyphens in `<slug>` become underscores in the Kotlin package):

- `build.gradle.kts`
- `README.md`
- `replay-scenario.properties`
- `src/main/kotlin/dev/myengine/games/<slug_with_underscores>/CanonicalScenario.kt`
- `src/test/kotlin/dev/myengine/games/<slug_with_underscores>/CanonicalScenarioTest.kt`
- `content/<slug>/manifest.properties`
- `content/<slug>/tiles.properties`
- `content/<slug>/resources.properties`
- `content/<slug>/recipes.properties`
- `content/<slug>/towers.properties`
- `content/<slug>/enemies.properties`
- `content/<slug>/waves.properties`
- `content/<slug>/incidents.properties`
- `content/<slug>/strings.properties`
- `content/<slug>/maps.json`
- `spec/00_manifest.yaml`
- `spec/product-brief.md`
- `spec/requirements.md`
- `spec/user-stories.md`
- `spec/acceptance/AC-001.feature`
- `spec/design.md`
- `spec/content-plan.md`
- `spec/engine-gap-analysis.md`
- `spec/balance-plan.md`
- `spec/android-ux.md`
- `spec/nfr.md`
- `spec/risks.md`
- `spec/traceability.csv`

The generated module contains a JVM test with a 12-tick seeded canonical scenario and expected
replay hash `b8a9908f5d7a8281`; the pack contains the minimal manifest, flat content definitions,
and one canonical JSON map. The spec directory follows `docs/GAME_SPEC_PIPELINE.md` and starts as
draft scaffolding, not as game-bundle approval or traceability evidence.

### Idempotence, path, and encoding rules

1. `<slug>` must match `^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$`: lower-kebab-case, starting with a
   letter, with no uppercase, underscore, empty segment, or repeated hyphen.
2. The generator refuses an existing `games/<slug>`, an existing `games/.<slug>.scaffold`, or
   existing `:games:<slug>` / `projectDir` wiring. It never overwrites an existing game.
3. All game and staging destinations are resolved through containment checks under the selected
   repository root and its `games` directory. Files are written into `games/.<slug>.scaffold`
   first and moved into `games/<slug>` only after generation succeeds.
4. Generated files and the rewritten settings file use UTF-8 without BOM and end with one newline;
   existing settings newline style is retained. Template placeholders are replaced only for the
   requested slug and Kotlin package name.

### Procedure and gates

1. Run the script contract test; it uses an isolated temporary fixture and checks settings wiring,
   all 28 generated files, existing-slug refusal, and invalid-slug refusal:
   `powershell.exe -NoProfile -File scripts\tests\me-new-game.tests.ps1`.
2. Generate a real `<slug>` with the invocation above, then run the generated module test:
   `.\gradlew.bat :games:<slug>:test`.
3. Run `powershell.exe -File scripts\me-content-validate.ps1` to validate discovered content packs.
4. Run `powershell.exe -File scripts\me-sim-replay.ps1` to run the engine replay and generated
   canonical scenarios discovered from `replay-scenario.properties`.
5. Run `.\gradlew.bat projects` to verify the new module is part of the Gradle project graph.

### Historical validation (current DX-001 implementation)

The current `scripts/me-new-game.ps1` plus `scripts/tests/me-new-game.tests.ps1` is the historical
validation for this recipe: the implementation establishes the exact file list, canonical hash,
settings wiring, refusal/idempotence, path containment, and UTF-8-without-BOM contract described
above. Keep this recipe synchronized with that implementation; no reference-game bundle or copied
schema is part of DX-001.

## Close-out checklist

- Confirm the recipe's exact file list matches the actual diff; keep unrelated files unstaged.
- Run the narrowest focused tests and all gates required by the changed boundaries.
- Ask for a human gate only when scope, balance, API, ADR, or another documented decision is
  ambiguous; documentation-only cookbook maintenance needs no new ADR by itself.
- Update `STATE.md`, `.ai/handoff.md`, `Plane/README.md`, `.ai/DIGEST.md`, and
  `.ai/changes/agent-skill-log.md` at close-out. Record the historical commit used for the recipe.
