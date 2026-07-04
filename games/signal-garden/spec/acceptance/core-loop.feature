Feature: Signal Garden core loop

  @AC-001
  Scenario: Place a tower on a valid tile
    Given a fresh Signal Garden map
    When the player places a pulse tower on a buildable tile
    Then the tower exists in authoritative state
    And the spent charge is removed from inventory

  @AC-002
  Scenario: Wave reaches the core
    Given a fresh Signal Garden map
    When a storm wave runs until an enemy reaches the core
    Then core health is reduced
    And the replay hash changes from the initial state

  @AC-003
  Scenario: Save and replay remain deterministic
    Given a fixed seed and command stream
    When the scenario is run twice
    Then both runs produce the same final hash
    And save/load preserves that hash
