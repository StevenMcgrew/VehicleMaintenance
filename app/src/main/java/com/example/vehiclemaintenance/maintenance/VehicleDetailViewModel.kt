package com.example.vehiclemaintenance.maintenance

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

data class VehicleDetailUiState(
    val isLoading: Boolean = true,
    val vehicle: Vehicle? = null,
    val items: List<MaintenanceItem> = emptyList(),
    val loadFailed: Boolean = false,
    val vehicleNotFound: Boolean = false,
    val deleteFailed: Boolean = false,
)

class VehicleDetailViewModel(
    private val vehicles: VehicleRepository,
    private val items: MaintenanceItemRepository,
    private val vehicleId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailUiState())
    val uiState: StateFlow<VehicleDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vehicles.vehicles.collect { all ->
                _uiState.update { it.copy(vehicle = all.firstOrNull { v -> v.id == vehicleId }) }
            }
        }
        viewModelScope.launch {
            items.itemsFor(vehicleId).collect { list ->
                _uiState.update { it.copy(items = list) }
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

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val result = items.delete(itemId)
            _uiState.update { it.copy(deleteFailed = result is StoreResult.Failure) }
        }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteFailed = false) }
    }

    companion object {
        fun factory(vehicleId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                VehicleDetailViewModel(
                    application.container.vehicleRepository,
                    application.container.maintenanceItemRepository,
                    vehicleId,
                )
            }
        }
    }
}
