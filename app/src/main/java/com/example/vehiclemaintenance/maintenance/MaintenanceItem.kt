package com.example.vehiclemaintenance.maintenance

import com.example.vehiclemaintenance.data.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class MaintenanceItem(
    val id: String,
    val vehicleId: String,
    val name: String,
    val mileageInterval: Int? = null,
    val recurrence: Interval? = null,
    val reminder: Interval,
    @Serializable(with = LocalDateSerializer::class)
    val lastDoneDate: LocalDate? = null,
    val lastDoneMileage: Int? = null,
    // Feature 7 owns the notification cadence; this feature only round trips the field.
    val lastNotifiedAt: String? = null,
)
