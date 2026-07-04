# Phase 11 - Devtools, Balance Runner And Editor Direction

Status: Planned

## Цель

После playable slice сделать инструменты, которые ускоряют разработку будущих игр: headless scenario runner, balance metrics, content reports, replay inspector and editor plan/prototype.

## Входы

- Phase 10 sandbox
- Existing scripts/tasks
- `docs/TESTING_STRATEGY.md`

## Work Packages

### 11.1 Headless Scenario Runner

- Run scenario without rendering.
- Input: content pack, map, seed, command script, ticks.
- Output: JSON metrics and final hash.

### 11.2 Balance Metrics

Suggested metrics:

- enemies spawned/killed/leaked;
- core damage;
- resources generated/spent;
- tower DPS;
- wave duration;
- time to loss/win;
- sim ms per tick.

### 11.3 Balance Report

- Markdown or JSON report.
- Compare baseline vs changed content.
- Flag large deltas.

### 11.4 Content Report

- List ids.
- Unused references.
- Missing localization.
- Suspicious values.
- Duplicate/near-duplicate definitions.

### 11.5 Replay Inspector

Minimal:

- load replay;
- print command timeline;
- print hash checkpoints;
- optional desktop playback later.

### 11.6 Editor Direction

Create `docs/EDITOR_PLAN.md`:

- map editor MVP;
- content editor MVP;
- in-game debug editor vs desktop-only editor;
- why not build huge editor now.

Optional prototype:

- debug map paint mode in desktop only.

## Acceptance Gates

- A balance scenario can run headlessly.
- Balance metrics are machine-readable.
- Content report catches at least missing reference/localization/duplicate id.
- Editor scope is explicitly constrained.

## Standalone Prompt

```text
Ты выполняешь Phase 11: devtools, balance runner and editor direction.

Build headless scenario runner, JSON balance metrics, content report, replay inspector basics and docs/EDITOR_PLAN.md. Keep editor MVP scoped; do not build a full editor unless it naturally fits.

Run runner on sandbox scenario, save report, update Plane progress.
```

