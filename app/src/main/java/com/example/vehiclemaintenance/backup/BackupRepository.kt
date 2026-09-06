package com.example.vehiclemaintenance.backup

import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.StoreUnavailableException
import com.example.vehiclemaintenance.data.storeJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface BackupRepository {

    /** The whole store as the JSON an export writes, or a failure when it was never read. */
    suspend fun exportSnapshot(): StoreResult<String>

    suspend fun applyBackup(store: MaintenanceStore): StoreResult<Unit>
}

class JsonBackupRepository(
    private val holder: MaintenanceStoreHolder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BackupRepository {

    override suspend fun exportSnapshot(): StoreResult<String> = withContext(ioDispatcher) {
        // A store we could not read falls back to an empty one in memory, and handing the user an
        // empty file they believe is a backup is worse than refusing to export at all.
        if (!holder.isLoaded) {
            StoreResult.Failure(StoreUnavailableException())
        } else {
            StoreResult.Success(storeJson.encodeToString(holder.state.value))
        }
    }

    override suspend fun applyBackup(store: MaintenanceStore): StoreResult<Unit> =
        holder.replace(store)
}
