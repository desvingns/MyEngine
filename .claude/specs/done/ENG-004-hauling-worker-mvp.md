id: ENG-004
title: First worker agent MVP (hauling)
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Worker entity type in content (speed, capacity); a haul job reserves an item at the source, carries it to a stockpile (ENG-031), deposits; reservations prevent double-haul deterministically.
- Headless loop test: items relocate over N ticks; same seed produces the same hash.
- Save mid-carry roundtrips (in-transit inventory persisted); haul sources include ProducerSystem outputs.

Completion (2026-08-02): Added data-defined worker speed/capacity, deterministic source reservations,
source-to-stockpile hauling with persisted InventoryComponent carry, positioned ProducerSystem output
sources, stockpile contents, and SandboxSaveCodec v15 with v1-v14 migration. Full tests and all
content/replay/save-compat/benchmark/Android gates pass.
