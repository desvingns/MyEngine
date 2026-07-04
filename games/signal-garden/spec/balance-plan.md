# Balance Plan

Initial targets:

- First wave starts after enough time to place one tower.
- One tower should kill at least one enemy but not the full second wave.
- Core survives one missed enemy.
- Scenario sim time target: under 2 ms per tick on desktop harness.

Use `scripts/me-benchmark.ps1` and compare `enemies_spawned`, `enemies_killed`, `enemies_leaked`,
`core_damage`, and `sim_ms`.
