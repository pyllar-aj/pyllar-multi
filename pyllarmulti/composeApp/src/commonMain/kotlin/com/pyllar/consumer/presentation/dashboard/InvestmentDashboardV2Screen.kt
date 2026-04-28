package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.ceil

// --- Metal Texture Palettes & Constants ---
private val goldMetalColors = listOf(
    Color(0xFFC8892E), Color(0xFFE8C46A), Color(0xFFC9973A),
    Color(0xFFF0D080), Color(0xFFB8821A), Color(0xFFE0B84A), Color(0xFFC9973A)
)
private val silverMetalColors = listOf(
    Color(0xFF8A9DB0), Color(0xFFC8D8E4), Color(0xFF7A8FA0),
    Color(0xFFD8E8F0), Color(0xFF6A8090), Color(0xFFB8CCD8), Color(0xFF8A9DB0)
)
private const val GOLD_BRUSH_ALPHA = 10 / 255f
private const val SILVER_BRUSH_ALPHA = 15 / 255f
private val goldShadowColor = Color(0xFFB47814)
private val silverShadowColor = Color(0xFF506070)
private val goldStrokeColor = Color(0x80C9973A)
private val silverStrokeColor = Color(0x807A8FA0)

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
    onRetryKyc: () -> Unit = {},
    viewModel: InvestmentDashboardV2ViewModel = koinInject()
) {
    Log.d("InvestmentDashboardV2", "🎨 COMPOSABLE CALLED - userId: '$userId'")

    val dashboardState by viewModel.dashboardState.collectAsState()
    
    var isSelectingGoal by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InvestmentDashboardV2")
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadDashboardData(userId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfHeight = size.height / 2f
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1B5E20),
                    Color(0xFF2E7D32),
                    Color(0xFF388E3C),
                    Color(0xFF4CAF50),
                    Color(0xFF66BB6A),
                    Color(0xFFA5D6A7),
                    Color(0xFFE8F5E9)
                ),
                startY = 0f,
                endY = halfHeight
            )
            drawRect(brush = gradient)
            drawRect(
                color = Color.White,
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
                Spacer(modifier = Modifier.height(16.dp))
                UserHeader(
                    userName = dashboardState.userName,
                    isLoading = dashboardState.isLoading,
                    onClick = onNavigateToProfile,
                    onNavigateToHelp = onNavigateToHelp,
                    showMenu = showMenu,
                    onMenuClick = { showMenu = true },
                    onDismissMenu = { showMenu = false },
                    onShareClick = { /* Handle Share */ },
                    onLanguageClick = { /* Handle Language */ },
                    onRateUsClick = { /* Handle Rate Us */ }
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
                    isLoading = dashboardState.isLoading
                )
            }

            if (!dashboardState.isLoading) {
                item {
                    StatusInfoCard(
                        kycStatus = dashboardState.kycStatus,
                        mandateStatuses = dashboardState.fundDetails.mapNotNull { it.mandateStatus }.distinct(),
                        onRetryKyc = onRetryKyc
                    )
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
                        onClick = {
                            onNavigateToSchemeDetails(goal.goalId)
                        }
                    )
                }
                
                // Promotion Card if there are investments
                item {
                    PromotionShareCard(onShareClick = { /* Handle Share */ })
                }
            } else if (!dashboardState.isLoading && dashboardState.primaryGoals.isEmpty()) {
                // Show Journey Card if no goals
                item {
                    StartInvestmentJourneyCard(
                        onNeedHelpClick = { /* Handle WhatsApp */ },
                        onExploreClick = { 
                            // Scroll to recommended goals or handle explore
                        }
                    )
                }
            }

            if (!dashboardState.isLoading && dashboardState.milestoneMessage.isNotBlank() && dashboardState.hasFirstMilestone) {
                item {
                    MilestoneBanner(message = dashboardState.milestoneMessage)
                }
            }

            if (!dashboardState.isLoading && dashboardState.recommendedGoals.isNotEmpty()) {
                item {
                    NextGoalsSection(
                        goals = dashboardState.recommendedGoals + dashboardState.allGoals,
                        onGoalClick = { goalId ->
                            coroutineScope.launch {
                                isSelectingGoal = true
                                val result = viewModel.initGoalTxn(userId, goalId)
                                if (result is Resource.Success) {
                                    onNavigateToGoal(goalId)
                                }
                                isSelectingGoal = false
                            }
                        }
                    )
                }
            }

            item {
                DashboardTrustFooter()
            }

//            item {
//                PoweredByAmcsSection()
//            }

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
    onLanguageClick: () -> Unit = {},
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
            color = Color(0xFFE8F5E9)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.width(120.dp).height(20.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF4CAF50),
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
                Text(
                    text = " >",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.6f)
                )
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
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Language")
                        }
                    },
                    onClick = {
                        onDismissMenu()
                        onLanguageClick()
                    }
                )
            }
        }
    }
}

@Composable
fun CombinedDashboardCard(
    totalValue: Double,
    profitLoss: Double,
    profitLossPercentage: Double,
    goldUnitsInGm: Double?,
    silverUnitsInGm: Double?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gold & Silver Row
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Gold
                val goldSectionShape = RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(4.dp, goldSectionShape, spotColor = goldShadowColor, ambientColor = goldShadowColor)
                        .clip(goldSectionShape)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = goldMetalColors,
                                    start = Offset(0f, h),
                                    end = Offset(w, 0f)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                                    startY = 0f, endY = h
                                )
                            )
                            var y = 0f
                            while (y < h) {
                                drawLine(
                                    color = Color.White.copy(alpha = GOLD_BRUSH_ALPHA),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f
                                )
                                y += 3f
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isLoading) "..." else "${formatWeight(goldUnitsInGm ?: 0.0)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A3600)
                        )
                        Text("Gold", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6A4C00))
                    }
                }
                // Silver
                val silverSectionShape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(4.dp, silverSectionShape, spotColor = silverShadowColor, ambientColor = silverShadowColor)
                        .clip(silverSectionShape)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = silverMetalColors,
                                    start = Offset(0f, h),
                                    end = Offset(w, 0f)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                                    startY = 0f, endY = h
                                )
                            )
                            var y = 0f
                            while (y < h) {
                                drawLine(
                                    color = Color.White.copy(alpha = SILVER_BRUSH_ALPHA),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f
                                )
                                y += 3f
                            }
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = Color.White.copy(alpha = 0.4f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BrightnessLow,
                                    contentDescription = null,
                                    tint = Color(0xFFC0C0C0),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isLoading) "..." else "${formatWeight(silverUnitsInGm ?: 0.0)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C343A)
                        )
                        Text("Silver", style = MaterialTheme.typography.labelSmall, color = Color(0xFF505A61))
                    }
                }
            }

            // Total Value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD5ECD6))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Value", style = MaterialTheme.typography.titleSmall, color = Color(0xFF5F6F64))
                    Text(
                        text = if (isLoading) "₹..." else "₹${formatIndian(ceil(totalValue))}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1C1C1C)
                    )
                    if (!isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (profitLoss >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (profitLoss >= 0) Color(0xFF2E7D32) else Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = " ₹${formatIndian(profitLoss)} (${formatPercent(profitLossPercentage)}%)",
                                color = if (profitLoss >= 0) Color(0xFF2E7D32) else Color.Red,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusInfoCard(
    kycStatus: String,
    mandateStatuses: List<String>,
    onRetryKyc: () -> Unit
) {
    val isKycPending = kycStatus.uppercase() != "SUCCESS" && kycStatus.uppercase() != "COMPLETED"
    if (!isKycPending) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("KYC Status", style = MaterialTheme.typography.labelMedium)
                Text(formatKycStatus(kycStatus), fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }
            Button(
                onClick = onRetryKyc,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text("Complete KYC")
            }
        }
    }
}

@Composable
fun PrimaryGoalCard(
    goal: InvestmentGoal,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.iconType, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(goal.name, fontWeight = FontWeight.Bold)
                    Text(goal.schemeName ?: "Direct Plan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${formatIndian(goal.currentValue)}", fontWeight = FontWeight.Bold)
                    Text("${formatPercent(goal.returnsPercentage)}%", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (goal.progressPercentage / 100f).toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = getCorrelationColorForCategory(goal.category, goal.colorTheme)
            )
        }
    }
}

@Composable
fun MilestoneBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎉", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1565C0))
        }
    }
}

@Composable
fun NextGoalsSection(
    goals: List<InvestmentGoal>,
    onGoalClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Your Next Goals",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        goals.forEach { goal ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onGoalClick(goal.goalId) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.iconType, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.name, fontWeight = FontWeight.Bold)
                        Text(goal.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
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
    val cardLightGreenTop = Color(0xFFE8F5E9)
    val cardLightGreenBottom = Color(0xFFC8E6C9)
    val topBorderGreenDark = Color(0xFF2E7D32)
    val topBorderGreen = Color(0xFF66BB6A)
    val rocketGreenDark = Color(0xFF388E3C)
    val rocketGreen = Color(0xFF66BB6A)
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
                Text("Start Your Investment Journey", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF1B5E20), textAlign = TextAlign.Center)
                Text("Expertly curated investment options", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
                Text("Grow your wealth with Pyllar. Simple, secure, and smart.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                
                Button(onClick = onExploreClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
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
                    Text("P", fontWeight = FontWeight.Bold, color = darkGreenText)
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
                Text("SEBI Registered Investment Platform", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
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
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
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
