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
import com.example.vietforces.ui.components.EmptyStateComposable
import com.example.vietforces.ui.components.GameModeCard
import com.example.vietforces.ui.components.ShimmerBox
import com.example.vietforces.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onDailyChallengeClick: () -> Unit = {},
    onWritingPracticeClick: () -> Unit = {},
    onLearningPathClick: () -> Unit = {},
    onRoleplayClick: () -> Unit = {},
    onSearchFriendsClick: () -> Unit = {},
    onGameModeClick: (GameMode) -> Unit
) {
    // Get real data from UserProgressManager
    val currentStreak = UserProgressManager.getCurrentStreak()
    val eloRating = UserProgressManager.getEloRating()
    val notificationCount = NotificationManager.unreadCount

    val gameModes = remember { GameMode.getAllModes() }

    // UX-02: brief shimmer loading state before rendering game mode cards
    var cardsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150L)
        cardsLoaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                containerColor = MaterialTheme.colorScheme.surface
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

            // AI Learning Path banner
            item {
                LearningPathBanner(onClick = onLearningPathClick)
            }

            // Daily Challenge card
            item {
                DailyChallengeCard(onClick = onDailyChallengeClick)
            }

            // Find Friends card (SOCIAL-01, SOCIAL-02)
            item {
                FindFriendsCard(onClick = onSearchFriendsClick)
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

            // Game Mode Cards — shimmer placeholders while loading (UX-02, UX-03)
            if (!cardsLoaded) {
                items(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(bottom = 4.dp)
                    )
                }
            } else if (gameModes.isEmpty()) {
                // UX-01: empty state when no game modes are available
                item {
                    EmptyStateComposable(
                        illustration = "🎮",
                        message = "Chưa có chế độ chơi nào.\nVui lòng thử lại sau.",
                        ctaText = null,
                        onCtaClick = null
                    )
                }
            } else {
                items(gameModes) { gameMode ->
                    GameModeCard(
                        gameMode = gameMode,
                        onClick = { onGameModeClick(gameMode) }
                    )
                }
            }

            // Roleplay conversation tutor (AI chat)
            item {
                RoleplayCard(onClick = onRoleplayClick)
            }

            // Writing Practice (AI-graded) — separate from game modes
            item {
                WritingPracticeCard(onClick = onWritingPracticeClick)
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
            containerColor = MaterialTheme.colorScheme.surface
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
private fun FindFriendsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B5E20).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔍", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tìm bạn bè 👥",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Tìm kiếm và theo dõi người dùng khác",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun DailyChallengeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VietRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎯", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thử thách hôm nay 🎯",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VietRed
                )
                Text(
                    text = "Hoàn thành để nhận +50 ELO",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = VietRed
            )
        }
    }
}

@Composable
private fun LearningPathBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VietRed),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lộ trình học cá nhân",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AI phân tích điểm yếu & gợi ý nên luyện gì",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun RoleplayCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1565C0).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Nhập vai",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Nhập vai trò chuyện",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = VietYellow.copy(alpha = 0.25f)) {
                        Text(
                            text = "AI",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8D6E00)
                        )
                    }
                }
                Text(
                    text = "Đi chợ, gọi món, bắt xe... chat với AI bằng tiếng Việt",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun WritingPracticeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VietRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Luyện viết",
                    tint = VietRed,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Luyện viết ngắn",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = VietYellow.copy(alpha = 0.25f)) {
                        Text(
                            text = "AI",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8D6E00)
                        )
                    }
                }
                Text(
                    text = "Viết đoạn văn, AI chấm và sửa lỗi cho bạn",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
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

