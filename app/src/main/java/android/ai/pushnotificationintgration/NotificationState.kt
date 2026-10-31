package android.ai.pushnotificationintgration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global singleton state repository to manage FCM Registration Token and
 * reactive updates for received FCM notifications across Services and Activities.
 */
object NotificationState {

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _latestNotification = MutableStateFlow<NotificationModel?>(null)
    val latestNotification: StateFlow<NotificationModel?> = _latestNotification.asStateFlow()

    fun updateToken(token: String) {
        _fcmToken.value = token
    }

    fun updateNotification(notification: NotificationModel) {
        _latestNotification.value = notification
    }
}
