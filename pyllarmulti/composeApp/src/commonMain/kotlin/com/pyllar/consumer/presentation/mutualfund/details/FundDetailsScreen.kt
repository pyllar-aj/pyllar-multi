package com.pyllar.consumer.presentation.mutualfund.details

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
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.platformLog
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundDetailsScreen(
    isin: String = "",
    userId: String = "",
    goalId: String = "",
    sipAmount: Double = 0.0,
    onBackClick: () -> Unit = {},
    onSipCreated: (Double, String?) -> Unit = { _, _ -> },
    viewModel: FundDetailsViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

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
                    // Implementation for Invest button
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
                        
                        // Chart Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.LightGray.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("NAV Chart Placeholder (KMP)", modifier = Modifier.alpha(0.5f))
                        }

                        FundMetricsGrid(
                            expenseRatio = details.expenseRatio?.toString() ?: "-",
                            aum = details.aum?.toString() ?: "-",
                            exitLoad = details.exitLoad?.toString() ?: "-"
                        )
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
