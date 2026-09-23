# MyEngine Roadmap

Status: Phase 00 through Phase 14 complete  
Last updated: 2026-09-23

This roadmap mirrors `Plane/` and keeps the project staged. Do not skip ahead into a playable
game before the engine contracts and stack scaffold exist.

## Stage 0 - Foundation

| Phase | Status | Outcome |
|---:|---|---|
| 00 | Done | Reference research, borrow/reject decisions, license policy |
| 01 | Done | README, AGENTS, constitution, roadmap, `.ai` workspace |
| 02 | Done | Stack ADR, dependency policy, Gradle/libGDX-style scaffold |
| 03 | Done | Architecture contracts, API sketch, content model, testing strategy |
| 04 | Done | Agentic pipeline bootstrap |

## Stage 1 - Engine Runtime

| Phase | Status | Outcome |
|---:|---|---|
| 05 | Done | Deterministic tick loop, command queue, RNG, replay hash |
| 06 | Done | Tile world, content schemas, validation, save/load v1 |
| 07 | Done | Entities/systems, jobs/tasks, path requests |
| 08 | Done | Logistics, defense, waves, enemies, towers, minimal storyteller |
| 09 | Done | Rendering boundary, Android shell, input, debug overlay |
| 10 | Done | First playable vertical slice sandbox |

## Stage 2 - Tooling And Growth

| Phase | Status | Outcome |
|---:|---|---|
| 11 | Done | Devtools, balance runner, editor direction |
| 12 | Done | Game-spec pipeline and first sample game spec |
| 13 | Done | Self-improvement loop |
| 14 | Done | Hardening, release discipline, first game kickoff |

## Near-Term Milestones

1. ENG-036 implementation and technical gates are accepted locally (2026-09-23).
2. Resolve the reviewed local checkpoint from Git history (create it only if absent); MySD can
   pin that exact accepted SHA under ADR-0004. One coordinated telemetry event and retro are recorded.
3. Publication remains pending; do not mark the delivery board done until the canonical scoped
   commit/push gate is fulfilled. Preserve all earlier performance failures and final paired03 evidence.
4. Resume general backlog order at ENG-015.

## Current Non-Goals

- Multiplayer or real-time networking.
- Full editor.
- Large campaign/progression systems.
- Production art pipeline.
- Monetization.
- Direct copying from reference projects.
