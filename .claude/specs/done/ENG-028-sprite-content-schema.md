id: ENG-028
title: Sprite/atlas references in content schema
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Sprite/atlas ref per tile/tower-tier/enemy/building with palette fallback; manifest validation checks files exist and atlas keys resolve; actionable errors (pack, key, path).
- Sim stays asset-free: refs flow as opaque ids through the snapshot; desktop and Android renderers resolve them.
- `docs/content-schemas/PROPERTIES_SCHEMA.md` updated; PROC-009 golden refresh via handoff note.
