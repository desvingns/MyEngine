package dev.myengine.android

import dev.myengine.core.Tick

/** Returns the next input tick, or null once the run or deterministic clock is terminal. */
internal fun nextInputTickOrNull(currentTick: Tick, terminal: Boolean = false): Tick? =
    if (terminal || currentTick.value == Long.MAX_VALUE) null else currentTick.next()
