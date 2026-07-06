id: ENG-035
title: Resource extractor building
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review; split from ENG-023)

Acceptance:
- Extractor building type: requires an adjacent/underlying resource node (placement validated), produces into its own inventory via the ProducerSystem path at a content-defined rate.
- Node depletion (finite/infinite per content) is deterministic.
- Save + replay coverage; output consumable by hauling (ENG-004) and belts (ENG-023).
