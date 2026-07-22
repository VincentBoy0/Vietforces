package com.example.vietforces.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ELO result returned by the `calculate_elo` SQL RPC.
 * Field names match the JSON keys produced by json_build_object() in the function.
 *
 * ELO-01: the server computes the delta — the client only sends raw game metrics.
 */
@Serializable
data class EloResult(
    @SerialName("new_elo")   val newElo: Int,
    @SerialName("rank_tier") val rankTier: String,
    @SerialName("elo_delta") val eloDelta: Int
)

/**
 * Repository that wraps the `calculate_elo` Supabase RPC.
 *
 * After a successful call the result is available to the caller;
 * local state updates (ELO rating, notification check) are performed
 * by [ProgressRepository.postGame] which owns the full post-game pipeline.
 *
 * Companion object [instance] is set in `init {}` so non-Hilt call sites
 * (e.g. a standalone ProgressRepository hook) can reach the singleton.
 */
@Singleton
class EloRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) {

    companion object {
        /**
         * Static accessor set during Hilt initialisation.
         * Always non-null after the singleton is first injected.
         */
        var instance: EloRepository? = null
    }

    init {
        instance = this
    }

    /**
     * Calls the `calculate_elo` RPC with raw game metrics and returns [EloResult].
     *
     * Returns [Result.failure] if the user is not authenticated or if the RPC throws.
     */
    suspend fun calculateElo(correct: Int, total: Int, timeMs: Long): Result<EloResult> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            // Threat T-03-05: only raw game metrics — server computes the ELO delta.
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_correct", correct)
                put("p_total", total)
                put("p_time_ms", timeMs)
            }
            val result = supabase.postgrest
                .rpc("calculate_elo", params)
                .decodeAs<EloResult>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
