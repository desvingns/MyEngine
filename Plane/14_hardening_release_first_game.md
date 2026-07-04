# Phase 14 - Hardening, Release Discipline And First Game Kickoff

Status: Planned

## Цель

После vertical slice and tooling перевести `MyEngine` из prototype mode в usable foundation for the first real game. Это не "дописать всё", а стабилизировать API, gates, release rhythm and first game plan.

## Входы

- Phase 10 vertical slice
- Phase 11 devtools
- Phase 12 game spec
- Phase 13 self-improvement

## Work Packages

### 14.1 Hardening Audit

Review:

- deterministic replay reliability;
- save compatibility;
- content validation coverage;
- Android lifecycle;
- memory/performance;
- module boundaries;
- docs freshness;
- agentic workflow gaps.

### 14.2 API Freeze v0.1

Classify APIs:

- stable for first game;
- experimental;
- internal;
- deprecated/replace soon.

### 14.3 Release Checklist

Create `docs/RELEASE_CHECKLIST.md`:

- version bump;
- tests;
- content validate;
- replay suite;
- save compatibility;
- benchmark;
- Android assemble/smoke;
- docs update;
- changelog;
- known issues.

### 14.4 First Game Kickoff

Pick one game spec from Phase 12 and create:

- `games/<slug>/ROADMAP.md`;
- first milestone;
- content list;
- required engine gaps;
- first 5 backlog SPECs.

### 14.5 Quality Bar

Set thresholds:

- replay pass rate;
- max sim ms per tick for target scenario;
- frame budget;
- max allocation spikes if measurable;
- content validation must be clean;
- save migration tests must pass.

## Acceptance Gates

- Release checklist exists.
- v0.1 API classification exists.
- First game has concrete first milestone.
- Engine gaps are prioritized.
- No "done" claim without test/build evidence.

## Standalone Prompt

```text
Ты выполняешь Phase 14: hardening, release discipline and first game kickoff.

Audit MyEngine after vertical slice. Create API stability classification, release checklist, hardening backlog and first game kickoff plan. Use Phase 12 sample spec or ask for chosen game spec if none exists.

Do not start large gameplay implementation until hardening gates are clear.

Update Plane progress and STATE/handoff.
```

