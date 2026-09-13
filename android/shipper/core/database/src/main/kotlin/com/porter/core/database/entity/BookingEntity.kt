package com.porter.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val draftId: String?,
    val status: String,
    val containerType: String,
    val pickupCity: String,
    val pickupAddress: String,
    val deliveryCity: String,
    val deliveryAddress: String,
    val totalFarePaise: Long,
    val cargoWeightKg: Double,
    val cargoDescription: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
