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
import com.pyllar.consumer.presentation.ui.components.rememberDebouncedClick
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.*
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.navigation.AppRoutes
import kotlinx.coroutines.launch
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.pyllar.consumer.presentation.ui.theme.V2Cream
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
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
    
    var showTotalValueInfoPopup by remember { mutableStateOf(false) }
    var showEstimatedGoldInfoPopup by remember { mutableStateOf(false) }
    var showEstimatedSilverInfoPopup by remember { mutableStateOf(false) }
    var showInvestmentInProgressDialog by remember { mutableStateOf(false) }
    var showFolioPendingDialog by remember { mutableStateOf(false) }

    var schemeParams by remember { mutableStateOf<SchemeDetailsParams?>(SchemeDetailsParamsManager.get()) }
    
    val displaySchemeName = schemeParams?.schemeName?.takeIf { it.isNotBlank() } ?: state.schemeName.orEmpty()
    val displayGoalName = schemeParams?.goalName?.takeIf { it.isNotBlank() } ?: state.goalName.orEmpty()
    val category = state.category ?: schemeParams?.category
    val colorTheme = state.colorTheme ?: schemeParams?.colorTheme

    val goalType = identifyGoalType(category, displaySchemeName)
    val accentColor = if (category?.uppercase() == "SILVER") Color.Black else getCorrelationColorForCategory(category, colorTheme)

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetailsV2")
    }

    LaunchedEffect(userId, purpose) {
        platformLog("SchemeDetailsV2: 🔄 Initial load LaunchedEffect - userId: $userId, purpose: $purpose")
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            // Attempt to restore params from session store if manager is empty
            if (SchemeDetailsParamsManager.get() == null) {
                platformLog("SchemeDetailsV2: 🔍 Manager empty, attempting restore from sessionStore")
                val stored = sessionStore.getValue("scheme_details_params_$purpose")
                val restored = SchemeDetailsParamsManager.fromJson(stored)
                if (restored != null) {
                    platformLog("SchemeDetailsV2: ✅ Restored params from sessionStore")
                    SchemeDetailsParamsManager.set(restored)
                    schemeParams = restored
                }
            } else if (schemeParams == null) {
                schemeParams = SchemeDetailsParamsManager.get()
            }

            val currentParams = schemeParams ?: SchemeDetailsParamsManager.get()
            val uipid = currentParams?.userPurposeId ?: purpose
            platformLog("SchemeDetailsV2: 🚀 Calling loadTransactions with uipid: $uipid")
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
        platformLog("SchemeDetailsV2: 🔄 reloadData called - userId: $userId, purpose: $purpose")
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            val currentParams = schemeParams ?: SchemeDetailsParamsManager.get()
            val uipid = currentParams?.userPurposeId ?: purpose
            platformLog("SchemeDetailsV2: 🚀 Reloading transactions with uipid: $uipid")
            viewModel.loadTransactions(userId, uipid, currentParams)
        }
    }

    val handleAddFunds = { gid: String, isLumpsum: Boolean ->
        if (isLumpsum && state.folioNumber.isNullOrBlank()) {
            showFolioPendingDialog = true
        } else {
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
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (!state.isLoading) {
                Surface(
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButtonModuleV2(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.add_money),
                            icon = Icons.Default.Add,
                            containerColor = accentColor,
                            onClick = { handleAddFunds(purpose, true) }
                        )
                        ActionButtonModuleV2(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.new_plan),
                            icon = Icons.Default.FlashOn,
                            containerColor = accentColor,
                            onClick = { handleAddFunds(purpose, false) }
                        )
                        ActionButtonModuleV2(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.withdraw),
                            icon = if ((state.instantRedemptionValue ?: 0.0) > 0.0) Icons.Default.Bolt else Icons.Default.CallReceived,
                            containerColor = accentColor,
                            onClick = {
                                if (state.currentValue > 0) {
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
                                } else {
                                    showInvestmentInProgressDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
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
                    onAddFunds = { /* Moved to bottomBar */ },
                    onLumpsum = { /* Moved to bottomBar */ },
                    onWithdraw = {
                        if (state.currentValue > 0) {
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
                        } else {
                            showInvestmentInProgressDialog = true
                        }
                    },
                    onFundDetails = {
                        state.isin?.let { isin ->
                            onNavigateToFundDetails(isin, userId, purpose, 0.0, "", "", false)
                        }
                    },
                    onShowTotalValueInfo = { showTotalValueInfoPopup = true },
                    onShowGoldInfo = { showEstimatedGoldInfoPopup = true },
                    onShowSilverInfo = { showEstimatedSilverInfoPopup = true }
                )
            }

            // Overlays & Sheets
            if (showPlansView) {
                PlansOverlay(
                    mandates = state.mandates,
                    isLoading = state.isLoading,
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
                    isLoading = cancelSipLoading,
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

            if (showInvestmentInProgressDialog) {
                AlertDialog(
                    onDismissRequest = { showInvestmentInProgressDialog = false },
                    title = { Text(stringResource(Res.string.investment_in_progress_title)) },
                    text = { Text(stringResource(Res.string.investment_in_progress_message)) },
                    confirmButton = {
                        TextButton(onClick = { showInvestmentInProgressDialog = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showFolioPendingDialog) {
                AlertDialog(
                    onDismissRequest = { showFolioPendingDialog = false },
                    title = { Text(stringResource(Res.string.investment_in_progress_title)) },
                    text = { Text(stringResource(Res.string.folio_pending_message)) },
                    confirmButton = {
                        TextButton(onClick = { showFolioPendingDialog = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showTotalValueInfoPopup) {
                AlertDialog(
                    onDismissRequest = { showTotalValueInfoPopup = false },
                    title = { Text(stringResource(Res.string.total_value_label)) },
                    text = { Text(stringResource(Res.string.total_value_info_popup)) },
                    confirmButton = {
                        TextButton(onClick = { showTotalValueInfoPopup = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showEstimatedGoldInfoPopup) {
                AlertDialog(
                    onDismissRequest = { showEstimatedGoldInfoPopup = false },
                    title = { Text(stringResource(Res.string.estimated_gold)) },
                    text = {
                        Column {
                            Text(stringResource(Res.string.estimated_gold_info_popup_body))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.for_representational_purposes_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEstimatedGoldInfoPopup = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showEstimatedSilverInfoPopup) {
                AlertDialog(
                    onDismissRequest = { showEstimatedSilverInfoPopup = false },
                    title = { Text(stringResource(Res.string.estimated_silver)) },
                    text = {
                        Column {
                            Text(stringResource(Res.string.estimated_silver_info_popup_body))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.for_representational_purposes_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEstimatedSilverInfoPopup = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
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
    onFundDetails: () -> Unit,
    onShowTotalValueInfo: () -> Unit,
    onShowGoldInfo: () -> Unit,
    onShowSilverInfo: () -> Unit
) {
    val gradient = getGradientForCategory(goalType)
    val debouncedFundDetails = rememberDebouncedClick(onClick = onFundDetails)
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(25.dp))
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.linearGradient(
                    colors = gradient,
                    start = Offset(0f, 0f),
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                ))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                        }
                         Text(
                            text = displayGoalName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                        
                        if ((state.instantRedemptionValue ?: 0.0) > 0.0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = V2Cream,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, V2SuccessGreen.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.instant_redeem),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { debouncedFundDetails() }
                            .padding(horizontal = 8.dp)
                    ) {
                        val fundLogo = getFundLogo(state.schemeName ?: displaySchemeName)
                        Image(
                            painter = painterResource(fundLogo),
                            contentDescription = "Fund Logo",
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                val title = when (goalType) {
                    GoalType.GOLD -> "Your Gold"
                    GoalType.SILVER -> "Your Silver"
                    else -> "Your Savings"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            when (goalType) {
                                GoalType.GOLD -> onShowGoldInfo()
                                GoalType.SILVER -> onShowSilverInfo()
                                else -> onShowTotalValueInfo()
                            }
                        },
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                val unitsText = when (goalType) {
                    GoalType.GOLD, GoalType.SILVER -> {
                        val units = state.unitsInGm ?: 0.0
                        if (units < 1.0) "${(units * 1000).toInt()}${stringResource(Res.string.mg_label)}" else "${formatDecimal(units, 1)}${stringResource(Res.string.g_label)}"
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
                    if (state.investmentInProgress > 0) {
                        StatusPill(
                            text = "₹${formatIndian(state.investmentInProgress)} processing",
                            backgroundColor = Color.White.copy(alpha = 0.4f),
                            contentColor = Color.Black,
                            icon = Icons.Default.Schedule
                        )
                    }
                }
                if (state.investmentInProgress > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = "Units are typically allocated within 2 business days",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            ),
                            color = Color.Black.copy(alpha = 0.6f)
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
                    .size(130.dp)
                    .padding(end = 20.dp, bottom = 16.dp)
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
                    subtext = "Total money in"
                )
                SummaryBox(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL VALUE",
                    value = state.cummulativeValue,
                    gain = state.totalGain,
                    subtext = "Current value"
                )
            }

            val activeMandates = state.mandates.filter { 
                val s = it.status?.uppercase().orEmpty()
                (s.contains("ACTIVE") || s.contains("APPROVED")) && !s.contains("PAUSED")
            }
            val nextSipDateStr = activeMandates.firstOrNull { !it.nextSipDate.isNullOrBlank() }?.nextSipDate
            val hasApprovedPlan = activeMandates.isNotEmpty()
            val showFirstSaveDate = hasApprovedPlan && state.investedAmount == 0.0 && state.cummulativeValue == 0.0 && !nextSipDateStr.isNullOrBlank()

            if (state.redemptionInProgress > 0 || showFirstSaveDate) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.in_motion),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color.Gray.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (showFirstSaveDate) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.first_save_date_message, formatDate(nextSipDateStr!!)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.withdrawal_in_progress_title),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = stringResource(Res.string.will_be_credited_in_days_approx),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "₹${formatIndian(state.redemptionInProgress)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dashboard Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.plans),
                    description = stringResource(Res.string.view_and_manage_plans_description),
                    icon = Icons.Default.FlashOn,
                    iconColor = accentColor,
                    onClick = onShowPlans
                )
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.transactions),
                    description = stringResource(Res.string.track_investments_withdrawals_description),
                    icon = Icons.Default.Schedule,
                    iconColor = accentColor,
                    onClick = onShowTransactions
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.view_details),
                    description = stringResource(Res.string.see_goal_details_description),
                    icon = Icons.Default.Info,
                    iconColor = accentColor,
                    onClick = onShowDetails
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
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
    val debouncedClick = rememberDebouncedClick(onClick = onClick)
    Surface(
        modifier = modifier.height(110.dp).clickable(onClick = debouncedClick),
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
    val debouncedClick = rememberDebouncedClick(onClick = onClick)
    Surface(
        modifier = modifier.height(64.dp).clickable(onClick = debouncedClick),
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansOverlay(
    mandates: List<MandateDisplayItem>,
    isLoading: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPause: (MandateDisplayItem) -> Unit,
    onResume: (MandateDisplayItem) -> Unit,
    onCancel: (MandateDisplayItem) -> Unit
) {
    val sortedMandates = mandates.sortedByDescending { mandateSortDateMillis(it) }

    val approvedMandates = sortedMandates.filter { m ->
        val s = m.status?.uppercase().orEmpty()
        (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
    }
    val pausedMandates = sortedMandates.filter { m ->
        m.status?.uppercase()?.contains("PAUSED") == true
    }
    val otherMandates = sortedMandates.filter { m ->
        val s = m.status?.uppercase().orEmpty()
        !((s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")) && !s.contains("PAUSED") && !s.contains("INITIATED")
    }


    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.plans), fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF8F9FA)
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (mandates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.no_plans_found), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        SipPlansSummaryStrip(
                            mandates = mandates,
                            goalColor = accentColor,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (pausedMandates.isNotEmpty()) {
                        item { Text(stringResource(Res.string.state_paused), style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp)) }
                        items(pausedMandates) { mandate ->
                            MandateItemV2(mandate, accentColor, onPause, onResume, onCancel)
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    if (approvedMandates.isNotEmpty()) {
                        item { Text(stringResource(Res.string.state_approved), style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp)) }
                        items(approvedMandates) { mandate ->
                            MandateItemV2(mandate, accentColor, onPause, onResume, onCancel)
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                    if (otherMandates.isNotEmpty()) {
                        item { Text(stringResource(Res.string.state_other), style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp)) }
                        items(otherMandates) { mandate ->
                            MandateItemV2(mandate, accentColor, onPause, onResume, onCancel)
                        }
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
    val statusUpper = mandate.status?.uppercase().orEmpty()
    val isApproved = statusUpper.contains("APPROVED") || statusUpper.contains("ACTIVE")
    val isPaused = statusUpper.contains("PAUSED")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "₹${formatIndian(mandate.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    val displayStatus = when {
                        statusUpper.contains("ACTIVE") -> stringResource(Res.string.status_active)
                        statusUpper.contains("APPROVED") -> stringResource(Res.string.status_approved)
                        statusUpper.contains("PAUSED") -> stringResource(Res.string.status_paused)
                        statusUpper.contains("PENDING") -> stringResource(Res.string.status_pending)
                        statusUpper.contains("CANCELLED") -> stringResource(Res.string.status_cancelled)
                        statusUpper.contains("REJECTED") -> stringResource(Res.string.status_rejected)
                        statusUpper.contains("FAILED") -> stringResource(Res.string.status_failed)
                        else -> statusUpper.replace("_", " ")
                    }

                    val badgeColor = when {
                        statusUpper.contains("ACTIVE") -> Color(0xFFE3F2FD)
                        statusUpper.contains("APPROVED") -> Color(0xFFE8F5E9)
                        statusUpper.contains("PAUSED") || statusUpper.contains("PENDING") -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                    val badgeTextColor = when {
                        statusUpper.contains("ACTIVE") -> Color(0xFF1976D2)
                        statusUpper.contains("APPROVED") -> Color(0xFF2E7D32)
                        statusUpper.contains("PAUSED") || statusUpper.contains("PENDING") -> Color(0xFFF57C00)
                        else -> Color(0xFFD32F2F)
                    }

                    Surface(color = badgeColor, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = displayStatus,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(text = "Daily Saving Plan", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            if (isApproved || isPaused) {
                val debouncedCancel = rememberDebouncedClick { onCancel(mandate) }
                val debouncedPauseResume = rememberDebouncedClick { if (isPaused) onResume(mandate) else onPause(mandate) }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = debouncedCancel) {
                        Text("CANCEL", style = MaterialTheme.typography.labelMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = debouncedPauseResume) {
                        Text(if (isPaused) "RESUME" else "PAUSE", style = MaterialTheme.typography.labelMedium, color = accentColor, fontWeight = FontWeight.Bold)
                    }
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions) { tx ->
                        TransactionItemV2(transaction = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemV2(transaction: TransactionDisplayItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                verticalAlignment = Alignment.Top
            ) {
                // Left: Buy/Sell on top, amount, then date below
                Column(modifier = Modifier.weight(1f)) {
                    val buyOrSell = when (transaction.transactionType?.uppercase()) {
                        "LUMP_SUM_PURCHASE" -> stringResource(Res.string.transaction_one_time)
                        "PURCHASE" -> stringResource(Res.string.transaction_buy)
                        else -> stringResource(Res.string.transaction_sell)
                    }
                    Text(
                        text = buyOrSell,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${formatIndian(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (transaction.isCredit) V2SuccessGreen else Color(0xFFF44336)
                    )
                    if (transaction.date != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                // Right: all state badges
                if (transaction.state != null) {
                    val stateUpper = transaction.state.uppercase()
                    val isPurchase = transaction.transactionType?.uppercase() == "PURCHASE" || transaction.transactionType?.uppercase() == "LUMP_SUM_PURCHASE"

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isPurchase) {
                            if (stateUpper == "SUCCESS" || stateUpper == "SUCCESSFUL") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFC8E6C9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.payment_completed),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.units_allotted),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.status_success),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else if (stateUpper == "SUBMITTED" || stateUpper == "IN_PROGRESS" || stateUpper == "ALLOCATING UNITS") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFC8E6C9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.payment_completed),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.allocating_units),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1976D2),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                val displayState = stateUpper.replace("_", " ")
                                val (bg, textColor) = when {
                                    stateUpper == "FAILED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                                    else -> Color(0xFFF5F5F5) to Color(0xFF616161)
                                }
                                Surface(
                                    color = bg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = displayState,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            val displayState = when {
                                transaction.transactionType?.uppercase() == "REDEMPTION" &&
                                        (stateUpper == "SUBMITTED" || stateUpper == "IN_PROGRESS") -> stringResource(Res.string.in_motion)
                                else -> stateUpper.replace("_", " ")
                            }
                            val (bg, textColor) = when {
                                stateUpper in listOf("SUCCESS", "SUCCESSFUL") -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                stateUpper in listOf("SUBMITTED", "IN_PROGRESS", "PENDING") -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
                                stateUpper == "FAILED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                                else -> Color(0xFFF5F5F5) to Color(0xFF616161)
                            }
                            Surface(
                                color = bg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = displayState,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
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
    val isGoldOrSilver = goalType == GoalType.GOLD || goalType == GoalType.SILVER
    val isEstimatedGold = goalType == GoalType.GOLD

    val accentColor = if (state.category?.uppercase() == "SILVER") Color.Black else getCorrelationColorForCategory(state.category, state.colorTheme)
    
    var showPopupGoldInfo by remember { mutableStateOf(false) }
    var showPopupSilverInfo by remember { mutableStateOf(false) }

    if (showPopupGoldInfo) {
        AlertDialog(
            onDismissRequest = { showPopupGoldInfo = false },
            title = { Text(stringResource(Res.string.estimated_gold)) },
            text = {
                Column {
                    Text(stringResource(Res.string.estimated_gold_info_popup_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.for_representational_purposes_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPopupGoldInfo = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    if (showPopupSilverInfo) {
        AlertDialog(
            onDismissRequest = { showPopupSilverInfo = false },
            title = { Text(stringResource(Res.string.estimated_silver)) },
            text = {
                Column {
                    Text(stringResource(Res.string.estimated_silver_info_popup_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.for_representational_purposes_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPopupSilverInfo = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8F9FA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (schemeName.isNotBlank()) schemeName else goalName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }

            val activeMandates = state.mandates.filter { m ->
                val s = m.status?.uppercase().orEmpty()
                (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
            }

            SchemeDetailsCardV2(
                schemeName = schemeName,
                goalName = goalName,
                unitsInGm = state.unitsInGm,
                category = state.category,
                colorTheme = state.colorTheme,
                folioNumber = state.folioNumber,
                investedAmount = state.investedAmount,
                totalUnitsAllotted = state.totalUnitsAllotted,
                totalValue = state.cummulativeValue,
                currentValue = state.currentValue,
                investmentInProgress = state.investmentInProgress,
                totalGain = state.totalGain,
                redemptionInProgress = state.redemptionInProgress,
                hasApprovedPlan = activeMandates.isNotEmpty(),
                showBottomSection = false,
                containerColor = Color.White,
                onEstimatedGoldInfoClick = { showPopupGoldInfo = true },
                onEstimatedSilverInfoClick = { showPopupSilverInfo = true }
            )

            Spacer(modifier = Modifier.height(24.dp))


            if (activeMandates.isNotEmpty() || state.investmentInProgress > 0) {
                Text(
                    text = stringResource(Res.string.whats_happening_section),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        var needDivider = false
                        if (activeMandates.isNotEmpty()) {
                            val totalDaily = activeMandates.sumOf { it.amount }
                            val freq = "daily"
                            val nextSipMandate = activeMandates.filter { !it.nextSipDate.isNullOrBlank() }.minByOrNull { it.nextSipDate!! }
                            val nextDeduction = if (nextSipMandate != null) {
                                stringResource(Res.string.next_deduction_date, nextSipMandate.nextSipDate!!)
                            } else {
                                stringResource(Res.string.next_deduction_pending)
                            }

                            WhatsHappeningRowV2(
                                title = stringResource(Res.string.saving_amount_freq, formatIndian(totalDaily), freq),
                                subtitle = nextDeduction,
                                badgeText = stringResource(Res.string.active_badge),
                                badgeBg = Color(0xFFE8F5E9),
                                badgeFg = Color(0xFF2E7D32)
                            )
                            needDivider = true
                        }

                        if (state.investmentInProgress > 0) {
                            if (needDivider) HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            val inProgTitle = when {
                                isEstimatedGold -> stringResource(Res.string.gold_being_allocated)
                                isGoldOrSilver -> stringResource(Res.string.silver_being_allocated)
                                else -> stringResource(Res.string.savings_being_allocated)
                            }
                            WhatsHappeningRowV2(
                                title = inProgTitle,
                                subtitle = stringResource(Res.string.allocation_processing_sub, formatIndian(state.investmentInProgress), stringResource(Res.string.units)),
                                badgeText = stringResource(Res.string.processing_badge),
                                badgeBg = Color(0xFFFFF3E0),
                                badgeFg = Color(0xFFF57C00)
                            )
                            needDivider = true
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.for_your_records_section),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!state.folioNumber.isNullOrBlank()) {
                        RecordRowV2(stringResource(Res.string.folio_no), state.folioNumber!!)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    }
                    
                    val allottedValue = when {
                        isGoldOrSilver && (state.unitsInGm ?: 0.0) > 0 -> {
                            val units = state.unitsInGm!!
                            if (units < 1.0) "${(units * 1000).toInt()}${stringResource(Res.string.mg_label)}" else "${formatDecimal(units, 2)}${stringResource(Res.string.g_label)}"
                        }
                        else -> "₹${formatIndian(state.cummulativeValue)}"
                    }
                    val sub = if (isGoldOrSilver) stringResource(if (isEstimatedGold) Res.string.internal_units_gold else Res.string.internal_units_silver) else stringResource(Res.string.internal_units_generic)
                    RecordRowV2(stringResource(Res.string.units_allotted), allottedValue, sub)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    RecordRowV2(stringResource(Res.string.total_value_label), "₹${formatIndian(state.cummulativeValue)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecordRowV2(stringResource(Res.string.current_value), "₹${formatIndian(state.currentValue)}")
                            RecordRowV2(stringResource(Res.string.investment_in_progress), "₹${formatIndian(state.investmentInProgress)}")
                            if (state.redemptionInProgress > 0) {
                                RecordRowV2(stringResource(Res.string.withdrawal_in_progress_amount).replace("₹%1\$s ", ""), "₹${formatIndian(state.redemptionInProgress)}")
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    if (state.totalGain != 0.0) {
                        val diffColor = if(state.totalGain >= 0) Color(0xFF2E7D32) else Color.Red
                        RecordRowV2(stringResource(Res.string.total_gain), "₹${formatIndian(state.totalGain)}", valueColor = diffColor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    }

                    RecordRowV2(stringResource(Res.string.can_i_take_it_out), stringResource(Res.string.yes_anytime))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Text(
                        text = stringResource(Res.string.sebi_mutual_fund_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
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
    containerColor: Color? = null,
    onEstimatedGoldInfoClick: () -> Unit = {},
    onEstimatedSilverInfoClick: () -> Unit = {}
) {
    val goalColor = getCorrelationColorForCategory(category, colorTheme)
    val finalContainerColor = containerColor ?: goalColor.copy(alpha = 0.1f)
    val goalType = identifyGoalType(category, schemeName)

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
            
            if (unitsInGm != null && unitsInGm > 0) {
                val label = when (goalType) {
                    GoalType.GOLD -> "Estimated Gold"
                    GoalType.SILVER -> "Estimated Silver"
                    else -> "Units Allotted"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$label: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    val unitsText = if (unitsInGm < 1.0) "${(unitsInGm * 1000).toInt()}${stringResource(Res.string.mg_label)}" else "${formatDecimal(unitsInGm, 1)}${stringResource(Res.string.g_label)}"
                    Text(unitsText, style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            if (goalType == GoalType.GOLD) onEstimatedGoldInfoClick()
                            else if (goalType == GoalType.SILVER) onEstimatedSilverInfoClick()
                        },
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                    }
                }
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
fun RecordRowV2(
    label: String,
    value: String,
    subtext: String? = null,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black
            )
            if (subtext != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End
        )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.6f)
            )
        }
        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeFg,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
    Dialog(onDismissRequest = onGoBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
}


@Composable
fun CancelSipReasonScreen(
    selectedReason: CancelSipReason?,
    isLoading: Boolean,
    onReasonSelected: (CancelSipReason) -> Unit,
    onContinue: () -> Unit,
    onGoBack: () -> Unit
) {
    Dialog(onDismissRequest = onGoBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                IconButton(onClick = onGoBack, enabled = !isLoading) { Icon(Icons.Default.ArrowBack, null) }
                Text("Why are you cancelling?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    CancelSipReason.values().forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { onReasonSelected(reason) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { onReasonSelected(reason) },
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(reason.label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val debouncedContinue = rememberDebouncedClick(onClick = onContinue)
                Button(onClick = debouncedContinue, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = selectedReason != null && !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Continue")
                    }
                }
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
    val debouncedConfirm = rememberDebouncedClick(onClick = onConfirm)
    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Pause Daily Saving?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("You can resume your savings at any time.", color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Go Back") }
                Button(onClick = debouncedConfirm, modifier = Modifier.weight(1f), enabled = !isLoading) {
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
    val debouncedConfirm = rememberDebouncedClick(onClick = onConfirm)
    ModalBottomSheet(onDismissRequest = onCancel) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Resume Daily Saving?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Start saving daily again towards your goal.", color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Go Back") }
                Button(onClick = debouncedConfirm, modifier = Modifier.weight(1f), enabled = !isLoading) {
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
    if (dateString.isNullOrBlank() || dateString == "null") return ""
    return try {
        val parts = dateString.substringBefore("T").split("-")
        if (parts.size < 3) return dateString
        val year = parts[0]
        val monthNum = parts[1].toInt()
        val day = parts[2].toInt()
        val monthName = when (monthNum) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }
        "$monthName $day, $year"
    } catch (e: Exception) {
        dateString
    }
}

private fun dayOfMonthOrdinal(dateString: String?): String? {
    if (dateString.isNullOrBlank() || dateString == "null") return null
    val parts = dateString.split("-")
    if (parts.size < 3) return null
    val dayPart = parts[2]
    val dayStr = if (dayPart.contains("T")) dayPart.substringBefore("T") else dayPart
    val day = dayStr.toIntOrNull() ?: return null
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

private fun mandateSortDateMillis(mandate: MandateDisplayItem): Long {
    val raw = mandate.mandateCreatedDate?.takeIf { it.isNotBlank() }
        ?: mandate.mandateApprovedDate?.takeIf { it.isNotBlank() }
        ?: return 0L
    return try {
        val cleanRaw = if (raw.contains("T")) {
            raw.substringBefore(".")
        } else {
            "${raw}T00:00:00"
        }
        val localDateTime = kotlinx.datetime.LocalDateTime.parse(cleanRaw)
        localDateTime.toInstant(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
    } catch (_: Exception) {
        0L
    }
}

@Composable
private fun SipPlansSummaryStrip(
    mandates: List<MandateDisplayItem>,
    goalColor: Color,
    modifier: Modifier = Modifier
) {
    val dailyMandates = mandates.filter { it.frequency?.uppercase() != "MONTHLY" }
    val monthlyMandates = mandates.filter { it.frequency?.uppercase() == "MONTHLY" }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SipFrequencySummaryRow(freqMandates = dailyMandates, isMonthly = false, goalColor = goalColor)
        SipFrequencySummaryRow(freqMandates = monthlyMandates, isMonthly = true, goalColor = goalColor)
    }
}

@Composable
private fun SipFrequencySummaryRow(
    freqMandates: List<MandateDisplayItem>,
    isMonthly: Boolean,
    goalColor: Color
) {
    if (freqMandates.isEmpty()) return

    val activePlans = freqMandates.filter { m ->
        val s = m.status?.uppercase().orEmpty()
        (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
    }
    val pausedPlans = freqMandates.filter { it.status?.uppercase()?.contains("PAUSED") == true }
    val activeTotal = activePlans.sumOf { it.amount }
    val pausedTotal = pausedPlans.sumOf { it.amount }
    val displayAmount = "₹${(if (activeTotal > 0) activeTotal else pausedTotal).toInt()}"

    val allPaused = activePlans.isEmpty() && pausedPlans.isNotEmpty()
    val statusText = when {
        allPaused -> "All Paused"
        pausedPlans.isNotEmpty() -> "${activePlans.size} Active · ${pausedPlans.size} Paused"
        else -> "${activePlans.size} Active"
    }
    val statusColor = if (allPaused) Color(0xFF7A5200) else Color(0xFF0B6B30)
    val statusBg = if (allPaused) Color(0xFFFFC850).copy(alpha = 0.35f) else Color(0xFF0B6B30).copy(alpha = 0.10f)

    val detailText = if (!isMonthly) {
        if (allPaused) "Daily cadence paused" else "Daily cadence active"
    } else {
        val activeDays = activePlans.mapNotNull { dayOfMonthOrdinal(it.nextSipDate) }.distinct()
        when {
            activeDays.isNotEmpty() -> "Monthly runs on ${activeDays.joinToString(" & ")}"
            else -> {
                val pausedDays = pausedPlans.mapNotNull { dayOfMonthOrdinal(it.nextSipDate) }.distinct()
                if (pausedDays.isNotEmpty()) "Paused runs on ${pausedDays.joinToString(" & ")}" else "Cadence: monthly"
            }
        }
    }

    val nextActive = activePlans
        .filter { !it.nextSipDate.isNullOrBlank() && it.nextSipDate != "null" }
        .minByOrNull { it.nextSipDate!! }
    val nextText = if (nextActive != null) {
        "Next: ${formatDate(nextActive.nextSipDate)}"
    } else {
        "No upcoming debit"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(goalColor.copy(alpha = 0.07f), RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (isMonthly) Icons.Default.DateRange else Icons.Default.FlashOn,
            contentDescription = null,
            tint = goalColor,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF3E2723)
                )
                Text(
                    text = if (isMonthly) "/ month" else "/ day",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF3E2723).copy(alpha = 0.6f)
                )
            }
            Text(
                text = detailText,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF3E2723).copy(alpha = 0.5f)
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(color = statusBg, shape = RoundedCornerShape(50)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = nextText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFF3E2723).copy(alpha = 0.45f)
            )
        }
    }
}


fun getGradientForCategory(goalType: GoalType): List<Color> {
    return when (goalType) {
        GoalType.GOLD -> listOf(Color(0xFFFFF9E6), Color(0xFFFFE8B8))
        GoalType.SILVER -> listOf(Color(0xFFF5F5F5), Color(0xFFE8E8E8))
        GoalType.SAVINGS, GoalType.SAVINGS_PLUS -> listOf(Color(0xFFE8F5E9), Color(0xFFA5D6A7))
        GoalType.FESTIVAL_SPENDS -> listOf(Color(0xFFFFF5F5), Color(0xFFFFD7B5))
        GoalType.CHILDRENS_EDUCATION -> listOf(Color(0xFFF5F9FF), Color(0xFFEBF3FF))
        GoalType.VACATION -> listOf(Color(0xFFFDF5FF), Color(0xFFF8EBFF))
        GoalType.GLOBAL_EXPOSURE -> listOf(Color(0xFFE0F2F1), Color(0xFFF3E5F5))
        GoalType.ALL_IN_ONE -> listOf(Color(0xFFE8EAF6), Color(0xFF9FA8DA))
        else -> listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))
    }
}
