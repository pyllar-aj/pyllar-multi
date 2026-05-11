package com.pyllar.consumer.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.navigation.AppRoutes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailsV2Screen(
    userId: String = "",
    purpose: String = "",
    onNavigateBack: () -> Unit = {},
    onNavigateToWithdraw: (WithdrawInitParams) -> Unit = {},
    onNavigateToAddFunds: (userId: String, kycAttemptId: String, investorId: String, goalId: String, isExistingInvestment: Boolean) -> Unit = { _, _, _, _, _ -> },
    onNavigateToLumpsum: (userId: String, kycAttemptId: String, investorId: String, goalId: String, isExistingInvestment: Boolean) -> Unit = { _, _, _, _, _ -> },
    onNavigateToFundDetails: (isin: String, userId: String, goalId: String, sipAmount: Double, kycAttemptId: String, investorId: String, fromSipAmount: Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
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

    var showPlansView by remember { mutableStateOf(false) }
    var showTransactionsView by remember { mutableStateOf(false) }
    var showDetailsPopup by remember { mutableStateOf(false) }

    // SIP Action States
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

    var schemeParams by remember { mutableStateOf<SchemeDetailsParams?>(SchemeDetailsParamsManager.get()) }
    
    val displaySchemeName = schemeParams?.schemeName?.takeIf { it.isNotBlank() } ?: state.schemeName.orEmpty()
    val displayGoalName = schemeParams?.goalName?.takeIf { it.isNotBlank() } ?: state.goalName.orEmpty()
    val category = state.category ?: schemeParams?.category
    val colorTheme = state.colorTheme ?: schemeParams?.colorTheme

    val goalType = identifyGoalType(category, displaySchemeName)
    val accentColor = getCorrelationColorForCategory(category, colorTheme)

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetailsV2")
    }

    LaunchedEffect(userId, purpose) {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            val currentParams = schemeParams ?: SchemeDetailsParamsManager.get()
            val uipid = currentParams?.userPurposeId ?: purpose
            viewModel.loadTransactions(userId, uipid, currentParams)
        }
    }

    // Handle Results
    LaunchedEffect(cancelSipResult) {
        if (cancelSipResult is CancelSipResult.Success) {
            showCancelSipScreen = false
            showCancelReasonScreen = false
            showCancelSipSuccessSheet = true
            viewModel.clearCancelSipResult()
        } else if (cancelSipResult is CancelSipResult.Error) {
            showCancelSipScreen = false
            showCancelReasonScreen = false
            showCancelSipErrorSheet = true
            viewModel.clearCancelSipResult()
        }
    }

    LaunchedEffect(pauseSipResult) {
        if (pauseSipResult is PauseSipResult.Success) {
            showPauseSipQuestionSheet = false
            showPauseSipSuccessSheet = true
            viewModel.clearPauseSipResult()
        } else if (pauseSipResult is PauseSipResult.Error) {
            showPauseSipQuestionSheet = false
            showPauseSipErrorSheet = true
            viewModel.clearPauseSipResult()
        }
    }

    LaunchedEffect(resumeSipResult) {
        if (resumeSipResult is ResumeSipResult.Success) {
            showResumeSipQuestionSheet = false
            showResumeSipSuccessSheet = true
            viewModel.clearResumeSipResult()
        } else if (resumeSipResult is ResumeSipResult.Error) {
            showResumeSipQuestionSheet = false
            showResumeSipErrorSheet = true
            viewModel.clearResumeSipResult()
        }
    }

    val reloadData = {
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            val currentParams = schemeParams ?: SchemeDetailsParamsManager.get()
            viewModel.loadTransactions(userId, purpose, currentParams)
        }
    }

    val handleAddFunds = { gid: String, isLumpsum: Boolean ->
        scope.launch {
            try {
                val result = dashboardViewModel.initGoalTxn(userId, gid)
                if (result is Resource.Success) {
                    result.data?.let { response ->
                        if (response.userPurposeId.isNotBlank()) {
                            sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                        }
                    }
                }
                
                sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, gid)
                val hasInvestment = state.investedAmount > 0 || !state.folioNumber.isNullOrBlank()
                sessionStore.saveValue("isExistingInvestment", hasInvestment.toString())

                val kycAttemptId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                val investorId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""

                if (isLumpsum) {
                    onNavigateToLumpsum(userId, kycAttemptId, investorId, gid, hasInvestment)
                } else {
                    onNavigateToAddFunds(userId, kycAttemptId, investorId, gid, hasInvestment)
                }
            } catch (e: Exception) {
                platformLog("Error: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                MainContentV2(
                    state = state,
                    goalType = goalType,
                    accentColor = accentColor,
                    displayGoalName = displayGoalName,
                    displaySchemeName = displaySchemeName,
                    onBack = { onNavigateBack() },
                    onShowPlans = { showPlansView = true },
                    onShowTransactions = { showTransactionsView = true },
                    onShowDetails = { showDetailsPopup = true },
                    onAddFunds = { handleAddFunds(purpose, false) },
                    onLumpsum = { handleAddFunds(purpose, true) },
                    onWithdraw = {
                        val instantVal = state.instantRedemptionValue 
                            ?: schemeParams?.instantRedemptionValue 
                            ?: SchemeDetailsParamsManager.get()?.instantRedemptionValue

                        val params = WithdrawInitParams(
                            isin = state.isin ?: "",
                            folio = state.folioNumber,
                            amount = state.currentValue,
                            investmentInProgress = state.investmentInProgress,
                            bankAccountNumber = "",
                            bankAccountIfscCode = "",
                            schemeName = displaySchemeName,
                            canWithdraw = state.canWithdraw,
                            redemptionInProgress = state.redemptionInProgress,
                            redeemableAmount = state.redeemableAmount,
                            instantRedemptionValue = instantVal
                        )
                        WithdrawParamsManager.set(params)
                        scope.launch {
                            sessionStore.saveValue("withdraw_init_params", WithdrawParamsManager.toJson(params))
                        }
                        onNavigateToWithdraw(params)
                    },
                    onFundDetails = {
                        state.isin?.let { isin ->
                            onNavigateToFundDetails(isin, userId, purpose, 0.0, "", "", false)
                        }
                    }
                )
            }

            // Overlays & Sheets
            if (showPlansView) {
                PlansOverlay(
                    mandates = state.mandates,
                    accentColor = accentColor,
                    onDismiss = { showPlansView = false },
                    onPause = { m ->
                        mandateForPauseSip = m
                        showPauseSipQuestionSheet = true
                    },
                    onResume = { m ->
                        mandateForResumeSip = m
                        showResumeSipQuestionSheet = true
                    },
                    onCancel = { m ->
                        mandateForCancelSip = m
                        showCancelSipScreen = true
                    }
                )
            }

            if (showTransactionsView) {
                TransactionsOverlay(
                    transactions = state.transactions,
                    accentColor = accentColor,
                    onDismiss = { showTransactionsView = false }
                )
            }

            if (showDetailsPopup) {
                SchemeDetailsPopupV2(
                    state = state,
                    goalName = displayGoalName,
                    schemeName = displaySchemeName,
                    onDismiss = { showDetailsPopup = false },
                    onInvestMore = {
                        showDetailsPopup = false
                        handleAddFunds(purpose, false)
                    }
                )
            }

            // SIP Action Overlays
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

            // Sheets
            if (showCancelSipSuccessSheet) {
                CancelSipSuccessBottomSheet(onDone = { showCancelSipSuccessSheet = false; reloadData() })
            }
            if (showCancelSipErrorSheet) {
                CancelSipErrorBottomSheet(onDone = { showCancelSipErrorSheet = false })
            }
            if (showPauseSipQuestionSheet && mandateForPauseSip != null) {
                PauseSipConfirmBottomSheet(
                    isLoading = pauseSipLoading,
                    onCancel = { showPauseSipQuestionSheet = false },
                    onConfirm = { viewModel.pauseSip(userId, mandateForPauseSip?.planId, mandateForPauseSip?.mandateId) }
                )
            }
            if (showPauseSipSuccessSheet) {
                PauseSipSuccessBottomSheet(onDone = { showPauseSipSuccessSheet = false; reloadData() })
            }
            if (showPauseSipErrorSheet) {
                PauseSipErrorBottomSheet(onDone = { showPauseSipErrorSheet = false })
            }
            if (showResumeSipQuestionSheet && mandateForResumeSip != null) {
                ResumeSipConfirmBottomSheet(
                    isLoading = resumeSipLoading,
                    onCancel = { showResumeSipQuestionSheet = false },
                    onConfirm = { viewModel.resumeSip(userId, mandateForResumeSip?.planId, mandateForResumeSip?.mandateId) }
                )
            }
            if (showResumeSipSuccessSheet) {
                ResumeSipSuccessBottomSheet(onDone = { showResumeSipSuccessSheet = false; reloadData() })
            }
            if (showResumeSipErrorSheet) {
                ResumeSipErrorBottomSheet(onDone = { showResumeSipErrorSheet = false })
            }
        }
    }
}

@Composable
fun MainContentV2(
    state: SchemeDetailsState,
    goalType: GoalType,
    accentColor: Color,
    displayGoalName: String,
    displaySchemeName: String,
    onBack: () -> Unit,
    onShowPlans: () -> Unit,
    onShowTransactions: () -> Unit,
    onShowDetails: () -> Unit,
    onAddFunds: () -> Unit,
    onLumpsum: () -> Unit,
    onWithdraw: () -> Unit,
    onFundDetails: () -> Unit
) {
    val gradient = getGradientForCategory(goalType)
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(Brush.linearGradient(
                    colors = gradient,
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                ))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    IconButton(onClick = onFundDetails) {
                        Icon(Icons.Default.Info, contentDescription = "Fund Info", tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                val title = when (goalType) {
                    GoalType.GOLD -> "Your Gold"
                    GoalType.SILVER -> "Your Silver"
                    else -> "Your Savings"
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                
                val unitsText = when (goalType) {
                    GoalType.GOLD, GoalType.SILVER -> {
                        val units = state.unitsInGm ?: 0.0
                        if (units < 1.0) "${(units * 1000).toInt()} mg" else "${units} g"
                    }
                    else -> "₹${formatIndian(state.cummulativeValue)}"
                }
                
                Text(
                    text = unitsText,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 36.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.mandates.any { it.status?.uppercase()?.contains("ACTIVE") == true }) {
                        StatusPill(
                            text = "Daily Saving Active",
                            backgroundColor = Color.White.copy(alpha = 0.4f),
                            contentColor = Color.Black,
                            icon = Icons.Default.Check
                        )
                    }
                    if (state.investmentInProgress > 0) {
                        StatusPill(
                            text = "Allocation in progress",
                            backgroundColor = Color.White.copy(alpha = 0.4f),
                            contentColor = Color.Black,
                            icon = Icons.Default.Schedule
                        )
                    }
                }
            }

            // Locker Image
            val lockerImg = when (goalType) {
                GoalType.GOLD -> Res.drawable.gold_locker
                GoalType.SILVER -> Res.drawable.silver_locker
                else -> Res.drawable.savings_locker
            }
            
            Image(
                painter = painterResource(lockerImg),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(200.dp)
                    .offset(x = 40.dp, y = 20.dp)
                    .alpha(0.8f)
            )
        }

        // Stats Grid
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "INVESTED",
                    value = state.investedAmount + state.investmentInProgress,
                    subtext = "Total input"
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL VALUE",
                    value = state.cummulativeValue,
                    gain = state.totalGain,
                    subtext = "Current worth"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dashboard Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = "Plans",
                    description = "${state.mandates.size} active automations",
                    icon = Icons.Default.FlashOn,
                    iconColor = accentColor,
                    onClick = onShowPlans
                )
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = "Transactions",
                    description = "History & Statements",
                    icon = Icons.Default.History,
                    iconColor = accentColor,
                    onClick = onShowTransactions
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = "Goal Info",
                    description = "Details & Breakdowns",
                    icon = Icons.Default.BarChart,
                    iconColor = accentColor,
                    onClick = onShowDetails
                )
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = "Withdraw",
                    description = "Sell & Redeem funds",
                    icon = Icons.Default.CallReceived,
                    iconColor = accentColor,
                    onClick = onWithdraw
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButtonModuleV2(
                    modifier = Modifier.weight(1f),
                    text = "Daily Save",
                    icon = Icons.Default.FlashOn,
                    containerColor = accentColor,
                    onClick = onAddFunds
                )
                ActionButtonModuleV2(
                    modifier = Modifier.weight(1f),
                    text = "One-time",
                    icon = Icons.Default.Add,
                    containerColor = accentColor,
                    onClick = onLumpsum
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryBox(
    modifier: Modifier = Modifier,
    label: String,
    value: Double,
    gain: Double? = null,
    subtext: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${formatIndian(value)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                if (gain != null && value != 0.0) {
                    val percent = abs(gain / value * 100)
                    val color = if (gain >= 0) Color(0xFF2E7D32) else Color.Red
                    Text(
                        text = " (${if (gain >= 0) "+" else "-"}${formatDecimal(percent, 1)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun DashboardTile(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(110.dp).clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ActionButtonModuleV2(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(64.dp).clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansOverlay(
    mandates: List<MandateDisplayItem>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPause: (MandateDisplayItem) -> Unit,
    onResume: (MandateDisplayItem) -> Unit,
    onCancel: (MandateDisplayItem) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Daily Plans", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { padding ->
            if (mandates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active plans found", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(mandates) { mandate ->
                        MandateItemV2(mandate, accentColor, onPause, onResume, onCancel)
                    }
                }
            }
        }
    }
}

@Composable
fun MandateItemV2(
    mandate: MandateDisplayItem,
    accentColor: Color,
    onPause: (MandateDisplayItem) -> Unit,
    onResume: (MandateDisplayItem) -> Unit,
    onCancel: (MandateDisplayItem) -> Unit
) {
    val isPaused = mandate.status?.uppercase()?.contains("PAUSED") == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "₹${formatIndian(mandate.amount)}/day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(text = "Investment Plan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                StatusPill(
                    text = if (isPaused) "Paused" else "Active",
                    backgroundColor = if (isPaused) Color.Red.copy(alpha = 0.1f) else Color(0xFFE8F5E9),
                    contentColor = if (isPaused) Color.Red else Color(0xFF2E7D32),
                    icon = if (isPaused) Icons.Default.Pause else Icons.Default.Check
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onCancel(mandate) }) {
                    Text("Cancel", color = Color.Red)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (isPaused) onResume(mandate) else onPause(mandate) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isPaused) "Resume" else "Pause")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsOverlay(
    transactions: List<TransactionDisplayItem>,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color.White
        ) { padding ->
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    items(transactions) { tx ->
                        TransactionItemV2(tx)
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemV2(tx: TransactionDisplayItem) {
    val isCredit = tx.isCredit
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(if (isCredit) Color(0xFFE8F5E9) else Color(0xFFFFF3E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Default.Add else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = if (isCredit) Color(0xFF2E7D32) else Color(0xFFF57C00),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = if (isCredit) "Money Added" else "Withdrawal", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = tx.date ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isCredit) "+" else "-"} ₹${formatIndian(tx.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = if (isCredit) Color(0xFF2E7D32) else Color.Black
            )
            Text(text = tx.state ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun SchemeDetailsPopupV2(
    state: SchemeDetailsState,
    goalName: String,
    schemeName: String,
    onDismiss: () -> Unit,
    onInvestMore: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SchemeDetailsPopupContentV2(
            state = state,
            goalName = goalName,
            schemeName = schemeName,
            onDismiss = onDismiss,
            onInvestMore = onInvestMore
        )
    }
}

@Composable
fun SchemeDetailsPopupContentV2(
    state: SchemeDetailsState,
    goalName: String,
    schemeName: String,
    onDismiss: () -> Unit,
    onInvestMore: () -> Unit
) {
    val goalType = identifyGoalType(state.category, schemeName)
    val accentColor = getCorrelationColorForCategory(state.category, state.colorTheme)
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, null) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatSchemeName(schemeName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            SchemeDetailsCardV2(
                schemeName = schemeName,
                goalName = goalName,
                unitsInGm = state.unitsInGm,
                category = state.category,
                colorTheme = state.colorTheme,
                folioNumber = state.folioNumber,
                investedAmount = state.investedAmount,
                totalUnitsAllotted = state.unitsInGm ?: 0.0,
                totalValue = state.cummulativeValue,
                currentValue = state.currentValue,
                investmentInProgress = state.investmentInProgress,
                totalGain = state.totalGain,
                redemptionInProgress = state.redemptionInProgress,
                hasApprovedPlan = state.mandates.isNotEmpty(),
                showBottomSection = false,
                containerColor = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.investmentInProgress > 0 || state.mandates.isNotEmpty()) {
                Text(
                    text = "WHAT'S HAPPENING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (state.mandates.isNotEmpty()) {
                            val totalDaily = state.mandates.sumOf { it.amount }
                            WhatsHappeningRowV2(
                                title = "Saving ₹${formatIndian(totalDaily)} daily",
                                subtitle = "Automatic daily investment",
                                badgeText = "ACTIVE",
                                badgeBg = Color(0xFFE8F5E9),
                                badgeFg = Color(0xFF2E7D32)
                            )
                        }
                        if (state.investmentInProgress > 0) {
                            if (state.mandates.isNotEmpty()) HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            WhatsHappeningRowV2(
                                title = "Allocation in progress",
                                subtitle = "₹${formatIndian(state.investmentInProgress)} being invested",
                                badgeText = "PROCESSING",
                                badgeBg = Color(0xFFFFF3E0),
                                badgeFg = Color(0xFFF57C00)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onInvestMore,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Invest More", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SchemeDetailsCardV2(
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
    redemptionInProgress: Double = 0.0,
    hasApprovedPlan: Boolean = false,
    showBottomSection: Boolean = true,
    containerColor: Color? = null
) {
    val goalColor = getCorrelationColorForCategory(category, colorTheme)
    val finalContainerColor = containerColor ?: goalColor.copy(alpha = 0.1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = finalContainerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("INVESTED", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    RupeeAmountBlock(value = investedAmount, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("CURRENT VALUE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    RupeeAmountBlock(value = totalValue, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
            }

            if (totalGain != 0.0) {
                val color = if (totalGain >= 0) Color(0xFF2E7D32) else Color.Red
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OVERALL GAIN: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    RupeeAmountBlock(value = totalGain, style = MaterialTheme.typography.labelSmall, color = color, showPlus = true)
                }
            }

            if (folioNumber != null) {
                Text("Folio: $folioNumber", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RupeeAmountBlock(
    value: Double,
    style: TextStyle,
    color: Color,
    showPlus: Boolean = false,
    decimalPlaces: Int = 1
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showPlus && value > 0) Text("+", style = style, color = color)
        else if (value < 0) Text("-", style = style, color = color)
        Text("₹", style = style, color = color)
        Text(formatIndian(abs(value)), style = style, color = color)
    }
}

@Composable
fun WhatsHappeningRowV2(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBg: Color,
    badgeFg: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Surface(color = badgeBg, shape = RoundedCornerShape(4.dp)) {
            Text(
                text = badgeText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = badgeFg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CancelSipInfoScreen(
    schemeName: String,
    dailyAmount: Double,
    onCancelSip: () -> Unit,
    onGoBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Cancel Daily Plan?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Are you sure you want to cancel your daily saving of ₹${formatIndian(dailyAmount)} in $schemeName?",
                textAlign = TextAlign.Center, color = Color.Gray
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onCancelSip,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel Daily Saving", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onGoBack) { Text("Keep Saving", color = Color.Black) }
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
    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(onClick = onGoBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Why are you cancelling?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            CancelSipReason.values().forEach { reason ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onReasonSelected(reason) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedReason == reason, onClick = { onReasonSelected(reason) })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(reason.label)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = selectedReason != null) {
                Text("Continue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSipSuccessBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(64.dp))
            Text("Plan Cancelled", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Your daily saving plan has been successfully cancelled.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSipErrorBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Text("Cancellation Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Something went wrong. Please try again.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("OK") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipConfirmBottomSheet(isLoading: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Pause Daily Saving?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("You can resume your savings at any time.", color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Go Back") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f), enabled = !isLoading) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Pause")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipSuccessBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PauseCircle, null, tint = Color(0xFFF57C00), modifier = Modifier.size(64.dp))
            Text("Plan Paused", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipErrorBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Text("Pause Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("OK") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipConfirmBottomSheet(isLoading: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Resume Daily Saving?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Start saving daily again towards your goal.", color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Go Back") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f), enabled = !isLoading) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Resume")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipSuccessBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(64.dp))
            Text("Plan Resumed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipErrorBottomSheet(onDone: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Text("Resume Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("OK") }
        }
    }
}


private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return ""
    return dateString
}

fun getGradientForCategory(goalType: GoalType): List<Color> {
    return when (goalType) {
        GoalType.GOLD -> listOf(Color(0xFFFFF9E6), Color(0xFFFFE8B8))
        GoalType.SILVER -> listOf(Color(0xFFF5F5F5), Color(0xFFE8E8E8))
        GoalType.SAVINGS, GoalType.SAVINGS_PLUS -> listOf(Color(0xFFE8F5E9), Color(0xFFA5D6A7))
        else -> listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))
    }
}
