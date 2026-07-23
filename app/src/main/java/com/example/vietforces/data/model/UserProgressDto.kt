package com.example.vietforces.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for user progress synced to/from Supabase user_progress table.
 * Uses @SerialName for snake_case ↔ camelCase mapping.
 *
 * Column mapping aligned with 001_initial_schema.sql user_progress table:
 *   elo_score          — ELO rating (was incorrectly "elo_rating")
 *   streak_count       — current streak (was incorrectly "current_streak")
 *   last_practice_date — last practice date (was incorrectly "last_practiced")
 *   total_games        — replaces the absent "words_learned_count" column
 * Note: "longest_streak" has no corresponding DB column and has been removed.
 */
@Serializable
data class UserProgressDto(
    @SerialName("user_id")            val userId: String,
    @SerialName("elo_score")          val eloScore: Int,
    @SerialName("streak_count")       val streakCount: Int,
    @SerialName("last_practice_date") val lastPracticeDate: String?,
    @SerialName("total_games")        val totalGames: Int = 0,
    @SerialName("updated_at")         val updatedAt: String = ""
)
