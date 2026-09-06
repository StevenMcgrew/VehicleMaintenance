package com.example.vehiclemaintenance.backup

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Whatever the user picked, refuse to hold more than this in memory. */
const val MAX_BACKUP_BYTES = 8 * 1024 * 1024

sealed interface BackupRead {
    data class Success(val json: String) : BackupRead
    data object TooLarge : BackupRead
    data object Unreadable : BackupRead
}

/** The Storage Access Framework side of export and import, kept off the main thread. */
class BackupFiles(
    private val resolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun write(uri: Uri, json: String): Boolean = withContext(ioDispatcher) {
        try {
            // "wt" truncates, so overwriting a longer file cannot leave its tail behind.
            val stream = resolver.openOutputStream(uri, "wt") ?: return@withContext false
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    suspend fun read(uri: Uri): BackupRead = withContext(ioDispatcher) {
        try {
            val stream = resolver.openInputStream(uri) ?: return@withContext BackupRead.Unreadable
            stream.use { readCapped(it) }
        } catch (e: IOException) {
            BackupRead.Unreadable
        } catch (e: SecurityException) {
            BackupRead.Unreadable
        }
    }

    private fun readCapped(input: InputStream): BackupRead {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(CHUNK_BYTES)
        while (true) {
            val count = input.read(chunk)
            if (count == -1) break
            if (buffer.size() + count > MAX_BACKUP_BYTES) return BackupRead.TooLarge
            buffer.write(chunk, 0, count)
        }
        return BackupRead.Success(buffer.toString(Charsets.UTF_8.name()))
    }

    private companion object {
        const val CHUNK_BYTES = 8 * 1024
    }
}
