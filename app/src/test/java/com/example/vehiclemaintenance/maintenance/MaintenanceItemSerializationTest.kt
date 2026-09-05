package com.example.vehiclemaintenance.maintenance

import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.storeJson
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceItemSerializationTest {

    private val fullItem = MaintenanceItem(
        id = "m-1",
        vehicleId = "v-1",
        name = "Oil change",
        mileageInterval = 5000,
        recurrence = Interval(6, IntervalUnit.MONTHS),
        reminder = Interval(5, IntervalUnit.MONTHS),
        lastDoneDate = LocalDate.of(2026, 3, 15),
        lastDoneMileage = 42000,
    )

    private val minimalItem = MaintenanceItem(
        id = "m-2",
        vehicleId = "v-1",
        name = "Tire rotation",
        reminder = Interval(90, IntervalUnit.DAYS),
    )

    private fun encodedItem(item: MaintenanceItem): String =
        storeJson.encodeToString(MaintenanceStore(maintenanceItems = listOf(item)))

    private fun itemJson(item: MaintenanceItem) =
        storeJson.parseToJsonElement(encodedItem(item)).jsonObject["maintenanceItems"]!!
            .jsonArray.single().jsonObject

    @Test
    fun `a round trip preserves every field`() {
        val store = MaintenanceStore(maintenanceItems = listOf(fullItem))

        val decoded = storeJson.decodeFromString<MaintenanceStore>(storeJson.encodeToString(store))

        assertEquals(fullItem, decoded.maintenanceItems.single())
    }

    @Test
    fun `absent optional fields are omitted rather than written as null`() {
        val keys = itemJson(minimalItem).keys

        assertEquals(setOf("id", "vehicleId", "name", "reminder"), keys)
    }

    @Test
    fun `an interval unit encodes as its enum name`() {
        val reminder = itemJson(fullItem)["reminder"]!!.jsonObject

        assertEquals("5", reminder["value"]?.toString())
        assertEquals("\"MONTHS\"", reminder["unit"]?.toString())
    }

    @Test
    fun `a last done date encodes as an ISO-8601 day`() {
        assertEquals("\"2026-03-15\"", itemJson(fullItem)["lastDoneDate"]?.toString())
    }

    @Test
    fun `a last done date decodes back to the same day`() {
        val json = """
            {"schemaVersion":1,"vehicles":[],"maintenanceItems":[
              {"id":"m-1","vehicleId":"v-1","name":"Oil change",
               "reminder":{"value":5,"unit":"MONTHS"},"lastDoneDate":"2026-03-15"}
            ],"serviceLogEntries":[]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)

        assertEquals(LocalDate.of(2026, 3, 15), decoded.maintenanceItems.single().lastDoneDate)
    }

    @Test
    fun `an unknown item key decodes without throwing`() {
        val json = """
            {"schemaVersion":1,"vehicles":[],"maintenanceItems":[
              {"id":"m-1","vehicleId":"v-1","name":"Oil change",
               "reminder":{"value":5,"unit":"MONTHS"},"futureItemKey":"ignored"}
            ],"serviceLogEntries":[]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)

        assertEquals("m-1", decoded.maintenanceItems.single().id)
        assertNull(decoded.maintenanceItems.single().recurrence)
    }

    @Test
    fun `lastNotifiedAt round trips untouched for the notifications feature`() {
        val json = """
            {"schemaVersion":1,"vehicles":[],"maintenanceItems":[
              {"id":"m-1","vehicleId":"v-1","name":"Oil change",
               "reminder":{"value":5,"unit":"MONTHS"},
               "lastNotifiedAt":"2026-04-01T09:00:00Z"}
            ],"serviceLogEntries":[]}
        """.trimIndent()

        val decoded = storeJson.decodeFromString<MaintenanceStore>(json)
        val reEncoded = storeJson.encodeToString(decoded)

        assertEquals("2026-04-01T09:00:00Z", decoded.maintenanceItems.single().lastNotifiedAt)
        assertTrue(reEncoded.contains("\"lastNotifiedAt\":\"2026-04-01T09:00:00Z\""))
    }

    @Test
    fun `typed items and raw log entries survive the same round trip`() {
        val json = """
            {"schemaVersion":1,"vehicles":[],"maintenanceItems":[
              {"id":"m-1","vehicleId":"v-1","name":"Oil change",
               "reminder":{"value":5,"unit":"MONTHS"}}
            ],"serviceLogEntries":[{"id":"s-1","vehicleId":"v-1","odometer":42000}]}
        """.trimIndent()

        val reEncoded = storeJson.encodeToString(storeJson.decodeFromString<MaintenanceStore>(json))

        assertTrue(reEncoded.contains("\"odometer\":42000"))
        assertTrue(reEncoded.contains("\"name\":\"Oil change\""))
        assertFalse(reEncoded.contains("null"))
    }
}
