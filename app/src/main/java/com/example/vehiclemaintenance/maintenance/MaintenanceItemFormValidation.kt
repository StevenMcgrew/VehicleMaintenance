package com.example.vehiclemaintenance.maintenance

import java.time.LocalDate

data class MaintenanceItemFormFields(
    val name: String = "",
    val mileageInterval: String = "",
    val recurrenceValue: String = "",
    val recurrenceUnit: IntervalUnit? = null,
    val reminderValue: String = "",
    val reminderUnit: IntervalUnit? = IntervalUnit.MONTHS,
    val lastDoneDate: LocalDate? = null,
    val lastDoneMileage: String = "",
)

enum class ItemFieldError {
    REQUIRED,
    NOT_A_POSITIVE_NUMBER,
    NOT_A_NON_NEGATIVE_NUMBER,
    VALUE_REQUIRED,
    UNIT_REQUIRED,
    DATE_IN_FUTURE,
}

data class MaintenanceItemFormErrors(
    val name: ItemFieldError? = null,
    val mileageInterval: ItemFieldError? = null,
    val recurrenceValue: ItemFieldError? = null,
    val recurrenceUnit: ItemFieldError? = null,
    val reminderValue: ItemFieldError? = null,
    val reminderUnit: ItemFieldError? = null,
    val lastDoneDate: ItemFieldError? = null,
    val lastDoneMileage: ItemFieldError? = null,
) {
    val hasAny: Boolean
        get() = listOf(
            name,
            mileageInterval,
            recurrenceValue,
            recurrenceUnit,
            reminderValue,
            reminderUnit,
            lastDoneDate,
            lastDoneMileage,
        ).any { it != null }
}

sealed interface MaintenanceItemFormValidation {
    data class Valid(val draft: MaintenanceItemDraft) : MaintenanceItemFormValidation
    data class Invalid(val errors: MaintenanceItemFormErrors) : MaintenanceItemFormValidation
}

object MaintenanceItemFormValidator {

    fun validate(
        fields: MaintenanceItemFormFields,
        vehicleId: String,
        today: LocalDate,
    ): MaintenanceItemFormValidation {
        val name = fields.name.trim()
        val mileage = fields.mileageInterval.trim()
        val recurrenceValue = fields.recurrenceValue.trim()
        val reminderValue = fields.reminderValue.trim()
        val lastDoneMileage = fields.lastDoneMileage.trim()

        // Recurrence is all or nothing: both parts empty means the user does not want one.
        val wantsRecurrence = recurrenceValue.isNotEmpty() || fields.recurrenceUnit != null

        val errors = MaintenanceItemFormErrors(
            name = ItemFieldError.REQUIRED.takeIf { name.isEmpty() },
            mileageInterval = positiveError(mileage, required = false),
            recurrenceValue = when {
                !wantsRecurrence -> null
                recurrenceValue.isEmpty() -> ItemFieldError.VALUE_REQUIRED
                else -> positiveError(recurrenceValue, required = true)
            },
            recurrenceUnit = ItemFieldError.UNIT_REQUIRED
                .takeIf { wantsRecurrence && fields.recurrenceUnit == null },
            reminderValue = positiveError(reminderValue, required = true),
            reminderUnit = ItemFieldError.UNIT_REQUIRED.takeIf { fields.reminderUnit == null },
            lastDoneDate = ItemFieldError.DATE_IN_FUTURE
                .takeIf { fields.lastDoneDate?.isAfter(today) == true },
            lastDoneMileage = nonNegativeError(lastDoneMileage),
        )

        if (errors.hasAny) {
            return MaintenanceItemFormValidation.Invalid(errors)
        }

        return MaintenanceItemFormValidation.Valid(
            MaintenanceItemDraft(
                vehicleId = vehicleId,
                name = name,
                mileageInterval = mileage.toIntOrNull(),
                recurrence = if (wantsRecurrence) {
                    Interval(recurrenceValue.toInt(), requireNotNull(fields.recurrenceUnit))
                } else {
                    null
                },
                reminder = Interval(reminderValue.toInt(), requireNotNull(fields.reminderUnit)),
                lastDoneDate = fields.lastDoneDate,
                lastDoneMileage = lastDoneMileage.toIntOrNull(),
            ),
        )
    }

    private fun positiveError(text: String, required: Boolean): ItemFieldError? = when {
        text.isEmpty() -> ItemFieldError.REQUIRED.takeIf { required }
        (text.toIntOrNull() ?: 0) < 1 -> ItemFieldError.NOT_A_POSITIVE_NUMBER
        else -> null
    }

    private fun nonNegativeError(text: String): ItemFieldError? = when {
        text.isEmpty() -> null
        (text.toIntOrNull() ?: -1) < 0 -> ItemFieldError.NOT_A_NON_NEGATIVE_NUMBER
        else -> null
    }
}
