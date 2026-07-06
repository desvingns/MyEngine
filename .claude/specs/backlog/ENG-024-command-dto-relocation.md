id: ENG-024
title: Move command DTOs out of engine-render; InputAdapter state fix
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review; MTD-003 follow-up)

Acceptance:
- `BuildTowerCommand`/`UpgradeTowerCommand` move from `engine-render` (RenderModel.kt, InputAdapter.kt) to a render-free home (engine-core command package); all modules compile, no game code duplicates DTOs.
- `InputAdapter` stops owning `nextCommandId` (ID issuance moves to the command-queue side or caller); `selectedTowerId` moves to an explicit UI-state holder.
- Pure-refactor gate: all existing tests pass and canonical scenario replay hashes are byte-identical.
- `docs/contracts/render.md` updated to reflect the boundary (render observes snapshots, submits commands only).
