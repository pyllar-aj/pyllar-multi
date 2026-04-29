package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Login
import org.jetbrains.compose.resources.painterResource
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.goldbar_icon
import pyllar.composeapp.generated.resources.silver_icon
import pyllar.composeapp.generated.resources.invesco
import pyllar.composeapp.generated.resources.aditya
import pyllar.composeapp.generated.resources.axis_lo
import pyllar.composeapp.generated.resources.nippon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    onNavigateToFundDetails: (userId: String, goalId: String, amount: Double, kycAttemptId: String, investorId: String) -> Unit = { _, _, _, _, _ -> },
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
    var isCustomMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSavingsGrowthBottomSheet by remember { mutableStateOf(false) }
    var savingsGrowthSelectedYears by remember { mutableStateOf(7) }
    var showDetailsBottomSheet by remember { mutableStateOf(false) }

    val goalType = remember(goalId) { identifyGoalType(goalId) }

    LaunchedEffect(userId, goalId) {
        platformLog("SipAmountScreenV2: Loading limits and fund details for user $userId, goal $goalId")
        viewModel.fetchInvestmentLimits(goalId.ifBlank { "default_purpose" })
        fundDetailsViewModel.loadFundDetailsByGoal(userId, goalId)
    }

    // Helper to get investment status text
    val getInvestmentStatus = {
        if (fundDetailsState.isLoading) "Fetching..."
        else if (fundDetailsState.error != null) "Not Available"
        else "Active"
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
                    IconButton(onClick = { platformActions.shareText("Start your investment journey with Pyllar! https://pyllar.in") }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = {
                        try {
                            onNavigateToHelp()
                        } catch (e: Exception) {
                            com.pyllar.consumer.util.platformLog("SipAmount: Help click failed: ${e.message}")
                        }
                    }) {
                        Text("Help", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Daily Investment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        if (isCustomMode) {
                            var customValue by remember(amount) { mutableStateOf(amount.toInt().toString()) }
                            OutlinedTextField(
                                value = customValue,
                                onValueChange = { 
                                    customValue = it.filter { c -> c.isDigit() }
                                    if (customValue.isNotEmpty()) {
                                        amount = customValue.toFloat()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { isCustomMode = false }
                                ),
                                modifier = Modifier.width(150.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(
                                "₹${amount.toInt()}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { isCustomMode = true }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${getGoalDisplayName(goalType)} Limits",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "₹${minAmount.toInt()} - ₹${maxAmount.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                                    isSelected = !isCustomMode && amount.toInt() == valOpt,
                                    isPopular = valOpt == defaultAmount.toInt() && minAmount.toInt() != defaultAmount.toInt(),
                                    onClick = { 
                                        amount = valOpt.toFloat()
                                        isCustomMode = false
                                    }
                                )
                            }
                            AmountChip(
                                label = "Custom",
                                isSelected = isCustomMode,
                                isPopular = false,
                                onClick = { isCustomMode = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SIP starts at section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SIP starts at", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(getInvestmentStatus(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Investing in section
                        InvestingInCard(
                            fundDetailsState = fundDetailsState,
                            onClick = {
                                onNavigateToFundDetails(
                                    userId,
                                    goalId,
                                    amount.toDouble(),
                                    kycAttemptId,
                                    investorId
                                )
                            }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val title = when (goalType) {
                GoalType.GOLD -> "Gold Daily SIP"
                GoalType.SILVER -> "Silver Daily SIP"
                else -> "Daily SIP"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    fundDetailsState.fundDetails?.fundName?.let { fundName ->
                        Text(
                            text = "Powered by $fundName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Fund Logo
                fundDetailsState.fundDetails?.fundName?.let { fundName ->
                    val logo = when {
                        fundName.contains("Invesco", true) -> Res.drawable.invesco
                        fundName.contains("Aditya", true) -> Res.drawable.aditya
                        fundName.contains("Axis", true) -> Res.drawable.axis_lo
                        fundName.contains("Nippon", true) -> Res.drawable.nippon
                        else -> null
                    }
                    if (logo != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(logo),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            if (fundDetailsState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Investment Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daily Amount", style = MaterialTheme.typography.labelSmall)
                        Text("₹${amount.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Bank Details
                fundDetailsState.bankAccountNumber?.let { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF4CAF50))
                            Column {
                                Text("Bank Account", style = MaterialTheme.typography.labelSmall)
                                Text("A/C: $acc", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Invest ₹${amount.toInt()}/day", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
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

@Composable
fun InvestingInCard(
    fundDetailsState: com.pyllar.consumer.presentation.mutualfund.details.FundDetailsState,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Investing in", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                fundDetailsState.fundDetails?.fundName?.let { fundName ->
                    val logo = when {
                        fundName.contains("Invesco", true) -> Res.drawable.invesco
                        fundName.contains("Aditya", true) -> Res.drawable.aditya
                        fundName.contains("Axis", true) -> Res.drawable.axis_lo
                        fundName.contains("Nippon", true) -> Res.drawable.nippon
                        else -> null
                    }
                    if (logo != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(logo),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            fundDetailsState.fundDetails?.let { details ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = details.fundName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(details.category ?: "Mutual Fund", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Risk Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(details.riskLevel ?: "Moderate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            } ?: run {
                if (fundDetailsState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    Text("Fund details not available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
