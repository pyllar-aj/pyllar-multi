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
import pyllar.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.pyllar.consumer.presentation.ui.theme.*
import com.pyllar.consumer.util.toUserFriendlyErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(
    userId: String,
    selectedGoal: InvestmentGoal? = null,
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
                viewModel.loadWithdrawData(userId, selectedGoal)
            }
            
            // Mark initial load as complete
            isInitialLoadComplete = true
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("Withdraw")
    }

    val state by viewModel.withdrawState.collectAsState()
    
    // Error Dialog
    state.errorMessage?.let { errorMsg ->
        val friendlyMsg = errorMsg.toUserFriendlyErrorMessage()
        val isNetworkError = friendlyMsg.contains("connect", ignoreCase = true) ||
                friendlyMsg.contains("internet", ignoreCase = true) ||
                friendlyMsg.contains("network", ignoreCase = true) ||
                friendlyMsg.contains("timeout", ignoreCase = true) ||
                friendlyMsg.contains("offline", ignoreCase = true)
        
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text(if (isNetworkError) "Network Error" else "Error", fontWeight = FontWeight.Bold) },
            text = { Text(friendlyMsg) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearErrorMessage()
                    if (userId.isNotBlank()) viewModel.loadWithdrawData(userId)
                }) { Text(if (isNetworkError) "Retry" else "OK") }
            },
            dismissButton = if (isNetworkError) {
                {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) { Text("Cancel") }
                }
            } else null
        )
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    // Show loading screen until initial load is complete AND data is loaded
    val isLoading = !isInitialLoadComplete || state.isLoading

    Scaffold(
        containerColor = V2Cream,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 32.dp),
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
                    item { Spacer(modifier = Modifier.height(32.dp)) }

                    // Selected Goal Info Card
                    if (selectedGoal != null) {
                        item {
                            SelectedGoalCard(
                                goal = selectedGoal,
                                isLoading = state.isLoading
                            )
                        }
                    }
                    // Balance Summary Card
                    item {
                        BalanceSummaryCard(
                            investmentInProgress = state.investmentInProgress,
                            withdrawalInProgress = state.withdrawalInProgress,
                            availableToWithdraw = state.availableToWithdraw
                        )
                    }

                    // Select withdrawal mode header
                    item {
                        Text(
                            text = "Select withdrawal mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Instant withdrawal option
                    if (state.isInstantAvailable) {
                        item {
                            InstantWithdrawalCard(
                                amount = state.instantRedemptionValue ?: 0.0,
                                isSelected = state.selectedWithdrawMode == WithdrawMode.INSTANT,
                                onSelect = { viewModel.selectWithdrawMode(WithdrawMode.INSTANT) }
                            )
                        }
                    }

                    // Regular withdrawal option
                    item {
                        RegularWithdrawalCard(
                            amount = state.availableToWithdraw,
                            isSelected = state.selectedWithdrawMode == WithdrawMode.REGULAR,
                            onSelect = { viewModel.selectWithdrawMode(WithdrawMode.REGULAR) }
                        )
                    }

                    // Schemes list (only show if multiple schemes)
                    if (state.schemes.size > 1) {
                        items(state.schemes) { scheme ->
                            SchemeSelectionItem(
                                scheme = scheme,
                                isSelected = state.selectedSchemeId == scheme.id,
                                selectedWithdrawMode = state.selectedWithdrawMode,
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
                            selected?.let { 
                                WithdrawSchemeManager.set(it) 
                                val modeString = if (state.selectedWithdrawMode == WithdrawMode.INSTANT) "INSTANT" else null
                                WithdrawSchemeManager.setMode(modeString)
                                onProceed(modeString, it)
                            }
                        },
                        enabled = state.selectedSchemeId != null,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = V2Obsidian,
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
fun SelectedGoalCard(
    goal: InvestmentGoal,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = V2SubtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = V2SuccessGreen
                    )
                    Text(
                        text = goal.schemeName ?: "Scheme",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (goal.folioNo != null) {
                Text(
                    text = "Folio: ${goal.folioNo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = V2SuccessGreen) else MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) V2SuccessGreen else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "\u20B9${formatIndian(amount)}",
            style = if (isHighlighted) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = V2SuccessGreen) else MaterialTheme.typography.bodyMedium,
            color = if (isHighlighted) V2SuccessGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InstantWithdrawalCard(
    amount: Double,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSelect() }
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, V2GoldDeep, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, V2SubtleBorder, RoundedCornerShape(16.dp))
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                    color = V2SuccessGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt, // Using Bolt as fallback for FlashOn
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = stringResource(Res.string.instant_withdrawal),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.instant_withdrawal_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = V2Obsidian)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = V2SubtleBorder)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Up to \u20B9${formatIndian(amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = V2SubtleBorder,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.eighty_percent_of_balance),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = V2SuccessGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(Res.string.instant_limit_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun RegularWithdrawalCard(amount: Double, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onSelect() }
        .then(
            if (isSelected) {
                Modifier.border(1.5.dp, V2GoldDeep, RoundedCornerShape(16.dp))
            } else {
                Modifier.border(1.dp, V2SubtleBorder, RoundedCornerShape(16.dp))
            }
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = Color(0xFFF5F5F5), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(stringResource(Res.string.regular_withdrawal), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(stringResource(Res.string.regular_withdrawal_subtitle), style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.7f))
                    }
                }
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = V2Obsidian)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = V2SubtleBorder)
            
            Text(
                text = "Up to \u20B9${formatIndian(amount)}", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                color = V2SuccessGreen
            )
        }
    }
}

@Composable
fun SchemeSelectionItem(scheme: WithdrawScheme, isSelected: Boolean, selectedWithdrawMode: WithdrawMode = WithdrawMode.REGULAR, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(if (isSelected) Modifier.border(2.dp, V2GoldDeep, RoundedCornerShape(12.dp)) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = V2Obsidian)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(scheme.schemeName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                if (scheme.folioNo != null) {
                    Text("Folio: ${scheme.folioNo}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val available = if (selectedWithdrawMode == WithdrawMode.INSTANT) {
                    ((scheme.instantRedemptionValue ?: 0.0) - scheme.redemptionInProgress).coerceAtLeast(0.0)
                } else {
                    (scheme.redeemableAmount - scheme.redemptionInProgress).coerceAtLeast(0.0)
                }
                Text("\u20B9${formatIndian(available)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = V2SuccessGreen)
                Text("Available", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
            }
        }
    }
}
