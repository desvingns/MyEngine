# me-dev — MyEngine Claude Code plugin

Ships the `/me` dev pipeline as a Claude Code plugin: the `me` skill plus the full
agent roster under `agents/`. This is a **thin adapter** — canonical behavior lives
in `docs/agentic/PIPELINE.md` and `docs/agentic/AGENT_CONTRACTS.md`.

## Contents

- `skills/me/SKILL.md` — the `/me` orchestrator (modes, gates, delegation map).
- `agents/*.md` — the role roster (architect, engine-developer, gameplay-designer,
  content-schema-designer, tester, runner, verifier, simulation/render/save/android
  reviewers, balance-simulator, docs, reflect, improve) with contract-scoped tool
  grants that enforce "writer never reviews its own work".
- `.claude-plugin/plugin.json` — plugin manifest.

## Install (shipped via the repo marketplace)

    /plugin marketplace add .
    /plugin install me-dev@myengine

Then run `/me --feature --next` (see `skills/me/SKILL.md` for all modes).

## Keeping the adapter honest

Update `docs/agentic/*` first, then this plugin, then log the change in
`.ai/changes/agent-skill-log.md`. Verify wiring with `scripts\me-selfcheck.ps1`.
