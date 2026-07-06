# MyEngine project & backlog review — 2026-07-06

Scope: whole repo + `.claude/specs/` board, reviewed against the project vision:
(1) engine maximally convenient for AI-agent-driven development (Claude Code + Codex CLI),
(2) games ship exclusively on Android,
(3) target genres: RimWorld-like colony sim; TD like Infinitode 2 / Block Defense; Mindustry-like hybrid.

Outcome: 47 new backlog cards (ENG-001..035, DX-001..008, PROC-011..014), roadmap update,
this document. Prefixes ENG-/DX- and TD-first priority confirmed by the owner.

## Verdict

The foundation is genuinely strong and unusually well suited to agent development:
deterministic fixed-tick sim, seeded RNG, replay hashes, versioned saves (v1->v3 with
migrations), data-driven content packs with high-quality validation errors, 21 test files,
deterministic gate scripts, telemetry + human-gated self-improvement loop, thin
Claude/Codex adapters over canonical docs. Phases 00-14 delivered what they promised.

The gap is between that foundation and the stated goals: today the engine can only run a
minimal TD vertical slice, and on the one shipping platform (Android) it renders as ASCII
text in a TextView with no touch input and no game loop. Several shipped systems are dead
or misplaced (see findings). None of this is architectural debt — the invariants held —
it is simply unbuilt surface, now carded.

## Strengths (keep doing this)

- Determinism discipline: sorted command queue, stable IDs, FNV-1a state hash, canonical
  scenario hashes pinned in tests. Rare even in commercial engines.
- Save codec already survived two versioned migrations (v1->v2 pending commands,
  v2->v3 upgrade tiers) with tests.
- Content validation errors are agent-grade: file, id, field, message.
- Module boundaries actually enforced (sim is Android/render-free; render observes
  snapshots); verifier checks them per run.
- Process: gap dedup rule caught MTD-001 as a duplicate of SG-002 in live use; telemetry
  and retro cadence exist and were exercised.

## Findings

| # | Finding | Evidence | Card |
|---|---|---|---|
| F1 | Android shell is a stub: snapshot printed as text into a TextView; no SurfaceView/Canvas, no Choreographer loop, no touch input | `android/src/main/kotlin/dev/myengine/android/MyEngineActivity.kt` | ENG-026, ENG-027 |
| F2 | Map hardcoded: 64x64, spawn (1,1), core (32,32) | `games/sandbox/.../SandboxGame.kt:110-111` | ENG-005 (then ENG-006) |
| F3 | Enemy paths precomputed at spawn, never recalculated on world change; BFS only — mazing (core TD mechanic) impossible | `engine-ai/.../Pathfinding.kt`, `engine-defense/.../DefenseRuntime.kt` | ENG-002 (agents: ENG-001) |
| F4 | Dead incident call + RNG smell: `IncidentDirector` allocated every tick with fresh `SeededRandom(17)`, result discarded | `SandboxGame.kt:143` | ENG-016 |
| F5 | JobBoard exists but is never called from the tick loop | `engine-ai/.../Jobs.kt` (refs only in its own test) | ENG-003, ENG-004 |
| F6 | Command DTOs live in engine-render; InputAdapter holds `nextCommandId` + `selectedTowerId` state (known MTD-003 follow-up) | `engine-render/.../RenderModel.kt`, `InputAdapter.kt` | ENG-024 |
| F7 | TD staples absent: win/lose, speed control, sell/refund, targeting modes, AoE, status effects, armor, flying, bosses, endless, multi-spawn, wave preview | genre comparison vs Infinitode 2 / Block Defense | ENG-007..015, ENG-018, ENG-025, ENG-030 |
| F8 | Targeting is an O(n) Manhattan scan; no spatial index, no scale benchmark | `DefenseRuntime.kt` | ENG-020 (feeds PROC-004) |
| F9 | Board hygiene: 8 done cards sit in `backlog/`; SG-001..003 card statuses were stale (known — roadmap note); `active/`/`done/` unused | `.claude/specs/` | statuses flipped in this review; moves -> PROC-013 |
| F10 | `.properties` strains with nesting (upgrade tiers today; maps/tech-DAG next) | `ContentLoader.kt` upgrade.<branch>.<tier>.<field> parsing | DX-008 gates ENG-005/017/028 |
| F11 | No release lane (signing, R8, versioning) despite Android-only shipping | no release config in `android/` | PROC-014 |
| F12 | Reviewer premise disproved during verification: `codex-plugins/me-dev` EXISTS (thin adapter by design) — card reframed to parity audit | `codex-plugins/me-dev/skills/me-dev/SKILL.md` | PROC-011 |
| F13 | RimWorld/Mindustry-side systems entirely absent: job execution, workers, stockpiles, construction, needs, research, extractors, belts, structure attacks, procgen | code sweep | ENG-001/003/004/006/016/017/023/031..035 |

## Existing backlog disposition (PROC-001..010)

All ten remain valid. Notes:
- PROC-003 (domain sequencing): this sweep materializes exactly that sequencing; close by
  reference once the roadmap's recommended order is adopted.
- Pairs to implement together: PROC-005 <-> DX-003 (golden trajectories / bisector),
  PROC-004 <-> ENG-020 (budgets / benchmark + index), PROC-009 <-> PROC-012
  (visual smoke / emulator lane).

## New cards (47)

- P0 — first playable Android TD (5): ENG-024, ENG-005, ENG-014, ENG-026, ENG-027.
- P1 — TD depth (18): ENG-002, 007, 008, 009, 010, 011, 012, 013, 015, 018, 019, 020,
  021, 022, 025, 028, 029, 030.
- P2 — colony-sim & factory (12): ENG-001, 003, 004, 006, 016, 017, 023, 031, 032, 033,
  034, 035.
- DX — agent-facing tooling (8): DX-001..008.
- PROC — process/platform (4): PROC-011..014.

Deliberately not carded (bounded scope; revisit when a game spec demands them): power
grid, fluid transport, unit production/control (Mindustry deep-end); temperature/roofing,
animals, trading (RimWorld deep-end); achievements/quests, tutorials (game-side).

## Recommended order

1. Human: commit the in-flight MTD-003 close-out working-tree changes; then run PROC-013.
2. MTD-004 (already next) -> ENG-024 -> ENG-005 -> ENG-014 -> MTD-005 -> ENG-026 ->
   ENG-027 = **first playable Android TD milestone**.
3. P1 opener: ENG-002 -> ENG-013 -> ENG-008 -> ENG-015 -> ENG-030.
4. Anytime, high leverage for the agent pipeline: DX-002 (agent eyes), DX-006 (cookbook),
   DX-005 (schema drift gate).
5. DX-008 (content format ADR) before starting ENG-005/017/028 schema work.
