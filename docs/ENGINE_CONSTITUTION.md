# MyEngine Engine Constitution

Status: Accepted  
Last updated: 2026-07-02

These rules are intentionally stronger than preferences. Changing them requires an ADR and human
approval.

## 1. Android Is The Shipping Platform

Android is the only release target. Desktop/JVM exists to improve development speed through test
runners, debug harnesses, simulation tools, replay inspection, benchmarks, and possible editor
experiments.

## 2. Simulation Is Authoritative

The simulation owns game truth. Rendering, input, audio, UI, and Android lifecycle code may observe
or submit commands, but they must not mutate authoritative world state directly.

## 3. Determinism Matters

Core gameplay systems must be deterministic where practical:

- fixed-step ticks;
- explicit system ordering;
- seedable RNG;
- command queue/log;
- stable IDs;
- replay hashes;
- tests that can rerun scenarios.

## 4. Content Is Data-Driven

Tiles, resources, buildings, towers, enemies, recipes, waves, incidents, research, localization,
and scenario rules should be represented as versioned content definitions whenever practical.
Content packs need validation before they are used by gameplay.

## 5. Saves Are Versioned

Save files start at version 1. Persistence must include version fields, migration boundaries, and
roundtrip tests. Save compatibility is an engine concern, not a game-specific afterthought.

## 6. Tests Before Done

No non-trivial engine behavior is done without an appropriate gate:

- deterministic unit/scenario tests for simulation;
- content validation tests for schemas and sample packs;
- replay tests for command/tick systems;
- save roundtrip/migration tests for persistence;
- Android smoke/performance checks for Android/render/input changes.

## 7. Multiple Games, One Engine

The engine must not hardcode a single game. Sample games prove the engine but do not define engine
internals. Game-specific content belongs under game modules or content packs.

## 8. Clones Allowed, Not Verbatim IP

Mechanics, rules, tech trees, and structure may be cloned closely from references. Do not copy
exact IP — art, names, UI strings, lore, or distinctive content — from reference games without
a dedicated ADR.

## 9. License Safety Comes First

GPL, MPL, unknown-license, custom-license, and local-unspecified material is study-only by default.
Direct reuse requires an ADR that documents source, license, obligations, attribution, compatibility,
and alternatives.

## 10. Human-Gated Self-Improvement

Agentic self-improvement must follow:

observe -> reflect -> propose -> human approval -> apply -> record -> propagate

No agent may silently rewrite its own instructions, adapters, skills, or pipeline contracts without
evidence and a human gate.
