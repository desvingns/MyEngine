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
- `--feature --next --chain`
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

`--chain` is valid only in the exact `$me --feature --next --chain` combination. Follow the
active-first board resolution in `docs/agentic/PIPELINE.md`: resume exactly one active card;
otherwise take the first runnable `backlog` card in `ENGINE_ROADMAP.md` table order. An empty board
drains normally; multiple active cards, a blocked card, or an ambiguous order is `needs_human`.

After a chained feature has moved to `done`, passed all applicable gates, recorded telemetry, and
pushed its scoped commit, re-run the board check. If a runnable successor remains, confirm
`git branch --show-current` is `main`, then call `fork_thread` with
`environment: { type: "same-directory" }`. Send the fork exactly:

```text
Run $me --feature --next --chain now. Work directly in the current main checkout; do not create a worktree or Git branch. If no active or runnable backlog card remains, report the drained board and stop.
```

The fork keeps the selected model and reasoning effort while staying in the same local checkout;
never create or switch to a Git worktree/branch. If forking or sending the prompt fails, report it
once and do not create another task.
