package com.pyllar.consumer.presentation.mutualfund.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.domain.models.PortfolioResponse
import com.pyllar.consumer.domain.models.PurchaseOrder
import com.pyllar.consumer.domain.models.SipOrder
import com.pyllar.consumer.util.Resource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    userId: String,
    viewModel: PortfolioViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    val portfolioResult by viewModel.portfolioResult.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadPortfolio(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portfolio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPortfolio(userId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val result = portfolioResult) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(result.message ?: "Failed to load portfolio", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.refreshPortfolio(userId) }) { Text("Retry") }
                    }
                }
                is Resource.Success -> {
                    PortfolioContent(portfolio = result.data)
                }
                null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Welcome to your portfolio")
                    }
                }
            }
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun PortfolioContent(portfolio: PortfolioResponse?) {
    if (portfolio == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No portfolio data available")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            // Summary cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("Active SIPs", "${portfolio.activeSips}", Modifier.weight(1f))
                SummaryCard("Total SIPs", "${portfolio.totalSips}", Modifier.weight(1f))
                SummaryCard("Purchases", "${portfolio.totalPurchases}", Modifier.weight(1f))
            }
        }

        if (!portfolio.sipOrders.isNullOrEmpty()) {
            item {
                Text(
                    "SIP Orders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(portfolio.sipOrders) { sip ->
                SipOrderCard(sip)
            }
        }

        if (!portfolio.purchaseOrders.isNullOrEmpty()) {
            item {
                Text(
                    "Purchase Orders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(portfolio.purchaseOrders) { purchase ->
                PurchaseOrderCard(purchase)
            }
        }

        if (portfolio.sipOrders.isNullOrEmpty() && portfolio.purchaseOrders.isNullOrEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No investments yet. Start your first SIP!")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SipOrderCard(sip: SipOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    sip.fundSchemeName ?: "SIP Order",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (sip.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )) {
                    Text(
                        if (sip.active) "ACTIVE" else "INACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sip.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabelValue("Amount", "₹${sip.amount}")
                LabelValue("Frequency", sip.frequency)
                LabelValue("Start Date", sip.startDate)
            }
        }
    }
}

@Composable
private fun PurchaseOrderCard(purchase: PurchaseOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    purchase.fundSchemeName ?: "Purchase Order",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Card(colors = CardDefaults.cardColors(
                    containerColor = when (purchase.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.primary
                        "PENDING" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )) {
                    Text(
                        purchase.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (purchase.status) {
                            "COMPLETED" -> MaterialTheme.colorScheme.onPrimary
                            "PENDING" -> MaterialTheme.colorScheme.onSecondary
                            else -> MaterialTheme.colorScheme.onError
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabelValue("Amount", "₹${purchase.amount}")
                LabelValue("Date", purchase.investmentDate)
            }
            Text("Order ID: ${purchase.id.takeLast(8)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    }
}
