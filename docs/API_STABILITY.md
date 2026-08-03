# MyEngine API Stability v0.1 Draft

Status: Phase 14 accepted  
Last updated: 2026-07-02

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

- `SandboxRuntime`
- `DefenseRuntime`
- `ProducerSystem`
- `IncidentDirector`
- `EngineSnapshot`, `InputAdapter`, `Camera`
- `DevtoolReports`

## Internal

- Sandbox save text encoding details.
- ASCII renderer output format.
- Script output wrapper fields beyond final JSON status.

## Replace Soon

- Reward handling in `DefenseRuntime`.
- Properties schema if content grows beyond simple scalar definitions.
- Text-only Android shell.
