package com.example.vehiclemaintenance.vehicles

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String,
    val nickname: String? = null,
    val year: Int,
    val make: String,
    val model: String,
    val engine: String,
)
