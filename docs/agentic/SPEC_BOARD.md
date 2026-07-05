# MyEngine Spec Board

Status: Phase 04 accepted; PROC-2026-07-04 improvements accepted  
Last updated: 2026-07-04

Spec work is tracked through:

- `.claude/specs/backlog`
- `.claude/specs/active`
- `.claude/specs/done`
- future `/me-spec` bundles under `games/<slug>/spec`

## Spec States

| State | Meaning |
|---|---|
| Backlog | Approved idea, not currently implemented |
| Active | Current implementation target |
| Done | Implemented, verified, and documented |

## Spec Minimum Fields

```yaml
id: ME-000
title: Short title
status: backlog
owner: human|codex|claude
phase: 05
requirements:
  - R-001
acceptance:
  - command or scenario
gates:
  - tests
  - replay
  - save_compat
```

## Backlog Bridge

Game specs created by `/me-spec` become backlog candidates only after traceability exists. Engine
gaps go to `.claude/specs/backlog` or a future `Plane/` phase; game-specific work stays under
`games/<slug>/spec`.

## Gap Dedup Rule

Before creating a new engine-gap backlog card, the spec author scans
`.claude/specs/backlog`, `.claude/specs/active`, `.claude/specs/done`, and
`docs/API_STABILITY.md`. If the capability already exists or is already tracked, the game's
`engine-gap-analysis.md` references the existing card/API instead of minting a duplicate.
(Live example: MTD-001 duplicated SG-002, which was already implemented.)

## Engine Roadmap

`.claude/specs/ENGINE_ROADMAP.md` aggregates all engine gaps across games with a demand
counter (which games asked for it). A gap demanded by two or more games outranks
single-game gaps of the same severity. The roadmap is updated at two moments:

- by the `/me-spec` backlog bridge, when new gaps are minted or an existing gap gains a
  new demanding game;
- by `/me` close-out, when a gap's card status changes (backlog -> active -> done).
