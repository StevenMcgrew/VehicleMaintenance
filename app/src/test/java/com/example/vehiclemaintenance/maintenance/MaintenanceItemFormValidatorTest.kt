package com.example.vehiclemaintenance.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MaintenanceItemFormValidatorTest {

    private val today = LocalDate.of(2026, 9, 5)

    private val validFields = MaintenanceItemFormFields(
        name = "Oil change",
        mileageInterval = "5000",
        recurrenceValue = "6",
        recurrenceUnit = IntervalUnit.MONTHS,
        reminderValue = "5",
        reminderUnit = IntervalUnit.MONTHS,
    )

    private fun validate(fields: MaintenanceItemFormFields) =
        MaintenanceItemFormValidator.validate(fields, "v-1", today)

    private fun errorsOf(fields: MaintenanceItemFormFields) =
        (validate(fields) as MaintenanceItemFormValidation.Invalid).errors

    private fun draftOf(fields: MaintenanceItemFormFields) =
        (validate(fields) as MaintenanceItemFormValidation.Valid).draft

    @Test
    fun `a valid form produces a draft carrying the vehicle id`() {
        assertEquals(
            MaintenanceItemDraft(
                vehicleId = "v-1",
                name = "Oil change",
                mileageInterval = 5000,
                recurrence = Interval(6, IntervalUnit.MONTHS),
                reminder = Interval(5, IntervalUnit.MONTHS),
            ),
            draftOf(validFields),
        )
    }

    @Test
    fun `a blank name is required`() {
        assertEquals(ItemFieldError.REQUIRED, errorsOf(validFields.copy(name = "   ")).name)
    }

    @Test
    fun `surrounding whitespace is trimmed from the name and the numbers`() {
        val draft = draftOf(
            validFields.copy(
                name = "  Oil change  ",
                mileageInterval = " 5000 ",
                recurrenceValue = " 6 ",
                reminderValue = " 5 ",
            ),
        )

        assertEquals("Oil change", draft.name)
        assertEquals(5000, draft.mileageInterval)
        assertEquals(Interval(6, IntervalUnit.MONTHS), draft.recurrence)
    }

    @Test
    fun `a missing reminder value is required`() {
        assertEquals(
            ItemFieldError.REQUIRED,
            errorsOf(validFields.copy(reminderValue = "")).reminderValue,
        )
    }

    @Test
    fun `a non positive or non numeric reminder value is rejected`() {
        assertEquals(
            ItemFieldError.NOT_A_POSITIVE_NUMBER,
            errorsOf(validFields.copy(reminderValue = "0")).reminderValue,
        )
        assertEquals(
            ItemFieldError.NOT_A_POSITIVE_NUMBER,
            errorsOf(validFields.copy(reminderValue = "-3")).reminderValue,
        )
        assertEquals(
            ItemFieldError.NOT_A_POSITIVE_NUMBER,
            errorsOf(validFields.copy(reminderValue = "five")).reminderValue,
        )
    }

    @Test
    fun `a missing reminder unit is required`() {
        assertEquals(
            ItemFieldError.UNIT_REQUIRED,
            errorsOf(validFields.copy(reminderUnit = null)).reminderUnit,
        )
    }

    @Test
    fun `an entirely empty recurrence is accepted as absent`() {
        val draft = draftOf(validFields.copy(recurrenceValue = "", recurrenceUnit = null))

        assertNull(draft.recurrence)
    }

    @Test
    fun `a recurrence value without a unit is rejected`() {
        val errors = errorsOf(validFields.copy(recurrenceUnit = null))

        assertEquals(ItemFieldError.UNIT_REQUIRED, errors.recurrenceUnit)
        assertNull(errors.recurrenceValue)
    }

    @Test
    fun `a recurrence unit without a value is rejected`() {
        val errors = errorsOf(validFields.copy(recurrenceValue = ""))

        assertEquals(ItemFieldError.VALUE_REQUIRED, errors.recurrenceValue)
        assertNull(errors.recurrenceUnit)
    }

    @Test
    fun `a non positive recurrence value is rejected`() {
        assertEquals(
            ItemFieldError.NOT_A_POSITIVE_NUMBER,
            errorsOf(validFields.copy(recurrenceValue = "0")).recurrenceValue,
        )
    }

    @Test
    fun `an empty mileage interval is absent and a non positive one is rejected`() {
        assertNull(draftOf(validFields.copy(mileageInterval = "")).mileageInterval)
        assertEquals(
            ItemFieldError.NOT_A_POSITIVE_NUMBER,
            errorsOf(validFields.copy(mileageInterval = "0")).mileageInterval,
        )
    }

    @Test
    fun `a future last done date is rejected and today is accepted`() {
        assertEquals(
            ItemFieldError.DATE_IN_FUTURE,
            errorsOf(validFields.copy(lastDoneDate = today.plusDays(1))).lastDoneDate,
        )
        assertEquals(today, draftOf(validFields.copy(lastDoneDate = today)).lastDoneDate)
    }

    @Test
    fun `an empty last done mileage is absent and a negative one is rejected`() {
        assertNull(draftOf(validFields.copy(lastDoneMileage = "")).lastDoneMileage)
        assertEquals(0, draftOf(validFields.copy(lastDoneMileage = "0")).lastDoneMileage)
        assertEquals(
            ItemFieldError.NOT_A_NON_NEGATIVE_NUMBER,
            errorsOf(validFields.copy(lastDoneMileage = "-1")).lastDoneMileage,
        )
    }

    @Test
    fun `last done date and mileage are independent`() {
        assertEquals(
            41000,
            draftOf(validFields.copy(lastDoneMileage = "41000")).lastDoneMileage,
        )
        assertEquals(
            today.minusDays(3),
            draftOf(validFields.copy(lastDoneDate = today.minusDays(3))).lastDoneDate,
        )
    }

    @Test
    fun `every invalid field reports its own error at once`() {
        val errors = errorsOf(MaintenanceItemFormFields(reminderUnit = null))

        assertEquals(ItemFieldError.REQUIRED, errors.name)
        assertEquals(ItemFieldError.REQUIRED, errors.reminderValue)
        assertEquals(ItemFieldError.UNIT_REQUIRED, errors.reminderUnit)
        assertNull(errors.recurrenceValue)
        assertNull(errors.mileageInterval)
    }
}
