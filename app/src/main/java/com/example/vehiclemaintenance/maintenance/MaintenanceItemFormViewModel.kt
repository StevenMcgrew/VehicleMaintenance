package com.example.vehiclemaintenance.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.StoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MaintenanceItemFormUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val fields: MaintenanceItemFormFields = MaintenanceItemFormFields(),
    val errors: MaintenanceItemFormErrors = MaintenanceItemFormErrors(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val saveFailed: Boolean = false,
    val isDeleting: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    val deleteFailed: Boolean = false,
    val itemNotFound: Boolean = false,
    val today: LocalDate = LocalDate.now(),
)

class MaintenanceItemFormViewModel(
    private val repository: MaintenanceItemRepository,
    private val vehicleId: String,
    private val itemId: String?,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MaintenanceItemFormUiState(isEditing = itemId != null, today = today()),
    )
    val uiState: StateFlow<MaintenanceItemFormUiState> = _uiState.asStateFlow()

    private var showErrors = false
    private var editing: MaintenanceItem? = null

    init {
        viewModelScope.launch {
            if (itemId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val existing = repository.itemsFor(vehicleId).value.firstOrNull { it.id == itemId }
            editing = existing
            _uiState.update {
                if (existing == null) {
                    it.copy(isLoading = false, itemNotFound = true)
                } else {
                    it.copy(isLoading = false, fields = existing.toFormFields())
                }
            }
        }
    }

    fun onNameChange(value: String) = updateFields { it.copy(name = value) }

    fun onMileageIntervalChange(value: String) = updateFields { it.copy(mileageInterval = value) }

    fun onRecurrenceValueChange(value: String) = updateFields { it.copy(recurrenceValue = value) }

    fun onRecurrenceUnitChange(unit: IntervalUnit?) = updateFields { it.copy(recurrenceUnit = unit) }

    fun onReminderValueChange(value: String) = updateFields { it.copy(reminderValue = value) }

    fun onReminderUnitChange(unit: IntervalUnit?) = updateFields { it.copy(reminderUnit = unit) }

    fun onLastDoneDateChange(date: LocalDate?) = updateFields { it.copy(lastDoneDate = date) }

    fun onLastDoneMileageChange(value: String) = updateFields { it.copy(lastDoneMileage = value) }

    fun save() {
        val fields = _uiState.value.fields
        when (val validation = validate(fields)) {
            is MaintenanceItemFormValidation.Invalid -> {
                showErrors = true
                _uiState.update { it.copy(errors = validation.errors) }
            }

            is MaintenanceItemFormValidation.Valid -> {
                showErrors = false
                _uiState.update {
                    it.copy(
                        errors = MaintenanceItemFormErrors(),
                        isSaving = true,
                        saveFailed = false,
                    )
                }
                viewModelScope.launch {
                    val result = when (val current = editing) {
                        null -> repository.add(validation.draft)
                        else -> repository.update(current.merge(validation.draft))
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            savedSuccessfully = result is StoreResult.Success,
                            saveFailed = result is StoreResult.Failure,
                        )
                    }
                }
            }
        }
    }

    fun delete() {
        val id = itemId ?: return
        _uiState.update { it.copy(isDeleting = true, deleteFailed = false) }
        viewModelScope.launch {
            val result = repository.delete(id)
            _uiState.update {
                it.copy(
                    isDeleting = false,
                    deletedSuccessfully = result is StoreResult.Success,
                    deleteFailed = result is StoreResult.Failure,
                )
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveFailed = false) }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteFailed = false) }
    }

    private fun validate(fields: MaintenanceItemFormFields) =
        MaintenanceItemFormValidator.validate(fields, vehicleId, today())

    private fun updateFields(
        transform: (MaintenanceItemFormFields) -> MaintenanceItemFormFields,
    ) {
        _uiState.update { state ->
            val fields = transform(state.fields)
            val errors = if (showErrors) {
                when (val validation = validate(fields)) {
                    is MaintenanceItemFormValidation.Invalid -> validation.errors
                    is MaintenanceItemFormValidation.Valid -> MaintenanceItemFormErrors()
                }
            } else {
                state.errors
            }
            state.copy(fields = fields, errors = errors)
        }
    }

    companion object {
        fun factory(vehicleId: String, itemId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                            as VehicleMaintenanceApplication
                    MaintenanceItemFormViewModel(
                        application.container.maintenanceItemRepository,
                        vehicleId,
                        itemId,
                    )
                }
            }
    }
}

private fun MaintenanceItem.toFormFields() = MaintenanceItemFormFields(
    name = name,
    mileageInterval = mileageInterval?.toString().orEmpty(),
    recurrenceValue = recurrence?.value?.toString().orEmpty(),
    recurrenceUnit = recurrence?.unit,
    reminderValue = reminder.value.toString(),
    reminderUnit = reminder.unit,
    lastDoneDate = lastDoneDate,
    lastDoneMileage = lastDoneMileage?.toString().orEmpty(),
)

/** An edit keeps the item's identity and the fields this form does not own. */
private fun MaintenanceItem.merge(draft: MaintenanceItemDraft) = copy(
    name = draft.name,
    mileageInterval = draft.mileageInterval,
    recurrence = draft.recurrence,
    reminder = draft.reminder,
    lastDoneDate = draft.lastDoneDate ?: lastDoneDate,
    lastDoneMileage = draft.lastDoneMileage,
)
