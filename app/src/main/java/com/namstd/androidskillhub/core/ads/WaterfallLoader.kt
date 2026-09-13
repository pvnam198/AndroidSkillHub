package com.namstd.androidskillhub.core.ads

/** Runs one load attempt at a time and advances only after a failure. */
internal class WaterfallLoader<T>(ids: List<String>) {
    private val ids = ids.map(String::trim).filter(String::isNotEmpty)

    fun load(
        attempt: (id: String, onLoaded: (T) -> Unit, onFailed: (String) -> Unit) -> Unit,
        onLoaded: (T, String) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        fun tryAt(index: Int, lastError: String) {
            if (index >= ids.size) {
                onFailed(if (ids.isEmpty()) "No ad unit IDs configured" else lastError)
                return
            }
            val id = ids[index]
            attempt(
                id,
                { value -> onLoaded(value, id) },
                { error -> tryAt(index + 1, error) })
        }
        tryAt(0, "No fill from configured ad units")
    }
}
