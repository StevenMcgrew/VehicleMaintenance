package com.example.vehiclemaintenance.data

import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

const val CURRENT_SCHEMA_VERSION = 1

/**
 * The whole on-disk store, which is also the export format.
 *
 * Maintenance items and service log entries stay as raw [JsonObject] until the features that own
 * them land, so this feature can preserve entries it does not understand without inventing their
 * contract.
 */
@Serializable
data class MaintenanceStore(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val vehicles: List<Vehicle> = emptyList(),
    val maintenanceItems: List<JsonObject> = emptyList(),
    val serviceLogEntries: List<JsonObject> = emptyList(),
)

val storeJson: Json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
    prettyPrint = false
}
