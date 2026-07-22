package com.example.vietforces.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.AiCallResult
import com.example.vietforces.data.model.DifficultyMode
import com.example.vietforces.data.model.VocabularyItem
import com.example.vietforces.data.repository.ProgressRepository
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Game state for Image to Word game
 */
data class ImageToWordGameState(
    val currentWord: VocabularyItem? = null,
    val options: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 10,
    val eloChange: Int = 0,
    val showResult: Boolean = false,
    val userInput: String = "", // For hard mode
    val correctAnswer: String = "", // Keep the correct answer to avoid leaking it during transition
    val isTransitioning: Boolean = false, // Transitioning to the next question
    val isGrading: Boolean = false, // AI is grading an open answer (hard mode)
    val aiNote: String? = null // Short AI note when a near-correct answer is accepted
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToWordScreen(
    onBackClick: () -> Unit
) {
    var difficultyMode by remember { mutableStateOf<DifficultyMode?>(null) }
    var gameState by remember { mutableStateOf(ImageToWordGameState()) }
    var showGameOver by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Track session start time for postGame elapsed-time metric (ELO-01)
    val startTimeMs = remember { System.currentTimeMillis() }

    // Show difficulty selection first
    if (difficultyMode == null) {
        ImageToWordDifficultyScreen(
            onBackClick = onBackClick,
            onSelectDifficulty = { mode ->
                difficultyMode = mode
                gameState = startNewGame()
            }
        )
    } else if (showGameOver) {
        ImageToWordGameOverScreen(
            score = gameState.score,
            totalQuestions = gameState.totalQuestions,
            eloChange = gameState.eloChange,
            onPlayAgain = {
                showGameOver = false
                gameState = startNewGame()
            },
            onBackClick = onBackClick
        )
    } else {
        // Main game screen
        ImageToWordGameContent(
            gameState = gameState,
            difficultyMode = difficultyMode!!,
            onBackClick = onBackClick,
            onAnswerSelected = { answer ->
                val correctWord = gameState.currentWord?.word ?: ""
                val difficulty = gameState.currentWord?.difficulty ?: 1
                val wordId = gameState.currentWord?.id
                val exact = answer.trim().equals(correctWord, ignoreCase = true)

                // Apply a final verdict (used directly for exact/easy, or after AI grading).
                fun applyResult(isCorrect: Boolean, aiNote: String?) {
                    val eloChange = if (isCorrect) {
                        UserProgressManager.recordCorrectAnswer(difficulty)
                    } else {
                        UserProgressManager.recordWrongAnswer(difficulty)
                    }

                    wordId?.let {
                        EncounteredItemsManager.recordEncounter(GameMode.IMAGE_TO_WORD, it, isCorrect)
                    }

                    if (isCorrect) {
                        MascotFeedbackManager.showCorrectFeedback(
                            "Bài nhìn hình đoán từ. Từ đúng: \"$correctWord\", người học nhập: \"$answer\"."
                        )
                    } else {
                        MascotFeedbackManager.showWrongFeedback(
                            "Bài nhìn hình đoán từ. Từ đúng: \"$correctWord\", người học trả lời: \"$answer\"."
                        )
                    }

                    gameState = gameState.copy(
                        selectedAnswer = answer,
                        isCorrect = isCorrect,
                        score = if (isCorrect) gameState.score + 1 else gameState.score,
                        eloChange = gameState.eloChange + eloChange,
                        showResult = true,
                        correctAnswer = correctWord,
                        isGrading = false,
                        aiNote = aiNote
                    )
                }

                // Exact match, easy (multiple choice) or AI off → grade locally (§10.1).
                if (exact || difficultyMode != DifficultyMode.HARD || !AiManager.isAvailable()) {
                    applyResult(exact, null)
                } else {
                    // Hard mode + typed answer differs from key → ask AI if it's
                    // acceptable in meaning (near-correct / missing tones) (§6.1/6.3).
                    gameState = gameState.copy(isGrading = true)
                    scope.launch {
                        when (val r = AiManager.gradeOpenAnswer(
                            question = "Từ tiếng Việt đúng cho hình ảnh là gì?",
                            expectedAnswer = correctWord,
                            userAnswer = answer
                        )) {
                            is AiCallResult.Success ->
                                applyResult(r.data.isAcceptable, r.data.feedback.ifBlank { null })
                            is AiCallResult.Error ->
                                applyResult(false, null) // fallback: strict local result
                        }
                    }
                }
            },
            onNextQuestion = {
                if (gameState.questionNumber >= gameState.totalQuestions) {
                    // Record game mode stats
                    UserProgressManager.recordGameModeResult(
                        gameModeId = "image_to_word",
                        correct = gameState.score,
                        wrong = gameState.totalQuestions - gameState.score,
                        score = gameState.score
                    )
                    // Post-game server sync: ELO + streak update (ELO-01, STREAK-01)
                    val elapsedMs = System.currentTimeMillis() - startTimeMs
                    scope.launch {
                        ProgressRepository.instance?.postGame(
                            correct = gameState.score,
                            total = gameState.totalQuestions,
                            timeMs = elapsedMs
                        )
                    }
                    showGameOver = true
                } else {
                    gameState = nextQuestion(gameState)
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
private fun ImageToWordGameContent(
    gameState: ImageToWordGameState,
    difficultyMode: DifficultyMode,
    onBackClick: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onUserInputChange: (String) -> Unit
) {
    // State to control showing the result
    var showResultFeedback by remember { mutableStateOf(false) }
    var resultIsCorrect by remember { mutableStateOf(false) }
    var resultCorrectAnswer by remember { mutableStateOf("") }

    // When a new result arrives, show feedback
    LaunchedEffect(gameState.showResult) {
        if (gameState.showResult && gameState.isCorrect != null) {
            // Store the result and show feedback
            resultIsCorrect = gameState.isCorrect
            resultCorrectAnswer = gameState.correctAnswer
            showResultFeedback = true

            // Wait 1.5s
            delay(1500)

            // Hide feedback first
            showResultFeedback = false

            // Wait for the exit animation to finish (300ms fadeOut)
            delay(300)

            // Then load the next question
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
                    text = "Nhìn hình đoán từ",
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
                // Progress indicator
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
            color = VietRed,
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

            // Image card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    gameState.currentWord?.let { word ->
                        Image(
                            painter = painterResource(id = word.imageResId),
                            contentDescription = word.word,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question text
            Text(
                text = "Đây là gì?",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Answer section
            if (difficultyMode == DifficultyMode.EASY) {
                // Multiple choice options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gameState.options.forEach { option ->
                        val isOptionCorrect = if (showResultFeedback) {
                            option == resultCorrectAnswer
                        } else null

                        AnswerOptionButton(
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
                OutlinedTextField(
                    value = gameState.userInput,
                    onValueChange = onUserInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nhập từ tiếng Việt") },
                    singleLine = true,
                    enabled = !showResultFeedback && !gameState.showResult,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VietRed,
                        focusedLabelColor = VietRed
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onAnswerSelected(gameState.userInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gameState.userInput.isNotBlank() && !showResultFeedback &&
                        !gameState.showResult && !gameState.isGrading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VietRed
                    )
                ) {
                    if (gameState.isGrading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI đang chấm...")
                    } else {
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (resultIsCorrect) PrimaryGreen.copy(alpha = 0.1f) else VietRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (resultIsCorrect) "🎉 Chính xác!" else "❌ Sai rồi! Đáp án: $resultCorrectAnswer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (resultIsCorrect) PrimaryGreen else VietRed
                        )
                        // AI note when a near-correct answer was accepted (§6.3)
                        gameState.aiNote?.let { note ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🤖 $note",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerOptionButton(
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
        isSelected -> VietYellow.copy(alpha = 0.3f)
        else -> Color.White
    }

    val borderColor = when {
        isCorrect == true -> PrimaryGreen
        isWrong == true -> VietRed
        isSelected -> VietYellow
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
 * Start a new game with weighted vocabulary selection (less seen items more likely)
 */
private fun startNewGame(): ImageToWordGameState {
    val allIds = VocabularyRepository.allVocabulary.map { it.id }
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.IMAGE_TO_WORD,
        allItemIds = allIds
    )
    val firstWord = VocabularyRepository.allVocabulary.find { it.id == selectedId }
        ?: VocabularyRepository.allVocabulary.firstOrNull()

    return ImageToWordGameState(
        currentWord = firstWord,
        options = generateOptions(firstWord),
        questionNumber = 1,
        totalQuestions = minOf(10, VocabularyRepository.allVocabulary.size),
        correctAnswer = firstWord?.word ?: ""
    )
}

/**
 * Move to next question with weighted selection
 */
private fun nextQuestion(currentState: ImageToWordGameState): ImageToWordGameState {
    val allIds = VocabularyRepository.allVocabulary.map { it.id }
    val excludeIds = setOfNotNull(currentState.currentWord?.id)
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.IMAGE_TO_WORD,
        allItemIds = allIds,
        excludeIds = excludeIds
    )
    val nextWord = VocabularyRepository.allVocabulary.find { it.id == selectedId }
        ?: VocabularyRepository.allVocabulary.firstOrNull { it.id !in excludeIds }

    return currentState.copy(
        currentWord = nextWord,
        options = generateOptions(nextWord),
        selectedAnswer = null,
        isCorrect = null,
        questionNumber = currentState.questionNumber + 1,
        showResult = false,
        userInput = "",
        correctAnswer = nextWord?.word ?: "" // Set correctAnswer as soon as a new question is created
    )
}

/**
 * Generate multiple choice options including correct answer and distractors
 */
private fun generateOptions(word: VocabularyItem?): List<String> {
    if (word == null) return emptyList()

    val options = mutableListOf(word.word)
    options.addAll(word.distractors.take(3))

    // If not enough distractors, add random words
    if (options.size < 4) {
        val otherWords = VocabularyRepository.allVocabulary
            .filter { it.id != word.id }
            .map { it.word }
            .shuffled()
            .take(4 - options.size)
        options.addAll(otherWords)
    }

    return options.shuffled()
}

/**
 * Difficulty selection screen for Image to Word game
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageToWordDifficultyScreen(
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
                    text = "Nhìn hình đoán từ",
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

/**
 * Game over screen for Image to Word game
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageToWordGameOverScreen(
    score: Int,
    totalQuestions: Int,
    eloChange: Int,
    onPlayAgain: () -> Unit,
    onBackClick: () -> Unit
) {
    val percentage = (score.toFloat() / totalQuestions * 100).toInt()
    val message = when {
        percentage >= 90 -> "🏆 Xuất sắc!"
        percentage >= 70 -> "🎉 Giỏi lắm!"
        percentage >= 50 -> "👍 Khá tốt!"
        else -> "💪 Cố gắng thêm nhé!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Kết quả",
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
                text = message,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score / $totalQuestions",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = VietRed
                    )
                    Text(
                        text = "Câu trả lời đúng",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$percentage%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "Độ chính xác",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${if (eloChange >= 0) "+" else ""}$eloChange",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (eloChange >= 0) PrimaryGreen else VietRed
                            )
                            Text(
                                text = "Elo",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VietRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Chơi lại",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VietRed)
            ) {
                Text(
                    text = "Về trang chủ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
