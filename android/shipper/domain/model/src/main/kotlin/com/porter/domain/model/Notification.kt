package com.porter.domain.model

import kotlinx.datetime.Instant

enum class NotificationType {
    BOOKING_CONFIRMED,
    DRIVER_ASSIGNED,
    PICKUP_STARTED,
    AT_PICKUP,
    LOADED,
    IN_TRANSIT,
    AT_DELIVERY,
    DELIVERED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    INVOICE_READY,
    DOCUMENT_READY,
    GENERAL,
}

data class PorterNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val bookingId: String?,
    val tripId: String?,
    val isRead: Boolean,
    val receivedAt: Instant,
    val deepLink: String?,
)
