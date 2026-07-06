id: ENG-012
title: Boss/elite enemies + wave modifiers
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content: elite/boss flags with stat scaling plus a wave modifier list (hp%, speed%, count); validation + fixtures; no per-game engine branching.
- Deterministic modifier application at spawn; snapshot marks bosses for render emphasis.
- Replay hash + save roundtrip mid-boss-wave; reward scaling visible in the balance report.
