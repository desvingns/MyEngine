# Phase 12 - Game Spec Pipeline

Status: Planned

## Цель

Создать workflow для проектирования новых игр на базе `MyEngine`: от идеи до traceable game spec bundle, engine gap analysis and backlog.

## Входы

- Phase 04 agentic pipeline
- Phase 10 vertical slice
- `PROMPT_PACK.md` PROMPT 5

## Work Packages

### 12.1 Game Spec Bundle Format

Target:

```text
games/<slug>/spec/
  00_manifest.yaml
  product-brief.md
  requirements.md
  user-stories.md
  acceptance/*.feature
  design.md
  content-plan.md
  engine-gap-analysis.md
  balance-plan.md
  android-ux.md
  nfr.md
  risks.md
  traceability.csv
```

### 12.2 Interview Protocol

Decision-tree questions:

- core fantasy;
- session shape;
- world/map;
- player actions;
- threats;
- economy/logistics;
- progression/research;
- content volume;
- Android UX;
- out of scope.

### 12.3 Engine Gap Analysis

Every game spec must separate:

- reusable engine work;
- game-specific rule/content work;
- asset work;
- tooling work.

### 12.4 First Sample Game Spec

Create one sample spec:

- tiny tower-defense/factory hybrid, may closely clone a reference game's mechanics;
- first playable milestone in 1-2 weeks;
- content volume small.

### 12.5 Backlog Bridge

Define how game spec becomes `.claude/specs/backlog` or `Plane` phase tasks.

## Acceptance Gates

- Spec format is documented.
- First sample game spec exists.
- Traceability links requirements -> stories -> acceptance -> design -> engine gaps.
- Engine gaps become backlog candidates, not hidden hacks.

## Standalone Prompt

```text
Ты выполняешь Phase 12: game spec pipeline.

Создай reusable spec workflow for games built on MyEngine. Define bundle format, interview protocol, traceability, engine gap analysis and backlog bridge. Then create one small original sample game spec under games/<slug>/spec.

Do not implement the game. This phase is planning/spec only.

Update Plane progress.
```

