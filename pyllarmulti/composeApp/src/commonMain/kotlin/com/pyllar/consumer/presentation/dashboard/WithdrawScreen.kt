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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                modifier = Modifier.padding(top = 16.dp),
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
                    item { Spacer(modifier = Modifier.height(2.dp)) }

                    // Withdrawal In Progress Card (at top when redemption/withdrawal in progress > 0)
                    if (state.withdrawalInProgress > 0) {
                        item {
                            val sourceName = state.schemes.firstOrNull { it.redemptionInProgress > 0 }?.schemeName
                                ?: selectedGoal?.schemeName
                                ?: selectedGoal?.name
                            WithdrawalInProgressCard(
                                amount = state.withdrawalInProgress,
                                sourceName = sourceName
                            )
                        }
                    }

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
                        enabled = state.selectedSchemeId != null && run {
                            val scheme = state.schemes.find { it.id == state.selectedSchemeId }
                            val availableAmount = if (state.selectedWithdrawMode == WithdrawMode.INSTANT) {
                                scheme?.instantRedemptionValue ?: 0.0
                            } else {
                                scheme?.redeemableAmount ?: 0.0
                            }
                            availableAmount > 0.0
                        },
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
                isHighlighted = true,
                showDecimals = true
            )
        }
    }
}

@Composable
fun BalanceRow(label: String, amount: Double, isHighlighted: Boolean = false, showDecimals: Boolean = false) {
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
            text = if (showDecimals) "\u20B9${formatIndianWithDecimals(amount)}" else "\u20B9${formatIndian(amount)}",
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
                text = "Up to \u20B9${formatIndianWithDecimals(amount)}", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                color = MaterialTheme.colorScheme.onSurface
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
                    scheme.instantRedemptionValue ?: 0.0
                } else {
                    scheme.redeemableAmount
                }
                Text("\u20B9${formatIndianWithDecimals(available)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = V2SuccessGreen)
                Text("Available", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
            }
        }
    }
}

@Composable
fun WithdrawalInProgressCard(
    amount: Double,
    sourceName: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = V2Obsidian),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row with title and clock icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.withdrawal_in_progress_title_uppercase),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = TextUnit(1.2f, TextUnitType.Sp)
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Amount and source (scheme name or goal name)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "\u20B9${formatIndianWithDecimals(amount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                if (!sourceName.isNullOrBlank()) {
                    Text(
                        text = "from $sourceName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Progress Bar & Step Labels Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 3 segment progress lines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Line 1: Requested (Light Green)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Color(0xFF81C784), shape = RoundedCornerShape(2.dp))
                    )
                    // Line 2: Processing (Light Green)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Color(0xFF81C784), shape = RoundedCornerShape(2.dp))
                    )
                    // Line 3: Credit within T+2 days (Inactive line)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(2.dp))
                    )
                }

                // Labels under progress lines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Requested",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Processing",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(Res.string.credit_within_t_plus_2_days),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            // Info Footer Text
            Text(
                text = "This amount is already on its way to your bank. " + stringResource(Res.string.withdrawal_in_progress_card_caption),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = TextUnit(18f, TextUnitType.Sp)
            )
        }
    }
}

