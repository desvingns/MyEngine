# Agent And Skill Change Log

Append-only log for changes to agent prompts, skills, adapters, workflow contracts, and pipeline
rules. Project docs can change without an entry here unless they alter agent behavior.

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
