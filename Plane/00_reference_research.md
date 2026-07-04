# Phase 00 - Reference Research And License Guardrails

Status: Planned

## Цель

Создать доказательную базу для архитектуры `MyEngine`: какие решения берём из open-source проектов, какие отвергаем, какие требуют license ADR, и как это влияет на engine modules, content model, tooling и agentic pipeline.

Этот этап защищает проект от двух крайностей: "изобретать всё с нуля" и "бездумно копировать чужой код".

## Входы

- `PROMPT_PACK.md`
- Раздел `Репозитории-референсы`
- Локальный `D:\Pet\mobile-pipeline`
- Доступ к интернету для GitHub repositories

## Основные референсы

Tier A:

- Mindustry
- Unciv
- King under the Mountain
- Mountaincore
- LibColony

Tier B:

- Arc
- Shattered Pixel Dungeon
- OTD
- Heavydefense
- OpenRA
- Warzone 2100
- Freeciv

Libraries:

- gdx-liftoff
- KTX
- Ashley
- gdx-ai

Agentic:

- `D:\Pet\mobile-pipeline`
- SWE-agent
- OpenHands
- aider
- Claude subagent/skill catalogs

## Work Packages

### 00.1 Repository Snapshot

- Зафиксировать ссылку, default branch, основной язык, build system, license.
- Найти docs, module layout, samples, tests, release/build notes.
- Не читать весь код без цели. Сначала смотреть README, build files, module tree, docs.

### 00.2 Borrow/Reject Matrix

Для каждого Tier A проекта заполнить:

- что можно перенять напрямую как архитектурную идею;
- что можно перенять как API shape;
- что можно перенять как test/build workflow;
- что нельзя или рано брать;
- что опасно из-за лицензии;
- какой модуль `MyEngine` получает влияние.

### 00.3 License Risk Map

Сделать отдельную секцию:

- permissive references: MIT/Apache-like;
- weak copyleft: MPL-like;
- strong copyleft: GPL-like;
- unknown / must verify.

Правило: GPL/MPL проекты можно изучать, но прямое заимствование кода/файлов/схем запрещено до ADR.

### 00.4 Reference Decisions

Сформировать 10-20 конкретных решений:

- `Adopt`: берём.
- `Adapt`: берём идею, но меняем под Android/MyEngine.
- `Reject`: не берём.
- `Defer`: не сейчас.

Примеры:

- Adopt Mindustry-style Android/Desktop split as dev-loop pattern.
- Adapt Mindustry content taxonomy, but avoid GPL code and exact schemas.
- Adopt Unciv-style data-driven rules documentation mindset.
- Defer full map editor until after vertical slice.
- Reject multiplayer/network determinism in v1.

## Deliverables

- `docs/REFERENCE_RESEARCH.md`
- `docs/DECISIONS/ADR-0000-license-policy.md`
- `.ai/memory/reference-policy.md` if `.ai/` exists, otherwise create later in Phase 01.

## Acceptance Gates

- `docs/REFERENCE_RESEARCH.md` contains at least 12 repositories.
- Every Tier A repo has `borrow`, `reject`, `license risk`, `architecture impact`.
- License policy explicitly blocks accidental GPL/MPL copying.
- At least 10 concrete decisions are listed.
- Phase 01 can use this document without browsing again.

## Standalone Prompt

```text
Ты выполняешь Phase 00 для `MyEngine`: reference research and license guardrails.

Прочитай `PROMPT_PACK.md`, особенно раздел `Репозитории-референсы`. Исследуй Tier A/Tier B/library/agentic repositories. Не копируй код. Создай `docs/REFERENCE_RESEARCH.md` с таблицей:

repo | language/stack | license | what to borrow | what not to borrow | license risk | architecture impact | priority

Также создай `docs/DECISIONS/ADR-0000-license-policy.md`, где зафиксируй правила использования permissive/MPL/GPL references. Для каждого Tier A repo выпиши 3-7 borrow decisions и 1-3 reject/defer decisions.

Acceptance:
- минимум 12 repos;
- минимум 10 concrete reference decisions;
- explicit no-copy rule для GPL/MPL до отдельного ADR;
- финальный ответ перечисляет созданные файлы и следующие шаги.
```

