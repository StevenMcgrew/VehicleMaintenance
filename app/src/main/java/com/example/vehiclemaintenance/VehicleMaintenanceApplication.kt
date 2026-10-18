package com.example.vehiclemaintenance

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.vehiclemaintenance.backup.BackupRepository
import com.example.vehiclemaintenance.backup.JsonBackupRepository
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.maintenance.JsonMaintenanceItemRepository
import com.example.vehiclemaintenance.maintenance.MaintenanceItemRepository
import com.example.vehiclemaintenance.reminders.ReminderNotifier
import com.example.vehiclemaintenance.reminders.scheduleDailyDueCheck
import com.example.vehiclemaintenance.servicelog.JsonServiceLogRepository
import com.example.vehiclemaintenance.servicelog.ServiceLogRepository
import com.example.vehiclemaintenance.settings.SettingsRepository
import com.example.vehiclemaintenance.settings.SharedPreferencesSettingsRepository
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

const val STORE_FILE_NAME = "vehicle-maintenance.json"

class AppContainer(storeFile: File, preferences: () -> SharedPreferences) {
    private val storeHolder = MaintenanceStoreHolder(JsonFileStore(storeFile))

    val vehicleRepository: VehicleRepository = JsonVehicleRepository(storeHolder)

    val maintenanceItemRepository: MaintenanceItemRepository =
        JsonMaintenanceItemRepository(storeHolder)

    val serviceLogRepository: ServiceLogRepository = JsonServiceLogRepository(storeHolder)

    val backupRepository: BackupRepository = JsonBackupRepository(storeHolder)

    val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(preferences)
}

class VehicleMaintenanceApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(File(filesDir, STORE_FILE_NAME)) {
            getSharedPreferences(
                SharedPreferencesSettingsRepository.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
        }
        // Started before the activity so the saved theme is usually in place for the first frame.
        applicationScope.launch { container.settingsRepository.load() }
        ReminderNotifier(this).createChannel()
        scheduleDailyDueCheck(this)
    }
}
