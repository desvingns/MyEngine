# Phase 02 - Stack ADR And Project Scaffold

Status: Done

## Цель

Выбрать stack и создать минимальный buildable scaffold. Предпочтительная гипотеза: Kotlin/JVM + libGDX + Android/Desktop modules, но она должна быть подтверждена ADR, а не принята по инерции.

## Входы

- Phase 00-01 deliverables
- `docs/REFERENCE_RESEARCH.md`
- `docs/ENGINE_CONSTITUTION.md`
- `AGENTS.md`

## Решения, которые нужно принять

- Kotlin vs Java for engine code.
- libGDX vs custom Android Canvas/OpenGL vs Unity/Godot as non-goals.
- KTX dependency or plain libGDX APIs.
- Ashley ECS as dependency vs custom minimal ECS vs no ECS in phase 1.
- gdx-ai as dependency vs using only pathfinding ideas.
- Gradle module structure.
- Package/group naming.
- Test framework and property-test approach.

## Target Scaffold

Предпочтительный layout:

```text
settings.gradle.kts
build.gradle.kts
gradle/
core/
  build.gradle.kts
  src/main/kotlin/
  src/test/kotlin/
desktop/
  build.gradle.kts
  src/main/kotlin/
android/
  build.gradle.kts
  src/main/AndroidManifest.xml
engine-core/
engine-world/
engine-content/
engine-testkit/
games/sandbox/
```

Можно выбрать другой layout, если ADR объясняет почему.

## Work Packages

### 02.1 ADR-0001 Stack

Создать `docs/DECISIONS/ADR-0001-stack.md`:

- context;
- options;
- decision;
- consequences;
- dependency/license impact;
- how references influenced decision.

### 02.2 Build Scaffold

- Создать Gradle wrapper if appropriate.
- Создать minimal modules.
- Настроить tests.
- Настроить desktop launcher placeholder.
- Настроить Android launcher placeholder.
- Добавить basic CI-like local commands in README.

### 02.3 Dependency Policy

Создать `docs/DECISIONS/ADR-0002-dependency-policy.md`:

- dependency review checklist;
- license verification;
- Android method count/performance concerns;
- when to prefer tiny local abstraction over dependency.

## Deliverables

- `docs/DECISIONS/ADR-0001-stack.md`
- `docs/DECISIONS/ADR-0002-dependency-policy.md`
- Build scaffold files
- Minimal compile/test commands documented in README

## Verification

Run what exists:

- `./gradlew projects`
- `./gradlew test`
- `./gradlew desktop:run` if a placeholder launcher exists
- `./gradlew android:assembleDebug` if Android plugin/environment is ready

If Gradle/Android SDK is unavailable, document exact failure.

## Acceptance Gates

- Stack choice is documented and references are cited.
- Project has reproducible build commands.
- At least one JVM unit test runs.
- Android module exists or a documented blocker explains why scaffold waits.
- No large game systems implemented yet.

## Standalone Prompt

```text
Ты выполняешь Phase 02: stack ADR and project scaffold.

Прочитай AGENTS, README, docs/REFERENCE_RESEARCH, docs/ENGINE_CONSTITUTION, Plane/README.

Сделай ADR-0001-stack и ADR-0002-dependency-policy. Затем создай минимальный Gradle/libGDX-style scaffold, достаточный для JVM tests and future Android launcher. Не реализуй gameplay systems beyond placeholders.

Run available build/test commands and update README + Plane/README with results.
```
