package com.pyllar.consumer.presentation.mutualfund.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.presentation.dashboard.formatIndian
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.pyllar.consumer.data.remote.model.dto.NavChartDataDto
import com.pyllar.consumer.data.remote.model.dto.FundReturnsDto
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundDetailsScreen(
    isin: String = "",
    userId: String = "",
    goalId: String = "",
    sipAmount: Double = 0.0,
    kycAttemptId: String = "",
    investorId: String = "",
    onBackClick: () -> Unit = {},
    onSipCreated: (Double, String?) -> Unit = { _, _ -> },
    viewModel: FundDetailsViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("FundDetails")
    }

    LaunchedEffect(isin, userId, goalId) {
        if (isin.isNotBlank()) {
            viewModel.loadFundDetails(isin)
        } else if (userId.isNotBlank() && goalId.isNotBlank()) {
            viewModel.loadFundDetailsByGoal(userId, goalId)
        }
    }

    if (state.sipError != null) {
        AlertDialog(
            onDismissRequest = { /* Clear error in VM if needed, but for now just hide */ },
            title = { Text("Error") },
            text = { Text(state.sipError!!) },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearSipError()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.fundDetails?.fundName ?: "Fund Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            FundDetailsBottomBar(
                isLoading = state.isSipCreating,
                sipAmount = sipAmount,
                onInvestClick = {
                    if (sipAmount <= 0) return@FundDetailsBottomBar
                    
                    coroutineScope.launch {
                        val result = viewModel.createSip(
                            userId = userId,
                            kycAttemptId = kycAttemptId,
                            investorId = investorId,
                            amount = sipAmount
                        )
                        if (result is SipCreationResult.Success) {
                            onSipCreated(sipAmount, result.nextScreen)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else {
                state.fundDetails?.let { details ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        FundHeader(details.fundName ?: "", details.category ?: "")
                        
                        FundChartSection(
                            state = state,
                            onPeriodSelected = { viewModel.onPeriodSelected(it) }
                        )

                        RiskometerSection(details.riskLevel ?: "MODERATE")

                        FundMetricsGrid(
                            expenseRatio = details.expenseRatio?.toString() ?: "-",
                            aum = details.aum?.toString() ?: "-",
                            exitLoad = details.exitLoad?.toString() ?: "-"
                        )

                        FundDescriptionSection(details.fundName ?: "")
                    }
                }
            }
        }
    }
}

@Composable
fun FundHeader(name: String, category: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
        ) {
            Text(
                category,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun FundMetricsGrid(expenseRatio: String, aum: String, exitLoad: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Fund Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricItem("Expense Ratio", expenseRatio, Modifier.weight(1f))
            MetricItem("Fund Size (AUM)", "₹$aum Cr", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricItem("Exit Load", "$exitLoad%", Modifier.weight(1f))
            MetricItem("Lock-in", "-", Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FundDetailsBottomBar(isLoading: Boolean, sipAmount: Double, onInvestClick: () -> Unit) {
    Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SIP Amount", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                Text("₹$sipAmount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onInvestClick,
                modifier = Modifier.height(56.dp).weight(1.5f),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Invest Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun FundChartSection(
    state: FundDetailsState,
    onPeriodSelected: (String) -> Unit
) {
    val positiveColor = Color(0xFF4CAF50)
    val negativeColor = Color(0xFFF44336)
    val lineColor = if (state.isPositiveReturn) positiveColor else negativeColor

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1Y", "3Y", "5Y").forEach { period ->
                val selected = state.selectedPeriod == period
                FilterChip(
                    selected = selected,
                    onClick = { onPeriodSelected(period) },
                    label = { 
                        Text(
                            text = period,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        val returns = state.fundDetails?.returns
        if (returns != null) {
            val returnVal = when (state.selectedPeriod) {
                "1Y" -> returns.oneYear
                "3Y" -> returns.threeYear
                "5Y" -> returns.fiveYear
                else -> 0.0
            } ?: 0.0
            val returnColor = if (returnVal >= 0) positiveColor else negativeColor
            val sign = if (returnVal >= 0) "+" else ""
            
            Text(
                text = "$sign${returnVal}% CAGR (${state.selectedPeriod})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = returnColor
            )
        }

        if (state.chartData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No chart data available", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            SimpleNavChart(
                data = state.chartData,
                lineColor = lineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
fun SimpleNavChart(
    data: List<NavChartDataDto>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val minNav = data.minOf { it.nav }.toFloat()
        val maxNav = data.maxOf { it.nav }.toFloat()
        val navRange = maxNav - minNav
        
        val width = size.width
        val height = size.height
        val padding = 8.dp.toPx()
        
        val usableWidth = width - (2 * padding)
        val usableHeight = height - (2 * padding)

        val myPath = Path()
        val fillPath = Path()

        data.forEachIndexed { index, point ->
            val x = padding + (index.toFloat() / (data.size - 1)) * usableWidth
            val y = padding + usableHeight - ((point.nav.toFloat() - minNav) / navRange.coerceAtLeast(0.1f)) * usableHeight
            
            if (index == 0) {
                myPath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                myPath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (index == data.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)
            ),
            style = Fill
        )

        val strokeWidth = 3.dp.toPx()
        drawPath(
            path = myPath,
            color = lineColor,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun RiskometerSection(riskLevel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Riskometer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 12.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            ) {
                val progress = when(riskLevel.uppercase()) {
                    "LOW" -> 0.2f
                    "MODERATELY_LOW" -> 0.4f
                    "MODERATE" -> 0.6f
                    "MODERATELY_HIGH" -> 0.8f
                    "HIGH" -> 0.9f
                    "VERY_HIGH" -> 1.0f
                    else -> 0.5f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(getRiskColor(riskLevel), CircleShape)
                )
            }
            Text(
                riskLevel.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = getRiskColor(riskLevel)
            )
        }
        Text(
            "This fund has ${riskLevel.lowercase().replace("_", " ")} risk",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.7f)
        )
    }
}

fun getRiskColor(riskLevel: String): Color {
    return when(riskLevel.uppercase()) {
        "LOW", "MODERATELY_LOW" -> Color(0xFF4CAF50)
        "MODERATE" -> Color(0xFFFFC107)
        "MODERATELY_HIGH" -> Color(0xFFFF9800)
        "HIGH", "VERY_HIGH" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

@Composable
fun FundDescriptionSection(name: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("About the Fund", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "$name is an open-ended equity scheme that seeks to generate long-term capital appreciation by investing in a diversified portfolio of companies.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alpha(0.8f)
        )
    }
}
