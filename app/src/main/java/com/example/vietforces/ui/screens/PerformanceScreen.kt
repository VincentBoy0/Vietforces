package com.example.vietforces.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.EloHistoryEntry
import com.example.vietforces.data.model.EloRank
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Performance Screen - shows user achievements, Elo history, activity heatmap and streak
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun PerformanceScreen(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Force refresh data every time screen is shown by using a refresh key
    var refreshKey by remember { mutableIntStateOf(0) }

    // Refresh when screen becomes visible
    LaunchedEffect(Unit) {
        refreshKey++
    }

    // Get fresh data from UserProgressManager using key to trigger recomposition
    val eloRating = remember(refreshKey) { UserProgressManager.getEloRating() }
    val currentStreak = remember(refreshKey) { UserProgressManager.getCurrentStreak() }
    val longestStreak = remember(refreshKey) { UserProgressManager.getLongestStreak() }
    val eloHistory = remember(refreshKey) { UserProgressManager.getEloHistory() }
    val dailyPracticeHistory = remember(refreshKey) { UserProgressManager.getDailyPracticeHistory() }
    val userSession = remember(refreshKey) { UserProgressManager.getUserSession() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Thành tích",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elo Rating Card
            EloRatingCard(
                eloRating = eloRating,
                totalExercises = userSession.totalExercisesCompleted,
                accuracyRate = userSession.accuracyRate
            )

            // Streak Card
            StreakCard(
                currentStreak = currentStreak,
                longestStreak = longestStreak
            )

            // Elo History Chart
            EloHistoryCard(eloHistory = eloHistory)

            // Elo Rank Table - Full ranking system
            EloRankTableCard(eloRating = eloRating)

            // Activity Heatmap
            ActivityHeatmapCard(dailyPracticeHistory = dailyPracticeHistory)

            // Game Mode Stats
            GameModeStatsCard(refreshKey = refreshKey)

            // Stats Summary
            StatsSummaryCard(userSession = userSession)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Elo Rating Card with rank visualization
 */
@Composable
private fun EloRatingCard(
    eloRating: Int,
    totalExercises: Int,
    accuracyRate: Float
) {
    val currentRank = EloRankUtils.getCurrentRank(eloRating)
    val rankName = EloRankUtils.getVietnameseRankName(currentRank.name)
    val rankColor = currentRank.color

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Điểm Elo",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Elo rating with animation
            Text(
                text = eloRating.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor
            )

            // Rank badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = rankColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = rankName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = rankColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalExercises.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Bài tập",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(accuracyRate * 100).toInt()}%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Chính xác",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}


/**
 * Streak Card showing current and longest streak
 */
@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Current streak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(VietRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$currentStreak ngày",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
                Text(
                    text = "Chuỗi hiện tại",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(100.dp)
                    .background(Color(0xFFEEEEEE))
            )

            // Longest streak
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(VietYellow.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$longestStreak ngày",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VietGold
                )
                Text(
                    text = "Kỷ lục",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Elo History Chart Card
 */
@Composable
private fun EloHistoryCard(eloHistory: List<EloHistoryEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Lịch sử Elo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                if (eloHistory.isNotEmpty()) {
                    val lastChange = eloHistory.last().change
                    Text(
                        text = "${if (lastChange >= 0) "+" else ""}$lastChange",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (lastChange >= 0) PrimaryGreen else VietRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (eloHistory.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📊",
                            fontSize = 40.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có dữ liệu",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Hoàn thành bài tập để xem biểu đồ",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Elo chart
                EloLineChart(
                    eloHistory = eloHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

/**
 * Simple Elo Line Chart
 */
@Composable
private fun EloLineChart(
    eloHistory: List<EloHistoryEntry>,
    modifier: Modifier = Modifier
) {
    val points = eloHistory.takeLast(20) // Show last 20 points
    if (points.isEmpty()) return

    val minElo = (points.minOfOrNull { it.elo } ?: 1000) - 50
    val maxElo = (points.maxOfOrNull { it.elo } ?: 1000) + 50
    val range = maxElo - minElo

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f

        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // Draw grid lines
        val gridColor = Color(0xFFEEEEEE)
        for (i in 0..4) {
            val y = padding + (chartHeight * i / 4)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Draw line chart
        if (points.size > 1) {
            val path = Path()
            points.forEachIndexed { index, entry ->
                val x = padding + (chartWidth * index / (points.size - 1))
                val y = padding + chartHeight - (chartHeight * (entry.elo - minElo) / range)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = PrimaryBlue,
                style = Stroke(width = 3f)
            )

            // Draw points
            points.forEachIndexed { index, entry ->
                val x = padding + (chartWidth * index / (points.size - 1))
                val y = padding + chartHeight - (chartHeight * (entry.elo - minElo) / range)

                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = if (entry.change >= 0) PrimaryGreen else VietRed,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }

        // Draw labels
        drawContext.canvas.nativeCanvas.apply {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 24f
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            // Y-axis labels
            drawText(maxElo.toString(), padding - 8, padding + 8, textPaint)
            drawText(minElo.toString(), padding - 8, height - padding + 8, textPaint)
        }
    }
}

/**
 * Activity Heatmap Card - GitHub style contribution graph
 */
@Composable
private fun ActivityHeatmapCard(dailyPracticeHistory: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📅 Hoạt động học tập",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Generate last 12 weeks of data
            val weeks = generateWeeksData(dailyPracticeHistory)

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Empty space for alignment
                Spacer(modifier = Modifier.width(28.dp))

                // Week numbers/months could go here
                Text(
                    text = "12 tuần gần nhất",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Heatmap grid
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Day labels
                Column(
                    modifier = Modifier.width(24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 8.sp,
                            color = TextSecondary,
                            modifier = Modifier.height(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Heatmap cells
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(weeks) { week ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            week.forEach { dayData ->
                                HeatmapCell(
                                    level = dayData.level
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ít",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                (0..4).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(level))
                    )
                    if (level < 4) Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Nhiều",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Data class for heatmap day
 */
private data class HeatmapDayData(
    val date: String,
    val count: Int,
    val level: Int // 0-4 intensity level
)

/**
 * Generate weeks data for heatmap
 * Current date will be at bottom-right corner
 */
private fun generateWeeksData(dailyPracticeHistory: Map<String, Int>): List<List<HeatmapDayData>> {
    val weeks = mutableListOf<List<HeatmapDayData>>()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    val today = Calendar.getInstance()

    // Get current day of week (1 = Sunday, 2 = Monday, ..., 7 = Saturday)
    // Convert to Monday-based (0 = Monday, 6 = Sunday)
    val todayDayOfWeek = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7

    // Calculate start date: go back 11 full weeks + days until Monday of current week
    calendar.time = today.time
    calendar.add(Calendar.DAY_OF_MONTH, -(11 * 7 + todayDayOfWeek))

    repeat(12) { weekIndex ->
        val week = mutableListOf<HeatmapDayData>()
        repeat(7) { dayIndex ->
            val currentDate = calendar.time
            val dateStr = dateFormat.format(currentDate)

            // Check if this date is in the future
            val isFuture = calendar.after(today)

            if (isFuture) {
                // Don't show future dates - use empty/invisible cell
                week.add(HeatmapDayData(dateStr, -1, -1))
            } else {
                val count = dailyPracticeHistory[dateStr] ?: 0
                val level = when {
                    count == 0 -> 0
                    count <= 2 -> 1
                    count <= 5 -> 2
                    count <= 10 -> 3
                    else -> 4
                }
                week.add(HeatmapDayData(dateStr, count, level))
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        weeks.add(week)
    }

    return weeks
}

/**
 * Get heatmap cell color based on activity level
 */
private fun getHeatmapColor(level: Int): Color {
    return when (level) {
        0 -> Color(0xFFEBEDF0)
        1 -> Color(0xFF9BE9A8)
        2 -> Color(0xFF40C463)
        3 -> Color(0xFF30A14E)
        4 -> Color(0xFF216E39)
        else -> Color(0xFFEBEDF0)
    }
}

/**
 * Single heatmap cell
 */
@Composable
private fun HeatmapCell(
    level: Int
) {
    // Hide future dates (level = -1)
    if (level < 0) {
        Box(modifier = Modifier.size(14.dp))
        return
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(getHeatmapColor(level))
            .border(
                width = 0.5.dp,
                color = Color(0x22000000),
                shape = RoundedCornerShape(2.dp)
            )
    )
}

/**
 * Stats Summary Card
 */
@Composable
private fun StatsSummaryCard(userSession: com.example.vietforces.data.model.UserSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📊 Thống kê chi tiết",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        icon = "✅",
                        label = "Trả lời đúng",
                        value = userSession.totalCorrectAnswers.toString(),
                        color = PrimaryGreen
                    )
                    StatItem(
                        icon = "❌",
                        label = "Trả lời sai",
                        value = userSession.totalWrongAnswers.toString(),
                        color = VietRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        icon = "📚",
                        label = "Từ đã học",
                        value = UserProgressManager.getLearnedWordsCount().toString(),
                        color = PrimaryBlue
                    )
                    StatItem(
                        icon = "📅",
                        label = "Ngày luyện tập",
                        value = userSession.dailyPracticeHistory.size.toString(),
                        color = VietGold
                    )
                }
            }
        }
    }
}

/**
 * Individual stat item
 */
@Composable
private fun RowScope.StatItem(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Game mode stats data
 */
private data class GameModeInfo(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color
)

/**
 * Game Mode Stats Card - shows statistics for each game mode
 */
@Composable
private fun GameModeStatsCard(refreshKey: Int) {
    val gameModes = listOf(
        GameModeInfo("image_to_word", "Nhìn hình đoán từ", "🖼️", GameModeImageToWord),
        GameModeInfo("word_to_image", "Nhìn từ đoán hình", "📝", GameModeWordToImage),
        GameModeInfo("syllable_match", "Ghép âm tiết", "🔤", GameModeSyllable),
        GameModeInfo("sentence_order", "Sắp xếp câu", "📖", GameModeSentence),
        GameModeInfo("fill_blank", "Điền từ vào chỗ trống", "✏️", GameModeFillBlank),
        GameModeInfo("word_chain", "Nối từ", "🔗", GameModeWordChain),
        GameModeInfo("word_search", "Tìm từ trong bảng", "🔍", GameModeWordSearch)
    )

    // Get fresh game mode stats using refreshKey
    val gameModeStats = remember(refreshKey) {
        gameModes.associate { it.id to UserProgressManager.getGameModeStats(it.id) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🎮 Thống kê theo chế độ chơi",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            gameModes.forEach { gameMode ->
                val stats = gameModeStats[gameMode.id] ?: UserProgressManager.getGameModeStats(gameMode.id)
                GameModeStatRow(
                    gameMode = gameMode,
                    gamesPlayed = stats.gamesPlayed,
                    accuracyRate = stats.accuracyRate
                )
                if (gameMode != gameModes.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Individual game mode stat row
 */
@Composable
private fun GameModeStatRow(
    gameMode: GameModeInfo,
    gamesPlayed: Int,
    accuracyRate: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gameMode.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = gameMode.icon,
                fontSize = 22.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and games played
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = gameMode.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = if (gamesPlayed > 0) "$gamesPlayed lượt chơi" else "Chưa chơi",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Accuracy
        if (gamesPlayed > 0) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${(accuracyRate * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        accuracyRate >= 0.8f -> PrimaryGreen
                        accuracyRate >= 0.5f -> VietGold
                        else -> VietRed
                    }
                )
                Text(
                    text = "Chính xác",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        } else {
            Text(
                text = "—",
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Elo Rank Table Card - shows full ranking system
 */
@Composable
private fun EloRankTableCard(eloRating: Int) {
    val currentRank = EloRankUtils.getCurrentRank(eloRating)
    val allRanks = EloRankUtils.getEloRanks()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🏆 Bảng xếp hạng Elo",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // All ranks from Newbie to Legendary Grandmaster
            allRanks.forEach { rank ->
                val isCurrentRank = rank == currentRank
                EloRankRow(
                    rank = rank,
                    isCurrentRank = isCurrentRank,
                    currentElo = if (isCurrentRank) eloRating else null
                )
                if (rank != allRanks.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Individual Elo rank row
 */
@Composable
private fun EloRankRow(
    rank: EloRank,
    isCurrentRank: Boolean,
    currentElo: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentRank) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rank.color.copy(alpha = 0.1f))
                        .border(1.dp, rank.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                } else {
                    Modifier.padding(8.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(rank.color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Rank name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = EloRankUtils.getVietnameseRankName(rank.name),
                fontSize = 14.sp,
                fontWeight = if (isCurrentRank) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrentRank) rank.color else TextPrimary
            )
            Text(
                text = rank.name,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // Elo range
        Text(
            text = if (rank.maxElo >= 9999) "${rank.minElo}+" else "${rank.minElo} - ${rank.maxElo}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCurrentRank) rank.color else TextSecondary
        )

        // Current Elo indicator
        if (isCurrentRank && currentElo != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "← Bạn",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = rank.color
            )
        }
    }
}

