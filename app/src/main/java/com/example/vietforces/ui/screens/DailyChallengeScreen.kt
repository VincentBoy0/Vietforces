package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vietforces.data.model.VocabularyItem
import com.example.vietforces.data.repository.DailyChallenge
import com.example.vietforces.data.repository.DailyChallengeHistoryItem
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.ShimmerBox
import com.example.vietforces.ui.theme.*
import com.example.vietforces.ui.viewmodel.DailyChallengeUiState
import com.example.vietforces.ui.viewmodel.DailyChallengeViewModel
import java.util.Locale

/**
 * Full-screen daily challenge composable.
 *
 * States handled:
 *  - Loading     → ShimmerBox placeholders
 *  - NoChallenge → empty state with retry
 *  - Ready       → countdown card + challenge info + inline quiz (or completed banner)
 *  - Completed   → celebration state with ELO earned
 *  - Error       → error message with retry button
 *
 * DAILY-01, DAILY-02, DAILY-03, DAILY-04.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    onBackClick: () -> Unit,
    viewModel: DailyChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // ── Top bar ────────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = "Thử thách hôm nay 🎯",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = VietRed
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // ── State dispatch ─────────────────────────────────────────────────────
        when (val state = uiState) {

            is DailyChallengeUiState.Loading -> {
                LoadingContent()
            }

            is DailyChallengeUiState.NoChallenge -> {
                NoChallengeContent(onRetry = { viewModel.loadChallenge() })
            }

            is DailyChallengeUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadChallenge() }
                )
            }

            is DailyChallengeUiState.Ready -> {
                ReadyContent(
                    state = state,
                    onSubmit = { challengeDate ->
                        viewModel.submitCompletion(challengeDate)
                    }
                )
            }

            is DailyChallengeUiState.Completed -> {
                CompletedContent(
                    eloEarned = state.eloEarned,
                    streakUpdated = state.streakUpdated,
                    onBackClick = onBackClick
                )
            }
        }
    }
}

// ── Loading ────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }
    }
}

// ── No Challenge ───────────────────────────────────────────────────────────────

@Composable
private fun NoChallengeContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "😴", fontSize = 48.sp)
            Text(
                text = "Chưa có thử thách hôm nay",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Quay lại sau nhé!",
                fontSize = 14.sp,
                color = TextSecondary
            )
            TextButton(onClick = onRetry) {
                Text(text = "Thử lại", color = VietRed)
            }
        }
    }
}

// ── Error ──────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "❌", fontSize = 40.sp)
            Text(
                text = message,
                fontSize = 15.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = VietRed)
            ) {
                Text("Thử lại", color = Color.White)
            }
        }
    }
}

// ── Ready ──────────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    state: DailyChallengeUiState.Ready,
    onSubmit: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Countdown card (DAILY-02)
        item {
            CountdownCard(countdownSeconds = state.countdownSeconds)
        }

        // Challenge info card (DAILY-01)
        item {
            ChallengeInfoCard(challenge = state.challenge)
        }

        // Completed banner OR inline quiz (DAILY-03)
        if (state.isCompleted) {
            item {
                CompletedBanner(eloEarned = state.eloEarned)
            }
        } else {
            item {
                InlineQuizSection(
                    challenge = state.challenge,
                    onComplete = { onSubmit(state.challenge.challengeDate) }
                )
            }
        }

        // 7-day history (DAILY-04)
        item {
            HistorySection(history = state.history)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ── Completed (this session) ───────────────────────────────────────────────────

@Composable
private fun CompletedContent(
    eloEarned: Int,
    streakUpdated: Boolean,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "🎉", fontSize = 64.sp)
            Text(
                text = "Xuất sắc!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = VietRed
            )
            Text(
                text = "+$eloEarned ELO",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen
            )
            if (streakUpdated) {
                Text(
                    text = "Streak được cập nhật! 🔥",
                    fontSize = 16.sp,
                    color = PrimaryOrange
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(containerColor = VietRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Về trang chủ", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

/** DAILY-02: HH:MM:SS countdown until midnight UTC. */
@Composable
private fun CountdownCard(countdownSeconds: Long) {
    val hours = countdownSeconds / 3600
    val minutes = (countdownSeconds % 3600) / 60
    val seconds = countdownSeconds % 60
    val formattedTime = String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⏰ Còn lại:",
                fontSize = 15.sp,
                color = TextSecondary
            )
            Text(
                text = formattedTime,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VietRed
            )
        }
    }
}

/** DAILY-01: Displays game mode, vocabulary count, and bonus ELO. */
@Composable
private fun ChallengeInfoCard(challenge: DailyChallenge) {
    val gameModeDisplay = when (challenge.gameMode) {
        "image_to_word"   -> "Hình → Từ"
        "word_to_image"   -> "Từ → Hình"
        "syllable_match"  -> "Ghép âm tiết"
        "sentence_order"  -> "Sắp xếp câu"
        "fill_blank"      -> "Điền vào chỗ trống"
        "word_chain"      -> "Chuỗi từ"
        "word_search"     -> "Tìm từ"
        else              -> challenge.gameMode
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Thách đấu hôm nay 🎯",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = gameModeDisplay,
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${challenge.vocabularyIds.size} từ vựng",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                // Bonus ELO chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VietRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "+${challenge.bonusElo} ELO",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VietRed
                    )
                }
            }
        }
    }
}

/** Already-completed banner shown when the user finished the challenge before this session. */
@Composable
private fun CompletedBanner(eloEarned: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🏆", fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = "✅ Đã hoàn thành hôm nay!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "+$eloEarned ELO đã được cộng",
                    fontSize = 13.sp,
                    color = Color(0xFF388E3C)
                )
            }
        }
    }
}

/** DAILY-03: Inline 5-question multiple-choice quiz using challenge vocabulary IDs. */
@Composable
private fun InlineQuizSection(
    challenge: DailyChallenge,
    onComplete: () -> Unit
) {
    // Resolve VocabularyItem objects from static repository
    val quizWords = remember(challenge) {
        challenge.vocabularyIds.take(5).mapNotNull { id ->
            VocabularyRepository.allVocabulary.find { it.id == id }
        }
    }

    var quizIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            if (quizWords.isEmpty()) {
                // No matching vocabulary found — allow bypassing
                Text(
                    text = "Không tìm thấy từ vựng cho thử thách này.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = VietRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hoàn thành", color = Color.White)
                }
                return@Column
            }

            if (quizIndex < quizWords.size) {
                // Active question
                val word = quizWords[quizIndex]
                val options = remember(quizIndex) {
                    buildQuizOptions(word, quizWords)
                }

                Text(
                    text = "Câu ${quizIndex + 1}/${quizWords.size}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { quizIndex.toFloat() / quizWords.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = VietRed,
                    trackColor = VietRed.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "\"${word.word}\"",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chọn định nghĩa đúng:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                options.forEach { option ->
                    OutlinedButton(
                        onClick = {
                            if (option == word.word) correctCount++
                            quizIndex++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = option,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                // All questions answered — show result and complete button
                val score = correctCount.toFloat() / quizWords.size.toFloat()
                Text(
                    text = "Kết quả",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { score },
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryGreen,
                    trackColor = PrimaryGreen.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$correctCount/${quizWords.size} câu đúng",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (score >= 0.6f) PrimaryGreen else PrimaryOrange
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = VietRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hoàn thành +${50} ELO 🎯", color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * Builds 4 answer options: the correct answer + up to 3 distractors from the word,
 * shuffled deterministically based on [VocabularyItem.word].
 */
private fun buildQuizOptions(word: VocabularyItem, allQuizWords: List<VocabularyItem>): List<String> {
    val correct = word.word
    // Prefer distractors defined on the item; fall back to other quiz words' names
    val distractors = (word.distractors.take(3) +
        allQuizWords.filter { it.id != word.id }.map { it.word })
        .distinct()
        .filter { it != correct }
        .take(3)
    val options = (listOf(correct) + distractors).shuffled()
    return options
}

/** DAILY-04: 7-day history section. */
@Composable
private fun HistorySection(history: List<DailyChallengeHistoryItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "7 ngày qua",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (history.isEmpty()) {
                Text(
                    text = "Chưa có dữ liệu lịch sử.",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            } else {
                // Horizontal row of 7 circles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    history.forEach { item ->
                        HistoryDayCircle(item = item)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))
                // Detail rows
                history.forEach { item ->
                    HistoryRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun HistoryDayCircle(item: DailyChallengeHistoryItem) {
    val shortDate = item.challengeDate.takeLast(5).replace("-", "/") // MM/DD
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (item.isCompleted) PrimaryGreen else Color(0xFFEEEEEE),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (item.isCompleted) "✅" else "⬜",
                fontSize = 14.sp
            )
        }
        Text(text = shortDate, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun HistoryRow(item: DailyChallengeHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.challengeDate,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        if (item.isCompleted) {
            Text(
                text = "✅ +${item.eloEarned} ELO",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen
            )
        } else {
            Text(
                text = "—",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}
