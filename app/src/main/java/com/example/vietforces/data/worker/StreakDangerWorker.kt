package com.example.vietforces.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vietforces.R
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.storage.PreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Hourly WorkManager worker that checks if the user is at risk of losing their streak.
 *
 * STREAK-02: fires after 22:00 local time when last_practice_date != today UTC.
 * Posts a system notification via NotificationCompat using channel ID [CHANNEL_ID].
 */
class StreakDangerWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "streak_danger"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "streak_danger_check"
    }

    override suspend fun doWork(): Result {
        // Skip entirely for guest users — they have no streak.
        if (PreferencesManager.getIsGuest()) {
            return Result.success()
        }

        // T-03-13: Use LOCAL Calendar.HOUR_OF_DAY per spec (alert fires in user's time zone).
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 22) {
            return Result.success()
        }

        // Get last practiced date from in-memory session (loaded from SharedPreferences on app start).
        val lastPracticed = UserProgressManager.getUserSession().lastPracticeDate

        // PRE-01: UTC date string for "today" comparison — Locale.ROOT + UTC timezone.
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val todayUtc = sdf.format(Date())

        if (lastPracticed == todayUtc) {
            // Already practiced today — streak is safe.
            return Result.success()
        }

        // Ensure the notification channel exists (required for Android 8+).
        createNotificationChannelIfNeeded()

        // Build the danger notification.
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Streak sắp bị mất!")
            .setContentText("Bạn chưa học hôm nay. Hãy chơi 1 game để giữ chuỗi ngày!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("⚠️ Streak sắp bị mất! Học ngay để duy trì chuỗi ngày của bạn 🔥")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Android 13+ requires POST_NOTIFICATIONS permission; handle SecurityException silently.
        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted — fail silently rather than crashing the worker.
        }

        return Result.success()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streak Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhắc nhở khi streak sắp bị mất"
            }
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
