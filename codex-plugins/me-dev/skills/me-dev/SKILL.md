---
name: me-dev
description: Thin repo-local adapter for MyEngine /me workflows.
---

# me-dev

Read `docs/agentic/PIPELINE.md` and `docs/agentic/AGENT_CONTRACTS.md` before acting. This adapter
does not redefine canonical process rules.

For `--feature`, enforce the canonical delivery gate: a feature is complete only after its scoped
artifacts and close-out docs are committed and pushed directly to `main` from a clean or explicitly user-approved
baseline. Never include unrelated worktree changes; a failed commit or push is `blocked`.
