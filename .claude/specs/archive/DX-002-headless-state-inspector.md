id: DX-002
title: Headless state inspector ("agent eyes")
status: backlog
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Devtools command runs any game/pack scenario to tick N (optional command script) and emits an ASCII frame + JSON state dump (entities, inventories, defense metrics, hash).
- Byte-identical output for identical args (determinism test); no sandbox-only hardcoding.
- Referenced from AGENTS.md as the default debugging step for agents.
