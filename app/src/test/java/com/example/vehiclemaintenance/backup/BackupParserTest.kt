package com.example.vehiclemaintenance.backup

import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.vehicles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BackupParserTest {

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")

    private val item = MaintenanceItem(
        id = "i-1",
        vehicleId = "v-1",
        name = "Oil change",
        mileageInterval = 5000,
        recurrence = Interval(6, IntervalUnit.MONTHS),
        reminder = Interval(5, IntervalUnit.MONTHS),
        lastDoneDate = LocalDate.of(2026, 3, 15),
        lastDoneMileage = 42000,
        lastNotifiedAt = "2026-08-01T09:00:00Z",
    )

    private val entry = ServiceLogEntry(
        id = "s-1",
        vehicleId = "v-1",
        maintenanceItemId = "i-1",
        description = "Oil and filter",
        date = LocalDate.of(2026, 3, 15),
        odometer = 42000,
        cost = 6499,
        notes = "Belts look fine",
    )

    private val store = MaintenanceStore(
        vehicles = listOf(vehicle),
        maintenanceItems = listOf(item),
        serviceLogEntries = listOf(entry),
    )

    private fun parse(json: String) = parseBackup(json)

    @Test
    fun `a full export round trips back to the same store`() {
        val result = parse(storeJson.encodeToString(store))

        assertEquals(BackupParse.Valid(store), result)
    }

    @Test
    fun `an export with no vehicles is a valid backup`() {
        val empty = MaintenanceStore()

        assertEquals(BackupParse.Valid(empty), parse(storeJson.encodeToString(empty)))
    }

    @Test
    fun `an empty object is rejected rather than read as an empty store`() {
        assertEquals(BackupParse.Invalid, parse("{}"))
    }

    @Test
    fun `malformed json is rejected`() {
        assertEquals(BackupParse.Invalid, parse("{ not json"))
        assertEquals(BackupParse.Invalid, parse(""))
        assertEquals(BackupParse.Invalid, parse("plain text, not a backup"))
    }

    @Test
    fun `an array root is rejected`() {
        assertEquals(BackupParse.Invalid, parse("""[{"schemaVersion":1}]"""))
    }

    @Test
    fun `a missing schema version is rejected`() {
        assertEquals(BackupParse.Invalid, parse("""{"vehicles":[]}"""))
    }

    @Test
    fun `a non integer schema version is rejected`() {
        assertEquals(BackupParse.Invalid, parse("""{"schemaVersion":"1"}"""))
        assertEquals(BackupParse.Invalid, parse("""{"schemaVersion":1.5}"""))
        assertEquals(BackupParse.Invalid, parse("""{"schemaVersion":null}"""))
    }

    @Test
    fun `a version this build does not know is reported with its number`() {
        assertEquals(BackupParse.UnsupportedVersion(2), parse("""{"schemaVersion":2}"""))
        assertEquals(BackupParse.UnsupportedVersion(0), parse("""{"schemaVersion":0}"""))
    }

    @Test
    fun `a bad date is rejected instead of throwing`() {
        val json = storeJson.encodeToString(store).replace("2026-03-15", "not-a-date")

        assertEquals(BackupParse.Invalid, parse(json))
    }

    @Test
    fun `a missing required field is rejected`() {
        val json = """{"schemaVersion":1,"vehicles":[{"id":"v-1","year":2014}]}"""

        assertEquals(BackupParse.Invalid, parse(json))
    }

    @Test
    fun `duplicate ids are rejected in every list`() {
        assertEquals(
            BackupParse.Invalid,
            parse(storeJson.encodeToString(store.copy(vehicles = listOf(vehicle, vehicle)))),
        )
        assertEquals(
            BackupParse.Invalid,
            parse(storeJson.encodeToString(store.copy(maintenanceItems = listOf(item, item)))),
        )
        assertEquals(
            BackupParse.Invalid,
            parse(storeJson.encodeToString(store.copy(serviceLogEntries = listOf(entry, entry)))),
        )
    }

    @Test
    fun `an item owned by a missing vehicle is rejected`() {
        val orphaned = store.copy(maintenanceItems = listOf(item.copy(vehicleId = "gone")))

        assertEquals(BackupParse.Invalid, parse(storeJson.encodeToString(orphaned)))
    }

    @Test
    fun `a log entry owned by a missing vehicle is rejected`() {
        val orphaned = store.copy(serviceLogEntries = listOf(entry.copy(vehicleId = "gone")))

        assertEquals(BackupParse.Invalid, parse(storeJson.encodeToString(orphaned)))
    }

    @Test
    fun `a log entry pointing at a deleted item is still valid`() {
        val kept = store.copy(serviceLogEntries = listOf(entry.copy(maintenanceItemId = "gone")))

        assertEquals(BackupParse.Valid(kept), parse(storeJson.encodeToString(kept)))
    }
}
