# ADR-0000: Reference And License Policy

Status: Accepted  
Date: 2026-07-02  
Phase: 00 - Reference Research And License Guardrails

## Context

`MyEngine` is inspired by open-source games, libraries, and agentic-development systems. The
project needs to learn from them without accidentally importing license obligations, copyrightable
structure, or third-party IP into the engine.

The final license for `MyEngine` is not selected yet. Until that is decided, the safest policy is
to treat references as design inputs and require an explicit ADR before direct reuse.

## Decision

The default rule is: borrow ideas, not files.

Permitted without a new ADR:

- Reading public documentation, README files, module layouts, build scripts, and tests to understand trade-offs.
- Summarizing architectural ideas in original words.
- Creating original APIs, schemas, tests, and docs that solve `MyEngine` requirements.
- Depending on permissive libraries through package managers after the stack ADR approves them.
- Citing reference repositories in docs.

Requires a new ADR before use:

- Copying or closely translating code, build files, schemas, prompt text, generated adapters, docs, test fixtures, assets, or data tables.
- Adding a new dependency whose license or transitive obligations are not already approved.
- Reusing material from any GPL, MPL, unknown-license, or custom-license project.
- Importing exact content names, maps, UI strings, lore, visual identity, or assets from a reference game.

Blocked until explicitly approved:

- Direct copying from GPL-2.0, GPL-3.0, MPL-2.0, unknown-license, or unspecified local sources.
- Shipping any asset, name, or UI expression copied verbatim from a reference game.

## License Classes

Permissive:

- Examples: MIT, Apache-2.0, CC0-1.0.
- Ideas may be used. Direct reuse still needs attribution, source tracking, and an ADR if files are copied or adapted.
- Dependencies may be considered in stack/module ADRs.

Weak copyleft:

- Example: MPL-2.0.
- Ideas may be studied. Files and derived schemas are not copied by default.
- Any reuse must document file-level obligations, notice requirements, and compatibility with the intended `MyEngine` license.

Strong copyleft:

- Examples: GPL-2.0, GPL-3.0.
- Study only unless the project intentionally adopts a compatible license strategy through an ADR.
- No code, assets, data files, schemas, build files, or prompt text may be copied into `MyEngine`.

Unknown/custom/unspecified:

- Treat as blocked for direct reuse.
- Verify the license from the repository, package metadata, or maintainer documentation before any reuse.

## Required ADR For Direct Reuse

A reuse ADR must include:

- Source repository, commit or release, file paths, and license.
- Exact material proposed for reuse.
- Whether the material is copied, translated, adapted, or used as a dependency.
- Compatibility analysis with `MyEngine`'s current/planned license.
- Required attribution and notice changes.
- Alternatives considered, including original implementation.
- Tests or review gates that prove the reused material is isolated and compliant.

## Consequences

- Phase 00 research can be safely used by later phases without browsing every reference again.
- Phase 02 stack work may consider permissive dependencies such as libGDX, KTX, Ashley, and gdx-ai through ADRs.
- GPL/MPL projects remain valuable for architecture and product thinking, but not as copy sources.
- Agentic pipeline assets from `D:\Pet\mobile-pipeline` are process references; they are not copied wholesale without approval.

## Follow-Ups

- Phase 02: create `ADR-0001-stack.md` for runtime/build/module choices.
- Phase 03: document module contracts and which dependencies, if any, each module can use.
- Before first public release: select the `MyEngine` repository license and audit notices.
