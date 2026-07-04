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
