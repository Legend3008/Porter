package com.porter.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PorterNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_SHIPMENTS = "channel_shipments"
        const val CHANNEL_PAYMENTS = "channel_payments"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val shipmentChannel = NotificationChannel(
                CHANNEL_SHIPMENTS,
                "Shipment Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live tracking and status updates for container shipments"
            }

            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENTS,
                "Payment & Invoices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Payment confirmations and invoice receipts"
            }

            notificationManager.createNotificationChannel(shipmentChannel)
            notificationManager.createNotificationChannel(paymentChannel)
        }
    }

    fun showShipmentNotification(id: Int, title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_SHIPMENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
