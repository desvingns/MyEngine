# Runs

This directory is reserved for future run telemetry and deterministic workflow outputs.

Expected later uses:

- simulation runner JSONL;
- replay verification output;
- content validation output;
- benchmark summaries;
- agentic workflow telemetry.

Rules:

- Prefer append-only JSONL for machine-readable run logs.
- Do not store secrets.
- Do not treat telemetry as a substitute for tests.
- Summaries that change project direction belong in `.ai/retro/` or `.ai/proposals/`.
