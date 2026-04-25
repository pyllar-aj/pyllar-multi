package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
fun WithdrawScreen(
    userId: String = "",
    selectedGoal: InvestmentGoal? = null,
    onNavigateBack: () -> Unit = {},
    onProceed: (String?, WithdrawScheme?) -> Unit = { _, _ -> },
    viewModel: WithdrawViewModel = koinInject()
) {
    val state by viewModel.withdrawState.collectAsState()
    
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            val params = WithdrawParamsManager.get()
            if (params != null) {
                viewModel.loadWithdrawDataWithParams(userId, params)
            } else {
                viewModel.loadWithdrawData(userId, selectedGoal)
            }
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("Withdraw")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw Funds", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            WithdrawBalanceCard(
                                available = state.availableToWithdraw,
                                inProgress = state.investmentInProgress
                            )
                        }

                        item {
                            Text(
                                "Select Investment to Withdraw",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        items(state.schemes) { scheme ->
                            WithdrawSchemeItem(
                                scheme = scheme,
                                isSelected = state.selectedSchemeId == scheme.id,
                                onSelect = { viewModel.selectScheme(scheme.id) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val selected = state.schemes.find { it.id == state.selectedSchemeId }
                            selected?.let { WithdrawSchemeManager.set(it) }
                            onProceed(state.selectedSchemeId, selected)
                        },
                        enabled = state.selectedSchemeId != null,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Proceed to Withdraw", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawBalanceCard(available: Double, inProgress: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available to Withdraw", style = MaterialTheme.typography.bodyMedium)
                Text("\u20B9${formatCurrency(available)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
            }
            if (inProgress > 0) {
                Text(
                    "Investment in progress: \u20B9${formatCurrency(inProgress)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.7f)
                )
            }
        }
    }
}

@Composable
fun WithdrawSchemeItem(scheme: WithdrawScheme, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(scheme.schemeName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                if (scheme.folioNo != null) {
                    Text("Folio: ${scheme.folioNo}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("\u20B9${formatCurrency(scheme.currentValue)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
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
