# MyEngine Sandbox

This is the first tiny end-to-end proof game for `MyEngine`.

## Current Loop

- 64x64 tile map with walls, one core and one resource node.
- External content pack under `games/sandbox/content/sandbox`.
- Scripted tower placement command.
- Waves spawn enemies that path toward the core.
- Towers damage enemies by deterministic targeting order.
- A generator recipe produces the starter resource over time.
- Save/load v1 and replay hash are covered by JVM tests.

## Controls Target

Phase 09 introduced the input boundary: tap maps to a selected tile, and when a tower is selected
the input adapter emits `BuildTowerCommand`. The current desktop shell is still a deterministic
headless/ASCII smoke runner; a fuller libGDX UI can build on the snapshot model without touching
authoritative state.

## Known Limitations

- Placeholder ASCII/Android text presentation only.
- No asset atlas or real gesture handling yet.
- Enemy speed is one tile per tick.
- Rewards are tracked in metrics but not yet deposited into a player wallet.
