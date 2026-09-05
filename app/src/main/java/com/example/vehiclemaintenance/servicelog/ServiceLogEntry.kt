package com.example.vehiclemaintenance.servicelog

import com.example.vehiclemaintenance.data.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * One completed service, either against a maintenance item or as a standalone repair.
 *
 * The description is stored on the entry rather than read back through [maintenanceItemId], so the
 * history still reads correctly after its item is deleted and the link is cleared.
 */
@Serializable
data class ServiceLogEntry(
    val id: String,
    val vehicleId: String,
    /** Null for an ad-hoc repair that was never tracked as a recurring item. */
    val maintenanceItemId: String? = null,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val odometer: Int,
    /** Minor units, so money is never rounded through a floating point type. */
    val cost: Int? = null,
    val notes: String? = null,
)
