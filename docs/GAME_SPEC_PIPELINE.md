# MyEngine Game Spec Pipeline

Status: Phase 12 accepted  
Last updated: 2026-07-02

## Bundle Format

Each future game uses:

```text
games/<slug>/spec/
  00_manifest.yaml
  product-brief.md
  requirements.md
  user-stories.md
  acceptance/*.feature
  design.md
  content-plan.md
  engine-gap-analysis.md
  balance-plan.md
  android-ux.md
  nfr.md
  risks.md
  traceability.csv
```

## Interview Protocol

Ask in small batches:

1. Core fantasy and what the player protects or grows.
2. Session length, failure mode, and win/loss loop.
3. Map shape, player actions, and threat model.
4. Economy/logistics, production chains, and resource sinks.
5. Progression, research, and content volume.
6. Android UX posture: touch targets, one-handed use, offline behavior.
7. Explicit out of scope.

## Traceability

Every requirement gets an `FR-*` id. User stories use `US-*`. Acceptance files use `AC-*` scenario
tags. `traceability.csv` maps:

```text
requirement_id,user_story_id,acceptance_id,design_section,engine_gap_id,engine_gap_status
```

For a completed engine-gap card, `engine_gap_status` mirrors the status in
`engine-gap-analysis.md`. MyEngine's `scripts/me-spec-sync.ps1` reads the completed card's
`source:` path and referenced `EG-*` ids, reports stale target rows as one JSON result, and only
updates those rows when `-Apply` is supplied. External writes additionally require
`-AllowExternalWrite`; report-only mode is the default.

## Engine Gap Split

Every spec separates:

- reusable engine work;
- game-specific rules/content;
- asset work;
- tooling work.

Engine gaps become `.claude/specs/backlog` candidates or future `Plane/` tasks. Game-specific
content remains under the game folder.

Before minting a gap, apply the Gap Dedup Rule and update the Engine Roadmap
(`docs/agentic/SPEC_BOARD.md`): scan existing backlog/active/done cards and
`docs/API_STABILITY.md`; reference existing capabilities instead of duplicating them,
and record which games demand each gap in `.claude/specs/ENGINE_ROADMAP.md`.

## Gates

Gate 1: user accepts feature inventory and scope.  
Gate 2: user accepts final bundle and known risks before implementation starts.
