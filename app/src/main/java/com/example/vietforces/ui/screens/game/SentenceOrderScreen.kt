package com.example.vietforces.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.SentenceItem
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay


/**
 * Game state for Sentence Order game
 */
data class SentenceOrderGameState(
    val currentSentence: SentenceItem? = null,
    val shuffledWords: List<String> = emptyList(),
    val selectedWords: List<String> = emptyList(),
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 10,
    val eloChange: Int = 0,
    val showResult: Boolean = false,
    val correctSentence: String = "" // Lưu câu đúng để tránh flicker khi chuyển câu
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceOrderScreen(
    onBackClick: () -> Unit
) {
    var gameState by remember { mutableStateOf(startSentenceOrderGame()) }
    var showGameOver by remember { mutableStateOf(false) }

    if (showGameOver) {
        GameOverScreen(
            score = gameState.score,
            totalQuestions = gameState.totalQuestions,
            eloChange = gameState.eloChange,
            onPlayAgain = {
                showGameOver = false
                gameState = startSentenceOrderGame()
            },
            onBackClick = onBackClick
        )
    } else {
        SentenceOrderGameContent(
            gameState = gameState,
            onBackClick = onBackClick,
            onWordSelected = { word ->
                // Add word to selected list and remove from shuffled
                val newSelected = gameState.selectedWords + word
                val newShuffled = gameState.shuffledWords.toMutableList().apply {
                    remove(word)
                }
                gameState = gameState.copy(
                    selectedWords = newSelected,
                    shuffledWords = newShuffled
                )
            },
            onWordRemoved = { word ->
                // Remove word from selected list and add back to shuffled
                val newSelected = gameState.selectedWords.toMutableList().apply {
                    remove(word)
                }
                val newShuffled = gameState.shuffledWords + word
                gameState = gameState.copy(
                    selectedWords = newSelected,
                    shuffledWords = newShuffled
                )
            },
            onCheckAnswer = {
                val correctAnswer = gameState.currentSentence?.words ?: emptyList()
                val isCorrect = gameState.selectedWords == correctAnswer
                val eloChange = if (isCorrect) {
                    UserProgressManager.recordCorrectAnswer(gameState.currentSentence?.difficulty ?: 1)
                } else {
                    UserProgressManager.recordWrongAnswer(gameState.currentSentence?.difficulty ?: 1)
                }

                // Record encounter for spaced repetition
                gameState.currentSentence?.let { sentence ->
                    EncounteredItemsManager.recordEncounter(
                        gameMode = GameMode.SENTENCE_ORDER,
                        itemId = sentence.id,
                        wasCorrect = isCorrect
                    )
                }

                // Show mascot feedback
                if (isCorrect) {
                    MascotFeedbackManager.showCorrectFeedback()
                } else {
                    MascotFeedbackManager.showWrongFeedback()
                }

                gameState = gameState.copy(
                    isCorrect = isCorrect,
                    score = if (isCorrect) gameState.score + 1 else gameState.score,
                    eloChange = gameState.eloChange + eloChange,
                    showResult = true
                )
            },
            onNextQuestion = {
                if (gameState.questionNumber >= gameState.totalQuestions) {
                    // Record game mode stats
                    UserProgressManager.recordGameModeResult(
                        gameModeId = "sentence_order",
                        correct = gameState.score,
                        wrong = gameState.totalQuestions - gameState.score,
                        score = gameState.score
                    )
                    showGameOver = true
                } else {
                    gameState = nextSentenceOrderQuestion(gameState)
                }
            },
            onClearSelection = {
                gameState = gameState.copy(
                    selectedWords = emptyList(),
                    shuffledWords = gameState.currentSentence?.words?.shuffled() ?: emptyList()
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SentenceOrderGameContent(
    gameState: SentenceOrderGameState,
    onBackClick: () -> Unit,
    onWordSelected: (String) -> Unit,
    onWordRemoved: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onClearSelection: () -> Unit
) {
    // State để kiểm soát hiển thị result
    var showResultFeedback by remember { mutableStateOf(false) }
    var resultIsCorrect by remember { mutableStateOf(false) }
    var resultCorrectSentence by remember { mutableStateOf("") }

    // Khi có kết quả mới, hiển thị feedback
    LaunchedEffect(gameState.showResult) {
        if (gameState.showResult && gameState.isCorrect != null) {
            // Lưu kết quả và hiển thị feedback
            resultIsCorrect = gameState.isCorrect
            resultCorrectSentence = gameState.correctSentence
            showResultFeedback = true

            // Chờ 2s
            delay(2000)

            // Tắt feedback trước
            showResultFeedback = false

            // Chờ animation tắt hoàn toàn (300ms cho fadeOut)
            delay(300)

            // Sau đó mới load câu hỏi mới
            onNextQuestion()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Xếp câu",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                Text(
                    text = "${gameState.questionNumber}/${gameState.totalQuestions}",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Progress bar
        LinearProgressIndicator(
            progress = { gameState.questionNumber.toFloat() / gameState.totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = GameModeSentence,
            trackColor = Color(0xFFE0E0E0)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Score and Elo change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Điểm: ${gameState.score}",
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Elo: ${if (gameState.eloChange >= 0) "+" else ""}${gameState.eloChange}",
                    fontWeight = FontWeight.Medium,
                    color = if (gameState.eloChange >= 0) PrimaryGreen else VietRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translation hint
            gameState.currentSentence?.translation?.let { translation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GameModeSentence.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🇬🇧 $translation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sắp xếp các từ thành câu tiếng Việt",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selected words area (answer)
            Text(
                text = "Câu trả lời của bạn:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Answer area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        gameState.isCorrect == true -> PrimaryGreen.copy(alpha = 0.1f)
                        gameState.isCorrect == false -> VietRed.copy(alpha = 0.1f)
                        else -> Color.White
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = when {
                        showResultFeedback && resultIsCorrect -> PrimaryGreen
                        showResultFeedback && !resultIsCorrect -> VietRed
                        else -> Color(0xFFE0E0E0)
                    }
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameState.selectedWords.isEmpty()) {
                        Text(
                            text = "Nhấn vào các từ bên dưới để xếp câu",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gameState.selectedWords.forEach { word ->
                                WordChip(
                                    word = word,
                                    isSelected = true,
                                    enabled = !showResultFeedback && !gameState.showResult,
                                    onClick = { onWordRemoved(word) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Available words
            Text(
                text = "Các từ có sẵn:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Word chips
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameState.shuffledWords.isEmpty() && gameState.selectedWords.isNotEmpty()) {
                        Text(
                            text = "Tất cả từ đã được chọn!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            gameState.shuffledWords.forEach { word ->
                                WordChip(
                                    word = word,
                                    isSelected = false,
                                    enabled = !showResultFeedback && !gameState.showResult,
                                    onClick = { onWordSelected(word) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            if (!showResultFeedback && !gameState.showResult) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clear button
                    OutlinedButton(
                        onClick = onClearSelection,
                        modifier = Modifier.weight(1f),
                        enabled = gameState.selectedWords.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa")
                    }

                    // Check button
                    Button(
                        onClick = onCheckAnswer,
                        modifier = Modifier.weight(2f),
                        enabled = gameState.shuffledWords.isEmpty() && gameState.selectedWords.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = GameModeSentence)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Check",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kiểm tra")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result feedback
            AnimatedVisibility(
                visible = showResultFeedback,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (resultIsCorrect) PrimaryGreen.copy(alpha = 0.1f) else VietRed.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (resultIsCorrect) "🎉 Chính xác!" else "❌ Sai rồi!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (resultIsCorrect) PrimaryGreen else VietRed
                            )
                        }
                    }

                    if (!resultIsCorrect) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Đáp án đúng: $resultCorrectSentence",
                            fontSize = 14.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordChip(
    word: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) GameModeSentence else Color.White,
        shadowElevation = if (isSelected) 0.dp else 2.dp,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp,
            GameModeSentence
        ) else null
    ) {
        Text(
            text = word,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else GameModeSentence
        )
    }
}

/**
 * Start a new Sentence Order game with weighted selection
 */
private fun startSentenceOrderGame(): SentenceOrderGameState {
    val allIds = VocabularyRepository.allSentences.map { it.id }
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.SENTENCE_ORDER,
        allItemIds = allIds
    )
    val firstSentence = VocabularyRepository.allSentences.find { it.id == selectedId }
        ?: VocabularyRepository.allSentences.firstOrNull()

    return SentenceOrderGameState(
        currentSentence = firstSentence,
        shuffledWords = firstSentence?.words?.shuffled() ?: emptyList(),
        questionNumber = 1,
        totalQuestions = minOf(10, VocabularyRepository.allSentences.size),
        correctSentence = firstSentence?.fullSentence ?: ""
    )
}

/**
 * Move to next question with weighted selection
 */
private fun nextSentenceOrderQuestion(currentState: SentenceOrderGameState): SentenceOrderGameState {
    val allIds = VocabularyRepository.allSentences.map { it.id }
    val excludeIds = setOfNotNull(currentState.currentSentence?.id)
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.SENTENCE_ORDER,
        allItemIds = allIds,
        excludeIds = excludeIds
    )
    val nextSentence = VocabularyRepository.allSentences.find { it.id == selectedId }
        ?: VocabularyRepository.allSentences.firstOrNull { it.id !in excludeIds }

    return currentState.copy(
        currentSentence = nextSentence,
        shuffledWords = nextSentence?.words?.shuffled() ?: emptyList(),
        selectedWords = emptyList(),
        isCorrect = null,
        questionNumber = currentState.questionNumber + 1,
        showResult = false,
        correctSentence = nextSentence?.fullSentence ?: ""
    )
}

