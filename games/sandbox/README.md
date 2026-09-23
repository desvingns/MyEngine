# MyEngine Sandbox

This is the first tiny end-to-end proof game for `MyEngine`.

## Current Loop

- 64x64 tile map with walls, one core and one resource node.
- External content pack under `games/sandbox/content/sandbox`.
- Scripted tower placement command.
- Waves spawn enemies that path toward the core.
- Towers damage enemies by deterministic targeting order.
- A generator recipe produces the starter resource over time.
- Versioned save/load migration through v7 and replay hashes are covered by passing JVM tests.
- `SandboxRuntime` adapts the reusable Android-free `engine-runtime` session contract while this
  module retains sandbox state, rules, content selection, snapshot projection, and codec payload.

## Controls Target

Phase 09 introduced the input boundary: tap maps to a selected tile, and when a tower is selected
the input adapter emits `BuildTowerCommand`. The current desktop shell is still a deterministic
headless/ASCII smoke runner; a fuller libGDX UI can build on the snapshot model without touching
authoritative state.

## Known Limitations

- Placeholder ASCII/Android text presentation only.
- No asset atlas or real gesture handling yet.
- Enemy speed is one tile per tick.
- ENG-036 technical gates are accepted locally (2026-09-23), including final 184/0 engine tests,
  replay/save/content/static/build checks and calibrated paired03 headless timing. The API remains
  Experimental; scoped delivery/publication is pending. See `docs/contracts/runtime-benchmark.md`
  for retained failed measurements and the final pass; no Android frame-budget claim follows.
