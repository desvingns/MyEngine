id: PROC-002
title: ADR — how game repos consume the engine (multi-repo strategy)
status: backlog
phase: process
source: architecture review 2026-07-04 (P2.4)

Context: MyTD lives at D:/Pet/MyTD and its spec bridges gaps into this repo's backlog,
but there is no decided mechanism for a game repo to consume engine code: composite
Gradle build vs local Maven publish + semver, and how docs/API_STABILITY.md
(Experimental vs Stable) maps to version guarantees.

Acceptance:
- One ADR under docs/DECISIONS/ choosing the consumption mechanism with trade-offs.
- Defines when an engine version is tagged and what a game may depend on (Stable only?).
- ENGINE_ROADMAP.md references the ADR for cross-repo gaps.
