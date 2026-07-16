package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalFocusManager
import com.pyllar.consumer.presentation.ui.theme.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsViewModel
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.GoalType
import com.pyllar.consumer.util.identifyGoalType
import com.pyllar.consumer.util.getGoalDisplayName
import com.pyllar.consumer.util.calculateGoldReturns
import com.pyllar.consumer.util.formatRupeesShort
import com.pyllar.consumer.util.getInvestmentStatus
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.presentation.mutualfund.details.SipCreationResult
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlinx.datetime.Clock.System as kClockSystem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DayOfWeek
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SipAmountScreenV2(
    userId: String,
    kycAttemptId: String = "",
    investorId: String = "",
    goalId: String = "",
    isExistingInvestment: Boolean = false,
    onSipCreated: (amount: Double, mandateUrl: String?, mandateId: Long?, mandateRef: Long?) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToFundDetails: (userId: String, goalId: String, amount: Double, kycAttemptId: String, investorId: String) -> Unit = { _, _, _, _, _ -> },
    viewModel: SipAmountScreenV2ViewModel = koinInject(),
    fundDetailsViewModel: FundDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject(),
    sessionStore: SessionStore = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val limitsState by viewModel.limitsState.collectAsState()
    val fundDetailsState by fundDetailsViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    // Effective IDs - fetch from repository when params are empty
    var effectiveUserId by remember(userId) { mutableStateOf(userId) }
    var isFetchingIds by remember { mutableStateOf(false) }
    var effectiveKycAttemptId by remember(kycAttemptId) { mutableStateOf(kycAttemptId) }
    var effectiveInvestorId by remember(investorId) { mutableStateOf(investorId) }
    var effectiveGoalId by remember(goalId) { mutableStateOf(goalId) }
    var isInitializing by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }
    var isInitTxnLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, goalId, kycAttemptId, investorId) {
        platformLog("SipAmountScreenV2: Received params - userId: '$userId', goalId: '$goalId', kyc: '$kycAttemptId', inv: '$investorId'")
        if (userId.isNotBlank()) effectiveUserId = userId
        if (goalId.isNotBlank()) effectiveGoalId = goalId
        if (kycAttemptId.isNotBlank()) effectiveKycAttemptId = kycAttemptId
        if (investorId.isNotBlank()) effectiveInvestorId = investorId
    }

    // Fetch effective values from repository on composition
    LaunchedEffect(Unit) {
        try {
            isFetchingIds = true
            platformLog("SipAmountV2: 🔄 Initializing screen IDs. Provided: kyc='$kycAttemptId', inv='$investorId'")
            
            // Fetch userId from repository if param is empty
            if (userId.isBlank()) {
                try {
                    val storedUserId = sessionStore.getCurrentUserId()
                    if (storedUserId.isNotBlank()) {
                        effectiveUserId = storedUserId
                        platformLog("SipAmountScreenV2: Restored userId from storage: '$storedUserId'")
                    }
                } catch (e: Exception) {
                    platformLog("SipAmountScreenV2: Error fetching userId from storage: ${e.message}")
                }
            }

            // Fetch goalId from repository if param is empty
            if (goalId.isBlank()) {
                try {
                    val storedGoalId = sessionStore.getValue(KeyValueConstants.SELECTED_GOAL_ID) ?: ""
                    if (storedGoalId.isNotBlank()) {
                        effectiveGoalId = storedGoalId
                        platformLog("SipAmountScreenV2: Restored goalId from storage: '$storedGoalId'")
                    }
                } catch (e: Exception) {
                    platformLog("SipAmountScreenV2: Error fetching goalId from storage: ${e.message}")
                }
            }

            // Always try to restore from storage if provided ones are blank
            if (effectiveKycAttemptId.isBlank() || effectiveInvestorId.isBlank()) {
                val storedKycId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                val storedInvId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""
                platformLog("SipAmountV2: Restoration check: storedKyc='$storedKycId', storedInv='$storedInvId'")
                
                if (effectiveKycAttemptId.isBlank() && storedKycId.isNotBlank()) {
                    effectiveKycAttemptId = storedKycId
                }
                if (effectiveInvestorId.isBlank() && storedInvId.isNotBlank()) {
                    effectiveInvestorId = storedInvId
                }
            }
        } finally {
            isFetchingIds = false
            isInitializing = false
            isInitialized = true
        }
    }

    val minAmount = limitsState.minAmount.toFloat()
    val maxAmount = limitsState.maxAmount.toFloat()
    val defaultAmount = limitsState.defaultAmount?.toFloat() ?: minAmount
    
    val targetAmount = remember(minAmount, defaultAmount) {
        if (defaultAmount != minAmount) defaultAmount else minAmount
    }

    var amount by remember { mutableStateOf(targetAmount) }
    var amountText by remember {
        val initial = targetAmount.toInt().toString()
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var isCustomMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSavingsGrowthBottomSheet by remember { mutableStateOf(false) }
    var savingsGrowthSelectedYears by remember { mutableStateOf(7) }
    var showDetailsBottomSheet by remember { mutableStateOf(false) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    var isAmountFocused by remember { mutableStateOf(false) }
    
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val amountFocusRequester = remember { FocusRequester() }


    // Track resolved goal type - update it when initGoalTxn returns the real purpose
    var resolvedGoalType by remember(effectiveGoalId) { mutableStateOf(identifyGoalType(effectiveGoalId)) }

    // Load fund details and dashboard data when effectiveUserId and effectiveGoalId are available
    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveUserId.isNotBlank()) {
            platformLog("SipAmountScreenV2: Loading dashboard data for KYC status - userId: '$effectiveUserId'")
            dashboardViewModel.loadDashboardData(effectiveUserId)
            
            if (effectiveGoalId.isNotBlank()) {
                platformLog("SipAmountScreenV2: Triggering initial fund details load - userId: '$effectiveUserId', goalId: '$effectiveGoalId'")
                fundDetailsViewModel.loadFundDetailsByGoal(effectiveUserId, effectiveGoalId)
            }
        }
    }

    // Fetch userPurposeId and get investment limits - depends on effective values
    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveUserId.isNotBlank() && effectiveGoalId.isNotBlank()) {
            isInitTxnLoading = true
            coroutineScope.launch {
                try {
                    platformLog("SipAmountScreenV2: Fetching userPurposeId for goalId ('$effectiveGoalId') via initGoalTxn")
                    when (val result = dashboardViewModel.initGoalTxn(effectiveUserId, effectiveGoalId)) {
                        is Resource.Success -> {
                            result.data?.let { response ->
                                // Update goal type from response if available
                                if (response.investmentPurpose.isNotBlank()) {
                                    val newGoalType = identifyGoalType(response.investmentPurpose)
                                    platformLog("SipAmountScreenV2: Updating resolvedGoalType from '${response.investmentPurpose}' -> $newGoalType")
                                    resolvedGoalType = newGoalType
                                }
                                if (response.userPurposeId.isNotBlank()) {
                                    sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                                    val fetchedUserPurposeId = response.userPurposeId
                                    platformLog("SipAmountScreenV2: Fetched and stored userPurposeId: $fetchedUserPurposeId")
                                    
                                    // Now fetch investment limits
                                    viewModel.fetchInvestmentLimits(fetchedUserPurposeId)
                                }
                            }
                        }
                        is Resource.Error -> {
                            platformLog("SipAmountScreenV2: Failed to fetch userPurposeId: ${result.message}")
                            // Fallback to goalId if initGoalTxn fails
                            viewModel.fetchInvestmentLimits(effectiveGoalId)
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    platformLog("SipAmountScreenV2: Exception calling initGoalTxn: ${e.message}")
                } finally {
                    isInitTxnLoading = false
                }
            }
        } else if (effectiveGoalId.isNotBlank()) {
            viewModel.fetchInvestmentLimits(effectiveGoalId)
        }
    }

    // Helper to get investment status text
    val investmentStatusText = when {
        fundDetailsState.isLoading -> "Fetching..."
        fundDetailsState.error != null -> "Not Available"
        else -> getInvestmentStatus()
    }
    
    LaunchedEffect(limitsState) {
        if (!limitsState.isLoading) {
            amount = targetAmount
            val newText = targetAmount.toInt().toString()
            amountText = TextFieldValue(newText, TextRange(newText.length))
        }
    }

    val isFetching = isInitializing || isInitTxnLoading || limitsState.isLoading || fundDetailsState.isLoading || isFetchingIds

    com.pyllar.consumer.util.BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 25.dp),
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
        },
        bottomBar = {
            val canContinue = !isFetching && !isLoading
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDetailsBottomSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = canContinue,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isFetching || isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isLoading) "Processing..." else "Loading...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    } else {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Text(
                    "You can change or stop your SIP anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFetching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                    // Amount Selection
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { 
                                    focusManager.clearFocus()
                                    isCustomMode = false 
                                })
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Projection Card
                    ProjectionCard(
                        amount = amount.toDouble(),
                        goalType = resolvedGoalType,
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
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { newValue ->
                                    val filteredText = newValue.text.filter { it.isDigit() }
                                    if (filteredText.isNotEmpty()) {
                                        val newAmount = filteredText.toIntOrNull()
                                        if (newAmount != null && newAmount in minAmount.toInt()..maxAmount.toInt()) {
                                            amountText = TextFieldValue(filteredText, newValue.selection)
                                            amount = newAmount.toFloat()
                                        } else if (newAmount != null && newAmount > maxAmount.toInt()) {
                                            val maxText = maxAmount.toInt().toString()
                                            amountText = TextFieldValue(maxText, TextRange(maxText.length))
                                            amount = maxAmount
                                        } else if (newAmount != null && newAmount < minAmount.toInt()) {
                                            // Allow typing but don't update 'amount' until valid
                                            amountText = TextFieldValue(filteredText, newValue.selection)
                                        }
                                    } else {
                                        amountText = TextFieldValue("", TextRange(0))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier
                                    .width(150.dp)
                                    .focusRequester(amountFocusRequester)
                                    .bringIntoViewRequester(bringIntoViewRequester)
                                    .onFocusChanged { focusState: androidx.compose.ui.focus.FocusState ->
                                        val wasFocused = isAmountFocused
                                        isAmountFocused = focusState.isFocused
                                        
                                        if (focusState.isFocused && !wasFocused) {
                                            val text = amountText.text
                                            amountText = TextFieldValue(text, TextRange(text.length))
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(300)
                                                bringIntoViewRequester.bringIntoView()
                                            }
                                        }

                                        // On focus loss, validate min amount
                                        if (!focusState.isFocused && wasFocused) {
                                            val currentInput = amountText.text.toIntOrNull()
                                            if (currentInput == null || currentInput < minAmount.toInt()) {
                                                val newText = minAmount.toInt().toString()
                                                amountText = TextFieldValue(newText, TextRange(newText.length))
                                                amount = minAmount
                                            }
                                        }
                                    },
                                prefix = { Text("₹") },
                                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                        } else {
                            Text(
                                "₹${amount.toInt()}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { 
                                    isCustomMode = true 
                                }
                            )
                        }

                        LaunchedEffect(isCustomMode) {
                            if (isCustomMode) {
                                kotlinx.coroutines.delay(300)
                                amountFocusRequester.requestFocus()
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${getGoalDisplayName(resolvedGoalType)} Limits",
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
                        val chipAmounts = remember(minAmount, defaultAmount, maxAmount) {
                            val minVal = minAmount.toInt()
                            val defaultVal = defaultAmount.toInt()
                            val maxVal = maxAmount.toInt()
                            val secondVal = if (defaultVal != minVal) defaultVal else minVal + 100
                            listOf(minVal, secondVal, maxVal)
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
                                        val newText = valOpt.toString()
                                        amountText = TextFieldValue(newText, TextRange(newText.length))
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
                                Text(investmentStatusText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Investing in section
                        InvestingInCard(
                            fundDetailsState = fundDetailsState,
                            onClick = {
                                onNavigateToFundDetails(
                                    effectiveUserId,
                                    effectiveGoalId,
                                    amount.toDouble(),
                                    effectiveKycAttemptId,
                                    effectiveInvestorId
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSavingsGrowthBottomSheet) {
        SavingsGrowthBottomSheet(
            dailyAmount = amount.toDouble(),
            years = savingsGrowthSelectedYears,
            goalType = resolvedGoalType,
            onDismiss = { showSavingsGrowthBottomSheet = false }
        )
    }

    if (showDetailsBottomSheet) {
        FundDetailsBottomSheet(
            amount = amount.toDouble(),
            goalType = resolvedGoalType,
            fundDetailsState = fundDetailsState,
            onConfirm = {
                coroutineScope.launch {
                    platformLog("SipAmountV2: onConfirm clicked. Current IDs: userId='$effectiveUserId', kycId='$effectiveKycAttemptId', invId='$effectiveInvestorId'")
                    
                    // Check KYC status only after dashboard has loaded
                    val kycStatus = dashboardState.kycStatus
                    val isKycPending = !dashboardState.isLoading &&
                            (kycStatus.equals("PENDING", ignoreCase = true) ||
                             kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                             kycStatus.equals("EXPIRED", ignoreCase = true))
                    
                    if (isKycPending) {
                        platformLog("SipAmountV2: KYC Pending ('$kycStatus'). Showing KycPendingBottomSheet.")
                        showDetailsBottomSheet = false
                        showKycPendingBottomSheet = true
                        return@launch
                    }

                    // Proceed with API call
                    platformLog("SipAmountV2: Proceeding with createSip API call...")
                    isLoading = true
                    showDetailsBottomSheet = false
                    
                    try {
                        val result = viewModel.createSip(effectiveUserId, effectiveKycAttemptId, effectiveInvestorId, amount.toDouble())
                        platformLog("SipAmountV2: createSip result: $result")
                        
                        when (result) {
                            is SipCreationResult.Success -> {
                                platformLog("SipAmountV2: SIP Creation Success! Navigating...")
                                onSipCreated(amount.toDouble(), result.mandateWrapper?.uri, result.mandateWrapper?.mandateId, result.mandateWrapper?.finMandateId)
                            }
                            is SipCreationResult.Failure -> {
                                platformLog("SipAmountV2: SIP Creation Failure: ${result.message}")
                                errorMessage = result.message?.toUserFriendlyErrorMessage()
                            }
                            else -> {
                                platformLog("SipAmountV2: Unexpected result type: ${result::class.simpleName}")
                            }
                        }
                    } catch (e: Exception) {
                        platformLog("SipAmountV2: Exception during createSip: ${e.message}")
                        errorMessage = ("An unexpected error occurred: " + e.message).toUserFriendlyErrorMessage()
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = { showDetailsBottomSheet = false }
        )
    }

    if (showKycPendingBottomSheet) {
        com.pyllar.consumer.presentation.dashboard.KycPendingBottomSheet(
            onDismiss = { showKycPendingBottomSheet = false },
            onRetryKyc = {
                showKycPendingBottomSheet = false
                // Optional: Navigation back to KYC screen if needed
            },
            kycStatus = dashboardState.kycStatus
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Investment Failed") },
            text = { Text(errorMessage ?: "An unexpected error occurred. Please try again.") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
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
            color = if (isTotal) V2SuccessGreen else MaterialTheme.colorScheme.onSurface
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
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = V2SuccessGreen)
                            Column {
                                Text("Bank Account", style = MaterialTheme.typography.labelSmall)
                                val maskedAcc = if (acc.length > 4) {
                                    acc.takeLast(4).padStart(acc.length, '*')
                                } else {
                                    acc
                                }
                                Text("A/C: $maskedAcc", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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

