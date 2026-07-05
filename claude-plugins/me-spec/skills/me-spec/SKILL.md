---
name: me-spec
description: >-
  MyEngine spec author. Builds a traceable game or engine-feature spec bundle and
  bridges accepted specs to the backlog. Use when the user types /me-spec or asks to
  create or update a game design spec, a greenfield game concept, an engine feature
  spec plus gap analysis, or a spec bundle for handoff to /me. Two human gates
  (inventory, final acceptance). Modes: --greenfield-game, --engine-feature.
allowed-tools: Read, Grep, Glob, Write, Task
---

# /me-spec — MyEngine spec pipeline

Thin adapter. Source of truth: `docs/agentic/SPEC_BOARD.md` and
`docs/GAME_SPEC_PIPELINE.md`. Read them before creating or updating a bundle.

## Intake

Read `AGENTS.md`, `STATE.md`, `.ai/handoff.md`, then the two spec docs above.

## Modes (from `$ARGUMENTS`)

| Mode | Purpose | Gates |
|---|---|---|
| `--greenfield-game` | Original idea -> traceable game spec bundle under `games/<slug>/spec` | inventory + final acceptance |
| `--engine-feature` | Engine feature spec + gap analysis -> backlog bridge | human before backlog bridge |

## Rules

- Reference games may be cloned for mechanics/flow; do not copy reference
  code/assets/schemas/IP directly (see `AGENTS.md` invariants and
  `docs/REFERENCE_RESEARCH.md`).
- A spec becomes a backlog candidate only after traceability exists
  (`SPEC_BOARD.md` -> Backlog Bridge). Engine gaps go to `.claude/specs/backlog`;
  game work stays under `games/<slug>/spec`.
- Gap dedup is mandatory (`SPEC_BOARD.md` -> Gap Dedup Rule): scan
  backlog/active/done cards, `.claude/specs/ENGINE_ROADMAP.md`, and
  `docs/API_STABILITY.md` before minting a gap; reference existing capabilities
  instead of duplicating. The backlog bridge updates the roadmap's demand counts.
- Minimum spec fields are defined in `SPEC_BOARD.md`
  (`id, title, status, owner, phase, requirements, acceptance, gates`).

## Delegate

Use the `Task` tool with `me-game-spec-author` (writer). Keep authoring and review
in separate roles. Each agent returns exactly one JSON envelope per
`AGENT_CONTRACTS.md`.

## Close out

Log spec/adapter changes in `.ai/changes/agent-skill-log.md`; update `STATE.md`
and `.ai/handoff.md` when a bundle is accepted.
