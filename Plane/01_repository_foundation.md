# Phase 01 - Repository Foundation

Status: Planned

## Цель

Сделать репозиторий самодостаточным для будущих Claude/Codex сессий: правила, roadmap, конституция, handoff, memory, progress files. После этого этапа новый агент должен понимать, что строит, зачем, где брать контекст и как не ломать процесс.

## Входы

- Phase 00 deliverables
- `PROMPT_PACK.md`
- Текущая папка `D:\Pet\MyEngine`

## Work Packages

### 01.1 Canonical Project Docs

Создать:

- `README.md`: назначение движка, поддерживаемые игры, high-level architecture, quick start placeholder.
- `AGENTS.md`: canonical instructions для Claude/Codex, engineering rules, no-copy reference policy, testing gates.
- `STATE.md`: текущий статус, next action, active phase, known blockers.
- `DOCUMENTATION.md`: changelog/history для крупных возможностей.
- `docs/ROADMAP.md`: staged roadmap aligned with `Plane/`.

### 01.2 Engine Constitution

Создать `docs/ENGINE_CONSTITUTION.md`:

- Android-only shipping platform.
- Simulation/render/input separation.
- Determinism and replayability.
- Data-driven content.
- Versioned saves and migrations.
- Tests before "done".
- Clones of reference mechanics allowed; no verbatim IP borrowing without an ADR.
- Human-gated self-improvement.

### 01.3 `.ai` Workspace

Создать:

```text
.ai/
  handoff.md
  memory/MEMORY.md
  memory/reference-policy.md
  tasks/README.md
  changes/agent-skill-log.md
  proposals/README.md
  runs/README.md
  retro/README.md
```

### 01.4 Progress Sync

Обновить `Plane/README.md`:

- отметить Phase 00 done, если выполнена;
- отметить Phase 01 in progress/done;
- добавить progress log.

## Deliverables

- `README.md`
- `AGENTS.md`
- `STATE.md`
- `DOCUMENTATION.md`
- `docs/ROADMAP.md`
- `docs/ENGINE_CONSTITUTION.md`
- `.ai/*`

## Acceptance Gates

- Новый агент может прочитать `AGENTS.md` и продолжить без дополнительных объяснений.
- `STATE.md` содержит exact next command/phase.
- `docs/ENGINE_CONSTITUTION.md` содержит непереговорные инварианты.
- `.ai/handoff.md` заполнен: DONE, DECISIONS, NEXT, BLOCKERS.
- Никаких production engine modules ещё не нужно создавать, если это не требуется scaffold.

## Standalone Prompt

```text
Ты выполняешь Phase 01 для `MyEngine`: repository foundation.

Сначала прочитай `PROMPT_PACK.md`, `Plane/README.md`, `Plane/00_reference_research.md` и `docs/REFERENCE_RESEARCH.md`, если он уже есть.

Создай canonical docs and `.ai` workspace:
- README.md
- AGENTS.md
- STATE.md
- DOCUMENTATION.md
- docs/ROADMAP.md
- docs/ENGINE_CONSTITUTION.md
- .ai/handoff.md
- .ai/memory/MEMORY.md
- .ai/memory/reference-policy.md
- .ai/tasks/README.md
- .ai/changes/agent-skill-log.md
- .ai/proposals/README.md
- .ai/runs/README.md
- .ai/retro/README.md

Не реализуй engine code. Цель этапа - сделать репозиторий понятным и управляемым.

В конце обнови `Plane/README.md`: Phase 01 status, дата, created/changed, verification, next.
```

