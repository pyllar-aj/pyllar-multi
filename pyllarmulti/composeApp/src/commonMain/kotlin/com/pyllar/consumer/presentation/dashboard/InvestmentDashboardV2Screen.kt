package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val coroutineScope = rememberCoroutineScope()
    val dashboardState by viewModel.dashboardState.collectAsState()
    
    var isSelectingGoal by remember { mutableStateOf(false) }

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

        LazyColumn(
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
                    onNavigateToHelp = onNavigateToHelp
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
    onNavigateToHelp: () -> Unit
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
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.White)
            }
        } else {
            Text(
                text = "Hi ${userName.ifBlank { "User" }} >",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        TextButton(onClick = onNavigateToHelp) {
            Text("Help", color = Color.White)
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
                Box(
                    modifier = Modifier.weight(1f).background(Color(0xFFFFF9E6)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isLoading) "..." else "${formatWeight(goldUnitsInGm)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA27915)
                        )
                        Text("Gold", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA27915))
                    }
                }
                // Silver
                Box(
                    modifier = Modifier.weight(1f).background(Color(0xFFFAFAFA)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BrightnessLow,
                            contentDescription = null,
                            tint = Color(0xFFC0C0C0),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isLoading) "..." else "${formatWeight(silverUnitsInGm)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818181)
                        )
                        Text("Silver", style = MaterialTheme.typography.labelSmall, color = Color(0xFF818181))
                    }
                }
            }

            // Total Value
            Box(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFC8E6C9)).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Value", style = MaterialTheme.typography.titleSmall, color = Color.Black.copy(alpha = 0.6f))
                    Text(
                        text = if (isLoading) "₹..." else "₹${formatIndian(ceil(totalValue))}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
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
fun PoweredByAmcsSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Powered by Leading AMCs", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Placeholders for AMC logos
            Text("AXIS", fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("SBI", fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("HDFC", fontWeight = FontWeight.Bold, color = Color.LightGray)
            Text("ICICI", fontWeight = FontWeight.Bold, color = Color.LightGray)
        }
    }
}

private fun formatWeight(units: Double?): String {
    if (units == null || units <= 0) return "0 g"
    return if (units < 1.0) {
        "${(units * 1000).toInt()} mg"
    } else {
        "${(units).toString().take(5)} g"
    }
}
