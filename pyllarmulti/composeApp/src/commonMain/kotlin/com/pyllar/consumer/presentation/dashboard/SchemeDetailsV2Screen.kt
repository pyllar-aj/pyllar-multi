package com.pyllar.consumer.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.presentation.dashboard.WithdrawInitParams
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailsV2Screen(
    userId: String,
    purpose: String,
    onNavigateBack: () -> Unit,
    onNavigateToWithdraw: (WithdrawInitParams) -> Unit,
    onNavigateToAddFunds: (String) -> Unit, // purpose/goalId
    viewModel: SchemeDetailsViewModel? = null, // Can be passed for testing
    dashboardViewModel: DashboardViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val scope = rememberCoroutineScope()
    val effectiveViewModel = viewModel ?: koinInject<SchemeDetailsViewModel>()
    val state by effectiveViewModel.uiState.collectAsState()
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showDetailsPopup by remember { mutableStateOf(false) }
    var showInvestmentInProgressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId, purpose) {
        val params = SchemeDetailsParamsManager.get()
        effectiveViewModel.loadTransactions(userId, purpose, params)
    }
    
    // SIP Action States
    var showPauseSipQuestionSheet by remember { mutableStateOf(false) }
    var mandateForPauseSip by remember { mutableStateOf<MandateDisplayItem?>(null) }
    val pauseSipLoading by effectiveViewModel.pauseSipLoading.collectAsState()
    val pauseSipResult by effectiveViewModel.pauseSipResult.collectAsState()

    var showResumeSipQuestionSheet by remember { mutableStateOf(false) }
    var mandateForResumeSip by remember { mutableStateOf<MandateDisplayItem?>(null) }
    val resumeSipLoading by effectiveViewModel.resumeSipLoading.collectAsState()
    val resumeSipResult by effectiveViewModel.resumeSipResult.collectAsState()

    var showCancelSipScreen by remember { mutableStateOf(false) }
    var showCancelReasonScreen by remember { mutableStateOf(false) }
    var selectedCancelReason by remember { mutableStateOf<CancelSipReasonV2?>(null) }
    var mandateForCancelSip by remember { mutableStateOf<MandateDisplayItem?>(null) }
    val cancelSipLoading by effectiveViewModel.cancelSipLoading.collectAsState()
    val cancelSipResult by effectiveViewModel.cancelSipResult.collectAsState()

    val goalColor = getCorrelationColorForCategory(state.category, state.colorTheme)
    
    LaunchedEffect(pauseSipResult) {
        if (pauseSipResult is PauseSipResult.Success) {
            showPauseSipQuestionSheet = false
            dashboardViewModel.refreshDashboardData(userId)
        }
    }
    LaunchedEffect(resumeSipResult) {
        if (resumeSipResult is ResumeSipResult.Success) {
            showResumeSipQuestionSheet = false
            dashboardViewModel.refreshDashboardData(userId)
        }
    }
    LaunchedEffect(cancelSipResult) {
        if (cancelSipResult is CancelSipResult.Success) {
            showCancelReasonScreen = false
            showCancelSipScreen = false
            dashboardViewModel.refreshDashboardData(userId)
        }
    }

    if (showDetailsPopup) {
        SchemeDetailsPopup(
            goalName = state.goalName,
            schemeName = state.schemeName,
            unitsInGm = state.unitsInGm,
            folioNumber = state.folioNumber,
            totalUnitsAllotted = state.totalUnitsAllotted,
            investedAmount = state.investedAmount,
            currentValue = state.currentValue,
            totalValue = state.totalValue,
            investmentInProgress = state.investmentInProgress,
            totalGain = state.totalGain,
            withdrawnGain = state.withdrawnGain,
            availableGain = state.availableGain,
            redemptionInProgress = state.redemptionInProgress,
            category = state.category,
            colorTheme = state.colorTheme,
            mandates = state.mandates,
            onDismiss = { showDetailsPopup = false },
            onInvestMore = { 
                showDetailsPopup = false
                scope.launch {
                    dashboardViewModel.initGoalTxn(userId, purpose).collectLatest { result ->
                        if (result is Resource.Success) {
                            sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, purpose)
                            onNavigateToAddFunds(purpose)
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheme Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!showCancelSipScreen && !showCancelReasonScreen) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 12.dp
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButtonModule(
                                modifier = Modifier.weight(1f),
                                text = "Add Money",
                                icon = Icons.Default.Add,
                                containerColor = goalColor,
                                onClick = {
                                    scope.launch {
                                        dashboardViewModel.initGoalTxn(userId, purpose).collectLatest { result ->
                                            if (result is Resource.Success) {
                                                sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, purpose)
                                                onNavigateToAddFunds(purpose)
                                            }
                                        }
                                    }
                                }
                            )
                            ActionButtonModule(
                                modifier = Modifier.weight(1f),
                                text = "New Plan",
                                icon = Icons.Default.FlashOn,
                                containerColor = goalColor.copy(alpha = 0.82f),
                                onClick = {
                                    scope.launch {
                                        dashboardViewModel.initGoalTxn(userId, purpose).collectLatest { result ->
                                            if (result is Resource.Success) {
                                                sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, purpose)
                                                onNavigateToAddFunds(purpose)
                                            }
                                        }
                                    }
                                }
                            )
                            ActionButtonModule(
                                modifier = Modifier.weight(1f),
                                text = "Withdraw",
                                icon = Icons.Default.CallReceived,
                                containerColor = goalColor.copy(alpha = 0.6f),
                                onClick = {
                                    if (state.currentValue > 0) {
                                        val params = WithdrawInitParams(
                                            isin = state.isin ?: "",
                                            folio = state.folioNumber,
                                            amount = state.currentValue,
                                            investmentInProgress = state.investmentInProgress,
                                            schemeName = state.schemeName,
                                            canWithdraw = state.canWithdraw,
                                            redemptionInProgress = state.redemptionInProgress,
                                            redeemableAmount = state.redeemableAmount
                                        )
                                        onNavigateToWithdraw(params)
                                    } else {
                                        showInvestmentInProgressDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = goalColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val approvedMandate = state.mandates.firstOrNull { m ->
                            val s = m.status?.uppercase().orEmpty()
                            (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
                        }
                        SchemeDetailsCard(
                            schemeName = state.schemeName,
                            goalName = state.goalName,
                            unitsInGm = state.unitsInGm,
                            category = state.category,
                            colorTheme = state.colorTheme,
                            folioNumber = state.folioNumber,
                            investedAmount = state.investedAmount + state.investmentInProgress,
                            totalUnitsAllotted = state.totalUnitsAllotted,
                            totalValue = state.cummulativeValue,
                            currentValue = state.currentValue,
                            investmentInProgress = state.investmentInProgress,
                            totalGain = state.totalGain,
                            withdrawnGain = state.withdrawnGain,
                            availableGain = state.availableGain,
                            redemptionInProgress = state.redemptionInProgress,
                            onWithdrawClick = { }, // Handled by bottom bar
                            onViewDetailsClick = { showDetailsPopup = true },
                            hasApprovedPlan = approvedMandate != null,
                            nextSipDate = approvedMandate?.nextSipDate
                        )
                    }

                    item {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {}
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
                                            style = if (isSelected) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTabIndex == 0) {
                        // Plans List
                        if (state.mandates.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No plans found.", color = Color.Gray)
                                }
                            }
                        } else {
                            val pausedMandates = state.mandates.filter { it.status?.uppercase()?.contains("PAUSED") == true }
                            val approvedMandates = state.mandates.filter { m ->
                                val s = m.status?.uppercase().orEmpty()
                                (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
                            }
                            val otherMandates = state.mandates.filter { m ->
                                val s = m.status?.uppercase().orEmpty()
                                !((s.contains("APPROVED") || s.contains("ACTIVE")) || s.contains("PAUSED"))
                            }

                            if (pausedMandates.isNotEmpty()) {
                                item { SectionHeader("Paused") }
                                items(pausedMandates) { mandate ->
                                    MandateItem(
                                        mandate = mandate,
                                        onResume = { mandateForResumeSip = it; showResumeSipQuestionSheet = true }
                                    )
                                }
                            }
                            if (approvedMandates.isNotEmpty()) {
                                item { SectionHeader("Approved") }
                                items(approvedMandates) { mandate ->
                                    MandateItem(
                                        mandate = mandate,
                                        onPause = { mandateForPauseSip = it; showPauseSipQuestionSheet = true },
                                        onCancel = { mandateForCancelSip = it; showCancelSipScreen = true }
                                    )
                                }
                            }
                            if (otherMandates.isNotEmpty()) {
                                item { SectionHeader("Other") }
                                items(otherMandates) { mandate ->
                                    MandateItem(mandate = mandate)
                                }
                            }
                        }
                    } else {
                        // Transactions List
                        if (state.transactions.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No transactions found.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(state.transactions) { transaction ->
                                TransactionItem(transaction = transaction)
                            }
                        }
                    }
                }
            }

            // Sheets and Dialogs
            if (showPauseSipQuestionSheet && mandateForPauseSip != null) {
                PauseSipSheet(
                    onConfirm = { effectiveViewModel.pauseSip(userId, mandateForPauseSip?.planId, mandateForPauseSip?.mandateId) },
                    onDismiss = { showPauseSipQuestionSheet = false },
                    isLoading = pauseSipLoading
                )
            }

            if (showResumeSipQuestionSheet && mandateForResumeSip != null) {
                ResumeSipSheet(
                    onConfirm = { effectiveViewModel.resumeSip(userId, mandateForResumeSip?.planId, mandateForResumeSip?.mandateId) },
                    onDismiss = { showResumeSipQuestionSheet = false },
                    isLoading = resumeSipLoading
                )
            }

            if (showCancelSipScreen && mandateForCancelSip != null) {
                CancelSipInfoScreen(
                    schemeName = state.schemeName ?: "Scheme",
                    dailyAmount = mandateForCancelSip?.amount ?: 0.0,
                    onCancelSip = { showCancelSipScreen = false; showCancelReasonScreen = true },
                    onGoBack = { showCancelSipScreen = false }
                )
            }

            if (showCancelReasonScreen && mandateForCancelSip != null) {
                CancelSipReasonScreen(
                    selectedReason = selectedCancelReason,
                    onReasonSelected = { selectedCancelReason = it },
                    onContinue = {
                        effectiveViewModel.cancelSip(userId, mandateForCancelSip?.planId, mandateForCancelSip?.mandateId, selectedCancelReason?.keyword)
                    },
                    onGoBack = { showCancelReasonScreen = false; showCancelSipScreen = true },
                    isLoading = cancelSipLoading
                )
            }

            if (showInvestmentInProgressDialog) {
                AlertDialog(
                    onDismissRequest = { showInvestmentInProgressDialog = false },
                    title = { Text("Investment in Progress") },
                    text = { Text("Your first investment is being processed. You can withdraw once units are allotted.") },
                    confirmButton = {
                        TextButton(onClick = { showInvestmentInProgressDialog = false }) { Text("OK") }
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
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun ActionButtonModule(
    modifier: Modifier = Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SchemeDetailsCard(
    schemeName: String?,
    goalName: String? = null,
    unitsInGm: Double? = null,
    category: String? = null,
    colorTheme: String? = null,
    folioNumber: String?,
    investedAmount: Double = 0.0,
    totalUnitsAllotted: Double = 0.0,
    totalValue: Double = 0.0,
    currentValue: Double = 0.0,
    investmentInProgress: Double = 0.0,
    totalGain: Double = 0.0,
    withdrawnGain: Double = 0.0,
    availableGain: Double = 0.0,
    redemptionInProgress: Double = 0.0,
    onWithdrawClick: () -> Unit,
    onViewDetailsClick: () -> Unit = {},
    hasApprovedPlan: Boolean = false,
    nextSipDate: String? = null,
    showBottomSection: Boolean = true
) {
    val isGoldOrSilver = schemeName?.contains("Gold", ignoreCase = true) == true ||
            schemeName?.contains("Silver", ignoreCase = true) == true ||
            goalName?.contains("Gold", ignoreCase = true) == true ||
            goalName?.contains("Silver", ignoreCase = true) == true
    val isGold = schemeName?.contains("Gold", ignoreCase = true) == true || goalName?.contains("Gold", ignoreCase = true) == true
    val isSilver = schemeName?.contains("Silver", ignoreCase = true) == true || goalName?.contains("Silver", ignoreCase = true) == true
    val isEstimatedGold = isGoldOrSilver && (goalName?.contains("Gold", ignoreCase = true) == true || schemeName?.contains("Gold", ignoreCase = true) == true)
    
    val allottedValue = when {
        isGoldOrSilver && unitsInGm != null && unitsInGm > 0 -> if (unitsInGm < 1.0) "${formatRupeeAmount(unitsInGm * 1000, 0)} mg" else "${formatRupeeAmount(unitsInGm, 2)} g"
        totalUnitsAllotted > 0 -> "${formatRupeeAmount(totalUnitsAllotted, 3)} Units"
        else -> "—"
    }
    
    val goalColor = getCorrelationColorForCategory(category, colorTheme)
    val finalContainerColor = goalColor.copy(alpha = 0.08f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = finalContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Allotted Section
            val titleStr = when {
                isEstimatedGold -> "Your gold"
                isGoldOrSilver -> "Your silver"
                else -> "Your savings"
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = titleStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = allottedValue,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Boxes for Invested and Total Value
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoxModule(label = if (showBottomSection) "Invested" else "You invested", value = investedAmount, modifier = Modifier.weight(1f))
                BoxModule(label = if (showBottomSection) "Total Value" else "Worth today", value = totalValue, modifier = Modifier.weight(1f))
            }

            // Processing Warnings
            if (hasApprovedPlan && (investedAmount > 0 || totalValue > 0)) {
                WarningBox(message = "Your savings are being processed and units will be allotted soon.", color = goalColor)
            } else if (hasApprovedPlan && investedAmount == 0.0 && totalValue == 0.0 && !nextSipDate.isNullOrBlank()) {
                WarningBox(message = "Your first savings will start on $nextSipDate", color = goalColor)
            }

            if (showBottomSection) {
                if (redemptionInProgress > 0) {
                    SuccessBox(message = "Withdrawal of ₹${formatRupeeAmount(redemptionInProgress, 0)} is in progress.")
                }
                
                if (availableGain != 0.0) {
                    val sign = if (availableGain >= 0) "+" else "-"
                    Text(
                        text = "$sign₹${formatRupeeAmount(availableGain.coerceAtLeast(0.0), 0)} now",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onViewDetailsClick).padding(4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Details →",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = goalColor
                    )
                }
            }
        }
    }
}

@Composable
fun BoxModule(label: String, value: Double, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = "₹${formatRupeeAmount(value, 0)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun WarningBox(message: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun SuccessBox(message: String) {
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(12.dp)).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20))
        }
    }
}

@Composable
fun TransactionItem(transaction: TransactionDisplayItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.transactionType ?: "Transaction",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "₹${formatRupeeAmount(transaction.amount, 0)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (transaction.isCredit) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    transaction.date?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                Surface(
                    color = if (transaction.state.uppercase() == "COMPLETED") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.state,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (transaction.state.uppercase() == "COMPLETED") Color(0xFF2E7D32) else Color(0xFFF57C00)
                    )
                }
            }
        }
    }
}

@Composable
fun MandateItem(
    mandate: MandateDisplayItem,
    onPause: ((MandateDisplayItem) -> Unit)? = null,
    onResume: ((MandateDisplayItem) -> Unit)? = null,
    onCancel: ((MandateDisplayItem) -> Unit)? = null
) {
    val statusUpper = mandate.status?.uppercase().orEmpty()
    val isApproved = statusUpper.contains("APPROVED") || statusUpper.contains("ACTIVE")
    val isPaused = statusUpper.contains("PAUSED")
    val showActionStrip = (isApproved || isPaused) && (onPause != null || onResume != null || onCancel != null)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "₹${formatRupeeAmount(mandate.amount, 0)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Surface(
                        color = when {
                            isApproved -> Color(0xFFE8F5E9)
                            isPaused -> Color(0xFFFFF3E0)
                            else -> Color(0xFFF5F5F5)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = statusUpper, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                mandate.nextSipDate?.let {
                    Text(text = "Next SIP: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            if (showActionStrip) {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    if (isApproved && onPause != null) {
                        ActionText(text = "Pause", modifier = Modifier.weight(1f), onClick = { onPause(mandate) })
                        VerticalDivider()
                    }
                    if (isPaused && onResume != null) {
                        ActionText(text = "Resume", modifier = Modifier.weight(1f), onClick = { onResume(mandate) })
                        VerticalDivider()
                    }
                    if (onCancel != null) {
                        ActionText(text = "Cancel", modifier = Modifier.weight(1f), color = Color.Red, onClick = { onCancel(mandate) })
                    }
                }
            }
        }
    }
}

@Composable
fun ActionText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Box(modifier = modifier.clickable(onClick = onClick).padding(12.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@Composable
fun VerticalDivider() {
    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.LightGray.copy(alpha = 0.3f)))
}

@Composable
fun PauseSipSheet(onConfirm: () -> Unit, onDismiss: () -> Unit, isLoading: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause SIP") },
        text = { Text("Are you sure you want to pause this SIP? You can resume it anytime.") },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Pause")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResumeSipSheet(onConfirm: () -> Unit, onDismiss: () -> Unit, isLoading: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resume SIP") },
        text = { Text("Are you sure you want to resume this SIP?") },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Resume")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CancelSipInfoScreen(schemeName: String, dailyAmount: Double, onCancelSip: () -> Unit, onGoBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        IconButton(onClick = onGoBack) { Icon(Icons.Default.Close, contentDescription = "Close") }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Wait! Don't stop your savings yet.", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Stopping your SIP in $schemeName might affect your long-term goals.")
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onGoBack, modifier = Modifier.fillMaxWidth()) { Text("Keep Saving") }
        TextButton(onClick = onCancelSip, modifier = Modifier.fillMaxWidth()) { Text("Stop SIP anyway", color = Color.Red) }
    }
}

@Composable
fun CancelSipReasonScreen(
    selectedReason: CancelSipReasonV2?,
    onReasonSelected: (CancelSipReasonV2) -> Unit,
    onContinue: () -> Unit,
    onGoBack: () -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        IconButton(onClick = onGoBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Why do you want to stop?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
        
        val reasons = listOf(
            CancelSipReasonV2("Financial crunch", "financial_crunch"),
            CancelSipReasonV2("Change in goals", "change_goals"),
            CancelSipReasonV2("Poor performance", "poor_performance"),
            CancelSipReasonV2("Other", "other")
        )
        
        reasons.forEach { reason ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onReasonSelected(reason) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedReason?.keyword == reason.keyword, onClick = { onReasonSelected(reason) })
                Text(text = reason.label, modifier = Modifier.padding(start = 8.dp))
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedReason != null && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Confirm Cancellation")
        }
    }
}

@Composable
fun SchemeDetailsPopup(
    goalName: String?,
    schemeName: String?,
    unitsInGm: Double?,
    folioNumber: String?,
    totalUnitsAllotted: Double,
    investedAmount: Double,
    currentValue: Double,
    totalValue: Double,
    investmentInProgress: Double,
    totalGain: Double,
    withdrawnGain: Double,
    availableGain: Double,
    redemptionInProgress: Double = 0.0,
    category: String?,
    colorTheme: String?,
    mandates: List<MandateDisplayItem> = emptyList(),
    onDismiss: () -> Unit,
    onInvestMore: () -> Unit
) {
    val popupBackground = lerp(MaterialTheme.colorScheme.surface, getCorrelationColorForCategory(category, colorTheme), 0.10f)
    
    Surface(modifier = Modifier.fillMaxSize(), color = popupBackground) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 48.dp, bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Text(text = schemeName ?: goalName ?: "Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = getCorrelationColorForCategory(category, colorTheme))
            }

            SchemeDetailsCard(
                schemeName = schemeName,
                goalName = goalName,
                unitsInGm = unitsInGm,
                category = category,
                colorTheme = colorTheme,
                folioNumber = folioNumber,
                investedAmount = investedAmount,
                totalUnitsAllotted = totalUnitsAllotted,
                totalValue = totalValue,
                currentValue = currentValue,
                investmentInProgress = investmentInProgress,
                totalGain = totalGain,
                withdrawnGain = withdrawnGain,
                availableGain = availableGain,
                redemptionInProgress = redemptionInProgress,
                onWithdrawClick = { },
                showBottomSection = false
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("About your investment")
            DetailRow("Folio Number", folioNumber ?: "—")
            DetailRow("Category", category ?: "—")
            DetailRow("Units Allotted", if (totalUnitsAllotted > 0) formatRupeeAmount(totalUnitsAllotted, 3) else "—")
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onInvestMore, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = getCorrelationColorForCategory(category, colorTheme))) {
                Text("Invest More")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

private fun formatRupeeAmount(value: Double, decimals: Int = 1): String {
    // Basic formatting for KMP (Native platforms might need better utils)
    val factor = when(decimals) {
        0 -> 1
        1 -> 10
        2 -> 100
        3 -> 1000
        else -> 10
    }
    val rounded = (value * factor).toLong().toDouble() / factor
    return rounded.toString()
}

data class CancelSipReasonV2(val label: String, val keyword: String)
