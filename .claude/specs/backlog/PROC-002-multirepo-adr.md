id: PROC-002
title: ADR — how game repos consume the engine (multi-repo strategy)
status: done
owner: codex
phase: process
source: architecture review 2026-07-04 (P2.4)
requirements:
  - PROC-002-R1
  - PROC-002-R2
  - PROC-002-R3
gates:
  - adr
  - consumer_ci
  - api_stability

Context: MyTD lives at D:/Pet/MyTD and its spec bridges gaps into this repo's backlog,
but there is no decided mechanism for a game repo to consume engine code: composite
Gradle build vs local Maven publish + semver, and how docs/API_STABILITY.md
(Experimental vs Stable) maps to version guarantees.

Acceptance:
- [x] ADR-0004 chooses Gradle composite build plus a full pinned MyEngine commit SHA and records
  the Maven-publication trade-off.
- [x] ADR-0004 defines Stable/Experimental/Internal cross-repo usage, accepted-commit lock updates,
  engine compatibility tags, and game release tags containing the exact pin.
- [x] The consumer CI contract reads the same lock, checks out the exact SHA, verifies HEAD, and
  runs the composite build.
- [x] ENGINE_ROADMAP.md references ADR-0004 for cross-repository capabilities.

Verification:
- MySD repository foundation includes `gradle/myengine.lock`, composite dependency substitution,
  and an exact-SHA CI checkout.
- This process card changes no simulation, content schema, save format, or replay hash.
