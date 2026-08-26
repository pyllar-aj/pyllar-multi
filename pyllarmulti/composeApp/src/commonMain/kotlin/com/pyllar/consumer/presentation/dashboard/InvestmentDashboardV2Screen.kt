package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.platform.PermissionManager
import kotlin.math.ceil
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import com.pyllar.consumer.presentation.ui.theme.*
import com.pyllar.consumer.presentation.ui.theme.V2Obsidian
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
import com.pyllar.consumer.presentation.ui.theme.V2HelpText
import com.pyllar.consumer.presentation.ui.theme.V2SubtleBorder
import com.pyllar.consumer.domain.storage.SessionStore
import pyllar.composeapp.generated.resources.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import com.pyllar.consumer.presentation.ui.theme.getCursiveFontFamily



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDashboardV2Screen(
    userId: String = "",
    onNavigateToPortfolio: () -> Unit = {},
    onNavigateToGoal: (String, String) -> Unit = { _, _ -> },
    onNavigateToSchemeDetails: (String) -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToReferral: () -> Unit = {},
    onRetryKyc: () -> Unit = {},
    onStartKyc: () -> Unit = {},
    panNumber: String? = null,
    viewModel: InvestmentDashboardV2ViewModel = koinInject(),
    doubtsSurveyViewModel: com.pyllar.consumer.presentation.mutualfund.onboarding.DoubtsSurveyViewModel = koinInject(),
    platformActions: PlatformActions = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    Log.d("InvestmentDashboardV2", "🎨 COMPOSABLE CALLED - userId: '$userId'")

    val dashboardState by viewModel.dashboardState.collectAsState()
    
    var isSurveyDoneOrSkipped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val done = sessionStore.getValue("survey_done_or_skipped") == "true"
        isSurveyDoneOrSkipped = done
    }
    val showSurvey = false

    var isSelectingGoal by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollOffsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 100.dp.roundToPx() }

    val hasMilestone = !dashboardState.isLoading && dashboardState.hasFirstMilestone && dashboardState.milestoneMessage.isNotBlank()
    val isKycPending = dashboardState.kycStatus.equals("PENDING", ignoreCase = true) ||
            dashboardState.kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
            dashboardState.kycStatus.equals("EXPIRED", ignoreCase = true) ||
            // dashboardState.kycStatus.equals("UNLINKED", ignoreCase = true) ||
            dashboardState.kycStatus.equals("REJECTED", ignoreCase = true)
    val hasPendingMandates = dashboardState.fundDetails.any { 
        it.mandateStatus?.contains("PENDING", ignoreCase = true) == true ||
        it.mandateStatus?.contains("SUBMITTED", ignoreCase = true) == true
    }
    val hasStatusCard = !dashboardState.isLoading && (isKycPending || hasPendingMandates || dashboardState.kycStatus.equals("INITIATE", ignoreCase = true))

    var scrollIndex = 3 // 0: Spacer, 1: Spacer + UserHeader, 2: CombinedDashboardCard
    if (hasStatusCard) {
        scrollIndex++
    }
    
    val activeGoalsIndex = scrollIndex
    
    if (!dashboardState.isLoading && dashboardState.primaryGoals.isNotEmpty()) {
        scrollIndex += 1 // Header text "Your Active Goals"
        scrollIndex += dashboardState.primaryGoals.size
    } else if (!dashboardState.isLoading && dashboardState.primaryGoals.isEmpty() && dashboardState.kycStatus.equals("SUCCESS", ignoreCase = true)) {
        scrollIndex += 1 // KycApprovedReadyToInvestCard
    }
    
    if (hasMilestone) {
        scrollIndex++
    }
    val nextGoalsIndex = scrollIndex

    val nextGoals = dashboardState.recommendedGoals

    var manualRetryUserId by remember { mutableStateOf("") }
    var effectiveUserId by remember { mutableStateOf(userId) }
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            effectiveUserId = userId
        } else {
            val sessionUserId = sessionStore.getCurrentUserId()
            if (sessionUserId.isNotBlank()) {
                effectiveUserId = sessionUserId
            }
        }
    }
    val resolvedUserId = if (effectiveUserId.isNotBlank()) effectiveUserId else manualRetryUserId

    val notifyUserIdNotReady: () -> Unit = {
        Log.w("InvestmentDashboardV2", "⚠️ Goal action blocked - effectiveUserId not resolved yet")
        coroutineScope.launch {
            val fetched = runCatching { sessionStore.getCurrentUserId() }.getOrNull().orEmpty()
            if (fetched.isNotBlank()) {
                manualRetryUserId = fetched
                effectiveUserId = fetched
            }
        }
    }

    val handleGoalSelection: (String) -> Unit = { goalId ->
        if (resolvedUserId.isBlank()) {
            notifyUserIdNotReady()
        } else if (!isSelectingGoal) {
            isSelectingGoal = true
            coroutineScope.launch {
                val result = viewModel.initGoalTxn(resolvedUserId, goalId)
                if (result is Resource.Success) {
                    onNavigateToGoal(goalId, dashboardState.kycStatus)
                } else if (result is Resource.Error) {
                    Log.e("InvestmentDashboardV2", "❌ initGoalTxn failed: ${result.message}")
                }
                isSelectingGoal = false
            }
        }
    }

    val handleActiveGoalClick: (InvestmentGoal, Int) -> Unit = { goal, tabIndex ->
        if (resolvedUserId.isBlank()) {
            notifyUserIdNotReady()
        } else if (!isSelectingGoal) {
            isSelectingGoal = true
            coroutineScope.launch {
                val result = viewModel.initGoalTxn(resolvedUserId, goal.goalId)
                if (result is Resource.Success) {
                    val response = result.data
                    if (response != null) {
                        val params = SchemeDetailsParams(
                            isin = goal.isin,
                            folioNumber = goal.folioNo,
                            schemeName = goal.schemeName,
                            currentValue = goal.currentValue,
                            investmentInProgress = goal.investmentInProgressValue,
                            investedAmount = goal.investedAmount,
                            goalName = goal.name,
                            unitsInGm = goal.unitsInGm,
                            category = goal.category,
                            colorTheme = goal.colorTheme,
                            profit = goal.profit,
                            realizedProfit = goal.realizedProfit,
                            unrealizedProfit = goal.unrealizedProfit,
                            redeemableAmount = goal.redeemableAmount,
                            redemptionInProgress = goal.redemptionInProgress,
                            instantRedemptionValue = goal.instantRedemptionValue,
                            selectedTab = tabIndex,
                            userPurposeId = response.userPurposeId
                        )
                        SchemeDetailsParamsManager.set(params)
                        sessionStore.saveValue("scheme_details_params_${goal.goalId}", SchemeDetailsParamsManager.toJson(params))
                        onNavigateToSchemeDetails(goal.goalId)
                    }
                } else if (result is Resource.Error) {
                    Log.e("InvestmentDashboardV2", "❌ initGoalTxn failed: ${result.message}")
                }
                isSelectingGoal = false
            }
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InvestmentDashboardV2")
    }

    LaunchedEffect(resolvedUserId) {
        if (resolvedUserId.isNotBlank()) {
            viewModel.loadDashboardData(resolvedUserId)
        }
    }

    // Check and request notification permission once if never asked before
    LaunchedEffect(dashboardState.isLoading) {
        if (!dashboardState.isLoading) {
            try {
                val hasAsked = sessionStore.getValue("has_asked_notifications") != null
                val isGranted = permissionManager.checkStatus().notificationsGranted
                if (!isGranted && !hasAsked) {
                    Log.d("InvestmentDashboardV2", "User has never been asked for notification permission before, prompting now...")
                    sessionStore.saveValue("has_asked_notifications", "true")
                    permissionManager.requestNotifications()
                }
            } catch (e: Exception) {
                Log.e("InvestmentDashboardV2", "Failed to check or request notification permission", e)
            }
        }
    }

    // Auto-trigger In-App Review after 1 second, once ever on the dashboard (with a 30-day cooldown from other prompts)
    LaunchedEffect(dashboardState.isLoading) {
        if (!dashboardState.isLoading) {
            delay(1000)
            val alreadyPromptedOnDashboard = sessionStore.getValue("dashboard_review_prompted") != null
            if (!alreadyPromptedOnDashboard) {
                val lastPromptTimeStr = sessionStore.getValue("last_review_prompt_time")
                val lastPromptTime = lastPromptTimeStr?.toLongOrNull() ?: 0L
                val currentTime = Clock.System.now().toEpochMilliseconds()
                val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000

                val actualCurrentValue = dashboardState.primaryGoals.sumOf { it.currentValue }
                if (currentTime - lastPromptTime > thirtyDaysInMillis && actualCurrentValue > 0) {
                    platformActions.requestInAppReview(
                        screenName = "InvestmentDashboardV2",
                        silentFallback = true,
                        trigger = "auto"
                    )
                    sessionStore.saveValue("last_review_prompt_time", currentTime.toString())
                    sessionStore.saveValue("dashboard_review_prompted", "true")
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfHeight = size.height / 2f
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    V2Obsidian,
                    Color(0xFF103620), // obsidian soft
                    V2Cream
                ),
                startY = 0f,
                endY = halfHeight
            )
            drawRect(brush = gradient)
            drawRect(
                color = V2Cream,
                topLeft = Offset(0f, halfHeight)
            )
        }

        PullToRefreshBox(
            isRefreshing = dashboardState.isLoading,
            onRefresh = { viewModel.refreshDashboardData(userId) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                item {
                Spacer(modifier = Modifier.height(16.dp))
                UserHeader(
                    userName = dashboardState.userName,
                    isLoading = dashboardState.isLoading,
                    onClick = onNavigateToProfile,
                    onNavigateToHelp = onNavigateToHelp,
                    showMenu = showMenu,
                    onMenuClick = { showMenu = true },
                    onDismissMenu = { showMenu = false },
                    onShareClick = { platformActions.shareText("Start your investment journey with Pyllar! Download now: https://pyllar.in", "Share Pyllar") },
                    onRateUsClick = { platformActions.requestInAppReview() }
                )
            }

            item {
                val goldGoal = dashboardState.primaryGoals.firstOrNull { 
                    it.category.equals("GOLD", ignoreCase = true) 
                }
                val silverGoal = dashboardState.primaryGoals.firstOrNull { 
                    it.category.equals("SILVER", ignoreCase = true) 
                }
                
                CombinedDashboardCard(
                    totalValue = dashboardState.totalValue,
                    profitLoss = dashboardState.profitLoss,
                    profitLossPercentage = dashboardState.profitLossPercentage,
                    goldUnitsInGm = goldGoal?.unitsInGm,
                    silverUnitsInGm = silverGoal?.unitsInGm,
                    isLoading = dashboardState.isLoading,
                    onTotalClick = {
                        coroutineScope.launch {
                            val target = if (nextGoals.isNotEmpty()) nextGoalsIndex else if (dashboardState.primaryGoals.isNotEmpty()) activeGoalsIndex else 0
                            if (target > 0) listState.animateScrollToItem(target, scrollOffset = scrollOffsetPx)
                        }
                    },
                    onGoldClick = {
                        if (goldGoal != null) handleActiveGoalClick(goldGoal, 0)
                        else handleGoalSelection("gold")
                    },
                    onSilverClick = {
                        if (silverGoal != null) handleActiveGoalClick(silverGoal, 0)
                        else handleGoalSelection("silver")
                    }
                )
            }

            if (hasStatusCard) {
                if (showSurvey) {
                    item {
                        DashboardSurveyCard(
                            onSurveyCompleted = {
                                coroutineScope.launch { sessionStore.saveValue("survey_done_or_skipped", "true") }
                                isSurveyDoneOrSkipped = true
                            },
                            onSurveySkipped = {
                                doubtsSurveyViewModel.submit(
                                    screenName = "InvestmentDashboardSurvey",
                                    goalId = dashboardState.primaryGoals.firstOrNull()?.goalId,
                                    selectedOption = "Skipped Survey"
                                )
                                coroutineScope.launch { sessionStore.saveValue("survey_done_or_skipped", "true") }
                                isSurveyDoneOrSkipped = true
                            },
                            onSubmitAnswer = { option, freeText, callback ->
                                doubtsSurveyViewModel.submit(
                                    screenName = "InvestmentDashboardSurvey",
                                    goalId = dashboardState.primaryGoals.firstOrNull()?.goalId,
                                    selectedOption = option,
                                    freeText = freeText,
                                    requestCallback = callback
                                )
                            },
                            platformActions = platformActions
                        )
                    }
                } else if (dashboardState.kycStatus.equals("UNLINKED", ignoreCase = true)) {
                    item {
                        KycAadhaarLinkingRequiredCard(
                            panNumber = panNumber,
                            onLearnMoreClick = {
                                platformActions.openUrl("https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/%20Link%20Aadhaar-faq")
                            }
                        )
                    }
                } else if (dashboardState.kycStatus.equals("IN_PROGRESS", ignoreCase = true)) {
                    item {
                        KycSubmittedAwaitingApprovalCard(
                            onContactSupport = {
                                platformActions.openWhatsApp("917676596301", "Hello, my KYC has been submitted and is currently awaiting approval.")
                            }
                        )
                    }
                } else if (dashboardState.kycStatus.equals("INITIATE", ignoreCase = true)) {
                    item {
                        InitiateKycCard(
                            onStartKyc = onStartKyc
                        )
                    }
                } else {
                    item {
                        StatusInfoCard(
                            kycStatus = dashboardState.kycStatus,
                            mandateStatuses = dashboardState.fundDetails.mapNotNull { it.mandateStatus }.distinct(),
                            onRetryKyc = onRetryKyc
                        )
                    }
                }
            }

            if (!dashboardState.isLoading && dashboardState.primaryGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Your Active Goals",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(dashboardState.primaryGoals) { goal ->
                    PrimaryGoalCard(
                        goal = goal,
                        isLoading = false,
                        onTopCardClick = { handleActiveGoalClick(goal, 0) },
                        onBottomCardClick = { handleActiveGoalClick(goal, 1) }
                    )
                }
                
//                // Promotion Card if there are investments
//                item {
//                    PromotionShareCard(onShareClick = { platformActions.shareText("Join Pyllar and build your wealth! https://pyllar.in", "Share Pyllar") })
//                }
            } else if (!dashboardState.isLoading && dashboardState.primaryGoals.isEmpty()) {
                if (dashboardState.kycStatus.equals("SUCCESS", ignoreCase = true)) {
                    item {
                        KycApprovedReadyToInvestCard(
                            onChooseGoalClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(nextGoalsIndex, scrollOffset = scrollOffsetPx)
                                }
                            }
                        )
                    }
                }
            }

            if (!dashboardState.isLoading && dashboardState.milestoneMessage.isNotBlank() && dashboardState.hasFirstMilestone) {
                item {
                    MilestoneBanner(message = dashboardState.milestoneMessage)
                }
            }

            if (!dashboardState.isLoading && dashboardState.recommendedGoals.isNotEmpty()) {
                val nextGoals = dashboardState.recommendedGoals
                if (nextGoals.isNotEmpty()) {
                    item {
                        NextGoalsSection(
                            goals = nextGoals,
                            onGoalClick = { goalId -> handleGoalSelection(goalId) }
                        )
                    }
                }
            }

            if (!dashboardState.isLoading && dashboardState.referralEnabled) {
                item {
                    ReferAndEarnCard(
                        coinsBalance = 100,
                        onClick = {
                            PlatformAnalyticsLogger.logEvent("referral_card_tapped")
                            onNavigateToReferral()
                        }
                    )
                }
            }



            item {
                DashboardTrustFooter()
            }

        }
        }

        if (showKycPendingBottomSheet) {
            KycPendingBottomSheet(
                onDismiss = { showKycPendingBottomSheet = false },
                onRetryKyc = onRetryKyc,
                kycStatus = dashboardState.kycStatus
            )
        }

        if (isSelectingGoal) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Connection/Network Error Dialog
        dashboardState.errorMessage?.let { errorMsg ->
            val isNetworkError = errorMsg.contains("connect", ignoreCase = true) ||
                    errorMsg.contains("internet", ignoreCase = true) ||
                    errorMsg.contains("network", ignoreCase = true) ||
                    errorMsg.contains("timeout", ignoreCase = true) ||
                    errorMsg.contains("offline", ignoreCase = true)

            AlertDialog(
                onDismissRequest = { viewModel.clearErrorMessage() },
                title = {
                    Text(
                        text = if (isNetworkError) "Network Error" else "Error",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(errorMsg)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearErrorMessage()
                            if (isNetworkError && userId.isNotBlank()) {
                                viewModel.loadDashboardData(userId)
                            }
                        }
                    ) {
                        Text(if (isNetworkError) "Retry" else "OK")
                    }
                },
                dismissButton = if (isNetworkError) {
                    {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("Cancel")
                        }
                    }
                } else null
            )
        }
    }
}

@Composable
fun UserHeader(
    userName: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    onNavigateToHelp: () -> Unit,
    showMenu: Boolean = false,
    onMenuClick: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRateUsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Box(modifier = Modifier.width(120.dp).height(20.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hi ${userName.ifBlank { "User" }}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
//                Text(
//                    text = " >",
//                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
//                    color = Color.White.copy(alpha = 0.6f)
//                )
            }
        }

        TextButton(onClick = onNavigateToHelp) {
            Text("Help", color = Color.White)
        }

        Box {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Rate Us")
                        }
                    },
                    onClick = {
                        onDismissMenu()
                        onRateUsClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Share")
                        }
                    },
                    onClick = {
                        onDismissMenu()
                        onShareClick()
                    }
                )
            }
        }
    }
}

// Brushed metal color palettes — used inside drawBehind with actual size
private val goldMetalColors = listOf(
    Color(0xFFC8892E), // deep warm gold
    Color(0xFFE8C46A), // bright highlight streak
    Color(0xFFC9973A), // mid gold
    Color(0xFFF0D080), // peak shine
    Color(0xFFB8821A), // shadow dip
    Color(0xFFE0B84A), // secondary highlight
    Color(0xFFC9973A)  // back to base
)
private val silverMetalColors = listOf(
    Color(0xFF8A9DB0), // steel blue-grey base
    Color(0xFFC8D8E4), // bright silver highlight
    Color(0xFF7A8FA0), // shadow dip
    Color(0xFFD8E8F0), // peak shine — near white
    Color(0xFF6A8090), // deep shadow
    Color(0xFFB8CCD8), // soft highlight
    Color(0xFF8A9DB0)  // back to base
)
private const val GOLD_BRUSH_ALPHA = 10 / 255f
private const val SILVER_BRUSH_ALPHA = 15 / 255f
private val goldShadowColor = Color(0xFFB47814)
private val silverShadowColor = Color(0xFF506070)
private val goldStrokeColor = Color(0x80C9973A)
private val silverStrokeColor = Color(0x807A8FA0)

@Composable
fun CombinedDashboardCard(
    totalValue: Double,
    profitLoss: Double,
    profitLossPercentage: Double,
    goldUnitsInGm: Double?,
    silverUnitsInGm: Double?,
    isLoading: Boolean,
    onTotalClick: () -> Unit = {},
    onGoldClick: () -> Unit = {},
    onSilverClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val goldSectionShape = RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            val silverSectionShape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            ) {
                // Gold Section
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = !isLoading, onClick = onGoldClick)
                        .shadow(4.dp, goldSectionShape, spotColor = goldShadowColor, ambientColor = goldShadowColor)
                        .clip(goldSectionShape)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = goldMetalColors,
                                    start = Offset(0f, h),
                                    end   = Offset(w, 0f)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0x33 / 255f), Color.Transparent),
                                    startY = 0f, endY = h
                                )
                            )
                            var y = 0f
                            while (y < h) {
                                drawLine(
                                    color = Color.White.copy(alpha = GOLD_BRUSH_ALPHA),
                                    start = Offset(0f, y),
                                    end   = Offset(w, y),
                                    strokeWidth = 1f
                                )
                                y += 3f
                            }
                        }
                        .border(1.dp, goldStrokeColor, goldSectionShape)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val goldHasValue = goldUnitsInGm != null && goldUnitsInGm > 0

                        Surface(
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.goldbar_icon),
                                    contentDescription = "Gold",
                                    modifier = Modifier.size(36.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF6A4C00),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val unitsText = if (goldHasValue && goldUnitsInGm != null) {
                                formatWeight(goldUnitsInGm, stringResource(Res.string.mg_label), stringResource(Res.string.g_label))
                            } else {
                                "0 g"
                            }
                            Text(
                                text = unitsText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4A3600)
                            )
                        }

                        Text(
                            text = "GOLD",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = getOutfitFontFamily(),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = Color(0xFF381E00)
                        )
                    }
                }
                
                // Silver Section
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = !isLoading, onClick = onSilverClick)
                        .shadow(4.dp, silverSectionShape, spotColor = silverShadowColor, ambientColor = silverShadowColor)
                        .clip(silverSectionShape)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = silverMetalColors,
                                    start = Offset(0f, h),
                                    end   = Offset(w, 0f)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0x40 / 255f), Color.Transparent),
                                    startY = 0f, endY = h
                                )
                            )
                            var y = 0f
                            while (y < h) {
                                drawLine(
                                    color = Color.White.copy(alpha = SILVER_BRUSH_ALPHA),
                                    start = Offset(0f, y),
                                    end   = Offset(w, y),
                                    strokeWidth = 1f
                                )
                                y += 3f
                            }
                        }
                        .border(1.dp, silverStrokeColor, silverSectionShape)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val silverHasValue = silverUnitsInGm != null && silverUnitsInGm > 0

                        Surface(
                            color = Color.White.copy(alpha = 0.4f),
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.silver_icon),
                                    contentDescription = "Silver",
                                    modifier = Modifier.size(36.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF505A61),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val unitsText = if (silverHasValue && silverUnitsInGm != null) {
                                formatWeight(silverUnitsInGm, stringResource(Res.string.mg_label), stringResource(Res.string.g_label))
                            } else {
                                "0 g"
                            }
                            Text(
                                text = unitsText,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2C343A)
                            )
                        }

                        Text(
                            text = "SILVER",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = getOutfitFontFamily(),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = Color(0xFF161E24)
                        )
                    }
                }
            }

//            // Total Value Section
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(V2SubtleBorder)
//                    .clickable { onTotalClick() }
//                    .padding(20.dp)
//            ) {
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Text(
//                        text = "Total Value",
//                        style = MaterialTheme.typography.titleMedium,
//                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                    )
//
//                    if (isLoading) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(24.dp),
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    } else {
//                        val ceiledTotalValue = ceil(totalValue)
//                        Text(
//                            text = "₹${formatIndian(ceiledTotalValue)}",
//                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
//                            color = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//                }
//            }
        }
    }
}

@Composable
fun StatusInfoCard(
    kycStatus: String,
    mandateStatuses: List<String>,
    onRetryKyc: () -> Unit
) {
    // Check if KYC is pending
    val isKycPending = kycStatus.equals("PENDING", ignoreCase = true) ||
            kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
            kycStatus.equals("EXPIRED", ignoreCase = true) ||
            // kycStatus.equals("UNLINKED", ignoreCase = true) ||
            kycStatus.equals("REJECTED", ignoreCase = true)

    // Filter only pending mandate statuses
    val pendingMandateStatuses = mandateStatuses.filter { status ->
        status.contains("PENDING", ignoreCase = true) ||
                status.contains("MANDATE_PENDING", ignoreCase = true) ||
                status.contains("SUBMITTED", ignoreCase = true) ||
                status.contains("MANDATE_NOT_CREATED", ignoreCase = true) ||
                status.contains("NOT_CREATED", ignoreCase = true)
    }

    // Only show card if there's at least one pending status
    if (!isKycPending && pendingMandateStatuses.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KYC Status - only show if pending
            if (isKycPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KYC Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = formatKycStatus(kycStatus),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFF57C00),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (kycStatus.equals("EXPIRED", ignoreCase = true) || kycStatus.equals("REJECTED", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onRetryKyc,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF57C00)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF57C00)
                        )
                    ) {
                        Text(
                            text = "Retry KYC",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Mandate Statuses - only show pending ones
            pendingMandateStatuses.forEach { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mandate Status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "In Progress",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFF57C00),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom message
            Text(
                text = "Waiting for verification from internal team",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PrimaryGoalCard(
    goal: InvestmentGoal?,
    isLoading: Boolean,
    fundDetails: List<FundDetail> = emptyList(),
    holdingsDetails: List<HoldingDetail> = emptyList(),
    onTopCardClick: () -> Unit = {},
    onBottomCardClick: () -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSavingsPlusInfo by remember { mutableStateOf(false) }
    var showGoldValueInfoDialog by remember { mutableStateOf(false) }
    var showSilverValueInfoDialog by remember { mutableStateOf(false) }
    val category = goal?.category?.uppercase().orEmpty()
    val isSavingsPlus = category == "SAVINGS_PLUS"
    val cursiveFontFamily = FontFamily(Font(Res.font.cursive_font))
    val borderColor = if (goal != null) {
        // Use colorTheme if available, otherwise use category
        goal.colorTheme.toColor()
    } else {
        V2SuccessGreen // Default green
    }
    
    val gradientColors = if (goal != null) {
        getGoalGradientColors(goal.category, goal.colorTheme)
    } else {
        listOf(Color.White, Color.White)
    }
    
    val correlationColor = if (goal != null) {
        getCorrelationColorForCategory(goal.category, goal.colorTheme)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val isGoldOrSilverWithGm = goal != null && (category == "GOLD" || category == "SILVER") && goal.unitsInGm != null && goal.unitsInGm > 0

    val topCardClickable = if (isLoading || goal == null) Modifier else Modifier.clickable { onTopCardClick() }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Top Card - Goal Info
        val hasPlanSummary = goal?.planSummary != null
        
        val topCardShape = if (hasPlanSummary) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(16.dp)
        }
        
        val topBorderModifier = if (hasPlanSummary) {
             Modifier.drawBehind {
                val stroke = Stroke(width = 2.dp.toPx())
                val radius = 16.dp.toPx() // Top corners radius
                
                // Draw Top-Left -> Top-Right -> Down
                val path = Path().apply {
                    // Start at Bottom-Left
                    moveTo(0f, size.height) 
                    lineTo(0f, radius)
                    
                    // Top-Left Corner
                    arcTo(
                        rect = Rect(0f, 0f, 2 * radius, 2 * radius),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    
                    lineTo(size.width - radius, 0f)
                    
                    // Top-Right Corner
                    arcTo(
                        rect = Rect(size.width - 2 * radius, 0f, size.width, 2 * radius),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = false
                    )
                    
                    lineTo(size.width, size.height)
                }
                drawPath(path, borderColor, style = stroke)
            }
        } else {
            Modifier.border(2.dp, borderColor, topCardShape)
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(topCardClickable)
                .then(topBorderModifier),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = topCardShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(topCardShape)
                    .background(
                        brush = Brush.verticalGradient(colors = gradientColors),
                        shape = topCardShape
                    )
            ) {
                 if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (goal != null) {
                    // Goal Info Section (with padding)
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Goal Header - Only Icon, Name, and Progress Circle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon and Goal Name Row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Goal Icon
                                val iconDrawable = getGoalIconDrawable(goal.category)
                                val iconBgColor = getIconBackgroundColorForCategory(goal.category, goal.colorTheme)
                                val iconText = goal.iconType.ifBlank { "🎯" }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = iconBgColor),
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (iconDrawable != null) {
                                            Image(
                                                painter = painterResource(iconDrawable),
                                                contentDescription = goal.name,
                                                modifier = Modifier.size(24.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Text(
                                                text = iconText,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSavingsPlus) {
                                        Text(
                                            text = buildAnnotatedString {
                                                append("Savings ")
                                                withStyle(SpanStyle(fontFamily = cursiveFontFamily, fontSize = 22.sp)) {
                                                    append("Plus")
                                                }
                                            },
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1B5E20)
                                        )
                                    } else {
                                        Text(
                                            text = formatGoalName(goal.name),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = correlationColor
                                        )
                                    }
                                    
                                    val status = goal.planSummary?.status?.uppercase()
                                    if (status == "PAUSED") {
                                        Surface(
                                            color = Color(0xFFFFF3E0),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PAUSED",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE65100)
                                                )
                                            )
                                        }
                                    } else if (status == "CANCELLED") {
                                        Surface(
                                            color = Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "CANCELLED",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Units in Gram (Gold/Silver) - Top Right
                            if (isGoldOrSilverWithGm) {
                                val ceiledCummulativeValue = ceil(goal.cummulativeValue)
                                val amtColor = if (category == "GOLD") correlationColor else Color(0xFF2C343A)
                                Text(
                                    text = "₹${formatIndian(ceiledCummulativeValue)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = amtColor
                                )
                            } else {
                                if (goal.unitsInGm != null && goal.unitsInGm > 0) {
                                    val unitsText = formatWeight(goal.unitsInGm)
                                    Text(
                                        text = unitsText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = correlationColor
                                    )
                                }

                                if (isSavingsPlus) {
                                    Surface(
                                        color = Color(0xFF2E7D32),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "⚡ Instant Redeem",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Goal Details - Below progress bar
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Current Value and Processing Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isGoldOrSilverWithGm && goal.unitsInGm != null) {
                                val unitsText = formatWeight(goal.unitsInGm)
                                val gmColor = if (category == "GOLD") Color(0xFF381E00) else Color(0xFF2C343A)
                                val labelText = if (category == "GOLD") "22K GOLD" else "99.9% SILVER"
                                val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = unitsText,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
                                        color = gmColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = labelText,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = labelColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = {
                                                if (category == "GOLD") {
                                                    showGoldValueInfoDialog = true
                                                } else {
                                                    showSilverValueInfoDialog = true
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Info",
                                                modifier = Modifier.size(16.dp),
                                                tint = labelColor
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Center-aligned column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Use cumulative value for display
                                    val ceiledCummulativeValue = ceil(goal.cummulativeValue)
                                    Text(
                                        text = "₹${formatIndian(ceiledCummulativeValue)}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "TOTAL VALUE",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { 
                                                if (isSavingsPlus) showSavingsPlusInfo = true
                                                else showInfoDialog = true 
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "Info",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSavingsPlus) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

        // Plan Details Section (Bottom Card)
        if (goal?.planSummary != null) {
            Spacer(modifier = Modifier.height(1.dp)) 
            
            val bottomCardShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBottomCardClick() }
                    .drawBehind {
                        val stroke = Stroke(width = 2.dp.toPx())
                        val radius = 16.dp.toPx() // Bottom corners radius
                        
                        // Draw Top-Left -> Bottom-Left -> Bottom-Right -> Top-Right
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(0f, size.height - radius)
                            
                            // Bottom-Left Corner
                            arcTo(
                                rect = Rect(0f, size.height - 2 * radius, 2 * radius, size.height),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            
                            lineTo(size.width - radius, size.height)
                            
                            // Bottom-Right Corner
                            arcTo(
                                rect = Rect(size.width - 2 * radius, size.height - 2 * radius, size.width, size.height),
                                startAngleDegrees = 90f,
                                sweepAngleDegrees = -90f,
                                forceMoveTo = false
                            )
                            
                            lineTo(size.width, 0f)
                        }
                        drawPath(path, borderColor, style = stroke)
                    },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = bottomCardShape
            ) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bottomCardShape)
                        .background(
                            brush = Brush.verticalGradient(colors = gradientColors),
                            shape = bottomCardShape
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PlanDetailsSection(summary = goal.planSummary)
                    }
                }
            }
        }
    }
    
    // Savings Plus Info Dialog
    if (showSavingsPlusInfo && goal != null) {
        SavingsPlusInfoDialog(
            instantRedemptionValue = goal.instantRedemptionValue,
            onDismiss = { showSavingsPlusInfo = false }
        )
    }

    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "Total Value",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Total value equals the current market value of your investments plus any payments currently being processed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInfoDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Gold Value Info Dialog
    if (showGoldValueInfoDialog) {
        AlertDialog(
            onDismissRequest = { showGoldValueInfoDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.estimated_gold),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.estimated_gold_info_popup_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.for_representational_purposes_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showGoldValueInfoDialog = false }
                ) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    // Silver Value Info Dialog
    if (showSilverValueInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSilverValueInfoDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.estimated_silver),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.estimated_silver_info_popup_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.for_representational_purposes_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSilverValueInfoDialog = false }
                ) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}

@Composable
fun MilestoneBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎉", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SavingsPlusInfoDialog(
    instantRedemptionValue: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Savings Plus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Savings Plus provides higher returns than a regular savings account with the added benefit of instant liquidity.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Instant Redeemable Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "₹${formatIndian(instantRedemptionValue)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You can withdraw up to ₹50,000 or 80% of your investment (whichever is lower) instantly, 24/7.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B5E20).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood", color = Color(0xFF2E7D32))
            }
        }
    )
}

@Composable
fun NextGoalsSection(
    goals: List<InvestmentGoal>,
    onGoalClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Next Goals",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose a goal to start your wealth building journey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        goals.forEach { goal ->
            NextGoalCard(
                goal = goal,
                onClick = { onGoalClick(goal.goalId) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun NextGoalCard(
    goal: InvestmentGoal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    val gradientColors = getGoalGradientColors(goal.category, goal.colorTheme)
    val borderColor = getDarkBorderColorForCategory(goal.category, goal.colorTheme)
    val correlationColor = getCorrelationColorForCategory(goal.category, goal.colorTheme)
    val category = goal.category.uppercase()
    val isAllInOne = category == "ALL_IN_ONE"
    var expanded by remember(initialExpanded) { mutableStateOf(initialExpanded) }

    Card(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconBgColor = getIconBackgroundColorForCategory(goal.category, goal.colorTheme)
                    val iconDrawable = getGoalIconDrawable(goal.category)
                    val iconText = goal.iconType.ifBlank { "🎯" }
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = iconBgColor
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (category == "SAVINGS_PLUS") {
                                Image(
                                    painter = painterResource(Res.drawable.savings_plus),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (iconDrawable != null) {
                                Image(
                                    painter = painterResource(iconDrawable),
                                    contentDescription = goal.name,
                                    modifier = Modifier.size(20.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(text = iconText, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                    
                    val cursiveFontFamily = FontFamily(Font(Res.font.cursive_font))
                    Row(
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category == "SAVINGS_PLUS") {
                            Text(
                                text = buildAnnotatedString {
                                    append("Savings ")
                                    withStyle(style = SpanStyle(fontFamily = cursiveFontFamily, fontWeight = FontWeight.Bold)) {
                                        append("Plus")
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = correlationColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            var showSavingsPlusInfo by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showSavingsPlusInfo = true },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = correlationColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (showSavingsPlusInfo) {
                                SavingsPlusInfoDialog(
                                    instantRedemptionValue = goal.instantRedemptionValue,
                                    onDismiss = { showSavingsPlusInfo = false }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            // Instant Pill
                            Surface(
                                color = V2Cream,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, V2SuccessGreen.copy(alpha = 0.3f))
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
                                        text = "Instant Redeem",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = formatGoalName(goal.name),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = correlationColor
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = correlationColor.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(0.32f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Daily SIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF424242),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        var fontSize by remember { mutableStateOf(12.sp) }
                        Text(
                            text = when (goal.category.uppercase()) {
                                "GOLD", "SAVINGS" -> "₹21 - ₹500"
                                "MARKET_EXPLORER" -> "₹21 - ₹1000"
                                "FESTIVAL_SPENDS" -> "₹11 - ₹500"
                                "GLOBAL_EXPOSURE" -> "₹101 - ₹1000"
                                "ALL_IN_ONE" -> "₹51 - ₹1000"
                                "INNOVATION" -> "₹101 - ₹1000"
                                else -> "₹101 - ₹500"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSize
                            ),
                            color = Color(0xFF424242),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            onTextLayout = { textLayoutResult ->
                                if (textLayoutResult.didOverflowWidth) {
                                    if (fontSize.value > 8f) {
                                        fontSize = (fontSize.value - 0.5f).sp
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.width(2.dp).height(50.dp).background(Color(0xFFE0E0E0))
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(0.9f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (category == "INNOVATION") {
                            Text(
                                text = "Key Themes Focus",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF9C27B0), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tech & Internet • Fintech",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF424242)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Auto & Mobility • Industrials",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF424242)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF2196F3), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Healthcare • Services & Retail",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF424242)
                                    )
                                }
                            }
                        } else {
                            val annotatedText = buildAnnotatedString {
                                when (category) {
                                    "GOLD" -> {
                                        append("Investing ₹101 daily since Jan 2023 gives you purchasing power of ")
                                        withStyle(SpanStyle(color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)) {
                                            append("~15.8g Gold")
                                        }
                                        append(".")
                                    }
                                    "SILVER" -> {
                                        append("Investing ₹101 daily since Jan 2023 yields ")
                                        withStyle(SpanStyle(color = Color(0xFF616161), fontWeight = FontWeight.Bold)) {
                                            append("~1.24kg Silver")
                                        }
                                        append(" worth.")
                                    }
                                    "SAVINGS", "SAVINGS_PLUS" -> {
                                        append("Investing ₹101 daily since Jan 2023 in this fund built a corpus of ~")
                                        withStyle(SpanStyle(color = Color(0xFF004D40), fontWeight = FontWeight.Bold)) {
                                            append("₹1.24 Lakhs")
                                        }
                                        append(".")
                                    }
                                    "FESTIVAL_SPENDS" -> {
                                        append("Investing ₹51 daily since Jan 2023 in this fund grew to ~")
                                        withStyle(SpanStyle(color = Color(0xFFFF6F00), fontWeight = FontWeight.Bold)) {
                                            append("₹62,408")
                                        }
                                        append(".")
                                    }
                                    "GLOBAL_EXPOSURE" -> {
                                        append("Investing ₹101 daily since Jan 2023 in international equity fund grew into ~")
                                        withStyle(SpanStyle(color = Color(0xFF00897B), fontWeight = FontWeight.Bold)) {
                                            append("₹1.54 Lakhs")
                                        }
                                        append(".")
                                    }
                                    "ALL_IN_ONE" -> {
                                        append("Investing ₹101 daily since Jan 2023 in a diversified multi-asset fund helped build a corpus of ~")
                                        withStyle(SpanStyle(color = Color(0xFF2C4C9C), fontWeight = FontWeight.Bold)) {
                                            append("₹1.41 Lakhs")
                                        }
                                        append(".")
                                    }
                                    "MARKET_EXPLORER" -> {
                                        val marketExplorerAccent = Color(0xFF0F6B5C)
                                        append("Investing ₹101 daily since Jan 2023 in a flexi-cap fund could have built a corpus of ~")
                                        withStyle(
                                            SpanStyle(
                                                color = marketExplorerAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append("₹1.42 Lakhs")
                                        }
                                        append(".")
                                    }
                                    else -> append(goal.description)
                                }
                            }
                            Text(
                                text = annotatedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF424242),
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                            )
                        }
                    }
                }

                if (isAllInOne || category == "MARKET_EXPLORER" || category == "INNOVATION") {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isAllInOne) {
                        val allInOneAccent = Color(0xFF2C4C9C)
                        DashedDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = allInOneAccent.copy(alpha = 0.8f),
                            dashLength = 4.dp,
                            gapLength = 4.dp
                        )
                    } else if (category == "INNOVATION") {
                        val innovationAccent = Color(0xFF68499A).copy(alpha = 0.85f)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashedDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = innovationAccent,
                                dashLength = 2.dp,
                                gapLength = 4.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "↗",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF68499A).copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            DashedDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = innovationAccent,
                                dashLength = 2.dp,
                                gapLength = 4.dp
                            )
                        }
                    } else {
                        val marketExplorerAccent = Color(0xFF0F6B5C).copy(alpha = 0.85f)
                        Row(
                            modifier = Modifier.fillMaxWidth().height(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashedDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = marketExplorerAccent,
                                dashLength = 2.dp,
                                gapLength = 4.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "↗",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F6B5C).copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            DashedDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = marketExplorerAccent,
                                dashLength = 2.dp,
                                gapLength = 4.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val correlationText = getCorrelationText(goal.category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = correlationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = borderColor
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = if (isAllInOne) Color(0xFF2C4C9C) else if (category == "INNOVATION") Color(0xFF7656A8) else Color(0xFF8A4E1E),
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(if (expanded) 180f else 0f)
                        )
                    }
                    if (category == "INNOVATION") {
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFF68499A).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFF68499A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Focuses on breakthrough & innovative themes 🚀",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF68499A)
                            )
                        }
                    }
                    if (expanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isAllInOne) {
                            // Goals badge: Ideal for long-term goals
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFFFEB3B).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ideal for long-term goals",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF424242)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Suggested holding period: 1 Year+ ⏰",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Asset Allocation",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Asset allocation bar: Equity 65%, Debt 20%, Gold 7.5%, Silver 7.5%
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.65f)
                                        .fillMaxHeight()
                                        .background(Color(0xFF2196F3))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.20f)
                                        .fillMaxHeight()
                                        .background(Color(0xFF00897B))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.075f)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF9800))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.075f)
                                        .fillMaxHeight()
                                        .background(Color(0xFF9E9E9E))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Legend: two rows so Equity/Debt/Gold/Silver each stay on one line on narrow screens
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF2196F3), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Equity (65%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF424242),
                                            maxLines = 1
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF00897B), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Debt (20%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF424242),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFFFF9800), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Gold (7.5%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF424242),
                                            maxLines = 1
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF9E9E9E), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Silver (7.5%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF424242),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (category == "INNOVATION") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Suggested holding period: 5 Years+ ⏰",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Holdings are actively picked by the fund manager and change as new innovative companies emerge.",
                                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                color = Color(0xFF68499A)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            // MARKET_EXPLORER
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFFFEB3B).copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adapts across market caps 🧭",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF424242)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Suggested holding period: 5 Years+ ⏰",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Market Cap Allocation",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF2196F3), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Large Cap",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF424242)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• Established market leaders",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF757575)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFFF9800), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mid Cap",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF424242)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• Growing businesses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF757575)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Small Cap",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF424242)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "• Emerging companies",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Allocation changes over time to capture the best market opportunities.",
                                style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                color = Color(0xFF8A4E1E).copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartInvestmentJourneyCard(
    onNeedHelpClick: () -> Unit,
    onExploreClick: () -> Unit
) {
    val cardLightGreenTop = V2Cream
    val cardLightGreenBottom = V2SubtleBorder
    val topBorderGreenDark = V2Obsidian
    val topBorderGreen = V2Obsidian
    val rocketGreenDark = V2Obsidian
    val rocketGreen = V2Obsidian
    val sparkleYellow = Color(0xFFFFC107)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(cardLightGreenTop, cardLightGreenBottom)))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(topBorderGreenDark, topBorderGreen))))
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(rocketGreenDark, rocketGreen))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White)
                }
                Text("Start Your Investment Journey", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = V2Obsidian, textAlign = TextAlign.Center)
                Text("Expertly curated investment options", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
                Text("Grow your wealth with Pyllar. Simple, secure, and smart.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                
                Button(onClick = onExploreClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = V2Obsidian)) {
                    Text("Explore Investment Options", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = sparkleYellow, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardTrustFooter() {
    val creamBg = Color(0xFFFBF9F4)
    val goldColor = Color(0xFFD4AF37)
    val premiumBrown = Color(0xFF3E2723)

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
                icon = Icons.Filled.Shield,
                modifier = Modifier.weight(1f)
            )
            TrustFeatureItemV2(
                title = stringResource(Res.string.trusted_title),
                icon = Icons.Filled.Verified,
                modifier = Modifier.weight(1f)
            )
            TrustFeatureItemV2(
                title = stringResource(Res.string.transparent_title),
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
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun TrustFeatureItemV2(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val iconGreen = Color(0xFF1A7A42)
    val premiumBrown = Color(0xFF3E2723)

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconGreen,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = premiumBrown
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun AmcLogoItemV2(resource: org.jetbrains.compose.resources.DrawableResource) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier
            .height(28.dp)
            .widthIn(max = 65.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun PromotionShareCard(onShareClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A2B2B))
            .drawBehind {
                val s = size
                drawCircle(Color.White.copy(alpha = 0.05f), radius = s.width * 0.4f, center = Offset(s.width * 0.9f, s.height * 0.2f))
            }
            .clickable { onShareClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
                Text("PYLLAR MONEY", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color(0xFFD1FAE5), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text("Invite friends to Pyllar and help them grow wealth!", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFD1FAE5), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share with Friends", color = Color(0xFFD1FAE5), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun KycAadhaarLinkingRequiredCard(
    modifier: Modifier = Modifier,
    panNumber: String? = null,
    onLearnMoreClick: () -> Unit = {}
) {
    val titleDark = Color(0xFF103620)
    val goldCursive = Color(0xFFBF9028)
    val bodyMuted = Color(0xFF6B5E4F)
    val accentGreen = Color(0xFF1E5E3A)

    val panSuffix = remember(panNumber) {
        if (!panNumber.isNullOrBlank() && panNumber.length >= 4) {
            panNumber.takeLast(4).uppercase()
        } else {
            ""
        }
    }

    val descriptionText = stringResource(Res.string.kyc_unlinked_message)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFF7EED8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFFFFDF9), shape = RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFFF2EAD3), shape = RoundedCornerShape(18.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFB58424),
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val titleText = if (panSuffix.isNotEmpty()) {
                    stringResource(Res.string.kyc_unlinked_pan_with_suffix_title, panSuffix)
                } else {
                    stringResource(Res.string.kyc_unlinked_pan_title)
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = titleDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(Res.string.kyc_unlinked_pan_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Normal,
                        fontSize = 22.sp
                    ),
                    color = goldCursive,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = bodyMuted,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .clickable { onLearnMoreClick() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Learn why Aadhaar-PAN linking is required",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentGreen
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = accentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycPendingBottomSheet(onDismiss: () -> Unit, onRetryKyc: () -> Unit, kycStatus: String) {
    val isUnlinked = kycStatus.equals("UNLINKED", ignoreCase = true)
    val title = if (isUnlinked) stringResource(Res.string.kyc_unlinked_title) else stringResource(Res.string.kyc_verification_pending_title)
    val message = if (isUnlinked) stringResource(Res.string.kyc_unlinked_message) else stringResource(Res.string.kyc_verification_pending_message)

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, color = Color.Gray)
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = V2Obsidian)) {
                Text(stringResource(Res.string.ok))
            }
            if (kycStatus == "EXPIRED" || kycStatus == "REJECTED") {
                OutlinedButton(onClick = onRetryKyc, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(Res.string.retry))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
@Composable
fun PoweredByAmcsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Powered by India's leading AMCs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AmcLogoItem(Res.drawable.axis_lo)
            AmcLogoItem(Res.drawable.invesco)
            AmcLogoItem(Res.drawable.aditya)
            AmcLogoItem(Res.drawable.nippon)
        }
    }
}

@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    dashLength: androidx.compose.ui.unit.Dp = 4.dp,
    gapLength: androidx.compose.ui.unit.Dp = 4.dp
) {
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        val dashLengthPx = dashLength.toPx()
        val gapLengthPx = gapLength.toPx()
        val thicknessPx = thickness.toPx()
        val width = size.width
        var currentX = 0f
        while (currentX < width) {
            drawLine(
                color = color,
                start = Offset(currentX, thicknessPx / 2),
                end = Offset(currentX + dashLengthPx, thicknessPx / 2),
                strokeWidth = thicknessPx
            )
            currentX += dashLengthPx + gapLengthPx
        }
    }
}

@Composable
private fun PlanDetailsSection(summary: PlanSummary) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // SIP Amount and Next SIP Date row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // SIP Amount
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "₹${formatIndian(summary.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SIP Amount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Next SIP Date or Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val statusUpper = summary.status?.uppercase()
                Text(
                    text = when (statusUpper) {
                        "PAUSED" -> "Paused"
                        "CANCELLED" -> "Cancelled"
                        else -> formatNextSipDate(summary.nextSipDate) ?: "—"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = when (statusUpper) {
                        "PAUSED" -> Color(0xFFF57C00) // Orange
                        "CANCELLED" -> Color(0xFFD32F2F) // Red
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Next SIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AmcLogoItem(resource: DrawableResource) {
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
fun ReferAndEarnCard(
    coinsBalance: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C3320), Color(0xFF04190F))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // Gold "NEW" tag at the top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFFBD9A3C),
                        shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "NEW",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF04190F),
                    fontSize = 10.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Row: Texts on left, Gold stack on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "REFER & EARN",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFBD9A3C)
                        )

                        // "Invite friends, earn ₹100 each"
                        val inviteHeading = buildAnnotatedString {
                            append("Invite friends, earn ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFFFFF59D), // Gold/yellow color
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Serif
                                )
                            ) {
                                append("\n₹100 ")
                            }
                            append(" each")
                        }

                        Text(
                            text = inviteHeading,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = Color.White
                        )

                        // Subtext
                        Text(
                            text = "Worth 100 coins when they invest 7 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Image(
                        painter = painterResource(Res.drawable.gold_icon),
                        contentDescription = "Referral rewards",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Divider line (thin gold/white translucent line)
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.15f),
                    thickness = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "🕐 Offer valid till 30 June",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Bottom Row: Pill/Wallet on left, Invite button on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pill displaying either "100 coins" or the actual balance
                        val displayBalance = if (coinsBalance <= 0) 100 else coinsBalance
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(
                                    BorderStroke(1.dp, Color(0xFFBD9A3C).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.gold_icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$displayBalance coins",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFF59D)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "≈ ₹$displayBalance in wallet",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Right: Invite > button at the very right end!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onClick() }
                    ) {
                        Text(
                            text = "Invite",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFF59D)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFF59D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KycSubmittedAwaitingApprovalCard(
    onContactSupport: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kyc_ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        ),
        label = "ring_rotation"
    )

    val cream = Color(0xFFFFFBF0)
    val goldBorder = Color(0xFFC9973A)
    val goldText = Color(0xFFA27915)
    val obsidian = Color(0xFF0A2415)
    val doneGreen = Color(0xFF388E3C)
    val inProgressGold = Color(0xFFF59E0B)
    val successGreen = Color(0xFF2E7D32)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, goldBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rotating dashed ring + clock icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(rotation)
                ) {
                    drawArc(
                        color = goldBorder,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(14f, 9f),
                                phase = 0f
                            )
                        )
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF3C4),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = goldBorder,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            // Title + cursive subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "KYC submitted",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = obsidian,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "being verified",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = getCursiveFontFamily(),
                        fontStyle = FontStyle.Italic
                    ),
                    color = goldText,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "We'll send you a notification the moment it's approved. Most verifications complete in a few minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            // Steps checklist
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    KycVerificationStep(label = "PAN verified", done = true, doneColor = doneGreen)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF000000).copy(alpha = 0.06f)))
                    KycVerificationStep(label = "Aadhaar e-KYC", done = true, doneColor = doneGreen)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF000000).copy(alpha = 0.06f)))
                    KycVerificationStep(label = "Final approval", done = false, inProgressColor = inProgressGold)
                }
            }

            // Notification callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = obsidian.copy(alpha = 0.04f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = obsidian.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "You’ll receive a push notification and a WhatsApp message once your KYC is approved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Help link
            TextButton(
                onClick = onContactSupport,
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text(
                    text = "Need help? Contact support →",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = successGreen
                )
            }
        }
    }
}

@Composable
private fun KycVerificationStep(
    label: String,
    done: Boolean,
    doneColor: Color = Color(0xFF388E3C),
    inProgressColor: Color = Color(0xFFF59E0B)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (done) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = doneColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .border(2.dp, inProgressColor, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(inProgressColor, CircleShape)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurface else inProgressColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (done) "Done" else "IN PROGRESS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (done) doneColor else inProgressColor
        )
    }
}

@Composable
fun KycApprovedReadyToInvestCard(
    onChooseGoalClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "approved_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_alpha"
    )

    val cardBg = Color(0xFFF7FAF7)
    val haloGreen = Color(0xFF2E7D32)
    val iconCircleBg = Color(0xFFC8DFC8)
    val shieldGreen = Color(0xFF3E7A3E)
    val titleDark = Color(0xFF1A3820)
    val goldCursive = Color(0xFFBF9028)
    val bodyMuted = Color(0xFF607060)
    val achievementBg = Color(0xFFDEEBDE)
    val achievementBorder = Color(0xFFB5CBB5)
    val achievementTitleGreen = Color(0xFF2A562A)
    val achievementSubGreen = Color(0xFF4A7A4A)
    val obsidian = Color(0xFF0A2415)
    val buttonGoldRing = Color(0xFFCBA030)
    val goldMicro = Color(0xFFBF9028)
    val sparkleGold = Color(0xFFB8902A)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon circle with pulsing green halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulseScale)
                        .background(haloGreen.copy(alpha = pulseAlpha), CircleShape)
                )
                Surface(
                    shape = CircleShape,
                    color = iconCircleBg,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = shieldGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = "KYC approved!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = titleDark,
                textAlign = TextAlign.Center
            )

            // Cursive subtitle
            Text(
                text = "you're all set",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = getCursiveFontFamily(),
                    fontStyle = FontStyle.Italic
                ),
                color = goldCursive,
                textAlign = TextAlign.Center
            )

            // Description
            Text(
                text = "You've already done the hard part. Your first investment is just one tap away.",
                style = MaterialTheme.typography.bodyMedium,
                color = bodyMuted,
                textAlign = TextAlign.Center
            )

            // Identity verified row card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = achievementBg,
                border = BorderStroke(1.dp, achievementBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🎉", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Identity verified",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = achievementTitleGreen
                        )
                        Text(
                            text = "Explore the investment options below",
                            style = MaterialTheme.typography.bodySmall,
                            color = achievementSubGreen
                        )
                    }
                }
            }

            // Sparkle divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(sparkleGold.copy(alpha = 0.25f))
                )
                Text(
                    text = "✦   ✦   ✦",
                    style = MaterialTheme.typography.labelSmall,
                    color = sparkleGold.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(sparkleGold.copy(alpha = 0.25f))
                )
            }

            // Primary CTA — pill button with gold outer ring
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, buttonGoldRing, RoundedCornerShape(50.dp))
                    .padding(3.dp)
            ) {
                Button(
                    onClick = onChooseGoalClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = obsidian,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = "Choose your first goal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Micro-step hint in gold
            Text(
                text = "⚡ Start with ₹21/day",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = goldMicro,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class StepStatus {
    COMPLETED,
    PENDING
}

@Composable
fun InitiateKycCard(
    onStartKyc: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF2EFE9))
    ) {
        Column {
            // Top Section with light warm background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDFBF7))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verify identity",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C1E11)
                            )
                        )
                        // Pill badge with clock icon (~2 min)
                        Surface(
                            color = Color(0xFFF9F3EB),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF8C7153),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "~ 2 mins",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF8C7153)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SEBI requires us to verify your identity before you can start investing in mutual funds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B5843)
                    )
                }
            }

            // Divider line
            HorizontalDivider(
                color = Color(0xFFF2EFE9),
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Timeline Steps Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1: PAN Verification
                KycStepRow(
                    title = "Verify PAN Card",
                    description = "Instantly verified using income tax database",
                    status = StepStatus.COMPLETED,
                    showBottomLine = true,
                    lineColor = Color(0xFF4CAF50)
                )

                // Step 2: Account setup
                KycStepRow(
                    title = "Complete Account Setup",
                    description = "Personal, professional and bank details",
                    status = StepStatus.COMPLETED,
                    showBottomLine = true,
                    lineColor = Color(0xFF4CAF50)
                )

                // Step 3: Aadhaar KYC
                KycStepRow(
                    title = "Verify Aadhaar (e-KYC)",
                    description = "Authenticate securely using Aadhaar OTP",
                    status = StepStatus.PENDING,
                    showBottomLine = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Start KYC button
                Button(
                    onClick = onStartKyc,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, Color(0xFFFFC107), RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF071D12),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Complete KYC",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Powered by DigiLocker footer
                Text(
                    text = "Your data is encrypted · Powered by DigiLocker",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KycStepRow(
    title: String,
    description: String,
    status: StepStatus,
    showBottomLine: Boolean,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.LightGray
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .then(
                        if (status == StepStatus.PENDING) {
                            Modifier.border(2.dp, Color(0xFFD4AF37), CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (status == StepStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (showBottomLine) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            }
        }

        // Content + Right Badge Column
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (status == StepStatus.COMPLETED) Color(0xFF2E7D32) else Color(0xFF8D6E63)
                        )
                    )
                    if (status == StepStatus.PENDING) {
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PENDING",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8D6E63)
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D4037)
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (status == StepStatus.COMPLETED) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Dotted circle on the right (Aadhaar KYC pending step)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFD4AF37),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        )
                    }
                }
            }
        }
    }
}

private enum class SurveyStep {
    START,
    QUESTION_ISSUES,
    QUESTION_WHY_INVESTING,
    QUESTION_WANT_CALL,
    QUESTION_RATE_APP,
    SUBMITTING,
    COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardSurveyCard(
    onSurveyCompleted: () -> Unit,
    onSurveySkipped: () -> Unit,
    onSubmitAnswer: (selectedOption: String, freeText: String?, requestCallback: Boolean) -> Unit,
    platformActions: PlatformActions = koinInject(),
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(SurveyStep.START) }
    var issuesSelected by remember { mutableStateOf("") }
    var issuesText by remember { mutableStateOf("") }
    var whyInvesting by remember { mutableStateOf("") }
    var wantCall by remember { mutableStateOf("") }
    var selectedStars by remember { mutableIntStateOf(0) }
    var ratingFeedbackText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(issuesSelected) {
        if (issuesSelected == "Yes") {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus errors
            }
        }
    }

    LaunchedEffect(selectedStars) {
        if (selectedStars > 0) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus errors
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFF2EFE9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFDFBF7))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentStep == SurveyStep.START) {
                Text(
                    text = stringResource(Res.string.survey_help_us_serve),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C1E11)
                    )
                )
                Text(
                    text = stringResource(Res.string.survey_quick_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5F5041))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSurveySkipped,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF103620)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF103620)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.survey_btn_skip))
                    }
                    Button(
                        onClick = { currentStep = SurveyStep.QUESTION_ISSUES },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF103620)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.survey_btn_start), color = Color.White)
                    }
                }
            } else if (currentStep == SurveyStep.SUBMITTING) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF103620))
                        Text(
                            stringResource(Res.string.survey_submitting),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5F5041))
                        )
                    }
                }
            } else if (currentStep == SurveyStep.COMPLETED) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(Res.string.survey_submitted_success),
                        tint = Color(0xFF103620),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = stringResource(Res.string.survey_thank_you),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2C1E11))
                    )
                    Text(
                        text = stringResource(Res.string.survey_submitted_success),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF5F5041)),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSurveyCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF103620)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.survey_btn_done), color = Color.White)
                    }
                }
            } else {
                val totalQuestions = 4
                val currentQuestionNumber = when (currentStep) {
                    SurveyStep.QUESTION_ISSUES -> 1
                    SurveyStep.QUESTION_WHY_INVESTING -> 2
                    SurveyStep.QUESTION_WANT_CALL -> 3
                    SurveyStep.QUESTION_RATE_APP -> 4
                    else -> 1
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.survey_title_feedback),
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF8C7F70))
                    )
                    Surface(
                        color = Color(0xFFF9F3EB),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$currentQuestionNumber of $totalQuestions",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8C7F70)
                            )
                        )
                    }
                }

                when (currentStep) {
                    SurveyStep.QUESTION_ISSUES -> {
                        Text(
                            text = stringResource(Res.string.survey_q1_issues),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2C1E11))
                        )
                        val options = listOf(
                            stringResource(Res.string.survey_option_yes) to "Yes",
                            stringResource(Res.string.survey_option_no) to "No"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (uiText, codeValue) ->
                                SurveyOptionItem(
                                    text = uiText,
                                    isSelected = issuesSelected == codeValue,
                                    onSelect = { issuesSelected = codeValue }
                                )
                            }
                        }
                        if (issuesSelected == "Yes") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = issuesText,
                                onValueChange = { issuesText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = { Text(stringResource(Res.string.survey_placeholder_describe_issue), color = Color(0xFF8C7F70)) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2C1E11)),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF2C1E11),
                                    unfocusedTextColor = Color(0xFF2C1E11),
                                    focusedBorderColor = Color(0xFF103620),
                                    unfocusedBorderColor = Color(0xFFE5DFD5)
                                )
                            )
                        }
                    }
                    SurveyStep.QUESTION_WHY_INVESTING -> {
                        Text(
                            text = stringResource(Res.string.survey_q2_why_investing),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2C1E11))
                        )
                        val options = listOf(
                            stringResource(Res.string.survey_option_children_school) to "Children School",
                            stringResource(Res.string.survey_option_savings) to "Savings",
                            stringResource(Res.string.survey_option_emergency) to "Emergency",
                            stringResource(Res.string.survey_option_marriage) to "Marriage"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (uiText, codeValue) ->
                                SurveyOptionItem(
                                    text = uiText,
                                    isSelected = whyInvesting == codeValue,
                                    onSelect = { whyInvesting = codeValue }
                                )
                            }
                        }
                    }
                    SurveyStep.QUESTION_WANT_CALL -> {
                        Text(
                            text = stringResource(Res.string.survey_q3_want_call),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2C1E11))
                        )
                        val options = listOf(
                            stringResource(Res.string.survey_option_yes) to "Yes",
                            stringResource(Res.string.survey_option_no) to "No"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (uiText, codeValue) ->
                                SurveyOptionItem(
                                    text = uiText,
                                    isSelected = wantCall == codeValue,
                                    onSelect = { wantCall = codeValue }
                                )
                            }
                        }
                    }
                    SurveyStep.QUESTION_RATE_APP -> {
                        Text(
                            text = stringResource(Res.string.survey_q4_rate_app),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2C1E11))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            for (i in 1..5) {
                                val isSelected = i <= selectedStars
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "$i Stars",
                                    tint = if (isSelected) Color(0xFFFFB300) else Color(0xFFE5DFD5),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable {
                                            selectedStars = i
                                        }
                                )
                            }
                        }
                        if (selectedStars > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(Res.string.survey_how_can_we_improve),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2C1E11))
                            )
                            OutlinedTextField(
                                value = ratingFeedbackText,
                                onValueChange = { ratingFeedbackText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                placeholder = { Text(stringResource(Res.string.survey_placeholder_feedback), color = Color(0xFF8C7F70)) },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2C1E11)),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF2C1E11),
                                    unfocusedTextColor = Color(0xFF2C1E11),
                                    focusedBorderColor = Color(0xFF103620),
                                    unfocusedBorderColor = Color(0xFFE5DFD5)
                                )
                            )
                        }
                    }
                    else -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentStep = when (currentStep) {
                                SurveyStep.QUESTION_ISSUES -> SurveyStep.START
                                SurveyStep.QUESTION_WHY_INVESTING -> SurveyStep.QUESTION_ISSUES
                                SurveyStep.QUESTION_WANT_CALL -> SurveyStep.QUESTION_WHY_INVESTING
                                SurveyStep.QUESTION_RATE_APP -> SurveyStep.QUESTION_WANT_CALL
                                else -> SurveyStep.START
                            }
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF103620)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF103620)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.survey_btn_back))
                    }

                    val isNextEnabled = when (currentStep) {
                        SurveyStep.QUESTION_ISSUES -> {
                            if (issuesSelected == "Yes") {
                                issuesText.trim().isNotEmpty()
                            } else {
                                issuesSelected == "No"
                            }
                        }
                        SurveyStep.QUESTION_WHY_INVESTING -> whyInvesting.isNotEmpty()
                        SurveyStep.QUESTION_WANT_CALL -> wantCall.isNotEmpty()
                        SurveyStep.QUESTION_RATE_APP -> {
                            if (selectedStars in 1..3) {
                                ratingFeedbackText.trim().isNotEmpty()
                            } else {
                                selectedStars >= 4
                            }
                        }
                        else -> false
                    }

                    Button(
                        onClick = {
                            when (currentStep) {
                                SurveyStep.QUESTION_ISSUES -> {
                                    if (issuesSelected == "Yes") {
                                        onSubmitAnswer("1. Facing issues using Pyllar: Yes", issuesText, false)
                                    } else {
                                        onSubmitAnswer("1. Facing issues using Pyllar: No", null, false)
                                    }
                                    currentStep = SurveyStep.QUESTION_WHY_INVESTING
                                }
                                SurveyStep.QUESTION_WHY_INVESTING -> {
                                    onSubmitAnswer("2. Why are you investing: $whyInvesting", null, false)
                                    currentStep = SurveyStep.QUESTION_WANT_CALL
                                }
                                SurveyStep.QUESTION_WANT_CALL -> {
                                    onSubmitAnswer("3. Do you want a call: $wantCall", null, wantCall.equals("Yes", ignoreCase = true))
                                    currentStep = SurveyStep.QUESTION_RATE_APP
                                }
                                SurveyStep.QUESTION_RATE_APP -> {
                                    onSubmitAnswer("4. App Rating: $selectedStars Stars", ratingFeedbackText, false)
                                    if (selectedStars >= 4) {
                                        platformActions.requestInAppReview(
                                            screenName = "InvestmentDashboardSurvey",
                                            silentFallback = true,
                                            trigger = "survey"
                                        )
                                    }
                                    currentStep = SurveyStep.COMPLETED
                                }
                                else -> {}
                            }
                        },
                        enabled = isNextEnabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF103620),
                            disabledContainerColor = Color(0xFFE5DFD5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentStep == SurveyStep.QUESTION_RATE_APP) stringResource(Res.string.survey_btn_submit) else stringResource(Res.string.survey_btn_next),
                            color = if (isNextEnabled) Color.White else Color(0xFF8C7F70)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurveyOptionItem(
    text: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFF4FAF6) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF103620) else Color(0xFFE5DFD5)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF103620),
                    unselectedColor = Color(0xFF8C7F70)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF103620) else Color(0xFF2C1E11)
                )
            )
        }
    }
}



