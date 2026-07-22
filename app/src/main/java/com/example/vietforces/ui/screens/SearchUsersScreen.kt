package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.data.repository.UserSearchResult
import com.example.vietforces.ui.components.EmptyStateComposable
import com.example.vietforces.ui.components.ShimmerBox
import com.example.vietforces.ui.theme.VietRed
import com.example.vietforces.ui.viewmodel.SearchUiState
import com.example.vietforces.ui.viewmodel.SocialViewModel

/**
 * Screen for searching users by username and following/unfollowing them.
 *
 * Features:
 *  - OutlinedTextField search bar with debounced query (300 ms)
 *  - LazyColumn of user cards: avatar emoji 🎓 + username + ELO tier chip
 *  - Follow/Unfollow OutlinedButton bound to SocialViewModel.followingIds
 *  - Empty state, searching shimmer, error state
 *  - Back arrow in TopAppBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersScreen(
    viewModel: SocialViewModel = hiltViewModel(),
    onUserClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()

    var queryText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tìm bạn bè",
                        fontWeight = FontWeight.Bold,
                        color = VietRed
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Search Field ─────────────────────────────────────────────────
            OutlinedTextField(
                value = queryText,
                onValueChange = { newValue ->
                    queryText = newValue
                    viewModel.search(newValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Nhập tên người dùng…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                leadingIcon = {
                    Text(text = "🔍", fontSize = 20.sp, modifier = Modifier.padding(start = 4.dp))
                }
            )

            // ── Content Area ─────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchState) {
                    is SearchUiState.Idle -> {
                        EmptyStateComposable(
                            illustration = "👥",
                            message = "Nhập tên để tìm kiếm người dùng",
                            ctaText = null,
                            onCtaClick = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is SearchUiState.Searching -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(4) {
                                ShimmerBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    is SearchUiState.Results -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp, vertical = 8.dp
                            )
                        ) {
                            items(
                                items = state.users,
                                key = { it.userId }
                            ) { user ->
                                UserSearchCard(
                                    user = user,
                                    isFollowing = user.userId in followingIds,
                                    onFollowToggle = { viewModel.toggleFollow(user.userId) },
                                    onCardClick = { onUserClick(user.userId) }
                                )
                            }
                        }
                    }

                    is SearchUiState.Empty -> {
                        EmptyStateComposable(
                            illustration = "🔍",
                            message = "Không tìm thấy người dùng nào.\nThử tên khác nhé!",
                            ctaText = null,
                            onCtaClick = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is SearchUiState.Error -> {
                        EmptyStateComposable(
                            illustration = "⚠️",
                            message = state.msg,
                            ctaText = null,
                            onCtaClick = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing a single user search result.
 *
 * @param user          The search result data.
 * @param isFollowing   Whether the current user is already following this user.
 * @param onFollowToggle Called when the Follow/Unfollow button is tapped.
 * @param onCardClick    Called when the card itself is tapped (navigate to profile).
 */
@Composable
private fun UserSearchCard(
    user: UserSearchResult,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onCardClick: () -> Unit
) {
    val tierColor = EloRankUtils.getRankColor(user.eloScore)
    val tierName = EloRankUtils.getVietnameseRankName(
        EloRankUtils.getCurrentRank(user.eloScore).name
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar emoji
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VietRed.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎓", fontSize = 22.sp)
            }

            // Username + ELO chip
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ELO chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tierColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tierName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                    }
                    Text(
                        text = "${user.eloScore} ELO",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Follow / Unfollow button
            OutlinedButton(
                onClick = onFollowToggle,
                modifier = Modifier.width(100.dp)
            ) {
                Text(
                    text = if (isFollowing) "Bỏ theo dõi" else "Theo dõi",
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = if (isFollowing) Color.Gray else VietRed
                )
            }
        }
    }
}
