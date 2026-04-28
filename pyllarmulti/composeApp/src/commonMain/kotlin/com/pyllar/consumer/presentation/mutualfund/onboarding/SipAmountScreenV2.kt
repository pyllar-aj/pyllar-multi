package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsViewModel
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.pow

// Goal type identification
enum class GoalType {
    GOLD,
    SILVER,
    SAVINGS,
    FESTIVAL_SPENDS,
    ALL_IN_ONE,
    GLOBAL_EXPOSURE,
    OTHER
}

fun identifyGoalType(goalId: String): GoalType {
    if (goalId.isBlank()) return GoalType.OTHER

    val lowerGoalId = goalId.lowercase()
    return when {
        lowerGoalId == "gold" || lowerGoalId.contains("gold") -> GoalType.GOLD
        lowerGoalId == "silver" || lowerGoalId.contains("silver") -> GoalType.SILVER
        lowerGoalId == "savings" || lowerGoalId == "saving" || lowerGoalId.contains("saving") -> GoalType.SAVINGS
        lowerGoalId == "festival_spends" || lowerGoalId.contains("festival") -> GoalType.FESTIVAL_SPENDS
        lowerGoalId == "all_in_one" || lowerGoalId.contains("all_in_one") || lowerGoalId.contains("all-in-one") -> GoalType.ALL_IN_ONE
        lowerGoalId == "global_exposure" || lowerGoalId.contains("global_exposure") || lowerGoalId.contains("global-exposure") -> GoalType.GLOBAL_EXPOSURE
        else -> GoalType.OTHER
    }
}

fun getGoalDisplayName(goalType: GoalType): String {
    return when (goalType) {
        GoalType.GOLD -> "Gold"
        GoalType.SILVER -> "Silver"
        GoalType.SAVINGS -> "Savings"
        GoalType.FESTIVAL_SPENDS -> "Festivals"
        GoalType.ALL_IN_ONE -> "All-in-One"
        GoalType.GLOBAL_EXPOSURE -> "Global Exposure"
        GoalType.OTHER -> "Goal"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipAmountScreenV2(
    userId: String,
    kycAttemptId: String = "",
    investorId: String = "",
    goalId: String = "",
    onSipCreated: (amount: Double, mandateUrl: String?, mandateId: Long?, mandateRef: Long?) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: SipAmountScreenV2ViewModel = koinInject(),
    fundDetailsViewModel: FundDetailsViewModel = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val limitsState by viewModel.limitsState.collectAsState()
    val fundDetailsState by fundDetailsViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    val minAmount = limitsState.minAmount.toFloat()
    val maxAmount = limitsState.maxAmount.toFloat()
    val defaultAmount = limitsState.defaultAmount?.toFloat() ?: minAmount
    
    val targetAmount = remember(minAmount, defaultAmount) {
        if (defaultAmount != minAmount) defaultAmount else minAmount
    }

    var amount by remember { mutableStateOf(targetAmount) }
    var isLoading by remember { mutableStateOf(false) }
    var showSavingsGrowthBottomSheet by remember { mutableStateOf(false) }
    var savingsGrowthSelectedYears by remember { mutableStateOf(7) }
    var showDetailsBottomSheet by remember { mutableStateOf(false) }

    val goalType = remember(goalId) { identifyGoalType(goalId) }

    LaunchedEffect(userId, goalId) {
        platformLog("SipAmountScreenV2: Loading limits and fund details for user $userId, goal $goalId")
        // In a real app, we'd fetch the userPurposeId first or use goalId to get it
        viewModel.fetchInvestmentLimits(goalId.ifBlank { "default_purpose" })
        fundDetailsViewModel.loadFundDetailsByGoal(userId, goalId)
    }
    
    LaunchedEffect(limitsState) {
        if (!limitsState.isLoading) {
            amount = targetAmount
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set SIP Amount", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        platformActions.shareText("Build your wealth with Pyllar! https://pyllar.in", "Share Pyllar")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onNavigateToHelp) {
                        Text("Help", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (limitsState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Projection Card
                    ProjectionCard(
                        amount = amount.toDouble(),
                        goalType = goalType,
                        onShowDetails = { years ->
                            savingsGrowthSelectedYears = years
                            showSavingsGrowthBottomSheet = true
                        }
                    )

                    // Amount Selection
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${getGoalDisplayName(goalType)} SIP Amount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "₹${minAmount.toInt()} - ₹${maxAmount.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Chip Amounts Calculation
                        val chipAmounts = remember(minAmount, defaultAmount) {
                            val min = minAmount.toInt()
                            val default = defaultAmount.toInt()
                            if (min != default) {
                                listOf(min, default, default + 50)
                            } else {
                                listOf(min, min + 50, min + 100)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chipAmounts.forEach { valOpt ->
                                AmountChip(
                                    amount = valOpt,
                                    isSelected = amount.toInt() == valOpt,
                                    isPopular = valOpt == defaultAmount.toInt() && minAmount.toInt() != defaultAmount.toInt(),
                                    onClick = { amount = valOpt.toFloat() }
                                )
                            }
                            // Custom Amount Chip placeholder logic
                            val isCustom = amount.toInt() !in chipAmounts
                            AmountChip(
                                label = if (isCustom) "₹${amount.toInt()}" else "Custom",
                                isSelected = isCustom,
                                isPopular = false,
                                onClick = { /* Show custom dialog or just leave slider */ }
                            )
                        }

                        Slider(
                            value = amount,
                            onValueChange = { amount = it },
                            valueRange = minAmount..maxAmount,
                            steps = if (maxAmount - minAmount > 0) ((maxAmount - minAmount) / 10).toInt() else 0
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { showDetailsBottomSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    
                    Text(
                        "You can change or stop your SIP anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    if (showSavingsGrowthBottomSheet) {
        SavingsGrowthBottomSheet(
            dailyAmount = amount.toDouble(),
            years = savingsGrowthSelectedYears,
            goalType = goalType,
            onDismiss = { showSavingsGrowthBottomSheet = false }
        )
    }

    if (showDetailsBottomSheet) {
        FundDetailsBottomSheet(
            amount = amount.toDouble(),
            goalType = goalType,
            fundDetailsState = fundDetailsState,
            onConfirm = {
                isLoading = true
                showDetailsBottomSheet = false
                coroutineScope.launch {
                    val result = viewModel.createSip(userId, kycAttemptId, investorId, amount.toDouble())
                    when (result) {
                        is SipCreationResult.Success -> {
                            onSipCreated(amount.toDouble(), result.mandateWrapper?.uri, result.mandateWrapper?.mandateId, null)
                        }
                        is SipCreationResult.Failure -> {
                            platformLog("SIP Creation failed: ${result.error}")
                        }
                        else -> {}
                    }
                    isLoading = false
                }
            },
            onDismiss = { showDetailsBottomSheet = false }
        )
    }
}

@Composable
fun ProjectionCard(
    amount: Double,
    goalType: GoalType,
    onShowDetails: (Int) -> Unit
) {
    val projected7Year = calculateGoldReturns(amount, 7, goalType)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF06D688), Color(0xFF02A366), Color(0xFF105E26))
                    )
                )
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "₹${amount.toInt()} / day could become",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRupeesShort(projected7Year),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onShowDetails(7) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(1, 3, 5, 7).forEach { year ->
                        GoldGrowthBar(
                            year = year,
                            amount = amount,
                            goalType = goalType,
                            onClick = { onShowDetails(year) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoldGrowthBar(
    year: Int,
    amount: Double,
    goalType: GoalType,
    onClick: () -> Unit
) {
    val projectedAmount = calculateGoldReturns(amount, year, goalType)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = formatRupeesShort(projectedAmount),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        
        if (year == 7) {
            when (goalType) {
                GoalType.GOLD -> CircularCoin("24K", Color(0xFFFFD700))
                GoalType.SILVER -> CircularCoin("999", Color(0xFFC0C0C0))
                else -> CircularCoin("₹", Color(0xFFB8A080))
            }
        }
        
        // Simplified stacks for KMP
        Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
            repeat(year) {
                Coin(goalType = goalType)
            }
        }
        
        Spacer(Modifier.height(4.dp))
        Text(text = "${year}Y", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
fun CircularCoin(text: String, color: Color) {
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}

@Composable
fun Coin(goalType: GoalType) {
    val color = when(goalType) {
        GoalType.GOLD -> Color(0xFFFFC107)
        GoalType.SILVER -> Color(0xFFC0C0C0)
        else -> Color(0xFFE0C0A0)
    }
    Box(
        modifier = Modifier.size(width = 30.dp, height = 12.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
fun AmountChip(
    amount: Int? = null,
    label: String? = null,
    isSelected: Boolean,
    isPopular: Boolean,
    onClick: () -> Unit
) {
    val text = label ?: "₹$amount"
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Box(modifier = Modifier.width(72.dp).height(70.dp).clickable(onClick = onClick)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        if (isPopular) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .zIndex(2f)
            ) {
                Text("POPULAR", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGrowthBottomSheet(
    dailyAmount: Double,
    years: Int,
    goalType: GoalType,
    onDismiss: () -> Unit
) {
    val totalInvested = dailyAmount * 365 * years
    val totalReturns = calculateGoldReturns(dailyAmount, years, goalType)
    val profit = totalReturns - totalInvested

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("How savings grow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            GrowthRow("Savings in $years years", totalInvested)
            GrowthRow("Estimated Earnings", profit)
            GrowthRow("Total Value", totalReturns, isTotal = true)
            
            Text(
                "Calculations based on historical performance. Future returns are not guaranteed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Got it")
            }
        }
    }
}

@Composable
fun GrowthRow(label: String, value: Double, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "₹${value.toInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isTotal) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundDetailsBottomSheet(
    amount: Double,
    goalType: GoalType,
    fundDetailsState: com.pyllar.consumer.presentation.mutualfund.details.FundDetailsState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${getGoalDisplayName(goalType)} Daily SIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            fundDetailsState.fundDetails?.let { details ->
                Text("Powered by ${details.fundName}", style = MaterialTheme.typography.bodyMedium)
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily Amount", style = MaterialTheme.typography.labelSmall)
                    Text("₹${amount.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            fundDetailsState.bankAccountNumber?.let { acc ->
                ListItem(
                    headlineContent = { Text("Bank Account") },
                    supportingContent = { Text("A/C: $acc") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
            
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Invest ₹${amount.toInt()}/day")
            }
        }
    }
}

fun calculateGoldReturns(dailyAmount: Double, years: Int, goalType: GoalType): Double {
    val days = years * 365
    val annualRate = when (goalType) {
        GoalType.GOLD -> 0.215
        GoalType.SILVER -> 0.295
        GoalType.SAVINGS -> 0.075
        GoalType.GLOBAL_EXPOSURE -> 0.23
        else -> 0.10
    }
    val dailyRate = (1.0 + annualRate).pow(1.0 / 365.0) - 1.0
    return if (dailyRate > 0) {
        dailyAmount * ((((1.0 + dailyRate).pow(days.toDouble()) - 1.0) / dailyRate) * (1.0 + dailyRate))
    } else {
        dailyAmount * days
    }
}

fun formatRupeesShort(amount: Double): String {
    return when {
        amount >= 10_000_000 -> "₹${(amount / 10_000_000).toInt()}Cr"
        amount >= 100_000 -> "₹${(amount / 100_000).toInt()}L"
        else -> "₹${amount.toInt()}"
    }
}
