# MyEngine Memory

Durable project memory for future agents. Store only facts that remain useful across sessions.
Do not store facts that are easy to rediscover from the repository.

## Durable Lessons

- `MyEngine` is Android-first; desktop/JVM is a development harness, not a shipping promise.
- The project is a reusable simulation/game framework, not one hardcoded game.
- Reference projects are inspiration only. GPL/MPL/unknown/local-unspecified sources are no-copy by
  default.
- The local `D:\Pet\mobile-pipeline` pattern is useful for process design: canonical markdown,
  thin adapters, structured payloads, append-only change logs, telemetry, retros, and human gates.
- Production engine code now exists through the v0.1 sandbox foundation. Keep first-game work
  content-first and preserve deterministic replay/save/content gates.
- v0.1 content packs use external `.properties` files parsed by `java.util.Properties` to avoid
  adding a schema/parser dependency before a concrete ADR.
- Signal Garden is the first sample game spec; next work starts with `SG-001-content-pack`.

## Index

- `reference-policy.md`: operational no-copy policy for reference material.
