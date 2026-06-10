package com.example.vietforces.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.vietforces.data.manager.AiManager
import com.example.vietforces.data.manager.SettingsManager
import com.example.vietforces.data.model.AiCallResult
import com.example.vietforces.data.remote.OpenAiClient
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Feedback type for mascot reactions
 */
enum class MascotFeedbackType {
    CORRECT,    // User answered correctly
    WRONG,      // User answered incorrectly
    IDLE        // Default idle state with tips
}

/**
 * Singleton to manage mascot feedback from anywhere in the app.
 *
 * Shows an instant hardcoded reaction for snappy UX, then optionally upgrades it
 * to a natural AI-generated message (§5). AI calls are throttled and only fire
 * when the user enabled the toggle and a key is configured (§12).
 */
object MascotFeedbackManager {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    var currentFeedback by mutableStateOf<MascotFeedbackType>(MascotFeedbackType.IDLE)
        private set
    var feedbackMessage by mutableStateOf("")
        private set
    var showFeedback by mutableStateOf(false)
        private set

    /** Bumped on every change (new feedback or AI replacement) so the UI can
     *  reset its auto-hide timer. */
    var feedbackStamp by mutableIntStateOf(0)
        private set

    private var aiJob: Job? = null
    private var lastAiAt: Long = 0L
    private const val AI_THROTTLE_MS = 6000L

    /**
     * Show feedback when user answers correctly.
     * @param context optional detail (e.g. the word) so AI can react specifically.
     */
    fun showCorrectFeedback(context: String? = null) {
        feedbackMessage = getCorrectMessage()
        currentFeedback = MascotFeedbackType.CORRECT
        showFeedback = true
        feedbackStamp++
        maybeFetchAi(wasCorrect = true, context = context)
    }

    /**
     * Show feedback when user answers incorrectly.
     * @param context optional detail (e.g. correct answer vs user answer).
     */
    fun showWrongFeedback(context: String? = null) {
        feedbackMessage = getWrongMessage()
        currentFeedback = MascotFeedbackType.WRONG
        showFeedback = true
        feedbackStamp++
        maybeFetchAi(wasCorrect = false, context = context)
    }

    /**
     * Hide feedback and return to idle
     */
    fun hideFeedback() {
        showFeedback = false
        currentFeedback = MascotFeedbackType.IDLE
        aiJob?.cancel()
    }

    /** Replace the instant message with an AI reaction when allowed + not throttled. */
    private fun maybeFetchAi(wasCorrect: Boolean, context: String?) {
        if (!AiManager.aiMascotEnabled || !OpenAiClient.isConfigured()) return
        val now = System.currentTimeMillis()
        if (now - lastAiAt < AI_THROTTLE_MS) return
        lastAiAt = now

        val situation = buildString {
            append(if (wasCorrect) "Người học vừa trả lời ĐÚNG. " else "Người học vừa trả lời SAI. ")
            if (!context.isNullOrBlank()) append(context)
        }

        aiJob?.cancel()
        aiJob = scope.launch {
            when (val r = AiManager.mascotReact(situation)) {
                is AiCallResult.Success ->
                    if (showFeedback && r.data.message.isNotBlank()) {
                        feedbackMessage = r.data.message
                        feedbackStamp++ // reset the auto-hide timer for the new text
                    }
                is AiCallResult.Error -> { /* keep the instant fallback */ }
            }
        }
    }

    /**
     * Get random praise message for correct answers
     */
    private fun getCorrectMessage(): String {
        return listOf(
            "Giỏi lắm! 🎉",
            "Xuất sắc! ⭐",
            "Đúng rồi! 👏",
            "Tuyệt vời! 🌟",
            "Chính xác! ✅",
            "Hay lắm! 🔥",
            "Đỉnh của chóp! 💯",
            "Pro quá! 🏆",
            "Thông minh! 🧠",
            "Siêu ghê! 💪",
            "Quá đỉnh! 🚀",
            "Chuẩn luôn! ✨",
            "Perfect! 💎",
            "Ò ó o! Giỏi! 🐓",
            "Gà Vàng nể! 👑"
        ).random()
    }

    /**
     * Get random teasing/encouraging message for wrong answers
     */
    private fun getWrongMessage(): String {
        return listOf(
            "Ối! Sai rồi 😅",
            "Hehe, thử lại! 🙃",
            "Gần đúng rồi! 😬",
            "Ui, chưa phải! 🫣",
            "Sai bét! 🤭",
            "Ê, tập trung! 😤",
            "Suy nghĩ lại đi! 🤔",
            "Không phải đâu! ❌",
            "Cố lên nào! 💪",
            "Lần sau nhé! 😉",
            "Đừng nản! 🌈",
            "Ò ó o, sai! 🐓",
            "Gà cũng biết! 🐔",
            "Học thêm đi! 📚",
            "Bình tĩnh nào! 😌"
        ).random()
    }
}

/**
 * Position state for the draggable mascot
 */
object MascotPositionState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var isInitialized by mutableStateOf(false)
    var isOnLeftEdge by mutableStateOf(false) // Track which edge mascot is on
}

/**
 * Draggable Mascot that can be moved around the screen
 * Appears on all screens as a floating companion
 * Always snaps to left or right edge
 */
@Composable
fun DraggableMascot(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Get sizes from SettingsManager
    val mascotSizeMultiplier = SettingsManager.mascotSizeMultiplier
    val textSizeMultiplier = SettingsManager.mascotTextSizeMultiplier

    val mascotSize = SettingsManager.getMascotSize().dp
    val mascotSizePx = with(density) { mascotSize.toPx() }
    val emojiSize = SettingsManager.getMascotEmojiSize().sp
    val textSize = SettingsManager.getMascotTextSize().sp

    val edgePadding = 10f
    val topPadding = 100f // Avoid status bar area
    val bottomPadding = 50f // Above bottom nav completely

    // Initialize position at bottom-right corner
    LaunchedEffect(Unit) {
        if (!MascotPositionState.isInitialized) {
            MascotPositionState.offsetX = screenWidthPx - mascotSizePx - edgePadding
            MascotPositionState.offsetY = screenHeightPx - mascotSizePx - bottomPadding - 100f
            MascotPositionState.isOnLeftEdge = false
            MascotPositionState.isInitialized = true
        }
    }

    // Bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Is being dragged state
    var isDragging by remember { mutableStateOf(false) }

    // Speech bubble visibility for idle tips
    var showIdleBubble by remember { mutableStateOf(true) }

    // Current idle tip message
    var currentIdleTip by remember { mutableStateOf(getRandomMascotTip()) }

    // Get feedback state from manager - direct access to trigger recomposition
    val showFeedback = MascotFeedbackManager.showFeedback
    val feedbackMessage = MascotFeedbackManager.feedbackMessage
    val feedbackType = MascotFeedbackManager.currentFeedback
    val feedbackStamp = MascotFeedbackManager.feedbackStamp

    // Always keep rooster emoji - don't change on feedback
    val mascotEmoji = "🐓"

    // Compute background color based on current state
    val mascotBackgroundColor = when {
        isDragging -> VietYellow
        showFeedback && feedbackType == MascotFeedbackType.CORRECT -> PrimaryGreen
        showFeedback && feedbackType == MascotFeedbackType.WRONG -> VietRed
        else -> RoosterOrange
    }

    // Auto-hide feedback after delay. Keyed on feedbackStamp so an incoming AI
    // message resets the timer (gives the AI reaction its full time on screen).
    LaunchedEffect(feedbackStamp, showFeedback) {
        if (showFeedback) {
            kotlinx.coroutines.delay(3500)
            MascotFeedbackManager.hideFeedback()
        }
    }

    // Auto-hide idle bubble after delay
    LaunchedEffect(showIdleBubble, showFeedback) {
        if (showIdleBubble && !showFeedback) {
            kotlinx.coroutines.delay(5000)
            showIdleBubble = false
        }
    }

    // Show idle bubble when drag ends (only if no feedback showing)
    LaunchedEffect(isDragging) {
        if (!isDragging && !showFeedback) {
            currentIdleTip = getRandomMascotTip()
            showIdleBubble = true
        }
    }

    // Determine bubble color based on feedback type
    val bubbleColor = when (feedbackType) {
        MascotFeedbackType.CORRECT -> Color(0xFFE8F5E9) // Light green
        MascotFeedbackType.WRONG -> Color(0xFFFFEBEE) // Light red
        MascotFeedbackType.IDLE -> Color.White
    }

    val bubbleBorderColor = when (feedbackType) {
        MascotFeedbackType.CORRECT -> PrimaryGreen
        MascotFeedbackType.WRONG -> VietRed
        MascotFeedbackType.IDLE -> Color.Transparent
    }

    // Determine which message to show
    val shouldShowBubble = showFeedback || (showIdleBubble && !isDragging && !showFeedback)
    val displayMessage = if (showFeedback) feedbackMessage else currentIdleTip

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(Float.MAX_VALUE) // Always on top of everything
    ) {
        // Speech bubble - position based on which edge mascot is on
        if (shouldShowBubble) {
            val bubbleWidth = 130.dp
            val bubbleWidthPx = with(density) { bubbleWidth.toPx() }

            // If mascot is on left edge, show bubble on right side of mascot
            // If mascot is on right edge, show bubble on left side of mascot
            val bubbleOffsetX = if (MascotPositionState.isOnLeftEdge) {
                MascotPositionState.offsetX + mascotSizePx + 8f
            } else {
                MascotPositionState.offsetX - bubbleWidthPx - 8f
            }

            // Clamp Y position so bubble doesn't go off screen
            val bubbleOffsetY = (MascotPositionState.offsetY - 10f)
                .coerceIn(topPadding, screenHeightPx - 100f)

            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            bubbleOffsetX.roundToInt().coerceIn(8, (screenWidthPx - bubbleWidthPx - 8f).toInt()),
                            bubbleOffsetY.roundToInt()
                        )
                    }
                    .width(bubbleWidth * textSizeMultiplier.coerceIn(0.8f, 1.5f)),
                shape = RoundedCornerShape(12.dp),
                color = bubbleColor,
                shadowElevation = 8.dp,
                border = if (showFeedback) androidx.compose.foundation.BorderStroke(2.dp, bubbleBorderColor) else null
            ) {
                Text(
                    text = displayMessage,
                    modifier = Modifier.padding(10.dp),
                    fontSize = if (showFeedback) (textSize.value * 1.2f).sp else textSize,
                    fontWeight = if (showFeedback) FontWeight.Bold else FontWeight.Normal,
                    color = if (showFeedback) {
                        when (feedbackType) {
                            MascotFeedbackType.CORRECT -> PrimaryGreen
                            MascotFeedbackType.WRONG -> VietRed
                            else -> TextPrimary
                        }
                    } else TextPrimary,
                    lineHeight = (textSize.value * 1.5f).sp
                )
            }
        }

        // Mascot
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        MascotPositionState.offsetX.roundToInt(),
                        (MascotPositionState.offsetY + if (!isDragging) bounceOffset else 0f).roundToInt()
                    )
                }
                .size(mascotSize)
                .shadow(
                    elevation = if (isDragging) 16.dp else 8.dp,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(mascotBackgroundColor)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            showIdleBubble = false
                        },
                        onDragEnd = {
                            isDragging = false

                            // Determine which edge is closer and snap to it
                            val centerX = MascotPositionState.offsetX + mascotSizePx / 2
                            val isCloserToLeft = centerX < screenWidthPx / 2

                            // Snap to the closer edge
                            MascotPositionState.offsetX = if (isCloserToLeft) {
                                edgePadding // Left edge
                            } else {
                                screenWidthPx - mascotSizePx - edgePadding // Right edge
                            }

                            // Update edge state
                            MascotPositionState.isOnLeftEdge = isCloserToLeft

                            // Clamp Y position
                            MascotPositionState.offsetY = MascotPositionState.offsetY
                                .coerceIn(topPadding, screenHeightPx - mascotSizePx - bottomPadding)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            MascotPositionState.offsetX += dragAmount.x
                            MascotPositionState.offsetY += dragAmount.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mascotEmoji,
                    fontSize = emojiSize
                )
                if (isDragging) {
                    Text(
                        text = "~",
                        fontSize = (emojiSize.value * 0.4f).sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Get random mascot tip/message for idle state
 */
private fun getRandomMascotTip(): String {
    return listOf(
        "Kéo tôi đi đâu cũng được! 🐓",
        "Học mỗi ngày nhé! 📚",
        "Cố lên bạn ơi! 💪",
        "Gà Vàng ở đây! 🌟",
        "Chạm để nghe tip! ✨",
        "Bạn giỏi lắm! 👏",
        "Tiếp tục phát huy! 🔥",
        "Ò ó o! 🐔",
        "Học vui vẻ nhé! 😄",
        "Từ mới chờ bạn! 📖"
    ).random()
}

