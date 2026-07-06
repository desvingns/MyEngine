id: DX-001
title: New-game scaffolder script
status: backlog
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- One script (`scripts/me-new-game.ps1`) creates `games/<slug>`: module wired into settings.gradle.kts, starter content pack, canonical scenario test with replay hash, and spec dir per `docs/GAME_SPEC_PIPELINE.md`.
- The generated project passes tests + content-validate + replay gates out of the box.
- Idempotent (refuses an existing slug); the recipe is documented in the DX-006 cookbook.
