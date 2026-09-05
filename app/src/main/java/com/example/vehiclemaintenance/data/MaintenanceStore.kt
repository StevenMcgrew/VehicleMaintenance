package com.example.vehiclemaintenance.data

import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CURRENT_SCHEMA_VERSION = 1

/** The whole on-disk store, which is also the export format. */
@Serializable
data class MaintenanceStore(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val vehicles: List<Vehicle> = emptyList(),
    val maintenanceItems: List<MaintenanceItem> = emptyList(),
    val serviceLogEntries: List<ServiceLogEntry> = emptyList(),
)

val storeJson: Json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
    prettyPrint = false
}
