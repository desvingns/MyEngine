id: DX-007
title: Property-based fuzz tests for ContentLoader + SaveCodec
status: done
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Fixed-seed property tests: malformed content (bad keys, dangling refs, bad numerics) always yields typed errors, never crashes; SaveCodec roundtrips arbitrary valid states; corrupted saves yield typed errors.
- Every discovered crasher becomes a pinned regression fixture.
- Runs inside the normal test gate under a 30s local budget.
