id: PROC-015
title: Reference-game evidence bridge for me-spec
status: backlog
owner: human
phase: process
source: MySD reference-spec pipeline 2026-07-18
requirements:
  - PROC-015-R1
  - PROC-015-R2
  - PROC-015-R3
  - PROC-015-R4
  - PROC-015-R5
acceptance:
  - schema_import
  - clone_strict_coverage
  - traceability
  - gap_dedup
  - public_safety
gates:
  - tests
  - evaluator
  - gate_1
  - gate_2
  - adapter_selfcheck

# Goal

Extend the canonical `/me-spec` workflow so a reference game can import a sanitized
`state-graph.v1` plus `mechanic-claims.csv`, lock an evidence-backed inventory, and bridge only
accepted reusable gaps into the MyEngine backlog.

# Requirements

## PROC-015-R1 — Versioned import

Validate and import:

- hierarchical node kinds `screen|overlay|battle_phase|meta_state`;
- parent relationships, routes, phases, semantic flags, affordances, evidence IDs, source, and
  confidence;
- edges with actions, preconditions, costs, observed effects, timings, before/after evidence,
  `observed|inferred`, and confidence;
- volatile observations separate from node identity;
- mechanic claims with controlled variables, sample count, supporting/contradicting evidence,
  confidence, and future trace links.

Unknown schema versions and broken references fail before an LLM authoring phase.

## PROC-015-R2 — Game-aware deduplication

Replace text-only state deduplication for game crawls with:

- structural signature: activity plus normalized affordances with volatile counters removed;
- visual signature: perceptual hash after declared currency/timer/HP/wave/animation masks;
- semantic signature: route, overlay, battle phase, and stable flags.

Exact HP, currency, wave, timer, and energy values remain observations.

## PROC-015-R3 — Clone-strict Gate 1

Gate 1 is blocked unless:

- all declared root routes are reached;
- the core loop reaches a terminal state;
- every visible affordance maps to an edge, explicit deviation, or blocker;
- required positive/negative states are present;
- six consecutive crawl iterations add no state, affordance, or mechanic claim;
- every inference below confidence 0.8 is in open questions.

The user accepts inventory, scope, deviations, and blockers before requirements are authored.

## PROC-015-R4 — Claim and traceability policy

An unaccepted/inferred mechanic claim cannot become an FR automatically. The accepted lineage is:

`evidence -> observation/edge -> claim -> inventory -> FR -> US -> AC -> engine gap -> test/fit`.

The evaluator treats orphan states, unmatched affordances, ungrounded requirements, and missing
trace links as blockers in clone-strict mode.

## PROC-015-R5 — Backlog bridge and public safety

Before minting or incrementing demand, scan backlog/active/done, Engine Roadmap, and API stability.
Update demand only after Gate 1 confirms the capability. Create a new ENG card only for a reusable
gap with evidence/confidence, EARS/Gherkin acceptance, deterministic ordering, save/replay impact,
content schema, performance gate, and dependency order.

Raw APKs, media, UI dumps, extracted assets, credentials, and verbatim reference copy are never
imported into MyEngine or a public bundle.

# Acceptance scenarios

```gherkin
@PROC-015-AC1
Scenario: Import a valid hierarchical game graph
  Given a valid state-graph.v1 and mechanic-claims.csv
  When me-spec performs reference intake
  Then every node, edge, observation, claim, and evidence link is preserved
  And volatile observations do not create duplicate states

@PROC-015-AC2
Scenario: Block unmatched affordances
  Given an observed state with a visible affordance
  And the affordance has no edge, deviation, or blocker
  When clone-strict Gate 1 is evaluated
  Then the gate fails with the affordance ID

@PROC-015-AC3
Scenario: Keep weak inference out of requirements
  Given a mechanic claim has confidence below 0.8
  When the inventory and requirements are generated
  Then the claim appears in open questions
  And no FR is created from it

@PROC-015-AC4
Scenario: Deduplicate an accepted engine gap
  Given an accepted inventory capability already maps to an existing ENG card
  When the backlog bridge runs
  Then no duplicate ENG card is created
  And the existing roadmap row gains the game demand and trace link
```

# Process/change impact

No simulation, save, replay, or content runtime behavior changes. If canonical contracts or plugin
adapters change, update canonical docs first, run selfcheck, log the agent/skill change, and bump the
affected plugin version according to the existing process.
