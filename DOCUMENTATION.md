# MyEngine Documentation Log

This file records major project-facing changes. It is not a replacement for ADRs, `STATE.md`, or
the phase plan.

## 2026-07-02 - Phase 00: Reference Research

Created the reference and license guardrail baseline:

- `docs/REFERENCE_RESEARCH.md`
- `docs/DECISIONS/ADR-0000-license-policy.md`

Key outcomes:

- Captured 20+ game/library/agentic references.
- Documented detailed borrow/reject/impact notes for Tier A references.
- Established the no-copy default for GPL, MPL, unknown-license, and local-unspecified sources.
- Listed concrete adopt/adapt/reject/defer decisions for later phases.

## 2026-07-02 - Phase 01: Repository Foundation

Created the repository foundation:

- `README.md`
- `AGENTS.md`
- `STATE.md`
- `DOCUMENTATION.md`
- `docs/ROADMAP.md`
- `docs/ENGINE_CONSTITUTION.md`
- `.ai/` workspace files

Key outcomes:

- New agents can discover the project purpose, phase plan, constraints, and next action.
- Engine invariants are centralized.
- `.ai` contains handoff, memory, tasks, changes, proposals, runs, and retros.
- Phase 02 is the next recommended step.

## 2026-07-02 - Phase 02: Stack ADR And Project Scaffold

Created the initial buildable scaffold:

- `docs/DECISIONS/ADR-0001-stack.md`
- `docs/DECISIONS/ADR-0002-dependency-policy.md`
- Gradle wrapper and version catalog
- `engine-core`, `engine-world`, `engine-content`, `engine-testkit`
- `games/sandbox`, `desktop`, and `android`

Key outcomes:

- Selected Kotlin-first JVM/Android with libGDX approved for future render/input work.
- Used AGP 9 built-in Kotlin support for the Android module.
- Added the first JVM unit test.
- Verified `projects`, `test`, `desktop:run`, and `android:assembleDebug`.

## 2026-07-02 - Phase 03: Engine Architecture Contracts

Created the architecture contract baseline:

- `docs/ARCHITECTURE.md`
- `docs/API_SKETCH.md`
- `docs/CONTENT_MODEL.md`
- `docs/TESTING_STRATEGY.md`
- `docs/contracts/*.md`

Key outcomes:

- Documented dependency graph and no-Android-in-simulation boundary.
- Defined draft API ownership, stability, extension points, and test gates.
- Documented content versioning, migrations, and replay-hash testing expectations.

## 2026-07-02 - Phase 04-14: Runtime, Sandbox, Tooling, Spec, Hardening

Implemented the remaining phase plan through the first hardened sandbox foundation:

- Agentic pipeline docs and repo-local Claude/Codex adapter stubs.
- Deterministic core runtime with ticks, seeded RNG, commands, events, scenario runner, and stable
  hash tests.
- Tile world, external content pack loader/validator, and v1 sandbox save/load codec.
- Entity store, grid pathfinding, generic job board, logistics producer system, defense runtime,
  and incident director.
- Snapshot/input/camera boundary plus desktop ASCII and Android text smoke shells.
- Playable sandbox content pack under `games/sandbox/content/sandbox`.
- Devtools JSON reports for content validation, replay inspection, balance metrics, and save compat.
- Game-spec workflow and sample `Signal Garden` bundle.
- Telemetry/retro scripts and hardening/release docs.

Key outcomes:

- `.\gradlew.bat test`, `desktop:run`, `android:assembleDebug`, and runner scripts pass.
- The original `Plane/` phases 00-14 are complete.
- Next work moves to first-game backlog specs under `.claude/specs/backlog/SG-*`.
