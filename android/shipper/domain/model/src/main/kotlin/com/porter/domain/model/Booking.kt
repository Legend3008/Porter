package com.porter.domain.model

import kotlinx.datetime.Instant

// ── Container Types ───────────────────────────────────────────────────────────
enum class ContainerType(val displayName: String, val code: String) {
    DRY_20FT("20ft Dry", "20DRY"),
    DRY_40FT("40ft Dry", "40DRY"),
    HIGH_CUBE_40FT("40ft High Cube", "40HC"),
    REEFER_20FT("20ft Reefer", "20RF"),
    REEFER_40FT("40ft Reefer", "40RF"),
    FLAT_RACK_20FT("20ft Flat Rack", "20FR");

    val label: String get() = displayName
}

// ── Booking Status ────────────────────────────────────────────────────────────
enum class BookingStatus {
    DRAFT,
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    CONFIRMED,
    ASSIGNED,
    IN_TRANSIT,
    AT_PORT,
    LOADED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    PAYMENT_FAILED,
}

// ── Address ───────────────────────────────────────────────────────────────────
data class Address(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val state: String,
    val pincode: String,
    val country: String = "India",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
)

// ── Shipment Details ──────────────────────────────────────────────────────────
data class ShipmentDetails(
    val originPort: String,
    val destinationPort: String,
    val pickupAddress: Address,
    val deliveryAddress: Address,
    val containerType: ContainerType,
    val containerCount: Int,
    val totalWeightKg: Double,
    val commodity: String,
    val isHazardous: Boolean = false,
    val scheduledDate: Instant,
)

// ── Booking ───────────────────────────────────────────────────────────────────
data class Booking(
    val id: String,
    val referenceNumber: String,        // Human-readable, e.g. PRT-20240914-001
    val status: BookingStatus,
    val shipmentDetails: ShipmentDetails,
    val quoteId: String,
    val totalAmountPaise: Long,         // Always in paise — never Float
    val paidAmountPaise: Long,
    val assignedTripId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val estimatedPickupAt: Instant?,
    val estimatedDeliveryAt: Instant?,
    val actualDeliveredAt: Instant?,
    val cancellationReason: String?,
)

// ── Booking List Item (lightweight) ──────────────────────────────────────────
data class BookingListItem(
    val id: String,
    val referenceNumber: String,
    val status: BookingStatus,
    val originPort: String,
    val destinationPort: String,
    val containerType: ContainerType,
    val containerCount: Int,
    val totalAmountPaise: Long,
    val createdAt: Instant,
    val estimatedDeliveryAt: Instant?,
)
