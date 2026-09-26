package com.example.vehiclemaintenance.vehicles

data class VehicleFormFields(
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val engine: String = "",
    val mileage: String = "",
)

enum class VehicleFieldError {
    REQUIRED,
    YEAR_NOT_A_NUMBER,
    YEAR_OUT_OF_RANGE,
    MILEAGE_NOT_A_NUMBER,
}

data class VehicleFormErrors(
    val year: VehicleFieldError? = null,
    val make: VehicleFieldError? = null,
    val model: VehicleFieldError? = null,
    val engine: VehicleFieldError? = null,
    val mileage: VehicleFieldError? = null,
) {
    val hasAny: Boolean
        get() = listOf(year, make, model, engine, mileage).any { it != null }
}

sealed interface VehicleFormValidation {
    data class Valid(val draft: VehicleDraft) : VehicleFormValidation
    data class Invalid(val errors: VehicleFormErrors) : VehicleFormValidation
}

object VehicleFormValidator {

    const val MIN_YEAR = 1900

    fun validate(fields: VehicleFormFields, currentYear: Int): VehicleFormValidation {
        val make = fields.make.trim()
        val model = fields.model.trim()
        val engine = fields.engine.trim()
        val yearText = fields.year.trim()
        val year = yearText.toIntOrNull()
        val mileage = parseMileage(fields.mileage)

        val errors = VehicleFormErrors(
            year = when {
                yearText.isBlank() -> VehicleFieldError.REQUIRED
                year == null -> VehicleFieldError.YEAR_NOT_A_NUMBER
                year !in MIN_YEAR..(currentYear + 1) -> VehicleFieldError.YEAR_OUT_OF_RANGE
                else -> null
            },
            make = VehicleFieldError.REQUIRED.takeIf { make.isEmpty() },
            model = VehicleFieldError.REQUIRED.takeIf { model.isEmpty() },
            engine = VehicleFieldError.REQUIRED.takeIf { engine.isEmpty() },
            mileage = VehicleFieldError.MILEAGE_NOT_A_NUMBER
                .takeIf { mileage == MileageInput.Invalid },
        )

        return if (errors.hasAny || year == null) {
            VehicleFormValidation.Invalid(errors)
        } else {
            val recordedMileage = (mileage as? MileageInput.Miles)?.value
            VehicleFormValidation.Valid(VehicleDraft(year, make, model, engine, recordedMileage))
        }
    }
}
