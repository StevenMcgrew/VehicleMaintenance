package com.example.vehiclemaintenance.servicelog

import java.math.BigDecimal
import java.time.LocalDate

data class ServiceLogFormFields(
    val description: String = "",
    val date: LocalDate? = null,
    val odometer: String = "",
    val cost: String = "",
    val notes: String = "",
)

enum class LogFieldError {
    REQUIRED,
    NOT_A_NON_NEGATIVE_NUMBER,
    NOT_A_VALID_AMOUNT,
    DATE_IN_FUTURE,
}

data class ServiceLogFormErrors(
    val description: LogFieldError? = null,
    val date: LogFieldError? = null,
    val odometer: LogFieldError? = null,
    val cost: LogFieldError? = null,
) {
    val hasAny: Boolean
        get() = listOf(description, date, odometer, cost).any { it != null }
}

sealed interface ServiceLogFormValidation {
    data class Valid(val draft: ServiceLogDraft) : ServiceLogFormValidation
    data class Invalid(val errors: ServiceLogFormErrors) : ServiceLogFormValidation
}

object ServiceLogFormValidator {

    fun validate(
        fields: ServiceLogFormFields,
        vehicleId: String,
        itemId: String?,
        today: LocalDate,
    ): ServiceLogFormValidation {
        val description = fields.description.trim()
        val odometer = fields.odometer.trim()
        val cost = fields.cost.trim()
        val notes = fields.notes.trim()

        val errors = ServiceLogFormErrors(
            description = LogFieldError.REQUIRED.takeIf { description.isEmpty() },
            date = when {
                fields.date == null -> LogFieldError.REQUIRED
                fields.date.isAfter(today) -> LogFieldError.DATE_IN_FUTURE
                else -> null
            },
            odometer = when {
                odometer.isEmpty() -> LogFieldError.REQUIRED
                (odometer.toIntOrNull() ?: -1) < 0 -> LogFieldError.NOT_A_NON_NEGATIVE_NUMBER
                else -> null
            },
            cost = LogFieldError.NOT_A_VALID_AMOUNT
                .takeIf { cost.isNotEmpty() && cost.toMinorUnits() == null },
        )

        if (errors.hasAny) {
            return ServiceLogFormValidation.Invalid(errors)
        }

        return ServiceLogFormValidation.Valid(
            ServiceLogDraft(
                vehicleId = vehicleId,
                maintenanceItemId = itemId,
                description = description,
                date = requireNotNull(fields.date),
                odometer = odometer.toInt(),
                cost = if (cost.isEmpty()) null else cost.toMinorUnits(),
                notes = notes.ifEmpty { null },
            ),
        )
    }

    /**
     * Money is parsed as a [BigDecimal] and stored in minor units, never through a floating point
     * type. More than two decimal places is rejected rather than rounded, so no entered amount is
     * silently altered.
     */
    private fun String.toMinorUnits(): Int? {
        val amount = toBigDecimalOrNull() ?: return null
        if (amount.signum() < 0 || amount.scale() > 2) return null
        return runCatching { amount.movePointRight(2).intValueExact() }.getOrNull()
    }
}
