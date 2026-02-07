package com.example.vietforces.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.vietforces.ui.theme.*

/**
 * Mascot conditions for showing different messages
 */
enum class MascotMood {
    HAPPY,          // User is doing well
    ENCOURAGING,    // User needs motivation
    CELEBRATING,    // User achieved something
    REMINDING,      // Reminder to practice
    GREETING        // Default greeting
}

/**
 * Rooster mascot component that gives tips and encouragement
 */
@Composable
fun RoosterMascot(
    mood: MascotMood = MascotMood.GREETING,
    userName: String = "bạn",
    streak: Int = 0,
    modifier: Modifier = Modifier
) {
    val message = remember(mood, streak) {
        getMascotMessage(mood, userName, streak)
    }

    // Simple bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VietYellow.copy(alpha = 0.2f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rooster emoji/icon
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(RoosterOrange),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🐓",
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Speech bubble
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            Text(
                text = "Gà Vàng nói:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RoosterRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Get message based on mascot mood and conditions
 */
private fun getMascotMessage(mood: MascotMood, userName: String, streak: Int): String {
    return when (mood) {
        MascotMood.GREETING -> listOf(
            "Chào $userName! Hôm nay học gì nào? 📚",
            "Xin chào! Sẵn sàng học tiếng Việt chưa? 🎯",
            "Ò ó o! Chào buổi sáng $userName! ☀️"
        ).random()

        MascotMood.HAPPY -> listOf(
            "Giỏi lắm $userName! Tiếp tục phát huy nhé! 🌟",
            "Tuyệt vời! Bạn đang tiến bộ rất nhanh! 🚀",
            "Xuất sắc! Gà Vàng rất tự hào về bạn! 👏"
        ).random()

        MascotMood.ENCOURAGING -> listOf(
            "Đừng bỏ cuộc $userName! Cố lên nào! 💪",
            "Sai là cách để học! Thử lại nhé! 🔄",
            "Mỗi ngày một chút, bạn sẽ giỏi thôi! ⭐"
        ).random()

        MascotMood.CELEBRATING -> when {
            streak >= 30 -> "WOW! $streak ngày liên tục! Bạn là siêu sao! 🏆"
            streak >= 7 -> "Tuyệt vời! $streak ngày streak! Tiếp tục nhé! 🔥"
            streak >= 3 -> "Đã $streak ngày rồi! Cố thêm chút nữa! ⚡"
            else -> "Chúc mừng thành tích mới! 🎉"
        }

        MascotMood.REMINDING -> listOf(
            "Đã lâu không thấy $userName! Học chút đi! 📖",
            "Ò ó o! Đến giờ luyện tập rồi! ⏰",
            "Mỗi ngày 5 phút thôi, được không $userName? 🙏"
        ).random()
    }
}

/**
 * Compact version of mascot for smaller spaces
 */
@Composable
fun RoosterMascotCompact(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VietYellow.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🐓", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = TextPrimary
        )
    }
}

