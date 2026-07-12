package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
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
    onNavigateToGoal: (String) -> Unit = {},
    onNavigateToSchemeDetails: (String) -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToReferral: () -> Unit = {},
    onRetryKyc: () -> Unit = {},
    viewModel: InvestmentDashboardV2ViewModel = koinInject(),
    platformActions: PlatformActions = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    Log.d("InvestmentDashboardV2", "🎨 COMPOSABLE CALLED - userId: '$userId'")

    val dashboardState by viewModel.dashboardState.collectAsState()
    
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
            dashboardState.kycStatus.equals("REJECTED", ignoreCase = true)
    val hasPendingMandates = dashboardState.fundDetails.any { 
        it.mandateStatus?.contains("PENDING", ignoreCase = true) == true ||
        it.mandateStatus?.contains("SUBMITTED", ignoreCase = true) == true
    }
    val hasStatusCard = !dashboardState.isLoading && (isKycPending || hasPendingMandates)

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

    val nextGoals = (dashboardState.recommendedGoals + dashboardState.allGoals).filter { 
        it.category.uppercase() !in listOf("RETIREMENT", "CHILDRENS_EDUCATION", "VACATION", "FESTIVAL_SPENDS", "SAVINGS") 
    }

    val handleGoalSelection: (String) -> Unit = { goalId ->
        coroutineScope.launch {
            isSelectingGoal = true
            val result = viewModel.initGoalTxn(userId, goalId)
            if (result is Resource.Success) {
                onNavigateToGoal(goalId)
            }
            isSelectingGoal = false
        }
    }

    val handleActiveGoalClick: (InvestmentGoal, Int) -> Unit = { goal, tabIndex ->
        coroutineScope.launch {
            isSelectingGoal = true
            val result = viewModel.initGoalTxn(userId, goal.goalId)
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
            }
            isSelectingGoal = false
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InvestmentDashboardV2")
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadDashboardData(userId)
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

    // Auto-trigger In-App Review after 1 second, once every 10 days, after data is loaded
    LaunchedEffect(dashboardState.isLoading) {
        if (!dashboardState.isLoading) {
            delay(1000)
            val lastPromptTimeStr = sessionStore.getValue("last_review_prompt_time")
            val lastPromptTime = lastPromptTimeStr?.toLongOrNull() ?: 0L
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val tenDaysInMillis = 10L * 24 * 60 * 60 * 1000

            val actualCurrentValue = dashboardState.primaryGoals.sumOf { it.currentValue }
            if (currentTime - lastPromptTime > tenDaysInMillis && actualCurrentValue > 0) {
                platformActions.requestInAppReview(
                    screenName = "InvestmentDashboardV2",
                    silentFallback = true,
                    trigger = "auto"
                )
                sessionStore.saveValue("last_review_prompt_time", currentTime.toString())
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

            if (!dashboardState.isLoading) {
                if (dashboardState.kycStatus.equals("IN_PROGRESS", ignoreCase = true)) {
                    item {
                        KycSubmittedAwaitingApprovalCard(
                            onContactSupport = {
                                platformActions.openWhatsApp("917676596301", "Hello, my KYC has been submitted and is currently awaiting approval.")
                            }
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

            if (!dashboardState.isLoading && (dashboardState.recommendedGoals.isNotEmpty() || dashboardState.allGoals.isNotEmpty())) {
                val nextGoals = (dashboardState.recommendedGoals + dashboardState.allGoals).filter { 
                    it.category.uppercase() !in listOf("RETIREMENT", "CHILDRENS_EDUCATION", "VACATION", "FESTIVAL_SPENDS", "SAVINGS") 
                }
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

            item {
                PoweredByAmcsSection()
            }

            item {
                Text(
                    text = "Mutual fund investments are subject to market risks. Read all scheme related documents carefully before investing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
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
            Box(modifier = Modifier.width(120.dp).height(20.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
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
                                formatWeight(goldUnitsInGm)
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
                                formatWeight(silverUnitsInGm)
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

                        // Goal Details - Below progress bar
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Current Value and Processing Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
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
                                        text = "Total Value",
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
    modifier: Modifier = Modifier
) {
    val gradientColors = getGoalGradientColors(goal.category, goal.colorTheme)
    val borderColor = getDarkBorderColorForCategory(goal.category, goal.colorTheme)
    val correlationColor = getCorrelationColorForCategory(goal.category, goal.colorTheme)
    val category = goal.category.uppercase()

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
                        modifier = Modifier.weight(0.4f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Daily SIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = when (category) {
                                "GOLD", "SAVINGS" -> "₹21 - ₹500"
                                "FESTIVAL_SPENDS" -> "₹11 - ₹500"
                                "GLOBAL_EXPOSURE", "ALL_IN_ONE" -> "₹101 - ₹1000"
                                else -> "₹101 - ₹500"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF424242)
                        )
                    }

                    Box(
                        modifier = Modifier.width(1.dp).height(32.dp).background(Color(0xFFE0E0E0))
                    )

                    Column(
                        modifier = Modifier.weight(0.6f).padding(start = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        val annotatedText = buildAnnotatedString {
                            when (category) {
                                "GOLD" -> {
                                    append("Investing ₹101 daily since Jan 2023 gives you power of ")
                                    withStyle(SpanStyle(color = Color(0xFFB8860B), fontWeight = FontWeight.Bold)) {
                                        append("~15.8g Gold")
                                    }
                                }
                                "SILVER" -> {
                                    append("Investing ₹101 daily since Jan 2023 yields ")
                                    withStyle(SpanStyle(color = Color(0xFF616161), fontWeight = FontWeight.Bold)) {
                                        append("~1.24kg Silver")
                                    }
                                }
                                "SAVINGS" -> {
                                    append("Investing ₹101 daily since Jan 2023 built a corpus of ")
                                    withStyle(SpanStyle(color = Color(0xFF004D40), fontWeight = FontWeight.Bold)) {
                                        append("~₹1.24 Lakhs")
                                    }
                                }
                                "FESTIVAL_SPENDS" -> {
                                    append("Investing ₹51 daily since Jan 2023 grew to ")
                                    withStyle(SpanStyle(color = Color(0xFFFF6F00), fontWeight = FontWeight.Bold)) {
                                        append("~₹62,408")
                                    }
                                }
                                "GLOBAL_EXPOSURE" -> {
                                    append("Investing ₹101 daily since Jan 2023 in global fund grew to ")
                                    withStyle(SpanStyle(color = Color(0xFF00897B), fontWeight = FontWeight.Bold)) {
                                        append("~₹1.54 Lakhs")
                                    }
                                }
                                "SAVINGS_PLUS" -> {
                                    append("Investing ₹101 daily since Jan 2023 in Savings Plus grew to ")
                                    withStyle(SpanStyle(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)) {
                                        append("~₹1.28 Lakhs")
                                    }
                                }
                                "ALL_IN_ONE" -> {
                                    append("Investing ₹101 daily since Jan 2023 in a multi-asset fund grew to ")
                                    withStyle(SpanStyle(color = Color(0xFF2C4C9C), fontWeight = FontWeight.Bold)) {
                                        append("~₹1.41 Lakhs")
                                    }
                                }
                                else -> append(goal.description)
                            }
                        }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF424242),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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
    val creamBg = Color(0xFFFAF9F1)
    val goldColor = Color(0xFFC5A358)
    val darkGreenText = Color(0xFF1B4332)

    Column(
        modifier = Modifier.fillMaxWidth().background(creamBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 4.dp) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = "Pyllar Money Icon",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Pyllar Money", fontWeight = FontWeight.Bold, color = darkGreenText)
                Text("Built for everyday Indians", style = MaterialTheme.typography.bodySmall, color = darkGreenText.copy(alpha = 0.7f))
            }
        }
        Box(modifier = Modifier.width(60.dp).height(1.dp).background(goldColor.copy(alpha = 0.3f)))
        Surface(shape = RoundedCornerShape(4.dp), color = Color.White, border = BorderStroke(1.dp, goldColor.copy(alpha = 0.2f))) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = goldColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AMFI Registered Mutual Fund Distributor", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = "Pyllar Fintech Private Limited is an AMFI registered Mutual Fund distributor (ARN No: 341847)",
            style = MaterialTheme.typography.labelSmall,
            color = darkGreenText.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycPendingBottomSheet(onDismiss: () -> Unit, onRetryKyc: () -> Unit, kycStatus: String) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("KYC Verification Pending", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your KYC is being processed. This usually takes 24-48 hours.", color = Color.Gray)
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = V2Obsidian)) {
                Text("OK")
            }
            if (kycStatus == "EXPIRED" || kycStatus == "REJECTED") {
                OutlinedButton(onClick = onRetryKyc, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Retry KYC")
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
