package com.example.vietforces.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.vietforces.ui.theme.*

/**
 * Enum representing all available game modes in the app.
 * Easy to extend - just add a new entry here!
 */
enum class GameMode(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val hasHardMode: Boolean = false
) {
    IMAGE_TO_WORD(
        id = "image_to_word",
        title = "Nhìn hình đoán từ",
        description = "Xem hình ảnh và chọn/điền từ đúng",
        icon = Icons.Default.PhotoLibrary,
        color = GameModeImageToWord,
        hasHardMode = true
    ),
    WORD_TO_IMAGE(
        id = "word_to_image",
        title = "Nhìn từ đoán hình",
        description = "Đọc từ và chọn hình ảnh phù hợp",
        icon = Icons.Default.Spellcheck,
        color = GameModeWordToImage,
        hasHardMode = false
    ),
    SYLLABLE_MATCH(
        id = "syllable_match",
        title = "Ghép âm tiết",
        description = "Ghép các âm tiết thành từ hoàn chỉnh",
        icon = Icons.Default.Dashboard,
        color = GameModeSyllable,
        hasHardMode = false
    ),
    SENTENCE_ORDER(
        id = "sentence_order",
        title = "Xếp câu",
        description = "Sắp xếp các từ thành câu hoàn chỉnh",
        icon = Icons.Default.FormatListNumbered,
        color = GameModeSentence,
        hasHardMode = false
    ),
    WORD_SEARCH(
        id = "word_search",
        title = "Tìm từ",
        description = "Tìm các từ ẩn trong ma trận chữ cái",
        icon = Icons.Default.Search,
        color = GameModeWordSearch,
        hasHardMode = false
    ),
    FILL_BLANK(
        id = "fill_blank",
        title = "Điền từ còn thiếu",
        description = "Điền từ thích hợp vào chỗ trống",
        icon = Icons.Default.Edit,
        color = GameModeFillBlank,
        hasHardMode = true
    ),
    WORD_CHAIN(
        id = "word_chain",
        title = "Nối từ",
        description = "Nối từ bằng chữ cái cuối",
        icon = Icons.Default.SwapHoriz,
        color = GameModeWordChain,
        hasHardMode = false
    );

    companion object {
        fun fromId(id: String): GameMode? = entries.find { it.id == id }

        fun getAllModes(): List<GameMode> = entries.toList()
    }
}

/**
 * Difficulty mode for games that support it
 */
enum class DifficultyMode {
    EASY,   // Multiple choice
    HARD    // Type the answer
}

