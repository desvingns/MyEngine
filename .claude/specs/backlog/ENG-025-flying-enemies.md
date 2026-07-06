id: ENG-025
title: Flying enemies
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review; split from ENG-011)

Acceptance:
- Enemy movement mode ground|air in content; air routes deterministically ignoring blockers; ground unchanged; path-block rule (ENG-002) exempts air.
- Towers gain canTargetAir/canTargetGround; targeting filters respect the flags; a pack with air waves but no air-capable tower produces a balance-report warning.
- Mixed-wave replay hash + air-leak tests; movement mode persisted in save.
