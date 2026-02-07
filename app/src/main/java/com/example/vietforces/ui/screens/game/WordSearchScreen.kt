package com.example.vietforces.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.EncounteredItemsManager
import com.example.vietforces.data.manager.GameMode
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.repository.VocabularyRepository
import com.example.vietforces.ui.components.MascotFeedbackManager
import com.example.vietforces.ui.theme.*

/**
 * Direction for placing words in the grid
 */
enum class WordDirection {
    HORIZONTAL,      // Left to right
    VERTICAL,        // Top to bottom
    DIAGONAL_DOWN,   // Top-left to bottom-right
    DIAGONAL_UP      // Bottom-left to top-right
}

/**
 * Represents a placed word in the grid
 */
data class PlacedWord(
    val word: String,
    val startRow: Int,
    val startCol: Int,
    val direction: WordDirection,
    val isFound: Boolean = false
)

/**
 * Represents a cell in the grid
 */
data class GridCell(
    val char: Char,
    val row: Int,
    val col: Int,
    val isSelected: Boolean = false,
    val isPartOfFoundWord: Boolean = false
)

/**
 * Game state for Word Search game
 */
data class WordSearchGameState(
    val grid: List<List<GridCell>> = emptyList(),
    val gridSize: Int = 10,
    val wordsToFind: List<PlacedWord> = emptyList(),
    val selectedCells: List<Pair<Int, Int>> = emptyList(),
    val foundWords: Set<String> = emptySet(),
    val score: Int = 0,
    val eloChange: Int = 0,
    val isGameOver: Boolean = false,
    val startTime: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSearchScreen(
    onBackClick: () -> Unit
) {
    var gameState by remember { mutableStateOf(generateWordSearchGame()) }

    if (gameState.isGameOver) {
        WordSearchGameOverScreen(
            gameState = gameState,
            onPlayAgain = {
                gameState = generateWordSearchGame()
            },
            onBackClick = onBackClick
        )
    } else {
        WordSearchGameContent(
            gameState = gameState,
            onBackClick = onBackClick,
            onCellClick = { row, col ->
                gameState = handleCellClick(gameState, row, col)
            },
            onClearSelection = {
                gameState = gameState.copy(
                    selectedCells = emptyList(),
                    grid = gameState.grid.map { rowCells ->
                        rowCells.map { cell -> cell.copy(isSelected = false) }
                    }
                )
            },
            onCheckWord = {
                gameState = checkSelectedWord(gameState)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSearchGameContent(
    gameState: WordSearchGameState,
    onBackClick: () -> Unit,
    onCellClick: (Int, Int) -> Unit,
    onClearSelection: () -> Unit,
    onCheckWord: () -> Unit
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
                    text = "Tìm từ",
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
                // Found count
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "✓ ${gameState.foundWords.size}/${gameState.wordsToFind.size}",
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
            // Score
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

            // Word grid
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    gameState.grid.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEachIndexed { colIndex, cell ->
                                GridCellView(
                                    cell = cell,
                                    onClick = { onCellClick(rowIndex, colIndex) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClearSelection,
                    modifier = Modifier.weight(1f),
                    enabled = gameState.selectedCells.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa")
                }

                Button(
                    onClick = onCheckWord,
                    modifier = Modifier.weight(2f),
                    enabled = gameState.selectedCells.size >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = GameModeWordSearch)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Words to find
            Text(
                text = "Tìm các từ sau:",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Word list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(gameState.wordsToFind) { placedWord ->
                    WordListItem(
                        word = placedWord.word,
                        isFound = placedWord.word in gameState.foundWords
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCellView(
    cell: GridCell,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        cell.isPartOfFoundWord -> PrimaryGreen.copy(alpha = 0.3f)
        cell.isSelected -> GameModeWordSearch.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val borderColor = when {
        cell.isPartOfFoundWord -> PrimaryGreen
        cell.isSelected -> GameModeWordSearch
        else -> Color(0xFFE0E0E0)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.char.toString().uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                cell.isPartOfFoundWord -> PrimaryGreen
                cell.isSelected -> GameModeWordSearch
                else -> TextPrimary
            }
        )
    }
}

@Composable
private fun WordListItem(
    word: String,
    isFound: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isFound) PrimaryGreen.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isFound) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isFound) PrimaryGreen else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = word.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFound) PrimaryGreen else TextPrimary,
            textDecoration = if (isFound) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordSearchGameOverScreen(
    gameState: WordSearchGameState,
    onPlayAgain: () -> Unit,
    onBackClick: () -> Unit
) {
    val totalWords = gameState.wordsToFind.size
    val foundCount = gameState.foundWords.size
    val percentage = if (totalWords > 0) (foundCount * 100 / totalWords) else 0

    val message = when {
        percentage == 100 -> "🏆 Xuất sắc!"
        percentage >= 70 -> "🎉 Giỏi lắm!"
        percentage >= 50 -> "👍 Khá tốt!"
        else -> "💪 Cố gắng thêm nhé!"
    }

    // Record game mode stats once when screen appears
    LaunchedEffect(Unit) {
        UserProgressManager.recordGameModeResult(
            gameModeId = "word_search",
            correct = foundCount,
            wrong = totalWords - foundCount,
            score = foundCount
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
                        text = "$foundCount / $totalWords",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = GameModeWordSearch
                    )
                    Text(
                        text = "Từ đã tìm thấy",
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
                                text = "Hoàn thành",
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
                colors = ButtonDefaults.buttonColors(containerColor = GameModeWordSearch),
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GameModeWordSearch)
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
 * Vietnamese alphabet for filling empty cells
 * Using common Vietnamese consonants and vowels
 */
private val vietnameseChars = listOf(
    'a', 'ă', 'â', 'b', 'c', 'd', 'đ', 'e', 'ê', 'g', 'h', 'i',
    'k', 'l', 'm', 'n', 'o', 'ô', 'ơ', 'p', 'q', 'r', 's', 't',
    'u', 'ư', 'v', 'x', 'y'
)

/**
 * Generate a new Word Search game with improved word placement algorithm
 */
private fun generateWordSearchGame(): WordSearchGameState {
    val gridSize = 8  // Smaller grid for better word density
    val wordsToPlace = selectWordsForGame()

    // Sort words by length (longest first) for better placement
    val sortedWords = wordsToPlace.sortedByDescending { it.length }

    // Initialize empty grid
    val grid = MutableList(gridSize) { row ->
        MutableList(gridSize) { col ->
            GridCell(char = ' ', row = row, col = col)
        }
    }

    val placedWords = mutableListOf<PlacedWord>()

    // Try to place each word without overlap
    for (word in sortedWords) {
        val placed = tryPlaceWordNoOverlap(grid, word, gridSize)
        if (placed != null) {
            placedWords.add(placed)
        }
    }

    // Collect all characters used in placed words for smarter filling
    val usedChars = placedWords.flatMap { it.word.toList() }.toSet()
    val fillChars = if (usedChars.isNotEmpty()) {
        // Mix of used chars and random Vietnamese chars for better camouflage
        (usedChars.toList() + vietnameseChars).distinct()
    } else {
        vietnameseChars
    }

    // Fill remaining empty cells with weighted random characters
    for (row in 0 until gridSize) {
        for (col in 0 until gridSize) {
            if (grid[row][col].char == ' ') {
                grid[row][col] = grid[row][col].copy(char = fillChars.random())
            }
        }
    }

    return WordSearchGameState(
        grid = grid.map { it.toList() },
        gridSize = gridSize,
        wordsToFind = placedWords
    )
}

/**
 * Select words for the game using weighted selection (prefer less seen words)
 */
private fun selectWordsForGame(): List<String> {
    // Use the same word list as Word Chain game
    // For word search, we need shorter words that can fit in grid better
    // Split compound words and use individual parts, or filter single-word items
    val allWords = VocabularyRepository.wordDataset
        .map { it.vietnamese }
        .flatMap { word ->
            if (word.contains(" ")) {
                // For compound words like "cái bàn", use just the main word
                word.split(" ").filter { it.length in 2..6 }
            } else {
                listOf(word)
            }
        }
        .filter { it.length in 2..6 }  // Filter appropriate length for grid
        .distinct()

    if (allWords.size < 4) {
        // Fallback words if vocabulary is too small
        return listOf("mèo", "chó", "gà", "cơm", "nước", "cá")
    }

    // Use weighted selection to prefer less seen words
    val selectedWords = EncounteredItemsManager.selectWeightedItems(
        gameMode = GameMode.WORD_SEARCH,
        allItemIds = allWords,
        count = 6  // 6 words for 8x8 grid
    )

    return if (selectedWords.size >= 4) {
        selectedWords
    } else {
        allWords.shuffled().take(6)
    }
}

/**
 * Try to place a word WITHOUT overlapping any existing words
 */
private fun tryPlaceWordNoOverlap(
    grid: MutableList<MutableList<GridCell>>,
    word: String,
    gridSize: Int
): PlacedWord? {
    val directions = WordDirection.entries.shuffled()
    val validPositions = mutableListOf<Triple<Int, Int, WordDirection>>()

    // Find all positions where the word can be placed without overlap
    for (direction in directions) {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (canPlaceWordNoOverlap(grid, word, row, col, direction, gridSize)) {
                    validPositions.add(Triple(row, col, direction))
                }
            }
        }
    }

    // Pick a random valid position
    if (validPositions.isNotEmpty()) {
        val (row, col, direction) = validPositions.random()
        placeWord(grid, word, row, col, direction)
        return PlacedWord(word, row, col, direction)
    }

    return null
}

/**
 * Check if a word can be placed at the given position WITHOUT overlapping
 */
private fun canPlaceWordNoOverlap(
    grid: MutableList<MutableList<GridCell>>,
    word: String,
    startRow: Int,
    startCol: Int,
    direction: WordDirection,
    gridSize: Int
): Boolean {
    val (dRow, dCol) = getDirectionDelta(direction)

    for (i in word.indices) {
        val row = startRow + i * dRow
        val col = startCol + i * dCol

        // Check bounds
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) {
            return false
        }

        // Cell must be empty (no overlap allowed)
        if (grid[row][col].char != ' ') {
            return false
        }
    }

    return true
}

/**
 * Place a word in the grid
 */
private fun placeWord(
    grid: MutableList<MutableList<GridCell>>,
    word: String,
    startRow: Int,
    startCol: Int,
    direction: WordDirection
) {
    val (dRow, dCol) = getDirectionDelta(direction)

    for (i in word.indices) {
        val row = startRow + i * dRow
        val col = startCol + i * dCol
        grid[row][col] = grid[row][col].copy(char = word[i])
    }
}

/**
 * Get row and column delta for a direction
 */
private fun getDirectionDelta(direction: WordDirection): Pair<Int, Int> {
    return when (direction) {
        WordDirection.HORIZONTAL -> Pair(0, 1)
        WordDirection.VERTICAL -> Pair(1, 0)
        WordDirection.DIAGONAL_DOWN -> Pair(1, 1)
        WordDirection.DIAGONAL_UP -> Pair(-1, 1)
    }
}

/**
 * Handle cell click - add to selection
 */
private fun handleCellClick(
    gameState: WordSearchGameState,
    row: Int,
    col: Int
): WordSearchGameState {
    val cellPosition = Pair(row, col)

    // Toggle selection
    val newSelectedCells = if (cellPosition in gameState.selectedCells) {
        gameState.selectedCells - cellPosition
    } else {
        gameState.selectedCells + cellPosition
    }

    // Update grid with selection state
    val newGrid = gameState.grid.mapIndexed { r, rowCells ->
        rowCells.mapIndexed { c, cell ->
            cell.copy(isSelected = Pair(r, c) in newSelectedCells)
        }
    }

    return gameState.copy(
        selectedCells = newSelectedCells,
        grid = newGrid
    )
}

/**
 * Check if selected cells form a valid word
 */
private fun checkSelectedWord(gameState: WordSearchGameState): WordSearchGameState {
    if (gameState.selectedCells.size < 2) return gameState

    // Get selected word by sorting cells and extracting characters
    val sortedCells = gameState.selectedCells.sortedWith(compareBy({ it.first }, { it.second }))

    // Try to build word in different orders
    val selectedWord1 = sortedCells.map { (r, c) -> gameState.grid[r][c].char }.joinToString("")
    val selectedWord2 = sortedCells.reversed().map { (r, c) -> gameState.grid[r][c].char }.joinToString("")

    // Also try sorting by column first (for vertical words)
    val sortedByCol = gameState.selectedCells.sortedWith(compareBy({ it.second }, { it.first }))
    val selectedWord3 = sortedByCol.map { (r, c) -> gameState.grid[r][c].char }.joinToString("")
    val selectedWord4 = sortedByCol.reversed().map { (r, c) -> gameState.grid[r][c].char }.joinToString("")

    // Check if any word matches
    val wordsNotFound = gameState.wordsToFind.filter { it.word !in gameState.foundWords }

    var foundWord: PlacedWord? = null
    for (placedWord in wordsNotFound) {
        if (placedWord.word == selectedWord1 ||
            placedWord.word == selectedWord2 ||
            placedWord.word == selectedWord3 ||
            placedWord.word == selectedWord4) {
            // Verify cells are in correct positions
            if (verifyCellsMatchWord(gameState, placedWord)) {
                foundWord = placedWord
                break
            }
        }
    }

    if (foundWord != null) {
        // Word found!
        MascotFeedbackManager.showCorrectFeedback()
        val eloChange = UserProgressManager.recordCorrectAnswer(1)
        val newFoundWords = gameState.foundWords + foundWord.word

        // Record encounter for spaced repetition
        EncounteredItemsManager.recordEncounter(
            gameMode = GameMode.WORD_SEARCH,
            itemId = foundWord.word,
            wasCorrect = true
        )

        // Mark cells as part of found word
        val wordCells = getWordCells(foundWord)
        val newGrid = gameState.grid.mapIndexed { r, rowCells ->
            rowCells.mapIndexed { c, cell ->
                if (Pair(r, c) in wordCells) {
                    cell.copy(isSelected = false, isPartOfFoundWord = true)
                } else {
                    cell.copy(isSelected = false)
                }
            }
        }

        val isGameOver = newFoundWords.size == gameState.wordsToFind.size

        return gameState.copy(
            grid = newGrid,
            selectedCells = emptyList(),
            foundWords = newFoundWords,
            score = gameState.score + 10,
            eloChange = gameState.eloChange + eloChange,
            isGameOver = isGameOver
        )
    } else {
        // Word not found - clear selection
        MascotFeedbackManager.showWrongFeedback()
        val newGrid = gameState.grid.map { rowCells ->
            rowCells.map { cell -> cell.copy(isSelected = false) }
        }

        return gameState.copy(
            grid = newGrid,
            selectedCells = emptyList()
        )
    }
}

/**
 * Verify that selected cells match the word's position
 */
private fun verifyCellsMatchWord(
    gameState: WordSearchGameState,
    placedWord: PlacedWord
): Boolean {
    val wordCells = getWordCells(placedWord)
    return gameState.selectedCells.toSet() == wordCells
}

/**
 * Get all cells occupied by a placed word
 */
private fun getWordCells(placedWord: PlacedWord): Set<Pair<Int, Int>> {
    val (dRow, dCol) = getDirectionDelta(placedWord.direction)
    val cells = mutableSetOf<Pair<Int, Int>>()

    for (i in placedWord.word.indices) {
        val row = placedWord.startRow + i * dRow
        val col = placedWord.startCol + i * dCol
        cells.add(Pair(row, col))
    }

    return cells
}

