id: ENG-004
title: First worker agent MVP (hauling)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Worker entity type in content (speed, capacity); a haul job reserves an item at the source, carries it to a stockpile (ENG-031), deposits; reservations prevent double-haul deterministically.
- Headless loop test: items relocate over N ticks; same seed produces the same hash.
- Save mid-carry roundtrips (in-transit inventory persisted); haul sources include ProducerSystem outputs.
