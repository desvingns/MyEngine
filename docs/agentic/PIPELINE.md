# MyEngine Agentic Pipeline

Status: Phase 04 accepted; PROC-2026-07-04 improvements accepted  
Last updated: 2026-08-21

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
| `/me --feature --next --chain` | Run the same backlog feature flow, then (Codex only) continue with one fresh local task when another runnable card remains. | Human approval if scope changes |
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

## Feature queue and `--chain`

`--chain` is valid only in the exact selector `/me --feature --next --chain`. Reject a
standalone `--chain`, `--feature --chain`, and every combination with another mode before touching
the board. It is a conveyor for already-approved cards, never authorization to create a card,
skip a gate, or guess an order.

First run `powershell.exe -NoProfile -File scripts\me-spec-board-check.ps1`. Resolve the feature
queue as follows:

1. A single valid card in `.claude/specs/active/` always wins: resume it. Two or more active cards
   are `needs_human`; do not choose between them.
2. With no active card, read the `ENGINE_ROADMAP.md` capability table from top to bottom and choose
   the first matching card still in `.claude/specs/backlog/` with `status: backlog`. The card must
   have no unsatisfied `blocked_by` or `start_gates` condition. This table order is the authoritative
   `--next` order; never substitute filename order or an inferred priority.
3. If no runnable card remains, report a normally drained board and stop. If an eligible card cannot
   be determined from the board or its gates, report `needs_human` and stop.

For the exact `--chain` selector, continue only after the current card has passed every applicable
gate, moved to `done`, synchronized its roadmap/source status, recorded telemetry, and the scoped
feature commit has been pushed to `main`. Re-run the board check and queue resolution before a
handoff: an empty or ambiguous queue never creates an empty task.

In Codex, additionally confirm `git branch --show-current` returns `main`. Do not switch branches.
Resolve the current saved project with `list_projects`, verify that it is this repository, and
create a new Codex task with `create_thread` using
`target: { type: "project", projectId, environment: { type: "local" } }`. Omit
`startingState`, `model`, and `thinking`, then send the new task exactly this prompt:

```text
Run $me --feature --next --chain now. Work directly in the current main checkout; do not create a worktree or Git branch. If no active or runnable backlog card remains, report the drained board and stop.
```

The new task starts from the project's default `main` checkout with an empty conversation. It must
not inherit any turns or parent context and must not create a Git worktree or branch. If project
resolution, task creation, or sending the prompt fails, report that once and do not retry by opening
another task. Claude has no equivalent task-creation operation: after a successful close it reports
the same command for the user to run manually and stops.

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
   The deterministic source check is `powershell.exe -NoProfile -File scripts\me-spec-sync.ps1 -CardPath
   <completed-card>`. It is report-only by default; use `-Apply`, and for an external source also
   `-AllowExternalWrite`, only after the card/roadmap board check passes.
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
