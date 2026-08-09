# MyEngine API Stability v0.1 Draft

Status: Phase 14 accepted; ENG-036 runtime/session surface added
Last updated: 2026-08-09

## Stable For First Game

- `Tick`, `TickRate`, `TickScheduler`
- `SeededRandom`
- `CommandQueue`, `EngineCommand`, `CommandId`
- `StableHash`, `HashableState`
- `TileWorld`, `TilePosition`, `WorldSize`
- `ContentPackLoader`, `ContentRegistry`
- `ProceduralMapParameters`, `ProceduralMapGenerator`, `GeneratedMap`
- `EntityId`, `Entity`, `EntityStore`

## Experimental

- `GameRuntimeDescriptor`, `GameSession`, `GameRuntimeFactory` (`engine-runtime`)
- `QueuedGameSession` and `SandboxSessionFactory`
- `SandboxRuntime`
- `DefenseRuntime`
- `ProducerSystem`
- `IncidentDirector`
- `EngineSnapshot`, `InputAdapter`, `Camera`
- `DevtoolReports`

## Internal

- Concrete game save payloads remain owned by each game adapter; the generic session only dispatches
  pending commands to the save callback.
- Sandbox save text encoding details.
- ASCII renderer output format.
- Script output wrapper fields beyond final JSON status.

## Replace Soon

- Reward handling in `DefenseRuntime`.
- Properties schema if content grows beyond simple scalar definitions.
- Text-only Android shell.
