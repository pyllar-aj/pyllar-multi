package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.navigation.AppRoutes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailsScreen(
    userId: String = "",
    purpose: String = "",
    onNavigateBack: () -> Unit = {},
    onNavigateToWithdraw: (WithdrawInitParams) -> Unit = {},
    onNavigateToAddFunds: (userId: String, kycAttemptId: String, investorId: String, goalId: String, isExistingInvestment: Boolean) -> Unit = { _, _, _, _, _ -> },
    viewModel: SchemeDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    val cancelSipResult by viewModel.cancelSipResult.collectAsState()
    val pauseSipResult by viewModel.pauseSipResult.collectAsState()
    val resumeSipResult by viewModel.resumeSipResult.collectAsState()
    
    val cancelSipLoading by viewModel.cancelSipLoading.collectAsState()
    val pauseSipLoading by viewModel.pauseSipLoading.collectAsState()
    val resumeSipLoading by viewModel.resumeSipLoading.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showDetailsPopup by remember { mutableStateOf(false) }
    var showPauseConfirm by remember { mutableStateOf<MandateDisplayItem?>(null) }
    var showResumeConfirm by remember { mutableStateOf<MandateDisplayItem?>(null) }
    var showCancelConfirm by remember { mutableStateOf<MandateDisplayItem?>(null) }
    var isProcessingInvestMore by remember { mutableStateOf(false) }

    // SIP Action UI States (aligned with Android)
    var showCancelSipScreen by remember { mutableStateOf(false) }
    var mandateForCancelSip by remember { mutableStateOf<MandateDisplayItem?>(null) }
    var showCancelReasonScreen by remember { mutableStateOf(false) }
    var selectedCancelReason by remember { mutableStateOf<CancelSipReason?>(null) }
    var showCancelSipSuccessSheet by remember { mutableStateOf(false) }
    var showCancelSipErrorSheet by remember { mutableStateOf(false) }

    var showPauseSipQuestionSheet by remember { mutableStateOf(false) }
    var showPauseSipSuccessSheet by remember { mutableStateOf(false) }
    var showPauseSipErrorSheet by remember { mutableStateOf(false) }
    var mandateForPauseSip by remember { mutableStateOf<MandateDisplayItem?>(null) }

    var showResumeSipQuestionSheet by remember { mutableStateOf(false) }
    var showResumeSipSuccessSheet by remember { mutableStateOf(false) }
    var showResumeSipErrorSheet by remember { mutableStateOf(false) }
    var mandateForResumeSip by remember { mutableStateOf<MandateDisplayItem?>(null) }

    val schemeParams = remember(purpose) { SchemeDetailsParamsManager.get() }
    val displaySchemeName = schemeParams?.schemeName?.takeIf { it.isNotBlank() } ?: state.schemeName.orEmpty()
    val displayGoalName = schemeParams?.goalName?.takeIf { it.isNotBlank() } ?: state.goalName.orEmpty()

    val isSilverGoal = displaySchemeName.contains("Silver", ignoreCase = true) || displayGoalName.contains("Silver", ignoreCase = true) || state.category?.uppercase() == "SILVER"
    val baseAccentColor = getCorrelationColorForCategory(state.category, state.colorTheme)
    val accentColor = if (isSilverGoal && baseAccentColor == Color(0xFF818181)) Color(0xFF1A1A1A) else baseAccentColor

    // Info Popups state
    var showTotalValueInfo by remember { mutableStateOf(false) }
    var showGoldInfo by remember { mutableStateOf(false) }
    var showSilverInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetails")
    }

    LaunchedEffect(purpose) {
        if (schemeParams == null && purpose.isNotBlank()) {
            try {
                platformLog("🔍 Attempting to restore SchemeDetailsParams from storage for purpose=$purpose")
                val stored = sessionStore.getValue("scheme_details_params_$purpose")
                val restored = SchemeDetailsParamsManager.fromJson(stored)
                if (restored != null) {
                    platformLog("✅ Restored SchemeDetailsParams from storage for purpose=$purpose")
                    SchemeDetailsParamsManager.set(restored)
                    // We don't have a mutable state for schemeParams here, but SchemeDetailsParamsManager.get() 
                    // will now return the restored value on next recomposition.
                    // To trigger recomposition, we can use a local state if needed.
                }
            } catch (e: Exception) {
                platformLog("❌ Failed to restore SchemeDetailsParams: ${e.message}")
            }
        }
    }

    LaunchedEffect(userId, purpose) {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            platformLog("🔄 Initial load for userId: $userId, purpose: $purpose")
            val currentParams = schemeParams ?: SchemeDetailsParamsManager.get()
            val uipid = currentParams?.userPurposeId ?: purpose
            viewModel.loadTransactions(userId, uipid, currentParams)
            
            // Save params for future restoration
            currentParams?.let {
                sessionStore.saveValue("scheme_details_params_$purpose", SchemeDetailsParamsManager.toJson(it))
            }
        }
    }

    // Handle SIP Action Results
    LaunchedEffect(cancelSipResult) {
        when (cancelSipResult) {
            is CancelSipResult.Success -> {
                showCancelSipScreen = false
                showCancelReasonScreen = false
                showCancelSipSuccessSheet = true
                viewModel.clearCancelSipResult()
            }
            is CancelSipResult.Error -> {
                showCancelSipScreen = false
                showCancelReasonScreen = false
                showCancelSipErrorSheet = true
                viewModel.clearCancelSipResult()
            }
            else -> {}
        }
    }
    LaunchedEffect(pauseSipResult) {
        when (pauseSipResult) {
            is PauseSipResult.Success -> {
                showPauseSipQuestionSheet = false
                mandateForPauseSip = null
                showPauseSipSuccessSheet = true
                viewModel.clearPauseSipResult()
            }
            is PauseSipResult.Error -> {
                showPauseSipQuestionSheet = false
                mandateForPauseSip = null
                showPauseSipErrorSheet = true
                viewModel.clearPauseSipResult()
            }
            else -> {}
        }
    }
    LaunchedEffect(resumeSipResult) {
        when (resumeSipResult) {
            is ResumeSipResult.Success -> {
                showResumeSipQuestionSheet = false
                mandateForResumeSip = null
                showResumeSipSuccessSheet = true
                viewModel.clearResumeSipResult()
            }
            is ResumeSipResult.Error -> {
                showResumeSipQuestionSheet = false
                mandateForResumeSip = null
                showResumeSipErrorSheet = true
                viewModel.clearResumeSipResult()
            }
            else -> {}
        }
    }

    val reloadTransactions = {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            platformLog("🔄 Reloading transactions for userId: $userId, purpose: $purpose")
            val currentParams = schemeParams ?: SchemeDetailsParamsManager.get()
            val uipid = currentParams?.userPurposeId ?: purpose
            viewModel.loadTransactions(userId, uipid, currentParams)
        }
    }

    val handleAddFunds: (String) -> Unit = { goalId ->
        if (!isProcessingInvestMore) {
            isProcessingInvestMore = true
            scope.launch {
                try {
                    val result = dashboardViewModel.initGoalTxn(userId, goalId)
                    if (result is Resource.Success) {
                        result.data?.let { response ->
                            if (response.userPurposeId.isNotBlank()) {
                                sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                            }
                        }
                    }
                    
                    sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, goalId)
                    val hasInvestment = state.investedAmount > 0 || !state.folioNumber.isNullOrBlank()
                    sessionStore.saveValue("isExistingInvestment", hasInvestment.toString())

                    val kycAttemptId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                    val investorId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""

                    onNavigateToAddFunds(userId, kycAttemptId, investorId, goalId, hasInvestment)
                } catch (e: Exception) {
                    platformLog("Error in handleAddFunds: ${e.message}")
                } finally {
                    isProcessingInvestMore = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (!displayGoalName.isNullOrBlank()) formatGoalName(displayGoalName) else "Scheme Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Text(
                            text = formatSchemeName(displaySchemeName),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displaySchemeName.firstOrNull()?.toString() ?: "P",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading || isProcessingInvestMore) {
                LoadingScreen(text = if (isProcessingInvestMore) "Preparing your investment..." else "Loading details...", modifier = Modifier.fillMaxSize())
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
                            SchemeDetailsCard(
                                state = state,
                                schemeName = displaySchemeName,
                                goalName = displayGoalName,
                                onViewDetails = { showDetailsPopup = true },
                                onTotalValueInfo = { showTotalValueInfo = true },
                                onGoldInfo = { showGoldInfo = true },
                                onSilverInfo = { showSilverInfo = true }
                            )
                        }

                        item {
                            val goalColor = accentColor
                            TabRow(
                                selectedTabIndex = selectedTabIndex,
                                containerColor = Color.Transparent,
                                divider = {},
                                indicator = {} // Custom indicator handled inside Tab
                            ) {
                                val tabs = listOf("Plans", "Transactions")
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = selectedTabIndex == index
                                    Tab(
                                        selected = isSelected,
                                        onClick = { selectedTabIndex = index }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                                .background(
                                                    if (isSelected) goalColor else Color.Transparent,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedTabIndex == 1) {
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
                                    TransactionRowRefined(tx)
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
                                val approved = state.mandates.filter { 
                                    val s = it.status?.uppercase() ?: ""
                                    (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
                                }
                                val paused = state.mandates.filter { it.status?.uppercase()?.contains("PAUSED") == true }
                                val other = state.mandates.filter { m ->
                                    val s = m.status?.uppercase() ?: ""
                                    !((s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")) && !s.contains("PAUSED")
                                }

                                if (paused.isNotEmpty()) {
                                    item { SectionHeader("Paused Plans") }
                                    items(paused) { mandate ->
                                        MandateRowRefined(
                                            mandate = mandate,
                                            onPause = { 
                                                mandateForPauseSip = mandate
                                                showPauseSipQuestionSheet = true 
                                            },
                                            onResume = { 
                                                mandateForResumeSip = mandate
                                                showResumeSipQuestionSheet = true
                                            },
                                            onCancel = { 
                                                mandateForCancelSip = mandate
                                                showCancelSipScreen = true 
                                            },
                                            accentColor = accentColor
                                        )
                                    }
                                }

                                if (approved.isNotEmpty()) {
                                    item { SectionHeader("Active Plans") }
                                    items(approved) { mandate ->
                                        MandateRowRefined(
                                            mandate = mandate,
                                            onPause = { 
                                                mandateForPauseSip = mandate
                                                showPauseSipQuestionSheet = true 
                                            },
                                            onResume = { 
                                                mandateForResumeSip = mandate
                                                showResumeSipQuestionSheet = true
                                            },
                                            onCancel = { 
                                                mandateForCancelSip = mandate
                                                showCancelSipScreen = true 
                                            },
                                            accentColor = accentColor
                                        )
                                    }
                                }

                                if (other.isNotEmpty()) {
                                    item { SectionHeader("Other Plans") }
                                    items(other) { mandate ->
                                        MandateRowRefined(
                                            mandate = mandate,
                                            onPause = { 
                                                mandateForPauseSip = mandate
                                                showPauseSipQuestionSheet = true 
                                            },
                                            onResume = { 
                                                mandateForResumeSip = mandate
                                                showResumeSipQuestionSheet = true
                                            },
                                            onCancel = { 
                                                mandateForCancelSip = mandate
                                                showCancelSipScreen = true 
                                            },
                                            accentColor = accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { handleAddFunds(purpose) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invest More", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val params = WithdrawInitParams(
                                    amount = state.currentValue,
                                    investmentInProgress = state.investmentInProgress,
                                    isin = state.isin ?: "",
                                    folio = state.folioNumber,
                                    schemeName = displaySchemeName,
                                    redemptionInProgress = state.redemptionInProgress,
                                    redeemableAmount = state.redeemableAmount
                                )
                                onNavigateToWithdraw(params)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(20.dp), tint = accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Withdraw", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SIP Action Overlays (aligned with Android)
            if (showCancelSipScreen && mandateForCancelSip != null) {
                CancelSipInfoScreen(
                    schemeName = displaySchemeName,
                    dailyAmount = mandateForCancelSip?.amount ?: 0.0,
                    onCancelSip = { showCancelReasonScreen = true },
                    onGoBack = { 
                        showCancelSipScreen = false
                        mandateForCancelSip = null
                    }
                )
            }

            if (showCancelReasonScreen && mandateForCancelSip != null) {
                CancelSipReasonScreen(
                    selectedReason = selectedCancelReason,
                    onReasonSelected = { selectedCancelReason = it },
                    onContinue = {
                        selectedCancelReason?.let { reason ->
                            viewModel.cancelSip(userId, mandateForCancelSip?.planId, mandateForCancelSip?.mandateId, reason.keyword)
                        }
                    },
                    onGoBack = { showCancelReasonScreen = false }
                )
            }

            if (showCancelSipSuccessSheet) {
                CancelSipSuccessBottomSheet(
                    onDone = {
                        showCancelSipSuccessSheet = false
                        reloadTransactions()
                    }
                )
            }

            if (showCancelSipErrorSheet) {
                CancelSipErrorBottomSheet(
                    onDone = { showCancelSipErrorSheet = false }
                )
            }

            if (showPauseSipQuestionSheet && mandateForPauseSip != null) {
                PauseSipConfirmBottomSheet(
                    isLoading = pauseSipLoading,
                    onCancel = { 
                        showPauseSipQuestionSheet = false
                        mandateForPauseSip = null
                    },
                    onConfirm = {
                        viewModel.pauseSip(userId, mandateForPauseSip?.planId, mandateForPauseSip?.mandateId)
                    }
                )
            }

            if (showPauseSipSuccessSheet) {
                PauseSipSuccessBottomSheet(
                    onDone = {
                        showPauseSipSuccessSheet = false
                        reloadTransactions()
                    }
                )
            }

            if (showPauseSipErrorSheet) {
                PauseSipErrorBottomSheet(
                    onDone = { showPauseSipErrorSheet = false }
                )
            }

            if (showResumeSipQuestionSheet && mandateForResumeSip != null) {
                ResumeSipConfirmBottomSheet(
                    isLoading = resumeSipLoading,
                    onCancel = {
                        showResumeSipQuestionSheet = false
                        mandateForResumeSip = null
                    },
                    onConfirm = {
                        viewModel.resumeSip(userId, mandateForResumeSip?.planId, mandateForResumeSip?.mandateId)
                    }
                )
            }

            if (showResumeSipSuccessSheet) {
                ResumeSipSuccessBottomSheet(
                    onDone = {
                        showResumeSipSuccessSheet = false
                        reloadTransactions()
                    }
                )
            }

            if (showResumeSipErrorSheet) {
                ResumeSipErrorBottomSheet(
                    onDone = { showResumeSipErrorSheet = false }
                )
            }

            if (showDetailsPopup) {
                SchemeDetailsPopup(
                    state = state,
                    schemeName = displaySchemeName,
                    goalName = displayGoalName,
                    onDismiss = { showDetailsPopup = false },
                    onInvestMore = {
                        showDetailsPopup = false
                        handleAddFunds(purpose)
                    }
                )
            }

            // Info Dialogs
            if (showTotalValueInfo) {
                InfoDialog(title = "Total Value", text = "This is the current market value of your investments including any pending transactions.", onDismiss = { showTotalValueInfo = false })
            }
            if (showGoldInfo) {
                InfoDialog(title = "Estimated Gold", text = "This value represents the estimated gold grams based on current market rates. For representational purposes only.", onDismiss = { showGoldInfo = false })
            }
            if (showSilverInfo) {
                InfoDialog(title = "Estimated Silver", text = "This value represents the estimated silver grams based on current market rates. For representational purposes only.", onDismiss = { showSilverInfo = false })
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun SchemeDetailsCard(
    state: SchemeDetailsState,
    schemeName: String,
    goalName: String,
    onViewDetails: () -> Unit,
    onTotalValueInfo: () -> Unit,
    onGoldInfo: () -> Unit,
    onSilverInfo: () -> Unit
) {
    val isGold = schemeName.contains("Gold", ignoreCase = true) || goalName.contains("Gold", ignoreCase = true)
    val isSilver = schemeName.contains("Silver", ignoreCase = true) || goalName.contains("Silver", ignoreCase = true)
    
    val allottedLabel = when {
        isGold -> "Your Gold"
        isSilver -> "Your Silver"
        else -> "Total Value"
    }
    
    val unitsText = when {
        isGold || isSilver -> {
            val units = state.unitsInGm ?: 0.0
            if (units < 1.0) "${(units * 1000).toInt()} mg" else "${units} g"
        }
        else -> "₹${formatIndian(state.cummulativeValue)}"
    }

    val baseGoalColor = getCorrelationColorForCategory(state.category, state.colorTheme)
    val goalColor = if (isSilver && baseGoalColor == Color(0xFF818181)) Color(0xFF1A1A1A) else baseGoalColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = goalColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(allottedLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.alpha(0.6f))
                        if (isGold || isSilver) {
                            IconButton(onClick = if (isGold) onGoldInfo else onSilverInfo, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            }
                        }
                    }
                    Text(unitsText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    SummaryBox(label = "Invested", value = "₹${formatIndian(state.investedAmount + state.investmentInProgress)}")
                }
                Box(modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SummaryBox(label = "Total Value", value = "₹${formatIndian(state.cummulativeValue)}", modifier = Modifier.weight(1f))
                        IconButton(onClick = onTotalValueInfo, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }
                }
            }
            
            if (state.totalGain != 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                val sign = if (state.totalGain >= 0) "+" else "-"
                val gainColor = if (state.totalGain >= 0) Color(0xFF2E7D32) else Color.Red
                Text(
                    text = "$sign ₹${formatIndian(abs(state.totalGain))} returns now",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = gainColor
                )
            }

            // Status Messages
            if (state.investmentInProgress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                StatusBox(
                    text = "Your investment of ₹${formatIndian(state.investmentInProgress)} is being processed. It may take 2-3 working days to reflect.",
                    color = goalColor
                )
            }

            if (state.redemptionInProgress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                StatusBox(
                    text = "Withdrawal of ₹${formatIndian(state.redemptionInProgress)} is in progress.",
                    color = Color(0xFF2E7D32),
                    isSuccess = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            
            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text("View Details →", fontWeight = FontWeight.Bold, color = goalColor)
            }
        }
    }
}

@Composable
fun StatusBox(text: String, color: Color, isSuccess: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSuccess) Color(0xFFE8F5E9) else color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                Icons.Default.Info, 
                contentDescription = null, 
                modifier = Modifier.size(16.dp).padding(top = 2.dp),
                tint = if (isSuccess) Color(0xFF2E7D32) else color
            )
            Text(text, style = MaterialTheme.typography.bodySmall, color = if (isSuccess) Color(0xFF1B5E20) else Color.Black.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Black) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun TransactionRowRefined(tx: TransactionDisplayItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (tx.isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.isCredit) Icons.Default.Add else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = if (tx.isCredit) Color(0xFF2E7D32) else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(tx.transactionType ?: "Transaction", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(tx.date ?: "", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (tx.isCredit) "+" else "-") + "₹${formatIndian(tx.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (tx.isCredit) Color(0xFF2E7D32) else Color.Black
            )
            Text(
                text = tx.state,
                style = MaterialTheme.typography.labelSmall,
                color = when(tx.state) {
                    "SUCCESS" -> Color(0xFF2E7D32)
                    "SUBMITTED" -> Color(0xFF1565C0)
                    "FAILED" -> Color.Red
                    else -> Color.Gray
                }
            )
        }
    }
}
@Composable
fun MandateRowRefined(
    mandate: MandateDisplayItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Daily SIP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                    Text("₹${formatIndian(mandate.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Next Date", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
                    Text(mandate.nextSipDate ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }

            val status = mandate.status?.uppercase() ?: ""
            val isPaused = status.contains("PAUSED")
            val isActive = (status.contains("ACTIVE") || status.contains("APPROVED")) && !isPaused

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActive) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", fontSize = 12.sp)
                    }
                } else if (isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontSize = 12.sp)
                    }
                }
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel", color = Color.Red, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SchemeDetailsPopup(
    state: SchemeDetailsState,
    schemeName: String,
    goalName: String,
    onDismiss: () -> Unit,
    onInvestMore: () -> Unit
) {
    val isSilver = schemeName.contains("Silver", ignoreCase = true) || goalName.contains("Silver", ignoreCase = true)
    val baseGoalColor = getCorrelationColorForCategory(state.category, state.colorTheme)
    val goalColor = if (isSilver && baseGoalColor == Color(0xFF818181)) Color(0xFF1A1A1A) else baseGoalColor
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatSchemeName(schemeName),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Summary section
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow("Folio Number", state.folioNumber ?: "N/A")
                        DetailRow("Total Units", state.totalUnitsAllotted.toString())
                        DetailRow("Invested Amount", "₹${formatIndian(state.investedAmount + state.investmentInProgress)}")
                        DetailRow("Current Value", "₹${formatIndian(state.currentValue)}")
                        DetailRow("Total Value", "₹${formatIndian(state.cummulativeValue)}")
                        DetailRow("Total Returns", "₹${formatIndian(state.totalGain)}", valueColor = if (state.totalGain >= 0) Color(0xFF2E7D32) else Color.Red)
                        DetailRow("Withdrawn Amount", "₹${formatIndian(state.withdrawnGain)}")
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Plans section
                    if (state.mandates.isNotEmpty()) {
                        Text("Active Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        state.mandates.forEach { mandate ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Daily SIP", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
                                    Text("₹${formatIndian(mandate.amount)}", fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
                                    Text(mandate.status ?: "ACTIVE", color = goalColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Footer
                Button(
                    onClick = onInvestMore,
                    modifier = Modifier.fillMaxWidth().padding(20.dp).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = goalColor)
                ) {
                    Text("Invest More", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.alpha(0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
@Composable
fun CancelSipInfoScreen(
    schemeName: String,
    dailyAmount: Double,
    onCancelSip: () -> Unit,
    onGoBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Think before you cancel!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Did you know?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Continuing your daily SIP of ₹${formatIndian(dailyAmount)} could help you reach your goals faster due to compounding.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Are you sure you want to cancel your SIP in $schemeName?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Red
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onCancelSip,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cancel SIP", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go Back", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CancelSipReasonScreen(
    selectedReason: CancelSipReason?,
    onReasonSelected: (CancelSipReason) -> Unit,
    onContinue: () -> Unit,
    onGoBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Why are you cancelling?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            val reasons = CancelSipReason.values()
            reasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReasonSelected(reason) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == reason,
                        onClick = { onReasonSelected(reason) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = reason.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedReason != null
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Go Back", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSipSuccessBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {}, dragHandle = null) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SIP Cancelled", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Your SIP has been cancelled successfully. It might take up to 24-48 hours to reflect in all records.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSipErrorBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {}, dragHandle = null) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Cancellation Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
            Text("We were unable to cancel your SIP at this moment. Please try again later or contact support.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipConfirmBottomSheet(isLoading: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ModalBottomSheet(onDismissRequest = if (isLoading) ({}) else onCancel, dragHandle = null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp).padding(bottom = 16.dp).alpha(if (isLoading) 0.3f else 1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Pause SIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Are you sure you want to pause your daily SIP? You can resume it anytime later.", style = MaterialTheme.typography.bodyLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
                        Text("Go Back")
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
                        Text("Pause")
                    }
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipSuccessBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {}, dragHandle = null) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SIP Paused", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Your SIP has been paused successfully. You will not be charged until you resume it.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipErrorBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {}, dragHandle = null) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Pause Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
            Text("We were unable to pause your SIP. Please check your connection and try again.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipConfirmBottomSheet(isLoading: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ModalBottomSheet(onDismissRequest = if (isLoading) ({}) else onCancel, dragHandle = null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp).padding(bottom = 16.dp).alpha(if (isLoading) 0.3f else 1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Resume SIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Do you want to resume your daily SIP now?", style = MaterialTheme.typography.bodyLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
                        Text("Cancel")
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp), enabled = !isLoading) {
                        Text("Resume")
                    }
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipSuccessBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {}, dragHandle = null) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SIP Resumed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Your SIP has been resumed successfully. Your next installment will be processed as scheduled.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipErrorBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {}, dragHandle = null) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Resume Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
            Text("We were unable to resume your SIP. Please try again later.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}
