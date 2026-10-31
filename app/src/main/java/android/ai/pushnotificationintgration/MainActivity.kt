package android.ai.pushnotificationintgration

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.ai.pushnotificationintgration.ui.theme.PushNotificationIntgrationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.onPermissionResult(isGranted)
        val message = if (isGranted) "Notification permission granted" else "Notification permission denied"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create FCM Notification Channel for Android 8.0+
        NotificationHelper.createNotificationChannel(this)

        // Check runtime permission state for Android 13+ (API 33+)
        viewModel.checkNotificationPermission(this)

        // Fetch current FCM Registration Token
        viewModel.fetchFcmToken()

        // Handle intent if app launched from notification tap (Killed State)
        viewModel.handleNotificationIntent(intent, isKilledState = true)

        setContent {
            PushNotificationIntgrationTheme {
                val fcmToken by viewModel.fcmToken.collectAsState()
                val latestNotification by viewModel.latestNotification.collectAsState()
                val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()

                MainScreen(
                    fcmToken = fcmToken,
                    latestNotification = latestNotification,
                    isPermissionGranted = isPermissionGranted,
                    onRequestPermission = { requestNotificationPermission() },
                    onCopyToken = { copyToClipboard(fcmToken ?: "No token available") }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle intent if notification tapped while app in Background state
        viewModel.handleNotificationIntent(intent, isKilledState = false)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("FCM Token", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "FCM Token copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    fcmToken: String?,
    latestNotification: NotificationModel?,
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCopyToken: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FCM Push Notifications",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PermissionCard(
                    isGranted = isPermissionGranted,
                    onRequestPermission = onRequestPermission
                )
            }

            item {
                FcmTokenCard(
                    token = fcmToken,
                    onCopyToken = onCopyToken
                )
            }

            item {
                NotificationDetailsCard(notification = latestNotification)
            }
        }
    }
}

@Composable
fun PermissionCard(
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (isGranted) "Status: Allowed" else "Status: Permission Needed (Android 13+)",
                    fontSize = 14.sp,
                    color = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
            if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(onClick = onRequestPermission) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
fun FcmTokenCard(
    token: String?,
    onCopyToken: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FCM Registration Token",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Button(
                    onClick = onCopyToken,
                    enabled = !token.isNull_or_Empty()
                ) {
                    Text("Copy Token")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = token ?: "Fetching FCM token...",
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NotificationDetailsCard(notification: NotificationModel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latest Push Notification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (notification != null) {
                    StateBadge(state = notification.receiveState)
                }
            }

            HorizontalDivider()

            if (notification == null) {
                Text(
                    text = "No notification received yet.\nSend a message from the Firebase Console or via FCM API.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Title:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = notification.title ?: "(No Title)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Body:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = notification.body ?: "(No Body)",
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val formattedTime = remember(notification.timestamp) {
                        SimpleDateFormat("hh:mm:ss a, dd MMM yyyy", Locale.getDefault())
                            .format(Date(notification.timestamp))
                    }
                    Text(
                        text = "Received At: $formattedTime",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (notification.dataMap.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Custom Data Payload:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                notification.dataMap.forEach { (key, value) ->
                                    Text(
                                        text = "$key = $value",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StateBadge(state: AppReceiveState) {
    val (backgroundColor, textColor, label) = when (state) {
        AppReceiveState.FOREGROUND -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "FOREGROUND")
        AppReceiveState.BACKGROUND -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "BACKGROUND TAP")
        AppReceiveState.KILLED -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "KILLED LAUNCH")
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

private fun String?.isNull_or_Empty(): Boolean = this == null || this.trim().isEmpty()
