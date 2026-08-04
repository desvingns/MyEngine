# Desktop launcher

The desktop launcher consumes one validated content snapshot at a time. The normal command runs
the canonical sandbox scenario once, preserving the existing `desktop:run` smoke behavior:

```powershell
.\gradlew.bat desktop:run
```

For balance iteration, enable the dev-only watcher and optionally point it at another pack or seed:

```powershell
.\gradlew.bat desktop:run --args="--watch --pack=D:\Pet\MyEngine\games\sandbox\content\sandbox --seed=7"
```

Each filesystem burst is debounced, validated, and restarted as a new deterministic scenario with
the same seed. Invalid or partially-written packs print typed errors and leave the last-good
scenario active. No content is mutated during a simulation tick; the watcher swaps only at a
restart boundary. `reload_ms` reports the validation-plus-restart duration and should remain below
2 seconds for the sample pack.
