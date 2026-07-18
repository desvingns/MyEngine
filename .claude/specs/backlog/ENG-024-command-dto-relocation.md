id: ENG-024
title: Move command DTOs out of engine-render; InputAdapter state fix
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review; MTD-003 follow-up)

Acceptance:
- `BuildTowerCommand`/`UpgradeTowerCommand` move from `engine-render` (RenderModel.kt, InputAdapter.kt) to a render-free home (engine-core command package); all modules compile, no game code duplicates DTOs.
- `InputAdapter` stops owning `nextCommandId` (ID issuance moves to the command-queue side or caller); `selectedTowerId` moves to an explicit UI-state holder.
- Pure-refactor gate: all existing tests pass and canonical scenario replay hashes are byte-identical.
- `docs/contracts/render.md` updated to reflect the boundary (render observes snapshots, submits commands only).

Implementation (approved variant A, 2026-07-16):
- Moved `BuildTowerCommand` and `UpgradeTowerCommand` to
  `engine-core/src/main/kotlin/dev/myengine/core/command/TowerCommands.kt`, with shared
  `TileCoordinate`; no game-side DTO duplicates remain.
- Removed `nextCommandId` and `selectedTowerId` from `InputState`; added explicit `InputUiState`,
  with `CommandId` supplied by the caller. The sandbox performs the boundary conversion.
- `docs/contracts/render.md` records the render boundary: render observes snapshots and submits commands.

Verification:
- `./gradlew.bat test`, canonical replay, save-compat, `android:assembleDebug`, and static scan -> pass.
- Replay hashes unchanged: `9c495d8ff30fd83d` and `83a65da1a7881b2c`.
- `me-verifier` -> pass; all `boundary_checks` true.
- Content validation and benchmark were not run by scope.
