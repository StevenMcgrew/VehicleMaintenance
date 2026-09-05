package com.example.vehiclemaintenance.vehicles

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

data class VehicleFormUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val fields: VehicleFormFields = VehicleFormFields(),
    val errors: VehicleFormErrors = VehicleFormErrors(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val saveFailed: Boolean = false,
    val vehicleNotFound: Boolean = false,
    val minYear: Int = VehicleFormValidator.MIN_YEAR,
    val maxYear: Int = VehicleFormValidator.MIN_YEAR,
)

class VehicleFormViewModel(
    private val repository: VehicleRepository,
    private val vehicleId: String?,
    private val currentYear: Int = LocalDate.now().year,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VehicleFormUiState(isEditing = vehicleId != null, maxYear = currentYear + 1),
    )
    val uiState: StateFlow<VehicleFormUiState> = _uiState.asStateFlow()

    private var showErrors = false

    init {
        viewModelScope.launch {
            if (vehicleId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            // The form can be recreated after process death with the repository not yet read.
            if (repository.vehicles.value.isEmpty()) {
                repository.load()
            }
            val existing = repository.vehicles.value.firstOrNull { it.id == vehicleId }
            _uiState.update {
                if (existing == null) {
                    it.copy(isLoading = false, vehicleNotFound = true)
                } else {
                    it.copy(isLoading = false, fields = existing.toFormFields())
                }
            }
        }
    }

    fun onYearChange(value: String) = updateFields { it.copy(year = value) }

    fun onMakeChange(value: String) = updateFields { it.copy(make = value) }

    fun onModelChange(value: String) = updateFields { it.copy(model = value) }

    fun onEngineChange(value: String) = updateFields { it.copy(engine = value) }

    fun save() {
        val fields = _uiState.value.fields
        when (val validation = VehicleFormValidator.validate(fields, currentYear)) {
            is VehicleFormValidation.Invalid -> {
                showErrors = true
                _uiState.update { it.copy(errors = validation.errors) }
            }

            is VehicleFormValidation.Valid -> {
                showErrors = false
                _uiState.update { it.copy(errors = VehicleFormErrors(), isSaving = true, saveFailed = false) }
                viewModelScope.launch {
                    val result = if (vehicleId == null) {
                        repository.add(validation.draft)
                    } else {
                        repository.update(validation.draft.toVehicle(vehicleId))
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

    fun dismissSaveError() {
        _uiState.update { it.copy(saveFailed = false) }
    }

    private fun updateFields(transform: (VehicleFormFields) -> VehicleFormFields) {
        _uiState.update { state ->
            val fields = transform(state.fields)
            val errors = if (showErrors) {
                when (val validation = VehicleFormValidator.validate(fields, currentYear)) {
                    is VehicleFormValidation.Invalid -> validation.errors
                    is VehicleFormValidation.Valid -> VehicleFormErrors()
                }
            } else {
                state.errors
            }
            state.copy(fields = fields, errors = errors)
        }
    }

    companion object {
        fun factory(vehicleId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                VehicleFormViewModel(application.container.vehicleRepository, vehicleId)
            }
        }
    }
}

private fun Vehicle.toFormFields() = VehicleFormFields(
    year = year.toString(),
    make = make,
    model = model,
    engine = engine,
)

private fun VehicleDraft.toVehicle(id: String) = Vehicle(
    id = id,
    year = year,
    make = make,
    model = model,
    engine = engine,
)
