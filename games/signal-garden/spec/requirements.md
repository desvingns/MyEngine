# Requirements

- `FR-001`: The game shall run on Android as the only shipping platform.
- `FR-002`: The game shall load all tiles, resources, towers, enemies, recipes, waves, incidents,
  and strings from versioned content files.
- `FR-003`: The player shall place at least one tower on valid buildable tiles.
- `FR-004`: The game shall spawn storm enemies from a wave schedule.
- `FR-005`: Towers shall target and damage enemies deterministically.
- `FR-006`: A core shall lose health when enemies arrive.
- `FR-007`: A signal plant recipe shall produce charge over time.
- `FR-008`: Save/load v1 shall preserve current tick, resources, core health, towers, enemies, and
  wave state.
- `FR-009`: A replay test shall reproduce a final hash from seed plus commands.
