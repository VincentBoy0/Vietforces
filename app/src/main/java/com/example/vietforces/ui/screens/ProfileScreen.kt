package com.example.vietforces.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vietforces.data.manager.ProfileManager
import com.example.vietforces.data.manager.UserProgressManager
import com.example.vietforces.data.model.EloRank
import com.example.vietforces.data.model.EloRankUtils
import com.example.vietforces.data.storage.PreferencesManager
import com.example.vietforces.ui.theme.*

/**
 * Profile Screen with personal information and Elo rating
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Guest mode flags
    val isGuest = remember { PreferencesManager.getIsGuest() }
    val promptAlreadyShown = remember { PreferencesManager.getGuestPromptShown() }
    val totalGamesPlayed = remember {
        UserProgressManager.getAllGameModeStats().values.sumOf { it.gamesPlayed }
    }
    var showGuestDialog by remember { mutableStateOf(false) }

    // Show one-time post-game dialog nudge
    LaunchedEffect(isGuest, totalGamesPlayed, promptAlreadyShown) {
        if (isGuest && totalGamesPlayed >= 1 && !promptAlreadyShown) {
            showGuestDialog = true
        }
    }

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = {
                showGuestDialog = false
                PreferencesManager.setGuestPromptShown(true)
            },
            title = { Text("🎉 Bạn đang tiến bộ!") },
            text = {
                Text("Tạo tài khoản miễn phí để lưu tiến độ lên cloud và không bao giờ mất dữ liệu.")
            },
            confirmButton = {
                TextButton(onClick = {
                    PreferencesManager.setGuestPromptShown(true)
                    showGuestDialog = false
                }) {
                    Text("Để sau", color = TextSecondary)
                }
            },
            dismissButton = {
                Button(onClick = {
                    PreferencesManager.setGuestPromptShown(true)
                    showGuestDialog = false
                    onNavigateToRegister()
                }) {
                    Text("Đăng ký ngay")
                }
            }
        )
    }

    // User data from ProfileManager (persisted in SharedPreferences)
    var fullName by remember { mutableStateOf(ProfileManager.name.ifEmpty { "Chưa cập nhật" }) }
    var phoneNumber by remember { mutableStateOf(ProfileManager.phone.ifEmpty { "Chưa cập nhật" }) }
    var address by remember { mutableStateOf(ProfileManager.address.ifEmpty { "Chưa cập nhật" }) }
    var isEditing by remember { mutableStateOf(false) }

    // Get Elo from UserProgressManager
    val eloRating = UserProgressManager.getEloRating()
    val currentRank = EloRankUtils.getCurrentRank(eloRating)
    val userSession = UserProgressManager.getUserSession()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Hồ sơ",
                    fontWeight = FontWeight.Bold,
                    color = VietRed
                )
            },
            actions = {
                IconButton(onClick = {
                    if (isEditing) {
                        // Save profile data when clicking save
                        ProfileManager.updateName(if (fullName == "Chưa cập nhật") "" else fullName)
                        ProfileManager.updatePhone(if (phoneNumber == "Chưa cập nhật") "" else phoneNumber)
                        ProfileManager.updateAddress(if (address == "Chưa cập nhật") "" else address)
                    }
                    isEditing = !isEditing
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "Lưu" else "Chỉnh sửa",
                        tint = VietRed
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Guest banner card (shown only for guest users)
            if (isGuest) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VietRed.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VietRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "💾 Lưu tiến độ lên cloud",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VietRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Tạo tài khoản miễn phí để không mất dữ liệu",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                PreferencesManager.setGuestPromptShown(true)
                                onNavigateToRegister()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VietRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Đăng ký", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Profile Header Card with Avatar and Elo
            ProfileHeaderCard(
                fullName = fullName,
                eloRating = eloRating,
                currentRank = currentRank
            )

            // Personal Information Card
            PersonalInfoCard(
                fullName = fullName,
                phoneNumber = phoneNumber,
                address = address,
                isEditing = isEditing,
                onFullNameChange = { fullName = it },
                onPhoneNumberChange = { phoneNumber = it },
                onAddressChange = { address = it }
            )

            // Statistics Card
            ProfileStatsCard(userSession = userSession)

            // Rank Progress Card
            RankProgressCard(
                eloRating = eloRating,
                currentRank = currentRank
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Profile Header with Avatar and basic info
 */
@Composable
private fun ProfileHeaderCard(
    fullName: String,
    eloRating: Int,
    currentRank: EloRank
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with rank color border
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(currentRank.color, currentRank.secondaryColor)
                        )
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(VietYellow.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fullName.take(1).uppercase(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentRank.color
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name with rank color
            Text(
                text = fullName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = currentRank.color
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rank title
            Text(
                text = EloRankUtils.getVietnameseRankName(currentRank.name),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = currentRank.color
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Elo rating badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = currentRank.color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Elo: $eloRating",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentRank.color
                )
            }
        }
    }
}


/**
 * Personal Information Card
 */
@Composable
private fun PersonalInfoCard(
    fullName: String,
    phoneNumber: String,
    address: String,
    isEditing: Boolean,
    onFullNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "👤 Thông tin cá nhân",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Full Name
            ProfileInfoField(
                icon = Icons.Outlined.Person,
                label = "Họ và tên",
                value = fullName,
                isEditing = isEditing,
                onValueChange = onFullNameChange
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number
            ProfileInfoField(
                icon = Icons.Outlined.Phone,
                label = "Số điện thoại",
                value = phoneNumber,
                isEditing = isEditing,
                onValueChange = onPhoneNumberChange
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Address
            ProfileInfoField(
                icon = Icons.Outlined.LocationOn,
                label = "Địa chỉ",
                value = address,
                isEditing = isEditing,
                onValueChange = onAddressChange
            )
        }
    }
}

/**
 * Profile info field (editable/readonly)
 */
@Composable
private fun ProfileInfoField(
    icon: ImageVector,
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = VietRed,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VietRed,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    singleLine = true
                )
            } else {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * Profile Statistics Card
 */
@Composable
private fun ProfileStatsCard(userSession: com.example.vietforces.data.model.UserSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📊 Thống kê học tập",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem(
                    value = userSession.totalExercisesCompleted.toString(),
                    label = "Bài tập",
                    icon = "📝"
                )
                ProfileStatItem(
                    value = "${(userSession.accuracyRate * 100).toInt()}%",
                    label = "Chính xác",
                    icon = "🎯"
                )
                ProfileStatItem(
                    value = userSession.currentStreak.toString(),
                    label = "Chuỗi ngày",
                    icon = "🔥"
                )
            }
        }
    }
}

/**
 * Individual stat item for profile
 */
@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
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

/**
 * Rank Progress Card - shows progress to next rank
 */
@Composable
private fun RankProgressCard(
    eloRating: Int,
    currentRank: EloRank
) {
    val ranks = EloRankUtils.getEloRanks()
    val currentIndex = ranks.indexOfFirst { eloRating in it.minElo..it.maxElo }
    val nextRank = if (currentIndex < ranks.size - 1) ranks[currentIndex + 1] else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📈 Tiến độ thăng hạng",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (nextRank != null) {
                val progressInRank = eloRating - currentRank.minElo
                val rankRange = currentRank.maxElo - currentRank.minElo + 1
                val progress = progressInRank.toFloat() / rankRange

                // Current rank -> Next rank
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = EloRankUtils.getVietnameseRankName(currentRank.name),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentRank.color
                        )
                        Text(
                            text = "${currentRank.minElo}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = EloRankUtils.getVietnameseRankName(nextRank.name),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = nextRank.color
                        )
                        Text(
                            text = "${nextRank.minElo}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEEEEEE))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(currentRank.color, nextRank.color)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Points needed
                val pointsNeeded = nextRank.minElo - eloRating
                Text(
                    text = "Còn $pointsNeeded điểm để đạt ${EloRankUtils.getVietnameseRankName(nextRank.name)}",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Already at max rank
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bạn đã đạt hạng cao nhất!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentRank.color
                    )
                    Text(
                        text = "Huyền thoại Vietforces",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

