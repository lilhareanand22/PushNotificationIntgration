package android.ai.pushnotificationintgration

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    val fcmToken: StateFlow<String?> = NotificationState.fcmToken
    val latestNotification: StateFlow<NotificationModel?> = NotificationState.latestNotification

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    fun checkNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            _isPermissionGranted.value = (status == PackageManager.PERMISSION_GRANTED)
        } else {
            _isPermissionGranted.value = true
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _isPermissionGranted.value = isGranted
    }

    fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainViewModel", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("MainViewModel", "FCM Token: $token")
            NotificationState.updateToken(token)
        }
    }

    fun handleNotificationIntent(intent: Intent?, isKilledState: Boolean) {
        if (intent == null || intent.extras == null) return

        val extras = intent.extras ?: return

        // Extract title & body (check standard FCM keys or custom extra keys)
        val title = extras.getString("fcm_title")
            ?: extras.getString("gcm.notification.title")
            ?: extras.getString("title")

        val body = extras.getString("fcm_body")
            ?: extras.getString("gcm.notification.body")
            ?: extras.getString("body")

        val receivedStateStr = extras.getString("fcm_received_state")

        val state = when {
            receivedStateStr == AppReceiveState.FOREGROUND.name -> AppReceiveState.FOREGROUND
            isKilledState -> AppReceiveState.KILLED
            else -> AppReceiveState.BACKGROUND
        }

        // Extract custom data payload map
        val dataMap = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
            if (!key.startsWith("google.") && !key.startsWith("gcm.") &&
                key != "fcm_title" && key != "fcm_body" && key != "fcm_received_state"
            ) {
                val value = extras.getString(key) ?: extras[key]?.toString() ?: ""
                dataMap[key] = value
            }
        }

        if (title != null || body != null || dataMap.isNotEmpty()) {
            val notificationModel = NotificationModel(
                title = title ?: "FCM Notification",
                body = body ?: "Notification tapped",
                dataMap = dataMap,
                timestamp = System.currentTimeMillis(),
                receiveState = state
            )
            NotificationState.updateNotification(notificationModel)
        }
    }
}
