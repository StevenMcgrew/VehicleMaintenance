package com.example.vehiclemaintenance.vehicles

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String,
    val year: Int,
    val make: String,
    val model: String,
    val engine: String,
    /**
     * The odometer the owner last entered by hand, or the last service logged above it. Null on
     * vehicles saved before mileage was tracked, which fall back to their service log.
     */
    val recordedMileage: Int? = null,
)
