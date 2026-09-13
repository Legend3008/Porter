package com.porter.data.repository

import com.porter.domain.model.NotificationType
import com.porter.domain.model.PorterNotification
import com.porter.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryMockImpl @Inject constructor() : NotificationRepository {

    private val notifications = MutableStateFlow<List<PorterNotification>>(
        listOf(
            PorterNotification(
                id = "notif-1",
                type = NotificationType.DRIVER_ASSIGNED,
                title = "Driver Assigned",
                body = "Driver Rajesh Kumar (MH-04-AB-1234) has been assigned to shipment BK-2024-001.",
                bookingId = "BK-2024-001",
                tripId = "trip-001",
                isRead = false,
                receivedAt = Instant.parse("2024-06-01T09:30:00Z"),
                deepLink = "porter://tracking/trip-001"
            ),
            PorterNotification(
                id = "notif-2",
                type = NotificationType.IN_TRANSIT,
                title = "Container In Transit",
                body = "Shipment BK-2024-001 has departed JNPT Port and is en route to Pune Logistics Park.",
                bookingId = "BK-2024-001",
                tripId = "trip-001",
                isRead = false,
                receivedAt = Instant.parse("2024-06-01T10:00:00Z"),
                deepLink = "porter://tracking/trip-001"
            ),
            PorterNotification(
                id = "notif-3",
                type = NotificationType.PAYMENT_SUCCESS,
                title = "Payment Confirmed",
                body = "Payment of ₹45,430 for shipment BK-2024-001 was successful.",
                bookingId = "BK-2024-001",
                tripId = null,
                isRead = true,
                receivedAt = Instant.parse("2024-06-01T08:00:00Z"),
                deepLink = "porter://invoices"
            )
        )
    )

    override fun getNotifications(): Flow<List<PorterNotification>> {
        return notifications.asStateFlow()
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        notifications.value = notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        return Result.success(Unit)
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        notifications.value = notifications.value.map { it.copy(isRead = true) }
        return Result.success(Unit)
    }
}
