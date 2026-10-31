# Firebase Cloud Messaging (FCM) Complete Integration & Concepts Guide

This guide details the complete architecture, implementation patterns, message lifecycle handling across different application states, and the **Top 10 Android FCM Interview Questions** with in-depth answers.

---

## 1. Architecture & Component Overview

The application follows modern Android architecture (MVVM + Jetpack Compose + Kotlin Coroutines & StateFlow) to deliver a robust push notification experience.

```
                  ┌───────────────────────────────┐
                  │    Firebase Cloud Messaging   │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                     ┌──────────────────────────┐
                     │ MyFirebaseMessagingService│
                     └────────────┬─────────────┘
                                  │
         ┌────────────────────────┴────────────────────────┐
         ▼                                                 ▼
┌─────────────────┐                               ┌─────────────────┐
│NotificationState│ (Foreground UI update)        │NotificationHelper│ (Local Heads-up)
└────────┬────────┘                               └────────┬────────┘
         │                                                 │
         │                  ┌──────────────┐               │
         └─────────────────►│ MainViewModel│◄──────────────┘
                            └──────┬───────┘  (Intent Extras parsing)
                                   │
                                   ▼
                            ┌──────────────┐
                            │ MainActivity │ (Compose UI)
                            └──────────────┘
```

### Core Components
1. **`MyFirebaseMessagingService`**: Extends `FirebaseMessagingService`. Overrides `onNewToken()` for registration token management and `onMessageReceived()` for incoming push notifications in the foreground.
2. **`NotificationHelper`**: Responsible for creating Android 8.0+ Notification Channels (`fcm_default_channel`) and building local heads-up notifications with `PendingIntent` wrappers.
3. **`NotificationState`**: Thread-safe global repository backed by Kotlin `StateFlow` to reactively stream the FCM token and latest notification payload across components.
4. **`MainViewModel`**: Manages UI state, handles FCM token retrieval, checks/requests `POST_NOTIFICATIONS` runtime permissions, and parses notification intent extras.
5. **`MainActivity`**: Single-activity Jetpack Compose user interface displaying FCM token details, permission status, and live notification cards.

---

## 2. FCM Payload Types

FCM supports three types of message payloads:

| Payload Type | Structure Example | Description & Behavior |
| :--- | :--- | :--- |
| **Notification Payload** | `{"notification": {"title": "Hello", "body": "World"}}` | Pre-defined title & body. System tray handles display automatically when app is in Background/Killed state. |
| **Data Payload** | `{"data": {"key1": "value1", "user_id": "123"}}` | Custom key-value pairs. Delivered directly to `onMessageReceived()` regardless of app state. |
| **Combined Payload** | `{"notification": {...}, "data": {...}}` | System tray handles notification in background, while `data` key-value pairs are attached to the launch intent extras. |

---

## 3. FCM Message Routing Across App States

| App State | Delivery Mechanism | System Tray Behavior | Action on Notification Tap |
| :--- | :--- | :--- | :--- |
| **Foreground** | Delivered to `onMessageReceived()` | No automatic system notification. Created programmatically via `NotificationHelper`. | Tapping local notification fires `PendingIntent` to `MainActivity`. |
| **Background** | System tray handles notification automatically | System displays notification automatically in system drawer. | Opens `MainActivity` via `onNewIntent()`. Data payload is passed in `intent.extras`. |
| **Killed / Cold Start** | System tray handles notification automatically | System displays notification automatically in system drawer. | Cold-starts `MainActivity` via `onCreate()`. Data payload is passed in `intent.extras`. |

---

## 4. Intent Extra Parsing & SingleTop Launch Mode

To prevent duplicate activity instances when tapping notifications, `MainActivity` uses `android:launchMode="singleTop"`.

### Intent Parsing Flow
- **Killed State (Cold Start)**: Handled inside `MainActivity.onCreate(savedInstanceState)`:
  ```kotlin
  viewModel.handleNotificationIntent(intent, isKilledState = true)
  ```
- **Background State (Warm Navigation)**: Handled inside `MainActivity.onNewIntent(intent)`:
  ```kotlin
  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      viewModel.handleNotificationIntent(intent, isKilledState = false)
  }
  ```

---

## 5. Top 10 FCM Android Interview Questions & Answers

### Q1: What is the difference between a Notification Payload and a Data Payload in FCM?
**Answer**:
- **Notification Payload**: Contains predefined keys (`title`, `body`, `icon`, `click_action`). When the app is in the background or killed, the Android system automatically intercepts the message and displays a system notification without waking up your app code.
- **Data Payload**: Contains custom key-value pairs (`{"key1": "value1"}`). It is always delivered to `FirebaseMessagingService.onMessageReceived()`, allowing your app to perform background processing or build custom notifications regardless of whether the app is in the foreground or background.

---

### Q2: What happens when an FCM push notification is received in Foreground vs Background vs Killed state?
**Answer**:
1. **Foreground State**: `onMessageReceived()` is always triggered. The system does *not* display an automatic notification. The app must build and display a notification manually using `NotificationCompat.Builder`.
2. **Background State**:
   - For *Notification Payload*: System tray shows notification automatically. Tapping it launches the root activity and puts extras into `intent.extras`.
   - For *Data Payload*: Delivered to `onMessageReceived()`.
3. **Killed State**:
   - For *Notification Payload*: System tray shows notification automatically. Tapping it cold-starts the app (`onCreate()`).
   - For *Data Payload*: Delivered to `onMessageReceived()` (subject to OS battery/Doze restrictions).

---

### Q3: How do you extract custom payload parameters when a user clicks a notification?
**Answer**:
When a notification is clicked in background/killed state, custom data key-value pairs are attached to the launch `Intent` as extras.
```kotlin
val extras = intent.extras
val customValue = extras?.getString("my_custom_key")
```
In `onCreate()` or `onNewIntent()`, iterate through `intent.extras.keySet()` to extract key-value pairs and navigate to the appropriate screen.

---

### Q4: When is `onNewToken()` called in FCM, and why is it important to send the new token to your server?
**Answer**:
`onNewToken()` is invoked by the FCM SDK when a registration token is initially generated or refreshed.
Token refresh happens when:
1. App is restored on a new device.
2. User uninstalls and reinstall the app.
3. User clears app data.
4. Token is invalidated due to security or Firebase backend updates.

**Why it matters**: Your backend app server uses this token as the address to target push notifications to a specific device instance. If the token updates and your server is not notified, push notifications will fail (`UNREGISTERED` / `NOT_FOUND` error).

---

### Q5: Why is `android:launchMode="singleTop"` recommended for activities handling FCM notification clicks?
**Answer**:
If a user taps multiple notifications while the app is already open or in the background, default launch mode (`standard`) would create multiple stacked instances of `MainActivity`.
Using `singleTop` ensures that if `MainActivity` is already at the top of the activity stack, Android reuses the existing instance and routes the new notification intent to `onNewIntent(intent)` instead of recreating the activity.

---

### Q6: Why are Notification Channels mandatory on Android 8.0+ (API 26+), and how do they work with FCM?
**Answer**:
Starting with Android 8.0 (API 26), all notifications must be assigned to a `NotificationChannel`. Channels give users granular control to enable, disable, or adjust importance/sound for specific notification categories in System Settings.

For FCM:
You must define a default channel ID in `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.firebase.messaging.default_notification_channel_id"
    android:value="fcm_default_channel" />
```
And create the channel programmatically in code:
```kotlin
val channel = NotificationChannel("fcm_default_channel", "FCM Notifications", NotificationManager.IMPORTANCE_HIGH)
notificationManager.createNotificationChannel(channel)
```

---

### Q7: What changes were introduced in Android 13 (API 33) for Push Notifications?
**Answer**:
In Android 13 (API level 33) and higher, sending notifications requires the runtime permission:
`android.permission.POST_NOTIFICATIONS`

Key aspects:
1. Must declare `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` in `AndroidManifest.xml`.
2. Must prompt the user at runtime before posting notifications.
3. If permission is denied, system notifications generated by FCM or `NotificationManager` will be silently blocked by the OS.

---

### Q8: What are the payload size limits for FCM messages?
**Answer**:
- **Notification / Data Payload Limit**: **4 KB (4096 bytes)**.
- **Topic Messaging Limit**: **2 KB**.

*Best Practice*: Keep FCM payloads small (send notification IDs or deep links instead of large data objects) and fetch detailed content from your REST/GraphQL server upon receiving the push.

---

### Q9: How do High Priority messages and Doze Mode interact in FCM?
**Answer**:
Android's Doze Mode restricts background network and CPU access to preserve battery life.
- **Normal Priority Messages**: Delivered during periodic Doze maintenance windows.
- **High Priority Messages**: Bypasses Doze mode to wake up the network and CPU immediately for time-sensitive notifications (e.g., VoIP calls, chat messages).

*Note*: Google enforces quota limits on high-priority messages if abused.

---

### Q10: How do you troubleshoot when FCM push notifications are not received on a device?
**Answer**:
Troubleshooting steps:
1. **Verify Token**: Ensure `FirebaseMessaging.getInstance().token` returns a valid token and print it in logs.
2. **Check Google Services JSON**: Verify `google-services.json` package name matches `applicationId` in `build.gradle.kts`.
3. **Notification Permission**: On Android 13+, confirm `POST_NOTIFICATIONS` permission is granted.
4. **Check Device OEM Settings**: Chinese OEMs (Xiaomi, Oppo, Vivo, Huawei) have aggressive background task killers / battery savers. Auto-start permissions must be enabled.
5. **Use Firebase Console Messaging Tester**: Test sending directly to a single FCM registration token.
6. **Inspect Logcat**: Filter logs by `FCM_Service` or `FirebaseMessaging`.
