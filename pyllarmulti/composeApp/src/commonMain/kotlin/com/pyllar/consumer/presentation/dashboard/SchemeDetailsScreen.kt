package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.platformLog
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailsScreen(
    userId: String = "",
    purpose: String = "",
    onNavigateBack: () -> Unit = {},
    onNavigateToWithdraw: (WithdrawInitParams) -> Unit = {},
    onNavigateToAddFunds: (String) -> Unit = {},
    viewModel: SchemeDetailsViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val schemeParams = remember { SchemeDetailsParamsManager.get() }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetails")
    }

    LaunchedEffect(userId, purpose) {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            viewModel.loadTransactions(userId, purpose, schemeParams)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (!state.goalName.isNullOrBlank()) formatGoalName(state.goalName!!) else "Scheme Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadTransactions(userId, purpose, schemeParams) }) {
                        Text("Retry")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SchemeSummaryCard(
                                schemeName = state.schemeName ?: "Scheme",
                                currentValue = state.currentValue,
                                investedAmount = state.investedAmount,
                                totalGain = state.totalGain,
                                category = state.category,
                                colorTheme = state.colorTheme
                            )
                        }

                        item {
                            TabRow(selectedTabIndex = selectedTabIndex) {
                                Tab(
                                    selected = selectedTabIndex == 0,
                                    onClick = { selectedTabIndex = 0 },
                                    text = { Text("Transactions") }
                                )
                                Tab(
                                    selected = selectedTabIndex == 1,
                                    onClick = { selectedTabIndex = 1 },
                                    text = { Text("Plans") }
                                )
                            }
                        }

                        if (selectedTabIndex == 0) {
                            if (state.transactions.isEmpty()) {
                                item {
                                    Text(
                                        "No transactions found",
                                        modifier = Modifier.fillMaxWidth().padding(32.dp).alpha(0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(state.transactions) { tx ->
                                    TransactionRow(tx)
                                }
                            }
                        } else {
                            if (state.mandates.isEmpty()) {
                                item {
                                    Text(
                                        "No active plans found",
                                        modifier = Modifier.fillMaxWidth().padding(32.dp).alpha(0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(state.mandates) { mandate ->
                                    MandateRow(mandate)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToAddFunds(purpose) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Invest More")
                        }
                        OutlinedButton(
                            onClick = {
                                val params = WithdrawInitParams(
                                    isin = state.isin ?: "",
                                    folio = state.folioNumber,
                                    amount = state.currentValue,
                                    investmentInProgress = state.investmentInProgress,
                                    schemeName = state.schemeName
                                )
                                onNavigateToWithdraw(params)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Withdraw")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchemeSummaryCard(
    schemeName: String,
    currentValue: Double,
    investedAmount: Double,
    totalGain: Double,
    category: String?,
    colorTheme: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = formatSchemeName(schemeName),
                style = MaterialTheme.typography.bodyMedium,
                color = getCorrelationColorForCategory(category, colorTheme)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Current Value", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                    Text("\u20B9${formatCurrency(currentValue)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Gain", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                    Text(
                        text = (if (totalGain >= 0) "+" else "") + "\u20B9${formatCurrency(totalGain)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (totalGain >= 0) Color(0xFF2E7D32) else Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionDisplayItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(tx.transactionType ?: "Transaction", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(tx.date ?: "", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (tx.isCredit) "+" else "-") + "\u20B9${formatCurrency(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (tx.isCredit) Color(0xFF2E7D32) else Color.Black
            )
            Text(tx.state, style = MaterialTheme.typography.labelSmall, color = when(tx.state) {
                "SUCCESS" -> Color(0xFF2E7D32)
                "SUBMITTED" -> Color(0xFF1565C0)
                "FAILED" -> Color.Red
                else -> Color.Gray
            })
        }
    }
}

@Composable
fun MandateRow(mandate: MandateDisplayItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("SIP Amount", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                Text("\u20B9${formatCurrency(mandate.amount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Status", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                Text(mandate.status ?: "Active", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            }
        }
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
