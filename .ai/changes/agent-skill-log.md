# Agent And Skill Change Log

Append-only log for changes to agent prompts, skills, adapters, workflow contracts, and pipeline
rules. Project docs can change without an entry here unless they alter agent behavior.

## 2026-08-03 - ENG-025 Flying Enemies

- Owner: Codex
- Change type: feature pipeline close-out
- Changed: `engine-core`, `engine-content`, `engine-ai`, `engine-entities`, `engine-defense`,
  `engine-devtools`, `games/sandbox`, content/defense contracts, save migration fixtures, and
  roadmap/state/handoff artifacts for ENG-025.
- Summary: Used the `me-dev:me` feature pipeline with the local role fallback because the bounded
  roster was unavailable in this session. Added deterministic ground/air movement, air targeting
  capabilities, balance coverage warning, v21 save migration, and replay/leak coverage. No agent,
  skill, plugin, or pipeline contract changed.
- Verification: Focused tests plus full test/projects, content validation, replay, save-compat,
  benchmark, selfcheck, headless inspect, Android assemble, and diff-check gates.

## 2026-08-03 - ENG-006 Seeded Procedural Map Generation

- Owner: Codex
- Change type: feature pipeline close-out
- Changed: `engine-content`, `games/sandbox`, `engine-devtools`, content/devtools/API contracts, and
  roadmap/handoff/state artifacts for ENG-006.
- Summary: Used the me-dev feature pipeline and graphify fast-path tracing. Delegated scout/architect
  workers timed out after bounded waits; local implementation preserved the existing content, runtime,
  save, and replay boundaries. No agent, skill, plugin, or pipeline contract changed.
- Verification: Focused tests plus full test/projects, content validation, replay, save-compat,
  benchmark, Android assemble, selfcheck, and diff-check gates.

## 2026-08-03 - ENG-033 Authored Colony Scope Unblock

- Owner: Codex
- Change type: spec/gate documentation
- Changed:
  - `.claude/specs/backlog/ENG-033-colonist-needs-mvp.md`
  - `.claude/specs/ENGINE_ROADMAP.md`
  - `Plane/15_domain_systems_sequencing.md`
  - `STATE.md`
  - `.ai/handoff.md`
  - `.ai/DIGEST.md`
- Summary: Formalized the existing ENG-033 acceptance as a human-authored colony scope with named
  COL-FR/COL-NFR requirements, traceable acceptance/gates, and an explicit boundary excluding MySD's
  TD evidence from colony claims. ENG-033 is now eligible for a separate implementation run while
  remaining in backlog.
- Human gate: explicit user request to unlock MyEngine specs on 2026-08-03.

## 2026-07-02 - Phase 01 Foundation

- Owner: Codex
- Change type: repository coordination
- Changed:
  - `AGENTS.md`
  - `.ai/handoff.md`
  - `.ai/memory/MEMORY.md`
  - `.ai/memory/reference-policy.md`
  - `.ai/tasks/README.md`
  - `.ai/proposals/README.md`
  - `.ai/runs/README.md`
  - `.ai/retro/README.md`
- Summary: Established initial agent operating rules and `.ai` coordination workspace.
- Human gate: implicit user request to implement Phase 00 and Phase 01.

## 2026-07-02 - Phase 04 Agentic Pipeline Bootstrap

- Owner: Codex
- Change type: workflow contracts and adapters
- Changed:
  - `docs/agentic/PIPELINE.md`
  - `docs/agentic/AGENT_CONTRACTS.md`
  - `docs/agentic/SELF_IMPROVEMENT.md`
  - `docs/agentic/SPEC_BOARD.md`
  - `claude-plugins/me-dev/`
  - `claude-plugins/me-spec/`
  - `codex-plugins/me-dev/`
  - `codex-plugins/me-spec/`
  - `.claude/`
  - `.codex/`
- Summary: Established `/me` and `/me-spec` canonical contracts with thin Claude/Codex adapters.
- Human gate: user request to implement the remaining plan.

## 2026-07-02 - Phase 13 Self-Improvement Loop

- Owner: Codex
- Change type: telemetry and retro loop
- Changed:
  - `scripts/me-record-run.ps1`
  - `scripts/me-retro.ps1`
  - `scripts/me-content-validate.ps1`
  - `scripts/me-sim-replay.ps1`
  - `scripts/me-benchmark.ps1`
  - `scripts/me-save-compat.ps1`
  - `.ai/retro/retro-2026-07-02.md`
- Summary: Added JSONL telemetry recording and deterministic retro aggregation for MyEngine gates.
- Human gate: future improvements still require explicit approval before edits.

## 2026-07-03 - Claude Plugin Marketplace And Agent Roster

- Owner: Claude
- Change type: workflow adapters and distribution
- Changed:
  - `.claude-plugin/marketplace.json` (new repo-local marketplace: `myengine`)
  - `claude-plugins/me-dev/.claude-plugin/plugin.json`
  - `claude-plugins/me-dev/skills/me/SKILL.md`
  - `claude-plugins/me-dev/agents/*.md` (15 roster agents with contract-scoped tools)
  - `claude-plugins/me-dev/README.md`
  - `claude-plugins/me-spec/.claude-plugin/plugin.json`
  - `claude-plugins/me-spec/skills/me-spec/SKILL.md`
  - `claude-plugins/me-spec/agents/me-game-spec-author.md`
  - `claude-plugins/me-spec/README.md`
  - `scripts/me-selfcheck.ps1`
- Summary: Turned the Claude adapters from README stubs into installable Claude Code
  plugins (skills + full agent roster) and added a repo-local marketplace so the engine
  ships with ready-made `/me` and `/me-spec` workflows. Skill name `me` now matches the
  canonical `/me` command. Canonical process in `docs/agentic` is unchanged; adapters
  stay thin and reference canon. Verified via `scripts/me-selfcheck.ps1` -> pass.
- Human gate: explicit user request to implement all proposed improvements and add a
  marketplace.

## 2026-07-04 - MyTD Game Spec Bundle (/me-spec --greenfield-game)

- Owner: Claude
- Change type: game spec authoring + backlog bridge
- Changed:
  - `D:/Pet/MyTD/spec/*` (new greenfield-game bundle: manifest, product-brief, requirements,
    user-stories, acceptance/core-loop.feature, design, content-plan, engine-gap-analysis,
    balance-plan, android-ux, nfr, risks, traceability.csv) — outside the repo per user request
  - `.claude/specs/backlog/MTD-001-reward-deposit.md`
  - `.claude/specs/backlog/MTD-002-gold-cost-gating.md`
  - `.claude/specs/backlog/MTD-003-tower-upgrade-hook.md`
  - `.claude/specs/backlog/MTD-004-difficulty-modifiers.md`
  - `.claude/specs/backlog/MTD-005-render-input-surface.md`
- Summary: Ran `/me-spec` clone-intake against reference `com.vipubstd.games.block.defense`
  (minimalist tower defense). Authored an original, traceable MyTD bundle via
  `me-game-spec-author` (mechanics cloned, all names/numbers original; art own-style). Both
  human gates passed (Gate 1 inventory/scope, Gate 2 final bundle/risks). After traceability
  existed, promoted engine gaps EG-001..EG-005 to `.claude/specs/backlog` as MTD-001..MTD-005;
  game content EG-006 stays in the MyTD bundle. Bundle lives at `D:/Pet/MyTD` per user request,
  not under `games/<slug>/spec`.
- Human gate: user accepted Gate 1 (scope: slice + upgrades + difficulty) and Gate 2 (accept
  bundle + promote backlog bridge).

## 2026-07-04 - Signal Garden SG-001 (/me --feature --next run)

- Owner: Claude
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `games/signal-garden/content/signal-garden/*`,
    `engine-content/src/test/kotlin/dev/myengine/content/SignalGardenContentPackTest.kt`
- Summary: A `/me --feature --next` run executed the standard
  developer -> tester -> verifier -> docs pipeline for SG-001 (Signal Garden content pack).
  Roles: writer `me-gameplay-designer`, tester `me-tester`, verifier `me-verifier` (verdict pass,
  all four boundary_checks true), docs `me-docs`. Logged here only for pipeline traceability per
  this file's convention; the canonical process in `docs/agentic` and all adapters are unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-04 - Signal Garden SG-002 (/me --feature run)

- Owner: Claude
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `engine-defense/src/main/kotlin/dev/myengine/defense/DefenseRuntime.kt`,
    `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`,
    `engine-defense/src/test/kotlin/dev/myengine/defense/DefenseRuntimeTest.kt`,
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxRewardDepositTest.kt`,
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxVerticalSliceTest.kt`
- Summary: A `/me --feature` run executed the standard
  architect -> developer -> tester -> simulation-reviewer + verifier -> docs pipeline for SG-002
  (reward deposit hook). Roles: architect `me-architect` (chose Option A; no ADR since
  `DefenseRuntime` is Experimental per `docs/API_STABILITY.md`), developer
  `me-engine-developer`, tester `me-tester` (one runner-caught fix to sandbox e2e tower
  positions), reviewers `me-simulation-reviewer` (pass) and `me-verifier` (pass, all four
  boundary_checks true), docs `me-docs`. Logged here only for pipeline traceability per this
  file's convention; the canonical process in `docs/agentic` and all adapters are unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-04 - Signal Garden SG-003 (/me --feature run)

- Owner: Claude
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `engine-render/src/main/kotlin/dev/myengine/render/PlaceholderRenderSurface.kt`,
    `engine-render/src/test/kotlin/dev/myengine/render/RenderBoundaryTest.kt`,
    `engine-render/src/test/kotlin/dev/myengine/render/PlaceholderRenderSurfaceTest.kt`,
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxRenderNonMutationTest.kt`
- Summary: A `/me --feature` run executed the standard
  developer -> tester -> renderer-qa + verifier -> docs pipeline for SG-003 (placeholder render
  surface). Roles: developer `me-engine-developer` (architect skipped — scope small and
  well-bounded, like SG-001), tester `me-tester`, reviewers `me-renderer-qa` (pass: snapshot
  boundary, camera math, coverage sound) and `me-verifier` (pass, all four boundary_checks true,
  render_snapshot_only confirmed), docs `me-docs`. Added a pure, game-agnostic
  `snapshot -> RenderFrame` projection in engine-render (no game/Android imports, no simulation
  mutation), plus camera pan/zoom and render non-mutation coverage. Logged here only for pipeline
  traceability per this file's convention; the canonical process in `docs/agentic` and all adapters
  are unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-04 - Signal Garden SG-003 follow-up (/me --feature run)

- Owner: Claude
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `engine-render/src/main/kotlin/dev/myengine/render/RenderPalette.kt` (new),
    `desktop/src/main/kotlin/dev/myengine/desktop/FrameRasterizer.kt` (new),
    `desktop/src/main/kotlin/dev/myengine/desktop/DesktopLauncher.kt` (edit),
    `desktop/build.gradle.kts` (edit),
    `desktop/src/test/kotlin/dev/myengine/desktop/FrameRasterizerPixelSmokeTest.kt` (new)
- Summary: A `/me --feature` run executed the
  architect -> developer -> tester -> runner -> renderer-qa + verifier -> docs pipeline for the
  SG-003 follow-up (consume `RenderFrame` in a real desktop launcher + pixel-smoke; six
  `RenderKind`s now have visual coverage). Roles: architect `me-architect` (chose a pure headless
  AWT rasterizer; refined the module split so AWT stays out of engine-render because it compiles
  into Android; no ADR — Experimental presentation boundary, no new module edge), developer
  `me-engine-developer`, tester `me-tester`, runner `me-runner`, reviewers `me-renderer-qa` (pass)
  and `me-verifier` (pass, all four boundary_checks true), docs `me-docs`. Reviewers found only
  cosmetic/low nits; orchestrator applied one post-review cosmetic fix (debug PNG path). Logged here
  only for pipeline traceability per this file's convention; the canonical process in `docs/agentic`
  and all adapters are unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-04 - PROC pipeline improvements v0.2.0 (architecture review -> improve batch)

- Owner: Claude
- Change type: workflow contracts, adapters, telemetry scripts, spec-board rules
- Changed:
  - `docs/agentic/SELF_IMPROVEMENT.md` (mandatory telemetry incl. failures; new fields
    `duration_min`/`malformed_json_count`/`gate_failures`/`attributed_agent`; reflection cadence
    — after every failed run and every 5th event; proposal queue `.ai/proposals/` +
    `--improve --drain`; plugin-versioning rule; memory boundary)
  - `docs/agentic/PIPELINE.md` (selfcheck at intake; `.ai/DIGEST.md` intake digest; Reviewer
    Matrix — conditional domain reviewers by changed paths; new `--improve --drain` and
    `--upgrade` commands; close-out flips spec card status + syncs game bundle traceability;
    runner invocation rule `powershell.exe -File`)
  - `docs/agentic/AGENT_CONTRACTS.md` (roster + Scout schema for new `me-scout`;
    `attributed_agent` in Verifier/Reflect findings; runner invocation rule)
  - `docs/agentic/SPEC_BOARD.md` (Gap Dedup Rule; Engine Roadmap rule)
  - `docs/GAME_SPEC_PIPELINE.md` (gap dedup + roadmap in Engine Gap Split)
  - `scripts/me-record-run.ps1` (needs_human verdict; new event fields; `reflect_required`
    in output), `scripts/me-retro.ps1` (attribution/gate-failure/retry aggregation)
  - `claude-plugins/me-dev/skills/me/SKILL.md` (intake selfcheck + digest, reviewer matrix,
    scout delegation, mandatory close-out telemetry + reflect trigger, new modes)
  - `claude-plugins/me-dev/agents/*.md` (model assignments on all 15: haiku for runner,
    sonnet for domain reviewers/tester/docs/reflect/content-schema/balance, inherit for
    architect/developer/gameplay-designer/verifier/improve; contract updates in
    verifier/reflect/improve/runner/docs)
  - `claude-plugins/me-dev/agents/me-scout.md` (new cheap fact-finding agent)
  - `claude-plugins/me-spec/skills/me-spec/SKILL.md` + `agents/me-game-spec-author.md`
    (mandatory gap dedup + roadmap updates; model: inherit)
  - `.claude/specs/ENGINE_ROADMAP.md` (new: aggregated gaps with demand counts; flags
    MTD-001 as duplicate of implemented SG-002)
  - `claude-plugins/me-dev/.claude-plugin/plugin.json`,
    `claude-plugins/me-spec/.claude-plugin/plugin.json` (0.1.0 -> 0.2.0)
- Summary: First real pass of the improve loop, sourced from an architecture review of the
  agentic solution. Closes the observe->reflect->improve gap (telemetry was pass-only and
  `--improve` had never run), cuts cost via model tiering + conditional reviewers + scout +
  intake digest, and stops cross-game engine-gap duplication via dedup rule + demand-counted
  roadmap. Deferred items (spec back-sync automation, multi-repo ADR, perf budgets, golden
  hashes, CI, migration matrix, playtest bot, Android visual smoke, cost telemetry) are filed
  as PROC-00x backlog cards, not implemented.
- Human gate: user approved the review plan and the P0+P1+P2.1-2.2 scope explicitly
  (plan approval, 2026-07-04).

## 2026-07-04 - Signal Garden SG-004 (/me --feature run)

- Owner: Claude
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`,
    `docs/contracts/android.md`):
    `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt` (new),
    `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` (edit),
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt` (new)
- Summary: A `/me --feature` run executed the
  architect -> developer -> tester -> save-compat-reviewer + android-performance + verifier -> docs
  pipeline for SG-004 (Android lifecycle save smoke). Roles: architect `me-architect` (chose
  Option A — pure Android-free `SandboxSession` holder in games/sandbox + thin Activity adapter;
  command-queue/RNG persistence deferred; no ADR, no save-format change), developer
  `me-engine-developer`, tester `me-tester` (added a post-review future/non-numeric version-rejection
  guard), reviewers `me-save-compat-reviewer` (pass), `me-android-performance` (pass) and
  `me-verifier` (pass, all four boundary_checks true), docs `me-docs`. Device-independent lifecycle
  proof is JVM-covered; the on-device Bundle round-trip is documented as device-pending
  (acceptance #3 blocker). Note: the `me-docs` close-out sub-agent was interrupted by a process exit
  after writing STATE/handoff/Plane/android.md; the orchestrator appended this final log entry to
  complete the close-out. Logged here only for pipeline traceability per this file's convention; the
  canonical process in `docs/agentic` and all adapters are unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-05 - Signal Garden SG-004 follow-up (/me --feature run)

- Owner: Claude
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `engine-core/src/main/kotlin/dev/myengine/core/Command.kt` (edit — new `CommandQueue.pending()`),
    `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt` (edit —
    `SAVE_VERSION` 1->2, `pendingCommands` encode/decode, `decodePendingCommands`,
    `SandboxRuntime.pendingCommands()`/`submitAll()`),
    `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxSession.kt` (edit — save/restore
    wiring, KDoc drops quiescent-save precondition),
    `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` (edit — `DEBUG_SAVE` ->
    `BuildConfig.DEBUG`), `android/build.gradle.kts` (edit — `buildFeatures { buildConfig = true }`),
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt` (edit —
    pending-command regression, v1->v2 migration, CommandId/Tick preservation, retargeted
    version-rejection tests)
- Summary: A `/me --feature` run executed the
  architect -> developer -> tester -> runner -> simulation-reviewer + save-compat-reviewer +
  android-performance + verifier -> docs pipeline for the SG-004 follow-up (sandbox save-format v2,
  persisting the runtime's pending `CommandQueue` so lifecycle saves are sound at any tick, not just
  quiescent ones). This was the last recorded gap under SG-004 in `STATE.md`/`.ai/handoff.md`.
  Roles: architect `me-architect` (ruled no ADR needed — covered by the existing
  "saves are versioned from v1 and migration-aware" invariant; `SandboxSaveCodec` stays Experimental
  per `docs/API_STABILITY.md`, same precedent as the original v1 codec), developer
  `me-engine-developer`, tester `me-tester`, runner `me-runner` (`:games:sandbox:test` pass, full
  `test` pass, `me-save-compat.ps1` pass, `:android:compileDebugKotlin` pass after a machine-local
  `local.properties` fix — not a code change), reviewers `me-simulation-reviewer` (pass, 2
  non-blocking mediums), `me-save-compat-reviewer` (pass, 1 non-blocking low),
  `me-android-performance` (pass, 1 non-blocking low + 1 pre-existing medium), `me-verifier` (pass,
  all four boundary_checks true; confirmed all three SG-004 acceptance criteria satisfied, with
  "lifecycle pause does not corrupt simulation state" now strictly stronger than before). Closes
  SG-004 follow-up items #1 (command-queue persistence) and #2 (`DEBUG_SAVE` gated on
  `BuildConfig.DEBUG`); flips the `SG-004-android-save-smoke.md` backlog card to `done` and updates
  `.claude/specs/ENGINE_ROADMAP.md`. Logged here only for pipeline traceability per this file's
  convention; the canonical process in `docs/agentic` and all adapters are unchanged.
- Human gate: user request to close out the SG-004 follow-up.

## 2026-07-05 - MyTD MTD-001/MTD-002 (/me --feature --next run)

- Owner: Codex
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxTowerCostGatingTest.kt` (new),
    `.claude/specs/backlog/MTD-001-reward-deposit.md`,
    `.claude/specs/backlog/MTD-002-gold-cost-gating.md`, `.claude/specs/ENGINE_ROADMAP.md`,
    `D:/Pet/MyTD/spec/engine-gap-analysis.md`, `D:/Pet/MyTD/spec/traceability.csv`,
    `D:/Pet/MyTD/spec/risks.md`.
- Summary: A `/me --feature --next` run closed MTD-001 as a duplicate of completed SG-002 and
  closed MTD-002 by verifying the existing generic resource-cost placement path with new acceptance
  tests. Roles: scout `me-scout`, architect `me-architect` (no ADR; keep MyTD gold as content data
  and keep `engine-defense` inventory-free), tester `me-tester` (pass, no findings), runner gates
  pass, verifier `me-verifier` (pass, all four boundary checks true, no findings). Logged here only for pipeline traceability per this file's
  convention; the canonical process in `docs/agentic` and all adapters are unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-05 - MyTD MTD-003 (/me --feature --next run)

- Owner: Codex
- Change type: none (normal feature run; no agent/skill/adapter/pipeline contract change)
- Changed:
  - No agent prompts, skills, adapters, or workflow contracts changed.
  - Feature artifacts (logged in `STATE.md`, `.ai/handoff.md`, `Plane/README.md`):
    `engine-content/src/main/kotlin/dev/myengine/content/ContentDefinitions.kt`,
    `engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt`,
    `engine-content/src/test/kotlin/dev/myengine/content/ContentPackLoaderTest.kt`,
    `engine-entities/src/main/kotlin/dev/myengine/entities/EntityModel.kt`,
    `engine-render/src/main/kotlin/dev/myengine/render/RenderModel.kt`,
    `games/sandbox/content/sandbox/towers.properties`,
    `games/sandbox/src/main/kotlin/dev/myengine/games/sandbox/SandboxGame.kt`,
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxSessionLifecycleTest.kt`,
    `games/sandbox/src/test/kotlin/dev/myengine/games/sandbox/SandboxTowerUpgradeTest.kt`,
    `.claude/specs/backlog/MTD-003-tower-upgrade-hook.md`,
    `.claude/specs/ENGINE_ROADMAP.md`, and `D:/Pet/MyTD/spec/*` status sync.
- Summary: A `/me --feature --next` run implemented MTD-003 (tower upgrade hook). Roles: scouts
  `me-scout`, architect `me-architect` (no ADR; required tier-transition validation and
  delimiter-safe branch ids), developer/orchestrator, tester `me-tester` (pass), runner gates pass,
  reviewers `me-simulation-reviewer`, `me-save-compat-reviewer`, `me-renderer-qa`,
  `me-balance-simulator`, and `me-verifier` all pass. Canonical process and adapters unchanged.
- Human gate: user request to run the feature pipeline for the next backlog item.

## 2026-07-05T10:05-brain-level-scope-and-twins
type: add
target: docs/agentic/AGENT_CONTRACTS.md, docs/agentic/SELF_IMPROVEMENT.md, AGENTS.md, claude-plugins/me-dev/agents/me-docs.md, claude-plugins/me-dev/agents/me-improve.md
summary: brain-level scope for me-reflect findings; me-docs flushes brain candidates to D:/Pet/brain/inbox; optional twin_applicability (vs mp, per brain/pipelines/TWINS.md) in improve proposals
reason: second-brain repo (github.com/desvingns/brain) now carries cross-project knowledge and the mp<->me twin registry; MyEngine lessons that generalize route to the brain inbox (human-gated promotion), and proposals declare twin applicability so /brain sync-twins can stage mp-side ports
affects: claude, codex
by: claude

## 2026-07-18 - Feature delivery commit-and-push gate

- Owner: Codex
- Change type: pipeline and adapter contract
- Changed: `docs/agentic/PIPELINE.md`, `docs/agentic/SELF_IMPROVEMENT.md`,
  `claude-plugins/me-dev/skills/me/SKILL.md`, `codex-plugins/me-dev/skills/me-dev/SKILL.md`,
  `claude-plugins/me-dev/.claude-plugin/plugin.json` (0.2.0 -> 0.2.1), and
  `codex-plugins/me-dev/.codex-plugin/plugin.json` (0.1.0 -> 0.1.1).
- Summary: successful `/me --feature` runs now require one scoped conventional commit and a direct
  push to `main` before they can be reported complete. Intake is blocked by a dirty
  worktree unless the existing changes are explicitly identified as the approved baseline; unrelated
  files must never be staged, amended, committed, or pushed. Commit/push failure leaves the feature
  blocked and its backlog card incomplete.
- Human gate: direct user instruction: each completed me-dev skill call must commit and push the feature.

## 2026-07-21 - MyEngine ENG-030 documentation close-out

- Owner: Codex
- Change type: none (documentation-only close-out; no pipeline, adapter, or plugin change)
- Changed: ENG-030 backlog card, engine roadmap, properties schema, `STATE.md`, `.ai/handoff.md`,
  `Plane/README.md`, `.ai/DIGEST.md`, and this log.
- Summary: Recorded accepted wave preview/early-call behavior, typed command, content-defined
  `resourceId + amount` bonus validation, strict scheduled-tick guard, `SAVE_VERSION=8`, v1-v7
  migration, replay/save/gate evidence, and the remediation test coverage.
- Balance review: partial; current content packs are valid and contain no hardcoded bonus, the
  schema-documentation gap was closed in this docs close-out, and the optional bonus remains
  unconfigured pending an approved balance value.
- Risks: Existing packs intentionally omit bonus values pending balance approval; low HUD
  per-snapshot allocation/device profiling follow-up; pre-existing save delimiter assumption.
- No ADR was needed and the me-dev plugin version was not bumped. No commit or push was performed.

## 2026-07-29 - MyEngine PROC-013 Variant B close-out

- PROC-013 added board checker/selfcheck wiring and migrated 23 verified-done cards from backlog to
  done. No adapter/skill version bump was needed because no canonical contract or adapter changed.

## 2026-08-01 - MyEngine ENG-010 feature run

- Owner: Codex
- Change type: normal feature run; no agent/skill/adapter/pipeline contract change.
- Summary: `/me --feature --next` selected ENG-010 and implemented content-defined slow/DoT status
  effects, deterministic defense lifecycle, save v9 migration, immutable snapshot/render tags,
  and focused replay/content/save tests.
- Pipeline: selfcheck pass; scouts partial with verified file facts; architect pass/no ADR; writer
  agents timed out after partial production edits, so the orchestrator completed the bounded scope;
  tester agent timed out, so test-only updates were completed locally; runner agent timed out, so
  canonical runner commands were executed locally; save-compat reviewer pass/no findings; renderer
  review passed after defensive tag copying; the simulation review's non-enemy DoT metrics finding
  was fixed with regression coverage; other conditional reviewers/final verifier were unavailable
  after the subagent thread limit was reached.
- Verification: full tests, projects, content validation, replay, save-compat, benchmark, Android
  assemble, focused tests, and `git diff --check` passed.
- No plugin version bump; no ADR.

## 2026-08-02 - MyEngine ENG-018 feature run

- Owner: Codex
- Change type: normal feature run; no agent/skill/adapter/pipeline contract change.
- Summary: `/me --feature --next` selected ENG-018 and added data-defined endless waves, shared-RNG
  generation, no-win validation, effective generated enemy state, and deterministic scaling-table
  devtools output.
- Pipeline: selfcheck, scout, architect, and runner contracts passed. Developer returned partial
  after a timeout; bounded integration was completed locally. Tester, conditional simulation
  reviewer, final verifier, and docs workers timed out; focused tests, local boundary review, full
  gates, and docs close-out were completed locally. No malformed JSON was recorded.
- Verification: full tests/projects/content/replay/save-compat/benchmark/diff-check passed; replay
  hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`.
- No plugin version bump; no ADR.

## 2026-08-02 - MyEngine ENG-007 documentation close-out

- Owner: Codex
- Change type: none (documentation-only close-out; no agent/skill/adapter/pipeline contract change)
- Summary: moved the accepted ENG-007 card to `done/`, synchronized the roadmap, state, handoff,
  Plane README, and digest with multiple spawn points + per-wave routing, and corrected stale
  references that assigned enemy armor + damage types to ENG-007; that capability is ENG-011.
- Verification: recorded the accepted runner gates, canonical replay hashes, benchmark metrics,
  59 focused remediation tests, and simulation/save-compat/verifier review passes. `SAVE_VERSION`
  remains 11; no ADR was needed.
- No plugin, skill, or adapter version bump; no production code, tests, or scripts were edited.

## 2026-08-02 - MyEngine ENG-016 documentation close-out

- Owner: Codex
- Change type: none (documentation-only close-out; no agent/skill/adapter/pipeline contract change)
- Changed:
  - `.claude/specs/done/ENG-016-incident-execution.md` (status done, completion note, verification;
    moved from `.claude/specs/backlog/` per board convention)
  - `docs/content-schemas/PROPERTIES_SCHEMA.md`, `.claude/specs/ENGINE_ROADMAP.md`, `STATE.md`,
    `.ai/handoff.md`, `Plane/README.md`, and `.ai/DIGEST.md`
- DONE: Documented optional incidents, cadence start/end, pacing threat windows, cooldown defaults,
  typed spawn-wave/resource-event/modifier effects, diagnostics paths, atomic preflight, `Long`
  aggregation remediation, and v10/v1-v9 save compatibility. Synced the card, roadmap, state,
  handoff, Plane, and digest to the accepted implementation.
- DECISIONS: No ADR; default pack balance and Android production, renderer, and input boundaries
  remain unchanged. This close-out did not edit the pre-existing ENG-016 production/test changes.
- NEXT: Run `/me --feature --next` for PROC-007, then continue the adopted chain.
- BLOCKERS: No ENG-016 implementation blocker remains. No device proof is claimed; existing device,
  FrameMetrics/JankStats, and other manual performance follow-ups remain pending. Gradle requires
  process-local `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.
- VERIFICATION: Full Gradle test/projects/content/replay/save-compat/benchmark/diff-check lanes,
  focused `SandboxIncidentTest` and content tests, and simulation/save reviews passed. Canonical
  replay `e4892bcc18f9d8dc`, kill replay `a763da4ac32b15b4`; remediation benchmark `sim=418 ms`,
  `kill=85 ms`, `spatial-index-1k=6.1036 ms`, `goal-field=10.427 ms`. Initial `614/120 ms` values
  are superseded. No plugin version bump; no commit or push was performed.

## 2026-08-02 - MyEngine ENG-012 feature run

- Owner: Codex
- Change type: normal feature run; no agent/skill/adapter/pipeline contract change.
- Summary: `/me --feature --next` selected ENG-012 and implemented data-defined elite/boss ranks,
  enemy and indexed wave scaling, deterministic effective spawn state, boss snapshot/render marking,
  save v11 with v1-v10 migration, and effective balance reporting.
- Pipeline: selfcheck and scout/architect intake passed; developer/tester/runner and conditional
  reviewer threads were unavailable after repeated timeouts, so the bounded implementation, tests,
  local boundary review, and runner gates were completed in the orchestrator. No malformed JSON or
  retry-triggered agent failure was recorded.
- Verification: full tests, projects, content validation, replay, save-compat v1-v11 matrix,
  benchmark, Android assemble, focused tests, selfcheck, and `git diff --check` passed. Canonical
  replay hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`.
- No plugin version bump; no ADR.

## 2026-08-02 - MyEngine ENG-019 documentation close-out

- Owner: Codex
- Change type: none (documentation-only close-out; no pipeline, adapter, or plugin contract change).
- Changed: ENG-019 card moved from `.claude/specs/backlog/` to `.claude/specs/done/`, roadmap/board
  status, `STATE.md`, `.ai/handoff.md`, `Plane/README.md`, `.ai/DIGEST.md`, and directly related
  SandboxSession/SandboxSaveSlotStore KDoc references from authoritative save v11 to v12.
- Summary: Recorded the validated 1x1 wall MVP, render-free place/remove commands, atomic resource /
  occupancy and path validation, immutable snapshot health, save v12 with v1-v11 migration, approved
  balance (2 bolt / 20 HP / 50% refund), and the positive wall-cost validation remediation while
  preserving non-negative tower-cost compatibility. No ADR was needed.
- Verification: final runner 9/9 passed; replay hashes are
  `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, and `3f02607020d48668`; save-compat includes the v12
  building fixture and v1-v11 matrix; benchmark metrics are GoalField 64x64 / 3,844 reachable /
  11,813,800 ns, spatial index 1,024 enemies / 16 towers / 16 queries / 16 shots / 5.201 ms,
  canonical and kill scenarios 35 ticks / 431 ms and 79 ms. Android assembleDebug passed only;
  no device, emulator, visual-golden, or frame-budget claim is made.
- Next: `/me --feature --next` for ENG-001 (A* point-to-point pathfinding for agents), the next
  backlog item in the deferred colony slice. No plugin, skill, or adapter version bump.

## 2026-08-02 - MyEngine ENG-031 documentation close-out

- Owner: Codex / me-docs
- Change type: normal feature-run close-out; documentation/spec/telemetry only. No production code
  or tests were edited by this role; no commit or push was performed.
- Changed: ENG-031 card moved to `.claude/specs/done/` with status `done`; roadmap, `STATE.md`,
  `.ai/handoff.md`, `Plane/README.md`, and `.ai/DIGEST.md` synced to the accepted Option A and
  next order `ENG-004 -> ENG-032`; appended a NEW cross-project lesson to the brain inbox.
- Decisions: No ADR, game-bundle traceability update, or plugin/skill/pipeline contract change;
  no plugin version bump.
- Verification: selfcheck passed; focused ENG-031/remediation tests and full runner evidence passed;
  non-blocking reviewer follow-ups remain recorded in the close-out documents.

## 2026-08-02 - MyEngine ENG-004 feature run

- Owner: Codex
- Change type: normal feature run; no agent/skill/adapter/pipeline contract change.
- Summary: `/me --feature --next` selected ENG-004 and delivered data-defined worker capabilities,
  deterministic source reservations, source-to-stockpile hauling, positioned producer output sources,
  persisted stockpile contents/carry, and save v15 migration.
- Pipeline: selfcheck passed; scout and architect contracts passed. Developer and verifier workers
  timed out after bounded waits, so the scoped production implementation, tests, runner gates, and
  local boundary review were completed by the orchestrator. No malformed JSON was returned.
- Verification: full tests/projects/content validation/replay/save-compat/benchmark/Android assemble
  and diff-check passed. Replay hashes remain `e4892bcc18f9d8dc` / `a763da4ac32b15b4`.
- No plugin version bump; no ADR.

## 2026-08-02 - MyEngine ENG-032 feature run

- Owner: Codex
- Change type: normal feature run; no agent/skill/adapter/pipeline contract change.
- Summary: `/me --feature --next` selected ENG-032 and added deterministic construction blueprints,
  ENG-004 haul delivery to construction sites, build jobs, source refunds on cancellation, save v16
  migration, and focused replay/save coverage. User gate decisions: refund to the original
  `HaulSourceStore`; automatic source selection by ascending `sourceId` with retry.
- Pipeline: selfcheck/scout/architect intake passed. Developer/tester/reviewer subagents timed out
  after bounded waits; the orchestrator completed the scoped implementation, tests, runner gates,
  and local boundary review. A late simulation review found and the orchestrator fixed resource-
  specific reservation accounting, mixed worker-cargo refund handling, and effect tie-breakers.
- Verification: full tests/projects/content validation/replay/save-compat/benchmark/selfcheck,
  Android assembleDebug, focused construction tests, and `git diff --check` passed.
- No plugin version bump; no ADR. Reviewer timeouts are recorded as a non-blocking infrastructure
  note in the close-out docs and telemetry.

## 2026-08-03 - MyEngine DX-002 feature run

- Owner: Codex
- Change type: normal feature run; no agent/skill/adapter/pipeline contract change.
- Summary: Added the provider-based headless state inspector, sandbox ServiceLoader adapter,
  deterministic ASCII/JSON state projection, optional command scripts, CLI aliases, and default
  AGENTS debugging reference. No save version or Android production change.
- Pipeline: selfcheck passed; scout confirmed DX-002 as the next accepted high-leverage card;
  architect/developer/tester/runner contracts were used. Verifier timed out after bounded waiting;
  local boundary review completed. Runner had one transient full-test invocation failure, then a
  confirmation pass; no malformed JSON was returned.
- Verification: focused inspector tests, confirmed full tests/projects/content validation/replay/
  save-compat/benchmark/selfcheck, Android assembleDebug, and diff-check passed.
- No plugin version bump; no ADR.

## 2026-08-03 - MyEngine DX-006 feature run

- Owner: Codex
- Change type: normal documentation feature run; no agent/skill/adapter/pipeline contract change.
- Summary: `/me --feature --next` selected DX-006 and added `docs/COOKBOOK.md` with five on-demand
  task recipes (tower, content field, save field, tick-loop system, snapshot field), exact file
  lists, gates, and historical commit references. `AGENTS.md` now links the cookbook after intake
  without expanding the always-loaded checklist. The card moved to `.claude/specs/done/` and the
  roadmap/state/handoff/Plane/digest were synchronized.
- Pipeline: selfcheck passed; scout, architect, and developer workers did not return within the
  initial bounded waits, so the orchestrator completed/reviewed the bounded documentation scope
  locally; their later valid contracts confirmed the same scope. The final verifier worker was
  requested but timed out; local boundary review found no blocker. No malformed JSON envelope was
  returned by a completed worker.
- Verification: full tests/projects, content validation, replay, save-compat, benchmark, selfcheck,
  Android `assembleDebug`, and `git diff --check` passed. The first Gradle test invocation lacked a
  valid `JAVA_HOME`; the confirmation run used Android Studio JBR and passed.
- Baseline: known untracked `.ai/retro/retro-2026-08-03.md` was preserved and excluded from scope.
- No plugin version bump; no ADR or human gate.

## 2026-08-03 - MyEngine DX-005 + PROC-006 feature run

- Owner: Codex
- Change type: combined schema-docs drift and deterministic pre-push lane; no engine/runtime,
  save, Android production, plugin, skill, or adapter contract change.
- Summary: Added `scripts/me-schema-docs-drift.ps1` with sorted bidirectional JSON drift output,
  aligned the properties schema for status effects and legacy indexed fields, added deterministic
  drift fixtures, wired the gate into selfcheck, and added `.githooks/pre-push` plus
  `scripts/me-pre-push.ps1` for the full tests/content/replay/save lane.
- Pipeline: user approved the combined DX-005 + PROC-006 scope after the architecture gate;
  scout and tester contracts passed (tester found and covered PowerShell scalar-array behavior),
  developer contract passed, runner was partial on the first no-JAVA_HOME attempt and the
  orchestrator confirmed the Gradle gates with Android Studio JBR. No malformed envelope was
  returned by completed workers; bounded worker timeout notes remain in telemetry.
- Verification: drift/fixture checks, pre-push tests, content validation, replay, save compatibility,
  selfcheck, benchmark (`sim_ms=15.9066`), `gradlew projects`, `:android:assembleDebug`, and
  `git diff --check` passed.
- Baseline: known untracked `.ai/retro/retro-2026-08-03.md` was preserved and excluded.
- No plugin version bump; no ADR.

## 2026-08-03 - MyEngine ENG-023 feature run

- Owner: Codex / `me-dev:me`
- Change type: normal engine feature run; no agent, skill, adapter, plugin, or pipeline contract
  change.
- Summary: `/me --feature --next` reviewed the accepted backlog and selected ENG-023. Added
  Android-free straight/corner conveyor state and deterministic transport, authored content timing,
  producer/core/entity endpoints, save v20 with v1-v19 migration, focused tests, and a 100-belt
  throughput benchmark. The card moved to `.claude/specs/done/`.
- Pipeline: scout and architect returned valid JSON; the first developer worker timed out and then
  returned `partial` with no edits, so implementation continued locally under the approved scope.
  The tester returned focused tests. Conditional reviewer/verifier workers could not start after
  the bounded subagent thread limit; local boundary review found no blocker. No malformed JSON
  envelope was returned by a completed worker.
- Verification: full `gradlew test`, projects, content/schema drift, replay, save compatibility,
  benchmark, selfcheck, required headless inspect, Android `assembleDebug`, and `git diff --check`
  passed. Replay hashes remained `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, and `3f02607020d48668`.
- Baseline: known untracked `.ai/retro/retro-2026-08-03.md` was preserved and excluded.
- No plugin version bump; no ADR.

## 2026-08-03 - MyEngine ENG-034 feature run

- Owner: Codex / `me-dev:me`
- Change type: normal engine feature run; no agent, skill, adapter, plugin, or pipeline contract
  change.
- Summary: `/me --feature --next` reviewed the accepted backlog, selected roadmap-ordered ENG-034,
  and added content-flagged blocked-enemy attacks against structures, deterministic target ordering,
  lethal occupancy/GoalField invalidation, save-compatible building health, and balance metrics.
  The card moved to `.claude/specs/done/` and state/hand-off/Plane/digest documents were synced.
- Pipeline: selfcheck, scout, architect, implementation, local tests, runner gates, and final review
  completed. Conditional tester/simulation/save/balance workers could not start after the bounded
  subagent-thread limit. The final verifier returned `partial` after its bounded wait and identified
  a missing dedicated breach-reroute replay test; the orchestrator added that test and reran the
  focused/full gates. No malformed JSON envelope was returned by a completed worker.
- Verification: focused tests, full tests/projects, content validation, replay, save compatibility,
  benchmark, selfcheck, Android `assembleDebug`, required headless inspect, and `git diff --check`
  passed. Replay hashes remained `e4892bcc18f9d8dc`, `a763da4ac32b15b4`, and `3f02607020d48668`.
- Baseline: known untracked `.ai/retro/retro-2026-08-03.md` was preserved and excluded.
- No plugin version bump; no ADR; no `SAVE_VERSION` bump.

## 2026-08-03 - MyEngine ENG-035 feature run

- Owner: Codex / `me-dev:me`
- Change type: normal engine feature run; no agent, skill, adapter, plugin, or pipeline contract
  change.
- Summary: `/me --feature --next` selected ENG-035 after the accepted-backlog review. Added
  content-defined finite/infinite resource nodes, output-only extractor building production,
  deterministic underlying/adjacent binding, stable ENG-004 haul sources, partial final batches,
  v19 save persistence with v1-v18 migration, and focused content/logistics/sandbox coverage.
  The card moved to `.claude/specs/done/`; ENG-023 belt transport remains a separate follow-up.
- Pipeline: user confirmed the architect's human-gated scope. The bounded developer worker returned
  no edits, so implementation continued locally under the same approved scope. Runner gates and
  final local boundary review passed; no malformed JSON envelope was returned.
- Verification: full `gradlew test`, content validation, replay, save compatibility, benchmark,
  selfcheck, required headless inspect, Android `assembleDebug`, and `git diff --check` passed.
  Save compatibility covers v1-v19 fixtures; replay hashes remained stable.
- Baseline: known untracked `.ai/retro/retro-2026-08-03.md` was preserved and excluded.
- No plugin version bump; no ADR.
