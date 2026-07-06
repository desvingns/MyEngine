id: PROC-011
title: Codex adapter parity audit + selfcheck coverage
status: backlog
phase: process
source: engine gap sweep 2026-07-06 (project review; adapters already exist — this is audit + drift coverage)

Acceptance:
- Audit confirms `codex-plugins/me-dev` + `codex-plugins/me-spec` adapters resolve canonical docs and expose the same modes as the claude-plugins counterparts.
- `scripts/me-selfcheck.ps1` drift check extended to codex-plugins + .codex registration; fails on divergence.
- One documented smoke run of a /me mode under Codex CLI recorded in `.ai/runs`.
