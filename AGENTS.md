# AGENTS.md - Working On MyEngine

This is the canonical operating guide for AI agents working in this repository. Read it before
editing files. Keep it concise, current, and aligned with `STATE.md` and `.ai/handoff.md`.

## Start Checklist

Read in this order:

1. `AGENTS.md`
2. `STATE.md`
3. `.ai/handoff.md`
4. `docs/ENGINE_CONSTITUTION.md`
5. `docs/REFERENCE_RESEARCH.md`
6. `Plane/README.md`
7. The current phase file in `Plane/`

Use `rg`/`rg --files` for repository search. If a file already exists, read it before editing it.
Do not overwrite user or agent changes you did not make.

For the default headless debugging step, run `.\gradlew.bat --quiet :engine-devtools:run --args "inspect sandbox default games/sandbox/content/sandbox 35"`.
The generic form is `inspect <factory> <scenario> <pack> <ticks> [script] [seed]`.
The optional inspection command script uses one command per line in the form
`tick:build_tower:towerId:x:y`; the legacy `inspect <ticks> [pack] [script] [seed]` form remains accepted.

## What This Repo Is

`MyEngine` is an Android-first reusable 2D simulation/game framework for future games in colony
simulation, tower defense, factory/logistics defense, and minimalist mobile strategy.

The project should build reusable engine capabilities first, then prove them through tiny sandbox
games. It is not a single hardcoded game.

## Non-Negotiable Invariants

- Android is the only shipping platform.
- Desktop/JVM is allowed for dev harnesses, tests, simulation runners, debug tools, and editors.
- Simulation, rendering, input, and persistence stay separated.
- Rendering/input do not own authoritative game state.
- Core simulation must be deterministic where practical: fixed tick, seedable RNG, command log,
  replay hash, stable system ordering.
- Content is data-driven and versioned from the beginning.
- Saves are versioned from v1 and migration-aware.
- Significant engine behavior needs tests before being called done.
- Reference projects may be used for ideas and as a basis for clones; original code/assets/schemas/IP must still not be copied without a dedicated ADR.
- GPL/MPL/unknown-license material is never copied without a dedicated ADR.
- Agentic self-improvement requires evidence, a proposal, and a human gate.

## Reference And License Rules

Use `docs/REFERENCE_RESEARCH.md` and `docs/DECISIONS/ADR-0000-license-policy.md` as the source of
truth. Short version:

- Ideas, trade-offs, and original summaries are allowed.
- Direct reuse of files, schemas, prompt text, generated adapters, assets, data tables, or exact
  game content requires an ADR.
- GPL, MPL, unknown-license, and unspecified local sources are study-only by default.

## Engineering Rules

- Prefer the repository phase plan over inventing a new process.
- Keep changes scoped to the active phase.
- Do not create production engine code before a phase asks for it.
- Record important decisions in ADRs or docs, not only in chat.
- Keep module boundaries testable without Android.
- Prefer structured parsers/APIs over ad hoc text manipulation for structured data.
- Add abstractions only when they remove real complexity or match an established local pattern.
- Update `STATE.md` and `.ai/handoff.md` at the end of substantial work.

## Testing And Verification

Early phases are documentation-only, so verification may be file/acceptance checks. Once code
exists, every non-trivial change should include the narrowest useful verification:

- Unit tests for core algorithms and system ordering.
- Replay determinism tests for simulation changes.
- Content schema validation tests for content changes.
- Save/load roundtrip and migration tests for persistence changes.
- Android smoke/performance checks when Android shell or rendering changes.

If a check cannot run, document why in the final report and in `STATE.md` or `.ai/handoff.md`.

## Progress Protocol

At the end of each phase:

- Update `Plane/README.md` status table and progress log.
- Update `STATE.md`.
- Update `.ai/handoff.md` with DONE, DECISIONS, NEXT, BLOCKERS, and VERIFICATION.
- Add durable lessons to `.ai/memory/MEMORY.md` only when they are useful beyond the current task.
- Lessons that generalize beyond MyEngine (domain / cross-pipeline / user-level): append a candidate
  to `D:/Pet/brain/inbox/` (second brain, entry point `D:/Pet/brain/INDEX.md`; this repo's card:
  `brain/projects/myengine.md`). Promotion is human-gated (`/brain promote`); skip if the repo is absent.
- Log agent/skill/pipeline changes in `.ai/changes/agent-skill-log.md`.

## Current Next Step

The original Phase 00-14 plan is complete, Signal Garden kickoff cards are done, and the colony
slice is complete through ENG-033. The next project step is to inspect the roadmap/backlog board
and select the next accepted feature; do not use the historical SG-001 path above.
