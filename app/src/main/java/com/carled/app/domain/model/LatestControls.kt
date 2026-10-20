package com.carled.app.domain.model

/** Coalesces each continuous control independently; the final value is never dropped. Main-thread confined. */
class LatestControls<K, V> {
    private val values = linkedMapOf<K, V>()
    fun offer(key: K, value: V) { values[key] = value }
    fun poll(): V? { val key = values.keys.firstOrNull() ?: return null; return values.remove(key) }
    fun clear() = values.clear()
}
