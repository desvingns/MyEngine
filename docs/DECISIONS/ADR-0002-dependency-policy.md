# ADR-0002: Dependency Policy

Status: Accepted  
Date: 2026-07-02  
Phase: 02 - Stack ADR And Project Scaffold

## Context

`MyEngine` needs small, durable dependencies because Android footprint, deterministic simulation,
license clarity, and long-term maintainability matter more than quick feature accumulation.
ADR-0000 already blocks copying reference files and requires review for risky reuse.

## Decision

Every new production dependency needs a short review before it lands. The review can live in a
module ADR, phase doc, or pull-request note, but durable architectural dependencies should get an
ADR.

The review must answer:

- What exact problem does the dependency solve?
- Which module owns it?
- Is it production, test-only, tooling-only, or Android-only?
- What is the direct license, and are transitive licenses acceptable?
- Does it work on Android min SDK and target SDK?
- What is the expected method-count, binary-size, allocation, startup, and runtime impact?
- Does it introduce nondeterminism, threads, time access, global state, reflection, native code, or
  platform-specific behavior?
- Can it be isolated behind a small local interface?
- What is the fallback if the dependency becomes abandoned or incompatible?
- Which tests prove the dependency boundary behaves deterministically?

## License Rules

- Apache-2.0, MIT, BSD, ISC, CC0, and similarly permissive licenses are generally acceptable after
  review.
- EPL/LGPL/MPL or other weak-copyleft licenses require explicit notes about obligations, notices,
  and whether use is test-only or production.
- GPL, AGPL, unknown, custom, source-available, and local-unspecified dependencies are blocked until
  a dedicated ADR approves them.
- Direct copying or close translation of third-party files always requires a reuse ADR, even from a
  permissive project.

## Android And Runtime Rules

- Prefer dependencies that work without reflection-heavy startup paths.
- Avoid dependencies that require background threads, wall-clock time, random global state, or
  platform services inside deterministic simulation modules.
- Keep Android-only dependencies out of `engine-core`, `engine-world`, `engine-content`,
  `engine-testkit`, and future simulation modules.
- Native dependencies require a reason, ABI review, and an Android smoke test.
- UI/render dependencies belong behind `engine-render`, `android`, or devtool modules, not inside
  authoritative simulation state.

## When To Prefer A Tiny Local Abstraction

Prefer a tiny local abstraction when:

- the needed behavior is smaller than the dependency integration;
- deterministic behavior must be fully controlled;
- the dependency would leak through public engine APIs;
- Android footprint is disproportionate;
- only one or two call sites need the behavior;
- a future module contract is not stable enough yet.

Prefer a dependency when:

- the domain is mature and easy to test at the boundary;
- correctness risk is higher than integration cost;
- Android support and license obligations are clear;
- the dependency replaces a substantial custom implementation;
- the dependency can be hidden behind a stable `MyEngine` interface.

## Initial Approved Dependencies

- Kotlin and Kotlin Gradle plugins for engine and Android code.
- Gradle wrapper for reproducible builds.
- Android Gradle Plugin for the Android application module.
- libGDX core and LWJGL3 backend for future render/input work and desktop harnessing.
- Kotlin test and JUnit 5 for JVM tests.

## Consequences

- Future agents should not add convenient libraries casually.
- Dependency decisions must mention determinism, Android footprint, license, and module boundary.
- Phase 03 contracts can mark dependency boundaries before code depends on them.

