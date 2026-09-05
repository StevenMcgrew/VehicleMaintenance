package com.example.vehiclemaintenance.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed interface StoreResult<out T> {
    data class Success<T>(val value: T) : StoreResult<T>
    data class Failure(val cause: Throwable) : StoreResult<Nothing>
}

/**
 * The single JSON file that holds everything, read and written whole.
 *
 * A partial overwrite would destroy the user's only copy of their history, so every write lands in
 * a temp file that is synced and then atomically renamed over the target.
 */
class JsonFileStore(
    private val file: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    suspend fun load(): StoreResult<MaintenanceStore> = withContext(ioDispatcher) {
        mutex.withLock {
            if (!file.exists()) {
                return@withLock StoreResult.Success(MaintenanceStore())
            }
            try {
                StoreResult.Success(storeJson.decodeFromString<MaintenanceStore>(file.readText()))
            } catch (e: SerializationException) {
                StoreResult.Failure(e)
            } catch (e: IOException) {
                StoreResult.Failure(e)
            }
        }
    }

    suspend fun save(store: MaintenanceStore): StoreResult<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            val temp = tempFile()
            try {
                file.parentFile?.mkdirs()
                FileOutputStream(temp).use { out ->
                    out.write(storeJson.encodeToString(store).toByteArray())
                    out.flush()
                    out.fd.sync()
                }
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
                StoreResult.Success(Unit)
            } catch (e: IOException) {
                temp.delete()
                StoreResult.Failure(e)
            } catch (e: SerializationException) {
                temp.delete()
                StoreResult.Failure(e)
            }
        }
    }

    private fun tempFile(): File = File(file.parentFile, file.name + TEMP_SUFFIX)

    private companion object {
        const val TEMP_SUFFIX = ".tmp"
    }
}
