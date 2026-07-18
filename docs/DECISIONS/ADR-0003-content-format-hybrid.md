# ADR-0003: Hybrid Content Format For Nested Assets

Status: Accepted
Date: 2026-07-16
Decision id: DX-008

## Context

The foundation content format uses small `.properties` files, which remain clear and
diff-friendly for flat definitions such as tiles, resources, towers, enemies, recipes,
waves, incidents, and difficulties. New engine capabilities need nested data: map layouts,
tower upgrade sets, future tech DAGs, and compound scenario configuration. Encoding those
shapes in dotted properties keys makes authoring and validation progressively less clear.

ENG-005 needs a versioned, Android-safe map asset with rows, a terrain-symbol mapping, named
spawn points, one core, and optional resource nodes. DX-008 is the format decision that gates
the schema choices in ENG-005, ENG-017, and ENG-028.

## Options Considered

### Keep `.properties` for all content

This keeps the smallest existing parser and produces concise line-based diffs. It is a good fit
for shallow scalar records, but nested maps, graph edges, and variable-size collections become
opaque dotted-key conventions. Validation errors lose structural context and authors must infer
relationships between scattered keys.

### Move every content file to JSON

JSON represents nested data directly and supports precise structural validation, but a wholesale
rewrite would churn stable content, invalidate focused validation fixtures, and make simple
single-field edits noisier. It would not create sufficient value for the flat entity definitions
already working well in `.properties`.

### Hybrid: `.properties` for flat definitions and JSON for nested assets

Keep `.properties` as the canonical format for flat entity definitions. Introduce JSON only for
assets whose natural shape is nested or graph-like, starting with map layouts in `maps.json`.
Use a structured parser rather than hand-written JSON parsing, while keeping parsed definitions
as local immutable engine-content models.

## Decision

Adopt the hybrid option.

- Existing `.properties` schemas remain supported and are not migrated as part of this decision.
- Nested map layouts use one `maps.json` asset per content pack. The format carries a map id,
  dimensions, terrain row strings, a symbol-to-terrain mapping, named spawn coordinates, one
  core coordinate, and optional resource-node data on a terrain symbol.
- `engine-content` uses `kotlinx-serialization-json` as an Android-compatible structured JSON
  parser. It is isolated inside the loader; JSON parser types do not escape `ContentRegistry`.
  The dependency is Apache-2.0, has no reflection requirement, background work, clock, random
  state, native code, or simulation ownership. Parsing happens at content-load time, outside the
  deterministic tick loop. The fallback is a future replacement behind the same loader boundary,
  not a custom JSON parser.
- The Android module packages the content directory as assets. `engine-content` itself keeps no
  Android dependency and remains JVM-testable.
- Loader validation reports map id and structural field paths for malformed JSON, row width,
  bounds, unknown terrain/resource ids, exactly-one-core, and every spawn-to-core walkability
  failure.

## Migration And Validation Plan

This is not a big-bang migration.

1. JSON map loading is additive; packs without `maps.json` remain valid so existing flat packs do
   not churn.
2. Consumers that need a world select a registered map explicitly, or require the sole map in a
   single-map pack. The sandbox ships an equivalent JSON map before its hard-coded construction is
   removed.
3. Content validation discovers `maps.json` through the normal pack loader. Parity fixtures cover
   the equivalent legacy sandbox layout and invalid maps (row width, bounds, unknown tile,
   core-count, and blocked spawn path) before another nested asset format is added.
4. Save data records map id plus the content-pack version. Save version migration accepts prior
   saves and resolves their sole map; new saves reject incompatible pack/content versions instead
   of silently loading a different world.

## Consequences

- Small, scalar content remains easy to review in `.properties`.
- Nested formats are introduced deliberately and receive structured validation at their loader
  boundary.
- A future JSON use must still add a documented schema and validation fixtures; this ADR is a
  format policy, not permission for unreviewed arbitrary JSON.
