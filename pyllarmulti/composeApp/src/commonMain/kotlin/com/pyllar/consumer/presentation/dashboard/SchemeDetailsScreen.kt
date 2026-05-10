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

    val schemeParams = remember { SchemeDetailsParamsManager.get() }
    val displaySchemeName = schemeParams?.schemeName?.takeIf { it.isNotBlank() } ?: state.schemeName.orEmpty()
    val displayGoalName = schemeParams?.goalName?.takeIf { it.isNotBlank() } ?: state.goalName.orEmpty()

    // Info Popups state
    var showTotalValueInfo by remember { mutableStateOf(false) }
    var showGoldInfo by remember { mutableStateOf(false) }
    var showSilverInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetails")
    }

    LaunchedEffect(userId, purpose) {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            viewModel.loadTransactions(userId, purpose, schemeParams)
        }
    }

    // Handle SIP Action Results
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cancelSipResult) {
        when (cancelSipResult) {
            is CancelSipResult.Success -> {
                showCancelConfirm = null
                viewModel.loadTransactions(userId, purpose, schemeParams)
                viewModel.clearCancelSipResult()
            }
            is CancelSipResult.Error -> {
                errorMessage = (cancelSipResult as CancelSipResult.Error).message
                viewModel.clearCancelSipResult()
            }
            else -> {}
        }
    }
    LaunchedEffect(pauseSipResult) {
        when (pauseSipResult) {
            is PauseSipResult.Success -> {
                showPauseConfirm = null
                viewModel.loadTransactions(userId, purpose, schemeParams)
                viewModel.clearPauseSipResult()
            }
            is PauseSipResult.Error -> {
                errorMessage = (pauseSipResult as PauseSipResult.Error).message
                viewModel.clearPauseSipResult()
            }
            else -> {}
        }
    }
    LaunchedEffect(resumeSipResult) {
        when (resumeSipResult) {
            is ResumeSipResult.Success -> {
                showResumeConfirm = null
                viewModel.loadTransactions(userId, purpose, schemeParams)
                viewModel.clearResumeSipResult()
            }
            is ResumeSipResult.Error -> {
                errorMessage = (resumeSipResult as ResumeSipResult.Error).message
                viewModel.clearResumeSipResult()
            }
            else -> {}
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
                            color = getCorrelationColorForCategory(state.category, state.colorTheme)
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
                            color = getCorrelationColorForCategory(state.category, state.colorTheme)
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
                            val goalColor = getCorrelationColorForCategory(state.category, state.colorTheme)
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
                                            onPause = { showPauseConfirm = mandate },
                                            onResume = { showResumeConfirm = mandate },
                                            onCancel = { showCancelConfirm = mandate },
                                            accentColor = getCorrelationColorForCategory(state.category, state.colorTheme)
                                        )
                                    }
                                }

                                if (approved.isNotEmpty()) {
                                    item { SectionHeader("Active Plans") }
                                    items(approved) { mandate ->
                                        MandateRowRefined(
                                            mandate = mandate,
                                            onPause = { showPauseConfirm = mandate },
                                            onResume = { showResumeConfirm = mandate },
                                            onCancel = { showCancelConfirm = mandate },
                                            accentColor = getCorrelationColorForCategory(state.category, state.colorTheme)
                                        )
                                    }
                                }

                                if (other.isNotEmpty()) {
                                    item { SectionHeader("Other Plans") }
                                    items(other) { mandate ->
                                        MandateRowRefined(
                                            mandate = mandate,
                                            onPause = { showPauseConfirm = mandate },
                                            onResume = { showResumeConfirm = mandate },
                                            onCancel = { showCancelConfirm = mandate },
                                            accentColor = getCorrelationColorForCategory(state.category, state.colorTheme)
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
                            colors = ButtonDefaults.buttonColors(containerColor = getCorrelationColorForCategory(state.category, state.colorTheme))
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
                            border = BorderStroke(1.dp, getCorrelationColorForCategory(state.category, state.colorTheme))
                        ) {
                            Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(20.dp), tint = getCorrelationColorForCategory(state.category, state.colorTheme))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Withdraw", color = getCorrelationColorForCategory(state.category, state.colorTheme), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action Dialogs
            showPauseConfirm?.let { mandate ->
                AlertDialog(
                    onDismissRequest = { showPauseConfirm = null },
                    title = { Text("Pause SIP") },
                    text = { Text("Are you sure you want to pause this SIP of ₹${formatIndian(mandate.amount)}?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.pauseSip(userId, mandate.planId, mandate.mandateId) },
                            enabled = !pauseSipLoading
                        ) {
                            if (pauseSipLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Pause")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showPauseConfirm = null }) { Text("Cancel") } }
                )
            }

            showResumeConfirm?.let { mandate ->
                AlertDialog(
                    onDismissRequest = { showResumeConfirm = null },
                    title = { Text("Resume SIP") },
                    text = { Text("Do you want to resume your SIP of ₹${formatIndian(mandate.amount)}?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.resumeSip(userId, mandate.planId, mandate.mandateId) },
                            enabled = !resumeSipLoading
                        ) {
                            if (resumeSipLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Resume")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showResumeConfirm = null }) { Text("Cancel") } }
                )
            }

            showCancelConfirm?.let { mandate ->
                AlertDialog(
                    onDismissRequest = { showCancelConfirm = null },
                    title = { Text("Cancel SIP") },
                    text = { Text("Cancelling your SIP will stop future investments. Are you sure?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.cancelSip(userId, mandate.planId, mandate.mandateId, "User Request") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            enabled = !cancelSipLoading
                        ) {
                            if (cancelSipLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text("Cancel SIP", color = Color.White)
                        }
                    },
                    dismissButton = { TextButton(onClick = { showCancelConfirm = null }) { Text("Close") } }
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

            errorMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Error") },
                    text = { Text(msg) },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) { Text("OK") }
                    }
                )
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

    val goalColor = getCorrelationColorForCategory(state.category, state.colorTheme)

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
    val goalColor = getCorrelationColorForCategory(state.category, state.colorTheme)
    
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
