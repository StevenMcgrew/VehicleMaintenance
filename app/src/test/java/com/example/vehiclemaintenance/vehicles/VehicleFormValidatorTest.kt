package com.example.vehiclemaintenance.vehicles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleFormValidatorTest {

    private val currentYear = 2026

    private val validFields = VehicleFormFields(
        nickname = "Daily",
        year = "2014",
        make = "Toyota",
        model = "Tacoma",
        engine = "4.0L V6",
    )

    private fun validate(fields: VehicleFormFields) =
        VehicleFormValidator.validate(fields, currentYear)

    private fun errorsOf(fields: VehicleFormFields) =
        (validate(fields) as VehicleFormValidation.Invalid).errors

    private fun draftOf(fields: VehicleFormFields) =
        (validate(fields) as VehicleFormValidation.Valid).draft

    @Test
    fun `a valid form produces a draft`() {
        assertEquals(
            VehicleDraft("Daily", 2014, "Toyota", "Tacoma", "4.0L V6"),
            draftOf(validFields),
        )
    }

    @Test
    fun `a blank make is required`() {
        assertEquals(VehicleFieldError.REQUIRED, errorsOf(validFields.copy(make = "   ")).make)
    }

    @Test
    fun `a blank model is required`() {
        assertEquals(VehicleFieldError.REQUIRED, errorsOf(validFields.copy(model = "")).model)
    }

    @Test
    fun `a blank engine is required`() {
        assertEquals(VehicleFieldError.REQUIRED, errorsOf(validFields.copy(engine = " ")).engine)
    }

    @Test
    fun `a blank year is required`() {
        assertEquals(VehicleFieldError.REQUIRED, errorsOf(validFields.copy(year = "")).year)
    }

    @Test
    fun `a non numeric year is rejected`() {
        assertEquals(
            VehicleFieldError.YEAR_NOT_A_NUMBER,
            errorsOf(validFields.copy(year = "20l4")).year,
        )
    }

    @Test
    fun `a year below the floor or above next year is out of range`() {
        assertEquals(
            VehicleFieldError.YEAR_OUT_OF_RANGE,
            errorsOf(validFields.copy(year = "1899")).year,
        )
        assertEquals(
            VehicleFieldError.YEAR_OUT_OF_RANGE,
            errorsOf(validFields.copy(year = "${currentYear + 2}")).year,
        )
    }

    @Test
    fun `the year bounds themselves are accepted`() {
        assertEquals(VehicleFormValidator.MIN_YEAR, draftOf(validFields.copy(year = "1900")).year)
        assertEquals(currentYear + 1, draftOf(validFields.copy(year = "${currentYear + 1}")).year)
    }

    @Test
    fun `every invalid field reports its own error at once`() {
        val errors = errorsOf(VehicleFormFields())

        assertEquals(VehicleFieldError.REQUIRED, errors.year)
        assertEquals(VehicleFieldError.REQUIRED, errors.make)
        assertEquals(VehicleFieldError.REQUIRED, errors.model)
        assertEquals(VehicleFieldError.REQUIRED, errors.engine)
    }

    @Test
    fun `surrounding whitespace is trimmed from every text field`() {
        val draft = draftOf(
            VehicleFormFields(
                nickname = "  Daily  ",
                year = " 2014 ",
                make = " Toyota ",
                model = "\tTacoma\t",
                engine = "  4.0L V6  ",
            ),
        )

        assertEquals(VehicleDraft("Daily", 2014, "Toyota", "Tacoma", "4.0L V6"), draft)
    }

    @Test
    fun `a blank nickname becomes absent`() {
        assertNull(draftOf(validFields.copy(nickname = "   ")).nickname)
        assertNull(draftOf(validFields.copy(nickname = "")).nickname)
    }
}
