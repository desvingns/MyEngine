id: ENG-017
title: Research/tech tree content type + unlock gating
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Tech node content type (cost, prerequisite DAG, unlock refs); validation: acyclic, refs resolve; format per the DX-008 ADR.
- `ResearchCommand` spends resources deterministically; unlocks gate build/recipe availability in-sim.
- Snapshot exposes tree state; progress persists in save (migration test); mid-run research replay hash test.
