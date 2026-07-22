package com.example.vietforces.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for user progress synced to/from Supabase user_progress table.
 * Uses @SerialName for snake_case ↔ camelCase mapping.
 */
@Serializable
data class UserProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("elo_rating") val eloRating: Int,
    @SerialName("current_streak") val currentStreak: Int,
    @SerialName("longest_streak") val longestStreak: Int,
    @SerialName("words_learned_count") val wordsLearnedCount: Int,
    @SerialName("last_practiced") val lastPracticed: String,
    @SerialName("updated_at") val updatedAt: String = ""
)
