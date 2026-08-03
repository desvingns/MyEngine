id: DX-001
title: New-game scaffolder script
status: done
owner: Codex
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Implementation:
- `scripts/me-new-game.ps1` creates `games/<slug>`, wires `:games:<slug>` into
  `settings.gradle.kts`, emits the starter content pack, canonical replay scenario/test, and
  `spec/` bundle from `docs/GAME_SPEC_PIPELINE.md`.
- Generation stages under `games/.<slug>.scaffold`, enforces contained paths and lower-kebab slugs,
  writes UTF-8 without BOM, and refuses existing game, staging, or settings wiring.
- Recipe 6 in `docs/COOKBOOK.md` documents the exact 28-file artifact list and gates.

Verification:
- `scripts/tests/me-new-game.tests.ps1` covers settings wiring, all generated files, canonical
  replay metadata, existing-slug refusal, and invalid-slug refusal; generated module test,
  content-validation, replay, and projects gates passed for the DX-001 implementation.
- No engine, save, Android, or renderer behavior changed. No game-bundle traceability update was
  needed; archived `archive/dx001-smoke*` fixtures are historical validation artifacts outside
  this feature scope.
