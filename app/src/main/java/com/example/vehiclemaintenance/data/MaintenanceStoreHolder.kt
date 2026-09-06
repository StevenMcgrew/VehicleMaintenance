package com.example.vehiclemaintenance.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Raised when the store could not be read, so writing would overwrite data we failed to parse. */
class StoreUnavailableException : IllegalStateException(
    "The maintenance store could not be read, so changes cannot be saved.",
)

/** What a caller wants done to the store, or why it refused. */
sealed interface StoreUpdate<out T> {
    data class Write<T>(val store: MaintenanceStore, val value: T) : StoreUpdate<T>
    data class Reject(val cause: Throwable) : StoreUpdate<Nothing>
}

/**
 * The one in-memory copy of the store, shared by every repository over the same file.
 *
 * Each repository owning its own cache would be a lost-update bug the moment a second one existed:
 * both would read, both would write their whole root, and the later write would drop the earlier
 * one's changes.
 */
class MaintenanceStoreHolder(private val file: JsonFileStore) {

    private val mutex = Mutex()
    private val _state = MutableStateFlow(MaintenanceStore())
    val state: StateFlow<MaintenanceStore> = _state.asStateFlow()

    private var loaded = false

    /** Whether the file was read successfully, so its contents are safe to write back out. */
    val isLoaded: Boolean get() = loaded

    suspend fun load(): StoreResult<Unit> = mutex.withLock {
        when (val result = file.load()) {
            is StoreResult.Success -> {
                loaded = true
                _state.value = result.value
                StoreResult.Success(Unit)
            }

            is StoreResult.Failure -> {
                loaded = false
                _state.value = MaintenanceStore()
                result
            }
        }
    }

    /**
     * Overwrites everything with [store], which is how an imported backup lands.
     *
     * This deliberately does not require a successful load: replacing a file we could not parse is
     * exactly how a user recovers from one.
     */
    suspend fun replace(store: MaintenanceStore): StoreResult<Unit> = mutex.withLock {
        when (val saved = file.save(store)) {
            is StoreResult.Success -> {
                loaded = true
                _state.value = store
                StoreResult.Success(Unit)
            }

            is StoreResult.Failure -> saved
        }
    }

    /**
     * Reads the current store, applies [transform], and persists the result under the same lock, so
     * concurrent writers cannot interleave a read with another writer's save.
     */
    suspend fun <T> update(transform: (MaintenanceStore) -> StoreUpdate<T>): StoreResult<T> =
        mutex.withLock {
            if (!loaded) {
                return@withLock StoreResult.Failure(StoreUnavailableException())
            }
            when (val update = transform(_state.value)) {
                is StoreUpdate.Reject -> StoreResult.Failure(update.cause)
                is StoreUpdate.Write -> when (val saved = file.save(update.store)) {
                    is StoreResult.Success -> {
                        _state.value = update.store
                        StoreResult.Success(update.value)
                    }

                    is StoreResult.Failure -> saved
                }
            }
        }
}
