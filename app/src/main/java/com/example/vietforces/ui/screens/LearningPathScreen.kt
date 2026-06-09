package com.example.vietforces.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode as SrGameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.AiCallResult
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.data.model.GameMode
import com.example.vietforces.data.model.LearningPathItem
import com.example.vietforces.data.model.LearningPlan
import com.example.vietforces.data.model.LearningWeakness
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningPathScreen(
    onBackClick: () -> Unit = {},
    onModeClick: (GameMode) -> Unit = {},
    onWritingClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isLoading by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<LearningPlan?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun analyze() {
        errorMessage = null
        plan = null
        isLoading = true
        scope.launch {
            when (val r = AiManager.buildLearningPlan(buildStatsSummary())) {
                is AiCallResult.Success -> plan = r.data
                is AiCallResult.Error -> errorMessage = r.message
            }
            isLoading = false
        }
    }

    // Auto-analyze the first time the screen opens.
    LaunchedEffect(Unit) { analyze() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = { Text("Lộ trình học", fontWeight = FontWeight.Bold, color = VietRed) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VietRed)
                }
            },
            actions = {
                IconButton(onClick = { if (!isLoading) analyze() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Phân tích lại", tint = VietRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---- Loading ----
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp), color = VietRed, strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("AI đang phân tích điểm yếu của bạn...", color = TextSecondary)
                    }
                }
            }

            // ---- Error / fallback ----
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF6C00))
                        Spacer(Modifier.width(10.dp))
                        Text(msg, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }

            // ---- Result ----
            AnimatedVisibility(visible = plan != null) {
                plan?.let { p ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (p.summary.isNotBlank()) SummaryCard(p.summary)
                        if (p.mainWeaknesses.isNotEmpty()) WeaknessSection(p.mainWeaknesses)
                        if (p.learningPath.isNotEmpty()) {
                            Text(
                                "🗺️ Lộ trình đề xuất",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                            )
                            p.learningPath.sortedBy { it.priority }.forEach { item ->
                                PathItemCard(
                                    item = item,
                                    onPractice = { routeToMode(item.recommendedMode, onModeClick, onWritingClick) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VietRed.copy(alpha = 0.06f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text("🤖", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Nhận định của AI", fontWeight = FontWeight.SemiBold, color = VietRed)
                Spacer(Modifier.height(4.dp))
                Text(summary, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun WeaknessSection(weaknesses: List<LearningWeakness>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("⚠️ Điểm yếu cần cải thiện", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        weaknesses.forEach { w ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(w.description, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    if (w.recommendedPractice.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("💡 ${w.recommendedPractice}", fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PathItemCard(item: LearningPathItem, onPractice: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(priorityColor(item.priority).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${item.priority}", fontWeight = FontWeight.Bold, color = priorityColor(item.priority))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.description, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                modeLabel(item.recommendedMode)?.let { label ->
                    Surface(shape = RoundedCornerShape(8.dp), color = VietYellow.copy(alpha = 0.2f)) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = Color(0xFF8D6E00),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPractice) {
                    Text("Luyện ngay", color = VietRed, fontWeight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = VietRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun priorityColor(priority: Int): Color = when (priority) {
    1 -> Color(0xFFC62828)
    2 -> Color(0xFFEF6C00)
    3 -> Color(0xFFF9A825)
    else -> Color(0xFF2E7D32)
}

/** Map an AI-returned recommendedMode string to a Vietnamese label. */
private fun modeLabel(mode: String): String? {
    if (mode.equals("writing", ignoreCase = true)) return "Luyện viết"
    return GameMode.fromId(mode.lowercase())?.title
}

/** Navigate to the recommended practice, if it maps to a known mode. */
private fun routeToMode(mode: String, onModeClick: (GameMode) -> Unit, onWritingClick: () -> Unit) {
    if (mode.equals("writing", ignoreCase = true)) {
        onWritingClick()
        return
    }
    GameMode.fromId(mode.lowercase())?.let(onModeClick)
}

/**
 * Build a compact Vietnamese summary of the user's stats for the AI to analyse (§6.4).
 * Pulls from Elo/streak, overall accuracy, per-mode accuracy and the words most
 * frequently answered wrong (spaced-repetition history).
 */
private fun buildStatsSummary(): String {
    val session = UserProgressManager.getUserSession()
    val rank = EloRankUtils.getCurrentRank(session.eloRating)
    val rankVi = EloRankUtils.getVietnameseRankName(rank.name)

    val sb = StringBuilder()
    sb.append("Thống kê học tập của người dùng:\n")
    sb.append("- Điểm ELO: ${session.eloRating} (hạng $rankVi), streak ${session.currentStreak} ngày.\n")
    sb.append("- Tổng câu đúng: ${session.totalCorrectAnswers}, sai: ${session.totalWrongAnswers}, ")
    sb.append("độ chính xác: ${(session.accuracyRate * 100).toInt()}%.\n")

    val modeStats = UserProgressManager.getAllGameModeStats()
    if (modeStats.isEmpty()) {
        sb.append("- Người dùng chưa chơi chế độ nào nhiều.\n")
    } else {
        sb.append("- Độ chính xác theo chế độ:\n")
        modeStats.forEach { (id, stat) ->
            val name = GameMode.fromId(id)?.title ?: id
            if (stat.totalAnswers > 0) {
                sb.append("  + $name: ${(stat.accuracyRate * 100).toInt()}% ")
                sb.append("(${stat.correctAnswers} đúng / ${stat.wrongAnswers} sai).\n")
            }
        }
    }

    // Words most often answered wrong across all modes.
    val wrongWords = mutableMapOf<String, Int>()
    SrGameMode.entries.forEach { mode ->
        EncounteredItemsManager.getStats(mode).values.forEach { item ->
            if (item.wrongCount > 0) {
                wrongWords[item.itemId] = (wrongWords[item.itemId] ?: 0) + item.wrongCount
            }
        }
    }
    val topWrong = wrongWords.entries.sortedByDescending { it.value }.take(8)
    if (topWrong.isNotEmpty()) {
        sb.append("- Các mục hay sai nhất: ")
        sb.append(topWrong.joinToString(", ") { "${it.key}(${it.value} lần)" })
        sb.append(".\n")
    }

    sb.append("\nHãy phân tích điểm yếu và tạo lộ trình học cá nhân phù hợp.")
    return sb.toString()
}
