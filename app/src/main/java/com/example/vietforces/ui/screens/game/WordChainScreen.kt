package com.example.vietforces.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Who starts first in Word Chain game
 */
enum class WordChainStarter {
    PLAYER,
    COMPUTER
}

/**
 * Represents a word entry in the chain
 */
data class WordChainEntry(
    val word: String,
    val isPlayer: Boolean,
    val isValid: Boolean = true
)

/**
 * Game state for Word Chain game
 */
data class WordChainGameState(
    val wordChain: List<WordChainEntry> = emptyList(),
    val usedWords: Set<String> = emptySet(),
    val availableWords: Set<String> = emptySet(),
    val currentInput: String = "",
    val isPlayerTurn: Boolean = true,
    val isGameOver: Boolean = false,
    val winner: String? = null, // "player" or "computer"
    val errorMessage: String? = null,
    val playerScore: Int = 0,
    val computerScore: Int = 0,
    val eloChange: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordChainScreen(
    onBackClick: () -> Unit
) {
    var starter by remember { mutableStateOf<WordChainStarter?>(null) }
    var gameState by remember { mutableStateOf(WordChainGameState()) }

    if (starter == null) {
        // Show starter selection screen
        WordChainStarterScreen(
            onBackClick = onBackClick,
            onSelectStarter = { selectedStarter ->
                starter = selectedStarter
                gameState = startWordChainGame(selectedStarter)
            }
        )
    } else if (gameState.isGameOver) {
        // Show game over screen
        WordChainGameOverScreen(
            gameState = gameState,
            onPlayAgain = {
                gameState = startWordChainGame(starter!!)
            },
            onBackClick = onBackClick
        )
    } else {
        // Main game
        WordChainGameContent(
            gameState = gameState,
            onBackClick = onBackClick,
            onInputChange = { input ->
                gameState = gameState.copy(currentInput = input, errorMessage = null)
            },
            onSubmitWord = {
                val result = processPlayerWord(gameState)
                gameState = result
            },
            onComputerTurn = { newState ->
                gameState = newState
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordChainStarterScreen(
    onBackClick: () -> Unit,
    onSelectStarter: (WordChainStarter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Nối từ",
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
            // Game rules
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GameModeWordChain.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📜 Luật chơi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameModeWordChain
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Nối từ bằng chữ cái cuối của từ trước\n" +
                                "• Mỗi từ chỉ được dùng một lần\n" +
                                "• Ai không nghĩ ra từ sẽ thua\n" +
                                "• Ví dụ: mèo → ông → gà → ăn...",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Ai đi trước?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Player first button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = { onSelectStarter(WordChainStarter.PLAYER) }
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
                            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🙋", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Tôi đi trước",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Bạn chọn từ đầu tiên",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Computer first button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = { onSelectStarter(WordChainStarter.COMPUTER) }
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
                            .background(GameModeWordChain.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🐓", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Gà Vàng đi trước",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GameModeWordChain
                        )
                        Text(
                            text = "Máy chọn từ đầu tiên",
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
private fun WordChainGameContent(
    gameState: WordChainGameState,
    onBackClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSubmitWord: () -> Unit,
    onComputerTurn: (WordChainGameState) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new word added
    LaunchedEffect(gameState.wordChain.size) {
        if (gameState.wordChain.isNotEmpty()) {
            listState.animateScrollToItem(gameState.wordChain.size - 1)
        }
    }

    // Computer's turn
    LaunchedEffect(gameState.isPlayerTurn) {
        if (!gameState.isPlayerTurn && !gameState.isGameOver) {
            delay(1000) // Think time
            val newState = processComputerTurn(gameState)
            onComputerTurn(newState)
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
                    text = "Nối từ",
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
                // Turn indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (gameState.isPlayerTurn) PrimaryBlue.copy(alpha = 0.1f)
                    else GameModeWordChain.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (gameState.isPlayerTurn) "🙋 Lượt bạn" else "🐓 Lượt máy",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (gameState.isPlayerTurn) PrimaryBlue else GameModeWordChain
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Score bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🙋", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Bạn: ${gameState.playerScore}",
                    fontWeight = FontWeight.Medium,
                    color = PrimaryBlue
                )
            }
            Text(
                text = "Từ còn lại: ${gameState.availableWords.size}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🐓", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Máy: ${gameState.computerScore}",
                    fontWeight = FontWeight.Medium,
                    color = GameModeWordChain
                )
            }
        }

        // Word chain display
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (gameState.wordChain.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (gameState.isPlayerTurn)
                                "Nhập từ đầu tiên để bắt đầu!"
                            else
                                "Đang chờ Gà Vàng...",
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(gameState.wordChain) { entry ->
                WordChainBubble(entry = entry)
            }

            // Loading indicator for computer turn
            if (!gameState.isPlayerTurn && !gameState.isGameOver) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = GameModeWordChain.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = GameModeWordChain
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Đang nghĩ...",
                                    fontSize = 14.sp,
                                    color = GameModeWordChain
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message
        AnimatedVisibility(
            visible = gameState.errorMessage != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            gameState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VietRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = VietRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            fontSize = 14.sp,
                            color = VietRed
                        )
                    }
                }
            }
        }

        // Input area
        if (gameState.isPlayerTurn && !gameState.isGameOver) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Hint for required starting letter
                    if (gameState.wordChain.isNotEmpty()) {
                        val lastWord = gameState.wordChain.last().word
                        val requiredChar = getLastChar(lastWord)
                        Text(
                            text = "Từ phải bắt đầu bằng: \"$requiredChar\"",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = gameState.currentInput,
                            onValueChange = onInputChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Nhập từ tiếng Việt...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                cursorColor = PrimaryBlue
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = onSubmitWord,
                            enabled = gameState.currentInput.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi"
                            )
                        }
                    }

                    // Give up button
                    TextButton(
                        onClick = {
                            // Player gives up - computer wins
                            val eloChange = UserProgressManager.recordWrongAnswer(2)
                            onComputerTurn(
                                gameState.copy(
                                    isGameOver = true,
                                    winner = "computer",
                                    eloChange = eloChange
                                )
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "🏳️ Bỏ cuộc",
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordChainBubble(entry: WordChainEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isPlayer) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (entry.isPlayer) 16.dp else 4.dp,
                bottomEnd = if (entry.isPlayer) 4.dp else 16.dp
            ),
            color = if (entry.isPlayer) PrimaryBlue else GameModeWordChain.copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!entry.isPlayer) {
                    Text(text = "🐓", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = entry.word,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (entry.isPlayer) Color.White else TextPrimary
                )
                if (entry.isPlayer) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "🙋", fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordChainGameOverScreen(
    gameState: WordChainGameState,
    onPlayAgain: () -> Unit,
    onBackClick: () -> Unit
) {
    val isPlayerWinner = gameState.winner == "player"

    // Record game mode stats once when screen appears
    LaunchedEffect(Unit) {
        UserProgressManager.recordGameModeResult(
            gameModeId = "word_chain",
            correct = gameState.playerScore,
            wrong = gameState.computerScore,
            score = gameState.playerScore
        )
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
            // Winner announcement
            Text(
                text = if (isPlayerWinner) "🎉 Bạn thắng!" else "🐓 Gà Vàng thắng!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlayerWinner) PrimaryGreen else GameModeWordChain
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPlayerWinner)
                    "Gà Vàng không nghĩ ra từ nào!"
                else
                    "Bạn không nghĩ ra từ phù hợp!",
                fontSize = 16.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🙋", fontSize = 24.sp)
                            Text(
                                text = "${gameState.playerScore}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Text(
                                text = "Bạn",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = "VS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🐓", fontSize = 24.sp)
                            Text(
                                text = "${gameState.computerScore}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = GameModeWordChain
                            )
                            Text(
                                text = "Gà Vàng",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${gameState.wordChain.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tổng số từ",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${if (gameState.eloChange >= 0) "+" else ""}${gameState.eloChange}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gameState.eloChange >= 0) PrimaryGreen else VietRed
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

            // Buttons
            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GameModeWordChain),
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GameModeWordChain)
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

// ========== Game Logic Functions ==========

/**
 * Get the last character of a Vietnamese word (handles diacritics)
 */
private fun getLastChar(word: String): String {
    if (word.isEmpty()) return ""
    return word.last().toString()
}

/**
 * Check if a word starts with the required character
 */
private fun wordStartsWith(word: String, char: String): Boolean {
    if (word.isEmpty() || char.isEmpty()) return false
    return word.first().toString().equals(char, ignoreCase = true)
}

/**
 * Start a new Word Chain game with weighted selection
 */
private fun startWordChainGame(starter: WordChainStarter): WordChainGameState {
    val allWords = VocabularyRepository.wordChainWords.toSet()

    val initialState = WordChainGameState(
        availableWords = allWords,
        isPlayerTurn = starter == WordChainStarter.PLAYER
    )

    // If computer starts first, let it pick a weighted word (prefer less seen)
    return if (starter == WordChainStarter.COMPUTER) {
        val allWordsList = allWords.toList()
        val selectedWord = EncounteredItemsManager.selectWeightedItem(
            gameMode = GameMode.WORD_CHAIN,
            allItemIds = allWordsList
        ) ?: allWordsList.randomOrNull() ?: ""

        if (selectedWord.isNotEmpty()) {
            // Record encounter
            EncounteredItemsManager.recordEncounter(GameMode.WORD_CHAIN, selectedWord)

            initialState.copy(
                wordChain = listOf(WordChainEntry(selectedWord, isPlayer = false)),
                usedWords = setOf(selectedWord),
                availableWords = allWords - selectedWord,
                isPlayerTurn = true,
                computerScore = 1
            )
        } else {
            initialState
        }
    } else {
        initialState
    }
}

/**
 * Process player's word submission
 */
private fun processPlayerWord(gameState: WordChainGameState): WordChainGameState {
    val inputWord = gameState.currentInput.trim().lowercase()

    // Validate word
    if (inputWord.isEmpty()) {
        return gameState.copy(errorMessage = "Vui lòng nhập từ!")
    }

    // Try to find Vietnamese word (support non-diacritic input)
    val matchedWord = VocabularyRepository.findVietnameseWord(inputWord)

    // Check if word is in dictionary
    if (matchedWord == null || (matchedWord !in gameState.availableWords && matchedWord !in VocabularyRepository.wordChainWords)) {
        MascotFeedbackManager.showWrongFeedback(
            "Bài nối từ. Người học nhập \"$inputWord\" nhưng từ này không có trong từ điển."
        )
        return gameState.copy(errorMessage = "Từ \"$inputWord\" không có trong từ điển!")
    }

    val word = matchedWord

    // Check if word already used
    if (word in gameState.usedWords) {
        MascotFeedbackManager.showWrongFeedback(
            "Bài nối từ. Từ \"$word\" đã được dùng trước đó rồi."
        )
        return gameState.copy(errorMessage = "Từ \"$word\" đã được sử dụng!")
    }

    // Check if word starts with correct character (if not first word)
    if (gameState.wordChain.isNotEmpty()) {
        val lastWord = gameState.wordChain.last().word
        val requiredChar = getLastChar(lastWord)
        if (!wordStartsWith(word, requiredChar)) {
            MascotFeedbackManager.showWrongFeedback(
                "Bài nối từ. Từ phải bắt đầu bằng chữ \"$requiredChar\" (chữ cuối của từ trước)."
            )
            return gameState.copy(errorMessage = "Từ phải bắt đầu bằng \"$requiredChar\"!")
        }
    }

    // Valid word - add to chain
    MascotFeedbackManager.showCorrectFeedback(
        "Bài nối từ. Người học nối đúng từ \"$word\"."
    )

    // Record encounter for spaced repetition
    EncounteredItemsManager.recordEncounter(GameMode.WORD_CHAIN, word, wasCorrect = true)

    val newEntry = WordChainEntry(word, isPlayer = true)
    return gameState.copy(
        wordChain = gameState.wordChain + newEntry,
        usedWords = gameState.usedWords + word,
        availableWords = gameState.availableWords - word,
        currentInput = "",
        isPlayerTurn = false,
        errorMessage = null,
        playerScore = gameState.playerScore + 1
    )
}

/**
 * Process computer's turn with weighted selection
 */
private fun processComputerTurn(gameState: WordChainGameState): WordChainGameState {
    if (gameState.wordChain.isEmpty()) {
        // Computer goes first - pick weighted word
        val availableList = gameState.availableWords.toList()
        val selectedWord = EncounteredItemsManager.selectWeightedItem(
            gameMode = GameMode.WORD_CHAIN,
            allItemIds = availableList
        ) ?: availableList.randomOrNull()

        if (selectedWord == null) {
            // No words available - player wins by default
            val eloChange = UserProgressManager.recordCorrectAnswer(2)
            return gameState.copy(
                isGameOver = true,
                winner = "player",
                eloChange = eloChange
            )
        }

        // Record encounter
        EncounteredItemsManager.recordEncounter(GameMode.WORD_CHAIN, selectedWord)

        return gameState.copy(
            wordChain = listOf(WordChainEntry(selectedWord, isPlayer = false)),
            usedWords = setOf(selectedWord),
            availableWords = gameState.availableWords - selectedWord,
            isPlayerTurn = true,
            computerScore = 1
        )
    }

    // Find a word that starts with the last character of the previous word
    val lastWord = gameState.wordChain.last().word
    val requiredChar = getLastChar(lastWord)

    val validWords = gameState.availableWords.filter { word ->
        wordStartsWith(word, requiredChar)
    }

    if (validWords.isEmpty()) {
        // Computer can't find a word - player wins!
        val eloChange = UserProgressManager.recordCorrectAnswer(2)
        return gameState.copy(
            isGameOver = true,
            winner = "player",
            eloChange = eloChange
        )
    }

    // Pick a weighted valid word (prefer less seen)
    val chosenWord = EncounteredItemsManager.selectWeightedItem(
        gameMode = GameMode.WORD_CHAIN,
        allItemIds = validWords.toList(),
        excludeIds = gameState.usedWords
    ) ?: validWords.random()

    // Record encounter
    EncounteredItemsManager.recordEncounter(GameMode.WORD_CHAIN, chosenWord)

    val newEntry = WordChainEntry(chosenWord, isPlayer = false)

    return gameState.copy(
        wordChain = gameState.wordChain + newEntry,
        usedWords = gameState.usedWords + chosenWord,
        availableWords = gameState.availableWords - chosenWord,
        isPlayerTurn = true,
        computerScore = gameState.computerScore + 1
    )
}

