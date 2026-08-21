# MyEngine Spec Board

Status: Phase 04 accepted; PROC-2026-07-04 improvements accepted  
Last updated: 2026-07-06

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

Phase values in use: `first-game` (game vertical slice), `engine` (engine capability),
`dx` (agent-facing tooling), `process` (pipeline/process), or a `Plane/` phase number.

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

## Queue Resolution

`/me --feature --next` uses this board deterministically. Exactly one valid card in `active/` is
resumed before any backlog work. More than one active card is a `needs_human` board conflict.
When `active/` is empty, the first row in `ENGINE_ROADMAP.md` whose matching card remains in
`backlog/` with `status: backlog` is next, provided `blocked_by` and every explicit `start_gates`
condition are satisfied. If the table does not identify an eligible card, the pipeline stops for a
human decision; it never guesses from filenames or creates a new card.

`/me --feature --next --chain` re-evaluates these same rules only after its completed card is
`done`. No remaining eligible card is a normal drained board, not a reason to open an empty task.

## Reference Evidence Bridge

`/me-spec --reference-game` accepts only a sanitized evidence root containing:

- `state-graph.v1.json` with nodes, edges, observations, coverage, and three-part signatures;
- `mechanic-claims.csv` with controlled variables, samples, evidence links, confidence, and
  promotion status;
- `evidence-index.csv` with hashes, sanitized summaries, local-only pointers, and IP/privacy
  review state.

The deterministic entry point is `powershell.exe -File scripts/me-reference-evidence.ps1`.
`-Mode validate` checks versioned import and references; `-Mode gate1` additionally checks
clone-strict coverage, human-locked scope, low-confidence claim quarantine, and signature-based
deduplication; `-Mode bridge` reports existing backlog/roadmap matches and never creates a
duplicate card. The bridge is report-only: new reusable gaps still require a human decision before
they can be written to the board.

Raw APKs, media, UI dumps, extracted assets, credentials, and verbatim reference copy are not
valid evidence inputs. Volatile observations remain top-level observations and do not define node
identity. Gate 1 accepts semantic/behavioral scope; visual completeness belongs to later Visual
Fit Gates.
