package com.example.vietforces.data.remote

import com.example.vietforces.data.model.UserProgressDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Postgrest data source for user_progress table.
 * Provides upsert (SYNC-01) and select (SYNC-02) operations.
 */
@Singleton
class RemoteProgressSource @Inject constructor(
    private val supabase: SupabaseClient
) {

    /**
     * Upserts progress row for the given user.
     * Conflict resolution: ON CONFLICT (user_id) DO UPDATE — last-write-wins via updated_at.
     */
    suspend fun upsertProgress(dto: UserProgressDto): Result<Unit> {
        return try {
            supabase.from("user_progress").upsert(dto) {
                onConflict = "user_id"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the progress row for the given userId, or null if no row exists.
     */
    suspend fun fetchProgress(userId: String): Result<UserProgressDto?> {
        return try {
            val result = supabase.from("user_progress")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserProgressDto>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
