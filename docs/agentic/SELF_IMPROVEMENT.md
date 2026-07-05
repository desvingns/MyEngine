# MyEngine Self-Improvement

Status: Phase 04 accepted; PROC-2026-07-04 improvements accepted  
Last updated: 2026-07-04

Self-improvement follows:

```text
observe -> reflect -> propose -> human gate -> apply -> record -> propagate
```

## Boundaries

- Raw telemetry is append-only JSONL under `.ai/runs/`.
- Durable lessons live in `.ai/memory/` and should record why-facts, not file facts.
- Project-local lessons and pipeline-level changes are separate.
- Reflection can propose changes but cannot edit files.
- `/me --improve` requires explicit human approval before applying any proposal.
- Canonical docs update first; adapters update second.

## Mandatory Telemetry

Every `/me` run that reaches delegation records exactly one telemetry event via
`scripts/me-record-run.ps1` at close-out — **including failed, blocked, and
`needs_human` runs**. A run that ends without a recorded event is a pipeline bug.
Failures are the primary learning signal: record the `failure_cluster`, retry
counts, and which gates failed, not just successes.

## Telemetry Fields

Required JSONL fields:

- `run_id`
- `timestamp`
- `workflow`
- `phase`
- `agent`
- `model`
- `verdict` (includes `needs_human`)
- `retries`
- `changed_files`
- `metrics.tests`
- `metrics.content_validate`
- `metrics.replay`
- `metrics.save_compat`
- `metrics.benchmark`
- `metrics.frame_ms`
- `metrics.sim_ms`
- `duration_min` — approximate wall-clock minutes for the run
- `malformed_json_count` — how many malformed agent envelopes were retried
- `gate_failures` — list of gates that failed at least once during the run
- `attributed_agent` — when a verifier/tester finding traces to a specific
  agent's miss, the agent whose prompt should improve (else empty)
- `note`
- `failure_cluster`

## Reflection Cadence

`/me --reflect` is mandatory, not optional:

- after every run whose verdict is `fail`, `blocked`, or `needs_human`;
- whenever `scripts/me-record-run.ps1` reports `retro_due: true`
  (every 5 telemetry events).

The orchestrator checks the last `me-record-run` output at close-out and either
runs `--reflect` immediately or states explicitly why it is deferred.

## Proposal Queue

Reflect findings that survive review become proposal files under `.ai/proposals/`,
one file per proposal, named `PROP-YYYYMMDD-<slug>.md` with fields: `status`
(`queued|applied|rejected`), `evidence`, `target_file`, `expected_effect`,
`rollback`, `scope`. `/me --improve` applies a single proposal; `/me --improve
--drain` applies all `queued` proposals in one batch behind a single human gate.
Applied/rejected proposals keep their files (status flipped) as an audit trail.

## Agent Attribution

When `me-verifier` or `me-tester` finds a defect that an earlier agent should have
caught or not introduced, the finding names that agent in `attributed_agent`. Retro
aggregation counts attributions per agent so improvement proposals target the
specific agent prompt with the worst record, instead of guessing.

## Plugin Versioning

Every accepted improvement that changes a skill, agent, or adapter bumps the
affected plugin's `plugin.json` version (semver patch/minor) in the same change,
and the version is named in the `.ai/changes/agent-skill-log.md` entry. A stale
version with changed content means the installed plugin cache can silently
diverge from the repo.

## Memory Boundary

`.ai/memory/MEMORY.md` is the canonical store for pipeline and project lessons —
anything an agent or adapter needs. Assistant-side personal memory
(`~/.claude/projects/...`) holds only user persona/preferences and must not
duplicate pipeline lessons; when in doubt, the repo file wins.

A third tier sits ABOVE both: the second brain (`D:/Pet/brain`, private repo
`github.com/desvingns/brain`) holds knowledge that generalizes beyond MyEngine —
domain lessons, cross-pipeline (mp<->me) patterns, user-level facts. Route there via
`scope: brain-level` findings (me-reflect) flushed by me-docs to `brain/inbox/`
(append-only, `status: NEW`); promotion into curated brain files is human-gated
(`/brain promote`). The twin registry `brain/pipelines/TWINS.md` maps me artifacts to
their mp counterparts; `/brain sync-twins` stages cross-pipeline ports as proposals —
they are applied ONLY through this pipeline's normal `--improve` gates.

## Scripts

- `scripts/me-record-run.ps1` appends one event and emits one JSON object
  (including `retro_due`).
- `scripts/me-retro.ps1` aggregates telemetry without network or LLM, including
  per-agent attribution counts and gate-failure counts.
- `scripts/me-content-validate.ps1`, `scripts/me-sim-replay.ps1`,
  `scripts/me-benchmark.ps1`, and `scripts/me-save-compat.ps1` are deterministic runner entry
  points.
- `scripts/me-selfcheck.ps1` validates adapters against canon and runs at intake.

## Human Gate

An improvement proposal must include evidence, target file, expected effect, rollback note, and
scope. Approval is recorded in `.ai/changes/agent-skill-log.md`.
