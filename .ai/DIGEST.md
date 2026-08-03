# MyEngine Intake Digest

Regenerated at close-out. Last updated: 2026-08-03 (DX-005 + PROC-006 close-out).

## Current next action

DX-005 + PROC-006 (schema drift and pre-push lane) are complete. Review the remaining accepted backlog before the next
`/me --feature --next` run.

## Active specs / roadmap

- DX-002, DX-005, DX-006, ENG-001, ENG-003, ENG-031, ENG-004, ENG-032, ENG-033, ENG-006, ENG-017, and PROC-006 are done.
- ENG-003 is a post-Phase-14/Phase-15 feature close-out; no new phase was created.
- ENG-002 remains the wave-enemy GoalField path; ENG-003 is the deterministic JobBoard/job-actor tick capability.
- ENG-033's authored scope is implemented with no game-bundle traceability update. MySD TD Gate 1 is
  accepted for its TD reference inventory but is not treated as evidence for colony behavior.
- No ADR or plugin/skill/pipeline contract change was needed.

## DX-005 + PROC-006 close-out

### DONE

- `me-schema-docs-drift.ps1` compares ContentLoader property keys with
  `PROPERTIES_SCHEMA.md` and reports both drift directions as one JSON line.
- Fixtures cover code-not-doc, doc-not-code, aligned input, missing input, and malformed-output
  boundaries. Selfcheck invokes the drift gate.
- `.githooks/pre-push` delegates to `me-pre-push.ps1`, which aggregates drift, fixtures, tests,
  content validation, replay, and save compatibility and blocks on any failed gate.

### VERIFICATION

- Pre-push, selfcheck, benchmark (`sim_ms=15.9066`), projects, Android `assembleDebug`, and
  `git diff --check` passed;
  the initial runner Gradle attempt lacked `JAVA_HOME`, then passed with Android Studio JBR.
- The known untracked `.ai/retro/retro-2026-08-03.md` baseline remains excluded from the feature.

## DX-006 close-out

### DONE

- `docs/COOKBOOK.md` contains five on-demand recipes with exact file lists, required gates, and
  historical commit references for tower, content, save, tick-loop, and snapshot work.
- `AGENTS.md` points to the cookbook after intake without loading it into the always-read context;
  the card is in `.claude/specs/done/` and the roadmap is synchronized.

### VERIFICATION

- Selfcheck, full tests/projects, content validation, replay, save-compat, benchmark, Android
  `assembleDebug`, and `git diff --check` passed. The first Gradle test call lacked a valid
  `JAVA_HOME`; the confirmation run used Android Studio JBR and passed.
- Documentation-only boundary review found no production, save, Android, or plugin changes. The
  known untracked retro baseline remains excluded.

## DX-002 close-out

### DONE

- Provider-based `HeadlessStateInspector` emits deterministic ASCII plus JSON state with entities,
  inventories, defense metrics, and stable hash. Sandbox registration uses `ServiceLoader`; CLI
  supports short and generic forms plus optional command scripts.

### VERIFICATION

- Focused inspector tests and confirmed full Gradle tests/projects, content validation, replay,
  save-compat, benchmark, selfcheck, Android assembleDebug, and diff-check passed. The initial
  full-test invocation had a transient PowerShell `RemoteException`; confirmation passed.
- Verifier worker timed out; local boundary review found no blocker. No device/emulator or visual proof
  is claimed.

## ENG-017 close-out

### DONE

- Optional DX-008 `tech-tree.json` graph content is validated for resource, prerequisite DAG, and
  typed tower/building/recipe unlock references. Research spending is deterministic and atomic;
  unlocks gate in-sim availability and sorted immutable snapshot state is exposed.
- `SandboxSaveCodec` v18 persists researched ids and pending research commands; v1-v17 migrate with
  empty research state.

### DECISIONS

- No ADR or human gate was needed. No plugin, skill, or pipeline contract changed; unreferenced
  definitions remain available for compatibility.

### NEXT

- Review the remaining accepted backlog and run `/me --feature --next` for the next selected feature;
  no new phase is planned.

### BLOCKERS

- No implementation blocker. No device/emulator or visual-golden proof is claimed beyond
  `assembleDebug`. The generated untracked `.ai/retro/retro-2026-08-03.md` is a baseline artifact
  and is not part of ENG-017 feature staging.

### VERIFICATION

- Focused content/research/gating/snapshot/replay/save tests, full tests/projects, content validation,
  replay, save-compat, benchmark, selfcheck, Android `assembleDebug`, and `git diff --check` passed.

## ENG-006 close-out

### DONE

- Added seeded procedural generation from validated content parameters, bounded route-safe retries,
  a deterministic corridor fallback, a seed-preserving sandbox session, and a devtools JSON/ASCII
  report. Existing `MapContent` and save identity contracts remain in use.

### VERIFICATION

- Focused generator/sandbox/devtools tests, full test/projects, content validation, replay,
  save-compat, benchmark, selfcheck, Android assembleDebug, and diff-check passed.

## ENG-033 close-out

### DONE

- Added optional `NeedContent`/`needs.properties`, `NeedsComponent`, deterministic `NeedsSystem`, typed
  target-owned `NeedRecovery` effects, and immutable sorted HUD need bars.
- `SandboxSaveCodec` v17 persists need levels and threshold-cycle counters; v1-v16 migrate with empty
  needs state. Legacy canonical replay hashes remain unchanged without needs entities.

### VERIFICATION

- Full test/projects/content validation/replay/save-compat/benchmark/selfcheck, Android assembleDebug,
  focused needs/content/sandbox tests, and diff-check passed. Conditional roster reviewers timed out;
  local boundary review found no blocker. Replay hashes: `e4892bcc18f9d8dc` / `a763da4ac32b15b4` /
  `3f02607020d48668`.

## ENG-003 close-out

### DONE

- Android-free `JobExecutionSystem` is wired into the sandbox fixed-tick pipeline.
- Positioned `JobActorComponent` entities claim jobs in deterministic worker/entity and priority/job-id order.
- Lifecycle advances through `CLAIMED -> IN_PROGRESS -> DONE/FAILED`; pathfinding movement, work ticks,
  typed `resource_delta` completion effects, invalid-target/no-path release, reservation guards, and
  same-tick reclaim prevention are covered.
- `SandboxSaveCodec` v13 persists JobBoard, in-flight jobs, actor assignment/progress, and effects;
  v12 migration produces empty job state.

### DECISIONS

- No ADR, game-bundle traceability update, or plugin/skill/pipeline contract change.
- Approved defaults: every positioned `JobActorComponent` is eligible for all job types, one work tick
  per simulation tick, in-world `TilePosition` validation, and deterministic release to `OPEN` for
  invalid/no-path jobs.

### NEXT

- Run `/me --feature --next` for ENG-031 (stockpile zones + designations).

### BLOCKERS

- No implementation blocker. Non-blocking follow-ups: two-worker replay is not in
  `DevtoolReports.replayInspect`; `scripts/me-save-compat.ps1` does not separately invoke
  `SandboxJobExecutionTest`; no job-heavy benchmark covers large worker/job counts or invalidated paths.

### VERIFICATION

- Selfcheck, full test/projects/content validation/replay/save-compat/benchmark, `:android:assembleDebug`,
  and `git diff --check` passed.
- Focused `JobExecutionSystemTest` and `SandboxJobExecutionTest` passed.
- Replay: `e4892bcc18f9d8dc` / `a763da4ac32b15b4` / `3f02607020d48668`.
- Final benchmark: simulation 430 ms; kill 76 ms; spatial index 6.6127 ms.
- Final verifier passed with all boundary checks true.

## ENG-031 close-out

### DONE

- Accepted Option A adds deterministic zone commands/store state, validated stockpile resource
  filters, one-shot harvest-node JobBoard designation jobs, immutable `EngineSnapshot.zones`, and
  `SandboxSaveCodec` v14 with v1-v13 migration.

### DECISIONS

- No ADR or game-bundle traceability update. Colony demand remains vision-only. Hauling, stockpile
  quantities/capacity, depletion/repeated harvest, and actual Android overlay consumption are
  deferred to follow-up scope; ENG-004 now delivers source-to-stockpile hauling. No plugin/skill/pipeline contract changed.

### NEXT

- Run `/me --feature --next` for ENG-032.

### BLOCKERS

- No implementation blocker. Follow-ups are non-blocking: claimed/in-progress designation removal
  can leave its job and allow a second job on one node; generic pending-command delimiter escaping
  remains a pre-existing codec concern while ENG-031 ids are regex-safe; RenderFrame/Android view do
  not consume `snapshot.zones`; per-frame zone snapshot allocations need later profiling/consumer work.

### VERIFICATION

- Selfcheck, focused ENG-031/remediation tests, full tests/projects, content validation (2 packs),
  replay, save-compat, benchmark, Android assembleDebug, and `git diff --check` passed with
  `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. Replay hashes:
  `e4892bcc18f9d8dc` / `a763da4ac32b15b4` / `3f02607020d48668`. Benchmark: canonical 413 ms,
  kill 85 ms, spatial index 5.2368 ms, goal rebuild 10958000 ns. Simulation, renderer, save,
  Android, and verifier reviews found no blocker; verifier boundary checks were all true.

## ENG-004 close-out

- DONE: Worker content speed/capacity, typed deterministic hauling, source reservations, persisted
  carry, stockpile contents, positioned producer sources, and save v15 with v1-v14 migration.
- NEXT: Run `/me --feature --next` for ENG-032 (construction blueprints).
- VERIFICATION: Full tests/projects/content/replay/save-compat/benchmark/selfcheck/diff-check and
  Android assembleDebug passed; replay `e4892bcc18f9d8dc` / `a763da4ac32b15b4`.
- BLOCKERS: No implementation blocker; no device/emulator or visual-golden proof beyond assembleDebug.

## ENG-032 close-out

- DONE: Non-blocking blueprint placement, source-aware construction sites, ENG-004 haul delivery,
  content-defined build jobs, completed-building spawn, and deterministic cancellation refunds to
  the original `HaulSourceStore`.
- DECISIONS: Sources are chosen automatically in ascending `sourceId` order and retried when
  unavailable. Save v16 persists sites and source ledgers; v1-v15 migrate with empty construction
  state. One existing material cost is supported; `buildWorkTicks` defaults to 1.
- VERIFICATION: Full tests/projects/content/replay/save-compat/benchmark/selfcheck, Android
  assembleDebug, focused construction tests, and diff-check passed.
- BLOCKERS: No implementation blocker; conditional reviewer subagents timed out after bounded
  waits. Local boundary review and gates passed. No device/emulator proof beyond assembleDebug.
