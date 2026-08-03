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
- `engine-render` compiles transitively into the Android artifact (`android` -> `:games:sandbox` ->
  `api(project(":engine-render"))`), so it must stay java.awt/android-free. Desktop-only AWT
  rendering (e.g. a `BufferedImage` rasterizer) belongs in `:desktop`, which is never built into
  Android. Shared render data that both harnesses need (e.g. the `RenderKind`->color mapping in
  `RenderPalette`) must be pure RGB/data with no toolkit imports.
- The `docs/contracts/render.md` "Screenshot or pixel-smoke" gate is satisfiable headlessly: a
  deterministic AWT `BufferedImage` rasterizer + a pixel-smoke test that asserts cell-center pixels
  against `RenderPalette` colors (and bit-for-bit determinism) covers it without a GL window.
- A "sound only at a quiescent tick" save-precondition can be closed later by a normal
  `SAVE_VERSION` bump + migration (old version keeps decoding via a default/empty value for the new
  field) rather than an ADR, as long as the save codec is still marked Experimental in
  `docs/API_STABILITY.md` — this was the precedent set by the original v1 `SandboxSaveCodec` and
  reused for its v1->v2 pending-CommandQueue bump (SG-004 follow-up, 2026-07-05). Don't treat every
  save-format version bump as ADR-worthy by default; check the module's stability tier first.
- `CommandQueue` needed a non-destructive `pending()` snapshot (distinct from the destructive
  `drainFor`) to make it save-safe — a reusable engine-core pattern for "peek without consuming" on
  any similar internal queue.

## Index

- `reference-policy.md`: operational no-copy policy for reference material.

## 2026-08-02 - ENG-019 retro lessons

- When extending content schemas, preserve the validation contract of existing content types and
  add boundary tests for both legacy and new definitions; wall cost must be positive while tower
  cost remains non-negative.
- A save-version bump is incomplete until current-version expectations, a checked-in fixture, and
  a dedicated save-compat invocation are updated together.
- Add focused acceptance tests before the full runner so content, command, and save regressions are
  localized before integration gates.

- Building-owned logistics sources need a deterministic reachable stand position when the producer
  tile is occupied; persist that source position and reject removal while output or reservations
  are pending so material is never silently lost.
