package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.AppNotification
import com.example.vietforces.data.manager.NotificationManager
import com.example.vietforces.data.manager.NotificationType
import com.example.vietforces.ui.theme.*

/**
 * Notification Screen - shows list of notifications
 * No bottom navigation, has back button to return to home
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit
) {
    val notifications = NotificationManager.notifications

    // Mark all as read when entering screen
    LaunchedEffect(Unit) {
        NotificationManager.markAllAsRead()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top bar with back button
        TopAppBar(
            title = {
                Text(
                    text = "Thông báo",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = VietRed
                    )
                }
            },
            actions = {
                if (notifications.isNotEmpty()) {
                    IconButton(onClick = { NotificationManager.clearAll() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Xóa tất cả",
                            tint = TextSecondary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        if (notifications.isEmpty()) {
            // Empty state
            EmptyNotificationState()
        } else {
            // Notification list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(notification = notification)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Empty notification state
 */
@Composable
private fun EmptyNotificationState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔔",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Không có thông báo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Các thông báo về thành tích sẽ xuất hiện ở đây",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * Single notification item
 */
@Composable
private fun NotificationItem(
    notification: AppNotification
) {
    val backgroundColor = when (notification.type) {
        NotificationType.ELO_MILESTONE -> VietYellow.copy(alpha = 0.1f)
        NotificationType.RANK_UP -> PrimaryGreen.copy(alpha = 0.1f)
        NotificationType.ACHIEVEMENT -> PrimaryBlue.copy(alpha = 0.1f)
        NotificationType.STREAK -> RoosterOrange.copy(alpha = 0.1f)
    }

    val accentColor = when (notification.type) {
        NotificationType.ELO_MILESTONE -> VietYellow
        NotificationType.RANK_UP -> PrimaryGreen
        NotificationType.ACHIEVEMENT -> PrimaryBlue
        NotificationType.STREAK -> RoosterOrange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = notification.icon,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = NotificationManager.getFormattedTime(notification.timestamp),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Type badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (notification.type) {
                            NotificationType.ELO_MILESTONE -> "Cột mốc Elo"
                            NotificationType.RANK_UP -> "Thăng hạng"
                            NotificationType.ACHIEVEMENT -> "Thành tích"
                            NotificationType.STREAK -> "Chuỗi ngày"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                }
            }
        }
    }
}

