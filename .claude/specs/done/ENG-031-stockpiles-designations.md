id: ENG-031
title: Stockpile zones + designations
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Zone primitive: player-designated tile sets via commands, deterministic ids/order, saved and restored.
- Stockpiles carry item filters referencing content resource ids (validated).
- Designations (harvest-node MVP) mint jobs onto the JobBoard for ENG-003 to execute.
- Snapshot exposes zones for the render overlay; overlap rules tested.

Close-out (2026-08-02): Accepted Option A is complete. The MVP includes deterministic zone
commands/store state, validated stockpile resource filters, one-shot harvest-node JobBoard minting,
immutable snapshot zone projection, and v14 save/restore with v1-v13 migration. No ADR or game-bundle
traceability update was needed. Hauling, stockpile quantities/capacity, depletion/repeated harvest,
and actual Android overlay consumption remain deferred to follow-up/ENG-004 scope.
