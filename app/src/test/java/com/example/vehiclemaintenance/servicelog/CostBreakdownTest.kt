package com.example.vehiclemaintenance.servicelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CostBreakdownTest {

    private var nextId = 0

    private fun tracked(
        description: String,
        cost: Int?,
        date: String = "2026-06-15",
        itemId: String = "m-1",
    ) = entry(description, cost, date, itemId)

    private fun repair(description: String, cost: Int?, date: String = "2026-06-15") =
        entry(description, cost, date, itemId = null)

    private fun entry(description: String, cost: Int?, date: String, itemId: String?) =
        ServiceLogEntry(
            id = "e-${nextId++}",
            vehicleId = "v-1",
            maintenanceItemId = itemId,
            description = description,
            date = LocalDate.parse(date),
            odometer = 50_000,
            cost = cost,
        )

    private fun breakdownOf(vararg entries: ServiceLogEntry) = costBreakdownOf(entries.toList())

    @Test
    fun `no costed entries give no slices`() {
        assertTrue(breakdownOf().isEmpty())
        assertTrue(breakdownOf(tracked("Oil change", null), repair("Alternator", null)).isEmpty())
    }

    @Test
    fun `every logged instance of a tracked item sums into one slice`() {
        val slices = breakdownOf(
            tracked("Oil change", 5_000, "2024-01-10"),
            tracked("Oil change", 6_000, "2025-01-10"),
            tracked("Oil change", 7_000, "2026-01-10"),
        )

        assertEquals(listOf(CostSlice("Oil change", 18_000, 100)), slices)
    }

    @Test
    fun `names group ignoring case and extra spaces`() {
        val slices = breakdownOf(
            tracked("Oil change", 1_000, "2025-01-10"),
            tracked("  oil   CHANGE ", 1_000, "2025-02-10", itemId = "m-2"),
        )

        assertEquals(1, slices.size)
        assertEquals(2_000L, slices.single().total)
    }

    @Test
    fun `the slice is named after its newest entry, trimmed`() {
        val slices = breakdownOf(
            tracked("oil change", 1_000, "2024-01-10"),
            tracked("  Oil Change  ", 1_000, "2026-01-10"),
            tracked("OIL CHANGE", 1_000, "2025-01-10"),
        )

        assertEquals("Oil Change", slices.single().name)
    }

    @Test
    fun `entries sharing the newest date take the name of the one listed first`() {
        val slices = breakdownOf(
            tracked("Oil Change", 1_000, "2026-01-10"),
            tracked("oil change", 1_000, "2026-01-10"),
        )

        assertEquals("Oil Change", slices.single().name)
    }

    @Test
    fun `ad-hoc repairs sum into one unnamed slice whatever their descriptions`() {
        val slices = breakdownOf(
            repair("Replaced the alternator", 78_250),
            repair("Windshield chip", 5_000),
            repair("Replaced the alternator", 1_750),
        )

        assertEquals(listOf(CostSlice(null, 85_000, 100)), slices)
    }

    @Test
    fun `a deleted item's history, its link cleared, counts as an additional repair`() {
        val slices = breakdownOf(
            tracked("Oil change", 6_000),
            repair("Oil change", 4_000),
        )

        assertEquals(
            listOf(CostSlice("Oil change", 6_000, 60), CostSlice(null, 4_000, 40)),
            slices,
        )
    }

    @Test
    fun `uncosted and zero cost entries are left out`() {
        val slices = breakdownOf(
            tracked("Oil change", 5_000),
            tracked("Oil change", null),
            tracked("Oil change", 0),
            tracked("Tire rotation", 0, itemId = "m-2"),
            tracked("Wipers", null, itemId = "m-3"),
            repair("Free warranty fix", 0),
        )

        assertEquals(listOf(CostSlice("Oil change", 5_000, 100)), slices)
    }

    @Test
    fun `tracked slices run largest first, ties by name, with additional repairs last`() {
        val slices = breakdownOf(
            repair("Alternator", 90_000),
            tracked("brakes", 2_000, itemId = "m-1"),
            tracked("Tires", 50_000, itemId = "m-2"),
            tracked("Air filter", 2_000, itemId = "m-3"),
        )

        assertEquals(listOf("Tires", "Air filter", "brakes", null), slices.map { it.name })
    }

    @Test
    fun `percents round half up to whole numbers`() {
        // 1/8 = 12.5% rounds up to 13; 7/8 = 87.5% rounds up to 88.
        val slices = breakdownOf(
            tracked("Brakes", 7_000, itemId = "m-1"),
            tracked("Wipers", 1_000, itemId = "m-2"),
        )

        assertEquals(listOf(88, 13), slices.map { it.percent })
    }

    @Test
    fun `a slice under half a percent shows as zero`() {
        val slices = breakdownOf(
            tracked("Engine", 99_999_00, itemId = "m-1"),
            tracked("Wipers", 1_00, itemId = "m-2"),
        )

        assertEquals(listOf(100, 0), slices.map { it.percent })
        assertTrue(slices.all { it.total > 0 })
    }

    @Test
    fun `totals beyond the Int range accumulate as Long`() {
        val slices = breakdownOf(
            tracked("Engine", Int.MAX_VALUE, "2025-01-10"),
            tracked("Engine", Int.MAX_VALUE, "2026-01-10"),
        )

        assertEquals(2L * Int.MAX_VALUE, slices.single().total)
        assertEquals(100, slices.single().percent)
    }

    @Test
    fun `service history carries the breakdown`() {
        val history = serviceHistoryOf(listOf(tracked("Oil change", 5_000), repair("Alternator", 5_000)))

        assertEquals(
            listOf(CostSlice("Oil change", 5_000, 50), CostSlice(null, 5_000, 50)),
            history.breakdown,
        )
    }

    @Test
    fun `colors cycle through the palette for tracked slices`() {
        assertEquals(listOf(0, 1, 2), sliceColorIndices(3, hasAdditionalRepairs = false, PALETTE))
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 0, 1),
            sliceColorIndices(10, hasAdditionalRepairs = false, PALETTE),
        )
    }

    @Test
    fun `additional repairs take their own slot after the tracked slices`() {
        assertEquals(
            listOf(0, 1, ADDITIONAL_REPAIRS_SLOT),
            sliceColorIndices(2, hasAdditionalRepairs = true, PALETTE),
        )
        assertEquals(listOf(ADDITIONAL_REPAIRS_SLOT), sliceColorIndices(0, hasAdditionalRepairs = true, PALETTE))
    }

    @Test
    fun `the last slice never shares a color with the first slice it touches`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 1),
            sliceColorIndices(9, hasAdditionalRepairs = false, PALETTE),
        )
        // Additional repairs sit between them, so the ninth tracked slice keeps slot 0.
        assertEquals(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 0, ADDITIONAL_REPAIRS_SLOT),
            sliceColorIndices(9, hasAdditionalRepairs = true, PALETTE),
        )
    }

    @Test
    fun `slice colors come from the palette with the neutral color for additional repairs`() {
        val palette = listOf("blue", "amber", "green")

        assertEquals(
            listOf("blue", "amber", "grey"),
            sliceColors(listOf("Oil change", "Brakes", null), palette, "grey"),
        )
        assertEquals(
            listOf("blue", "amber", "green", "blue", "amber"),
            sliceColors(listOf("a", "b", "c", "d", "e"), palette, "grey"),
        )
        assertEquals(listOf("grey"), sliceColors(listOf(null), palette, "grey"))
        assertTrue(sliceColors(emptyList(), palette, "grey").isEmpty())
    }

    @Test
    fun `a single slice keeps the first color`() {
        assertEquals(listOf(0), sliceColorIndices(1, hasAdditionalRepairs = false, PALETTE))
        assertTrue(sliceColorIndices(0, hasAdditionalRepairs = false, PALETTE).isEmpty())
    }

    private companion object {
        const val PALETTE = 8
    }
}
