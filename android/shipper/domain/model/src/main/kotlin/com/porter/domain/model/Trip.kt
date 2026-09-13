package com.porter.domain.model

import kotlinx.datetime.Instant

// ── Trip ──────────────────────────────────────────────────────────────────────
enum class TripStatus {
    ASSIGNED, EN_ROUTE_PICKUP, AT_PICKUP, LOADED, EN_ROUTE_DELIVERY,
    AT_DELIVERY, DELIVERED, COMPLETED
}

data class Trip(
    val id: String,
    val bookingId: String,
    val status: TripStatus,
    val driver: DriverInfo,
    val vehicle: VehicleInfo,
    val currentLocation: GpsLocation?,
    val lastLocationAt: Instant?,
    val statusHistory: List<TripStatusEvent>,
    val estimatedArrivalAt: Instant?,
)

data class DriverInfo(
    val id: String,
    val name: String,
    val phone: String,
    val rating: Double,
    val photoUrl: String?,
)

data class VehicleInfo(
    val number: String,
    val make: String,
    val model: String,
)

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val heading: Float?,
    val speedKmh: Double?,
    val recordedAt: Instant,
)

// ── Status Timeline ───────────────────────────────────────────────────────────
data class TripStatusEvent(
    val status: TripStatus,
    val label: String,
    val description: String?,
    val occurredAt: Instant? = null,
    val location: String?,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
)
