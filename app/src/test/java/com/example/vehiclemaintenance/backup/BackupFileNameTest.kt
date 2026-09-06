package com.example.vehiclemaintenance.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BackupFileNameTest {

    @Test
    fun `the offered name carries the date it was taken`() {
        assertEquals(
            "vehicle-maintenance-2026-09-06.json",
            backupFileName(LocalDate.of(2026, 9, 6)),
        )
    }

    @Test
    fun `single digit months and days are padded`() {
        assertEquals(
            "vehicle-maintenance-2026-01-02.json",
            backupFileName(LocalDate.of(2026, 1, 2)),
        )
    }
}
