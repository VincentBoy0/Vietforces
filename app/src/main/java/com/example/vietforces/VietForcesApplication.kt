package com.example.vietforces

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vietforces.data.storage.PreferencesManager
import com.example.vietforces.data.worker.StreakDangerWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class VietForcesApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SharedPreferences first — must be called before any code
        // (including StreakDangerWorker) accesses PreferencesManager (CR-03).
        PreferencesManager.init(this)

        // STREAK-02: Schedule hourly streak danger check.
        // T-03-12: KEEP policy prevents duplicate enqueues across restarts.
        val streakWorkRequest = PeriodicWorkRequestBuilder<StreakDangerWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            StreakDangerWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            streakWorkRequest
        )
    }
}
