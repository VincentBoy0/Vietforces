package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.vietforces.data.manager.NotificationManager
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.GameMode
import com.example.vietforces.ui.components.GameModeCard
import com.example.vietforces.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onGameModeClick: (GameMode) -> Unit
) {
    // Get real data from UserProgressManager
    val currentStreak = UserProgressManager.getCurrentStreak()
    val eloRating = UserProgressManager.getEloRating()
    val notificationCount = NotificationManager.unreadCount

    val gameModes = remember { GameMode.getAllModes() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top Bar with Profile and Notification
        TopAppBar(
            title = {
                Text(
                    text = "Vietforces",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            navigationIcon = {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VietYellow)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = VietRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            actions = {
                // Notification Bell with Badge
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(
                                containerColor = VietRed
                            ) {
                                Text(
                                    text = notificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable(onClick = onNotificationClick)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF616161),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak and Elo Summary Card
            item {
                StatsCard(
                    streak = currentStreak,
                    elo = eloRating
                )
            }


            // Section Title
            item {
                Text(
                    text = "Chọn chế độ luyện tập",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Game Mode Cards
            items(gameModes) { gameMode ->
                GameModeCard(
                    gameMode = gameMode,
                    onClick = { onGameModeClick(gameMode) }
                )
            }

            // Bottom spacing for navigation bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun StatsCard(
    streak: Int,
    elo: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Streak Section
            StatItem(
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFFF9800),
                value = streak.toString(),
                label = "Chuỗi ngày",
                backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f)
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(Color(0xFFE0E0E0))
            )

            // Elo Section
            StatItem(
                icon = Icons.Default.Star,
                iconColor = VietYellow,
                value = elo.toString(),
                label = "Điểm Elo",
                backgroundColor = VietYellow.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    backgroundColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

