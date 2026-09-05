package com.example.vehiclemaintenance.servicelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ServiceLogFormValidatorTest {

    private val today = LocalDate.of(2026, 9, 5)

    private val validFields = ServiceLogFormFields(
        description = "Oil and filter",
        date = today,
        odometer = "48000",
    )

    private fun validate(fields: ServiceLogFormFields) =
        ServiceLogFormValidator.validate(fields, "v-1", "m-1", today)

    private fun errorsOf(fields: ServiceLogFormFields) =
        (validate(fields) as ServiceLogFormValidation.Invalid).errors

    private fun draftOf(fields: ServiceLogFormFields) =
        (validate(fields) as ServiceLogFormValidation.Valid).draft

    @Test
    fun `a valid form produces a draft carrying the vehicle and item ids`() {
        assertEquals(
            ServiceLogDraft(
                vehicleId = "v-1",
                maintenanceItemId = "m-1",
                description = "Oil and filter",
                date = today,
                odometer = 48000,
            ),
            draftOf(validFields),
        )
    }

    @Test
    fun `a blank description is required`() {
        assertEquals(
            LogFieldError.REQUIRED,
            errorsOf(validFields.copy(description = "   ")).description,
        )
    }

    @Test
    fun `a future date is rejected`() {
        assertEquals(
            LogFieldError.DATE_IN_FUTURE,
            errorsOf(validFields.copy(date = today.plusDays(1))).date,
        )
    }

    @Test
    fun `today and an earlier date are both accepted`() {
        assertNull(errorsOf(validFields.copy(description = "")).date)
        assertEquals(
            LocalDate.of(2026, 1, 2),
            draftOf(validFields.copy(date = LocalDate.of(2026, 1, 2))).date,
        )
    }

    @Test
    fun `a missing date is required`() {
        assertEquals(LogFieldError.REQUIRED, errorsOf(validFields.copy(date = null)).date)
    }

    @Test
    fun `a missing odometer is required`() {
        assertEquals(LogFieldError.REQUIRED, errorsOf(validFields.copy(odometer = "  ")).odometer)
    }

    @Test
    fun `a negative odometer is rejected`() {
        assertEquals(
            LogFieldError.NOT_A_NON_NEGATIVE_NUMBER,
            errorsOf(validFields.copy(odometer = "-1")).odometer,
        )
    }

    @Test
    fun `a non-numeric odometer is rejected`() {
        assertEquals(
            LogFieldError.NOT_A_NON_NEGATIVE_NUMBER,
            errorsOf(validFields.copy(odometer = "abc")).odometer,
        )
    }

    @Test
    fun `a zero odometer is accepted`() {
        assertEquals(0, draftOf(validFields.copy(odometer = "0")).odometer)
    }

    @Test
    fun `an empty cost produces a null`() {
        assertNull(draftOf(validFields.copy(cost = "   ")).cost)
    }

    @Test
    fun `a cost is stored in minor units`() {
        assertEquals(4500, draftOf(validFields.copy(cost = "45")).cost)
        assertEquals(4550, draftOf(validFields.copy(cost = "45.5")).cost)
        assertEquals(4550, draftOf(validFields.copy(cost = "45.50")).cost)
        assertEquals(0, draftOf(validFields.copy(cost = "0")).cost)
    }

    @Test
    fun `a cost with more than two decimal places is rejected rather than rounded`() {
        assertEquals(
            LogFieldError.NOT_A_VALID_AMOUNT,
            errorsOf(validFields.copy(cost = "45.555")).cost,
        )
    }

    @Test
    fun `a negative or unparseable cost is rejected`() {
        assertEquals(LogFieldError.NOT_A_VALID_AMOUNT, errorsOf(validFields.copy(cost = "-1")).cost)
        assertEquals(LogFieldError.NOT_A_VALID_AMOUNT, errorsOf(validFields.copy(cost = "abc")).cost)
    }

    @Test
    fun `surrounding whitespace is trimmed from the text and the numbers`() {
        val draft = draftOf(
            validFields.copy(
                description = "  Oil and filter  ",
                odometer = " 48000 ",
                cost = " 45.50 ",
                notes = "  shop said belts look fine  ",
            ),
        )

        assertEquals("Oil and filter", draft.description)
        assertEquals(48000, draft.odometer)
        assertEquals(4550, draft.cost)
        assertEquals("shop said belts look fine", draft.notes)
    }

    @Test
    fun `blank notes produce a null`() {
        assertNull(draftOf(validFields.copy(notes = "   ")).notes)
    }

    @Test
    fun `a null item id produces an unlinked draft`() {
        val validation =
            ServiceLogFormValidator.validate(validFields, "v-1", null, today)

        assertNull((validation as ServiceLogFormValidation.Valid).draft.maintenanceItemId)
    }
}
