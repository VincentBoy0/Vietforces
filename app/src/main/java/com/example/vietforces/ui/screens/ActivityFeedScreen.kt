package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vietforces.ui.components.EmptyStateComposable
import com.example.vietforces.ui.components.ErrorStateComposable
import com.example.vietforces.ui.components.ShimmerBox
import com.example.vietforces.ui.theme.VietRed
import com.example.vietforces.ui.viewmodel.ActivityEventItem
import com.example.vietforces.ui.viewmodel.ActivityFeedViewModel
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Activity feed screen showing daily_completion and elo_milestone events
 * from users the current user follows (SOCIAL-04).
 *
 * States:
 *  - Loading: 4 ShimmerBox placeholders (UX-02)
 *  - Empty:   EmptyStateComposable with "Tìm bạn bè" CTA (UX-01)
 *  - Error:   ErrorStateComposable with retry (UX-04)
 *  - Success: LazyColumn of ActivityEventCard items, each tappable → PublicProfileScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedScreen(
    onBackClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: ActivityFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Hoạt động bạn bè 📰",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại"
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.retry() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Làm mới"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    // UX-02: shimmer loading placeholders matching card height
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(4) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    // UX-04: error state with retry
                    ErrorStateComposable(
                        message = uiState.error!!,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.events.isEmpty() -> {
                    // UX-01: empty state — user follows nobody or no recent activity
                    EmptyStateComposable(
                        illustration = "📭",
                        message = "Chưa có hoạt động nào\nTheo dõi bạn bè để thấy hoạt động của họ!",
                        ctaText = "Tìm bạn bè",
                        onCtaClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.events, key = { it.id }) { event ->
                            ActivityEventCard(
                                event = event,
                                username = uiState.usernameMap[event.userId] ?: "Người dùng",
                                onNavigateToProfile = onNavigateToProfile
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single feed card showing the event emoji, username, action description, and relative time.
 * Tapping navigates to that user's PublicProfileScreen.
 */
@Composable
private fun ActivityEventCard(
    event: ActivityEventItem,
    username: String,
    onNavigateToProfile: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onNavigateToProfile(event.userId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Event type icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VietRed.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (event.eventType) {
                        "daily_completion" -> "📅"
                        "elo_milestone" -> "⚡"
                        else -> "🎯"
                    },
                    fontSize = 20.sp
                )
            }

            // Content column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Event description
                when (event.eventType) {
                    "daily_completion" -> {
                        val challengeDate = event.metadata["challenge_date"]
                            ?.jsonPrimitive?.content ?: ""
                        val eloEarned = event.metadata["elo_earned"]
                            ?.jsonPrimitive?.intOrNull ?: 0
                        Text(
                            text = "đã hoàn thành thách đấu ngày $challengeDate (+$eloEarned ELO 🌟)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    "elo_milestone" -> {
                        val milestoneElo = event.metadata["milestone_elo"]
                            ?.jsonPrimitive?.intOrNull
                        val desc = if (milestoneElo != null) {
                            "vừa đạt $milestoneElo ELO! ⚡"
                        } else {
                            "vừa đạt ELO milestone! ⚡"
                        }
                        Text(
                            text = desc,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "có hoạt động mới",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRelativeTime(event.createdAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Converts an ISO-8601 UTC timestamp to a Vietnamese relative time string.
 *
 * T-05-04-04: wrapped in try/catch to handle malformed timestamps without crashing.
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val secondsAgo = ChronoUnit.SECONDS.between(instant, now)
        when {
            secondsAgo < 60 -> "vừa xong"
            secondsAgo < 3600 -> "${secondsAgo / 60} phút trước"
            secondsAgo < 86400 -> "${secondsAgo / 3600} giờ trước"
            else -> "${ChronoUnit.DAYS.between(instant, now)} ngày trước"
        }
    } catch (_: Exception) {
        ""
    }
}
