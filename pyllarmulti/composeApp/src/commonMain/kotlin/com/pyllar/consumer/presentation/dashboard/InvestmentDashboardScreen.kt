package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.presentation.mutualfund.onboarding.OnboardingViewModel
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun InvestmentDashboardScreen(
    userId: String = "",
    onNavigateToPortfolio: () -> Unit = {},
    onNavigateToSchemeDetails: (String) -> Unit = {},
    onNavigateToWithdraw: () -> Unit = {},
    onNavigateToOnboarding: (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    viewModel: DashboardViewModel = koinInject()
) {
    platformLog("InvestmentDashboardScreen: \uD83C\uDFA8 COMPOSABLE CALLED - userId: '$userId'")
    
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InvestmentDashboard")
    }
    
    val dashboardState by viewModel.dashboardState.collectAsState()
    
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadDashboardData(userId)
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Pyllar", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Simulated "money falling" or welcome emoji
                    Text(
                        text = "\uD83D\uDCE6 Welcome, ${dashboardState.username}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    PortfolioSummaryCard(
                        totalInvested = dashboardState.totalInvested,
                        currentValue = dashboardState.currentValue,
                        totalReturns = dashboardState.totalReturns,
                        xirr = dashboardState.xirr,
                        isLoading = dashboardState.isLoading
                    )
                }

                item {
                    DailySipScheduleCard(
                        todayDate = "Today",
                        todayStatus = when (dashboardState.sipStatus) {
                            SipStatus.COMPLETED -> "Completed"
                            SipStatus.PENDING -> "Pending"
                            SipStatus.SKIPPED -> "Skipped"
                        },
                        nextDate = "Tomorrow",
                        nextStatus = "Upcoming"
                    )
                }
                
                item {
                    PastPerformanceCard(points = dashboardState.portfolioGrowth)
                }
                
                item { RecentTransactionsSection(transactions = dashboardState.dailyTrends.take(3)) }
                
                item {
                    GoalSection(
                        onInvestMore = onNavigateToPortfolio,
                        dailySipAmount = dashboardState.dailySipAmount
                    )
                }

                item {
                    ActionsCard(onInvestMore = onNavigateToPortfolio)
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToPortfolio,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Portfolio")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.refreshDashboardData(userId)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Refresh")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioSummaryCard(
    totalInvested: Double,
    currentValue: Double,
    totalReturns: Double,
    xirr: Double,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFF1B5E20))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(
                    text = "\u20B9${formatCurrency(currentValue)}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Invested: \u20B9${formatCurrency(totalInvested)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text("\uD83D\uDCC8", fontSize = 24.sp)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (totalReturns >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "\u20B9${formatCurrency(totalReturns)}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                    
                    Text(
                        text = "${formatDouble(xirr)}% XIRR",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DailySipScheduleCard(
    todayDate: String,
    todayStatus: String,
    nextDate: String,
    nextStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Daily SIP Schedule",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\uD83D\uDCC5", fontSize = 20.sp)
                    Text("Today's SIP")
                }
                StatusChip(todayStatus)
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\u23F3", fontSize = 20.sp)
                    Text("Next SIP")
                }
                StatusChip(nextStatus)
            }
        }
    }
}

@Composable
fun PastPerformanceCard(points: List<PortfolioGrowthPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Past Performance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            PortfolioPerformanceChart(growthPoints = points)
        }
    }
}

@Composable
fun PortfolioPerformanceChart(growthPoints: List<PortfolioGrowthPoint>) {
    if (growthPoints.isEmpty()) return
    val maxValue = growthPoints.maxOf { it.value }
    val minValue = growthPoints.minOf { it.value }
    val range = (maxValue - minValue).coerceAtLeast(1.0)
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val path = Path()
        val stepX = size.width / (growthPoints.size - 1).toFloat()
        val pts = growthPoints.mapIndexed { index, p ->
            val x = index * stepX
            val y = size.height - (((p.value - minValue) / range) * size.height * 0.8f).toFloat()
            Offset(x, y)
        }
        pts.forEachIndexed { i, o -> if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
        drawPath(path = path, color = primaryColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun RecentTransactionsSection(transactions: List<DailyTrend>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            transactions.forEach { t ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = t.date, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Invested", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.7f))
                    }
                    Text(text = "\u20B9${formatCurrency(t.amount)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun GoalSection(onInvestMore: () -> Unit, dailySipAmount: Double = 100.0) {
    val progress = 0.35f
    val achievable = dailySipAmount * 365 * 1.5

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Goal Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Target by next year: \u20B9${formatCurrency(achievable)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun ActionsCard(onInvestMore: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onInvestMore, modifier = Modifier.weight(1f)) {
                Text("Invest More")
            }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("Withdraw")
            }
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    val (bg, fg) = when (text.lowercase()) {
        "completed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "upcoming" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "pending" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "skipped" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val s = amount.toLong().toString()
    if (s.length <= 3) return s
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val grouped = buildString {
        for ((i, c) in rest.reversed().withIndex()) {
            if (i > 0 && i % 2 == 0) append(',')
            append(c)
        }
    }.reversed()
    return "$grouped,$last3"
}

private fun formatDouble(value: Double): String {
    val s = value.toString()
    return if (s.contains('.')) {
        val parts = s.split('.')
        parts[0] + "." + parts[1].take(2)
    } else s
}
