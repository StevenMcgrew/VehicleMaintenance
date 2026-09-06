package com.example.vehiclemaintenance.backup

import com.example.vehiclemaintenance.data.CURRENT_SCHEMA_VERSION
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.storeJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.DateTimeException

sealed interface BackupParse {
    data class Valid(val store: MaintenanceStore) : BackupParse
    data class UnsupportedVersion(val version: Int) : BackupParse
    data object Invalid : BackupParse
}

/**
 * Decides whether [json] is a backup this build can restore, without touching the store.
 *
 * Every field of [MaintenanceStore] has a default and [storeJson] ignores unknown keys, so `{}` and
 * any unrelated JSON document would decode as a valid empty store and wipe the user's history. The
 * explicit `schemaVersion` check is what makes a wrong pick a rejection rather than a data loss.
 */
fun parseBackup(json: String): BackupParse {
    val root = jsonObjectOrNull(json) ?: return BackupParse.Invalid
    val version = schemaVersionOrNull(root) ?: return BackupParse.Invalid
    if (version != CURRENT_SCHEMA_VERSION) return BackupParse.UnsupportedVersion(version)

    val store = try {
        storeJson.decodeFromJsonElement(MaintenanceStore.serializer(), root)
    } catch (e: SerializationException) {
        return BackupParse.Invalid
    } catch (e: IllegalArgumentException) {
        return BackupParse.Invalid
    } catch (e: DateTimeException) {
        // A malformed date reaches LocalDateSerializer as a parse failure, not a decoding one.
        return BackupParse.Invalid
    }

    return if (isConsistent(store)) BackupParse.Valid(store) else BackupParse.Invalid
}

private fun jsonObjectOrNull(json: String): JsonObject? = try {
    storeJson.parseToJsonElement(json) as? JsonObject
} catch (e: SerializationException) {
    null
}

private fun schemaVersionOrNull(root: JsonObject): Int? {
    val primitive = root[SCHEMA_VERSION_KEY] as? JsonPrimitive ?: return null
    if (primitive.isString) return null
    return primitive.content.toIntOrNull()
}

private fun isConsistent(store: MaintenanceStore): Boolean {
    val vehicleIds = store.vehicles.map { it.id }
    if (!isUnique(vehicleIds)) return false
    if (!isUnique(store.maintenanceItems.map { it.id })) return false
    if (!isUnique(store.serviceLogEntries.map { it.id })) return false

    val owners = vehicleIds.toSet()
    if (store.maintenanceItems.any { it.vehicleId !in owners }) return false
    // A dangling maintenanceItemId is fine: deleting an item already keeps its history and clears
    // the link.
    return store.serviceLogEntries.none { it.vehicleId !in owners }
}

private fun isUnique(ids: List<String>): Boolean = ids.size == ids.toSet().size

private const val SCHEMA_VERSION_KEY = "schemaVersion"
