id: PROC-010
title: Pipeline cost telemetry (tokens/time per run)
status: done
owner: codex
blocked_by: none
start_gates:
  - baseline_selfcheck_pass
  - telemetry_contract_tests
phase: process
source: architecture review 2026-07-04 (P4.3)

Context: duration_min is now recorded, but there is no token/cost signal, so reflect
cannot propose cost-side pipeline improvements (e.g. demote an agent's model).

Acceptance:
- Telemetry events optionally carry estimated token usage per agent role (orchestrator
  fills from its own accounting or a rough chars/4 estimate).
- me-retro.ps1 aggregates cost per workflow and per agent.
- At least one retro demonstrates a cost-driven improvement proposal.

Implementation notes:
- `me-record-run.ps1` accepts explicit `agent=tokens` pairs, an orchestrator-supplied
  estimate, or deterministic `chars_per_4` fallback and stores the source with the event.
- `me-retro.ps1` aggregates estimated tokens by workflow and agent and emits a model-review
  proposal that remains human-gated through `/me --improve`.
- Contract coverage is in `scripts/tests/me-cost-telemetry.tests.ps1` with a fixed JSONL fixture;
  selfcheck and pre-push execute the contract test.
