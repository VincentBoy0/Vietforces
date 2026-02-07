package com.example.vietforces.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.DifficultyMode
import com.example.vietforces.data.model.SentenceItem
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay


/**
 * Game state for Fill Blank game
 */
data class FillBlankGameState(
    val currentSentence: SentenceItem? = null,
    val options: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val userInput: String = "",
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 10,
    val eloChange: Int = 0,
    val showResult: Boolean = false,
    val correctAnswer: String = "" // Lưu đáp án đúng để tránh flicker khi chuyển câu
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillBlankScreen(
    onBackClick: () -> Unit
) {
    var difficultyMode by remember { mutableStateOf<DifficultyMode?>(null) }
    var gameState by remember { mutableStateOf(FillBlankGameState()) }
    var showGameOver by remember { mutableStateOf(false) }

    // Show difficulty selection first
    if (difficultyMode == null) {
        FillBlankDifficultyScreen(
            onBackClick = onBackClick,
            onSelectDifficulty = { mode ->
                difficultyMode = mode
                gameState = startFillBlankGame()
            }
        )
    } else if (showGameOver) {
        GameOverScreen(
            score = gameState.score,
            totalQuestions = gameState.totalQuestions,
            eloChange = gameState.eloChange,
            onPlayAgain = {
                showGameOver = false
                gameState = startFillBlankGame()
            },
            onBackClick = onBackClick
        )
    } else {
        FillBlankGameContent(
            gameState = gameState,
            difficultyMode = difficultyMode!!,
            onBackClick = onBackClick,
            onAnswerSelected = { answer ->
                val correctAnswer = gameState.currentSentence?.blankWord ?: ""
                val isCorrect = answer.equals(correctAnswer, ignoreCase = true)
                val eloChange = if (isCorrect) {
                    UserProgressManager.recordCorrectAnswer(gameState.currentSentence?.difficulty ?: 1)
                } else {
                    UserProgressManager.recordWrongAnswer(gameState.currentSentence?.difficulty ?: 1)
                }

                // Record encounter for spaced repetition
                gameState.currentSentence?.let { sentence ->
                    EncounteredItemsManager.recordEncounter(
                        gameMode = GameMode.FILL_BLANK,
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
                    selectedAnswer = answer,
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
                        gameModeId = "fill_blank",
                        correct = gameState.score,
                        wrong = gameState.totalQuestions - gameState.score,
                        score = gameState.score
                    )
                    showGameOver = true
                } else {
                    gameState = nextFillBlankQuestion(gameState)
                }
            },
            onUserInputChange = { input ->
                gameState = gameState.copy(userInput = input)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FillBlankDifficultyScreen(
    onBackClick: () -> Unit,
    onSelectDifficulty: (DifficultyMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Điền từ vào chỗ trống",
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chọn độ khó",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🐓 Gà Vàng khuyên bạn bắt đầu với chế độ Dễ!",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Easy mode button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = { onSelectDifficulty(DifficultyMode.EASY) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌱", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "🌱 Dễ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                        Text(
                            text = "Chọn từ đáp án có sẵn",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hard mode button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = { onSelectDifficulty(DifficultyMode.HARD) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(VietRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔥", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "🔥 Khó",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VietRed
                        )
                        Text(
                            text = "Tự điền từ tiếng Việt",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FillBlankGameContent(
    gameState: FillBlankGameState,
    difficultyMode: DifficultyMode,
    onBackClick: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onUserInputChange: (String) -> Unit
) {
    // State để kiểm soát hiển thị result
    var showResultFeedback by remember { mutableStateOf(false) }
    var resultIsCorrect by remember { mutableStateOf(false) }
    var resultCorrectAnswer by remember { mutableStateOf("") }

    // Khi có kết quả mới, hiển thị feedback
    LaunchedEffect(gameState.showResult) {
        if (gameState.showResult && gameState.isCorrect != null) {
            // Lưu kết quả và hiển thị feedback
            resultIsCorrect = gameState.isCorrect
            resultCorrectAnswer = gameState.correctAnswer
            showResultFeedback = true

            // Chờ 1.5s
            delay(1500)

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
                    text = "Điền từ vào chỗ trống",
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
            color = GameModeFillBlank,
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

            Spacer(modifier = Modifier.height(24.dp))

            // Translation hint
            gameState.currentSentence?.translation?.let { translation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sentence with blank
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    gameState.currentSentence?.let { sentence ->
                        val annotatedString = buildAnnotatedString {
                            sentence.words.forEachIndexed { index, word ->
                                if (index == sentence.blankWordIndex) {
                                    withStyle(
                                        SpanStyle(
                                            color = when {
                                                gameState.isCorrect == true -> PrimaryGreen
                                                gameState.isCorrect == false -> VietRed
                                                else -> GameModeFillBlank
                                            },
                                            fontWeight = FontWeight.Bold,
                                            background = when {
                                                gameState.isCorrect == true -> PrimaryGreen.copy(alpha = 0.1f)
                                                gameState.isCorrect == false -> VietRed.copy(alpha = 0.1f)
                                                else -> GameModeFillBlank.copy(alpha = 0.1f)
                                            }
                                        )
                                    ) {
                                        if (gameState.showResult) {
                                            append(" ${sentence.blankWord} ")
                                        } else {
                                            append(" ______ ")
                                        }
                                    }
                                } else {
                                    append(word)
                                }
                                if (index < sentence.words.size - 1) {
                                    append(" ")
                                }
                            }
                        }

                        Text(
                            text = annotatedString,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Answer section
            if (difficultyMode == DifficultyMode.EASY) {
                // Multiple choice options
                Text(
                    text = "Chọn từ đúng:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gameState.options.forEach { option ->
                        val isOptionCorrect = if (showResultFeedback) {
                            option == resultCorrectAnswer
                        } else null

                        FillBlankOptionButton(
                            text = option,
                            isSelected = gameState.selectedAnswer == option,
                            isCorrect = isOptionCorrect,
                            isWrong = if (showResultFeedback && gameState.selectedAnswer == option)
                                !resultIsCorrect else null,
                            enabled = !showResultFeedback && !gameState.showResult,
                            onClick = { onAnswerSelected(option) }
                        )
                    }
                }
            } else {
                // Hard mode - text input
                Text(
                    text = "Điền từ còn thiếu:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gameState.userInput,
                    onValueChange = onUserInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập từ tiếng Việt") },
                    singleLine = true,
                    enabled = !showResultFeedback && !gameState.showResult,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GameModeFillBlank,
                        focusedLabelColor = GameModeFillBlank,
                        cursorColor = GameModeFillBlank
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onAnswerSelected(gameState.userInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gameState.userInput.isNotBlank() && !showResultFeedback && !gameState.showResult,
                    colors = ButtonDefaults.buttonColors(containerColor = GameModeFillBlank),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Check",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kiểm tra", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result feedback
            AnimatedVisibility(
                visible = showResultFeedback,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
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
                            text = if (resultIsCorrect) "🎉 Chính xác!" else "❌ Sai rồi! Đáp án: $resultCorrectAnswer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (resultIsCorrect) PrimaryGreen else VietRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FillBlankOptionButton(
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isWrong: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCorrect == true -> PrimaryGreen.copy(alpha = 0.2f)
        isWrong == true -> VietRed.copy(alpha = 0.2f)
        isSelected -> GameModeFillBlank.copy(alpha = 0.2f)
        else -> Color.White
    }

    val borderColor = when {
        isCorrect == true -> PrimaryGreen
        isWrong == true -> VietRed
        isSelected -> GameModeFillBlank
        else -> Color(0xFFE0E0E0)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            when {
                isCorrect == true -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Correct",
                    tint = PrimaryGreen
                )
                isWrong == true -> Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Wrong",
                    tint = VietRed
                )
            }
        }
    }
}

/**
 * Start a new Fill Blank game with weighted selection
 */
private fun startFillBlankGame(): FillBlankGameState {
    val allIds = VocabularyRepository.allSentences.map { it.id }
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.FILL_BLANK,
        allItemIds = allIds
    )
    val firstSentence = VocabularyRepository.allSentences.find { it.id == selectedId }
        ?: VocabularyRepository.allSentences.firstOrNull()

    return FillBlankGameState(
        currentSentence = firstSentence,
        options = generateFillBlankOptions(firstSentence),
        questionNumber = 1,
        totalQuestions = minOf(10, VocabularyRepository.allSentences.size),
        correctAnswer = firstSentence?.blankWord ?: ""
    )
}

/**
 * Move to next question with weighted selection
 */
private fun nextFillBlankQuestion(currentState: FillBlankGameState): FillBlankGameState {
    val allIds = VocabularyRepository.allSentences.map { it.id }
    val excludeIds = setOfNotNull(currentState.currentSentence?.id)
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.FILL_BLANK,
        allItemIds = allIds,
        excludeIds = excludeIds
    )
    val nextSentence = VocabularyRepository.allSentences.find { it.id == selectedId }
        ?: VocabularyRepository.allSentences.firstOrNull { it.id !in excludeIds }

    return currentState.copy(
        currentSentence = nextSentence,
        options = generateFillBlankOptions(nextSentence),
        selectedAnswer = null,
        userInput = "",
        isCorrect = null,
        questionNumber = currentState.questionNumber + 1,
        showResult = false,
        correctAnswer = nextSentence?.blankWord ?: ""
    )
}

/**
 * Generate multiple choice options for fill blank
 */
private fun generateFillBlankOptions(sentence: SentenceItem?): List<String> {
    if (sentence == null) return emptyList()

    val correctAnswer = sentence.blankWord
    val options = mutableListOf(correctAnswer)

    // Get other words from vocabulary as distractors
    val allWords = VocabularyRepository.allVocabulary.map { it.word }
    val distractors = allWords
        .filter { it != correctAnswer }
        .shuffled()
        .take(3)

    options.addAll(distractors)

    return options.shuffled()
}

