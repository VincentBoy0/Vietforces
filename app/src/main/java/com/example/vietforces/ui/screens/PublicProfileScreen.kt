package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.ui.components.EmptyStateComposable
import com.example.vietforces.ui.components.ErrorStateComposable
import com.example.vietforces.ui.components.ShimmerBox
import com.example.vietforces.ui.theme.VietRed
import com.example.vietforces.ui.theme.VietYellow
import com.example.vietforces.ui.viewmodel.PublicProfileViewModel

/**
 * Read-only public profile screen for a given [userId].
 *
 * Displays:
 * - Avatar initial, username (bold)
 * - ELO rank badge with tier color and Vietnamese name
 * - Stats row: ELO ⚡ | Streak 🔥 | Games 🎮
 * - Follow / Unfollow button (hidden when viewing own profile)
 *
 * Loading state: ShimmerBox placeholders
 * Error state: ErrorStateComposable with retry
 *
 * Navigation: TopAppBar back arrow → [onBackClick]
 *
 * Security: No edit controls rendered — no guest banner. Read-only composable.
 * T-05-03-05: LaunchedEffect(userId) ensures loadProfile() fires only on userId change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    // T-05-03-05: key=userId means loadProfile only fires when userId changes
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.profile?.username ?: "Hồ sơ",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    // Shimmer placeholders while data loads (UX-02/03)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    // Error state with retry (UX-04)
                    ErrorStateComposable(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadProfile(userId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.profile == null -> {
                    // No data / user not found
                    EmptyStateComposable(
                        illustration = "👤",
                        message = "Không tìm thấy người dùng này",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    val profile = uiState.profile!!
                    val rank = EloRankUtils.getCurrentRank(profile.eloScore)
                    val tierColor = rank.color
                    val tierName = EloRankUtils.getVietnameseRankName(rank.name)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar circle with first letter of username
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(VietYellow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = profile.username,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // ELO rank tier badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tierColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = tierName,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tierColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                emoji = "⚡",
                                value = "${profile.eloScore}",
                                label = "ELO"
                            )
                            StatItem(
                                emoji = "🔥",
                                value = "${profile.streakCount} ngày",
                                label = "Streak"
                            )
                            StatItem(
                                emoji = "🎮",
                                value = "${profile.totalGames}",
                                label = "Ván đấu"
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Follow / Unfollow button — hidden when viewing own profile
                        if (!uiState.isCurrentUser) {
                            if (uiState.isFollowing) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleFollow() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("✓ Đang theo dõi")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.toggleFollow() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = VietRed)
                                ) {
                                    Text("+ Theo dõi", color = Color.White)
                                }
                            }
                        }

                        // Error toast for follow/unfollow failures
                        if (uiState.error != null && !uiState.isLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single stat column: emoji + value (bold) + label (secondary color).
 * Used in the stats row for ELO, Streak, Games.
 */
@Composable
private fun StatItem(
    emoji: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = "$emoji $value",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
