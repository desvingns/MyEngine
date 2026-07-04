# MyEngine API Sketch

Status: Draft accepted for Phase 03  
Last updated: 2026-07-02

These APIs are sketches, not production source. They define ownership and dependency direction for
later phases. All contracts are experimental until the phase that implements them marks a subset as
stable.

## Stability Labels

- Stable: may be used by game modules without churn after tests exist.
- Experimental: can change between phases.
- Internal: module-local; not imported by game modules.

## Contract Summary

| API | Owner | Dependencies | Stability | Required gates |
|---|---|---|---|---|
| `Engine` | `engine-core` | content registry, simulation | Experimental | tick/replay tests |
| `Simulation` | `engine-core` | commands, systems | Experimental | deterministic scenario tests |
| `TickScheduler` | `engine-core` | none | Experimental | fixed-step tests |
| `Command` | `engine-core` | stable IDs | Experimental | serialization/order tests |
| `World` | `engine-world` | `engine-core` | Experimental | coordinate/serialization tests |
| `EntityId` | `engine-core` or `engine-entities` | none | Experimental | stability tests |
| `System` | `engine-core` | simulation context | Experimental | ordering tests |
| `ContentRegistry` | `engine-content` | `engine-core` | Experimental | schema/reference tests |
| `SaveGame` | future persistence area | core/world/content/entities | Experimental | roundtrip/migration tests |
| `Renderer` | `engine-render` | snapshots/content | Experimental | visual smoke tests |
| `InputAdapter` | `engine-render` or platform shell | commands | Experimental | command mapping tests |
| `ScenarioRunner` | `engine-devtools` | core/content/testkit | Experimental | replay hash tests |

## Draft Kotlin Shape

```kotlin
interface Engine {
    val currentTick: Tick
    fun submit(command: Command)
    fun step(ticks: Int = 1): TickResult
    fun snapshot(): EngineSnapshot
}

interface Simulation {
    fun apply(command: Command)
    fun update(context: SimulationContext): SimulationResult
}

interface TickScheduler {
    fun advance(accumulatedSeconds: Double): List<Tick>
}

interface Command {
    val id: CommandId
    val issuedAt: Tick
    val actor: EntityId?
}

interface World {
    val size: WorldSize
    fun tileAt(position: TilePosition): TileView
    fun canOccupy(position: TilePosition): Boolean
}

@JvmInline
value class EntityId(val raw: Long)

interface EngineSystem {
    val id: String
    val order: Int
    fun update(context: SimulationContext)
}

interface ContentRegistry {
    val packId: String
    val schemaVersion: Int
    fun <T : ContentDefinition> get(id: ContentId<T>): T
}

data class SaveGame(
    val saveVersion: Int,
    val engineVersion: String,
    val contentPacks: List<ContentPackRef>,
    val tick: Tick,
    val replayHash: String,
)

interface Renderer {
    fun resize(width: Int, height: Int)
    fun render(snapshot: EngineSnapshot, interpolation: Float)
}

interface InputAdapter {
    fun onInput(event: PlatformInputEvent): List<Command>
}

interface ScenarioRunner {
    fun run(scenario: ScenarioDefinition): ScenarioResult
}
```

## Extension Points

- `EngineSystem` should allow future modules to register systems while preserving stable ordering.
- `ContentRegistry` should support multiple packs and migrations without exposing parser internals.
- `Renderer` should support debug overlays through snapshots, not direct world mutation.
- `ScenarioRunner` should become the shared entry point for replay tests, balance checks, and
  devtool simulations.
- Save/load APIs should stay separate from rendering and Android lifecycle code.

## Explicit Non-Contracts

- No API exposes mutable render objects as simulation state.
- No API accepts Android `Context`, `View`, `MotionEvent`, or lifecycle objects inside simulation
  modules.
- No API promises multiplayer determinism or network serialization in v1.
- No API copies schemas or names from reference projects.

