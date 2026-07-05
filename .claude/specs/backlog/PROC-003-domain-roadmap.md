id: PROC-003
title: Sequence domain engine systems (AI/jobs/storyteller) against game specs
status: backlog
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
