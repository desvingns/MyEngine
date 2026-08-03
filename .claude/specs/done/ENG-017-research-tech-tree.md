id: ENG-017
title: Research/tech tree content type + unlock gating
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Tech node content type (cost, prerequisite DAG, unlock refs); validation: acyclic, refs resolve; format per the DX-008 ADR.
- `ResearchCommand` spends resources deterministically; unlocks gate build/recipe availability in-sim.
- Snapshot exposes tree state; progress persists in save (migration test); mid-run research replay hash test.

Implementation notes:
- Added optional `tech-tree.json` graph content with typed tower/building/recipe unlock refs,
  deterministic DAG/reference validation, atomic `ResearchCommand` spending, and sandbox gating.
- Exposed immutable string-only tech-tree snapshot state and persisted researched ids in
  `SandboxSaveCodec` v18; v1-v17 saves migrate with empty research state and pending research
  commands roundtrip safely.

Verification notes:
- Focused content, research, gating, snapshot, replay, and save-migration tests passed.
- Full Gradle tests/projects, content validation, replay, save-compat, benchmark, selfcheck,
  Android `assembleDebug`, and `git diff --check` passed. No device/emulator result is claimed.
