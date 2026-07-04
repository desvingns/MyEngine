# MyEngine Editor Plan

Status: Phase 11 accepted  
Last updated: 2026-07-02

## Direction

Do not build a large editor yet. The v0.1 editor direction is tool-assisted content iteration,
headless scenario reports, and a small future desktop-only map paint mode.

## Map Editor MVP

- Load a content pack.
- Paint terrain ids on a grid.
- Mark core and spawn points.
- Export a map fixture.
- Validate the map through the content validator.

## Content Editor MVP

- List ids by category.
- Edit scalar fields.
- Run reference/localization validation.
- Show balance warnings.

## In-Game Debug Editing

Allowed only for desktop/dev harness initially. Android debug overlays may inspect state, but they
must not become authoritative editor state.

## Why Not Bigger Now

Replay, save compatibility, and content validation need to harden before a GUI editor can safely
write durable game data.
