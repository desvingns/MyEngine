id: ENG-023
title: Conveyor transport MVP
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- engine-logistics belt building (straight + corner); items advance at content-defined ticks-per-cell; deterministic update order pinned by test; backpressure (full belt stalls) deterministic.
- Endpoints pull from extractor/producer output and push into building/core inventories.
- Items-on-belt persist in save (codec bump + migration); extractor->belt->core 1k-tick replay hash scenario; 100-belt throughput benchmark (PROC-004 feed).
