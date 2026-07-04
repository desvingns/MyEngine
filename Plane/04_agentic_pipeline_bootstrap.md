# Phase 04 - Agentic Pipeline Bootstrap

Status: Planned

## Цель

Создать dual-harness development pipeline для `MyEngine`: Claude и Codex должны работать по одному canonical process, с thin adapters, strict output contracts, telemetry hooks, human gates and self-improvement path.

## Входы

- Phase 01-03 deliverables
- `D:\Pet\mobile-pipeline`
- Agentic references from `PROMPT_PACK.md`

## Work Packages

### 04.1 Pipeline Docs

Создать:

```text
docs/agentic/PIPELINE.md
docs/agentic/AGENT_CONTRACTS.md
docs/agentic/SELF_IMPROVEMENT.md
docs/agentic/SPEC_BOARD.md
```

### 04.2 Commands

Определить commands:

- `/me --discuss`
- `/me --spec`
- `/me --feature --next`
- `/me --bugfix`
- `/me --balance`
- `/me --perf`
- `/me --content-validate`
- `/me --save-compat`
- `/me --reflect`
- `/me --improve`
- `/me-spec --greenfield-game`
- `/me-spec --engine-feature`

### 04.3 Agent Roster

Минимальные роли:

- `me-architect`
- `me-engine-developer`
- `me-gameplay-designer`
- `me-simulation-reviewer`
- `me-android-performance`
- `me-content-schema-designer`
- `me-balance-simulator`
- `me-renderer-qa`
- `me-save-compat-reviewer`
- `me-tester`
- `me-runner`
- `me-verifier`
- `me-docs`
- `me-reflect`
- `me-improve`
- `me-game-spec-author`

### 04.4 File Layout

Создать initial stubs:

```text
claude-plugins/me-dev/
claude-plugins/me-spec/
codex-plugins/me-dev/
codex-plugins/me-spec/
.claude/myengine/config.json
.claude/myengine/extras/
.claude/specs/backlog/
.claude/specs/active/
.claude/specs/done/
.codex/agents/
.codex/skills/
```

### 04.5 Contracts

Every LLM agent returns one JSON object or one BRAINSTORM block. Runner scripts emit one JSON line. Invalid structured output gets one retry.

### 04.6 Deterministic Scripts

Plan or stub:

- `scripts/me-record-run.*`
- `scripts/me-retro.*`
- `scripts/me-content-validate.*`
- `scripts/me-sim-replay.*`
- `scripts/me-benchmark.*`
- `scripts/me-save-compat.*`

## Acceptance Gates

- `docs/agentic/AGENT_CONTRACTS.md` defines exact output schema per agent.
- Claude/Codex canonical-source strategy is documented.
- Pipeline cannot self-modify without human gate.
- Agent roles are narrow; writer and final reviewer are separate.
- Project-local lessons and pipeline-level improvements are distinct.

## Standalone Prompt

```text
Ты выполняешь Phase 04: agentic pipeline bootstrap.

Изучи `D:\Pet\mobile-pipeline` как главный процессный референс. Создай docs/agentic, command designs, agent contracts, initial plugin/skill layout and scripts stubs for MyEngine. Не реализуй весь pipeline полностью, но сделай достаточно, чтобы будущий `/me --feature --next` workflow был формализован.

Keep canonical source single. Adapters are thin. JSON contracts strict. Human gates mandatory for spec approval and self-improvement.

Update Plane/README progress.
```

