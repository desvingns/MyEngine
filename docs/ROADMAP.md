# MyEngine Roadmap

Status: Phase 00 through Phase 14 complete  
Last updated: 2026-07-02

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

1. Signal Garden `SG-001`: create the first game content pack.
2. `SG-002`: add reward deposit hook.
3. `SG-003`: replace ASCII/text presentation with a placeholder render surface.
4. Keep sandbox tests green while first-game work proceeds.

## Current Non-Goals

- Multiplayer or real-time networking.
- Full editor.
- Large campaign/progression systems.
- Production art pipeline.
- Monetization.
- Direct copying from reference projects.
