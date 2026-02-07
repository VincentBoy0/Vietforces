package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.SettingsManager
import com.example.vietforces.ui.theme.*

/**
 * Settings Screen with mascot customization options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Get current settings
    var mascotSizeMultiplier by remember { mutableFloatStateOf(SettingsManager.mascotSizeMultiplier) }
    var mascotTextSizeMultiplier by remember { mutableFloatStateOf(SettingsManager.mascotTextSizeMultiplier) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Cài đặt",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mascot Settings Card
            MascotSettingsCard(
                mascotSizeMultiplier = mascotSizeMultiplier,
                mascotTextSizeMultiplier = mascotTextSizeMultiplier,
                onMascotSizeChange = {
                    mascotSizeMultiplier = it
                    SettingsManager.setMascotSize(it)
                },
                onMascotTextSizeChange = {
                    mascotTextSizeMultiplier = it
                    SettingsManager.setMascotTextSize(it)
                }
            )

            // Preview Card
            MascotPreviewCard(
                mascotSizeMultiplier = mascotSizeMultiplier,
                mascotTextSizeMultiplier = mascotTextSizeMultiplier
            )

            // Reset Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = {
                            SettingsManager.resetToDefaults()
                            mascotSizeMultiplier = 1.0f
                            mascotTextSizeMultiplier = 1.0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VietRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đặt lại mặc định",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // App Info
            AppInfoCard()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Mascot Settings Card
 */
@Composable
private fun MascotSettingsCard(
    mascotSizeMultiplier: Float,
    mascotTextSizeMultiplier: Float,
    onMascotSizeChange: (Float) -> Unit,
    onMascotTextSizeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "🐓 Cài đặt Linh vật",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mascot Size Slider
            SettingSlider(
                title = "Kích thước linh vật",
                value = mascotSizeMultiplier,
                onValueChange = onMascotSizeChange,
                valueRange = 0.5f..2.0f,
                steps = 5,
                valueLabel = "${(mascotSizeMultiplier * 100).toInt()}%",
                icon = "📏"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mascot Text Size Slider
            SettingSlider(
                title = "Kích thước chữ thông báo",
                value = mascotTextSizeMultiplier,
                onValueChange = onMascotTextSizeChange,
                valueRange = 0.5f..2.0f,
                steps = 5,
                valueLabel = "${(mascotTextSizeMultiplier * 100).toInt()}%",
                icon = "🔤"
            )
        }
    }
}

/**
 * Setting Slider Component
 */
@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    icon: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VietRed.copy(alpha = 0.1f)
            ) {
                Text(
                    text = valueLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = VietRed,
                activeTrackColor = VietRed,
                inactiveTrackColor = VietRed.copy(alpha = 0.2f)
            )
        )

        // Size labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Nhỏ",
                fontSize = 10.sp,
                color = TextSecondary
            )
            Text(
                text = "Vừa",
                fontSize = 10.sp,
                color = TextSecondary
            )
            Text(
                text = "Lớn",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Mascot Preview Card
 */
@Composable
private fun MascotPreviewCard(
    mascotSizeMultiplier: Float,
    mascotTextSizeMultiplier: Float
) {
    val mascotSize = (SettingsManager.BASE_MASCOT_SIZE * mascotSizeMultiplier).dp
    val emojiSize = (SettingsManager.BASE_MASCOT_EMOJI_SIZE * mascotSizeMultiplier).sp
    val textSize = (SettingsManager.BASE_MASCOT_TEXT_SIZE * mascotTextSizeMultiplier).sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "👁️ Xem trước",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Mascot preview
                Box(
                    modifier = Modifier
                        .size(mascotSize)
                        .clip(CircleShape)
                        .background(RoosterOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🐓",
                        fontSize = emojiSize
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speech bubble preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Xin chào! Đây là\nxem trước kích thước! 👋",
                        modifier = Modifier.padding(12.dp),
                        fontSize = textSize,
                        color = TextPrimary,
                        lineHeight = (textSize.value * 1.4).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info text
            Text(
                text = "Linh vật: ${mascotSize.value.toInt()}dp • Chữ: ${textSize.value.toInt()}sp",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * App Info Card
 */
@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "ℹ️ Thông tin ứng dụng",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(label = "Phiên bản", value = "1.0.0")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Tên ứng dụng", value = "Vietforces")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Ngôn ngữ", value = "Tiếng Việt")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

