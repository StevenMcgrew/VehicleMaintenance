package com.example.vehiclemaintenance.servicelog

import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.storeJson
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class ServiceLogEntrySerializationTest {

    private val fullEntry = ServiceLogEntry(
        id = "s-1",
        vehicleId = "v-1",
        maintenanceItemId = "m-1",
        description = "Oil and filter",
        date = LocalDate.of(2026, 3, 15),
        odometer = 42000,
        cost = 6499,
        notes = "shop said belts look fine",
    )

    private val minimalEntry = ServiceLogEntry(
        id = "s-2",
        vehicleId = "v-1",
        description = "Wiper blades",
        date = LocalDate.of(2026, 4, 1),
        odometer = 42500,
    )

    private fun entryJson(entry: ServiceLogEntry) = storeJson
        .parseToJsonElement(storeJson.encodeToString(MaintenanceStore(serviceLogEntries = listOf(entry))))
        .jsonObject["serviceLogEntries"]!!
        .jsonArray.single().jsonObject

    @Test
    fun `a round trip preserves every field`() {
        val store = MaintenanceStore(serviceLogEntries = listOf(fullEntry))

        val decoded = storeJson.decodeFromString<MaintenanceStore>(storeJson.encodeToString(store))

        assertEquals(fullEntry, decoded.serviceLogEntries.single())
    }

    @Test
    fun `an entry with no cost, notes, or item link omits those keys and reads back equal`() {
        val encoded = storeJson.encodeToString(MaintenanceStore(serviceLogEntries = listOf(minimalEntry)))

        assertEquals(
            setOf("id", "vehicleId", "description", "date", "odometer"),
            entryJson(minimalEntry).keys,
        )
        assertFalse(encoded.contains("null"))
        assertEquals(
            minimalEntry,
            storeJson.decodeFromString<MaintenanceStore>(encoded).serviceLogEntries.single(),
        )
    }

    @Test
    fun `a date encodes as an ISO-8601 day`() {
        assertEquals("\"2026-03-15\"", entryJson(fullEntry)["date"]?.toString())
    }

    @Test
    fun `cost encodes as whole minor units`() {
        assertEquals("6499", entryJson(fullEntry)["cost"]?.toString())
    }

    @Test
    fun `a stored entry decodes into the typed model`() {
        val json = """
            {"schemaVersion":1,"vehicles":[],"maintenanceItems":[],
             "serviceLogEntries":[{"id":"s-1","vehicleId":"v-1","maintenanceItemId":"m-1",
              "description":"Oil and filter","date":"2026-03-15","odometer":42000,
              "cost":6499,"notes":"shop said belts look fine"}]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)

        assertEquals(fullEntry, decoded.serviceLogEntries.single())
    }
}
