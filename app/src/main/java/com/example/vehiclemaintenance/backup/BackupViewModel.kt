package com.example.vehiclemaintenance.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.StoreResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface BackupMessage {
    data object ExportSucceeded : BackupMessage
    data object ExportFailed : BackupMessage
    data object StoreUnavailable : BackupMessage
    data object PickerUnavailable : BackupMessage
    data object ImportSucceeded : BackupMessage
    data object ImportFailed : BackupMessage
    data object FileUnreadable : BackupMessage
    data object FileTooLarge : BackupMessage
    data object FileInvalid : BackupMessage
    data class FileFromAnotherVersion(val version: Int) : BackupMessage
}

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: BackupMessage? = null,
    /** A file that parsed cleanly and is waiting for the user to confirm the replacement. */
    val pendingImport: MaintenanceStore? = null,
)

class BackupViewModel(
    private val repository: BackupRepository,
    private val files: BackupFiles,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun suggestedFileName(): String = backupFileName(LocalDate.now())

    fun export(target: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val message = when (val snapshot = repository.exportSnapshot()) {
                is StoreResult.Failure -> BackupMessage.StoreUnavailable
                is StoreResult.Success -> if (files.write(target, snapshot.value)) {
                    BackupMessage.ExportSucceeded
                } else {
                    BackupMessage.ExportFailed
                }
            }
            _uiState.update { it.copy(isBusy = false, message = message) }
        }
    }

    /** Reads and validates the picked file. Nothing is written until the user confirms. */
    fun prepareImport(source: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val read = files.read(source)) {
                BackupRead.Unreadable -> finish(BackupMessage.FileUnreadable)
                BackupRead.TooLarge -> finish(BackupMessage.FileTooLarge)
                is BackupRead.Success -> {
                    when (val parse = withContext(parseDispatcher) { parseBackup(read.json) }) {
                        BackupParse.Invalid -> finish(BackupMessage.FileInvalid)
                        is BackupParse.UnsupportedVersion ->
                            finish(BackupMessage.FileFromAnotherVersion(parse.version))

                        is BackupParse.Valid -> _uiState.update {
                            it.copy(isBusy = false, pendingImport = parse.store)
                        }
                    }
                }
            }
        }
    }

    fun confirmImport() {
        val store = _uiState.value.pendingImport ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, pendingImport = null) }
            val result = repository.applyBackup(store)
            finish(
                if (result is StoreResult.Success) {
                    BackupMessage.ImportSucceeded
                } else {
                    BackupMessage.ImportFailed
                },
            )
        }
    }

    fun cancelImport() {
        _uiState.update { it.copy(pendingImport = null) }
    }

    private fun finish(message: BackupMessage) {
        _uiState.update { it.copy(isBusy = false, message = message) }
    }

    fun reportPickerUnavailable() {
        _uiState.update { it.copy(message = BackupMessage.PickerUnavailable) }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as VehicleMaintenanceApplication
                BackupViewModel(
                    application.container.backupRepository,
                    BackupFiles(application.contentResolver),
                )
            }
        }
    }
}
