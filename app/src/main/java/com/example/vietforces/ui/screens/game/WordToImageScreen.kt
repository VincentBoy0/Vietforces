package com.example.vietforces.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.VocabularyItem
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay


/**
 * Game state for Word to Image game
 */
data class WordToImageGameState(
    val currentWord: VocabularyItem? = null,
    val imageOptions: List<VocabularyItem> = emptyList(),
    val selectedImageId: String? = null,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val questionNumber: Int = 0,
    val totalQuestions: Int = 10,
    val eloChange: Int = 0,
    val showResult: Boolean = false,
    val correctWordId: String = "" // Lưu ID đáp án đúng để tránh leak khi chuyển câu
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordToImageScreen(
    onBackClick: () -> Unit
) {
    var gameState by remember { mutableStateOf(startWordToImageGame()) }
    var showGameOver by remember { mutableStateOf(false) }

    if (showGameOver) {
        GameOverScreen(
            score = gameState.score,
            totalQuestions = gameState.totalQuestions,
            eloChange = gameState.eloChange,
            onPlayAgain = {
                showGameOver = false
                gameState = startWordToImageGame()
            },
            onBackClick = onBackClick
        )
    } else {
        WordToImageGameContent(
            gameState = gameState,
            onBackClick = onBackClick,
            onImageSelected = { selectedWord ->
                val correctId = gameState.currentWord?.id ?: ""
                val isCorrect = selectedWord.id == correctId
                val eloChange = if (isCorrect) {
                    UserProgressManager.recordCorrectAnswer(gameState.currentWord?.difficulty ?: 1)
                } else {
                    UserProgressManager.recordWrongAnswer(gameState.currentWord?.difficulty ?: 1)
                }

                // Record encounter for spaced repetition
                gameState.currentWord?.let { word ->
                    EncounteredItemsManager.recordEncounter(
                        gameMode = GameMode.WORD_TO_IMAGE,
                        itemId = word.id,
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
                    selectedImageId = selectedWord.id,
                    isCorrect = isCorrect,
                    score = if (isCorrect) gameState.score + 1 else gameState.score,
                    eloChange = gameState.eloChange + eloChange,
                    showResult = true,
                    correctWordId = correctId
                )
            },
            onNextQuestion = {
                if (gameState.questionNumber >= gameState.totalQuestions) {
                    // Record game mode stats
                    UserProgressManager.recordGameModeResult(
                        gameModeId = "word_to_image",
                        correct = gameState.score,
                        wrong = gameState.totalQuestions - gameState.score,
                        score = gameState.score
                    )
                    showGameOver = true
                } else {
                    gameState = nextWordToImageQuestion(gameState)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordToImageGameContent(
    gameState: WordToImageGameState,
    onBackClick: () -> Unit,
    onImageSelected: (VocabularyItem) -> Unit,
    onNextQuestion: () -> Unit
) {
    // State để kiểm soát hiển thị result
    var showResultFeedback by remember { mutableStateOf(false) }
    var resultIsCorrect by remember { mutableStateOf(false) }
    var resultCorrectWordId by remember { mutableStateOf("") }

    // Khi có kết quả mới, hiển thị feedback
    LaunchedEffect(gameState.showResult) {
        if (gameState.showResult && gameState.isCorrect != null) {
            // Lưu kết quả và hiển thị feedback
            resultIsCorrect = gameState.isCorrect
            resultCorrectWordId = gameState.correctWordId
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
                    text = "Nhìn từ đoán hình",
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
            color = PrimaryBlue,
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

            // Word display card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    gameState.currentWord?.let { word ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = word.fullWord,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            word.pronunciation?.let { pronunciation ->
                                Text(
                                    text = "/$pronunciation/",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Question text
            Text(
                text = "Chọn hình ảnh đúng",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image options grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(gameState.imageOptions) { option ->
                    val isOptionCorrect = if (showResultFeedback) {
                        option.id == resultCorrectWordId
                    } else null

                    ImageOptionCard(
                        vocabularyItem = option,
                        isSelected = gameState.selectedImageId == option.id,
                        isCorrect = isOptionCorrect,
                        isWrong = if (showResultFeedback && gameState.selectedImageId == option.id)
                            !resultIsCorrect else null,
                        enabled = !showResultFeedback && !gameState.showResult,
                        onClick = { onImageSelected(option) }
                    )
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
            }
        }
    }
}

@Composable
private fun ImageOptionCard(
    vocabularyItem: VocabularyItem,
    isSelected: Boolean,
    isCorrect: Boolean?,
    isWrong: Boolean?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCorrect == true -> PrimaryGreen
        isWrong == true -> VietRed
        isSelected -> PrimaryBlue
        else -> Color.Transparent
    }

    val borderWidth = when {
        isCorrect == true || isWrong == true || isSelected -> 3.dp
        else -> 0.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { if (enabled) onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = vocabularyItem.imageResId),
                contentDescription = vocabularyItem.word,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )

            // Overlay icons for correct/wrong
            when {
                isCorrect == true -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PrimaryGreen.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Correct",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                isWrong == true -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VietRed.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Wrong",
                            tint = VietRed,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Start a new Word to Image game with weighted selection
 */
private fun startWordToImageGame(): WordToImageGameState {
    val allIds = VocabularyRepository.allVocabulary.map { it.id }
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.WORD_TO_IMAGE,
        allItemIds = allIds
    )
    val firstWord = VocabularyRepository.allVocabulary.find { it.id == selectedId }
        ?: VocabularyRepository.allVocabulary.firstOrNull()

    return WordToImageGameState(
        currentWord = firstWord,
        imageOptions = generateImageOptions(firstWord, VocabularyRepository.allVocabulary),
        questionNumber = 1,
        totalQuestions = minOf(10, VocabularyRepository.allVocabulary.size),
        correctWordId = firstWord?.id ?: ""
    )
}

/**
 * Move to next question with weighted selection
 */
private fun nextWordToImageQuestion(currentState: WordToImageGameState): WordToImageGameState {
    val allIds = VocabularyRepository.allVocabulary.map { it.id }
    val excludeIds = setOfNotNull(currentState.currentWord?.id)
    val selectedId = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.WORD_TO_IMAGE,
        allItemIds = allIds,
        excludeIds = excludeIds
    )
    val nextWord = VocabularyRepository.allVocabulary.find { it.id == selectedId }
        ?: VocabularyRepository.allVocabulary.firstOrNull { it.id !in excludeIds }

    return currentState.copy(
        currentWord = nextWord,
        imageOptions = generateImageOptions(nextWord, VocabularyRepository.allVocabulary),
        selectedImageId = null,
        isCorrect = null,
        questionNumber = currentState.questionNumber + 1,
        showResult = false,
        correctWordId = nextWord?.id ?: "" // Set correctWordId ngay khi tạo câu mới
    )
}

/**
 * Generate image options (correct + 3 distractors)
 */
private fun generateImageOptions(
    correctWord: VocabularyItem?,
    allVocabulary: List<VocabularyItem>
): List<VocabularyItem> {
    if (correctWord == null) return emptyList()

    val options = mutableListOf(correctWord)

    // Add random distractors
    val distractors = allVocabulary
        .filter { it.id != correctWord.id }
        .shuffled()
        .take(3)

    options.addAll(distractors)

    return options.shuffled()
}

