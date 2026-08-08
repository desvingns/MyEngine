---
name: me-dev
description: Thin repo-local adapter for MyEngine /me workflows.
---

# me-dev

Read `docs/agentic/PIPELINE.md` and `docs/agentic/AGENT_CONTRACTS.md` before acting. This adapter
does not redefine canonical process rules.

The supported `/me` modes are the same as the Claude adapter and are defined canonically in
`docs/agentic/PIPELINE.md`:

- `--discuss`
- `--spec`
- `--feature --next`
- `--bugfix`
- `--balance`
- `--perf`
- `--content-validate`
- `--save-compat`
- `--reflect`
- `--improve` and `--improve --drain`
- `--upgrade`

For `--feature`, enforce the canonical delivery gate: a feature is complete only after its scoped
artifacts and close-out docs are committed and pushed directly to `main` from a clean or explicitly user-approved
baseline. Never include unrelated worktree changes; a failed commit or push is `blocked`.
