package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onProceed: (String?, WithdrawScheme?) -> Unit,
    viewModel: WithdrawViewModel = koinInject()
) {
    // State to track if initial data loading is complete
    var isInitialLoadComplete by remember { mutableStateOf(false) }
    
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            platformLog("WithdrawScreen: 🔍 LaunchedEffect - userId: $userId")
            
            // Wait a bit to ensure params are set from navigation (matching Android delay)
            delay(100)
            
            val params = WithdrawParamsManager.get()
            platformLog("WithdrawScreen: 🔍 Params from manager: $params")
            
            if (params != null) {
                platformLog("WithdrawScreen: ✅ Loading withdraw data WITH params")
                viewModel.loadWithdrawDataWithParams(userId, params)
            } else {
                viewModel.loadWithdrawData(userId)
            }
            
            // Mark initial load as complete
            isInitialLoadComplete = true
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("Withdraw")
    }

    val state by viewModel.withdrawState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Show loading screen until initial load is complete AND data is loaded
    val isLoading = !isInitialLoadComplete || state.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw Funds", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading withdrawal data...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }


                    // Balance Summary Card
                    item {
                        BalanceSummaryCard(
                            investmentInProgress = state.investmentInProgress,
                            withdrawalInProgress = state.withdrawalInProgress,
                            availableToWithdraw = state.availableToWithdraw
                        )
                    }

                    item {
                        Text(
                            text = "Select withdrawal mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Regular withdrawal option
                    item {
                        RegularWithdrawalCard(
                            amount = state.availableToWithdraw,
                            onClick = {
                                if (state.selectedSchemeId != null) {
                                    val selectedScheme = state.schemes.find { it.id == state.selectedSchemeId }
                                    selectedScheme?.let { WithdrawSchemeManager.set(it) }
                                    onProceed(state.selectedSchemeId, selectedScheme)
                                }
                            }
                        )
                    }

                    // Schemes list (only show if multiple schemes)
                    if (state.schemes.size > 1) {
                        items(state.schemes) { scheme ->
                            SchemeSelectionItem(
                                scheme = scheme,
                                isSelected = state.selectedSchemeId == scheme.id,
                                onSelect = { viewModel.selectScheme(scheme.id) }
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // Auto-select if only one scheme
                LaunchedEffect(state.schemes) {
                    if (state.schemes.size == 1 && state.selectedSchemeId == null) {
                        viewModel.selectScheme(state.schemes[0].id)
                    }
                }

                // Proceed button (floating at bottom)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = {
                            val selected = state.schemes.find { it.id == state.selectedSchemeId }
                            selected?.let { WithdrawSchemeManager.set(it) }
                            onProceed(state.selectedSchemeId, selected)
                        },
                        enabled = state.selectedSchemeId != null,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFB0BEC5),
                            disabledContentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.width(1.dp))
                            Text("PROCEED", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSummaryCard(
    investmentInProgress: Double,
    withdrawalInProgress: Double,
    availableToWithdraw: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BalanceRow(label = "Investment in progress", amount = investmentInProgress)
            if (withdrawalInProgress > 0) {
                BalanceRow(label = "Withdrawal in progress", amount = withdrawalInProgress)
            }
            BalanceRow(
                label = "Available to withdraw",
                amount = availableToWithdraw,
                isHighlighted = true
            )
        }
    }
}

@Composable
fun BalanceRow(label: String, amount: Double, isHighlighted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32)) else MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "\u20B9${formatIndian(amount)}",
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) else MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RegularWithdrawalCard(amount: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Color(0xFF4CAF50), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("\u2192", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
            }
            Column {
                Text("Regular Withdrawal", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text("Takes up to 2 business days", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.7f))
                Text("\u20B9${formatIndian(amount)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun SchemeSelectionItem(scheme: WithdrawScheme, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(if (isSelected) Modifier.border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp)) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4CAF50))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(scheme.schemeName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                if (scheme.folioNo != null) {
                    Text("Folio: ${scheme.folioNo}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val available = (scheme.redeemableAmount - scheme.redemptionInProgress).coerceAtLeast(0.0)
                Text("\u20B9${formatIndian(available)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                Text("Available", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
            }
        }
    }
}
