---
name: me-spec
description: Thin repo-local adapter for MyEngine /me-spec workflows.
---

# me-spec

Read `docs/agentic/SPEC_BOARD.md` and `docs/GAME_SPEC_PIPELINE.md` before creating or updating a
game spec bundle.

The supported `/me-spec` modes are the same as the Claude adapter and are defined canonically in
`docs/GAME_SPEC_PIPELINE.md`:

- `--greenfield-game`
- `--engine-feature`
- `--reference-game`

For `--reference-game`, use the deterministic sanitized-evidence gate at
`scripts/me-reference-evidence.ps1` before authoring or bridging requirements.
