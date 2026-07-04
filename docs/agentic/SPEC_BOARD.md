# MyEngine Spec Board

Status: Phase 04 accepted  
Last updated: 2026-07-02

Spec work is tracked through:

- `.claude/specs/backlog`
- `.claude/specs/active`
- `.claude/specs/done`
- future `/me-spec` bundles under `games/<slug>/spec`

## Spec States

| State | Meaning |
|---|---|
| Backlog | Approved idea, not currently implemented |
| Active | Current implementation target |
| Done | Implemented, verified, and documented |

## Spec Minimum Fields

```yaml
id: ME-000
title: Short title
status: backlog
owner: human|codex|claude
phase: 05
requirements:
  - R-001
acceptance:
  - command or scenario
gates:
  - tests
  - replay
  - save_compat
```

## Backlog Bridge

Game specs created by `/me-spec` become backlog candidates only after traceability exists. Engine
gaps go to `.claude/specs/backlog` or a future `Plane/` phase; game-specific work stays under
`games/<slug>/spec`.
