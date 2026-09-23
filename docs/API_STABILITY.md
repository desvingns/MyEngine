# MyEngine API Stability v0.1 Draft

Status: Phase 14 accepted  
Last updated: 2026-09-23 (ENG-036 technical gates accepted locally; not published)

## Stable For First Game

- `Tick`, `TickRate`, `TickScheduler`
- `SeededRandom`
- `CommandQueue`, `EngineCommand`, `CommandId`
- `StableHash`, `HashableState`
- `TileWorld`, `TilePosition`, `WorldSize`
- `ContentPackLoader`, `ContentRegistry`
- `EntityId`, `Entity`, `EntityStore`

## Experimental

ENG-036 runtime/session contracts passed their independent technical acceptance on 2026-09-23;
this does not promote them to Stable. Scoped delivery/publication remains pending. Cross-repository
consumers still require a game-owned adapter and an exact accepted engine commit under ADR-0004.

- `GameRuntimeDescriptor`, `GameSession`, `DeterministicGameSession`
- `GameRuntimeIdentity`, `VersionedGameSave`, and typed session operation results
- `SandboxRuntime`
- `DefenseRuntime`
- `ProducerSystem`
- `IncidentDirector`
- `EngineSnapshot`, `InputAdapter`, `Camera`
- `DevtoolReports`

## Internal

- Concrete save payload encodings carried opaquely by `VersionedGameSave`.
- Sandbox save text encoding details.
- ASCII renderer output format.
- Script output wrapper fields beyond final JSON status.

## Replace Soon

- Reward handling in `DefenseRuntime`.
- Properties schema if content grows beyond simple scalar definitions.
- Text-only Android shell.
