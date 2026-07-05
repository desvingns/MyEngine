id: PROC-010
title: Pipeline cost telemetry (tokens/time per run)
status: backlog
phase: process
source: architecture review 2026-07-04 (P4.3)

Context: duration_min is now recorded, but there is no token/cost signal, so reflect
cannot propose cost-side pipeline improvements (e.g. demote an agent's model).

Acceptance:
- Telemetry events optionally carry estimated token usage per agent role (orchestrator
  fills from its own accounting or a rough chars/4 estimate).
- me-retro.ps1 aggregates cost per workflow and per agent.
- At least one retro demonstrates a cost-driven improvement proposal.
