id: PROC-007
title: Save migration matrix test (v1 -> vN)
status: done
phase: process
source: architecture review 2026-07-04 (P3.4)

Context: the save-compat gate runs a single current-version roundtrip. As save versions
accumulate, upgrades from every historical version must stay loadable.

Acceptance:
- Checked-in fixture saves for each released save version.
- A matrix test loads every fixture through the migration chain to current and asserts
  a stable post-migration state hash.
- me-save-compat.ps1 includes the matrix result in its JSON.

Close-out (2026-08-02): Implemented with checked-in v1-v10 fixtures, an independently constructed
canonical state oracle and deterministic stable-hash matrix test, plus `matrix` in the compact
save-compat JSON. No ADR; production save encoding and `SAVE_VERSION=10` are unchanged.
