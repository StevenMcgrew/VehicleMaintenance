package com.example.vehiclemaintenance.servicelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.vehicles.Vehicle
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CostBreakdownUiState(
    val isLoading: Boolean = true,
    val vehicle: Vehicle? = null,
    val totals: VehicleCostTotals = VehicleCostTotals(0L, emptyList()),
    val loadFailed: Boolean = false,
    val vehicleNotFound: Boolean = false,
)

class CostBreakdownViewModel(
    private val vehicles: VehicleRepository,
    serviceLog: ServiceLogRepository,
    private val vehicleId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CostBreakdownUiState())
    val uiState: StateFlow<CostBreakdownUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vehicles.vehicles.collect { all ->
                _uiState.update { it.copy(vehicle = all.firstOrNull { v -> v.id == vehicleId }) }
            }
        }
        viewModelScope.launch {
            serviceLog.entriesFor(vehicleId).collect { entries ->
                _uiState.update { it.copy(totals = costTotalsOf(entries)) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val result = vehicles.load()
            _uiState.update {
                val failed = result is StoreResult.Failure
                it.copy(
                    isLoading = false,
                    loadFailed = failed,
                    vehicleNotFound = !failed && it.vehicle == null,
                )
            }
        }
    }

    companion object {
        fun factory(vehicleId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                CostBreakdownViewModel(
                    application.container.vehicleRepository,
                    application.container.serviceLogRepository,
                    vehicleId,
                )
            }
        }
    }
}
