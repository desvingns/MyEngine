# MyEngine Agent Contracts

Status: Phase 04 accepted; PROC-2026-07-04 improvements accepted  
Last updated: 2026-07-04

Every LLM agent returns exactly one JSON object or exactly one `BRAINSTORM` block. Invalid
structured output receives one retry. Writer and final reviewer are always separate roles.

## Common JSON Envelope

```json
{
  "agent": "me-runner",
  "verdict": "pass",
  "summary": "One sentence.",
  "changed_files": [],
  "findings": [],
  "next": [],
  "telemetry": {
    "workflow": "feature",
    "phase": "05",
    "tests": "pass",
    "replay": "pass",
    "save_compat": "not_run",
    "benchmark": "not_run"
  }
}
```

Allowed `verdict`: `pass`, `fail`, `partial`, `blocked`, `needs_human`.

## Brainstorm Block

```text
BRAINSTORM
problem: ...
options:
- id: A
  summary: ...
  tradeoffs: ...
recommendation: A
open_questions:
- ...
END_BRAINSTORM
```

## Agent Roster

| Agent | Writes files | Contract focus |
|---|---:|---|
| `me-architect` | No | Options, dependency direction, ADR need |
| `me-scout` | No | Cheap fan-out fact-finding: verified `file:line` facts, no review |
| `me-engine-developer` | Yes | Scoped implementation, changed files, risks |
| `me-gameplay-designer` | Yes | Mechanics/content may clone references, verbatim IP needs ADR |
| `me-simulation-reviewer` | No | Determinism, system order, replay hash |
| `me-android-performance` | No | Android shell, lifecycle, frame budget |
| `me-content-schema-designer` | Yes | Content fields, validation, migrations |
| `me-balance-simulator` | No | Scenario metrics, deltas, content-only proposal |
| `me-renderer-qa` | No | Snapshot boundary, camera/input, visual smoke |
| `me-save-compat-reviewer` | No | Save versioning, roundtrip, future version failure |
| `me-tester` | Yes | Tests only, no production code |
| `me-runner` | No | Commands run, one JSON result |
| `me-verifier` | No | Final gate, missing tests, boundary violations |
| `me-docs` | Yes | STATE/handoff/Plane/docs updates |
| `me-reflect` | No | Retro findings from telemetry |
| `me-improve` | No | Proposed process changes only |
| `me-game-spec-author` | Yes | Game spec bundle and traceability |

## Required Role Schemas

### Developer

```json
{
  "agent": "me-engine-developer",
  "verdict": "pass",
  "summary": "",
  "changed_files": ["path"],
  "behavior": ["implemented behavior"],
  "tests_needed": ["test or gate"],
  "risks": []
}
```

### Runner

```json
{
  "agent": "me-runner",
  "verdict": "pass",
  "commands": [
    {"command": ".\\gradlew.bat test", "exit_code": 0, "result": "pass"}
  ],
  "metrics": {
    "tests": "pass",
    "content_validate": "pass",
    "replay": "pass",
    "save_compat": "pass",
    "benchmark": "not_run",
    "sim_ms": null,
    "frame_ms": null
  },
  "notes": []
}
```

### Verifier

```json
{
  "agent": "me-verifier",
  "verdict": "pass",
  "boundary_checks": {
    "android_free_simulation": true,
    "render_snapshot_only": true,
    "content_external": true,
    "save_versioned": true
  },
  "findings": [
    {"finding": "", "severity": "low", "attributed_agent": ""}
  ],
  "manual_checks": []
}
```

`attributed_agent` names the roster agent whose miss produced the finding (e.g. a boundary
violation the developer introduced, a test the tester should have written). Empty when no
single agent is responsible. Attributions feed telemetry and retro aggregation
(`SELF_IMPROVEMENT.md`, Agent Attribution).

### Scout

```json
{
  "agent": "me-scout",
  "verdict": "pass",
  "facts": [
    {"fact": "", "file": "path", "line": 0}
  ],
  "not_found": []
}
```

### Reflect

```json
{
  "agent": "me-reflect",
  "verdict": "pass",
  "findings": [
    {
      "finding": "",
      "evidence": "",
      "proposed_minimal_change": "",
      "target_file": "",
      "attributed_agent": "",
      "scope": "project-local"
    }
  ]
}
```

Allowed `scope`: `project-local`, `pipeline-level`, `brain-level`.

`brain-level` findings generalize beyond MyEngine (domain lessons, cross-pipeline patterns,
user-level facts). They are NOT applied here: `me-docs` appends them at close-out to
`D:/Pet/brain/inbox/<YYYY-MM-DD>-myengine.md` (format: `brain/inbox/README.md`; `status: NEW`);
promotion into curated brain files is human-gated via `/brain promote` — never edit
`brain/core|domains|pipelines` directly. Reflect findings and Improve proposals may also carry
an optional `twin_applicability` object
`{"applicable":"yes|no|unknown","twin_target":"<mp artifact per D:/Pet/brain/pipelines/TWINS.md>","why":""}`
so `/brain sync-twins` can stage ports into the twin mp pipeline (applied only by its own
`/mp --improve --drain` gate).

## Runner Invocation Rule

`me-runner` invokes PowerShell gate scripts as `powershell.exe -File scripts\me-<name>.ps1`
(one uniform form from any host shell) and never inlines script bodies. Retried malformed
envelopes are counted and reported so close-out telemetry can record `malformed_json_count`.

## Retry Rule

If an agent returns malformed JSON, mixed prose plus JSON, or multiple objects, the orchestrator asks
for exactly one retry. A second invalid answer stops with `needs_human`.
