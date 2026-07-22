package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.data.repository.LeaderboardEntry
import com.example.vietforces.data.repository.LeaderboardTab
import com.example.vietforces.ui.components.EmptyStateComposable
import com.example.vietforces.ui.components.ErrorStateComposable
import com.example.vietforces.ui.components.ShimmerBox
import com.example.vietforces.ui.theme.VietRed
import com.example.vietforces.ui.viewmodel.LeaderboardViewModel

// Medal colours — rank 1/2/3 only (LEAD-03)
private val GoldColor = Color(0xFFFFD700)
private val SilverColor = Color(0xFFB0B0B8)
private val BronzeColor = Color(0xFFCD7F32)

/**
 * Leaderboard screen showing Top-50 players with real-time updates.
 *
 * Features:
 *  - Tab row for All-time (elo_score) vs This-week (weekly_elo) views (LEAD-03)
 *  - Shimmer placeholders while loading (UX-02/UX-03)
 *  - Error state with retry (UX-04)
 *  - Own-rank sticky footer when user is outside the top 50 (LEAD-02)
 *  - Gold/Silver/Bronze styling for top 3 (🥇🥈🥉)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top App Bar ──────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = "Bảng Xếp Hạng",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // ── Tab Row ──────────────────────────────────────────────────────────
        val selectedTabIndex = when (uiState.selectedTab) {
            LeaderboardTab.ALL_TIME -> 0
            LeaderboardTab.THIS_WEEK -> 1
            LeaderboardTab.FRIENDS -> 2
        }
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { viewModel.selectTab(LeaderboardTab.ALL_TIME) },
                text = { Text("All-time 🏆") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { viewModel.selectTab(LeaderboardTab.THIS_WEEK) },
                text = { Text("Tuần này 📅") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { viewModel.selectTab(LeaderboardTab.FRIENDS) },
                text = { Text("Bạn bè 👥") }
            )
        }

        // ── Content ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                // Loading: 3 shimmer placeholders (UX-02/UX-03)
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Error state with retry (UX-04)
                uiState.error != null -> {
                    ErrorStateComposable(
                        message = uiState.error!!,
                        onRetry = { viewModel.selectTab(uiState.selectedTab) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Empty state (UX-01)
                uiState.entries.isEmpty() -> {
                    if (uiState.selectedTab == LeaderboardTab.FRIENDS) {
                        EmptyStateComposable(
                            illustration = "👥",
                            message = "Bạn chưa theo dõi ai.\nTìm bạn bè để xem bảng xếp hạng.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyStateComposable(
                            illustration = "🏆",
                            message = "Chưa có dữ liệu xếp hạng",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Success: ranked list + optional own-rank footer
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            items = uiState.entries,
                            key = { _, entry -> entry.userId }
                        ) { index, entry ->
                            LeaderboardRow(
                                rank = index + 1,
                                entry = entry,
                                isMe = entry.userId == uiState.myEntry?.userId
                            )
                        }

                        // Own-rank footer — only when user is outside the top 50 (LEAD-02)
                        if (uiState.myEntry != null && uiState.myRank == -1) {
                            item(key = "own-rank-divider") {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "• • •",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Vị trí của bạn (ngoài top 50)",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            item(key = "own-rank-row") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(VietRed.copy(alpha = 0.08f))
                                ) {
                                    LeaderboardRow(
                                        rank = -1,
                                        entry = uiState.myEntry!!,
                                        isMe = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single row in the leaderboard list.
 *
 * @param rank   1-based position in the top-50 list; -1 for the own-rank footer.
 * @param entry  The leaderboard data to display.
 * @param isMe   True when this row belongs to the authenticated user.
 */
@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isMe: Boolean) {
    val rankColor = when (rank) {
        1 -> GoldColor
        2 -> SilverColor
        3 -> BronzeColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }
    val tierColor = EloRankUtils.getRankColor(entry.eloScore)
    val tierName = EloRankUtils.getVietnameseRankName(
        EloRankUtils.getCurrentRank(entry.eloScore).name
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) VietRed.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (rank in 1..3) 3.dp else if (isMe) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank column (medal emoji or "#N" text)
            Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                if (medalEmoji != null) {
                    Text(text = medalEmoji, fontSize = 22.sp)
                } else {
                    Text(
                        text = if (rank > 0) "#$rank" else "?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = rankColor
                    )
                }
            }

            // Username
            Text(
                text = entry.username,
                modifier = Modifier.weight(1f),
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isMe) VietRed else MaterialTheme.colorScheme.onSurface
            )

            // Rank-tier badge (colour from EloRankUtils)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = tierColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = tierName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tierColor
                )
            }

            // ELO score (bold, coloured by rank)
            Text(
                text = "${entry.eloScore}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = rankColor
            )
        }
    }
}
