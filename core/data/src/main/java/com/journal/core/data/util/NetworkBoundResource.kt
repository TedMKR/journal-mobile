package com.journal.core.data.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Generic offline-first resource loader implementing the NetworkBoundResource pattern.
 *
 * Flow of events:
 * 1. Immediately emits [Resource.Loading] + local cached data (if any).
 * 2. Decides whether to fetch from network ([shouldFetch]).
 * 3. On fetch success — saves result to DB, then re-emits from the DB flow.
 * 4. On fetch failure — emits [Resource.Error] but keeps showing cached data.
 *
 * @param localFlow    Flow that observes the Room table.
 * @param shouldFetch  Returns true when a fresh network call is needed.
 * @param fetch        Suspend function that calls the remote API.
 * @param saveFetchResult Suspend function that persists the API response to Room.
 * @param onFetchFailed Optional callback invoked when the network call throws.
 */
fun <Local, Remote> networkBoundResource(
    localFlow: () -> Flow<Local>,
    shouldFetch: suspend (Local?) -> Boolean = { true },
    fetch: suspend () -> Remote,
    saveFetchResult: suspend (Remote) -> Unit,
    onFetchFailed: (Throwable) -> Unit = {}
): Flow<Resource<Local>> = flow {

    val local = localFlow()

    // 1. Snapshot of current cache
    val cachedValue = local.firstOrNull()

    emit(Resource.Loading(cachedValue))

    if (shouldFetch(cachedValue)) {
        try {
            val remote = fetch()
            saveFetchResult(remote)
            // 2. Re-read from DB so the flow reflects merged state
            emitAll(local.map { Resource.Success(it) })
        } catch (t: Throwable) {
            onFetchFailed(t)
            emitAll(local.map { Resource.Error(t, it) })
        }
    } else {
        emitAll(local.map { Resource.Success(it) })
    }
}

sealed class Resource<out T> {
    /** Initial emission while a network call is in-flight. [data] may be null if cache is empty. */
    data class Loading<T>(val data: T?) : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val throwable: Throwable, val data: T? = null) : Resource<T>()
}
