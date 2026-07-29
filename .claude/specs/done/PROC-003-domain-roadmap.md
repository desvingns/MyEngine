id: PROC-003
title: Sequence domain engine systems (AI/jobs/storyteller) against game specs
status: done
phase: process
source: architecture review 2026-07-04 (P2.5)

Context: the engine targets TD and RimWorld-like games, but engine-ai, engine-entities,
and engine-storyteller are minimal placeholders: no flow-field pathfinding (TD core),
no job system (colony core), no incident director. Post-Phase-14 roadmap should
sequence these against the next game specs instead of abstract hardening.

Acceptance:
- Plane/ gets a phase plan that orders: flow-field/pathfinding (TD), job board + hauling
  (colony slice), storyteller incidents — each tied to a demanding game spec in
  ENGINE_ROADMAP.md.
- Each planned system has at least one named game requirement (FR-*) it unblocks.
  (amended 2026-07-29: vision:* demand tags per ENGINE_ROADMAP.md notes count as
  demand where no named game FR exists yet)

Close note (2026-07-29):
- Done via `Plane/15_domain_systems_sequencing.md`: flow-field/pathfinding already
  DONE via ENG-002 (tied to MyTD FR-003/FR-009/FR-013); colony slice ordered
  ENG-001 -> ENG-003 -> ENG-031 -> ENG-004 -> ENG-032 with a vision-only caveat and
  re-entry trigger (MySD Gate 1 via PROC-015 or an authored colony game spec);
  storyteller incidents = ENG-016 (vision-only demand plus defect fix F4).
- Adopted successor to ENG-020: ENG-010 (status effects framework, backed by named
  MyTD FR-007). Adopted chain: ENG-010 -> ENG-016 -> PROC-007 -> ENG-021 -> ENG-029
  -> ENG-012 -> ENG-007 -> ENG-018.
- Demand tags corrected: `mytd` removed from ENG-012/021/022/029 as unbacked by the
  MyTD spec bundle (verified against D:/Pet/MyTD/spec/requirements.md).
