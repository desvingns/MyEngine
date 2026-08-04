# MyEngine Agentic Pipeline

Status: Phase 04 accepted; PROC-2026-07-04 improvements accepted  
Last updated: 2026-07-04

## Purpose

`MyEngine` uses one canonical process for Claude and Codex. The project source of truth is this
repository: `docs/agentic/*`, `.ai/*`, `Plane/*`, and the root operating docs. Tool-specific
folders are thin adapters that point back to the canonical docs.

## Commands

| Command | Purpose | Gate |
|---|---|---|
| `/me --discuss` | Explore options and risks without editing files. | None |
| `/me --spec` | Turn an approved idea into a small implementation spec. | Human approval before code |
| `/me --feature --next` | Pick the next planned feature and run developer -> tester -> runner -> verifier -> docs. | Human approval if scope changes |
| `/me --bugfix` | Reproduce, fix, test, and document a bug. | Human approval if behavior is ambiguous |
| `/me --balance` | Run scenario/balance reports and propose content-only changes. | Human approval before content changes |
| `/me --perf` | Run benchmark/smoke checks and propose scoped optimizations. | Human approval before API changes |
| `/me --content-validate` | Validate content packs and report actionable errors. | None |
| `/me --save-compat` | Run save roundtrip/migration checks. | None |
| `/me --reflect` | Generate a deterministic retro from telemetry. | None |
| `/me --improve` | Apply one queued improvement proposal. | Human approval before edits |
| `/me --improve --drain` | Apply all queued proposals in `.ai/proposals/` as one batch. | One human approval for the batch |
| `/me --upgrade` | Review agent model assignments against newest Claude models; propose roster updates. | Human approval before edits |
| `/me-spec --greenfield-game` | Create a traceable game spec bundle from an original idea. | Two gates: inventory, final acceptance |
| `/me-spec --engine-feature` | Create a traceable engine feature spec and gap analysis. | Human approval before backlog bridge |

## Canonical Flow

1. Intake runs `scripts/me-selfcheck.ps1` (adapter drift check), then reads `.ai/DIGEST.md`
   when present — the compact state digest maintained at close-out. Full docs (`AGENTS.md`,
   `STATE.md`, `.ai/handoff.md`, the active phase, contracts) are read on demand when the
   digest is missing, stale, or insufficient for the task.
2. Architect or spec author narrows scope. For repo fact-finding (entry points, signatures,
   conventions), delegate to `me-scout` instead of burning orchestrator context.
3. Developer implements only the approved scope.
4. Tester adds or updates the narrowest useful tests.
5. Runner executes deterministic commands and emits one JSON object.
6. Domain reviewers run per the Reviewer Matrix below; then Verifier reviews boundaries,
   replay/save/content gates, and known risks.
7. Docs updates `STATE.md`, `.ai/handoff.md`, `Plane/README.md`, `.ai/DIGEST.md`, and durable
   docs. When a backlog spec is completed, its card status flips (backlog -> done) and, if the
   spec came from a game bundle, the game's `engine-gap-analysis.md` / `traceability.csv`
   status is updated too.
   A successful `--feature` run is not complete until its feature artifacts and close-out docs are
   committed as one intentional conventional commit and pushed directly to the configured `main`
   branch. Intake must start from a clean worktree (or a clearly documented,
   user-approved baseline); never stage, commit, amend, or push unrelated changes. If commit or
   push cannot succeed, leave the backlog card incomplete and report `blocked` rather than claiming
   feature completion.
8. Telemetry is appended through `scripts/me-record-run.ps1` for **every** run that reached
   delegation — pass or fail (see `SELF_IMPROVEMENT.md`, Mandatory Telemetry). If the output
   reports `retro_due: true`, or the run failed, run `--reflect` before closing.

## Reviewer Matrix

Domain reviewers are invoked **conditionally**, based on the run's `changed_files`.
A reviewer whose paths did not change is not invoked. `me-verifier` always runs.

| Changed paths (glob) | Reviewer |
|---|---|
| `engine-core/**`, `engine-entities/**`, `engine-ai/**`, `engine-defense/**`, `engine-logistics/**`, `engine-storyteller/**`, `engine-world/**` | `me-simulation-reviewer` |
| `engine-render/**`, `desktop/**` (render/input code) | `me-renderer-qa` |
| any save/serialization code (`**/save/**`, `*Save*`, `*Snapshot*` persistence) | `me-save-compat-reviewer` |
| `android/**` | `me-android-performance` |
| content packs / balance values (`**/content/**`, `*.properties` game data) | `me-balance-simulator` (report-only) |

If a change touches none of the rows (docs, scripts, specs), only `me-verifier` reviews.

Performance gate rule: when `changed_files` includes `scripts/me-benchmark.ps1`,
`config/performance-budgets.v1.json`, or `scripts/me-record-run.ps1`, `me-verifier` must require
the benchmark runner's `verdict: pass`, confirm the reported `budget_version` and measured
`metrics.sim_ms`, and reject missing required benchmark metrics. A JVM-only run may report
`frame_ms` as `not_measured`; a supplied frame value must still be checked against its budget.

## Adapter Strategy

- `claude-plugins/*` and `codex-plugins/*` are adapters, not source of truth.
- Adapter prompts must reference `docs/agentic/AGENT_CONTRACTS.md`.
- If a contract changes, update canonical docs first, then adapters, then log the change in
  `.ai/changes/agent-skill-log.md` and bump the affected plugin version
  (`SELF_IMPROVEMENT.md`, Plugin Versioning).
- No adapter may silently update itself.

## Runner Script Rule

Runner scripts emit one JSON line. If a tool produces noisy output, the script must summarize it
into one final JSON object with `status`, `command`, `exit_code`, and `notes`.

On Windows, `.ps1` entry points are invoked as `powershell.exe -File scripts\me-<name>.ps1`
(never inline script text), so invocations stay uniform across Bash and PowerShell hosts.
