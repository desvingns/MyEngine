# MyEngine Agentic Pipeline

Status: Phase 04 accepted  
Last updated: 2026-07-02

## Purpose

`MyEngine` uses one canonical process for Claude and Codex. The project source of truth is this
repository: `docs/agentic/*`, `.ai/*`, `Plane/*`, and the root operating docs. Tool-specific
folders are thin adapters that point back to the canonical docs.

## Commands

| Command | Purpose | Gate |
|---|---|---|
| `/me --discuss` | Explore options and risks without editing files. | None |
| `/me --spec` | Turn an approved idea into a small implementation spec. | Human approval before code |
| `/me --feature --next` | Pick the next planned feature and run developer -> tester -> runner -> verifier -> docs. | Human approval if scope changes |
| `/me --bugfix` | Reproduce, fix, test, and document a bug. | Human approval if behavior is ambiguous |
| `/me --balance` | Run scenario/balance reports and propose content-only changes. | Human approval before content changes |
| `/me --perf` | Run benchmark/smoke checks and propose scoped optimizations. | Human approval before API changes |
| `/me --content-validate` | Validate content packs and report actionable errors. | None |
| `/me --save-compat` | Run save roundtrip/migration checks. | None |
| `/me --reflect` | Generate a deterministic retro from telemetry. | None |
| `/me --improve` | Convert retro evidence into a proposed process change. | Human approval before edits |
| `/me-spec --greenfield-game` | Create a traceable game spec bundle from an original idea. | Two gates: inventory, final acceptance |
| `/me-spec --engine-feature` | Create a traceable engine feature spec and gap analysis. | Human approval before backlog bridge |

## Canonical Flow

1. Intake reads `AGENTS.md`, `STATE.md`, `.ai/handoff.md`, the active phase, and relevant docs.
2. Architect or spec author narrows scope.
3. Developer implements only the approved scope.
4. Tester adds or updates the narrowest useful tests.
5. Runner executes deterministic commands and emits one JSON object.
6. Verifier reviews boundaries, replay/save/content gates, and known risks.
7. Docs updates `STATE.md`, `.ai/handoff.md`, `Plane/README.md`, and durable docs.
8. Telemetry is appended through `scripts/me-record-run.ps1` when available.

## Adapter Strategy

- `claude-plugins/*` and `codex-plugins/*` are adapters, not source of truth.
- Adapter prompts must reference `docs/agentic/AGENT_CONTRACTS.md`.
- If a contract changes, update canonical docs first, then adapters, then log the change in
  `.ai/changes/agent-skill-log.md`.
- No adapter may silently update itself.

## Runner Script Rule

Runner scripts emit one JSON line. If a tool produces noisy output, the script must summarize it
into one final JSON object with `status`, `command`, `exit_code`, and `notes`.
