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

data class VehicleListUiState(
    val isLoading: Boolean = true,
    val vehicles: List<Vehicle> = emptyList(),
    val loadFailed: Boolean = false,
    val deleteFailed: Boolean = false,
)

class VehicleListViewModel(private val repository: VehicleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleListUiState())
    val uiState: StateFlow<VehicleListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.vehicles.collect { vehicles ->
                _uiState.update { it.copy(vehicles = vehicles) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val result = repository.load()
            _uiState.update {
                it.copy(isLoading = false, loadFailed = result is StoreResult.Failure)
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            val result = repository.delete(vehicleId)
            _uiState.update { it.copy(deleteFailed = result is StoreResult.Failure) }
        }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteFailed = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                VehicleListViewModel(application.container.vehicleRepository)
            }
        }
    }
}
