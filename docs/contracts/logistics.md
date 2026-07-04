# engine-logistics Contract

Status: Planned draft  
Owner: resources, inventories, and recipes

## Responsibilities

- Resource stacks.
- Inventories and storage constraints.
- Producers, consumers, and recipes.
- Throughput and reservation contracts.

## Non-Responsibilities

- Combat damage, rendering storage, Android UI, or game-specific economy balance.

## Dependencies

- Depends on `engine-core`, `engine-world`, and `engine-entities`.
- Consumes content definitions from `engine-content`.

## Public Contracts

- `ResourceId`
- `Inventory`
- `Recipe`
- `ProductionOrder`
- reservation interfaces.

## Test Gates

- No negative inventory tests.
- Conservation tests.
- Recipe duration tests.
- Producer/consumer deterministic scenario tests.

