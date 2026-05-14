package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.TextRange
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.presentation.dashboard.KycPendingBottomSheet
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsState
import com.pyllar.consumer.presentation.mutualfund.details.SipCreationResult
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.presentation.ui.components.TrustStrip
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.BackHandler
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.max
import kotlin.math.pow
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.presentation.dashboard.getFundLogo
import androidx.compose.foundation.Image
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LumpsumAmountScreenV2(
    userId: String,
    kycAttemptId: String,
    investorId: String,
    goalId: String = "",
    isExistingInvestment: Boolean = false,
    onLumpsumCreated: (Double, String?, MandateWrapper?) -> Unit = { _, _, _ -> },
    onForceLogout: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToFundDetails: (userId: String, goalId: String, amount: Double, kycAttemptId: String, investorId: String) -> Unit = { _, _, _, _, _ -> },
    sessionStore: SessionStore = koinInject(),
    fundDetailsViewModel: FundDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject()
) {
    val minAmount = 1000f
    val maxAmount = 50000f
    val targetAmount = 5000f

    var amount by remember { mutableStateOf(targetAmount) }
    var amountText by remember {
        val initial = targetAmount.toInt().toString()
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var isLoading by remember { mutableStateOf(false) }
    var isInitTxnLoading by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<String?>(null) }
    var showUnexpectedErrorDialog by remember { mutableStateOf(false) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    var showTrustStripInfoDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val timeoutState = rememberTimeoutState("LumpsumAmountV2", "continue")
    val focusManager = LocalFocusManager.current
    val platformActions = koinInject<PlatformActions>()

    var effectiveUserId by remember { mutableStateOf(userId) }
    var effectiveGoalId by remember { mutableStateOf(goalId) }

    LaunchedEffect(Unit) {
        try {
            if (effectiveUserId.isBlank()) {
                effectiveUserId = sessionStore.getValue(KeyValueConstants.USER_ID) ?: ""
            }
            if (effectiveGoalId.isBlank()) {
                effectiveGoalId = sessionStore.getValue(KeyValueConstants.SELECTED_GOAL_ID) ?: goalId
            }
        } catch (e: Exception) {
            platformLog("LumpsumAmountScreenV2: Error fetching ids: ${e.message}")
        }
    }

    val goalType = remember(effectiveGoalId) {
        identifyGoalType(effectiveGoalId)
    }

    val fundDetailsState by fundDetailsViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

    LaunchedEffect(effectiveUserId) {
        if (effectiveUserId.isNotBlank()) {
            dashboardViewModel.loadDashboardData(effectiveUserId)
        }
    }

    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveUserId.isNotBlank() && effectiveGoalId.isNotBlank()) {
            fundDetailsViewModel.loadFundDetailsByGoal(effectiveUserId, effectiveGoalId)
        }
    }

    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveGoalId.isNotBlank()) {
            isInitTxnLoading = true
            coroutineScope.launch {
                try {
                    val result = dashboardViewModel.initGoalTxn(effectiveUserId, effectiveGoalId)
                    if (result is Resource.Success) {
                        result.data?.let { response ->
                            if (response.userPurposeId.isNotBlank()) {
                                sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    platformLog("LumpsumAmountScreenV2: Error in initGoalTxn: ${e.message}")
                } finally {
                    isInitTxnLoading = false
                }
            }
        }
    }


    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 32.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            platformActions.shareText("Check out Pyllar for goal-based investing!")
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    TextButton(onClick = onNavigateToHelp) {
                        Text(
                            text = "Help",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                val years = listOf(1, 3, 5, 7)
                var selectedYear by remember { mutableStateOf(7) }

                val projectedAmounts = years.map { year ->
                    calculateLumpsumFutureValue(amount.toDouble(), year, goalType)
                }
                val selectedTotal = projectedAmounts.getOrNull(years.indexOf(selectedYear)) ?: amount.toDouble()

                LumpsumGrowthGraphCard(
                    investedAmount = amount.toDouble(),
                    totalAmount = selectedTotal,
                    years = years,
                    projectedAmounts = projectedAmounts,
                    goalType = goalType,
                    selectedYear = selectedYear,
                    onYearSelected = { selectedYear = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val chipAmounts = listOf(1000, 5000, 10000).filter { it <= maxAmount.toInt() }
                var isCustomMode by remember { mutableStateOf(amount.toInt() !in chipAmounts) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getGoalDisplayName(goalType),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "₹${minAmount.toInt()} - ₹${maxAmount.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        val filteredText = newValue.text.filter { it.isDigit() }
                        if (filteredText.isNotEmpty()) {
                            val newAmount = filteredText.toIntOrNull()
                            if (newAmount != null && newAmount <= maxAmount.toInt()) {
                                amountText = TextFieldValue(filteredText, newValue.selection)
                                amount = newAmount.toFloat()
                            } else if (newAmount != null && newAmount > maxAmount.toInt()) {
                                val maxText = maxAmount.toInt().toString()
                                amountText = TextFieldValue(maxText, TextRange(maxText.length))
                                amount = maxAmount
                            }
                        } else {
                            amountText = TextFieldValue("", TextRange(0))
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )

                Text(
                    text = "One-time investment amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    chipAmounts.forEach { preset ->
                        AmountChip(
                            amount = preset,
                            isSelected = !isCustomMode && amount.toInt() == preset,
                            isPopular = preset == 5000,
                            onClick = {
                                isCustomMode = false
                                amount = preset.toFloat()
                                val newText = preset.toString()
                                amountText = TextFieldValue(newText, TextRange(newText.length))
                                focusManager.clearFocus()
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCustomMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .clickable { isCustomMode = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Custom",
                            color = if (isCustomMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Investing in",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (fundDetailsState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            fundDetailsState.fundDetails?.fundName?.let { fundName ->
                                val logo = getFundLogo(fundName)
                                Image(
                                    painter = painterResource(logo),
                                    contentDescription = "Fund Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable(enabled = !isInitTxnLoading) {
                                            onNavigateToFundDetails(
                                                effectiveUserId,
                                                effectiveGoalId,
                                                amount.toDouble(),
                                                kycAttemptId,
                                                investorId
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                val tealPrimary = Color(0xFF0D7B6B)
                TimeoutButton(
                    modifier = Modifier.fillMaxWidth(),
                    timeoutState = timeoutState,
                    enabled = !isLoading && !isInitTxnLoading && !dashboardState.isLoading && fundDetailsState.fundDetails != null,
                    colors = ButtonDefaults.buttonColors(containerColor = tealPrimary, contentColor = Color.White),
                    onClick = {
                        if (amount < minAmount || amount > maxAmount) {
                            submitResult = "Amount must be between ₹${minAmount.toInt()} and ₹${maxAmount.toInt()}"
                            return@TimeoutButton
                        }
                        
                        val isKycPending = dashboardState.kycStatus.equals("PENDING", ignoreCase = true) ||
                                dashboardState.kycStatus.equals("IN_PROGRESS", ignoreCase = true)
                        
                        if (isKycPending) {
                            showKycPendingBottomSheet = true
                            return@TimeoutButton
                        }

                        coroutineScope.launch {
                            isLoading = true
                            val result = fundDetailsViewModel.createLumpsumPurchase(effectiveUserId, amount.toDouble())
                            isLoading = false

                            when (result) {
                                is SipCreationResult.LumpsumSuccess -> {
                                    submitResult = null
                                    val mappedData = result.lumpsumData?.let { data ->
                                        MandateWrapper(
                                            finMandateId = data.old_id ?: 0L,
                                            mandateId = data.payment_id ?: 0L,
                                            uri = data.token_url
                                        )
                                    }
                                    onLumpsumCreated(amount.toDouble(), result.nextScreen ?: effectiveGoalId, mappedData)
                                }
                                is SipCreationResult.Failure -> {
                                    submitResult = result.message
                                    showUnexpectedErrorDialog = true
                                }
                                else -> {}
                            }
                        }
                    }
                ) {
                    val isFetching = isInitTxnLoading || fundDetailsState.isLoading || dashboardState.isLoading
                    if (isLoading || isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLoading) "Submitting..." else "Loading...")
                    } else {
                        Text("Invest One-time", fontWeight = FontWeight.Bold)
                    }
                }

                submitResult?.let { message ->
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TrustStrip(onInfoClick = { showTrustStripInfoDialog = true })
            }
        }
    }

    if (showUnexpectedErrorDialog) {
        AlertDialog(
            onDismissRequest = { showUnexpectedErrorDialog = false },
            confirmButton = {
                TextButton(onClick = { showUnexpectedErrorDialog = false }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text(submitResult ?: "An unexpected error occurred.") }
        )
    }

    if (showTrustStripInfoDialog) {
        AlertDialog(
            onDismissRequest = { showTrustStripInfoDialog = false },
            title = { Text("About AMCs") },
            text = { Text("Investments are made in mutual funds managed by respective Asset Management Companies.") },
            confirmButton = {
                TextButton(onClick = { showTrustStripInfoDialog = false }) { Text("OK") }
            }
        )
    }

    if (showKycPendingBottomSheet) {
        KycPendingBottomSheet(
            onDismiss = { showKycPendingBottomSheet = false },
            onRetryKyc = { showKycPendingBottomSheet = false },
            kycStatus = dashboardState.kycStatus
        )
    }

    if (isInitTxnLoading || fundDetailsState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun LumpsumGrowthGraphCard(
    modifier: Modifier = Modifier,
    investedAmount: Double,
    totalAmount: Double,
    years: List<Int>,
    projectedAmounts: List<Double>,
    goalType: GoalType,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    backgroundColor: Color = Color(0xFF0D7B6B)
) {
    val interestAmount = (totalAmount - investedAmount).coerceAtLeast(0.0)
    val growthMultiple = if (investedAmount > 0) totalAmount * 100 / investedAmount else 0.0

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = "Projected Wealth", color = Color.White, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Estimated total value", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                Text(text = formatRupeesShort(totalAmount), color = Color.White, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1.2f)) {
                    LumpsumStackedBarChart(years, projectedAmounts, investedAmount, goalType, selectedYear, onYearSelected)
                }
                Column(modifier = Modifier.weight(0.7f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                    PillStat(Modifier.fillMaxWidth(), "Invested", formatRupeesShort(investedAmount), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.9f))
                    PillStat(Modifier.fillMaxWidth(), "Returns", "+${formatRupeesShort(interestAmount)}", Color.White.copy(alpha = 0.16f), Color.White)
                    PillStat(Modifier.fillMaxWidth(), "Growth", "${growthMultiple.toInt()}%", Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
private fun PillStat(modifier: Modifier = Modifier, label: String, value: String, containerColor: Color, textColor: Color) {
    Column(modifier = modifier.clip(RoundedCornerShape(999.dp)).background(containerColor).padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.Start) {
        Text(text = label, color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Text(text = value, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun LumpsumStackedBarChart(years: List<Int>, projectedAmounts: List<Double>, investedAmount: Double, goalType: GoalType, selectedYear: Int, onYearSelected: (Int) -> Unit) {
    val maxAmount = max(projectedAmounts.maxOrNull() ?: 0.0, investedAmount)
    Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        years.zip(projectedAmounts).forEach { (year, projected) ->
            val totalHeightRatio = if (maxAmount > 0) projected / maxAmount else 0.0
            val investedShareInBar = if (projected > 0.0) (investedAmount / projected).coerceIn(0.0, 1.0) else 0.0
            val investedHeightRatio = totalHeightRatio * investedShareInBar
            val interestHeightRatio = (totalHeightRatio - investedHeightRatio).coerceAtLeast(0.0)
            val investedColor = Color(0xFF80CBC4)
            val interestColor = when (goalType) {
                GoalType.GOLD -> Color(0xFFFFC107)
                GoalType.SILVER -> Color(0xFFB0BEC5)
                GoalType.SAVINGS_PLUS -> Color(0xFFC1E8C2)
                else -> Color(0xFF78CDEB)
            }
            val isSelected = year == selectedYear
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.clickable { onYearSelected(year) }) {
                Text(text = formatRupeesShort(projected), color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Clip)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.width(if (isSelected) 26.dp else 22.dp).height(if (isSelected) 100.dp else 90.dp)) {
                    val totalRatio = (investedHeightRatio + interestHeightRatio).coerceIn(0.0, 1.0)
                    Column(modifier = Modifier.fillMaxSize().align(Alignment.BottomCenter), verticalArrangement = Arrangement.Bottom) {
                        if (totalRatio < 1.0) Spacer(modifier = Modifier.weight(1f - totalRatio.toFloat()))
                        if (interestHeightRatio > 0.0) Box(modifier = Modifier.weight(interestHeightRatio.toFloat()).fillMaxWidth().background(interestColor))
                        if (investedHeightRatio > 0.0) Box(modifier = Modifier.weight(investedHeightRatio.toFloat()).fillMaxWidth().background(investedColor))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "${year}Y", color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun calculateLumpsumFutureValue(oneTimeAmount: Double, years: Int, goalType: GoalType = GoalType.OTHER): Double {
    val annualRate = when {
        goalType == GoalType.GOLD -> when (years) { 1 -> 0.754; 3 -> 0.342; 5 -> 0.221; 7 -> 0.215; else -> 0.215 }
        goalType == GoalType.SILVER -> when (years) { 1 -> 1.582; 3 -> 0.435; 5 -> 0.341; 7 -> 0.295; else -> 0.295 }
        goalType == GoalType.SAVINGS_PLUS -> 0.075
        goalType == GoalType.SAVINGS -> 0.075
        goalType == GoalType.FESTIVAL_SPENDS -> 0.075
        goalType == GoalType.GLOBAL_EXPOSURE -> 0.23
        goalType == GoalType.ALL_IN_ONE -> 0.175
        else -> 0.10
    }
    return (oneTimeAmount * (1.0 + annualRate).pow(years.toDouble())).coerceAtLeast(0.0)
}
