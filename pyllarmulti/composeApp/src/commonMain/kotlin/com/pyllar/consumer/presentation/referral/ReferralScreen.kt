package com.pyllar.consumer.presentation.referral

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.platform.PlatformActions
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*

// ==========================================
// CUSTOM DASHED BORDER MODIFIER
// ==========================================

fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 1.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
    cornerRadius: Dp = 12.dp
) = this.drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    val dashLengthPx = dashLength.toPx()
    val gapLengthPx = gapLength.toPx()
    val cornerRadiusPx = cornerRadius.toPx()

    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLengthPx, gapLengthPx), 0f)
        )
    )
}

// ==========================================
// CORE UI COMPOSABLE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    userId: String,
    uiState: ReferralUiState,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onWhatsAppShareClick: () -> Unit = {},
    onWithdrawClick: (Int) -> Unit = {},
    onDismissSuccessMessage: () -> Unit = {},
    onDismissErrorMessage: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val platformActions: PlatformActions = koinInject()

    var showExplainerSheet by remember { mutableStateOf(false) }
    var showWithdrawConfirmSheet by remember { mutableStateOf(false) }
    var showCopiedToast by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf<String?>(null) }
    var showErrorToast by remember { mutableStateOf<String?>(null) }

    // Auto dismiss copied notification
    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            kotlinx.coroutines.delay(2000)
            showCopiedToast = false
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            showSuccessToast = uiState.successMessage
            kotlinx.coroutines.delay(3000)
            showSuccessToast = null
            onDismissSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null && uiState.referralCode.isNotEmpty()) {
            showErrorToast = uiState.errorMessage
            kotlinx.coroutines.delay(3000)
            showErrorToast = null
            onDismissErrorMessage()
        }
    }

    // Premium Color Palette
    val primaryGreen = Color(0xFF0F3E26)   // Rich sleek forest green
    val accentGold = Color(0xFFBD9A3C)     // Warm premium gold
    val accentGoldLight = Color(0xFFFFFBEA) // Light golden background tint
    val backgroundCream = Color(0xFFFAF9F6) // Elegant warm cream/off-white background
    val textDark = Color(0xFF2C221E)        // Deep warm charcoal for premium typography
    val textGray = Color(0xFF757575)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.referral_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            ),
                            color = textDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(Res.string.referral_go_back_desc),
                                tint = textDark
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { showExplainerSheet = true }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = stringResource(Res.string.referral_how_it_works),
                                    tint = primaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.referral_how_it_works),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = primaryGreen
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundCream
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundCream)
                    .padding(paddingValues)
            ) {
                if (uiState.isCodeLoading || uiState.isStatsLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = primaryGreen,
                            strokeWidth = 3.dp
                        )
                    }
                } else if (uiState.errorMessage != null && uiState.referralCode.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "Connection Error",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textDark
                            )
                            Text(
                                text = "Check your Internet connection and try again",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRetryClick,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else if (!uiState.referralEnabled) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "Referrals coming soon",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textDark
                            )
                            Text(
                                text = "The referral programme isn't available for your account yet. Check back soon!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // 1. WALLET CARD SECTION
                        item {
                            WalletCard(
                                balanceCoins = uiState.balanceCoins,
                                lifetimeEarned = uiState.lifetimeEarnedCoins,
                                withdrawn = uiState.withdrawnCoins,
                                minimumCashoutAmount = uiState.minimumCashoutAmount,
                                primaryGreen = primaryGreen,
                                accentGold = accentGold,
                                onWithdrawClick = { showWithdrawConfirmSheet = true }
                            )
                        }

                        // 2. EXPLAINER STEPS (Only visible if 0 coins / First-time state: Scenario A)
                        if (uiState.balanceCoins == 0 && uiState.referredUsers.isEmpty() && uiState.lifetimeEarnedCoins == 0) {
                            item {
                                ExplainerStepsSection(
                                    qualifyingDays = uiState.qualifyingDays,
                                    primaryGreen = primaryGreen,
                                    accentGold = accentGold,
                                    textDark = textDark,
                                    textGray = textGray
                                )
                            }
                        }

                        // 3. SHARE INVITE LINK BOX
                        item {
                            ShareLinkBox(
                                referralCode = uiState.referralCode,
                                primaryGreen = primaryGreen,
                                accentGold = accentGold,
                                accentGoldLight = accentGoldLight,
                                textDark = textDark,
                                textGray = textGray,
                                onShareClick = onShareClick,
                                onWhatsAppShareClick = onWhatsAppShareClick,
                                shareUrl = uiState.shareUrl,
                                shareMessage = uiState.shareMessage
                            )
                        }

                        // 4. MY REFERRALS LIST SECTION
                        item {
                            ShareLinkBoxInvitesHeader(
                                referredUsers = uiState.referredUsers,
                                invitedCount = uiState.invitedCount,
                                earnedCount = uiState.earnedCount,
                                primaryGreen = primaryGreen,
                                accentGold = accentGold,
                                textDark = textDark,
                                textGray = textGray
                            )
                        }

                        // 5. WITHDRAWAL HISTORY SECTION (Only visible if history exists)
                        if (uiState.withdrawalHistory.isNotEmpty()) {
                            item {
                                WithdrawalHistorySection(
                                    history = uiState.withdrawalHistory,
                                    primaryGreen = primaryGreen,
                                    textDark = textDark,
                                    textGray = textGray
                                )
                            }
                        }

                        // 6. FAQS SECTION
                        item {
                            ReferralFaqsSection(
                                qualifyingDays = uiState.qualifyingDays,
                                primaryGreen = primaryGreen,
                                accentGold = accentGold,
                                textDark = textDark,
                                textGray = textGray
                            )
                        }

                        // Extra spacing at the bottom to ensure FAQs scroll completely above the floating bottom button
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }

                    // FLOATING PRIMARY BOTTOM BUTTON
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.98f),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .border(
                                BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = onShareClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.referral_btn_invite_first_friend),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // EXPLAINER OVERLAY (PREVIEW & COMPOSABLE SAFE bottom sheet)
        // ==========================================
        AnimatedVisibility(
            visible = showExplainerSheet,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showExplainerSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                        .animateContentSize(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundCream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        // Explainer Header with Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.referral_explainer_header_part1),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    ),
                                    color = textDark
                                )
                                Text(
                                    text = stringResource(Res.string.referral_explainer_header_part2),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = accentGold
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { showExplainerSheet = false },
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.referral_close_sheet_desc),
                                        tint = textDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                ExplainerStepsDetailedList(
                                    qualifyingDays = uiState.qualifyingDays,
                                    primaryGreen = primaryGreen,
                                    accentGold = accentGold,
                                    textDark = textDark,
                                    textGray = textGray
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            item {
                                KeyTermsCard(
                                    qualifyingDays = uiState.qualifyingDays,
                                    primaryGreen = primaryGreen,
                                    accentGold = accentGold,
                                    textDark = textDark,
                                    textGray = textGray,
                                    minimumCashoutAmount = uiState.minimumCashoutAmount
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showExplainerSheet = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 15.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.referral_got_it),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // ==========================================
        // WITHDRAW CONFIRMATION OVERLAY
        // ==========================================
        val withdrawAmount = (uiState.balanceCoins / uiState.minimumCashoutAmount) * uiState.minimumCashoutAmount
        AnimatedVisibility(
            visible = showWithdrawConfirmSheet,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showWithdrawConfirmSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundCream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.referral_withdraw_sheet_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textDark
                            )
                        )

                        Text(
                            text = stringResource(Res.string.referral_withdrawing_header),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = textGray
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                ),
                                color = primaryGreen,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = withdrawAmount.toString(),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 48.sp
                                ),
                                color = primaryGreen
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(Res.string.referral_coins_in_wallet),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textGray
                                    )
                                    Text(
                                        text = stringResource(Res.string.referral_coins_format).replace("%1\$d", uiState.balanceCoins.toString()),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textDark
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(Res.string.referral_withdrawing_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textGray
                                    )
                                    Text(
                                        text = stringResource(Res.string.referral_minus_coins_format).replace("%1\$d", withdrawAmount.toString()),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(Res.string.referral_balance_left),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textGray
                                    )
                                    Text(
                                        text = stringResource(Res.string.referral_coins_format).replace("%1\$d", (uiState.balanceCoins - withdrawAmount).toString()),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textDark
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                showWithdrawConfirmSheet = false
                                onWithdrawClick(withdrawAmount)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 15.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.referral_btn_confirm_withdrawal),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        TextButton(onClick = { showWithdrawConfirmSheet = false }) {
                            Text(
                                text = stringResource(Res.string.referral_btn_cancel),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textGray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Custom Copied Snackbar Toast
        AnimatedVisibility(
            visible = showCopiedToast,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C221E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(Res.string.referral_link_copied_toast),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                }
            }
        }

        if (uiState.isWithdrawLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = primaryGreen,
                    strokeWidth = 3.dp
                )
            }
        }

        // Custom Success Toast
        AnimatedVisibility(
            visible = showSuccessToast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3E26)), // Forest green for success
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = showSuccessToast ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                }
            }
        }

        // Custom Error Toast
        AnimatedVisibility(
            visible = showErrorToast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)), // Red for error
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = showErrorToast ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
fun WalletCard(
    balanceCoins: Int,
    lifetimeEarned: Int,
    withdrawn: Int,
    minimumCashoutAmount: Int,
    primaryGreen: Color,
    accentGold: Color,
    onWithdrawClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C3320), Color(0xFF04190F))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.referral_wallet_header),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFFA5D6A7)
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = balanceCoins.toString(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 42.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = stringResource(Res.string.referral_coins_label),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = stringResource(Res.string.referral_in_pocket_format).replace("%1\$d", balanceCoins.toString()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFFE4F3BE)
                    )
                }

                Image(
                    painter = painterResource(Res.drawable.gold_icon),
                    contentDescription = stringResource(Res.string.referral_gold_coins_desc),
                    modifier = Modifier.size(80.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val progress = (balanceCoins.toFloat() / minimumCashoutAmount.toFloat()).coerceAtMost(1.0f)
                val remaining = minimumCashoutAmount - balanceCoins

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF05150C))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF8F00))
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (balanceCoins >= minimumCashoutAmount) stringResource(Res.string.referral_available_to_withdraw) else stringResource(Res.string.referral_more_to_withdraw_format).replace("%1\$d", remaining.toString()),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (balanceCoins >= minimumCashoutAmount) Color(0xFF81C784) else Color(0xFFFFF59D)
                    )
                    Text(
                        text = "$balanceCoins / $minimumCashoutAmount",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFA5D6A7)
                    )
                }
            }

            if (balanceCoins >= minimumCashoutAmount) {
                Button(
                    onClick = onWithdrawClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentGold),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Withdraw Coins",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1A1A)
                    )
                }
            }

            if (lifetimeEarned > 0 || withdrawn > 0) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.referral_lifetime_earned),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                            color = Color(0xFF81C784)
                        )
                        Text(
                            text = stringResource(Res.string.referral_coins_format).replace("%1\$d", lifetimeEarned.toString()),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(Res.string.referral_withdrawn),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                            color = Color(0xFF81C784)
                        )
                        Text(
                            text = stringResource(Res.string.referral_coins_format).replace("%1\$d", withdrawn.toString()),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExplainerStepsSection(
    qualifyingDays: Int,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.referral_earn_first_100),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = primaryGreen
            )

            val subtitleRes = if (qualifyingDays == 1) {
                Res.string.referral_subtitle_one_day
            } else {
                Res.string.referral_subtitle_multi_day
            }
            val subtitleText = if (qualifyingDays == 1) {
                stringResource(subtitleRes)
            } else {
                stringResource(subtitleRes).replace("%1\$d", qualifyingDays.toString())
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textDark
            )

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)

            Text(
                text = stringResource(Res.string.referral_how_you_earn_discipline),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textDark
            )

            ExplainerStepsList(
                qualifyingDays = qualifyingDays,
                primaryGreen = primaryGreen,
                accentGold = accentGold,
                textDark = textDark,
                textGray = textGray
            )
        }
    }
}

@Composable
fun ExplainerStepsList(
    qualifyingDays: Int,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepRowInline(
            title = stringResource(Res.string.referral_step1_title),
            description = stringResource(Res.string.referral_step1_desc),
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            icon = { CustomLinkIcon(color = accentGold) }
        )
        StepRowInline(
            title = stringResource(Res.string.referral_step2_title),
            description = stringResource(Res.string.referral_step2_desc),
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            icon = { CustomCalendarIcon(color = accentGold) }
        )
        val step3Desc = if (qualifyingDays == 1) {
            stringResource(Res.string.referral_step3_desc_one_day)
        } else {
            stringResource(Res.string.referral_step3_desc_multi_day).replace("%1\$d", qualifyingDays.toString())
        }
        StepRowInline(
            title = stringResource(Res.string.referral_step3_title),
            description = step3Desc,
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            icon = { CustomCoinIcon(color = accentGold) }
        )
    }
}

@Composable
fun StepRowInline(
    title: String,
    description: String,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color(0xFFFFFBEA),
            border = BorderStroke(1.dp, accentGold)
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = textDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = textGray
            )
        }
    }
}

@Composable
fun ExplainerStepsDetailedList(
    qualifyingDays: Int,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        StepRowDetailed(
            stepNum = "1",
            title = stringResource(Res.string.referral_step1_detailed_title),
            description = stringResource(Res.string.referral_step1_detailed_desc),
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            showConnector = true,
            icon = { CustomLinkIcon(color = accentGold) }
        )
        StepRowDetailed(
            stepNum = "2",
            title = stringResource(Res.string.referral_step2_detailed_title),
            description = stringResource(Res.string.referral_step2_detailed_desc),
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            showConnector = true,
            icon = { CustomDownloadIcon(color = accentGold) }
        )
        val step3Title = if (qualifyingDays == 1) {
            stringResource(Res.string.referral_step3_detailed_title_one_day)
        } else {
            stringResource(Res.string.referral_step3_detailed_title_multi_day).replace("%1\$d", qualifyingDays.toString())
        }
        val step3Desc = if (qualifyingDays == 1) {
            stringResource(Res.string.referral_step3_detailed_desc_one_day)
        } else {
            stringResource(Res.string.referral_step3_detailed_desc_multi_day).replace("%1\$d", qualifyingDays.toString())
        }
        StepRowDetailed(
            stepNum = "3",
            title = step3Title,
            description = step3Desc,
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            showConnector = true,
            icon = { CustomCalendarIcon(color = accentGold) }
        )
        val step4Desc = if (qualifyingDays == 1) {
            stringResource(Res.string.referral_step4_detailed_desc_one_day)
        } else {
            stringResource(Res.string.referral_step4_detailed_desc_multi_day).replace("%1\$d", qualifyingDays.toString())
        }
        StepRowDetailed(
            stepNum = "4",
            title = stringResource(Res.string.referral_step4_detailed_title),
            description = step4Desc,
            primaryGreen = primaryGreen,
            accentGold = accentGold,
            textDark = textDark,
            textGray = textGray,
            showConnector = false,
            icon = { CustomCoinIcon(color = accentGold) }
        )
    }
}

@Composable
fun StepRowDetailed(
    stepNum: String,
    title: String,
    description: String,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color,
    showConnector: Boolean,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFFFFFBEA),
                border = BorderStroke(1.dp, accentGold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            if (showConnector) {
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                ) {
                    val dashLengthPx = 6.dp.toPx()
                    val gapLengthPx = 4.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = accentGold.copy(alpha = 0.4f),
                            start = Offset(size.width / 2, y),
                            end = Offset(size.width / 2, (y + dashLengthPx).coerceAtMost(size.height)),
                            strokeWidth = 2.dp.toPx()
                        )
                        y += dashLengthPx + gapLengthPx
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.referral_step_label_format).replace("%1\$s", stepNum),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = accentGold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = textGray
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Custom step vector drawings to maintain compile safety and premium custom aesthetic
@Composable
fun CustomLinkIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val strokeWidthPx = 2.dp.toPx()
        withTransform({
            rotate(-45f)
        }) {
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * 0.15f, size.height * 0.35f),
                size = Size(size.width * 0.45f, size.height * 0.3f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = strokeWidthPx)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * 0.4f, size.height * 0.35f),
                size = Size(size.width * 0.45f, size.height * 0.3f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}

@Composable
fun CustomDownloadIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val strokeWidthPx = 2.dp.toPx()
        val cx = size.width / 2

        drawLine(
            color = color,
            start = Offset(cx, size.height * 0.1f),
            end = Offset(cx, size.height * 0.65f),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(cx - size.width * 0.2f, size.height * 0.45f),
            end = Offset(cx, size.height * 0.65f),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(cx + size.width * 0.2f, size.height * 0.45f),
            end = Offset(cx, size.height * 0.65f),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.15f, size.height * 0.85f),
            end = Offset(size.width * 0.85f, size.height * 0.85f),
            strokeWidth = strokeWidthPx
        )
    }
}

@Composable
fun CustomCalendarIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        val strokeWidthPx = 2.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.15f, size.height * 0.25f),
            size = Size(size.width * 0.7f, size.height * 0.65f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = strokeWidthPx)
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.15f, size.height * 0.48f),
            end = Offset(size.width * 0.85f, size.height * 0.48f),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.35f, size.height * 0.1f),
            end = Offset(size.width * 0.35f, size.height * 0.25f),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.65f, size.height * 0.1f),
            end = Offset(size.width * 0.65f, size.height * 0.25f),
            strokeWidth = strokeWidthPx
        )
        drawCircle(
            color = color,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width * 0.38f, size.height * 0.68f)
        )
        drawCircle(
            color = color,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width * 0.62f, size.height * 0.68f)
        )
    }
}

@Composable
fun CustomCoinIcon(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(9.dp)) {
        val strokeWidthPx = 2.dp.toPx()
        val cx = size.width / 2
        val cy = size.height / 2

        drawCircle(
            color = color,
            radius = size.width * 0.42f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidthPx)
        )
        drawCircle(
            color = color,
            radius = size.width * 0.3f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidthPx)
        )
        // Mini rupee symbol drawn inside
        val barW = 4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(cx - barW, cy - 3.dp.toPx()),
            end = Offset(cx + barW, cy - 3.dp.toPx()),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(cx - barW, cy - 0.5.dp.toPx()),
            end = Offset(cx + barW, cy - 0.5.dp.toPx()),
            strokeWidth = strokeWidthPx
        )
        // R Curve
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - 2.5.dp.toPx(), cy - 3.dp.toPx()),
            size = Size(5.dp.toPx(), 5.dp.toPx()),
            style = Stroke(width = strokeWidthPx)
        )
        // Slash leg
        drawLine(
            color = color,
            start = Offset(cx - 1.dp.toPx(), cy + 1.dp.toPx()),
            end = Offset(cx + 2.dp.toPx(), cy + 4.dp.toPx()),
            strokeWidth = strokeWidthPx
        )
    }
}

@Composable
fun KeyTermsCard(
    qualifyingDays: Int,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color,
    minimumCashoutAmount: Int
) {
    Text(
        text = stringResource(Res.string.referral_key_terms_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = textDark,
        modifier = Modifier.padding(bottom = 2.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyTermRow(label = stringResource(Res.string.referral_term_reward), value = stringResource(Res.string.referral_term_reward_val), textDark = textDark, textGray = textGray)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
            KeyTermRow(label = stringResource(Res.string.referral_term_coin_value), value = stringResource(Res.string.referral_term_coin_value_val), textDark = textDark, textGray = textGray)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
            val actionVal = if (qualifyingDays == 1) {
                stringResource(Res.string.referral_qualifying_action_one_day)
            } else {
                stringResource(Res.string.referral_qualifying_action_multi_day).replace("%1\$d", qualifyingDays.toString())
            }
            KeyTermRow(label = stringResource(Res.string.referral_term_qualifying_action), value = actionVal, textDark = textDark, textGray = textGray)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
            KeyTermRow(label = stringResource(Res.string.referral_term_withdrawals), value = stringResource(Res.string.referral_term_withdrawals_val).replace("%d", minimumCashoutAmount.toString()), textDark = textDark, textGray = textGray)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
            KeyTermRow(label = stringResource(Res.string.referral_term_coin_expiry), value = stringResource(Res.string.referral_term_coin_expiry_val), textDark = textDark, textGray = textGray)
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
            KeyTermRow(label = stringResource(Res.string.referral_term_cap), value = stringResource(Res.string.referral_term_cap_val), textDark = textDark, textGray = textGray)
        }
    }
}

@Composable
fun KeyTermRow(
    label: String,
    value: String,
    textDark: Color,
    textGray: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textGray,
            modifier = Modifier.weight(0.4f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = textDark,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f, fill = false)
        )
    }
}

@Composable
fun ShareLinkBox(
    referralCode: String,
    primaryGreen: Color,
    accentGold: Color,
    accentGoldLight: Color,
    textDark: Color,
    textGray: Color,
    onShareClick: () -> Unit = {},
    onWhatsAppShareClick: () -> Unit = {},
    shareUrl: String = "",
    shareMessage: String = ""
) {
    val clipboardManager = LocalClipboardManager.current
    val linkText = shareUrl.ifBlank { "pyllar.in/refer?code=$referralCode" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.referral_invite_code),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textDark
            )

            Text(
                text = stringResource(Res.string.referral_code_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = textGray
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .dashedBorder(color = accentGold.copy(alpha = 0.5f), strokeWidth = 1.5.dp, dashLength = 8.dp, gapLength = 6.dp, cornerRadius = 12.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.referral_code_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                            color = textGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = referralCode.uppercase(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = textDark
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clickable {
                                clipboardManager.setText(AnnotatedString(linkText))
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = accentGoldLight,
                        border = BorderStroke(1.dp, accentGold.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(Res.string.referral_copy_code_desc),
                                tint = accentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(Res.string.referral_btn_copy),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = accentGold
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWhatsAppShareClick,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), 
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.referral_btn_whatsapp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textDark)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(Res.string.referral_more_options_desc),
                            tint = textDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(Res.string.referral_btn_more),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShareLinkBoxInvitesHeader(
    referredUsers: List<ReferredUser>,
    invitedCount: Int,
    earnedCount: Int,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color
) {
    var showAllReferrals by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.referral_my_referrals_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textDark
            )

            if (referredUsers.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.referral_my_referrals_summary_format)
                        .replace("%1\$d", invitedCount.toString())
                        .replace("%2\$d", earnedCount.toString()),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textGray
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (referredUsers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.referral_no_invites_yet),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textDark
                        )
                        Text(
                            text = stringResource(Res.string.referral_no_invites_yet_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textGray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val displayedUsers = if (showAllReferrals || referredUsers.size <= 2) {
                        referredUsers
                    } else {
                        referredUsers.take(2)
                    }

                    displayedUsers.forEachIndexed { index, user ->
                        ReferralUserRow(
                            user = user,
                            accentGold = accentGold,
                            textDark = textDark,
                            textGray = textGray
                        )
                        if (index < displayedUsers.size - 1) {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        }
                    }

                    if (referredUsers.size > 2) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        TextButton(
                            onClick = { showAllReferrals = !showAllReferrals },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = primaryGreen)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (showAllReferrals) "See less" else "See all referrals",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = primaryGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralUserRow(
    user: ReferredUser,
    accentGold: Color,
    textDark: Color,
    textGray: Color
) {
    val isEarned = user.statusType == ReferredUserStatus.EARNED
    val avatarBg = if (isEarned) Color(0xFFFFFBEA) else Color(0xFFF9F9F8)
    val avatarBorder = if (isEarned) BorderStroke(1.dp, accentGold) else null

    val rawName = user.name
    val phonePart = if (rawName.contains("...")) {
        rawName.substringAfterLast(" ")
    } else {
        ""
    }
    val namePart = if (phonePart.isNotEmpty()) {
        rawName.substringBeforeLast(" ")
    } else {
        rawName
    }

    val maskedName = if (namePart.isNotEmpty()) {
        val uppercaseName = namePart.uppercase()
        if (uppercaseName.length > 4) "${uppercaseName.take(4)}**" else "${uppercaseName}**"
    } else {
        ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(avatarBg, shape = CircleShape)
                    .then(if (avatarBorder != null) Modifier.border(avatarBorder, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                val initial = namePart.firstOrNull()?.toString()?.uppercase() ?: "F"
                Text(
                    text = initial,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = textDark
                )
            }

            Column {
                Text(
                    text = maskedName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = textDark
                )
                val detailsText = if (phonePart.isNotEmpty()) "+91 $phonePart" else ""
                if (detailsText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detailsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textGray
                    )
                }
            }
        }

        when (user.statusType) {
            ReferredUserStatus.EARNED -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFFF9F9F8), shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val coinsText = user.rewardText.replace(" coins", "").replace(" coin", "")
                    Text(
                        text = coinsText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = textDark
                    )
                }
            }
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color(0xFFF9F9F8), shape = RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(accentGold, shape = CircleShape)
                    )
                    Text(
                        text = "In progress",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textDark
                    )
                }
            }
        }
    }
}

@Composable
fun WithdrawalHistorySection(
    history: List<WithdrawalHistory>,
    primaryGreen: Color,
    textDark: Color,
    textGray: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.referral_withdrawal_history_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textDark
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                history.forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "₹${log.amount}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = textDark
                                )
                                Text(
                                    text = stringResource(Res.string.referral_withdrawn_history_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textGray
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${log.bankDetails} · ${log.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.referral_status_done),
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = log.status,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    if (log != history.last()) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralFaqsSection(
    qualifyingDays: Int,
    primaryGreen: Color,
    accentGold: Color,
    textDark: Color,
    textGray: Color
) {
    val platformActions: PlatformActions = koinInject()
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    val faqs = listOf(
        FaqItem(
            question = stringResource(Res.string.referral_faq_q1),
            answer = if (qualifyingDays == 1) {
                stringResource(Res.string.referral_faq_a1_one_day)
            } else {
                stringResource(Res.string.referral_faq_a1_multi_day).replace("%1\$d", qualifyingDays.toString())
            }
        ),
        FaqItem(
            question = stringResource(Res.string.referral_faq_q2),
            answer = stringResource(Res.string.referral_faq_a2)
        ),
        FaqItem(
            question = if (qualifyingDays == 1) {
                stringResource(Res.string.referral_faq_q3_one_day)
            } else {
                stringResource(Res.string.referral_faq_q3_multi_day).replace("%1\$d", qualifyingDays.toString())
            },
            answer = if (qualifyingDays == 1) {
                stringResource(Res.string.referral_faq_a3_one_day)
            } else {
                stringResource(Res.string.referral_faq_a3_multi_day).replace("%1\$d", qualifyingDays.toString())
            }
        ),
        FaqItem(
            question = stringResource(Res.string.referral_faq_q4),
            answer = if (qualifyingDays == 1) {
                stringResource(Res.string.referral_faq_a4_one_day)
            } else {
                stringResource(Res.string.referral_faq_a4_multi_day).replace("%1\$d", qualifyingDays.toString())
            }
        ),
        FaqItem(
            question = stringResource(Res.string.referral_faq_q5),
            answer = stringResource(Res.string.referral_faq_a5)
        ),
        FaqItem(
            question = stringResource(Res.string.referral_faq_q6),
            answer = stringResource(Res.string.referral_faq_a6)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.referral_faqs_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textDark
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                faqs.forEachIndexed { index, faq ->
                    val isExpanded = expandedIndex == index
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedIndex = if (isExpanded) null else index
                            }
                            .padding(vertical = 4.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.question,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isExpanded) primaryGreen else textDark
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (isExpanded) stringResource(Res.string.referral_faq_collapse_desc) else stringResource(Res.string.referral_faq_expand_desc),
                                tint = if (isExpanded) accentGold else textGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = textGray,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (index == 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val prefix = stringResource(Res.string.referral_faq_tc_prefix)
                                    val linkText = stringResource(Res.string.referral_faq_tc_link)
                                    val suffix = stringResource(Res.string.referral_faq_tc_suffix)

                                    val annotatedText = buildAnnotatedString {
                                        append(prefix)
                                        pushStringAnnotation(tag = "URL", annotation = "https://pyllar.in/referral-terms")
                                        withStyle(
                                            style = SpanStyle(
                                                color = primaryGreen,
                                                fontWeight = FontWeight.SemiBold,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ) {
                                            append(linkText)
                                        }
                                        pop()
                                        append(suffix)
                                    }

                                    ClickableText(
                                        text = annotatedText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = textGray,
                                            lineHeight = 20.sp
                                        ),
                                        onClick = { offset ->
                                            annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                                .firstOrNull()?.let { annotation ->
                                                    platformActions.openUrl(annotation.item)
                                                }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (index != faqs.lastIndex) {
                        HorizontalDivider(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

data class FaqItem(
    val question: String,
    val answer: String
)
