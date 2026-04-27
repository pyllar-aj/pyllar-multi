package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.navigation.AppRoutes
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── Goal colour helpers (pure Compose, no android.graphics) ──────────────────

private fun goalGradient(category: String): List<Color> = when (category.uppercase()) {
    "GOLD" -> listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3), Color(0xFFFFD54F))
    "SILVER" -> listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFBDBDBD))
    "SAVINGS" -> listOf(Color(0xFFE0F2F1), Color(0xFFB2DFDB), Color(0xFF80CBC4))
    "FESTIVAL_SPENDS" -> listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80))
    "CHILDRENS_EDUCATION" -> listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9), Color(0xFF9FA8DA))
    "VACATION" -> listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9), Color(0xFFB39DDB))
    else -> listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9))
}

private fun goalTextColor(category: String): Color = when (category.uppercase()) {
    "GOLD" -> Color(0xFF5D4037)
    "SILVER" -> Color(0xFF424242)
    "SAVINGS" -> Color(0xFF004D40)
    "FESTIVAL_SPENDS" -> Color(0xFF5D4037)
    "CHILDRENS_EDUCATION" -> Color(0xFF283593)
    "VACATION" -> Color(0xFF4A148C)
    else -> Color(0xFF1565C0)
}

private fun goalBorderColor(category: String): Color = when (category.uppercase()) {
    "GOLD" -> Color(0xFFFFB300)
    "SILVER" -> Color(0xFF9E9E9E)
    "SAVINGS" -> Color(0xFF00897B)
    "FESTIVAL_SPENDS" -> Color(0xFFFF6F00)
    "CHILDRENS_EDUCATION" -> Color(0xFF3949AB)
    "VACATION" -> Color(0xFF7B1FA2)
    else -> Color(0xFF1976D2)
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InitialDashboardScreen(
    userId: String = "",
    onNavigateToOnboarding: (goalId: String, userId: String) -> Unit,
    onNavigateToRoute: (screen: String, preVerificationId: String?) -> Unit = { _, _ -> }
) {
    val viewModel: InitialDashboardViewModel = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val goalsState by viewModel.goalsState.collectAsState()

    val allGoals = remember(goalsState.primaryGoals, goalsState.recommendedGoals) {
        (goalsState.primaryGoals + goalsState.recommendedGoals)
            .filter { goal ->
                val cat = goal.category.uppercase()
                cat == "GOLD" || cat == "SILVER" || cat == "SAVINGS"
            }
            .sortedBy { goal ->
                when (goal.category.uppercase()) {
                    "GOLD" -> 1; "SILVER" -> 2; "SAVINGS" -> 3; else -> 4
                }
            }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("InitialDashboard")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Share button row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = { PlatformAnalyticsLogger.logEvent("share_app_clicked", mapOf("screen" to "InitialDashboard")) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Hero card
            item { StartInvestingHeroCard() }

            // Title
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Choose Your Goal", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Start small, build wealth. Pick a goal to begin your investment journey.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                }
            }

            // Error
            if (errorMessage != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(errorMessage ?: "", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            // Loading
            if (goalsState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Goal cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        allGoals.forEach { goal ->
                            InitialGoalCard(
                                goal = goal,
                                bucketData = goalsState.growthData[goal.goalId] ?: goalsState.growthData[goal.category.lowercase()],
                                onClick = {
                                    if (isSubmitting) return@InitialGoalCard
                                    if (userId.isBlank()) {
                                        errorMessage = "User ID is missing. Please log in again."
                                        return@InitialGoalCard
                                    }
                                    isSubmitting = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val result = viewModel.selectGoal(userId = userId, goalId = goal.goalId)
                                            when (result) {
                                                is Resource.Success -> {
                                                    val nextScreen = result.navigation?.nextScreen
                                                    val userPurposeId = result.data?.userPurposeId
                                                    
                                                    if (!nextScreen.isNullOrBlank()) {
                                                        platformLog("🚀 Server-driven navigation to: $nextScreen (purposeId: $userPurposeId)")
                                                        onNavigateToRoute(nextScreen, userPurposeId)
                                                    } else {
                                                        platformLog("⚠️ No nextScreen in response, falling back to default onboarding")
                                                        onNavigateToOnboarding(goal.goalId, userId)
                                                    }
                                                }
                                                is Resource.Error -> {
                                                    errorMessage = result.message ?: "Unable to select goal"
                                                }
                                                else -> Unit
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Something went wrong"
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        // Submitting overlay
        if (isSubmitting) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun StartInvestingHeroCard() {
    val gradientColors = listOf(
        Color(0xFF0D4D2B), Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF1B4D1B)
    )
    val lightGreenAccent = Color(0xFFA5D6A7)
    val panelColor = Color(0xFF2E7D32).copy(alpha = 0.85f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(brush = Brush.verticalGradient(gradientColors), shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("START INVESTING", style = MaterialTheme.typography.labelMedium,
                        color = Color.White, fontWeight = FontWeight.Medium)
                    Surface(shape = CircleShape, color = Color(0xFFFFB300)) {
                        Text("🪙", modifier = Modifier.padding(6.dp),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Small SIPs,")
                        withStyle(SpanStyle(color = lightGreenAccent, fontWeight = FontWeight.Bold)) {
                            append(" big dreams.")
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(icon = "🏷️", text = "0% GST")
                    TagPill(icon = "🔓", text = "No Lock-in")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = panelColor) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PerformanceColumn("Gold (2Y)", "+148%")
                            PerformanceColumn("Silver (2Y)", "+266%")
                        }
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Savings goal earns", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                                Text("Up to 7% returns", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagPill(icon: String, text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.7f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, style = MaterialTheme.typography.bodySmall)
            Text(text, style = MaterialTheme.typography.labelSmall, color = Color(0xFF424242))
        }
    }
}

@Composable
private fun PerformanceColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.95f))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

// ── Goal card ─────────────────────────────────────────────────────────────────

@Composable
fun InitialGoalCard(
    goal: InvestmentGoal,
    bucketData: BucketGrowthData? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = goalGradient(goal.category)
    val textColor = goalTextColor(goal.category)
    val borderColor = goalBorderColor(goal.category)

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(brush = Brush.verticalGradient(gradientColors), shape = RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {

                // Header: icon + name
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape,
                        color = Color.White.copy(alpha = 0.6f)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(goal.iconType.ifBlank { "🎯" }, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SIP range + description
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(0.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Daily SIP", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                        Text(
                            text = when (goal.category.uppercase()) {
                                "GOLD", "SAVINGS" -> "₹21–₹500"
                                "FESTIVAL_SPENDS" -> "₹11–₹500"
                                else -> "₹101–₹500"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor, textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.width(2.dp).height(50.dp).background(textColor.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(
                            text = buildGoalDescription(goal.category, bucketData),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action button
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(goal.actionButtonText, style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

private fun Double.format(digits: Int): String {
    val factor = when(digits) {
        1 -> 10.0
        2 -> 100.0
        else -> 1.0
    }
    return if (digits == 0) {
        (this + 0.5).toLong().toString()
    } else {
        ((this * factor).toLong() / factor).toString()
    }
}

private fun buildGoalDescription(category: String, bucketData: BucketGrowthData?): String {
    return when (category.uppercase()) {
        "GOLD" -> {
            val weight = bucketData?.accumulatedUnits?.let { "${it.format(2)}g" } ?: "~15.8g"
            val date = bucketData?.startDate ?: "Jan 2023"
            "₹101/day since $date gives you purchasing power of $weight Gold."
        }
        "SILVER" -> {
            val weight = bucketData?.accumulatedUnits?.let { "${it.format(0)}g" } ?: "~1.24kg"
            val date = bucketData?.startDate ?: "Jan 2023"
            "₹101/day since $date yields $weight Silver."
        }
        "SAVINGS" -> {
            val corpus = bucketData?.currentValuation?.let { "₹${it.toLong()}" } ?: "₹1.24 Lakhs"
            val date = bucketData?.startDate ?: "Jan 2023"
            "₹101/day since $date builds ~$corpus corpus."
        }
        "FESTIVAL_SPENDS" -> {
            val corpus = bucketData?.currentValuation?.let { "₹${it.toLong()}" } ?: "₹62,408"
            val date = bucketData?.startDate ?: "Jan 2023"
            "₹51/day since $date grew to ~$corpus."
        }
        else -> "Start investing to grow your wealth."
    }
}
