package com.example.vietforces.data.manager

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

private const val TAG = "FCMTokenManager"

/**
 * Manages FCM device token registration and deletion in Supabase.
 *
 * The google-services.json is not required at compile time because we do NOT apply
 * the google-services Gradle plugin. If Firebase is not initialized at runtime the
 * token fetch will throw and we simply return early — the app continues normally.
 */
object FCMTokenManager {

    @Serializable
    private data class FcmTokenRow(
        val user_id: String,
        val token: String,
        val updated_at: String,
    )

    /**
     * Fetches the current FCM registration token and upserts it to the
     * `fcm_tokens` table keyed by [userId].
     *
     * Safe to call even when Firebase is not initialised — any exception is
     * caught and logged rather than propagated.
     */
    suspend fun registerToken(userId: String, supabaseClient: SupabaseClient) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val now = java.time.Instant.now().toString()
            supabaseClient.postgrest["fcm_tokens"].upsert(
                FcmTokenRow(
                    user_id = userId,
                    token = token,
                    updated_at = now,
                )
            )
            Log.d(TAG, "FCM token registered for user $userId")
        } catch (e: Exception) {
            // Firebase may not be initialised (no google-services.json) — this is
            // expected in development and is not a fatal error.
            Log.w(TAG, "Failed to register FCM token (Firebase may not be configured): ${e.message}")
        }
    }

    /**
     * Removes the FCM token row for [userId] from Supabase so this device
     * no longer receives push notifications for that account.
     */
    suspend fun deleteToken(userId: String, supabaseClient: SupabaseClient) {
        try {
            supabaseClient.postgrest["fcm_tokens"].delete {
                filter {
                    eq("user_id", userId)
                }
            }
            Log.d(TAG, "FCM token deleted for user $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete FCM token: ${e.message}")
        }
    }
}
