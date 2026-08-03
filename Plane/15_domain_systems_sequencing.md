# Phase 15 - Domain Systems Sequencing (PROC-003)

Status: Done (sequencing adopted 2026-07-29; documentation/phase plan only, no engine code)

## Goal

Sequence the domain engine systems (TD pathing, colony slice, storyteller incidents)
against real demand instead of abstract hardening, closing PROC-003. Each planned
system is tied to demand recorded in `.claude/specs/ENGINE_ROADMAP.md`.

## Inputs

- `.claude/specs/ENGINE_ROADMAP.md` (demand tags and counts)
- `D:/Pet/MyTD/spec/requirements.md` (named MyTD FRs)
- `docs/reviews/2026-07-06-project-and-backlog-review.md` (gap sweep, review finding F4)

## Work Packages

### 15.1 Flow-field/pathfinding (TD): DONE

- DONE via ENG-002 (accepted 2026-07-18): deterministic core-outward `GoalField`
  routing, prospective all-spawn placement rejection, same-tick reroute on
  walkability change, save field derived after restore.
- Tied to named MyTD requirements: FR-003 (path layouts), FR-009 (tick-keyed wave
  schedule), FR-013 (deterministic targeting). Reference: `D:/Pet/MyTD/spec/requirements.md`.

### 15.2 Colony slice: ordered, authored demand

Order:

1. ENG-001 - A* point-to-point pathfinding for agents.
2. ENG-003 - Job execution system (JobBoard wired into tick).
3. ENG-031 - Stockpile zones + designations mint jobs.
4. ENG-004 - First worker agent MVP (hauling); depends on ENG-031 per its card.
5. ENG-032 - Construction system (blueprint, haul, build); consumes hauling.
6. ENG-033 - Colonist needs MVP (hunger/rest); consumes the completed job, worker, and save/snapshot foundations.

Entry trigger is satisfied for ENG-033 by the human-authored, named-FR scope in
`.claude/specs/backlog/ENG-033-colonist-needs-mvp.md` (accepted 2026-08-03).
MySD Gate 1 is accepted for its TD reference inventory, but its evidence is not
treated as evidence for colony behavior. Future colony cards still require their
own named FRs or a confirmed MySD/PROC-015 bridge.

### 15.3 Storyteller incidents: ENG-016, vision-only demand

- ENG-016 - incident execution pipeline + RNG fix: wires the currently dead per-tick
  `IncidentDirector` (fresh `SeededRandom(17)` every tick) into a real incident
  pipeline; fixes review finding F4 in
  `docs/reviews/2026-07-06-project-and-backlog-review.md`.
- Demand is vision-only. SG FR-002 is already satisfied by SG-001 and is NOT demand.

## Sequencing decision (2026-07-29)

Adopted chain after ENG-020:

`ENG-010 -> ENG-016 -> PROC-007 -> ENG-021 -> ENG-029 -> ENG-012 -> ENG-007 -> ENG-018`

Rationale (one line each):

- ENG-010 (status effects framework): unique successor to ENG-020 - the only remaining
  backlog card backed by a named game FR, MyTD FR-007 (warden slow/support tower).
- ENG-016 (incident pipeline + RNG fix): demand 3 plus defect fix F4 (dead per-tick
  IncidentDirector).
- PROC-007 (save migration matrix): scheduled before the save-bumping cards because
  ENG-010 and ENG-021 bump the save codec.
- ENG-021 (save slots + autosave policy): highest remaining verified demand.
- ENG-029 (audio event hooks): highest remaining verified demand.
- ENG-012 (boss/elite enemies + wave modifiers): TD depth, vision demand.
- ENG-007 (multiple spawn points + per-wave routing): TD depth, vision demand.
- ENG-018 (endless wave generation): TD depth, vision demand.

Demand-tag corrections (verified unbacked by the MyTD spec bundle at
`D:/Pet/MyTD/spec/requirements.md`): the `mytd` tag was removed from ENG-012
(demand 2 -> 1), ENG-021 (4 -> 3), ENG-022 (2 -> 1), and ENG-029 (4 -> 3) in
`.claude/specs/ENGINE_ROADMAP.md`.

## PROC-003 criterion (b) amendment

Owner decision (2026-07-29): acceptance criterion (b) is amended - `vision:*` demand
tags (as defined in the ENGINE_ROADMAP.md notes) count as legitimate demand for
systems that have no named game FR yet. Recorded here and on the PROC-003 card.

## Verification

- Documentation/phase-plan change only; no engine code touched.
- Gates: not applicable (docs-only); board checker (`scripts/me-selfcheck.ps1`) covers
  card status/location consistency after the card move to `.claude/specs/done/`.

## Next

- ENG-010 through ENG-035 and the ENG-023 conveyor follow-up are complete; review the remaining
  accepted backlog before the next `/me --feature --next` run. ENG-036 and PROC-015 remain
  explicitly human-owned until their start gates are accepted.
