package com.example.vehiclemaintenance

import android.app.Application
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
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import java.io.File

const val STORE_FILE_NAME = "vehicle-maintenance.json"

class AppContainer(storeFile: File) {
    private val storeHolder = MaintenanceStoreHolder(JsonFileStore(storeFile))

    val vehicleRepository: VehicleRepository = JsonVehicleRepository(storeHolder)

    val maintenanceItemRepository: MaintenanceItemRepository =
        JsonMaintenanceItemRepository(storeHolder)

    val serviceLogRepository: ServiceLogRepository = JsonServiceLogRepository(storeHolder)

    val backupRepository: BackupRepository = JsonBackupRepository(storeHolder)
}

class VehicleMaintenanceApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(File(filesDir, STORE_FILE_NAME))
        ReminderNotifier(this).createChannel()
        scheduleDailyDueCheck(this)
    }
}
