package com.example.vietforces.data.model

import androidx.compose.ui.graphics.Color

/**
 * Elo rank data class with Codeforces-style ranking
 */
data class EloRank(
    val name: String,
    val minElo: Int,
    val maxElo: Int,
    val color: Color,
    val secondaryColor: Color = color
)

/**
 * Elo ranking utilities - Codeforces style
 */
object EloRankUtils {

    /**
     * Get all Codeforces-style Elo ranks
     */
    fun getEloRanks(): List<EloRank> = listOf(
        EloRank("Newbie", 0, 1199, Color(0xFF808080), Color(0xFF808080)),                    // Gray
        EloRank("Pupil", 1200, 1399, Color(0xFF008000), Color(0xFF008000)),                  // Green
        EloRank("Specialist", 1400, 1599, Color(0xFF03A89E), Color(0xFF03A89E)),             // Cyan
        EloRank("Expert", 1600, 1899, Color(0xFF0000FF), Color(0xFF0000FF)),                 // Blue
        EloRank("Candidate Master", 1900, 2099, Color(0xFFAA00AA), Color(0xFFAA00AA)),       // Violet
        EloRank("Master", 2100, 2299, Color(0xFFFF8C00), Color(0xFFFF8C00)),                 // Orange
        EloRank("International Master", 2300, 2399, Color(0xFFFF8C00), Color(0xFFFF8C00)),   // Orange
        EloRank("Grandmaster", 2400, 2599, Color(0xFFFF0000), Color(0xFFFF0000)),            // Red
        EloRank("International Grandmaster", 2600, 2899, Color(0xFFFF0000), Color(0xFFFF0000)), // Red
        EloRank("Legendary Grandmaster", 2900, 9999, Color(0xFFFF0000), Color(0xFF000000))   // Red with black
    )

    /**
     * Get Vietnamese Elo rank name
     */
    fun getVietnameseRankName(englishName: String): String = when (englishName) {
        "Newbie" -> "Nghiệp dư"
        "Pupil" -> "Nhập môn"
        "Specialist" -> "Chuyên viên"
        "Expert" -> "Chuyên gia"
        "Candidate Master" -> "Ứng viên Kiện tướng"
        "Master" -> "Kiện tướng"
        "International Master" -> "Kiện tướng Quốc tế"
        "Grandmaster" -> "Đại Kiện tướng"
        "International Grandmaster" -> "Đại Kiện tướng Quốc tế"
        "Legendary Grandmaster" -> "Huyền thoại"
        else -> englishName
    }

    /**
     * Get current rank based on Elo
     */
    fun getCurrentRank(elo: Int): EloRank {
        return getEloRanks().find { elo in it.minElo..it.maxElo }
            ?: getEloRanks().first()
    }

    /**
     * Get rank color for an Elo rating
     */
    fun getRankColor(elo: Int): Color {
        return getCurrentRank(elo).color
    }
}

