# ADR-0001: Runtime Stack And Project Scaffold

Status: Accepted  
Date: 2026-07-02  
Phase: 02 - Stack ADR And Project Scaffold

## Context

`MyEngine` needs an Android shipping shell, fast JVM/Desktop development loops, deterministic
simulation tests, and a data-driven content pipeline. Phase 00 identified libGDX-style Android plus
desktop splits as a useful pattern in permissive and study-only references, but ADR-0000 blocks
copying files or schemas from those projects.

Version pins were checked against primary package metadata on 2026-07-02:

- Gradle service metadata: https://services.gradle.org/versions/current
- Google Maven Android Gradle Plugin metadata: https://dl.google.com/dl/android/maven2/com/android/application/com.android.application.gradle.plugin/maven-metadata.xml
- Kotlin Gradle plugin metadata: https://plugins.gradle.org/m2/org/jetbrains/kotlin/jvm/org.jetbrains.kotlin.jvm.gradle.plugin/maven-metadata.xml
- libGDX Maven metadata: https://repo1.maven.org/maven2/com/badlogicgames/gdx/gdx/maven-metadata.xml
- JUnit Maven metadata: https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/maven-metadata.xml

## Options Considered

- Kotlin vs Java for engine modules.
- libGDX vs custom Android Canvas/OpenGL vs Unity/Godot.
- KTX now vs plain libGDX APIs first.
- Ashley ECS now vs custom minimal ECS later vs no ECS in the scaffold.
- gdx-ai now vs pathfinding/AI dependency review later.
- Single module vs explicit engine/game/launcher modules.

## Decision

- Use Kotlin-first engine code on JVM/Android. Java remains acceptable for interop if a library or
  Android API makes it clearer.
- Use Gradle Kotlin DSL with a checked-in Gradle wrapper.
- Use AGP 9 built-in Kotlin support for the Android app module instead of the removed
  `org.jetbrains.kotlin.android` plugin.
- Approve libGDX as the rendering/input/game-loop library family for the project, while keeping
  Phase 02 launchers as placeholders.
- Do not add KTX in Phase 02. Prefer plain libGDX APIs until a concrete API pain appears.
- Do not add Ashley in Phase 02. ECS/entity architecture is a Phase 03/07 contract decision.
- Do not add gdx-ai in Phase 02. Pathfinding and behavior helpers need deterministic tests before a
  dependency is accepted.
- Use package/group `dev.myengine` until a public distribution identity is chosen.
- Create these initial modules:
  - `engine-core`: deterministic runtime contracts and core utilities.
  - `engine-world`: world/tile contracts.
  - `engine-content`: content schema and validation contracts.
  - `engine-testkit`: deterministic test helpers.
  - `games/sandbox`: tiny sample-game consumer.
  - `desktop`: JVM development harness.
  - `android`: Android shipping shell placeholder.
- Use Kotlin test plus JUnit 5 for JVM unit tests. Property/scenario testing starts as a local
  `engine-testkit` concern; new test-framework dependencies require ADR-0002 review.

## Dependency And License Impact

- Kotlin, Android Gradle Plugin, Gradle, and libGDX are acceptable as package-manager dependencies
  for this stack decision. The Android module uses AGP built-in Kotlin as documented by Android
  Developers for AGP 9 and newer.
- JUnit 5 is accepted as a test-only dependency. Its EPL-2.0 license must be tracked in the future
  notices audit before public release.
- GPL/MPL/unknown reference projects remain study-only. No build files, code, schemas, assets, or
  prompts were copied from references.
- Adding KTX, Ashley, gdx-ai, serialization libraries, JSON/YAML parsers, or property-test libraries
  requires the dependency checklist in ADR-0002.

## Reference Influence

- gdx-liftoff influenced the Android/Desktop/libGDX project shape, not copied files.
- Unciv and Mindustry reinforced Android plus desktop development loops; their MPL/GPL files are
  not reused.
- King Under The Mountain, Mountaincore, and LibColony reinforced simulation-first module thinking.
- OpenRA influenced the engine/game separation and replay-determinism emphasis at the concept level.

## Consequences

- Android remains the only shipping platform, but most engine modules stay JVM-testable without
  Android.
- Phase 03 can document APIs and dependencies before production runtime implementation begins.
- More modules will be added when their phase starts; the scaffold avoids empty production packages
  for AI/logistics/defense/storyteller/render/devtools until their contracts are stable.
- The current Android activity and desktop main are placeholders only.
