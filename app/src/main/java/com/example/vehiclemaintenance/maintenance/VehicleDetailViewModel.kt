package com.example.vehiclemaintenance.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.servicelog.ServiceLogRepository
import com.example.vehiclemaintenance.vehicles.Vehicle
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MaintenanceItemRow(
    val item: MaintenanceItem,
    val status: MaintenanceItemStatus,
)

data class VehicleDetailUiState(
    val isLoading: Boolean = true,
    val vehicle: Vehicle? = null,
    val rows: List<MaintenanceItemRow> = emptyList(),
    val loadFailed: Boolean = false,
    val vehicleNotFound: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
    /** Names of the items a newly captured odometer reading just pushed overdue. */
    val newlyOverdueByMileage: List<String> = emptyList(),
)

class VehicleDetailViewModel(
    private val vehicles: VehicleRepository,
    private val items: MaintenanceItemRepository,
    private val serviceLog: ServiceLogRepository,
    private val vehicleId: String,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailUiState())
    val uiState: StateFlow<VehicleDetailUiState> = _uiState.asStateFlow()

    private var latestItems: List<MaintenanceItem> = emptyList()
    private var odometer: Int? = null
    private var seenLog = false
    private var overdueByMileage: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            vehicles.vehicles.collect { all ->
                _uiState.update { it.copy(vehicle = all.firstOrNull { v -> v.id == vehicleId }) }
            }
        }
        viewModelScope.launch {
            items.itemsFor(vehicleId).collect { list ->
                latestItems = list
                recompute(odometer, newReading = false)
            }
        }
        viewModelScope.launch {
            serviceLog.entriesFor(vehicleId).collect { entries ->
                val reading = currentOdometer(entries)
                // The first emission is the baseline this screen opened with, not a new reading.
                val isNew = seenLog && reading != null && (odometer == null || reading > odometer!!)
                seenLog = true
                recompute(reading, newReading = isNew)
            }
        }
        refresh()
    }

    /**
     * Only a rising odometer can raise the callout, so editing an item's mileage interval never
     * claims a reading pushed it overdue.
     */
    private fun recompute(reading: Int?, newReading: Boolean) {
        odometer = reading
        val now = today()
        val rows = latestItems.map { MaintenanceItemRow(it, statusOf(it, reading, now)) }
        val overdue = rows.filter { it.status.isOverdueByMileage }.map { it.item.id }.toSet()
        val newlyOverdue = if (newReading) {
            rows.filter { it.status.isOverdueByMileage && it.item.id !in overdueByMileage }
                .map { it.item.name }
        } else {
            emptyList()
        }
        overdueByMileage = overdue
        _uiState.update { state ->
            state.copy(
                rows = rows,
                newlyOverdueByMileage = newlyOverdue.ifEmpty { state.newlyOverdueByMileage },
            )
        }
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
        _uiState.update { it.copy(isDeleting = true, deleteFailed = false) }
        viewModelScope.launch {
            val result = items.delete(itemId)
            _uiState.update {
                it.copy(isDeleting = false, deleteFailed = result is StoreResult.Failure)
            }
        }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteFailed = false) }
    }

    fun dismissNewlyOverdue() {
        _uiState.update { it.copy(newlyOverdueByMileage = emptyList()) }
    }

    companion object {
        fun factory(vehicleId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                VehicleDetailViewModel(
                    application.container.vehicleRepository,
                    application.container.maintenanceItemRepository,
                    application.container.serviceLogRepository,
                    vehicleId,
                )
            }
        }
    }
}
