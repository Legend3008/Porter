package com.porter.shipper.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.porter.core.notifications.PorterNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PorterFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: PorterNotificationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send refreshed FCM token to backend securely
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Shipment Alert"
        val body = message.notification?.body ?: message.data["body"] ?: "New update on your container shipment."
        val notifId = System.currentTimeMillis().toInt()
        notificationManager.showShipmentNotification(notifId, title, body)
    }
}
