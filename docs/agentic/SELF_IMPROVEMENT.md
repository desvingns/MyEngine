# MyEngine Self-Improvement

Status: Phase 04 accepted  
Last updated: 2026-07-02

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

## Telemetry Fields

Required JSONL fields:

- `run_id`
- `timestamp`
- `workflow`
- `phase`
- `agent`
- `model`
- `verdict`
- `retries`
- `changed_files`
- `metrics.tests`
- `metrics.content_validate`
- `metrics.replay`
- `metrics.save_compat`
- `metrics.benchmark`
- `metrics.frame_ms`
- `metrics.sim_ms`
- `note`
- `failure_cluster`

## Scripts

- `scripts/me-record-run.ps1` appends one event and emits one JSON object.
- `scripts/me-retro.ps1` aggregates telemetry without network or LLM.
- `scripts/me-content-validate.ps1`, `scripts/me-sim-replay.ps1`,
  `scripts/me-benchmark.ps1`, and `scripts/me-save-compat.ps1` are deterministic runner entry
  points.

## Human Gate

An improvement proposal must include evidence, target file, expected effect, rollback note, and
scope. Approval is recorded in `.ai/changes/agent-skill-log.md`.
