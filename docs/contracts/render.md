# engine-render Contract

Status: Contract draft (ENG-024 / MTD-005 boundary)
Owner: renderer and visual snapshots

## Responsibilities

- Camera math.
- Observe immutable simulation snapshots and project them into render output; rendering is
  read-only with respect to authoritative state.
- Sprite/tile batching boundaries.
- Debug overlays.
- Visual test fixtures.
- Convert presentation coordinates at the boundary: screen -> world -> tile for input and tile
  -> world -> screen for rendering. `TileCoordinate` is the render-free command value carried
  across the simulation boundary; world-size/bounds conversion or clamping belongs to this
  boundary, while authoritative validity remains a simulation/world concern.
- Submit caller-provided commands to the simulation/command queue through an adapter. The render
  layer may describe intent, but it does not issue command IDs or apply commands.

## Non-Responsibilities

- Authoritative simulation mutation or ownership of authoritative state, Android lifecycle
  ownership, content validation, or save migration.
- Command DTO ownership: `BuildTowerCommand`, `UpgradeTowerCommand`, and `TileCoordinate` live in
  the render-free `engine-core` command package. `engine-render` imports that package; the command
  package does not depend on rendering.
- Command ID issuance: `InputAdapter` forwards the `CommandId` supplied by its caller. ID
  allocation belongs to the caller or command-queue side.
- UI selection state as simulation state: `InputUiState` (including `selectedTowerId`) is an
  explicit presentation/UI-state holder and stays outside authoritative simulation.
- Android `Canvas`/`MotionEvent` ownership: an Android View may own camera, selected tile, and gesture
  bookkeeping, but it does not apply a command or advance authoritative state directly. It emits a
  command through a callback; the lifecycle/controller layer owns command-id allocation and performs
  the submit/step transition.

## Dependencies

- Depends on `engine-core`, `engine-world`, and `engine-content`.
- May depend on libGDX render APIs.
- Does not depend on Android application code.
- Android-specific `Canvas` and `MotionEvent` glue lives in the `android` module only; the
  `engine-render` contract remains Android-free.

## Public Contracts

- `Renderer`
- `CameraState`
- `RenderSnapshot`
- Immutable snapshot projection and command-submission boundaries.
- `EngineSnapshot` carries render-safe run outcome data: `runStatus`, optional terminal
  reason/tick, and `RunSummary`. Active snapshots project current totals; terminal snapshots expose
  the frozen authoritative summary without giving render code mutable simulation state.
- `InputAdapter` accepts caller-supplied command IDs and explicit `InputUiState`; it returns
  commands without mutating authoritative simulation.
- An Android canvas consumer obtains a fresh immutable snapshot, projects it with
  `PlaceholderRenderSurface`, and renders in stable layer order: tiles, path, then entities. It can
  translate tap/pan/pinch into `InputAdapter`, but only its controller callback may submit and step.
- debug draw interfaces.

## Dependency Boundary

- `engine-render` may depend on the render-free core command package for command DTOs and on
  world types needed for camera/coordinate conversion.
- Coordinate conversion must not introduce a dependency from `engine-core` command DTOs back to
  `engine-render`, Android, or a game module.
- Render and input code can submit commands, but only simulation/command-queue code applies them
  to authoritative state.

## Test Gates

- Camera math tests.
- Snapshot-only mutation tests.
- Input boundary tests: caller-supplied IDs are preserved, `InputUiState` is explicit and not
  authoritative, and screen/world/tile conversion respects world boundaries.
- Core command-package tests and dependency checks: command DTOs compile without render imports,
  with no game-side DTO duplicates.
- Screenshot or pixel-smoke tests.
- Asset missing/fallback tests.
- Android device/manual checks (not satisfied by `assembleDebug` alone): tap-to-build after one tick,
  drag-without-build, pinch zoom, tiles/path/entities draw order, and rotate/process recreation with
  pending commands and caller-owned command IDs. Profile sustained pan/pinch with FrameMetrics or
  JankStats plus Allocation Tracker before claiming frame-budget compliance.
