# engine-storyteller Contract

Status: Planned draft  
Owner: incidents and pacing

## Responsibilities

- Incident budgets.
- Threat and relief pacing.
- Difficulty profiles.
- Deterministic incident selection.

## Non-Responsibilities

- Narrative content writing, UI presentation, Android notifications, or copying reference storyteller
  behavior.

## Dependencies

- Depends on `engine-core`, `engine-logistics`, and `engine-defense`.
- Consumes incident content from `engine-content`.

## Public Contracts

- `IncidentDefinition`
- `IncidentBudget`
- `PacingState`
- incident selection result types.

## Test Gates

- Seed repeatability tests.
- Budget bound tests.
- Cooldown tests.
- Scenario pacing tests.

