---
name: me
description: >-
  MyEngine dev orchestrator. Runs the canonical intake -> architect -> developer ->
  tester -> runner -> verifier -> docs pipeline for this repository. Use for any
  engine code or content change, or when the user types /me or asks to implement
  the next feature, fix a bug, run balance/perf/content/save-compat gates, reflect
  on telemetry, or propose a process improvement. Enforces the JSON agent contracts
  and human gates defined in docs/agentic. Modes: --discuss, --spec, --feature --next,
  --bugfix, --balance, --perf, --content-validate, --save-compat, --reflect, --improve.
allowed-tools: Read, Grep, Glob, Edit, Write, Bash, Task
---

# /me — MyEngine dev pipeline

This skill is a **thin adapter**. The source of truth is `docs/agentic/`. Do not
re-derive process rules here — read canon, then route and delegate.

## 1. Intake (always first)

Read in this order (per `AGENTS.md`) before acting:

1. `AGENTS.md`
2. `STATE.md`
3. `.ai/handoff.md`
4. `docs/ENGINE_CONSTITUTION.md`
5. `Plane/README.md` + the active phase file

Then read `docs/agentic/PIPELINE.md` and `docs/agentic/AGENT_CONTRACTS.md`.

## 2. Pick the mode (from `$ARGUMENTS`)

| Mode | Purpose | Gate |
|---|---|---|
| `--discuss` | Explore options/risks, no file edits | none |
| `--spec` | Approved idea -> small implementation spec | human before code |
| `--feature --next` | Next planned feature: developer -> tester -> runner -> verifier -> docs | human if scope changes |
| `--bugfix` | Reproduce -> fix -> test -> document | human if behavior ambiguous |
| `--balance` | Scenario/balance reports, content-only proposals | human before content |
| `--perf` | Benchmark/smoke -> scoped optimization | human before API changes |
| `--content-validate` | Validate content packs | none |
| `--save-compat` | Save roundtrip/migration checks | none |
| `--reflect` | Deterministic retro from telemetry | none |
| `--improve` | Retro evidence -> proposed process change | human before edits |

With no args: summarize the `STATE.md` "Next Exact Action" and offer `--feature --next`.

## 3. Delegate to roster subagents (writer never reviews its own work)

Use the `Task` tool with these agents (shipped in this plugin). Each returns
exactly one JSON envelope per `AGENT_CONTRACTS.md`:

- Plan / analyze: `me-architect`
- Implement: `me-engine-developer`, `me-gameplay-designer`, `me-content-schema-designer`
- Test: `me-tester`
- Run gates: `me-runner` (Bash-only; one JSON line)
- Review (read-only): `me-verifier`, `me-simulation-reviewer`, `me-renderer-qa`,
  `me-save-compat-reviewer`, `me-android-performance`, `me-balance-simulator`
- Document: `me-docs`
- Self-improvement: `me-reflect`, `me-improve`

The writer role and the final reviewer role must never be the same agent. On
malformed structured output, allow exactly one retry, then stop with `needs_human`.

## 4. Deterministic runner entry points (Windows / PowerShell)

- Build & tests: `.\gradlew.bat test`, `.\gradlew.bat projects`
- Gates: `scripts\me-content-validate.ps1`, `scripts\me-sim-replay.ps1`,
  `scripts\me-save-compat.ps1`, `scripts\me-benchmark.ps1`
- Telemetry: `scripts\me-record-run.ps1`; retro: `scripts\me-retro.ps1`
- Adapter self-check: `scripts\me-selfcheck.ps1`

Each script emits one compact JSON object — capture it, do not re-run noisily.

## 5. Close out

On substantial work: update `STATE.md`, `.ai/handoff.md`, `Plane/README.md`, and
durable docs; log adapter/skill/pipeline changes in
`.ai/changes/agent-skill-log.md`; record a telemetry event via
`scripts\me-record-run.ps1`.
