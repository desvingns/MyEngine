# Phase 13 - Self-Improvement Loop

Status: Planned

## Цель

Закрыть observe -> reflect -> propose -> human gate -> apply -> propagate loop for `MyEngine`, inspired by `mobile-pipeline`, but specific to engine/game-development failures.

## Входы

- Phase 04 agentic pipeline
- Phase 10 vertical slice
- `.ai` workspace
- `D:\Pet\mobile-pipeline\selfimprove`

## Work Packages

### 13.1 Telemetry Schema

JSONL fields:

- run_id;
- timestamp;
- workflow;
- phase/spec id;
- agent;
- model;
- verdict;
- retries;
- changed_files;
- metrics:
  - tests;
  - content_validate;
  - replay;
  - save_compat;
  - benchmark;
  - frame_ms;
  - sim_ms;
  - allocations if available;
- note;
- failure_cluster.

### 13.2 Record Script

- append-only;
- never blocks;
- emits one JSON line;
- returns retro_due.

### 13.3 Retro Script

No LLM:

- aggregate pass rate;
- recurring failure clusters;
- flaky steps;
- slow benchmarks;
- missed gates;
- top candidate improvements.

### 13.4 Reflection Prompt/Agent

Reads retro + lessons. Outputs:

- finding;
- evidence;
- proposed minimal change;
- target file;
- expected effect;
- local vs pipeline-level.

No automatic edits.

### 13.5 Improvement Gate

- human approval;
- one change per proposal or batched drain;
- update canonical source first;
- update adapters after;
- log in `.ai/changes/agent-skill-log.md`.

### 13.6 Lessons

- `.ai/memory/MEMORY.md`;
- `.ai/memory/*.md`;
- append-mostly;
- why-facts, not derivable file facts.

## Acceptance Gates

- Runner/reviewer/verifier can record telemetry.
- Retro can run without network/LLM.
- Reflection cannot edit files automatically.
- Human gate is documented.
- At least one synthetic telemetry event can produce a retro.

## Standalone Prompt

```text
Ты выполняешь Phase 13: self-improvement loop.

Изучи `D:\Pet\mobile-pipeline\selfimprove`. Реализуй/документируй MyEngine telemetry JSONL, record script, retro script, reflection prompt/agent and gated improvement flow. Focus on engine-specific signals: replay, content validation, save compatibility, benchmark, Android smoke.

Run a synthetic telemetry -> retro test. Update Plane progress.
```

