package android.ai.pushnotificationintgration

/**
 * Indicates the app state when the FCM push notification was received / processed.
 */
enum class AppReceiveState {
    FOREGROUND,
    BACKGROUND,
    KILLED
}

/**
 * Data class representing an FCM Notification message payload and metadata.
 */
data class NotificationModel(
    val title: String? = null,
    val body: String? = null,
    val dataMap: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val receiveState: AppReceiveState = AppReceiveState.FOREGROUND
)
