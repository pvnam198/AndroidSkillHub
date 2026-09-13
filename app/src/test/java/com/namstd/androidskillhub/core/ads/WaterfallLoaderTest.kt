package com.namstd.androidskillhub.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaterfallLoaderTest {
    @Test
    fun triesInOrderAndStopsAtFirstSuccess() {
        val attempts = mutableListOf<String>()
        var result: String? = null

        WaterfallLoader<String>(listOf("first", "second", "third")).load(
            attempt = { id, loaded, failed ->
                attempts += id
                if (id == "second") loaded("loaded") else failed("failed $id")
            },
            onLoaded = { value, _ -> result = value },
            onFailed = {},
        )

        assertEquals(listOf("first", "second"), attempts)
        assertEquals("loaded", result)
    }

    @Test
    fun skipsBlankIdsAndReportsEmptyLists() {
        val attempts = mutableListOf<String>()
        var error: String? = null
        WaterfallLoader<String>(listOf("", "  ")).load(
            attempt = { id, _, _ -> attempts += id },
            onLoaded = { _, _ -> },
            onFailed = { error = it },
        )
        assertEquals(emptyList<String>(), attempts)
        assertEquals("No ad unit IDs configured", error)
    }

    @Test
    fun everyNewLoadStartsAgainAtFirstId() {
        val firstAttempts = mutableListOf<String>()
        val loader = WaterfallLoader<String>(listOf("one", "two"))
        repeat(2) {
            loader.load(
                attempt = { id, loaded, _ -> firstAttempts += id; loaded(id) },
                onLoaded = { _, _ -> },
                onFailed = {},
            )
        }
        assertEquals(listOf("one", "one"), firstAttempts)
    }

    @Test
    fun returnsLastFailureOnlyAfterAllIdsFail() {
        var result: String? = null
        var error: String? = null
        WaterfallLoader<String>(listOf("one", "two")).load(
            attempt = { id, _, failed -> failed("error-$id") },
            onLoaded = { value, _ -> result = value },
            onFailed = { error = it },
        )
        assertNull(result)
        assertEquals("error-two", error)
    }
}
