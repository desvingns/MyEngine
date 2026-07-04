# Design

## Loop

The player spends charge to place towers. Signal plants produce charge. Storm enemies follow a path
to the core. Towers use deterministic nearest-target selection.

## Data Model

- Tile: floor, wall, core, charge node.
- Resource: charge.
- Tower: pulse tower, later splitter tower.
- Enemy: drift spark.
- Recipe: signal plant -> charge.
- Wave: timed spawn schedule.
- Incident: static burst budget placeholder.

## Engine Use

Use existing `SandboxRuntime` concepts as the starting proof. Any game-specific names and balance
numbers live in content files.
