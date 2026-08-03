# MyEngine

`MyEngine` is an Android-first reusable 2D game engine/framework for a small family of future
games: colony simulation, tower defense, factory/logistics defense, and minimalist mobile strategy.

The project is not trying to become a universal engine. It should become a focused platform for
data-driven simulation games where Android is the shipping target and JVM/Desktop is a fast
development harness.

## Current Status

- Phase 00: reference research and license guardrails are complete.
- Phase 01: repository foundation and `.ai` workspace are complete.
- Phase 02: stack ADR, dependency policy, and Gradle/libGDX-style scaffold are complete.
- Phase 03: architecture contracts, API sketch, content model, and testing strategy are complete.
- Phase 04-14: agentic pipeline, deterministic runtime, content/save, entities/jobs, logistics,
  defense, presentation boundary, sandbox slice, devtools, game-spec workflow, self-improvement,
  and hardening kickoff are complete.
- Next step: review the remaining accepted backlog, assign owner/blocked_by/start gates, and select
  the next feature; ENG-036 and PROC-015 remain human-owned start-gated work.

## Product Direction

MyEngine should support games with:

- Fixed-step deterministic simulation.
- Tile/world state, entities, jobs, resources, recipes, towers, waves, incidents, and research.
- Data-driven content packs with versions, validation, localization, and migrations.
- Save/load and replayability from the start.
- Android input/rendering that observes simulation state instead of owning it.
- Development tooling for content validation, replay checks, balance scenarios, and later editor workflows.

MyEngine should not become:

- A clone of RimWorld, Mindustry, Infinitode, Civilization, or any other reference game.
- A general-purpose 3D engine.
- A multiplayer/networking project in early phases.
- A production game before the reusable engine contracts exist.

## Planned Architecture

The Phase 02 stack is Kotlin-first JVM/Android with Gradle Kotlin DSL, libGDX approved for future
render/input work, AGP 9 built-in Kotlin for Android, and desktop as a development harness only.
Phase 03 defines these planned module boundaries:

- `engine-core`: tick loop, deterministic RNG, command queue, events, replay hashes.
- `engine-world`: tile/chunk grid, terrain, occupancy, buildability, spatial queries.
- `engine-entities`: entities, components, stable IDs, serialization boundaries.
- `engine-ai`: jobs, tasks, path requests, utility/behavior hooks.
- `engine-logistics`: resources, inventories, recipes, producers, consumers, throughput.
- `engine-defense`: waves, enemies, towers, targeting, damage, status effects, upgrades.
- `engine-storyteller`: pacing, incidents, threat budget, difficulty profiles.
- `engine-content`: schemas, validation, migrations, localization, sample packs.
- `engine-render`: camera, sprite/tile drawing, overlays, debug draw.
- `engine-android`: launcher, lifecycle, gestures, save location, frame pacing.
- `engine-devtools`: sim runner, validator, replay inspector, scenario/balance tooling.
- `games/sandbox-*`: tiny sample games that prove engine use without polluting engine modules.

## Quick Start

On this machine, Android Studio provides the JBR and Android SDK, but they may not be on `PATH`.
Set them explicitly before running Gradle:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat projects
.\gradlew.bat test
.\gradlew.bat desktop:run
.\gradlew.bat android:assembleDebug
```

For Unix-like shells, use `./gradlew` and equivalent `JAVA_HOME` / `ANDROID_HOME` values.

For a new agent session, start here:

```powershell
Get-Content -Raw AGENTS.md
Get-Content -Raw STATE.md
Get-Content -Raw .ai\handoff.md
Get-Content -Raw games\signal-garden\ROADMAP.md
Get-Content -Raw .claude\specs\backlog\SG-001-content-pack.md
```

Then implement the next approved first-game spec.

## Documentation Map

- `AGENTS.md`: canonical operating rules for Claude/Codex-style agents.
- `STATE.md`: current status, next action, blockers.
- `DOCUMENTATION.md`: project history and feature/change log.
- `Plane/README.md`: phase index and progress log.
- `docs/REFERENCE_RESEARCH.md`: Phase 00 reference matrix and decisions.
- `docs/DECISIONS/ADR-0000-license-policy.md`: license and no-copy policy.
- `docs/DECISIONS/ADR-0001-stack.md`: runtime/build/module stack decision.
- `docs/DECISIONS/ADR-0002-dependency-policy.md`: dependency review policy.
- `docs/ARCHITECTURE.md`: module graph and runtime/data flows.
- `docs/API_SKETCH.md`: draft engine API ownership and stability.
- `docs/CONTENT_MODEL.md`: draft versioned content model.
- `docs/content-schemas/PROPERTIES_SCHEMA.md`: v0.1 content properties schema.
- `docs/agentic/`: `/me` and `/me-spec` pipeline contracts.
- `docs/GAME_SPEC_PIPELINE.md`: reusable game spec bundle workflow.
- `docs/API_STABILITY.md`: v0.1 API classification.
- `docs/HARDENING_AUDIT.md`: hardening audit and backlog.
- `docs/RELEASE_CHECKLIST.md`: v0.1 release checklist.
- `docs/TESTING_STRATEGY.md`: test gates by change type.
- `docs/contracts/`: per-module responsibility contracts.
- `docs/ENGINE_CONSTITUTION.md`: non-negotiable engine invariants.
- `docs/ROADMAP.md`: staged roadmap aligned with `Plane/`.
- `.ai/handoff.md`: cross-session handoff.
- `.ai/memory/MEMORY.md`: durable project memory index.
- `games/sandbox/README.md`: current playable sandbox notes.
- `games/signal-garden/spec/`: first sample game spec bundle.

## License And Reference Safety

`MyEngine` has not selected a final repository license yet. Until then:

- References are sources of ideas, not copy sources.
- GPL, MPL, unknown-license, local-unspecified, and custom-license material must not be copied.
- Any direct reuse of code, assets, schemas, build files, prompts, generated adapters, or data tables requires a dedicated ADR.

See `docs/DECISIONS/ADR-0000-license-policy.md`.
