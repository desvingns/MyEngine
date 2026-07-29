id: DX-008
title: ADR — JSON vs properties content format
status: done
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- ADR evaluates properties vs JSON vs hybrid against: nested structures (upgrade tiers, tech DAG, map layouts), agent diff-friendliness, validation error quality, Android asset loading.
- The decision explicitly gates schema choices in ENG-005/ENG-017/ENG-028 (those cards cite it).
- If migration is chosen: a phased plan with content-validate parity fixtures, no big-bang.

Closed: 2026-07-16

Decision: accepted hybrid format in `docs/DECISIONS/ADR-0003-content-format-hybrid.md`.
Flat definitions remain in `.properties`; nested map assets use structured JSON with additive
loading and parity fixtures.
