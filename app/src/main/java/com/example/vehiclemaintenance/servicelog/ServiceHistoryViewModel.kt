package com.example.vehiclemaintenance.servicelog

import android.net.Uri
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
import java.io.File
import java.time.LocalDate

data class ServiceHistoryUiState(
    val isLoading: Boolean = true,
    val vehicle: Vehicle? = null,
    val history: ServiceHistory = ServiceHistory(null, null, emptyList()),
    val loadFailed: Boolean = false,
    val vehicleNotFound: Boolean = false,
    val isWritingPdf: Boolean = false,
    val pdfFailed: Boolean = false,
    /** A PDF ready for the screen to print or share; cleared once it has been handed over. */
    val readyPdf: ReadyPdf? = null,
)

enum class PdfAction { PRINT, SHARE }

data class ReadyPdf(val action: PdfAction, val file: File)

class ServiceHistoryViewModel(
    private val vehicles: VehicleRepository,
    serviceLog: ServiceLogRepository,
    private val pdfFiles: HistoryPdfFiles,
    private val vehicleId: String,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceHistoryUiState())
    val uiState: StateFlow<ServiceHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vehicles.vehicles.collect { all ->
                _uiState.update { it.copy(vehicle = all.firstOrNull { v -> v.id == vehicleId }) }
            }
        }
        viewModelScope.launch {
            serviceLog.entriesFor(vehicleId).collect { entries ->
                _uiState.update { it.copy(history = serviceHistoryOf(entries)) }
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

    /** The name offered to the file picker, or null before the vehicle has loaded. */
    fun pdfFileName(): String? = _uiState.value.vehicle?.let { historyPdfFileName(it, today()) }

    fun savePdf(uri: Uri) {
        val state = _uiState.value
        val vehicle = state.vehicle ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWritingPdf = true) }
            val saved = pdfFiles.writeTo(uri, vehicle, state.history, today())
            _uiState.update { it.copy(isWritingPdf = false, pdfFailed = !saved) }
        }
    }

    fun preparePdf(action: PdfAction) {
        val state = _uiState.value
        val vehicle = state.vehicle ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWritingPdf = true) }
            val file = pdfFiles.writeForSharing(vehicle, state.history, today())
            _uiState.update {
                it.copy(
                    isWritingPdf = false,
                    pdfFailed = file == null,
                    readyPdf = file?.let { written -> ReadyPdf(action, written) },
                )
            }
        }
    }

    fun onPdfHandled() {
        _uiState.update { it.copy(readyPdf = null) }
    }

    fun dismissPdfError() {
        _uiState.update { it.copy(pdfFailed = false) }
    }

    companion object {
        fun factory(vehicleId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                ServiceHistoryViewModel(
                    application.container.vehicleRepository,
                    application.container.serviceLogRepository,
                    HistoryPdfFiles(application),
                    vehicleId,
                )
            }
        }
    }
}
