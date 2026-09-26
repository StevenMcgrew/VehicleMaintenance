package com.example.vehiclemaintenance.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.servicelog.ServiceLogRepository
import com.example.vehiclemaintenance.vehicles.LowerMileageWarning
import com.example.vehiclemaintenance.vehicles.MileageInput
import com.example.vehiclemaintenance.vehicles.Vehicle
import com.example.vehiclemaintenance.vehicles.VehicleFieldError
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import com.example.vehiclemaintenance.vehicles.needsLowerMileageWarning
import com.example.vehiclemaintenance.vehicles.parseMileage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MaintenanceItemRow(
    val item: MaintenanceItem,
    val status: MaintenanceItemStatus,
)

/** The open Update mileage dialog, and the lower reading warning when it is waiting on an OK. */
data class MileageEditorState(
    val text: String = "",
    val error: VehicleFieldError? = null,
    val lowerMileageWarning: LowerMileageWarning? = null,
    val isSaving: Boolean = false,
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
    val lastRecordedMileage: Int? = null,
    val mileageEditor: MileageEditorState? = null,
    val mileageSaveFailed: Boolean = false,
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
    private var seenReading = false
    private var latestEntries: List<ServiceLogEntry> = emptyList()
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
            combine(vehicles.vehicles, serviceLog.entriesFor(vehicleId)) { all, entries ->
                latestEntries = entries
                currentOdometer(all.firstOrNull { it.id == vehicleId }?.recordedMileage, entries)
            }.collect { reading ->
                // The first emission is the baseline this screen opened with, not a new reading.
                val isNew =
                    seenReading && reading != null && (odometer == null || reading > odometer!!)
                seenReading = true
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
                lastRecordedMileage = reading,
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

    fun startMileageUpdate() {
        val current = _uiState.value.lastRecordedMileage
        _uiState.update {
            it.copy(mileageEditor = MileageEditorState(text = current?.toString().orEmpty()))
        }
    }

    fun onMileageTextChange(text: String) = updateEditor { it.copy(text = text, error = null) }

    fun submitMileage() {
        val editor = _uiState.value.mileageEditor ?: return
        val miles = when (val input = parseMileage(editor.text)) {
            MileageInput.Blank -> return updateEditor { it.copy(error = VehicleFieldError.REQUIRED) }
            MileageInput.Invalid -> return updateEditor {
                it.copy(error = VehicleFieldError.MILEAGE_NOT_A_NUMBER)
            }
            is MileageInput.Miles -> input.value
        }
        val highest = highestKnownMileage(_uiState.value.vehicle?.recordedMileage, latestEntries)
        if (highest != null && needsLowerMileageWarning(miles, highest)) {
            updateEditor { it.copy(lowerMileageWarning = LowerMileageWarning(miles, highest)) }
        } else {
            saveMileage(miles)
        }
    }

    fun confirmLowerMileage() {
        val warning = _uiState.value.mileageEditor?.lowerMileageWarning ?: return
        saveMileage(warning.entered)
    }

    fun dismissLowerMileageWarning() = updateEditor { it.copy(lowerMileageWarning = null) }

    fun cancelMileageUpdate() {
        _uiState.update { it.copy(mileageEditor = null) }
    }

    fun dismissMileageSaveError() {
        _uiState.update { it.copy(mileageSaveFailed = false) }
    }

    private fun saveMileage(miles: Int) {
        updateEditor { it.copy(lowerMileageWarning = null, isSaving = true) }
        viewModelScope.launch {
            val result = vehicles.updateMileage(vehicleId, miles)
            _uiState.update {
                it.copy(mileageEditor = null, mileageSaveFailed = result is StoreResult.Failure)
            }
        }
    }

    private fun updateEditor(transform: (MileageEditorState) -> MileageEditorState) {
        _uiState.update { state ->
            state.copy(mileageEditor = state.mileageEditor?.let(transform))
        }
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
