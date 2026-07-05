---
name: me
description: >-
  MyEngine dev orchestrator. Runs the canonical intake -> architect -> developer ->
  tester -> runner -> verifier -> docs pipeline for this repository. Use for any
  engine code or content change, or when the user types /me or asks to implement
  the next feature, fix a bug, run balance/perf/content/save-compat gates, reflect
  on telemetry, or propose a process improvement. Enforces the JSON agent contracts
  and human gates defined in docs/agentic. Modes: --discuss, --spec, --feature --next,
  --bugfix, --balance, --perf, --content-validate, --save-compat, --reflect, --improve
  [--drain], --upgrade.
allowed-tools: Read, Grep, Glob, Edit, Write, Bash, Task
---

# /me — MyEngine dev pipeline

This skill is a **thin adapter**. The source of truth is `docs/agentic/`. Do not
re-derive process rules here — read canon, then route and delegate.

## 1. Intake (always first)

1. Run `powershell.exe -File scripts\me-selfcheck.ps1` — on `fail`, stop and report
   the adapter drift before doing anything else.
2. Read `.ai/DIGEST.md` if it exists (compact state digest maintained at close-out).
   Fall back to the full set — `AGENTS.md`, `STATE.md`, `.ai/handoff.md`,
   `docs/ENGINE_CONSTITUTION.md`, `Plane/README.md` + active phase — when the digest
   is missing, stale, or insufficient for this task.
3. Read `docs/agentic/PIPELINE.md` and `docs/agentic/AGENT_CONTRACTS.md`.

For repo fact-finding (locate an API, confirm a convention, find a signature to
reuse), delegate to `me-scout` (one focus per call) instead of exploring inline.

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
| `--improve` | Apply one queued proposal from `.ai/proposals/` | human before edits |
| `--improve --drain` | Apply ALL queued proposals as one batch | one human gate for the batch |
| `--upgrade` | Review agent model assignments vs newest Claude models | human before edits |

With no args: summarize the `STATE.md` "Next Exact Action" and offer `--feature --next`.

## 3. Delegate to roster subagents (writer never reviews its own work)

Use the `Task` tool with these agents (shipped in this plugin). Each returns
exactly one JSON envelope per `AGENT_CONTRACTS.md`:

- Scout facts: `me-scout` (cheap, one focus per call)
- Plan / analyze: `me-architect`
- Implement: `me-engine-developer`, `me-gameplay-designer`, `me-content-schema-designer`
- Test: `me-tester`
- Run gates: `me-runner` (Bash-only; one JSON line)
- Review: per the **Reviewer Matrix** in `docs/agentic/PIPELINE.md` — invoke a domain
  reviewer (`me-simulation-reviewer`, `me-renderer-qa`, `me-save-compat-reviewer`,
  `me-android-performance`, `me-balance-simulator`) only when the run's
  `changed_files` match its paths; `me-verifier` always runs last.
- Document: `me-docs`
- Self-improvement: `me-reflect`, `me-improve`

The writer role and the final reviewer role must never be the same agent. On
malformed structured output, allow exactly one retry, then stop with `needs_human`
— and count retried envelopes for telemetry (`malformed_json_count`).

## 4. Deterministic runner entry points (Windows / PowerShell)

- Build & tests: `.\gradlew.bat test`, `.\gradlew.bat projects`
- Gates: `scripts\me-content-validate.ps1`, `scripts\me-sim-replay.ps1`,
  `scripts\me-save-compat.ps1`, `scripts\me-benchmark.ps1`
- Telemetry: `scripts\me-record-run.ps1`; retro: `scripts\me-retro.ps1`
- Adapter self-check: `scripts\me-selfcheck.ps1`

Invoke `.ps1` scripts as `powershell.exe -File scripts\me-<name>.ps1`. Each script
emits one compact JSON object — capture it, do not re-run noisily.

## 5. Close out

- Update `STATE.md`, `.ai/handoff.md`, `Plane/README.md`, `.ai/DIGEST.md`, and
  durable docs (via `me-docs`); log adapter/skill/pipeline changes in
  `.ai/changes/agent-skill-log.md` and bump the affected plugin version.
- When a backlog spec completes: flip its card status, update
  `.claude/specs/ENGINE_ROADMAP.md`, and sync the source game bundle's
  gap-analysis/traceability status.
- Record telemetry via `scripts\me-record-run.ps1` for **every** run that reached
  delegation — pass or fail — filling `Verdict`, `Retries`, `DurationMin`,
  `MalformedJsonCount`, `GateFailures`, `AttributedAgent`, `FailureCluster`.
- If the record-run output reports `reflect_required: true` (failed run or every
  5th event), run `--reflect` now or state explicitly why it is deferred.
