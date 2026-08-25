package com.pyllar.consumer.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import com.pyllar.consumer.presentation.ui.theme.getCursiveFontFamily
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.DrawableResource
import pyllar.composeapp.generated.resources.*

import com.pyllar.consumer.domain.storage.SessionStore

// ─── DESIGN TOKENS (PREMIUM V2 THEME) ──────────────────────────────────────────
private val WarmCreamBackground = Color(0xFFFBF9F4) // Sleek creamy warm white
private val PremiumDarkBrown   = Color(0xFF3E2723) // Rich bronze-brown for headers/text
private val LuxuryGold         = Color(0xFFD4AF37) // Premium gold accent
private val LuxurySilver       = Color(0xFF9E9E9E) // Premium silver accent
private val AccentGoldDark     = Color(0xFF8B6B25) // Deep gold for borders and subtitles
private val AccentGreen        = Color(0xFF2E7D32) // Emerald green for gains/positive metrics

private val GreenDark       = Color(0xFF1A7A42)

private val VolatilityRed      = Color(0xFFC62828) // Deep red for volatility labels
private val SecondaryBronze    = Color(0xFF6D4C41) // Secondary brown/bronze
private val SubtleBorderColor  = Color(0xFFEFEBE9) // Ultra light brown-gray for borders

private val GoldCardColorsV2    = listOf(Color(0xFFFFFDF7), Color(0xFFFFF9E6), Color(0xFFFFF3CD))
private val SilverCardColorsV2  = listOf(Color(0xFFFCFCFD), Color(0xFFF5F5F7), Color(0xFFEAECEE))
private val SavingsCardColorsV2 = listOf(Color(0xFFF9FBF7), Color(0xFFF1F8E9), Color(0xFFE8F5E9))
private val MarketExplorerCardColorsV2 = listOf(Color(0xFFF4F9F7), Color(0xFFE3F1EC), Color(0xFFD0E7DF))

// ─── CUSTOM SWALLOWTAIL SHAPE FOR BANNER ──────────────────────────────────────
class SwallowtailShape(private val cutWidth: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutPx = with(density) { cutWidth.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - cutPx, size.height / 2f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            lineTo(cutPx, size.height / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

// ─── SHIMMER ANIMATION FOR BORDERS ────────────────────────────────────────────
@Composable
fun Modifier.shimmerBorderV2(
    colors: List<Color> = listOf(
        Color(0xFFD4AF37).copy(alpha = 0.0f),
        Color(0xFFFFD700).copy(alpha = 0.3f),
        Color(0xFFFFF9C4).copy(alpha = 0.6f),
        Color(0xFFFFD700).copy(alpha = 0.3f),
        Color(0xFFD4AF37).copy(alpha = 0.0f)
    ),
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 20.dp
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_v2")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_rotation_v2"
    )

    return this.drawWithContent {
        drawContent()
        val strokePx = borderWidth.toPx()
        val cornerPx = cornerRadius.toPx()
        val rect = androidx.compose.ui.geometry.Rect(
            left = strokePx / 2f,
            top = strokePx / 2f,
            right = size.width - strokePx / 2f,
            bottom = size.height - strokePx / 2f
        )
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx)
                )
            )
        }
        rotate(rotation, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawPath(
                path = path,
                brush = Brush.sweepGradient(colors),
                style = Stroke(width = strokePx)
            )
        }
    }
}

// ─── SCREEN COMPOSABLE ────────────────────────────────────────────────────────
@Composable
fun InitialDashboardScreenV2(
    userId: String = "",
    onNavigateToOnboarding: (goalId: String, userId: String) -> Unit,
    onNavigateToRoute: (screen: String, preVerificationId: String?) -> Unit = { _, _ -> },
    onNavigateToHelp: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val viewModel: InitialDashboardViewModel = koinInject()
    val platformActions: PlatformActions = koinInject()
    val sessionStore: SessionStore = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val goalsState by viewModel.goalsState.collectAsState()

    val allGoals = remember(goalsState.primaryGoals, goalsState.recommendedGoals) {
        (goalsState.primaryGoals + goalsState.recommendedGoals)
            .filter { it.category.uppercase() in listOf("GOLD", "SILVER", "SAVINGS_PLUS", "MARKET_EXPLORER", "INNOVATION") }
            .sortedBy {
                when (it.category.uppercase()) {
                    "GOLD" -> 1
                    "SILVER" -> 2
                    "INNOVATION" -> 3
                    "MARKET_EXPLORER" -> 4
                    "SAVINGS_PLUS" -> 5
                    else -> 6
                }
            }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InitialDashboardV2")
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = "Are you sure you want to log out from this device?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        PlatformAnalyticsLogger.logEvent(
                            name = "user_logged_out_initial_dashboard",
                            params = mapOf(
                                "screen_name" to "InitialDashboardV2",
                                "user_id" to userId
                            )
                        )
                        coroutineScope.launch {
                            try {
                                sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.LAST_SCREEN, "")
                                sessionStore.logout()
                            } catch (_: Exception) {}
                            onLogout()
                        }
                    }
                ) {
                    Text(
                        text = "Logout",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    InitialDashboardContentV2(
        goals = allGoals,
        growthData = goalsState.growthData,
        isLoading = goalsState.isLoading,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        onNavigateToHelp = onNavigateToHelp,
        onLogoutClick = { showLogoutDialog = true },
        onGoalClick = { goalId ->
            if (isSubmitting || userId.isBlank()) return@InitialDashboardContentV2
            isSubmitting = true
            errorMessage = null
            coroutineScope.launch {
                try {
                    val result = viewModel.selectGoal(
                        userId = userId,
                        goalId = goalId,
                        currentScreen = com.pyllar.consumer.navigation.ScreenNames.ONBOARDING_GOALS_V3
                    )
                    when (result) {
                        is Resource.Success -> {
                            val nextScreen = result.navigation?.nextScreen
                            val userPurposeId = result.data?.userPurposeId
                            if (!nextScreen.isNullOrBlank()) {
                                platformLog("🚀 Server-driven navigation to: $nextScreen (purposeId: $userPurposeId)")
                                onNavigateToRoute(nextScreen, userPurposeId)
                            } else {
                                platformLog("⚠️ No nextScreen in response, falling back to default onboarding")
                                onNavigateToOnboarding(goalId, userId)
                            }
                        }
                        is Resource.Error -> {
                            errorMessage = (result.message ?: "Unable to select goal").toUserFriendlyErrorMessage()
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    errorMessage = (e.message ?: "Something went wrong").toUserFriendlyErrorMessage()
                } finally {
                    isSubmitting = false
                }
            }
        }
    )
}

// ─── MAIN CONTENT ─────────────────────────────────────────────────────────────
@Composable
private fun InitialDashboardContentV2(
    goals: List<InvestmentGoal>,
    growthData: Map<String, BucketGrowthData>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onNavigateToHelp: () -> Unit,
    onLogoutClick: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    val cursiveFont = getCursiveFontFamily()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Centered Top Header Badge
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Pyllar ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF11381E), letterSpacing = (-0.5).sp)
                            Text(text = "Money", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = LuxuryGold, letterSpacing = (-0.5).sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LanguageLetterButton(textColor = GreenDark)
                            Spacer(modifier = Modifier.width(4.dp))

                            // Logout icon button
                            IconButton(
                                onClick = onLogoutClick,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = GreenDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Help text action
                            Text(
                                text = stringResource(Res.string.help),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                ),
                                modifier = Modifier
                                    .clickable { onNavigateToHelp() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.small_steps),
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF11381E),
                            letterSpacing = (-0.5).sp
                        )
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontFamily = cursiveFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = LuxuryGold)) {
                                append(stringResource(Res.string.big_dreams))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(Res.drawable.gold_silver),
                        contentDescription = stringResource(Res.string.gold_and_silver_bars),
                        modifier = Modifier
                            .size(140.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Swallowtail-style Announcement Ribbon
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Horizontal line running behind the ribbon from edge to edge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        LuxuryGold.copy(alpha = 0.0f),
                                        LuxuryGold.copy(alpha = 0.5f),
                                        LuxuryGold.copy(alpha = 0.0f)
                                     )
                                )
                            )
                    )
                    
                    // Ribbon itself
                    val ribbonGradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFF11381E), Color(0xFF0A2213))
                    )
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(36.dp)
                            .background(
                                brush = ribbonGradient,
                                shape = SwallowtailShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(LuxuryGold.copy(alpha = 0.6f), AccentGoldDark.copy(alpha = 0.6f))
                                ),
                                shape = SwallowtailShape(12.dp)
                            )
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.tag_0_gst),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                            Text(
                                text = stringResource(Res.string.no_lock_in),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // Stat Highlight Panel
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+33.0%",
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                )
                                Text(
                                    text = stringResource(Res.string.gold_3y_cagr),
                                    style = TextStyle(fontSize = 11.sp, color = SecondaryBronze)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(SubtleBorderColor)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "+46.7%",
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                )
                                Text(
                                    text = stringResource(Res.string.silver_3y_cagr),
                                    style = TextStyle(fontSize = 11.sp, color = SecondaryBronze)
                                )
                            }
                        }
                    }
                }
            }

            // Elegant separation line
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(SubtleBorderColor)
                    )
                    Text(
                        text = " ✦ ",
                        color = LuxuryGold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(SubtleBorderColor)
                    )
                }
            }

            // Plan list section heading
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.dashboard_start_growing_wealth),
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenDark,
                            letterSpacing = (-0.5).sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.choose_goal_subtitle),
                        style = TextStyle(fontSize = 13.sp, color = PremiumDarkBrown.copy(alpha = 0.8f), lineHeight = 19.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }

            // Error Message UI
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LuxuryGold)
                    }
                }
            } else {
                items(goals.size) { index ->
                    val goal = goals[index]
                    InitialGoalCardV2(
                        goal = goal,
                        bucketData = growthData[goal.goalId] ?: growthData[goal.category.lowercase()],
                        enabled = !isSubmitting,
                        onClick = { onGoalClick(goal.goalId) }
                    )
                }
            }

            // Comparison table section
            item {
                ComparisonSection()
            }

            item {
                DashboardTrustFooterV2()
            }

            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        // Fixed Solid White Bottom Panel
        val goldGoal = remember(goals) { goals.find { it.category.uppercase() == "GOLD" } }
        val goldGoalId = goldGoal?.goalId ?: "gold"

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SubtleBorderColor)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFFFFD700).copy(alpha = 0.25f),
                            spotColor = Color(0xFFFFD700).copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .premiumClickableV2(enabled = !isSubmitting, onClick = { onGoalClick(goldGoalId) })
                        .buttonShineEffect(enabled = !isSubmitting),
                    shape = CircleShape,
                    color = Color(0xFF0A2415), // Obsidian green
                    border = BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFD700), Color(0xFF8B6B25)) // Gold gradient border
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Get Started",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LuxuryGold)
            }
        }
    }
}

// ─── PREMIUM V2 INITIAL GOAL CARD ─────────────────────────────────────────────
@Composable
fun InitialGoalCardV2(
    goal: InvestmentGoal,
    bucketData: BucketGrowthData? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = goal.category.uppercase()
    val isGold = category == "GOLD"
    val isSilver = category == "SILVER"
    val isSavings = category == "SAVINGS_PLUS" || category == "SAVINGS"
    val isMarketExplorer = category == "MARKET_EXPLORER"
    val isInnovation = category == "INNOVATION"
    val cursiveFont = getCursiveFontFamily()

    val gradient = Brush.linearGradient(
        colors = when {
            isGold -> GoldCardColorsV2
            isSilver -> SilverCardColorsV2
            isSavings -> SavingsCardColorsV2
            isMarketExplorer -> MarketExplorerCardColorsV2
            isInnovation -> listOf(Color(0xFFFBF8FF), Color(0xFFF5EEFD), Color(0xFFECE0FA))
            else -> SavingsCardColorsV2
        },
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    var cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 6.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.06f)
        )

    // Animated premium borders for precious metals
    cardModifier = when {
        isGold -> cardModifier
            .border(1.dp, AccentGoldDark.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .shimmerBorderV2(
                colors = listOf(
                    Color(0xFFD4AF37).copy(alpha = 0.0f),
                    Color(0xFFFFE082).copy(alpha = 0.3f),
                    Color(0xFFFFFDE7).copy(alpha = 0.6f),
                    Color(0xFFFFE082).copy(alpha = 0.3f),
                    Color(0xFFD4AF37).copy(alpha = 0.0f)
                ), cornerRadius = 20.dp
            )
        isSilver -> cardModifier
            .border(1.dp, SecondaryBronze.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .shimmerBorderV2(
                colors = listOf(
                    Color(0xFFBDBDBD).copy(alpha = 0.0f),
                    Color(0xFFEEEEEE).copy(alpha = 0.4f),
                    Color(0xFFFFFFFF).copy(alpha = 0.7f),
                    Color(0xFFEEEEEE).copy(alpha = 0.4f),
                    Color(0xFFBDBDBD).copy(alpha = 0.0f)
                ), cornerRadius = 20.dp
            )
        isMarketExplorer -> cardModifier.border(1.dp, Color(0xFF0F6B5C).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        isInnovation -> cardModifier.border(1.dp, Color(0xFF7656A8).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
        else -> cardModifier.border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    }

    cardModifier = cardModifier
        .clip(RoundedCornerShape(20.dp))
        .premiumClickableV2(enabled = enabled, onClick = onClick)

    Surface(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = null
    ) {
        var showSavingsPlusInfo by remember { mutableStateOf(false) }
        var showGoldInfo by remember { mutableStateOf(false) }
        var showSilverInfo by remember { mutableStateOf(false) }

        Box(modifier = Modifier.background(gradient)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 56.dp)
            ) {
                // Title and Information Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSavings) {
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(Res.string.intro_goal_savings) + " ")
                                withStyle(SpanStyle(fontFamily = cursiveFont, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(Res.string.plus_label))
                                }
                            },
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PremiumDarkBrown,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showSavingsPlusInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = stringResource(Res.string.content_description_info),
                                tint = PremiumDarkBrown.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (isMarketExplorer) {
                        Text(
                            text = goal.name.ifBlank { "Market Explorer" },
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PremiumDarkBrown,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    } else if (isInnovation) {
                        Text(
                            text = goal.name.ifBlank { "Innovation" },
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PremiumDarkBrown,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    } else {
                        Text(
                            text = if (isGold) stringResource(Res.string.intro_goal_gold) else stringResource(Res.string.intro_goal_silver),
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PremiumDarkBrown,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PremiumDarkBrown.copy(alpha = 0.06f))
                )
                Spacer(modifier = Modifier.height(10.dp))

                // ── Content Row (Divided) ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Daily + Amt
                    Column(
                        modifier = Modifier.weight(0.35f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.daily_label),
                            style = TextStyle(fontSize = 11.sp, color = SecondaryBronze, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = when {
                                isGold || category == "SAVINGS" -> "₹21 - ₹500"
                                isMarketExplorer               -> "₹21 - ₹1000"
                                isInnovation                   -> "₹101 - ₹1000"
                                else                            -> "₹101 - ₹500"
                            },
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PremiumDarkBrown)
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .background(Color.Gray.copy(alpha = 0.15f))
                    )

                    // RIGHT: Benefit text + Info Button
                    Row(
                        modifier = Modifier.weight(0.72f).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (isInnovation) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Key Themes Focus",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF68499A)
                                    )
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF9C27B0), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tech & Internet • Fintech",
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PremiumDarkBrown
                                            )
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF4CAF50), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Auto & Mobility • Industrials",
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PremiumDarkBrown
                                            )
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF2196F3), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Healthcare • Services & Retail",
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PremiumDarkBrown
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            val date = bucketData?.startDate ?: "Jan 2023"
                            val highlightColor = when {
                                isGold           -> AccentGoldDark
                                isSilver         -> SecondaryBronze
                                isMarketExplorer -> Color(0xFF0F6B5C)
                                else             -> AccentGreen
                            }

                            Column(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.daily_since_format, date),
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PremiumDarkBrown
                                    )
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            val hl = SpanStyle(fontWeight = FontWeight.ExtraBold, color = highlightColor, fontSize = 15.sp)
                                            when {
                                                isGold -> {
                                                    val rawVal = bucketData?.accumulatedUnits ?: 15.8
                                                    val roundedVal = (rawVal * 10).toInt() / 10.0
                                                    withStyle(hl) { append("≃${roundedVal}gm") }
                                                    append(" " + stringResource(Res.string.intro_goal_gold))
                                                }
                                                isSilver -> {
                                                    val rawVal = bucketData?.accumulatedUnits ?: 1.24
                                                    val roundedVal = (rawVal * 100).toInt() / 100.0
                                                    withStyle(hl) { append("≃${roundedVal}kg") }
                                                    append(" " + stringResource(Res.string.intro_goal_silver))
                                                }
                                                isSavings || isMarketExplorer -> {
                                                    val rawVal = bucketData?.currentValuation ?: (if (isMarketExplorer) 1.42 else 1.24)
                                                    withStyle(hl) { append("≃₹${rawVal} Lakhs") }
                                                }
                                                else -> ""
                                            }
                                        },
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PremiumDarkBrown
                                        )
                                    )
                                    
                                    if (isGold || isSilver) {
                                        IconButton(
                                            onClick = {
                                                if (isGold) showGoldInfo = true else showSilverInfo = true
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = stringResource(Res.string.content_description_info),
                                                tint = PremiumDarkBrown.copy(alpha = 0.6f),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Growth tag pill ───────────────────────────────────────────
                val tagColor = when {
                    isGold           -> AccentGoldDark
                    isSilver         -> SecondaryBronze
                    isMarketExplorer -> Color(0xFF0F6B5C)
                    isInnovation     -> Color(0xFF68499A)
                    else             -> AccentGreen
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.48f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isInnovation) Icons.Filled.Star else Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                isGold           -> stringResource(Res.string.intro_goal_gold_desc)
                                isSilver         -> stringResource(Res.string.intro_goal_silver_desc)
                                isSavings        -> stringResource(Res.string.up_to_7_returns)
                                isMarketExplorer -> "Growth through diversified equity investing"
                                isInnovation     -> "Focuses on breakthrough & innovative themes"
                                else             -> stringResource(Res.string.grows_market_performance)
                            },
                            style = TextStyle(fontSize = 11.sp, color = tagColor, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Absolute positioned 3D Illustration / Instant Pill / Emoji
            if (isSavings) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        color = WarmCreamBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(Res.string.instant_badge),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            )
                        }
                    }
                }
            } else if (isMarketExplorer) {
                Text(
                    text = "🧭",
                    fontSize = 32.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                )
            } else if (isInnovation) {
                Text(
                    text = "🚀",
                    fontSize = 32.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                )
            } else {
                val iconRes = if (isGold) Res.drawable.goldbar_icon else Res.drawable.silver_icon
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = goal.name,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .size(54.dp)
                        .graphicsLayer(rotationZ = if (isGold) 8f else -5f),
                    contentScale = ContentScale.Fit
                )
            }

            // Right-aligned Chevron indicator centered vertically
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(32.dp)
                    .background(
                        color = when {
                            isGold           -> AccentGoldDark.copy(alpha = 0.12f)
                            isSilver         -> SecondaryBronze.copy(alpha = 0.12f)
                            isMarketExplorer -> Color(0xFF0F6B5C).copy(alpha = 0.12f)
                            else             -> AccentGreen.copy(alpha = 0.12f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = when {
                        isGold           -> AccentGoldDark
                        isSilver         -> SecondaryBronze
                        isMarketExplorer -> Color(0xFF0F6B5C)
                        else             -> AccentGreen
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showSavingsPlusInfo) {
            AlertDialog(
                onDismissRequest = { showSavingsPlusInfo = false },
                title = {
                    Text(
                        text = stringResource(Res.string.savings_plus_info_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.savings_plus_info_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSavingsPlusInfo = false }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        if (showGoldInfo) {
            AlertDialog(
                onDismissRequest = { showGoldInfo = false },
                title = {
                    Text(
                        text = stringResource(Res.string.intro_goal_gold),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.gold_purchasing_power_explanation),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showGoldInfo = false }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }

        if (showSilverInfo) {
            AlertDialog(
                onDismissRequest = { showSilverInfo = false },
                title = {
                    Text(
                        text = stringResource(Res.string.intro_goal_silver),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.silver_purchasing_power_explanation),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showSilverInfo = false }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }
    }
}

// ─── TRUST COMPARISON MATRIX SECTION ──────────────────────────────────────────
@Composable
private fun ComparisonSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(Res.string.compare_physical_gold_silver),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumDarkBrown
            ),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp)
        )

        // Main table Box allowing overlapping elements
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Background Table Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SubtleBorderColor),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // LEFT COLUMN: Features
                    Column(
                        modifier = Modifier.weight(0.44f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(Res.string.features_header).uppercase(),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumDarkBrown.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SubtleBorderColor)
                        )

                        val features = listOf(
                            stringResource(Res.string.tag_0_gst),
                            stringResource(Res.string.sebi_regulated_funds),
                            stringResource(Res.string.no_making_charges),
                            stringResource(Res.string.regulated_storage),
                            stringResource(Res.string.no_lock_in)
                        )
                        features.forEachIndexed { index, feature ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = feature,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PremiumDarkBrown
                                    )
                                )
                            }
                            if (index < features.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(SubtleBorderColor)
                                )
                            }
                        }
                    }

                    // PLACEHOLDER COLUMN for elevated Pyllar Money card overlay
                    Spacer(modifier = Modifier.weight(0.34f))

                    // RIGHT COLUMN: Digital Gold & Silver
                    Column(
                        modifier = Modifier.weight(0.22f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.physical_header),
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumDarkBrown.copy(alpha = 0.7f),
                                    lineHeight = 14.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SubtleBorderColor)
                        )

                        val physicalAvailability = listOf(false, false, false, false, true)
                        physicalAvailability.forEachIndexed { index, isAvailable ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isAvailable) AccentGreen else VolatilityRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (index < physicalAvailability.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(SubtleBorderColor)
                                )
                            }
                        }
                    }
                }
            }

            // ELEVATED PYLLAR MONEY CARD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Spacer(modifier = Modifier.weight(0.44f))

                Card(
                    modifier = Modifier
                        .weight(0.34f)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = false
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFDF5)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFF1E3D3))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Pyllar money",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumDarkBrown
                                )
                            )
                            Text(
                                text = "Gold & Silver",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGoldDark
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF1E3D3))
                        )

                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(Color(0xFFFFFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (index < 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color(0xFFF1E3D3).copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.22f))
            }
        }
    }
}

// ─── TRUST FOOTER V2 ──────────────────────────────────────────────────────────
@Composable
private fun DashboardTrustFooterV2() {
    val creamBg = WarmCreamBackground
    val goldColor = LuxuryGold
    val premiumBrown = PremiumDarkBrown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(creamBg)
            .padding(top = 20.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier
                    .size(40.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = premiumBrown,
                        fontFamily = FontFamily.Serif
                    )
                )
                Text(
                    text = stringResource(Res.string.built_for_everyday_indians),
                    style = TextStyle(
                        fontFamily = getCursiveFontFamily(),
                        color = premiumBrown.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(1.dp)
                .background(goldColor.copy(alpha = 0.3f))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .height(48.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(2.dp)),
            shape = RoundedCornerShape(2.dp),
            color = Color.White,
            border = BorderStroke(1.dp, goldColor.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(goldColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.amfi_registered),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = premiumBrown)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = goldColor, fontWeight = FontWeight.Bold)) {
                            append(stringResource(Res.string.arn_label) + " ")
                        }
                        withStyle(SpanStyle(color = premiumBrown, fontWeight = FontWeight.Bold)) {
                            append(stringResource(Res.string.arn_value))
                        }
                    },
                    style = TextStyle(fontSize = 15.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrustFeatureItemV2(
                title = stringResource(Res.string.secure_title),
                subtitle = stringResource(Res.string.secure_subtitle),
                icon = Icons.Filled.Shield,
                modifier = Modifier.weight(1f)
            )
            TrustFeatureItemV2(
                title = stringResource(Res.string.trusted_title),
                subtitle = stringResource(Res.string.trusted_subtitle),
                icon = Icons.Filled.Verified,
                modifier = Modifier.weight(1f)
            )
            TrustFeatureItemV2(
                title = stringResource(Res.string.transparent_title),
                subtitle = stringResource(Res.string.transparent_subtitle),
                icon = Icons.Filled.Search,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(goldColor.copy(alpha = 0.4f)))
            Text(
                text = stringResource(Res.string.powered_by_leading_amcs),
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = goldColor
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(goldColor.copy(alpha = 0.4f)))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmcLogoItemV2(Res.drawable.axis_lo)
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                AmcLogoItemV2(Res.drawable.invesco)
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                AmcLogoItemV2(Res.drawable.aditya)
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                AmcLogoItemV2(Res.drawable.nippon)
            }
        }
    }
}

@Composable
private fun TrustFeatureItemV2(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val premiumBrown = PremiumDarkBrown

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(35.dp)
                .background(Color(0xFFF1F4E8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = premiumBrown,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = premiumBrown),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = TextStyle(fontSize = 10.sp, color = SecondaryBronze, lineHeight = 13.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AmcLogoItemV2(resource: DrawableResource) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier
            .width(70.dp)
            .height(35.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun Modifier.premiumClickableV2(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed && enabled) 0.97f else 1f, label = "press_scale_v2")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = if (enabled) LocalIndication.current else null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun Modifier.buttonShineEffect(
    enabled: Boolean = true,
    shineColor: Color = Color.White.copy(alpha = 0.35f),
    durationMillis: Int = 4000
): Modifier {
    if (!enabled) return this

    val transition = rememberInfiniteTransition(label = "button_shine")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = durationMillis
                0f at 0 with LinearEasing
                1f at 1200 with LinearEasing
                1f at durationMillis
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "button_shine_progress"
    )

    return this.drawWithContent {
        drawContent()
        
        val width = size.width
        val height = size.height
        
        val shineWidth = 150f
        val startX = -shineWidth
        val endX = width + shineWidth
        val xOffset = startX + (endX - startX) * progress
        
        val shineBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                shineColor.copy(alpha = 0.0f),
                shineColor.copy(alpha = 0.2f),
                shineColor.copy(alpha = 0.6f),
                shineColor.copy(alpha = 0.2f),
                Color.Transparent
            ),
            start = Offset(xOffset - 50f, 0f),
            end = Offset(xOffset + 50f, height)
        )
        
        drawRect(brush = shineBrush)
    }
}
