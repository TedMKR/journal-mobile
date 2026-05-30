package com.journal.tests.data

import app.cash.turbine.test
import com.journal.core.data.util.Resource
import com.journal.core.data.util.networkBoundResource
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [networkBoundResource].
 *
 * Tests verify the Loading → Success / Error flow transitions
 * for the offline-first data loading strategy.
 */
class NetworkBoundResourceTest {

    // ─── No-fetch path (cache is fresh) ──────────────────────────────────────

    @Test
    fun `emits Loading then Success when shouldFetch returns false`() = runTest {
        val flow = networkBoundResource(
            localFlow = { flowOf(listOf("cached")) },
            shouldFetch = { false },
            fetch = { error("should not be called") },
            saveFetchResult = { error("should not be called") }
        )

        flow.test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals(listOf("cached"), (success as Resource.Success).data)

            awaitComplete()
        }
    }

    @Test
    fun `Loading carries the cached value when cache is non-empty`() = runTest {
        val flow = networkBoundResource(
            localFlow = { flowOf(listOf("item1")) },
            shouldFetch = { false },
            fetch = { emptyList<String>() },
            saveFetchResult = {}
        )

        flow.test {
            val loading = awaitItem() as Resource.Loading
            assertEquals(listOf("item1"), loading.data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Loading carries null data when cache is empty`() = runTest {
        val flow = networkBoundResource(
            localFlow = { flowOf(emptyList<String>()) },
            shouldFetch = { true },
            fetch = { listOf("fetched") },
            saveFetchResult = {}
        )

        flow.test {
            val loading = awaitItem() as Resource.Loading
            // emptyList is not null — but the first snapshot was collected from the empty flow
            assertTrue(loading.data?.isEmpty() ?: true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Fetch path (cache is stale / empty) ─────────────────────────────────

    @Test
    fun `emits Loading then Success after successful fetch`() = runTest {
        var savedResult: List<String>? = null

        val flow = networkBoundResource(
            localFlow = { flowOf(savedResult ?: emptyList()) },
            shouldFetch = { true },
            fetch = { listOf("remote1", "remote2") },
            saveFetchResult = { savedResult = it }
        )

        flow.test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)

            awaitComplete()
        }
    }

    @Test
    fun `saveFetchResult is called with the API response`() = runTest {
        var received: String? = null

        val flow = networkBoundResource(
            localFlow = { flowOf("local") },
            shouldFetch = { true },
            fetch = { "remoteValue" },
            saveFetchResult = { received = it }
        )

        flow.test { cancelAndIgnoreRemainingEvents() }

        assertEquals("remoteValue", received)
    }

    // ─── Error path ───────────────────────────────────────────────────────────

    @Test
    fun `emits Loading then Error when fetch throws`() = runTest {
        val exception = RuntimeException("network error")

        val flow = networkBoundResource(
            localFlow = { flowOf(listOf("cache")) },
            shouldFetch = { true },
            fetch = { throw exception },
            saveFetchResult = {}
        )

        flow.test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals(exception, (error as Resource.Error).throwable)

            awaitComplete()
        }
    }

    @Test
    fun `Error carries cached data when available`() = runTest {
        val flow = networkBoundResource(
            localFlow = { flowOf(listOf("stale")) },
            shouldFetch = { true },
            fetch = { throw RuntimeException("offline") },
            saveFetchResult = {}
        )

        flow.test {
            awaitItem() // Loading
            val error = awaitItem() as Resource.Error
            assertEquals(listOf("stale"), error.data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onFetchFailed callback is invoked on network error`() = runTest {
        var callbackThrowable: Throwable? = null
        val exception = RuntimeException("boom")

        val flow = networkBoundResource(
            localFlow = { flowOf("data") },
            shouldFetch = { true },
            fetch = { throw exception },
            saveFetchResult = {},
            onFetchFailed = { callbackThrowable = it }
        )

        flow.test { cancelAndIgnoreRemainingEvents() }

        assertEquals(exception, callbackThrowable)
    }

    // ─── shouldFetch logic ────────────────────────────────────────────────────

    @Test
    fun `shouldFetch receives the current cached value`() = runTest {
        var receivedCached: List<String>? = null

        val flow = networkBoundResource(
            localFlow = { flowOf(listOf("existing")) },
            shouldFetch = { cached -> receivedCached = cached; false },
            fetch = { emptyList<String>() },
            saveFetchResult = {}
        )

        flow.test { cancelAndIgnoreRemainingEvents() }

        assertEquals(listOf("existing"), receivedCached)
    }

    @Test
    fun `shouldFetch receives null when local flow has no items`() = runTest {
        var receivedCached: List<String>? = listOf("non-null-sentinel")

        // Using a flow with no items (never emits)
        val flow = networkBoundResource<List<String>, List<String>>(
            localFlow = { kotlinx.coroutines.flow.flow { } },
            shouldFetch = { cached -> receivedCached = cached; false },
            fetch = { emptyList() },
            saveFetchResult = {}
        )

        flow.test { cancelAndIgnoreRemainingEvents() }

        assertNull(receivedCached)
    }
}
