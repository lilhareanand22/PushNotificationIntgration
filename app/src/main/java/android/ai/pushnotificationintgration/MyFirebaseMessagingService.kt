package android.ai.pushnotificationintgration

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Token: $token")
        NotificationState.updateToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "FCM Push Notification"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New notification received"

        val dataMap = remoteMessage.data

        Log.d(TAG, "Notification Title: $title, Body: $body, Data: $dataMap")

        val notificationModel = NotificationModel(
            title = title,
            body = body,
            dataMap = dataMap,
            timestamp = System.currentTimeMillis(),
            receiveState = AppReceiveState.FOREGROUND
        )

        // Update global state for active foreground Compose UI
        NotificationState.updateNotification(notificationModel)

        // Display local heads-up notification in system tray
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            dataMap = dataMap
        )
    }

    companion object {
        private const val TAG = "FCM_Service"
    }
}
