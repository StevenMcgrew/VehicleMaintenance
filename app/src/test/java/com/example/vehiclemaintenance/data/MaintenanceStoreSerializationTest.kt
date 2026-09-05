package com.example.vehiclemaintenance.data

import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceStoreSerializationTest {

    private val fullVehicle = Vehicle(
        id = "v-1",
        year = 2014,
        make = "Toyota",
        model = "Tacoma",
        engine = "4.0L V6",
    )

    @Test
    fun `round trip preserves every vehicle field`() {
        val store = MaintenanceStore(vehicles = listOf(fullVehicle))

        val decoded = storeJson.decodeFromString<MaintenanceStore>(storeJson.encodeToString(store))

        assertEquals(store, decoded)
        assertEquals(fullVehicle, decoded.vehicles.single())
    }

    @Test
    fun `schema version is always written as the current version`() {
        val encoded = storeJson.encodeToString(MaintenanceStore())

        val schemaVersion = storeJson.parseToJsonElement(encoded).jsonObject["schemaVersion"]
        assertEquals("1", schemaVersion?.toString())
        assertEquals(1, CURRENT_SCHEMA_VERSION)
    }

    @Test
    fun `all four root keys are written even when the lists are empty`() {
        val encoded = storeJson.encodeToString(MaintenanceStore())

        val keys = storeJson.parseToJsonElement(encoded).jsonObject.keys
        assertEquals(
            setOf("schemaVersion", "vehicles", "maintenanceItems", "serviceLogEntries"),
            keys,
        )
    }

    @Test
    fun `a stored nickname from an older file decodes and is dropped on rewrite`() {
        val json = """
            {"schemaVersion":1,"vehicles":[
              {"id":"v-1","nickname":"Daily","year":2014,"make":"Toyota",
               "model":"Tacoma","engine":"4.0L V6"}
            ],"maintenanceItems":[],"serviceLogEntries":[]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)

        assertEquals(fullVehicle, decoded.vehicles.single())
        assertFalse(storeJson.encodeToString(decoded).contains("nickname"))
    }

    @Test
    fun `unknown root and vehicle keys decode without throwing`() {
        val json = """
            {"schemaVersion":2,"futureRootKey":{"a":1},"vehicles":[
              {"id":"v-1","year":2014,"make":"Toyota","model":"Tacoma","engine":"4.0L V6",
               "futureVehicleKey":"ignored"}
            ],"maintenanceItems":[],"serviceLogEntries":[]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)

        assertEquals(2, decoded.schemaVersion)
        assertEquals("v-1", decoded.vehicles.single().id)
    }

    @Test
    fun `unrecognized maintenance and log entries survive a round trip`() {
        val json = """
            {"schemaVersion":1,"vehicles":[],
             "maintenanceItems":[{"id":"m-1","vehicleId":"v-1","name":"Oil change"}],
             "serviceLogEntries":[{"id":"s-1","vehicleId":"v-1","odometer":42000}]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)
        val reEncoded = storeJson.encodeToString(decoded)

        assertTrue(reEncoded.contains("\"name\":\"Oil change\""))
        assertTrue(reEncoded.contains("\"odometer\":42000"))
    }
}
