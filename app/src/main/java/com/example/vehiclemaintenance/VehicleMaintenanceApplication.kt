package com.example.vehiclemaintenance

import android.app.Application
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import java.io.File

const val STORE_FILE_NAME = "vehicle-maintenance.json"

class AppContainer(storeFile: File) {
    val vehicleRepository: VehicleRepository = JsonVehicleRepository(JsonFileStore(storeFile))
}

class VehicleMaintenanceApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(File(filesDir, STORE_FILE_NAME))
    }
}
