# me-spec — MyEngine Claude Code plugin

Ships the `/me-spec` spec pipeline as a Claude Code plugin: the `me-spec` skill plus
the `me-game-spec-author` agent. This is a **thin adapter** — the canonical spec
workflow lives in `docs/agentic/SPEC_BOARD.md` and `docs/GAME_SPEC_PIPELINE.md`.

## Contents

- `skills/me-spec/SKILL.md` — the `/me-spec` orchestrator (modes, gates, rules).
- `agents/me-game-spec-author.md` — the spec-bundle writer.
- `.claude-plugin/plugin.json` — plugin manifest.

## Install (shipped via the repo marketplace)

    /plugin marketplace add .
    /plugin install me-spec@myengine

Then run `/me-spec --greenfield-game` or `/me-spec --engine-feature`.

## Keeping the adapter honest

Update the canonical spec docs first, then this plugin, then log the change in
`.ai/changes/agent-skill-log.md`. Verify wiring with `scripts\me-selfcheck.ps1`.
