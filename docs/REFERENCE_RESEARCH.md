# MyEngine Reference Research

Status: Phase 00 done  
Last verified: 2026-07-02  
Inputs: `PROMPT_PACK.md`, GitHub repository metadata, and local `D:\Pet\mobile-pipeline`

This document records which open-source references may influence `MyEngine`, which parts are
out of scope, and which license guardrails apply. It is a design source, not permission to
copy code, assets, schemas, prompts, or exact content.

## Research Rules

- Use references for ideas, trade-offs, module boundaries, workflow shapes, and testing habits.
- Do not copy GPL, MPL, unknown-license, or game-specific files into `MyEngine` without a new ADR.
- Mechanics, tech trees, rulesets, and data-table structure may be cloned from references; do not copy exact IP such as names, art direction, campaign content, or UI text without a dedicated ADR.
- Prefer permissive libraries as dependencies through Gradle/Maven when they solve a real problem.
- Keep Android as the only shipping platform; desktop/JVM exists for development, tests, simulation runners, and editors.

## Repository Snapshot

GitHub metadata was sampled from default branches on 2026-07-02. Stars are not a project goal;
they are included only as a rough maturity signal.

| Repo | Tier | Branch | Language/stack | License | Layout/build notes | Priority |
|---|---|---:|---|---|---|---|
| [Anuken/Mindustry](https://github.com/Anuken/Mindustry) | A | `master` | Java, Gradle, Android/Desktop/iOS/server | GPL-3.0 | `core`, `android`, `desktop`, `server`, `tests`, `tools`, `fastlane` | High |
| [yairm210/Unciv](https://github.com/yairm210/Unciv) | A | `master` | Kotlin, Gradle KTS, LibGDX, Android/Desktop/server | MPL-2.0 | `core`, `android`, `desktop`, `docs`, `tests`, `fastlane` | High |
| [rossturner/king-under-the-mountain](https://github.com/rossturner/king-under-the-mountain) | A | `master` | Java, Gradle, LibGDX | MIT | `core`, `desktop`, `release_tools` | High |
| [rossturner/mountaincore](https://github.com/rossturner/mountaincore) | A | `master` | Java, Gradle, LibGDX | MIT | `core`, `desktop`, `raw_assets`, `release_tools` | High |
| [mafik/libcolony](https://github.com/mafik/libcolony) | A | `main` | C++ with JS/demo surface | MIT | `src`, `Makefile`, demo files | Medium |
| [Anuken/Arc](https://github.com/Anuken/Arc) | B/library | `master` | Java framework | Apache-2.0 | `arc-core`, `backends`, `extensions`, Gradle | Medium |
| [00-Evan/shattered-pixel-dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) | B | `master` | Java, LibGDX, Android/Desktop/iOS | GPL-3.0 | `core`, `android`, `desktop`, `docs`, services | Medium |
| [ogzkrt/OTD](https://github.com/ogzkrt/OTD) | B | `master` | Java, LibGDX | MIT | `android`, `desktop`, Gradle | Medium |
| [bneukom/heavydefense](https://github.com/bneukom/heavydefense) | B | `master` | Java, Android/Desktop | Unknown | `heavydefense`, `heavydefense-android`, `heavydefense-desktop` | Low |
| [OpenRA/OpenRA](https://github.com/OpenRA/OpenRA) | B | `bleed` | C#, RTS engine | GPL-3.0 | engine/game/mod separation, tests, packaging | Medium |
| [Warzone2100/warzone2100](https://github.com/Warzone2100/warzone2100) | B | `master` | C/C++, CMake | GPL-2.0 | `src`, `data`, `tests`, `tools`, `platforms` | Low |
| [freeciv/freeciv](https://github.com/freeciv/freeciv) | B | `main` | C, autotools/meson | GPL-2.0 | `common`, `server`, `client`, `data`, `ai`, `tests` | Low |
| [libgdx/gdx-liftoff](https://github.com/libgdx/gdx-liftoff) | library | `master` | Kotlin, Gradle project generator | Apache-2.0 | guides, generator source, Gradle conventions | High |
| [libktx/ktx](https://github.com/libktx/ktx) | library | `master` | Kotlin extensions for LibGDX | CC0-1.0 | many small modules, Gradle KTS | High |
| [libgdx/ashley](https://github.com/libgdx/ashley) | library | `master` | Java ECS | Apache-2.0 | `ashley`, `tests`, `benchmarks` | Medium |
| [libgdx/gdx-ai](https://github.com/libgdx/gdx-ai) | library | `master` | Java AI/pathfinding | Apache-2.0 | `gdx-ai`, `tests`, Gradle | Medium |
| [SWE-agent/SWE-agent](https://github.com/SWE-agent/SWE-agent) | agentic | `main` | Python | MIT | `sweagent`, `config`, `docs`, `tests`, `trajectories` | Medium |
| [OpenHands/OpenHands](https://github.com/OpenHands/OpenHands) | agentic | `main` | Python/TypeScript app platform | Unknown by API | `openhands`, `frontend`, `containers`, `skills`, `tests` | Medium |
| [Aider-AI/aider](https://github.com/Aider-AI/aider) | agentic | `main` | Python CLI | Apache-2.0 | `aider`, `benchmark`, `scripts`, `tests` | Medium |
| [VoltAgent/awesome-claude-code-subagents](https://github.com/VoltAgent/awesome-claude-code-subagents) | agentic | `main` | Shell/catalog | MIT | `.claude`, categories, installer | Low |
| [ComposioHQ/awesome-claude-skills](https://github.com/ComposioHQ/awesome-claude-skills) | agentic | `master` | Python/catalog | Unknown by API | many skill directories | Low |
| `D:\Pet\mobile-pipeline` | local agentic | local | Markdown, Bash, Claude/Codex adapters | Local/unspecified | `.ai`, `.claude`, `.codex`, plugin templates, selfimprove scripts | High |

## Borrow/Reject Matrix

### Mindustry

License risk: GPL-3.0 strong copyleft. Study ideas only. No code, assets, schemas, exact content
taxonomy, or build files may be copied before a dedicated license ADR.

What to borrow:

- Android/Desktop Gradle split as a development loop pattern: Android ships, desktop speeds tests and debugging.
- Broad content categories: blocks, items/resources, units/enemies, waves, tech/research, maps.
- Integration of logistics, production, and defense in one simulation vocabulary.
- Map editor/modding mindset: content should be externalized and validated early.
- Campaign/progression pressure as a long-term design axis, not a Phase 1 requirement.
- Tooling habit: keep build, release, test, and content tools visible in the repo.

What not to borrow now:

- GPL implementation details, assets, exact file formats, or exact content schemas.
- Multiplayer, server, networking determinism, and large campaign systems for v1.
- A full in-engine editor before the first deterministic vertical slice.

Architecture impact:

- `engine-content` gets versioned definitions and validators.
- `engine-logistics` and `engine-defense` must interoperate through resources, waves, and commands.
- `engine-android` remains the shipping shell; `desktop` is a dev harness only.
- `engine-devtools` should eventually host map/scenario tools, replay inspection, and balance runners.

### Unciv

License risk: MPL-2.0 weak copyleft. Files and derived schemas require careful MPL compliance
and an explicit ADR. Ideas are acceptable; direct file reuse is blocked by default.

What to borrow:

- Kotlin/LibGDX project organization for Android plus desktop development.
- Data-driven rules/mods mindset and documentation-heavy content philosophy.
- Low-end Android UX discipline: readable UI, localization, modest hardware assumptions.
- In-game explanatory documentation pattern similar to a civilopedia, adapted to original games.
- Long-lived community workflow habits: docs, tests, issue hygiene, and changelog discipline.

What not to borrow now:

- Civilization-like rules, names, techs, balance tables, diplomacy model, or any Firaxis-inspired IP.
- MPL files or exact mod/rules schemas without ADR.
- Full 4X scope, multiplayer complexity, and giant UI surface before vertical slice.

Architecture impact:

- `engine-content` needs localization hooks and explainable content metadata.
- `docs/TESTING_STRATEGY.md` later should include content-schema regression tests.
- Android UX must prefer clarity and low allocation churn over desktop-style density.

### King Under The Mountain

License risk: MIT permissive, but `MyEngine` still defaults to "learn, do not paste". Any direct
reuse must preserve copyright notices and be recorded in an ADR.

What to borrow:

- Simple `core` plus `desktop` LibGDX split as a readable starting shape.
- Settlement/colony decomposition: world, jobs, tasks, buildings, inventories, agents.
- Asset packing and release-tool awareness, even if production art is deferred.
- Simulation-first approach where rendering observes state rather than owning it.
- Colony task flow concepts: queued jobs, worker assignment, failures, and long-lived world state.

What not to borrow now:

- Desktop-first UX assumptions as shipping behavior.
- Game-specific art, lore, buildings, job names, or balance.
- Large colony scope before core deterministic tick/save/replay are proven.

Architecture impact:

- `engine-world` and `engine-ai` need job/task boundaries independent of render.
- Saves must preserve stable IDs, world state, queued commands, and agent/job state.
- Sample sandbox can prove colony primitives later without becoming a clone.

### Mountaincore

License risk: MIT permissive with the same attribution/ADR requirement for any direct reuse.

What to borrow:

- Lessons from a larger settlement simulation codebase: production, storage, buildings, jobs.
- Packaging/release tooling awareness for a long-running game project.
- Raw asset pipeline separation from processed game assets.
- The idea that simulation code needs strong organization before content scale grows.
- Storage/inventory patterns as inspiration for `engine-logistics`.

What not to borrow now:

- Full colony production chain in Phase 1.
- Desktop-centric feature surface or large UI/editor investment.
- Any game-specific content, art, names, balance, or lore.

Architecture impact:

- `engine-logistics` should model inventories, producers, consumers, and throughput.
- `engine-content` should keep content definitions versioned before content volume grows.
- Roadmap should delay large colony gameplay until the generic runtime is proven.

### LibColony

License risk: MIT permissive. Language/runtime mismatch means concepts are useful, code is not.

What to borrow:

- Autonomous job scheduling as a first-class design concern.
- Priority, assignment, and cancellation concepts for pawn/worker systems.
- Anti-micromanagement goals: agents should make local decisions from player-level intent.
- Failure handling and starvation/death-spiral prevention as simulation design topics.
- Small library mindset: a reusable scheduling core can serve multiple games.

What not to borrow now:

- C++/JS implementation details or APIs.
- A complete colony scheduler before `engine-core`, `engine-world`, and save/replay are stable.
- Hardcoded colony-only assumptions in generic engine modules.

Architecture impact:

- `engine-ai` should expose jobs/tasks as data and interfaces, not one game's hardcoded pawn brain.
- `engine-storyteller` can later use job/system telemetry to shape incidents.
- Tests should include scheduler determinism and starvation scenarios once the module exists.

## Additional Reference Notes

- Arc: useful for lightweight framework patterns and utility organization, but do not make it a dependency
  only because Mindustry uses it. Decide in stack ADR.
- Shattered Pixel Dungeon: useful mobile-first release/build/save reference; GPL blocks code copying.
- OTD: useful for a minimal LibGDX tower-defense slice under MIT; inspect for scope, not final architecture.
- Heavydefense: license was not detected by GitHub API; treat as unknown and reference only at idea level.
- OpenRA: strong reference for engine/game/mod separation, command/order models, replay determinism, and SDK thinking.
  GPL and non-Android/C# stack make it conceptual only.
- Warzone 2100 and Freeciv: useful for progression scale, rulesets, AI documentation, and large open-source project
  discipline; GPL and stack mismatch keep them conceptual.
- gdx-liftoff: high-priority source for initial Gradle/LibGDX project shape in Phase 02.
- KTX: high-priority source for Kotlin ergonomics if Kotlin/libGDX is chosen.
- Ashley: possible ECS dependency or API inspiration; decide through ADR before committing to ECS.
- gdx-ai: possible dependency for A*, pathfinding, behavior trees, and state machines; prefer dependency over
  rewriting if it fits Android footprint and deterministic needs.
- SWE-agent: reference issue-to-patch workflow, tool boundaries, and evaluation mindset.
- OpenHands: reference workspace/session/sandbox/observability concepts, but avoid cloud-platform scope creep.
- aider: reference repo-map, git-native edit loops, lint/test/fix rhythm, and concise pair-programming UX.
- Claude subagent/skill catalogs: reference taxonomy and packaging ideas only; prompts are not to be copied blindly.
- `D:\Pet\mobile-pipeline`: primary local reference for canonical markdown source, thin adapters, `.ai` workspace,
  structured payloads, telemetry, retros, and human-gated improvement propagation.

## License Risk Map

Permissive / low friction, still requiring attribution and ADR for direct reuse:

- MIT: King under the Mountain, Mountaincore, LibColony, OTD, SWE-agent, awesome-claude-code-subagents.
- Apache-2.0: Arc, gdx-liftoff, Ashley, gdx-ai, aider.
- CC0-1.0: KTX, according to GitHub API metadata.

Weak copyleft:

- MPL-2.0: Unciv. Study and adapt ideas. Direct file/schema reuse requires ADR and MPL compliance review.

Strong copyleft:

- GPL-3.0: Mindustry, Shattered Pixel Dungeon, OpenRA.
- GPL-2.0: Warzone 2100, Freeciv.

Unknown / must verify:

- Heavydefense, OpenHands, awesome-claude-skills, local mobile-pipeline license status.

Rule: GPL/MPL/unknown projects are sources of understanding, not sources of files. Direct borrowing of code,
assets, schemas, prompt text, generated adapters, or other copyrightable structure is blocked until an ADR
states exactly what is being reused, why, under which license obligations, and how compatibility is preserved.

## Concrete Reference Decisions

1. Adopt: Android-only shipping with desktop/JVM dev harness for tests, simulation runners, replay tools, and editor experiments.
2. Adopt: A Gradle multi-module shape with Android and desktop launcher modules after Phase 02 ADR approval.
3. Adapt: Mindustry-style content taxonomy into original `tiles`, `resources`, `buildings`, `towers`, `enemies`, `waves`, `recipes`, `incidents`, and `research`.
4. Adopt: Versioned, data-driven content packs from the first scaffold.
5. Adopt: Content validation as a required gate before claiming gameplay work done.
6. Adapt: Unciv-style explainable data and localization into original in-game docs and debug inspection.
7. Adopt: Simulation/render/input separation; rendering observes snapshots and never owns authoritative game state.
8. Adopt: Fixed-step simulation, seedable RNG, command queues, replay hashes, and deterministic tests.
9. Adapt: Colony job/task concepts from MIT colony references and LibColony into a generic `engine-ai` contract.
10. Adapt: Production/storage/building concepts into `engine-logistics`, but start with a minimal reusable model.
11. Adopt: Engine/game separation inspired by OpenRA, with `games/sandbox-*` as consumers of engine modules.
12. Defer: Full map editor until after a working vertical slice and replay/save gates.
13. Defer: Multiplayer, real-time networking, and server logic until after several single-player vertical slices.
14. Reject: Cloning RimWorld, Infinitode, Mindustry, Civilization, or any exact rules/content/assets.
15. Reject: GPL/MPL/unknown direct copying without a dedicated license ADR.
16. Defer: Large campaign, diplomacy, and 4X systems until a game spec proves need.
17. Adopt: `D:\Pet\mobile-pipeline` style `.ai` workspace with handoff, memory, tasks, changes, proposals, runs, and retros.
18. Adapt: mobile-pipeline structured agent contracts to a game-engine domain, keeping human gates before self-improvement.
19. Adopt: Append-only change logs for agent/skill/pipeline edits.
20. Defer: Native Claude/Codex plugin generation until the agentic pipeline phase, after engine architecture docs exist.
21. Decide later: Ashley as dependency vs custom minimal ECS vs no ECS in v1.
22. Decide later: gdx-ai dependency vs custom pathfinding primitives, based on Android footprint and determinism tests.

## Phase 01 Inputs

Phase 01 may use this document as the canonical reference summary. It should create `AGENTS.md`,
the `.ai` workspace, and a local reference policy that repeats the no-copy rule in shorter operational form.
