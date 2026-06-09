package com.example.vietforces.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Represents a syllable pair (classifier + noun)
 */
data class SyllablePair(
    val classifier: String,  // e.g., "con", "cái", "quả"
    val noun: String,        // e.g., "chó", "bàn", "táo"
    val meaning: String = "" // Vietnamese meaning/hint
)

/**
 * Represents a card in the matching game
 */
data class SyllableCard(
    val id: Int,
    val text: String,
    val pairId: Int,         // ID to match pairs
    val isClassifier: Boolean, // true = classifier, false = noun
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

/**
 * Game state for Syllable Match game
 */
data class SyllableMatchGameState(
    val cards: List<SyllableCard> = emptyList(),
    val selectedCard: SyllableCard? = null,
    val matchedPairs: Int = 0,
    val totalPairs: Int = 0,
    val attempts: Int = 0,
    val score: Int = 0,
    val eloChange: Int = 0,
    val isGameOver: Boolean = false,
    val showMismatch: Boolean = false,
    val message: String? = null,
    val showingCompletion: Boolean = false  // Showing completion before game over
)

/**
 * Predefined syllable pairs for the game
 * For 2-syllable phrases: join 2 pieces (con + mèo, xe + máy…)
 * For 3–4 syllable phrases: group by meaning (quán + cà phê, xe + buýt, áo + sơ mi)
 */
private val syllablePairs = listOf(
    // ==================== ANIMALS (20 words) ====================
    SyllablePair("con", "mèo", "con mèo"),
    SyllablePair("con", "chó", "con chó"),
    SyllablePair("con", "gà", "con gà"),
    SyllablePair("con", "vịt", "con vịt"),
    SyllablePair("con", "cá", "con cá"),
    SyllablePair("con", "heo", "con heo"),
    SyllablePair("con", "bò", "con bò"),
    SyllablePair("con", "dê", "con dê"),
    SyllablePair("con", "thỏ", "con thỏ"),
    SyllablePair("con", "voi", "con voi"),
    SyllablePair("con", "hổ", "con hổ"),
    SyllablePair("con", "ngựa", "con ngựa"),
    SyllablePair("con", "chim", "con chim"),
    SyllablePair("con", "ong", "con ong"),
    SyllablePair("con", "cua", "con cua"),
    SyllablePair("con", "tôm", "con tôm"),
    SyllablePair("con", "ếch", "con ếch"),
    SyllablePair("con", "rắn", "con rắn"),
    SyllablePair("con", "sóc", "con sóc"),
    SyllablePair("con", "khỉ", "con khỉ"),

    // ==================== SCHOOL SUPPLIES (12 words) ====================
    SyllablePair("cây", "bút", "cây bút"),
    SyllablePair("bút", "chì", "bút chì"),
    SyllablePair("cục", "tẩy", "cục tẩy"),
    SyllablePair("quyển", "sách", "quyển sách"),
    SyllablePair("cuốn", "vở", "cuốn vở"),
    SyllablePair("cái", "thước", "cái thước"),
    SyllablePair("cái", "kéo", "cái kéo"),
    SyllablePair("cái", "cặp", "cái cặp"),
    SyllablePair("bàn", "học", "bàn học"),
    SyllablePair("ghế", "học", "ghế học"),
    SyllablePair("bảng", "đen", "bảng đen"),
    SyllablePair("hộp", "bút", "hộp bút"),

    // ==================== HOUSEHOLD ITEMS (13 words) ====================
    SyllablePair("cái", "bàn", "cái bàn"),
    SyllablePair("cái", "ghế", "cái ghế"),
    SyllablePair("cái", "giường", "cái giường"),
    SyllablePair("cái", "tủ", "cái tủ"),
    SyllablePair("cái", "gối", "cái gối"),
    SyllablePair("cái", "chăn", "cái chăn"),
    SyllablePair("cái", "đèn", "cái đèn"),
    SyllablePair("cái", "quạt", "cái quạt"),
    SyllablePair("tủ", "lạnh", "tủ lạnh"),
    SyllablePair("máy", "giặt", "máy giặt"),
    SyllablePair("điều", "hòa", "điều hòa"),
    SyllablePair("cái", "khóa", "cái khóa"),
    SyllablePair("chìa", "khóa", "chìa khóa"),

    // ==================== KITCHEN (12 words) ====================
    SyllablePair("cái", "nồi", "cái nồi"),
    SyllablePair("cái", "chảo", "cái chảo"),
    SyllablePair("con", "dao", "con dao"),
    SyllablePair("cái", "thớt", "cái thớt"),
    SyllablePair("cái", "muỗng", "cái muỗng"),
    SyllablePair("cái", "nĩa", "cái nĩa"),
    SyllablePair("cái", "đũa", "cái đũa"),
    SyllablePair("cái", "bát", "cái bát"),
    SyllablePair("cái", "tô", "cái tô"),
    SyllablePair("cái", "ly", "cái ly"),
    SyllablePair("cái", "chén", "cái chén"),
    SyllablePair("cái", "ấm", "cái ấm"),

    // ==================== FOOD & DRINKS (20 words) ====================
    SyllablePair("bánh", "mì", "bánh mì"),
    SyllablePair("phở", "bò", "phở bò"),
    SyllablePair("bún", "bò", "bún bò"),
    SyllablePair("cơm", "gà", "cơm gà"),
    SyllablePair("cháo", "gà", "cháo gà"),
    SyllablePair("trứng", "rán", "trứng rán"),
    SyllablePair("trứng", "luộc", "trứng luộc"),
    SyllablePair("cá", "kho", "cá kho"),
    SyllablePair("thịt", "nướng", "thịt nướng"),
    SyllablePair("rau", "muống", "rau muống"),
    SyllablePair("cà", "chua", "cà chua"),
    SyllablePair("củ", "hành", "củ hành"),
    SyllablePair("trái", "táo", "trái táo"),
    SyllablePair("trái", "cam", "trái cam"),
    SyllablePair("trái", "chuối", "trái chuối"),
    SyllablePair("nước", "lọc", "nước lọc"),
    SyllablePair("cà", "phê", "cà phê"),
    SyllablePair("trà", "sữa", "trà sữa"),
    SyllablePair("nước", "cam", "nước cam"),
    SyllablePair("nước", "mía", "nước mía"),

    // ==================== PLACES (15 words) ====================
    SyllablePair("ngôi", "nhà", "ngôi nhà"),
    SyllablePair("trường", "học", "trường học"),
    SyllablePair("lớp", "học", "lớp học"),
    SyllablePair("thư", "viện", "thư viện"),
    SyllablePair("bệnh", "viện", "bệnh viện"),
    SyllablePair("siêu", "thị", "siêu thị"),
    SyllablePair("nhà", "hàng", "nhà hàng"),
    SyllablePair("quán", "ăn", "quán ăn"),
    SyllablePair("quán", "cà phê", "quán cà phê"),
    SyllablePair("công", "viên", "công viên"),
    SyllablePair("sân", "bay", "sân bay"),
    SyllablePair("nhà", "ga", "nhà ga"),
    SyllablePair("bến", "xe", "bến xe"),
    SyllablePair("bưu", "điện", "bưu điện"),
    SyllablePair("tiệm", "thuốc", "tiệm thuốc"),

    // ==================== VEHICLES (6 words) ====================
    SyllablePair("xe", "máy", "xe máy"),
    SyllablePair("xe", "đạp", "xe đạp"),
    SyllablePair("ô", "tô", "ô tô"),
    SyllablePair("xe", "buýt", "xe buýt"),
    SyllablePair("tàu", "hỏa", "tàu hỏa"),
    SyllablePair("máy", "bay", "máy bay"),

    // ==================== BODY & CLOTHING (11 words) ====================
    SyllablePair("mái", "tóc", "mái tóc"),
    SyllablePair("đôi", "mắt", "đôi mắt"),
    SyllablePair("cái", "mũ", "cái mũ"),
    SyllablePair("áo", "thun", "áo thun"),
    SyllablePair("áo", "sơ mi", "áo sơ mi"),
    SyllablePair("áo", "khoác", "áo khoác"),
    SyllablePair("quần", "jean", "quần jean"),
    SyllablePair("đôi", "giày", "đôi giày"),
    SyllablePair("dép", "lê", "dép lê"),
    SyllablePair("túi", "xách", "túi xách"),
    SyllablePair("đồng", "hồ", "đồng hồ")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllableMatchScreen(
    onBackClick: () -> Unit
) {
    var gameState by remember { mutableStateOf(generateSyllableMatchGame()) }

    // Handle mismatch animation
    LaunchedEffect(gameState.showMismatch) {
        if (gameState.showMismatch) {
            delay(800)
            gameState = hideMismatchedCards(gameState)
        }
    }

    // Handle completion delay (5 seconds before showing result)
    LaunchedEffect(gameState.showingCompletion) {
        if (gameState.showingCompletion) {
            delay(5000)  // 5 second delay
            gameState = gameState.copy(
                showingCompletion = false,
                isGameOver = true
            )
        }
    }

    if (gameState.isGameOver) {
        SyllableMatchGameOverScreen(
            gameState = gameState,
            onPlayAgain = {
                gameState = generateSyllableMatchGame()
            },
            onBackClick = onBackClick
        )
    } else {
        SyllableMatchGameContent(
            gameState = gameState,
            onBackClick = onBackClick,
            onCardClick = { card ->
                if (!gameState.showMismatch && !card.isMatched && !card.isFlipped) {
                    gameState = handleCardClick(gameState, card)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyllableMatchGameContent(
    gameState: SyllableMatchGameState,
    onBackClick: () -> Unit,
    onCardClick: (SyllableCard) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Ghép âm tiết",
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
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "✓ ${gameState.matchedPairs}/${gameState.totalPairs}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Điểm: ${gameState.score}",
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Lượt: ${gameState.attempts}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "Elo: ${if (gameState.eloChange >= 0) "+" else ""}${gameState.eloChange}",
                    fontWeight = FontWeight.Medium,
                    color = if (gameState.eloChange >= 0) PrimaryGreen else VietRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GameModeSyllable.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "Ghép từ loại với danh từ phù hợp\nVí dụ: \"con\" + \"mèo\" → \"con mèo\"",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Message
            AnimatedVisibility(
                visible = gameState.message != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                gameState.message?.let { message ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (message.contains("✓")) PrimaryGreen.copy(alpha = 0.2f)
                        else VietRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Medium,
                            color = if (message.contains("✓")) PrimaryGreen else VietRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameState.cards) { card ->
                    SyllableCardView(
                        card = card,
                        onClick = { onCardClick(card) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(
                    color = GameModeSyllable,
                    text = "Từ loại"
                )
                LegendItem(
                    color = GoldenYellow,
                    text = "Danh từ"
                )
            }
        }
    }
}

@Composable
private fun SyllableCardView(
    card: SyllableCard,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        card.isMatched -> PrimaryGreen.copy(alpha = 0.3f)
        card.isFlipped && card.isClassifier -> GameModeSyllable.copy(alpha = 0.3f)
        card.isFlipped && !card.isClassifier -> GoldenYellow.copy(alpha = 0.3f)
        else -> Color.White
    }

    val borderColor = when {
        card.isMatched -> PrimaryGreen
        card.isFlipped && card.isClassifier -> GameModeSyllable
        card.isFlipped && !card.isClassifier -> GoldenYellow
        else -> Color(0xFFE0E0E0)
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = !card.isMatched && !card.isFlipped, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (card.isFlipped || card.isMatched) 2.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (card.isFlipped || card.isMatched) {
                Text(
                    text = card.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        card.isMatched -> PrimaryGreen
                        card.isClassifier -> GameModeSyllable
                        else -> GoldenYellow
                    },
                    textAlign = TextAlign.Center
                )
            } else {
                Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = "Hidden",
                    tint = TextSecondary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.3f))
                .border(1.dp, color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyllableMatchGameOverScreen(
    gameState: SyllableMatchGameState,
    onPlayAgain: () -> Unit,
    onBackClick: () -> Unit
) {
    val accuracy = if (gameState.attempts > 0) {
        (gameState.matchedPairs * 100) / gameState.attempts
    } else 0

    val message = when {
        accuracy >= 80 -> "🏆 Xuất sắc!"
        accuracy >= 60 -> "🎉 Giỏi lắm!"
        accuracy >= 40 -> "👍 Khá tốt!"
        else -> "💪 Cố gắng thêm nhé!"
    }

    // Record game mode stats once when screen appears
    LaunchedEffect(Unit) {
        val wrongAttempts = gameState.attempts - gameState.matchedPairs
        UserProgressManager.recordGameModeResult(
            gameModeId = "syllable_match",
            correct = gameState.matchedPairs,
            wrong = if (wrongAttempts > 0) wrongAttempts else 0,
            score = gameState.matchedPairs
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
                        text = "${gameState.matchedPairs}/${gameState.totalPairs}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameModeSyllable
                    )
                    Text(
                        text = "Cặp đã ghép",
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
                                text = "${gameState.attempts}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Số lượt",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$accuracy%",
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

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GameModeSyllable),
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GameModeSyllable)
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
 * Generate a new Syllable Match game with weighted selection
 */
private fun generateSyllableMatchGame(): SyllableMatchGameState {
    // Create IDs for each syllable pair
    val pairIds = syllablePairs.mapIndexed { index, pair ->
        "${pair.classifier}_${pair.noun}"
    }

    // Select pairs using weighted selection (prefer less seen)
    val selectedIds = EncounteredItemsManager.selectWeightedItems(
        gameMode = GameMode.SYLLABLE_MATCH,
        allItemIds = pairIds,
        count = 6  // 6 pairs = 12 cards for 4x3 grid
    )

    // Get the actual pairs
    val selectedPairs = if (selectedIds.size >= 4) {
        selectedIds.mapNotNull { id ->
            val parts = id.split("_")
            if (parts.size == 2) {
                syllablePairs.find { it.classifier == parts[0] && it.noun == parts[1] }
            } else null
        }
    } else {
        syllablePairs.shuffled().take(6)
    }

    // Create cards
    val cards = mutableListOf<SyllableCard>()
    selectedPairs.forEachIndexed { index, pair ->
        // Classifier card
        cards.add(
            SyllableCard(
                id = index * 2,
                text = pair.classifier,
                pairId = index,
                isClassifier = true
            )
        )
        // Noun card
        cards.add(
            SyllableCard(
                id = index * 2 + 1,
                text = pair.noun,
                pairId = index,
                isClassifier = false
            )
        )
    }

    // Shuffle cards
    val shuffledCards = cards.shuffled()

    return SyllableMatchGameState(
        cards = shuffledCards,
        totalPairs = selectedPairs.size
    )
}

/**
 * Handle card click
 */
private fun handleCardClick(
    gameState: SyllableMatchGameState,
    card: SyllableCard
): SyllableMatchGameState {
    // Flip the card
    val updatedCards = gameState.cards.map {
        if (it.id == card.id) it.copy(isFlipped = true) else it
    }
    val flippedCard = card.copy(isFlipped = true)

    return if (gameState.selectedCard == null) {
        // First card selected
        gameState.copy(
            cards = updatedCards,
            selectedCard = flippedCard,
            message = null
        )
    } else {
        // Second card selected - check for match
        val firstCard = gameState.selectedCard
        val attempts = gameState.attempts + 1

        if (firstCard.pairId == flippedCard.pairId && firstCard.id != flippedCard.id) {
            // Match found! Must be classifier + noun pair
            if (firstCard.isClassifier != flippedCard.isClassifier) {
                MascotFeedbackManager.showCorrectFeedback(
                    "Bài ghép loại từ. Người học ghép đúng \"${firstCard.text}\" với \"${flippedCard.text}\"."
                )
                val eloChange = UserProgressManager.recordCorrectAnswer(1)
                val matchedPairs = gameState.matchedPairs + 1

                // Mark both cards as matched
                val matchedCards = updatedCards.map {
                    if (it.pairId == firstCard.pairId) it.copy(isMatched = true, isFlipped = true)
                    else it
                }

                // Get the matched word for message
                val classifier = if (firstCard.isClassifier) firstCard.text else flippedCard.text
                val noun = if (firstCard.isClassifier) flippedCard.text else firstCard.text

                // Record encounter for spaced repetition
                EncounteredItemsManager.recordEncounter(
                    gameMode = GameMode.SYLLABLE_MATCH,
                    itemId = "${classifier}_${noun}",
                    wasCorrect = true
                )

                val isCompleted = matchedPairs == gameState.totalPairs

                gameState.copy(
                    cards = matchedCards,
                    selectedCard = null,
                    matchedPairs = matchedPairs,
                    attempts = attempts,
                    score = gameState.score + 15,
                    eloChange = gameState.eloChange + eloChange,
                    showingCompletion = isCompleted,  // Trigger completion delay
                    isGameOver = false,  // Will be set to true after delay
                    message = if (isCompleted) "🎉 Hoàn thành! Đang tính kết quả..." else "✓ $classifier $noun"
                )
            } else {
                // Same type cards - not a valid match
                MascotFeedbackManager.showWrongFeedback(
                    "Bài ghép loại từ. Người học ghép hai thẻ cùng loại; cần ghép loại từ (con/cái/quyển...) với danh từ."
                )
                gameState.copy(
                    cards = updatedCards,
                    selectedCard = null,
                    attempts = attempts,
                    showMismatch = true,
                    message = "✗ Cần ghép từ loại với danh từ"
                )
            }
        } else {
            // No match
            MascotFeedbackManager.showWrongFeedback(
                "Bài ghép loại từ. Hai thẻ không thuộc cùng một cặp."
            )
            gameState.copy(
                cards = updatedCards,
                selectedCard = null,
                attempts = attempts,
                showMismatch = true,
                message = "✗ Không khớp"
            )
        }
    }
}

/**
 * Hide mismatched cards after delay
 */
private fun hideMismatchedCards(gameState: SyllableMatchGameState): SyllableMatchGameState {
    val hiddenCards = gameState.cards.map {
        if (!it.isMatched) it.copy(isFlipped = false) else it
    }

    return gameState.copy(
        cards = hiddenCards,
        showMismatch = false,
        message = null
    )
}

