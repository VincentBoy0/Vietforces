package com.example.vietforces

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "VFMessagingService"
private const val CHANNEL_ID = "vietforces_push"
private const val CHANNEL_NAME = "VietForces Thông báo"
private const val NOTIFICATION_ID_BASE = 2000

/**
 * Firebase Cloud Messaging service.
 *
 * Handles two lifecycle events:
 *  • [onNewToken]          — token refresh; re-registers with Supabase when a user is logged in.
 *  • [onMessageReceived]   — incoming push message; builds a system notification and optionally
 *                           deep-links into the Daily Challenge screen via [MainActivity].
 *
 * NOTE: This service will only receive messages when the Google services plugin is configured
 * (google-services.json present). Without it, the service class compiles fine and the app
 * operates normally — push notifications are simply not delivered.
 */
class VietForcesFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // -------------------------------------------------------------------------
    // Token lifecycle
    // -------------------------------------------------------------------------

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")

        // Attempt to re-register with Supabase if a user session is active.
        serviceScope.launch {
            try {
                val supabaseUrl = BuildConfig.SUPABASE_URL
                val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
                if (supabaseUrl.isBlank() || supabaseKey.isBlank()) return@launch

                val client = io.github.jan.supabase.createSupabaseClient(
                    supabaseUrl = supabaseUrl,
                    supabaseKey = supabaseKey,
                ) {
                    install(io.github.jan.supabase.postgrest.Postgrest)
                    install(io.github.jan.supabase.auth.Auth)
                }
                val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                com.example.vietforces.data.manager.FCMTokenManager.registerToken(userId, client)
            } catch (e: Exception) {
                Log.w(TAG, "Token re-registration failed: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Incoming message
    // -------------------------------------------------------------------------

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received from: ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "VietForces"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""

        ensureNotificationChannel()

        // Build a deep-link PendingIntent when the message targets the daily-challenge screen.
        val pendingIntent = buildPendingIntent(message.data["screen"])

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 100).toInt(), notification)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun ensureNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Thông báo đẩy từ VietForces"
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildPendingIntent(screen: String?): PendingIntent? {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (screen == "daily_challenge") {
                putExtra("navigate_to", "daily_challenge")
            }
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
