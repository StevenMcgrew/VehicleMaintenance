package com.example.vehiclemaintenance.servicelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.maintenance.MaintenanceItemRepository
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ServiceLogFormUiState(
    val isLoading: Boolean = true,
    val fields: ServiceLogFormFields = ServiceLogFormFields(),
    val errors: ServiceLogFormErrors = ServiceLogFormErrors(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val saveFailed: Boolean = false,
    val itemNotFound: Boolean = false,
    val vehicleNotFound: Boolean = false,
    val today: LocalDate = LocalDate.now(),
)

class ServiceLogFormViewModel(
    private val items: MaintenanceItemRepository,
    private val vehicles: VehicleRepository,
    private val serviceLog: ServiceLogRepository,
    private val vehicleId: String,
    /** Null logs an ad-hoc repair that was never tracked as a maintenance item. */
    private val itemId: String?,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceLogFormUiState(today = today()))
    val uiState: StateFlow<ServiceLogFormUiState> = _uiState.asStateFlow()

    private var showErrors = false

    init {
        viewModelScope.launch {
            _uiState.update { state ->
                if (itemId == null) startAdHocRepair(state) else startCompletionOf(itemId, state)
            }
        }
    }

    private fun startAdHocRepair(state: ServiceLogFormUiState): ServiceLogFormUiState =
        if (vehicles.vehicles.value.none { it.id == vehicleId }) {
            state.copy(isLoading = false, vehicleNotFound = true)
        } else {
            state.copy(isLoading = false, fields = state.fields.copy(date = today()))
        }

    private fun startCompletionOf(
        itemId: String,
        state: ServiceLogFormUiState,
    ): ServiceLogFormUiState {
        val item = items.itemsFor(vehicleId).value.firstOrNull { it.id == itemId }
        return if (item == null) {
            state.copy(isLoading = false, itemNotFound = true)
        } else {
            // The description starts as the item's name and stays editable, so the entry
            // still reads correctly if the item is later deleted and the link is cleared.
            state.copy(
                isLoading = false,
                fields = state.fields.copy(description = item.name, date = today()),
            )
        }
    }

    fun onDescriptionChange(value: String) = updateFields { it.copy(description = value) }

    fun onDateChange(date: LocalDate?) = updateFields { it.copy(date = date) }

    fun onOdometerChange(value: String) = updateFields { it.copy(odometer = value) }

    fun onCostChange(value: String) = updateFields { it.copy(cost = value) }

    fun onNotesChange(value: String) = updateFields { it.copy(notes = value) }

    fun save() {
        when (val validation = validate(_uiState.value.fields)) {
            is ServiceLogFormValidation.Invalid -> {
                showErrors = true
                _uiState.update { it.copy(errors = validation.errors) }
            }

            is ServiceLogFormValidation.Valid -> {
                showErrors = false
                _uiState.update {
                    it.copy(
                        errors = ServiceLogFormErrors(),
                        isSaving = true,
                        saveFailed = false,
                    )
                }
                viewModelScope.launch {
                    val result = serviceLog.add(validation.draft)
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

    fun dismissSaveError() {
        _uiState.update { it.copy(saveFailed = false) }
    }

    private fun validate(fields: ServiceLogFormFields) =
        ServiceLogFormValidator.validate(fields, vehicleId, itemId, today())

    private fun updateFields(transform: (ServiceLogFormFields) -> ServiceLogFormFields) {
        _uiState.update { state ->
            val fields = transform(state.fields)
            val errors = if (showErrors) {
                when (val validation = validate(fields)) {
                    is ServiceLogFormValidation.Invalid -> validation.errors
                    is ServiceLogFormValidation.Valid -> ServiceLogFormErrors()
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
                    ServiceLogFormViewModel(
                        application.container.maintenanceItemRepository,
                        application.container.vehicleRepository,
                        application.container.serviceLogRepository,
                        vehicleId,
                        itemId,
                    )
                }
            }
    }
}
