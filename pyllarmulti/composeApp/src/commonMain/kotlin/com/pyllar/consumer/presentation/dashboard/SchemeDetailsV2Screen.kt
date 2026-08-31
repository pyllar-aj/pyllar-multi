package com.pyllar.consumer.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.pyllar.consumer.presentation.ui.theme.V2Cream
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
import pyllar.composeapp.generated.resources.*
import kotlin.math.abs
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailsV2Screen(
    userId: String = "",
    purpose: String = "",
    onNavigateBack: () -> Unit = {},
    onNavigateToWithdraw: (WithdrawInitParams) -> Unit = {},
    onNavigateToAddFunds: (userId: String, kycAttemptId: String, investorId: String, goalId: String, isExistingInvestment: Boolean, kycStatus: String) -> Unit = { _, _, _, _, _, _ -> },
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
    var selectedCancelReason by remember { mutableStateOf<CancelSipReasonV2?>(null) }
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
    var showWithdrawalNotAvailableDialog by remember { mutableStateOf(false) }
    var showFolioPendingDialog by remember { mutableStateOf(false) }
    var showNewPlanPendingDialog by remember { mutableStateOf(false) }

    var schemeParams by remember { mutableStateOf<SchemeDetailsParams?>(SchemeDetailsParamsManager.get()) }
    
    val displaySchemeName = schemeParams?.schemeName?.takeIf { it.isNotBlank() } ?: state.schemeName.orEmpty()
    val displayGoalName = schemeParams?.goalName?.takeIf { it.isNotBlank() } ?: state.goalName.orEmpty()
    val displayCategory = schemeParams?.category ?: state.category
    val displayColorTheme = schemeParams?.colorTheme ?: state.colorTheme
    val goalColor = getCorrelationColorForCategory(displayCategory, displayColorTheme)
    val categoryUpper = displayCategory?.uppercase().orEmpty()

    val goalType = identifyGoalType(displayCategory, displaySchemeName)
    val accentColor = if (categoryUpper == "SILVER") Color.Black else getCorrelationColorForCategory(displayCategory, displayColorTheme)

    // Premium color system based on goal category
    val (contentColor, secondaryContentColor, pillBgColor, scaffoldBgColor) = when {
        categoryUpper == "GOLD" || categoryUpper == "SILVER" -> {
            // Light-theme header cards (dark texts, transparent button style, etc.)
            val primaryContentColor = if (categoryUpper == "GOLD") Color(0xFF381E00) else Color(0xFF2C343A)
            val secondaryContentColor = if (categoryUpper == "GOLD") Color(0xFF6B5120) else Color(0xFF5F6972)
            listOf(primaryContentColor, secondaryContentColor, Color.Black.copy(alpha = 0.06f), Color.White)
        }
        categoryUpper == "SAVINGS" || categoryUpper == "SAVINGS_PLUS" -> {
            listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.White)
        }
        categoryUpper == "GLOBAL_EXPOSURE" || categoryUpper == "GLOBAL" -> {
            listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.White)
        }
        categoryUpper == "INNOVATION" || categoryUpper == "SENSEX" -> {
            listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.White)
        }
        categoryUpper == "FESTIVAL_SPENDS" -> {
            listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.White)
        }
        categoryUpper == "CHILDRENS_EDUCATION" -> {
            listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.White)
        }
        categoryUpper == "VACATION" -> {
            listOf(Color.White, Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f), Color.White)
        }
        else -> {
            listOf(Color.White, Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.25f), Color.White)
        }
    }

    val isGoldOrSilver = displaySchemeName.contains("Gold", ignoreCase = true) ||
            displaySchemeName.contains("Silver", ignoreCase = true) ||
            displayGoalName.contains("Gold", ignoreCase = true) ||
            displayGoalName.contains("Silver", ignoreCase = true)
    val titleStr = when {
        displayGoalName.contains("Gold", ignoreCase = true) || displaySchemeName.contains("Gold", ignoreCase = true) -> stringResource(Res.string.your_gold)
        isGoldOrSilver -> stringResource(Res.string.your_silver)
        else -> stringResource(Res.string.your_savings)
    }

    val unitsVal = when {
        isGoldOrSilver && (state.unitsInGm ?: schemeParams?.unitsInGm ?: 0.0) > 0 -> {
            val u = state.unitsInGm ?: schemeParams?.unitsInGm ?: 0.0
            if (u < 1.0) {
                "${formatDecimal(u * 1000.0, 1)}${stringResource(Res.string.mg_label)}"
            } else {
                formatWeight(u, stringResource(Res.string.mg_label), stringResource(Res.string.g_label))
            }
        }
        else -> {
            formatRupeeAmount(state.cummulativeValue, 0)
        }
    }

    // Pick the earliest approved/active daily mandate for timeline calculation
    val activeMandate = remember(state.mandates) {
        state.mandates
            .filter { m ->
                val s = m.status?.uppercase().orEmpty()
                (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
            }
            .minByOrNull { m ->
                m.mandateCreatedDate ?: m.mandateApprovedDate ?: "9999-99-99"
            }
    }
    val hasApprovedPlan = activeMandate != null
    val nextSipDateStr = activeMandate?.nextSipDate
    val firstAllocationDateStr = activeMandate?.calculatedFirstUnitAllocationDate ?: activeMandate?.firstUnitAllocationDate
    val hasSuccessfulTransaction = state.transactions.any { txn ->
        val s = txn.state?.uppercase().orEmpty()
        s == "SUCCESS" || s == "ALLOTTED" || s == "COMPLETED" || txn.allottedUnits > 0
    }
    val isDailySip = activeMandate?.frequency?.uppercase() in listOf("DAILY", "DAY") || activeMandate?.frequency.isNullOrBlank()
    val mandateApprovedDate = remember(activeMandate) {
        try {
            val dStr = activeMandate?.mandateApprovedDate ?: activeMandate?.mandateCreatedDate ?: activeMandate?.firstDebitDate
            if (!dStr.isNullOrBlank() && dStr != "null") {
                val cleanVal = dStr.substringBefore("T").substringBefore(" ")
                kotlinx.datetime.LocalDate.parse(cleanVal)
            } else null
        } catch (e: Exception) { null }
    }
    val isRecentPlanSetup = remember(mandateApprovedDate, state.transactions) {
        if (mandateApprovedDate != null) {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            (today.toEpochDays() - mandateApprovedDate.toEpochDays()) <= 10
        } else {
            true
        }
    }
    // Only display for the user's FIRST plan ever created for this goal (not for subsequent/second plans)
    val isFirstPlanEverForGoal = remember(state.mandates, activeMandate) {
        val approvedMandates = state.mandates.filter { m ->
            val s = m.status?.uppercase().orEmpty()
            s.contains("APPROVED") || s.contains("ACTIVE") || s.contains("COMPLETED") || s.contains("CLOSED") || s.contains("CANCELLED")
        }.sortedBy { m ->
            m.mandateCreatedDate ?: m.mandateApprovedDate ?: "9999-99-99"
        }
        // Is this active mandate the very first approved mandate created for this goal?
        approvedMandates.size <= 1 || approvedMandates.firstOrNull()?.mandateId == activeMandate?.mandateId
    }
    val isFirstInvestmentInProgress = isDailySip && hasApprovedPlan && isRecentPlanSetup && isFirstPlanEverForGoal
    val showFirstSaveDate = false

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("SchemeDetailsV2")
    }

    LaunchedEffect(userId, purpose) {
        platformLog("SchemeDetailsV2: LaunchedEffect initial load - userId: $userId, purpose: $purpose")
        if (userId.isNotBlank() && purpose.isNotBlank()) {
            if (SchemeDetailsParamsManager.get() == null) {
                val stored = sessionStore.getValue("scheme_details_params_$purpose")
                val restored = SchemeDetailsParamsManager.fromJson(stored)
                if (restored != null) {
                    SchemeDetailsParamsManager.set(restored)
                    schemeParams = restored
                }
            } else if (schemeParams == null) {
                schemeParams = SchemeDetailsParamsManager.get()
            }

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
            val uipid = currentParams?.userPurposeId ?: purpose
            viewModel.loadTransactions(userId, uipid, currentParams)
        }
    }

    val handleLumpsumClick: () -> Unit = {
        if (state.currentValue > 0 || !isDailySip) {
            val hasActualInvestment = state.investedAmount > 0 || !state.folioNumber.isNullOrBlank()
            if (hasActualInvestment && state.folioNumber.isNullOrBlank()) {
                showFolioPendingDialog = true
            } else {
                scope.launch {
                    try {
                        val result = dashboardViewModel.initGoalTxn(userId, purpose)
                        if (result is Resource.Success) {
                            result.data?.let { response ->
                                if (response.userPurposeId.isNotBlank()) {
                                    sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                                }
                            }
                        }
                        sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, purpose)
                        sessionStore.saveValue("isExistingInvestment", hasActualInvestment.toString())
                        val kycAttemptId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                        val investorId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""
                        onNavigateToLumpsum(userId, kycAttemptId, investorId, purpose, hasActualInvestment)
                    } catch (e: Exception) {
                        platformLog("Error: ${e.message}")
                    }
                }
            }
        } else {
            showFolioPendingDialog = true
        }
    }

    val handleSipClick: () -> Unit = {
        val hasFolio = !state.folioNumber.isNullOrBlank() && state.folioNumber != "null"
        val hasPendingInvestmentOrSip = state.investmentInProgress > 0 || hasApprovedPlan
        if (!hasFolio && hasPendingInvestmentOrSip && isDailySip) {
            showNewPlanPendingDialog = true
        } else {
            scope.launch {
                try {
                    val result = dashboardViewModel.initGoalTxn(userId, purpose)
                    if (result is Resource.Success) {
                        result.data?.let { response ->
                            if (response.userPurposeId.isNotBlank()) {
                                sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                            }
                        }
                    }
                    sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, purpose)
                    val hasActualInvestment = state.investedAmount > 0 || !state.folioNumber.isNullOrBlank()
                    sessionStore.saveValue("isExistingInvestment", hasActualInvestment.toString())
                    val kycAttemptId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                    val investorId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""
                    onNavigateToAddFunds(userId, kycAttemptId, investorId, purpose, hasActualInvestment, "SUCCESS")
                } catch (e: Exception) {
                    platformLog("Error: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        containerColor = scaffoldBgColor,
        bottomBar = {
            val inOverlayFlow = showCancelReasonScreen ||
                    showCancelSipScreen ||
                    showCancelSipSuccessSheet ||
                    showCancelSipErrorSheet ||
                    showPauseSipQuestionSheet ||
                    showPauseSipSuccessSheet ||
                    showPauseSipErrorSheet ||
                    showResumeSipQuestionSheet ||
                    showResumeSipSuccessSheet ||
                    showResumeSipErrorSheet

            if (!inOverlayFlow && !state.isLoading && !(state.errorMessage != null && schemeParams == null)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButtonModuleV2(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.add_money),
                                icon = Icons.Default.Add,
                                containerColor = goalColor,
                                onClick = handleLumpsumClick
                            )
                            ActionButtonModuleV2(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.new_plan),
                                icon = Icons.Default.FlashOn,
                                containerColor = goalColor,
                                onClick = handleSipClick
                            )
                            ActionButtonModuleV2(
                                modifier = Modifier.weight(1f),
                                text = stringResource(Res.string.withdraw),
                                icon = Icons.Default.CallReceived,
                                containerColor = goalColor,
                                onClick = {
                                    if (state.currentValue > 0) {
                                        if (state.redeemableAmount <= 0.0 && state.redemptionInProgress == 0.0) {
                                            showWithdrawalNotAvailableDialog = true
                                        } else {
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
                                                instantRedemptionValue = instantVal,
                                                unitsInGm = state.unitsInGm ?: schemeParams?.unitsInGm
                                            )
                                            WithdrawParamsManager.set(params)
                                            scope.launch {
                                                sessionStore.saveValue("withdraw_init_params", WithdrawParamsManager.toJson(params))
                                            }
                                            onNavigateToWithdraw(params)
                                        }
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
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = if (categoryUpper.isNotEmpty()) 12.dp else 8.dp,
                color = Color.White
            ) {
                Column {
                    val headerBackground = getGradientForCategory(displayCategory, displayColorTheme)
                    val headerContentColor = contentColor

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = headerBackground)
                    ) {
                        if (categoryUpper.isNotEmpty() && !(showPlansView || showTransactionsView)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.08f), Color.Transparent)
                                        )
                                    )
                            )
                        }
                        Column(modifier = Modifier.statusBarsPadding()) {
                            Spacer(modifier = Modifier.height(1.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val inOverlayFlow = showCancelReasonScreen ||
                                            showCancelSipScreen ||
                                            showCancelSipSuccessSheet ||
                                            showCancelSipErrorSheet ||
                                            showPauseSipQuestionSheet ||
                                            showPauseSipSuccessSheet ||
                                            showPauseSipErrorSheet ||
                                            showResumeSipQuestionSheet ||
                                            showResumeSipSuccessSheet ||
                                            showResumeSipErrorSheet
                                    if (inOverlayFlow) {
                                        when {
                                            showCancelReasonScreen -> { showCancelReasonScreen = false; showCancelSipScreen = true }
                                            showCancelSipScreen -> { showCancelSipScreen = false; mandateForCancelSip = null }
                                            showCancelSipSuccessSheet -> { showCancelSipSuccessSheet = false; reloadData() }
                                            showCancelSipErrorSheet -> showCancelSipErrorSheet = false
                                            showPauseSipQuestionSheet -> { showPauseSipQuestionSheet = false; mandateForPauseSip = null }
                                            showPauseSipSuccessSheet -> { showPauseSipSuccessSheet = false; reloadData() }
                                            showPauseSipErrorSheet -> showPauseSipErrorSheet = false
                                            showResumeSipQuestionSheet -> { showResumeSipQuestionSheet = false; mandateForResumeSip = null }
                                            showResumeSipSuccessSheet -> { showResumeSipSuccessSheet = false; reloadData() }
                                            showResumeSipErrorSheet -> showResumeSipErrorSheet = false
                                        }
                                    } else if (showPlansView || showTransactionsView) {
                                        showPlansView = false
                                        showTransactionsView = false
                                    } else {
                                        onNavigateBack()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = headerContentColor
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    if (displayGoalName.isNotBlank()) {
                                        Text(
                                            text = displayGoalName.uppercase(),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.5.sp
                                            ),
                                            color = headerContentColor
                                        )
                                    }
                                }

                                val fundLogo = getFundLogo(displaySchemeName)
                                Image(
                                    painter = painterResource(fundLogo),
                                    contentDescription = "Fund Logo",
                                    modifier = Modifier
                                        .height(if (displaySchemeName.contains("aditya", ignoreCase = true)) 30.dp else 44.dp)
                                        .clickable {
                                            state.isin?.let { isin ->
                                                onNavigateToFundDetails(isin, userId, purpose, 0.0, "", "", false)
                                            }
                                        }
                                        .padding(end = 16.dp),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }

                            if (!(showPlansView || showTransactionsView)) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp)
                                        .padding(top = 2.dp)
                                        .padding(bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = titleStr,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                                    color = secondaryContentColor
                                                )
                                                if (isGoldOrSilver) {
                                                    IconButton(
                                                        onClick = {
                                                            if (displaySchemeName.contains("Gold", ignoreCase = true) || displayGoalName.contains("Gold", ignoreCase = true)) {
                                                                showEstimatedGoldInfoPopup = true
                                                            } else {
                                                                showEstimatedSilverInfoPopup = true
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = secondaryContentColor.copy(alpha = 0.4f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (categoryUpper.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .offset(x = (-20).dp)
                                                            .size(width = 180.dp, height = 80.dp)
                                                            .background(
                                                                Brush.radialGradient(
                                                                    colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                                                                    radius = 300f
                                                                )
                                                            )
                                                    )
                                                }
                                                Text(
                                                    text = buildAnnotatedString {
                                                        val parts = unitsVal.split(" ")
                                                        withStyle(SpanStyle(fontSize = 52.sp, fontWeight = FontWeight.Black)) {
                                                            append(parts[0])
                                                        }
                                                        if (parts.size > 1) {
                                                            withStyle(SpanStyle(fontSize = 52.sp, fontWeight = FontWeight.Black)) {
                                                                append(" " + parts.subList(1, parts.size).joinToString(" "))
                                                            }
                                                        }
                                                    },
                                                    color = contentColor
                                                )
                                            }
                                        }

                                        val lockerIcon = when (categoryUpper) {
                                            "GOLD" -> Res.drawable.gold_locker
                                            "SILVER" -> Res.drawable.silver_locker
                                            else -> Res.drawable.savings_locker
                                        }
                                        Image(
                                            painter = painterResource(lockerIcon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .padding(end = 4.dp)
                                                .alpha(0.85f)
                                        )
                                    }
                                }

                                if (state.investmentInProgress > 0) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    ) {
                                        StatusPill(
                                            text = stringResource(Res.string.investment_in_progress_main, formatIndian(state.investmentInProgress)),
                                            backgroundColor = pillBgColor,
                                            contentColor = contentColor,
                                            icon = Icons.Default.Check
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    if (!(showPlansView || showTransactionsView)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.current_value),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = goalColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = formatRupeeAmount(state.currentValue, 0),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .width(1.dp)
                                    .background(goalColor.copy(alpha = 0.15f))
                            )

                            Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                                Text(
                                    text = "In progress",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = goalColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = formatRupeeAmount(state.investmentInProgress, 0),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            } else if (state.errorMessage != null && schemeParams == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.errorMessage ?: stringResource(Res.string.an_error_occurred),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        if (userId.isNotBlank() && purpose.isNotBlank()) {
                            val latestParams = schemeParams ?: SchemeDetailsParamsManager.get()
                            val uipid = latestParams?.userPurposeId ?: purpose
                            viewModel.loadTransactions(userId, uipid, latestParams)
                        }
                    }) {
                        Text(stringResource(Res.string.retry))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (showPlansView) Color(0xFFFBF9F4) else Color.Transparent)
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 10.dp, end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (showPlansView || showTransactionsView) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showPlansView) stringResource(Res.string.your_plans) else stringResource(Res.string.transactions),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            if (showPlansView) {
                                if (state.mandates.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(Res.string.no_plans_found),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    item {
                                        SipPlansSummaryStrip(
                                            mandates = state.mandates,
                                            goalColor = goalColor,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }

                                    fun planStatusRank(m: MandateDisplayItem): Int {
                                        val s = m.status?.uppercase().orEmpty()
                                        return when {
                                            s.contains("PAUSED") -> 1
                                            (s.contains("APPROVED") || s.contains("ACTIVE")) -> 0
                                            else -> 2
                                        }
                                    }

                                    val sortedMandates = state.mandates.sortedWith(
                                        compareBy<MandateDisplayItem> { planStatusRank(it) }
                                            .thenByDescending { mandateSortDateMillis(it) }
                                    )

                                    itemsIndexed(sortedMandates) { index, mandate ->
                                        val rank = planStatusRank(mandate)
                                        SipPlanCardV2(
                                            mandate = mandate,
                                            planNumber = index + 1,
                                            goalColor = goalColor,
                                            category = categoryUpper,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            onPause = if (rank == 0) { { mandateForPauseSip = it; showPauseSipQuestionSheet = true } } else null,
                                            onResume = if (rank == 1) { { mandateForResumeSip = it; showResumeSipQuestionSheet = true } } else null,
                                            onCancel = if (rank == 0 || rank == 1) { { mandateForCancelSip = it; showCancelSipScreen = true } } else null
                                        )
                                    }

                                    item {
                                        SipPlansTrustStripV2(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            } else {
                                if (state.transactions.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(Res.string.no_transactions_found),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    items(state.transactions) { transaction ->
                                        TransactionItemV2(transaction = transaction)
                                    }
                                }
                            }
                        } else {
                            if (state.redemptionInProgress > 0 || showFirstSaveDate || isFirstInvestmentInProgress) {
                                item {
                                    val mandateForTimeline = activeMandate
                                    val firstDebitStr = mandateForTimeline?.firstDebitDate ?: mandateForTimeline?.mandateApprovedDate
                                    val nextSipStr = mandateForTimeline?.nextSipDate
                                    val firstAllocationDateStr = mandateForTimeline?.calculatedFirstUnitAllocationDate ?: mandateForTimeline?.firstUnitAllocationDate
                                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                                    val firstInvestmentTxnDate = remember(state.transactions) {
                                        state.transactions.filter { txn ->
                                            val s = txn.state?.uppercase().orEmpty()
                                            s in listOf("SUBMITTED", "IN_PROGRESS", "ALLOCATING UNITS", "DEBITED", "PROCESSING", "SUCCESS", "ALLOTTED", "COMPLETED", "FAILED", "CANCELLED", "REJECTED")
                                        }.minByOrNull { txn ->
                                            (txn.sortDate ?: txn.date)?.trim() ?: "9999-99-99"
                                        }?.let { txn ->
                                            val raw = (txn.sortDate ?: txn.date)?.trim()
                                            raw?.let { cleanVal ->
                                                try {
                                                    val isoPart = cleanVal.substringBefore("T").substringBefore(" ")
                                                    if (isoPart.contains("-")) {
                                                        val parts = isoPart.split("-")
                                                        if (parts.size == 3) {
                                                            return@let LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                                                        }
                                                    }
                                                    LocalDate.parse(isoPart)
                                                } catch (e: Exception) { null }
                                            }
                                        }
                                    }

                                    val day1Date = remember(firstDebitStr, nextSipStr, firstInvestmentTxnDate) {
                                        try {
                                            if (!firstDebitStr.isNullOrBlank() && firstDebitStr != "null") {
                                                val cleanVal = firstDebitStr.substringBefore("T").substringBefore(" ")
                                                LocalDate.parse(cleanVal)
                                            } else if (firstInvestmentTxnDate != null) {
                                                firstInvestmentTxnDate
                                            } else if (!nextSipStr.isNullOrBlank() && nextSipStr != "null") {
                                                val cleanVal = nextSipStr.substringBefore("T").substringBefore(" ")
                                                val parsedNext = LocalDate.parse(cleanVal)
                                                getPreviousBusinessDay(parsedNext, 1)
                                            } else {
                                                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                            }
                                        } catch (e: Exception) {
                                            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                        }
                                    }

                                    val day2Date = remember(day1Date) { getNextBusinessDay(day1Date, 1) }
                                    val day3Date = remember(day2Date) { getNextBusinessDay(day2Date, 1) }
                                    val day4Date = remember(day1Date, firstAllocationDateStr) {
                                        try {
                                            if (!firstAllocationDateStr.isNullOrBlank() && firstAllocationDateStr != "null") {
                                                LocalDate.parse(firstAllocationDateStr.substringBefore("T"))
                                            } else {
                                                getNextBusinessDay(day1Date, 3)
                                            }
                                        } catch (e: Exception) {
                                            getNextBusinessDay(day1Date, 3)
                                        }
                                    }
                                    val day5Date = remember(day4Date) { getNextBusinessDay(day4Date, 1) }

                                    val validStates = setOf("SUBMITTED", "IN_PROGRESS", "ALLOCATING UNITS", "DEBITED", "PROCESSING", "SUCCESS", "ALLOTTED", "COMPLETED")
                                    val hasAnyDebit = state.transactions.any { txn ->
                                        val s = txn.state?.uppercase().orEmpty()
                                        s in validStates
                                    } || state.investmentInProgress > 0.0 || state.investedAmount > 0.0
                                    val hasFolio = !state.folioNumber.isNullOrBlank()

                                    val step1Done = true
                                    val step2Done = hasAnyDebit
                                    val step3Done = step2Done && today >= day3Date
                                    val step4Done = step3Done && hasFolio
                                    val step5Done = step4Done && today >= day5Date
                                    val stepTimelineActive = !(step5Done && today > day5Date)

                                    val shouldShowInMotionCard = state.redemptionInProgress > 0 || showFirstSaveDate || (isFirstInvestmentInProgress && stepTimelineActive)

                                    if (shouldShowInMotionCard) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isFirstInvestmentInProgress && stepTimelineActive) goalColor.copy(alpha = 0.08f) else Color.White
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                                        ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = stringResource(Res.string.in_motion),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = Color.Gray.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))

                                            if (state.redemptionInProgress > 0) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(goalColor.copy(alpha = 0.1f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Schedule,
                                                            contentDescription = null,
                                                            tint = goalColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = stringResource(Res.string.withdrawal_in_progress_title),
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                                        )
                                                        Text(
                                                            text = stringResource(Res.string.will_be_credited_in_days_approx),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    RupeeAmountBlock(
                                                        value = state.redemptionInProgress,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.Black
                                                    )
                                                }
                                            }

                                            if (state.redemptionInProgress > 0 && (showFirstSaveDate || isFirstInvestmentInProgress)) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }

                                             if (isFirstInvestmentInProgress) {
                                                // Dynamic SIP Allotment Timeline
                                                val mandateForTimeline = activeMandate
                                                val sipAmount = mandateForTimeline?.amount ?: (state.investmentInProgress.takeIf { it > 0 } ?: 100.0)
                                                val dailyAmountStr = formatRupeeAmount(sipAmount, 0)
                                                
                                                val firstDebitStr = mandateForTimeline?.firstDebitDate ?: mandateForTimeline?.mandateApprovedDate
                                                val nextSipStr = mandateForTimeline?.nextSipDate
                                                
                                                val firstInvestmentTxnDate = state.transactions.filter { txn ->
                                                    val s = txn.state?.uppercase().orEmpty()
                                                    s in listOf("SUBMITTED", "IN_PROGRESS", "ALLOCATING UNITS", "DEBITED", "PROCESSING", "SUCCESS", "ALLOTTED", "COMPLETED", "FAILED", "CANCELLED", "REJECTED")
                                                }.minByOrNull { txn ->
                                                    (txn.sortDate ?: txn.date)?.trim() ?: "9999-99-99"
                                                }?.let { txn ->
                                                    val raw = (txn.sortDate ?: txn.date)?.trim()
                                                    raw?.let { cleanVal ->
                                                        try {
                                                            val isoPart = cleanVal.substringBefore("T").substringBefore(" ")
                                                            if (isoPart.contains("-")) {
                                                                val parts = isoPart.split("-")
                                                                if (parts.size == 3) {
                                                                    return@let LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                                                                }
                                                            }
                                                            LocalDate.parse(isoPart)
                                                        } catch (e: Exception) { null }
                                                    }
                                                }

                                                val day1Date = remember(firstDebitStr, nextSipStr, firstInvestmentTxnDate) {
                                                    try {
                                                        if (!firstDebitStr.isNullOrBlank() && firstDebitStr != "null") {
                                                            val cleanVal = firstDebitStr.substringBefore("T").substringBefore(" ")
                                                            LocalDate.parse(cleanVal)
                                                        } else if (firstInvestmentTxnDate != null) {
                                                            firstInvestmentTxnDate
                                                        } else if (!nextSipStr.isNullOrBlank() && nextSipStr != "null") {
                                                            val cleanVal = nextSipStr.substringBefore("T").substringBefore(" ")
                                                            val parsedNext = LocalDate.parse(cleanVal)
                                                            getPreviousBusinessDay(parsedNext, 1)
                                                        } else {
                                                            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                                        }
                                                    } catch (e: Exception) {
                                                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                                    }
                                                }

                                                val day2Date = remember(day1Date) {
                                                    getNextBusinessDay(day1Date, 1)
                                                }

                                                val day3Date = getNextBusinessDay(day2Date, 1)

                                                val day4Date = remember(day1Date, firstAllocationDateStr) {
                                                    try {
                                                        if (!firstAllocationDateStr.isNullOrBlank() && firstAllocationDateStr != "null") {
                                                            LocalDate.parse(firstAllocationDateStr.substringBefore("T"))
                                                        } else {
                                                            getNextBusinessDay(day1Date, 3)
                                                        }
                                                    } catch (e: Exception) {
                                                        getNextBusinessDay(day1Date, 3)
                                                    }
                                                }

                                                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                                                val daysElapsed = (today.toEpochDays() - day1Date.toEpochDays()).coerceAtLeast(0)
                                                val debitedDays = when {
                                                    today < day1Date -> 1
                                                    today < day2Date -> 1
                                                    today < day3Date -> 2
                                                    else -> 3
                                                }
                                                 val totalDebitedAmt = sipAmount * debitedDays

                                                 val validStates = setOf("SUBMITTED", "IN_PROGRESS", "ALLOCATING UNITS", "DEBITED", "PROCESSING", "SUCCESS", "ALLOTTED", "COMPLETED")
                                                 val hasAnyDebit = state.transactions.any { txn ->
                                                     val s = txn.state?.uppercase().orEmpty()
                                                     s in validStates
                                                 } || state.investmentInProgress > 0.0 || state.investedAmount > 0.0
                                                 val hasFolio = !state.folioNumber.isNullOrBlank()

                                                 val day5Date = remember(day4Date) { getNextBusinessDay(day4Date, 1) }

                                                 // Progression logic for 5 steps:
                                                 // Step 1 (Plan setup): Ticked immediately after plan is created
                                                 val step1Done = true
                                                
                                                // Step 2 (Money debits started): Ticked after 1st debit transaction occurs OR if folio allocated
                                                val step2Done = hasAnyDebit || hasFolio

                                                // Step 3 (Folio creation): Ticked if folio is allocated OR on Day 3 (today >= day3Date)
                                                val step3Done = step2Done && (hasFolio || today >= day3Date)

                                                // Step 4 (Unit allotment started): Ticked when folio is allocated
                                                val step4Done = (step3Done && hasFolio) || hasFolio

                                                 // Step 5 (Account setup complete): Ticked on Day 5 (today >= day5Date) when folio allocation completes
                                                val step5Done = step4Done && today >= day5Date
                                                // Timeline stays visible on Day 5 (ticked), and stops being displayed starting on Day 5 + 1 (today > day5Date)
                                                val stepTimelineActive = !(step5Done && today > day5Date)

                                                if (stepTimelineActive) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        // Timeline Header
                                                        Text(
                                                            text = stringResource(Res.string.account_setup_progress_title),
                                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )

                                                        // Timeline Steps (5 Steps matching design image)
                                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                            // Step 1: Plan setup
                                                            SetupProgressStepRow(
                                                                title = stringResource(Res.string.step_plan_setup_title),
                                                                subtitle = stringResource(Res.string.step_plan_setup_desc),
                                                                isCompleted = step1Done,
                                                                isInProgress = false,
                                                                goalColor = goalColor
                                                            )

                                                            // Step 2: Money debits started
                                                            SetupProgressStepRow(
                                                                title = stringResource(Res.string.step_money_debits_title),
                                                                subtitle = stringResource(
                                                                    Res.string.step_money_debits_desc,
                                                                    dailyAmountStr
                                                                ),
                                                                isCompleted = step2Done,
                                                                isInProgress = !step2Done,
                                                                overrideStatusText = if (!step2Done) stringResource(Res.string.status_upcoming) else null,
                                                                goalColor = goalColor
                                                            )

                                                            // Step 3: Folio creation
                                                            val step3InProgress = step2Done && !step3Done
                                                            SetupProgressStepRow(
                                                                title = stringResource(Res.string.step_folio_creation_title),
                                                                subtitle = stringResource(Res.string.step_folio_creation_desc),
                                                                isCompleted = step3Done,
                                                                isInProgress = step3InProgress,
                                                                goalColor = goalColor
                                                            )

                                                            // Step 4: Unit allotment started
                                                            val step4InProgress = step3Done && !step4Done
                                                            SetupProgressStepRow(
                                                                title = stringResource(Res.string.step_unit_allotment_title),
                                                                subtitle = stringResource(Res.string.step_unit_allotment_desc),
                                                                isCompleted = step4Done,
                                                                isInProgress = step4InProgress,
                                                                goalColor = goalColor
                                                            )

                                                            // Step 5: Account setup complete
                                                            val step5InProgress = step4Done && !step5Done
                                                            SetupProgressStepRow(
                                                                title = stringResource(Res.string.step_account_setup_complete_title),
                                                                subtitle = stringResource(Res.string.step_account_setup_complete_desc),
                                                                isCompleted = step5Done,
                                                                isInProgress = step5InProgress,
                                                                goalColor = goalColor
                                                            )
                                                        }

                                                        // Checklist Footer
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            ChecklistRow(text = stringResource(Res.string.allotment_checklist_1))
                                                            ChecklistRow(text = stringResource(Res.string.allotment_checklist_2))
                                                            ChecklistRow(text = stringResource(Res.string.your_money_invested_from_day_one))
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))
                                                }
                                            }

                                             if (showFirstSaveDate) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(goalColor.copy(alpha = 0.1f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = goalColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = stringResource(Res.string.first_save_date_message, formatDate(nextSipDateStr!!)),
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                            item {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val tileBgColor = goalColor.copy(alpha = 0.06f).compositeOver(Color.White)
                                        DashboardTile(
                                            modifier = Modifier.weight(1f),
                                            title = stringResource(Res.string.plans),
                                            description = stringResource(Res.string.view_and_manage_plans_description),
                                            onClick = { showPlansView = true },
                                            backgroundColor = tileBgColor,
                                            iconColor = goalColor,
                                            icon = Icons.Default.FlashOn
                                        )
                                        DashboardTile(
                                            modifier = Modifier.weight(1f),
                                            title = stringResource(Res.string.transactions),
                                            description = stringResource(Res.string.track_investments_withdrawals_description),
                                            onClick = { showTransactionsView = true },
                                            backgroundColor = tileBgColor,
                                            iconColor = goalColor,
                                            icon = Icons.Default.Schedule
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val tileBgColor = goalColor.copy(alpha = 0.06f).compositeOver(Color.White)
                                        DashboardTile(
                                            modifier = Modifier.weight(1f),
                                            title = stringResource(Res.string.details_title),
                                            description = stringResource(Res.string.see_goal_details_description),
                                            onClick = { showDetailsPopup = true },
                                            backgroundColor = tileBgColor,
                                            iconColor = goalColor,
                                            icon = Icons.Default.Info
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // Overlays & Sheets
            if (showDetailsPopup) {
                SchemeDetailsPopupV2(
                    goalName = displayGoalName,
                    schemeName = displaySchemeName,
                    unitsInGm = state.unitsInGm ?: schemeParams?.unitsInGm,
                    folioNumber = state.folioNumber ?: schemeParams?.folioNumber,
                    totalUnitsAllotted = state.totalUnitsAllotted,
                    investedAmount = if (state.investedAmount > 0) (state.investedAmount + state.investmentInProgress) else ((schemeParams?.investedAmount ?: 0.0) + (schemeParams?.investmentInProgress ?: 0.0)),
                    currentValue = if (state.currentValue != 0.0) state.currentValue else (schemeParams?.currentValue ?: 0.0),
                    totalValue = if (state.cummulativeValue != 0.0) state.cummulativeValue else ((schemeParams?.currentValue ?: 0.0) + (schemeParams?.investmentInProgress ?: 0.0)),
                    investmentInProgress = if (state.investmentInProgress != 0.0) state.investmentInProgress else (schemeParams?.investmentInProgress ?: 0.0),
                    totalGain = if (state.totalGain != 0.0) state.totalGain else (schemeParams?.profit ?: 0.0),
                    withdrawnGain = if (state.withdrawnGain != 0.0) state.withdrawnGain else (schemeParams?.realizedProfit ?: 0.0),
                    availableGain = if (state.availableGain != 0.0) state.availableGain else (schemeParams?.unrealizedProfit ?: 0.0),
                    redemptionInProgress = if (state.redemptionInProgress != 0.0) state.redemptionInProgress else (schemeParams?.redemptionInProgress ?: 0.0),
                    category = displayCategory,
                    colorTheme = displayColorTheme,
                    mandates = state.mandates,
                    onDismiss = { showDetailsPopup = false },
                    onInvestMore = {
                        showDetailsPopup = false
                        handleSipClick()
                    },
                    onAddMoneyClick = {
                        showDetailsPopup = false
                        handleLumpsumClick()
                    },
                    onNewPlanClick = {
                        showDetailsPopup = false
                        handleSipClick()
                    },
                    onWithdrawClick = {
                        showDetailsPopup = false
                        if (state.currentValue > 0) {
                            if (state.redeemableAmount <= 0.0 && state.redemptionInProgress == 0.0) {
                                showWithdrawalNotAvailableDialog = true
                            } else {
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
                                    instantRedemptionValue = instantVal,
                                    unitsInGm = state.unitsInGm ?: schemeParams?.unitsInGm
                                )
                                WithdrawParamsManager.set(params)
                                scope.launch {
                                    sessionStore.saveValue("withdraw_init_params", WithdrawParamsManager.toJson(params))
                                }
                                onNavigateToWithdraw(params)
                            }
                        } else {
                            showInvestmentInProgressDialog = true
                        }
                    }
                )
            }

            // SIP Action Overlays
            if (showCancelSipScreen && mandateForCancelSip != null) {
                CancelSipInfoScreenV2(
                    schemeName = displaySchemeName,
                    dailyAmount = mandateForCancelSip?.amount ?: 0.0,
                    fundReturnPercent = getAnnualisedReturnPercent(goalType),
                    mandate = mandateForCancelSip,
                    onCancelSip = {
                        showCancelSipScreen = false
                        showCancelReasonScreen = true
                        selectedCancelReason = null
                    },
                    onGoBack = { 
                        showCancelSipScreen = false
                        mandateForCancelSip = null
                    }
                )
            }

            if (showCancelReasonScreen && mandateForCancelSip != null) {
                CancelSipReasonScreenV2(
                    isLoading = cancelSipLoading,
                    selectedReason = selectedCancelReason,
                    onReasonSelected = { selectedCancelReason = it },
                    onContinue = {
                        val mandate = mandateForCancelSip
                        val reason = selectedCancelReason
                        if (mandate != null && reason != null && userId.isNotBlank()) {
                            viewModel.cancelSip(userId, mandate.planId, mandate.mandateId, reason.keyword)
                        }
                    },
                    onGoBack = {
                        showCancelReasonScreen = false
                        showCancelSipScreen = true
                    }
                )
            }

            // Sheets
            if (showCancelSipSuccessSheet) {
                CancelSipSuccessBottomSheetV2(
                    onDone = { 
                        showCancelSipSuccessSheet = false
                        mandateForCancelSip = null
                        reloadData() 
                    }
                )
            }
            if (showCancelSipErrorSheet) {
                CancelSipErrorBottomSheetV2(onDone = { showCancelSipErrorSheet = false })
            }
            if (showPauseSipQuestionSheet && mandateForPauseSip != null) {
                val mandatePause = mandateForPauseSip
                PauseSipConfirmBottomSheetV2(
                    isLoading = pauseSipLoading,
                    mandate = mandatePause,
                    onCancel = {
                        showPauseSipQuestionSheet = false
                        mandateForPauseSip = null
                    },
                    onConfirm = {
                        if (userId.isNotBlank() && mandatePause != null) {
                            viewModel.pauseSip(userId, mandatePause.planId, mandatePause.mandateId)
                        }
                    }
                )
            }
            if (showPauseSipSuccessSheet) {
                PauseSipSuccessBottomSheetV2(onDone = { showPauseSipSuccessSheet = false; reloadData() })
            }
            if (showPauseSipErrorSheet) {
                PauseSipErrorBottomSheetV2(onDone = { showPauseSipErrorSheet = false })
            }
            if (showResumeSipQuestionSheet && mandateForResumeSip != null) {
                val mandateResume = mandateForResumeSip
                ResumeSipConfirmBottomSheetV2(
                    isLoading = resumeSipLoading,
                    onCancel = {
                        showResumeSipQuestionSheet = false
                        mandateForResumeSip = null
                    },
                    onConfirm = {
                        if (userId.isNotBlank() && mandateResume != null) {
                            viewModel.resumeSip(userId, mandateResume.planId, mandateResume.mandateId)
                        }
                    }
                )
            }
            if (showResumeSipSuccessSheet) {
                ResumeSipSuccessBottomSheetV2(onDone = { showResumeSipSuccessSheet = false; reloadData() })
            }
            if (showResumeSipErrorSheet) {
                ResumeSipErrorBottomSheetV2(onDone = { showResumeSipErrorSheet = false })
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

            if (showWithdrawalNotAvailableDialog) {
                AlertDialog(
                    onDismissRequest = { showWithdrawalNotAvailableDialog = false },
                    title = { Text(stringResource(Res.string.withdrawal_not_available_title)) },
                    text = { Text(stringResource(Res.string.withdrawal_not_available_message)) },
                    confirmButton = {
                        TextButton(onClick = { showWithdrawalNotAvailableDialog = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showFolioPendingDialog) {
                AlertDialog(
                    onDismissRequest = { showFolioPendingDialog = false },
                    text = { Text(stringResource(Res.string.folio_pending_message)) },
                    confirmButton = {
                        TextButton(onClick = { showFolioPendingDialog = false }) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showNewPlanPendingDialog) {
                AlertDialog(
                    onDismissRequest = { showNewPlanPendingDialog = false },
                    text = { Text(stringResource(Res.string.new_plan_pending_message)) },
                    confirmButton = {
                        TextButton(onClick = { showNewPlanPendingDialog = false }) {
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

            // Error Dialog
            state.errorMessage?.let { errorMsg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearErrorMessage() },
                    title = { Text(if (errorMsg.contains("connect", true) || errorMsg.contains("Internet", true)) "Network Error" else "Error", fontWeight = FontWeight.Bold) },
                    text = { Text(errorMsg) },
                    confirmButton = {
                        TextButton(onClick = {
                            val isNetwork = errorMsg.contains("connect", true) || errorMsg.contains("Internet", true)
                            viewModel.clearErrorMessage()
                            if (isNetwork && userId.isNotBlank() && purpose.isNotBlank()) {
                                val latestParams = schemeParams ?: SchemeDetailsParamsManager.get()
                                val uipid = latestParams?.userPurposeId ?: purpose
                                viewModel.loadTransactions(userId, uipid, latestParams)
                            }
                        }) {
                            Text(if (errorMsg.contains("connect", true) || errorMsg.contains("Internet", true)) "Retry" else "OK")
                        }
                    }
                )
            }

            // Back Handling for sheets/views
            com.pyllar.consumer.util.BackHandler(
                enabled = showCancelReasonScreen ||
                        showCancelSipScreen ||
                        showCancelSipSuccessSheet ||
                        showCancelSipErrorSheet ||
                        showPauseSipQuestionSheet ||
                        showPauseSipSuccessSheet ||
                        showPauseSipErrorSheet ||
                        showResumeSipQuestionSheet ||
                        showResumeSipSuccessSheet ||
                        showResumeSipErrorSheet ||
                        showPlansView ||
                        showTransactionsView
            ) {
                when {
                    showCancelReasonScreen -> {
                        showCancelReasonScreen = false
                        showCancelSipScreen = true
                    }
                    showCancelSipScreen -> {
                        showCancelSipScreen = false
                        mandateForCancelSip = null
                    }
                    showCancelSipSuccessSheet -> { showCancelSipSuccessSheet = false; reloadData() }
                    showCancelSipErrorSheet -> showCancelSipErrorSheet = false
                    showPauseSipQuestionSheet -> {
                        showPauseSipQuestionSheet = false
                        mandateForPauseSip = null
                    }
                    showPauseSipSuccessSheet -> { showPauseSipSuccessSheet = false; reloadData() }
                    showPauseSipErrorSheet -> showPauseSipErrorSheet = false
                    showResumeSipQuestionSheet -> {
                        showResumeSipQuestionSheet = false
                        mandateForResumeSip = null
                    }
                    showResumeSipSuccessSheet -> { showResumeSipSuccessSheet = false; reloadData() }
                    showResumeSipErrorSheet -> showResumeSipErrorSheet = false
                    showPlansView || showTransactionsView -> {
                        showPlansView = false
                        showTransactionsView = false
                    }
                }
            }
        }
    }
}

@Composable
fun SchemeDetailsCardV2(
    schemeName: String?,
    goalName: String?,
    unitsInGm: Double?,
    category: String?,
    colorTheme: String?,
    folioNumber: String?,
    investedAmount: Double,
    totalUnitsAllotted: Double,
    totalValue: Double,
    currentValue: Double,
    investmentInProgress: Double,
    totalGain: Double,
    withdrawnGain: Double,
    availableGain: Double,
    redemptionInProgress: Double = 0.0,
    hasApprovedPlan: Boolean = false,
    nextSipDate: String? = null,
    showBottomSection: Boolean = true,
    containerColor: Color = Color.White,
    onViewDetailsClick: () -> Unit = {},
    onEstimatedGoldInfoClick: () -> Unit = {},
    onEstimatedSilverInfoClick: () -> Unit = {},
    decimalPlaces: Int = 1
) {
    val goalColor = getCorrelationColorForCategory(category, colorTheme)
    val isGold = category?.uppercase() == "GOLD"
    val isSilver = category?.uppercase() == "SILVER"
    val isGoldOrSilver = isGold || isSilver

    val allottedValue = when {
        isGoldOrSilver && unitsInGm != null && unitsInGm > 0 -> {
            if (unitsInGm < 1.0) "${formatDecimal(unitsInGm * 1000.0, 1)}${stringResource(Res.string.mg_label)}" else "${formatDecimal(unitsInGm, 2)}${stringResource(Res.string.g_label)}"
        }
        else -> {
            formatRupeeAmount(totalValue, 0)
        }
    }

    val hasFolio = !folioNumber.isNullOrBlank() && folioNumber != "null"
    val hasAllotted = (isGoldOrSilver && unitsInGm != null && unitsInGm > 0) || totalUnitsAllotted > 0
    val hasInvestedAmount = investedAmount > 0
    val hasTotalValue = totalValue > 0
    val hasAvailableGain = availableGain != 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (hasAllotted) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isGold) stringResource(Res.string.estimated_gold) else if (isSilver) stringResource(Res.string.estimated_silver) else stringResource(Res.string.your_savings),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = goalColor
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = allottedValue,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isGoldOrSilver) {
                            IconButton(
                                onClick = {
                                    if (isGold) onEstimatedGoldInfoClick() else onEstimatedSilverInfoClick()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else if (!schemeName.isNullOrBlank()) {
                Text(
                    text = formatSchemeName(schemeName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasInvestedAmount) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (showBottomSection) stringResource(Res.string.invested_label) else stringResource(Res.string.you_invested),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            RupeeAmountBlock(
                                value = investedAmount,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                decimalPlaces = decimalPlaces
                            )
                            if (!showBottomSection) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(Res.string.received),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(10.dp).padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (hasTotalValue) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (showBottomSection) stringResource(Res.string.total_value_label) else stringResource(Res.string.worth_today),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            RupeeAmountBlock(
                                value = totalValue,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                decimalPlaces = decimalPlaces
                            )
                            if (!showBottomSection) {
                                Text(
                                    text = stringResource(Res.string.allocated_pending),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (hasApprovedPlan && (investedAmount > 0 || totalValue > 0)) {
                val assetTypeStr = when {
                    schemeName?.contains("Gold", ignoreCase = true) == true || goalName?.contains("Gold", ignoreCase = true) == true -> stringResource(Res.string.gold_units_label)
                    schemeName?.contains("Silver", ignoreCase = true) == true || goalName?.contains("Silver", ignoreCase = true) == true -> stringResource(Res.string.silver_units_label)
                    else -> stringResource(Res.string.intro_goal_savings)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(goalColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = goalColor,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = stringResource(Res.string.processing_warning_message, assetTypeStr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            } else if (hasApprovedPlan && investedAmount == 0.0 && totalValue == 0.0 && !nextSipDate.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(goalColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = goalColor,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = stringResource(Res.string.first_save_date_message, formatDate(nextSipDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (showBottomSection) {
                if (redemptionInProgress > 0) {
                    val amountStr = formatDecimal(redemptionInProgress, decimalPlaces)
                    val fullMsg = stringResource(Res.string.redemption_in_progress_message, amountStr)
                    val annotatedMessage = buildAnnotatedString {
                        val startIndex = fullMsg.indexOf(amountStr)
                        if (startIndex != -1) {
                            append(fullMsg.substring(0, startIndex))
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(amountStr)
                            }
                            append(fullMsg.substring(startIndex + amountStr.length))
                        } else {
                            append(fullMsg)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = annotatedMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }

                if (hasAvailableGain) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 2.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val signString = if (availableGain >= 0) "+" else "-"
                        val formattedGain = formatRupeeAmount(abs(availableGain), decimalPlaces)
                        val additionalMsg = when {
                            availableGain < 0 && isGold -> " " + stringResource(Res.string.gold_price_dipped_message)
                            availableGain < 0 && isSilver -> " " + stringResource(Res.string.silver_price_dipped_message)
                            else -> ""
                        }

                        Text(
                            text = "$signString$formattedGain"  + " " + stringResource(Res.string.now) + additionalMsg,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable(onClick = onViewDetailsClick)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.view_details) + " →",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = goalColor
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SchemeDetailsPopupV2(
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
    onInvestMore: () -> Unit,
    onAddMoneyClick: () -> Unit = {},
    onNewPlanClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        SchemeDetailsPopupContentV2(
            goalName = goalName,
            schemeName = schemeName,
            unitsInGm = unitsInGm,
            folioNumber = folioNumber,
            totalUnitsAllotted = totalUnitsAllotted,
            investedAmount = investedAmount,
            currentValue = currentValue,
            totalValue = totalValue,
            investmentInProgress = investmentInProgress,
            totalGain = totalGain,
            withdrawnGain = withdrawnGain,
            availableGain = availableGain,
            redemptionInProgress = redemptionInProgress,
            category = category,
            colorTheme = colorTheme,
            mandates = mandates,
            onDismiss = onDismiss,
            onInvestMore = onInvestMore,
            onAddMoneyClick = onAddMoneyClick,
            onNewPlanClick = onNewPlanClick,
            onWithdrawClick = onWithdrawClick
        )
    }
}

@Composable
fun SchemeDetailsPopupContentV2(
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
    onInvestMore: () -> Unit,
    onAddMoneyClick: () -> Unit = {},
    onNewPlanClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {}
) {
    val isGoldOrSilver = schemeName?.contains("Gold", ignoreCase = true) == true ||
            schemeName?.contains("Silver", ignoreCase = true) == true ||
            goalName?.contains("Gold", ignoreCase = true) == true ||
            goalName?.contains("Silver", ignoreCase = true) == true
    val isEstimatedGold = isGoldOrSilver && (goalName?.contains("Gold", ignoreCase = true) == true || schemeName?.contains("Gold", ignoreCase = true) == true)

    val allottedValue = when {
        isGoldOrSilver && unitsInGm != null && unitsInGm > 0 -> {
            if (unitsInGm < 1.0) "${formatDecimal(unitsInGm * 1000.0, 1)}${stringResource(Res.string.mg_label)}" else "${formatDecimal(unitsInGm, 2)}${stringResource(Res.string.g_label)}"
        }
        else -> {
            formatRupeeAmount(totalValue, 0)
        }
    }

    val hasFolio = !folioNumber.isNullOrBlank() && folioNumber != "null"
    val hasAllotted = (isGoldOrSilver && unitsInGm != null && unitsInGm > 0) || totalUnitsAllotted > 0
    val hasInvestedAmount = investedAmount > 0
    val hasCurrentValue = currentValue > 0
    val hasTotalGain = totalGain != 0.0
    val hasAccountDetails = hasFolio || hasAllotted || hasInvestedAmount || hasCurrentValue

    val displayTitle = if (!schemeName.isNullOrBlank()) formatSchemeName(schemeName) else formatGoalName(goalName ?: "")

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

    val popupBackground = lerp(
        MaterialTheme.colorScheme.surface,
        getCorrelationColorForCategory(category, colorTheme),
        0.10f
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = popupBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 15.dp, bottom = 160.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = getCorrelationColorForCategory(category, colorTheme)
                    )
                }

                val approvedMandatePopup = mandates.firstOrNull { m ->
                    val s = m.status?.uppercase().orEmpty()
                    (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
                }
                SchemeDetailsCardV2(
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
                    hasApprovedPlan = approvedMandatePopup != null,
                    nextSipDate = approvedMandatePopup?.nextSipDate,
                    showBottomSection = false,
                    containerColor = Color.White,
                    onEstimatedGoldInfoClick = { showPopupGoldInfo = true },
                    onEstimatedSilverInfoClick = { showPopupSilverInfo = true },
                    decimalPlaces = 1
                )

                Spacer(modifier = Modifier.height(24.dp))

                val activeMandates = mandates.filter { m ->
                    val s = m.status?.uppercase().orEmpty()
                    (s.contains("APPROVED") || s.contains("ACTIVE")) && !s.contains("PAUSED")
                }

                if (activeMandates.isNotEmpty() || investmentInProgress > 0 || redemptionInProgress > 0) {
                    Text(
                        text = stringResource(Res.string.whats_happening_section),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            var needDivider = false

                            if (activeMandates.isNotEmpty()) {
                                val totalDailyAmount = activeMandates.sumOf { it.amount }
                                val freq = activeMandates.firstOrNull()?.frequency?.lowercase() ?: ""
                                val savingAmountFormatted = formatDecimal(totalDailyAmount, 1)
                                val savingText = stringResource(Res.string.saving_amount_freq, savingAmountFormatted, freq)
                                
                                val nextSipMandate = activeMandates.filter { !it.nextSipDate.isNullOrBlank() }
                                    .minByOrNull { it.nextSipDate!! }
                                val nextDeduction = if (nextSipMandate != null) {
                                    stringResource(Res.string.next_deduction_date, formatDate(nextSipMandate.nextSipDate))
                                } else {
                                    stringResource(Res.string.next_deduction_pending)
                                }
//                                WhatsHappeningRowV2(
//                                    title = savingText,
//                                    subtitle = nextDeduction,
//                                    badgeText = stringResource(Res.string.active_badge),
//                                    badgeBg = Color(0xFFE8F5E9),
//                                    badgeFg = Color(0xFF2E7D32)
//                                )
//                                needDivider = true
                            }

                            if (investmentInProgress > 0) {
                                if (needDivider) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                                val inProgTitle = when {
                                    isEstimatedGold -> stringResource(Res.string.gold_being_allocated)
                                    isGoldOrSilver -> stringResource(Res.string.silver_being_allocated)
                                    else -> stringResource(Res.string.savings_being_allocated)
                                }
                                val amountStr = formatDecimal(investmentInProgress, 1)
                                val allocationDateStr = activeMandates.firstOrNull()?.calculatedFirstUnitAllocationDate ?: activeMandates.firstOrNull()?.firstUnitAllocationDate
                                val inProgSub = if (!allocationDateStr.isNullOrBlank()) {
                                    "₹$amountStr received · will be allocated on ${formatDate(allocationDateStr)}"
                                } else {
                                    stringResource(Res.string.allocation_processing_sub, amountStr, stringResource(Res.string.units))
                                }
                                WhatsHappeningRowV2(
                                    title = inProgTitle,
                                    subtitle = inProgSub,
                                    badgeText = stringResource(Res.string.processing_badge),
                                    badgeBg = Color(0xFFFFF3E0),
                                    badgeFg = Color(0xFFF57C00)
                                )
                                needDivider = true
                            }

                            if (redemptionInProgress > 0) {
                                if (needDivider) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                                val amountStr = formatDecimal(redemptionInProgress, 1)
                                WhatsHappeningRowV2(
                                    title = stringResource(Res.string.withdrawal_in_progress_title),
                                    subtitle = "₹$amountStr · ${stringResource(Res.string.takes_upto_2_business_days)}",
                                    badgeText = stringResource(Res.string.processing_badge),
                                    badgeBg = Color(0xFFFFF3E0),
                                    badgeFg = Color(0xFFF57C00)
                                )
                                needDivider = true
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (hasAccountDetails) {
                    Text(
                        text = stringResource(Res.string.for_your_records_section),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (hasFolio) {
                                RecordRowV2(stringResource(Res.string.folio_no), folioNumber!!)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                            if (hasAllotted) {
                                val sub = if (isGoldOrSilver) stringResource(if (isEstimatedGold) Res.string.internal_units_gold else Res.string.internal_units_silver) else stringResource(Res.string.internal_units_generic)
                                RecordRowV2(stringResource(Res.string.your_savings), allottedValue, sub)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                            if (hasInvestedAmount || hasCurrentValue) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = stringResource(Res.string.total_value_label),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatRupeeAmount(totalValue, 1),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val gain = availableGain
                                        val invested = investedAmount + investmentInProgress
//                                        if (invested > 0) {
//                                            val isLoss = gain < 0
//                                            val percent = abs(gain / invested * 100)
//                                            Text(
//                                                text = "(${if(!isLoss)"+" else "-"}${formatDecimal(percent, 1)}%)",
//                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
//                                                color = if(!isLoss) Color(0xFF2E7D32) else Color(0xFF808080)
//                                            )
//                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        RecordRowV2(stringResource(Res.string.current_value), formatRupeeAmount(currentValue, 1))
                                        RecordRowV2(stringResource(Res.string.investment_in_progress), formatRupeeAmount(investmentInProgress, 1))
                                        if (redemptionInProgress > 0) {
                                            RecordRowV2(stringResource(Res.string.withdrawal_in_progress_title), formatRupeeAmount(redemptionInProgress, 1))
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                            if (hasTotalGain) {
                                val diffColor = if(totalGain >= 0) Color(0xFF2E7D32) else Color.Black
                                val diffSubtext = when {
                                    isEstimatedGold -> stringResource(Res.string.total_gain_disclaimer_gold)
                                    isGoldOrSilver -> stringResource(Res.string.total_gain_disclaimer_silver)
                                    else -> null
                                }
                                RecordRowV2(
                                    stringResource(Res.string.total_gain),
                                    formatGainRupeeAmount(totalGain, 1),
                                    diffSubtext,
                                    valueColor = diffColor
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }
                            RecordRowV2(stringResource(Res.string.available_gain), formatGainRupeeAmount(availableGain, 1), valueColor = if(availableGain >= 0) Color(0xFF2E7D32) else Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (withdrawnGain != 0.0) {
                                val wgColor = if(withdrawnGain >= 0) Color(0xFF2E7D32) else Color.Black
                                RecordRowV2(
                                    stringResource(Res.string.withdrawn_gain),
                                    formatGainRupeeAmount(withdrawnGain, 1),
                                    valueColor = wgColor
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            }

                            RecordRowV2(stringResource(Res.string.can_i_take_it_out), stringResource(Res.string.yes_anytime), null, MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(getCorrelationColorForCategory(category, colorTheme).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = getCorrelationColorForCategory(category, colorTheme),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = stringResource(Res.string.sebi_mutual_fund_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            val goalColor = getCorrelationColorForCategory(category, colorTheme)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButtonModuleV2(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.add_money),
                            icon = Icons.Default.Add,
                            containerColor = goalColor,
                            onClick = onAddMoneyClick
                        )

                        ActionButtonModuleV2(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.new_plan),
                            icon = Icons.Default.FlashOn,
                            containerColor = goalColor,
                            onClick = onNewPlanClick
                        )

                        ActionButtonModuleV2(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.withdraw),
                            icon = Icons.Default.CallReceived,
                            containerColor = goalColor,
                            onClick = onWithdrawClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRowV2(
    label: String,
    value: String,
    subtext: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtext != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
private fun WhatsHappeningRowV2(
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CancelSipInfoScreenV2(
    schemeName: String,
    dailyAmount: Double,
    fundReturnPercent: Double?,
    mandate: MandateDisplayItem? = null,
    onCancelSip: () -> Unit,
    onGoBack: () -> Unit
) {
    val isDaily = mandate?.frequency?.uppercase() == "DAILY"
    val firstDebitDateStr = mandate?.firstDebitDate ?: mandate?.nextSipDate
    val calculatedAllocationDateStr = mandate?.calculatedFirstUnitAllocationDate

    val isFirstDebitPassed = isDatePassed(firstDebitDateStr)
    val isAllocationPassed = isDatePassed(calculatedAllocationDateStr)
    val showFirstDebitNotPassed = isDaily && mandate != null && !firstDebitDateStr.isNullOrBlank() && !isFirstDebitPassed
    val showAllocationNotPassed = isDaily && mandate != null && isFirstDebitPassed && !calculatedAllocationDateStr.isNullOrBlank() && !isAllocationPassed
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp, top = 50.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (showFirstDebitNotPassed || showAllocationNotPassed) {
                Text(
                    text = "Cancel SIP?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showAllocationNotPassed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Your investment is still being processed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF57C00)
                        )
                    }


                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF9F4)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3E2723).copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showFirstDebitNotPassed) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PLAN CREATED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
                                    color = Color(0xFF3E2723).copy(alpha = 0.5f)
                                )
                                Text(
                                    text = formatDate(mandate?.mandateCreatedDate ?: mandate?.mandateApprovedDate).ifBlank { "—" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF3E2723)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FIRST DEBIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
                                    color = Color(0xFF3E2723).copy(alpha = 0.5f)
                                )
                                Text(
                                    text = formatDate(firstDebitDateStr).ifBlank { "—" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF3E2723)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FIRST DEBIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
                                    color = Color(0xFF3E2723).copy(alpha = 0.5f)
                                )
                                Text(
                                    text = formatDate(firstDebitDateStr).ifBlank { "—" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF3E2723)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UNIT ALLOCATION (EXPECTED)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
                                    color = Color(0xFF3E2723).copy(alpha = 0.5f)
                                )
                                Text(
                                    text = formatDate(calculatedAllocationDateStr).ifBlank { "—" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF3E2723)
                                )
                            }
                        }
                    }
                }

                if (showAllocationNotPassed) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Almost there!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Your first investment is still being processed. Waiting until the units are allocated lets you see your first SIP investment before deciding whether to cancel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Cancelling now will stop your SIP before your first investment begins.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.cancel_sip_fund_performance_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val percentText = fundReturnPercent?.let {
                            "${formatDecimal(it, 1)}%"
                        } ?: "--%"
                        Text(
                            text = stringResource(Res.string.cancel_sip_fund_performance_body, percentText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(Res.string.cancel_sip_before_heading),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.cancel_sip_fund_performance_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val percentText = fundReturnPercent?.let {
                            "${formatDecimal(it, 1)}%"
                        } ?: "--%"
                        Text(
                            text = stringResource(Res.string.cancel_sip_fund_performance_body, percentText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.cancel_sip_did_you_know_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(Res.string.cancel_sip_did_you_know_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                val amountText = formatIndian(dailyAmount)
                val schemeLabel = if (schemeName.isNotBlank()) schemeName else "this scheme"
                Text(
                    text = stringResource(Res.string.cancel_sip_warning_body, amountText, schemeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCancelSip,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.cancel_sip),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }

            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.go_back),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

private enum class CancelSipReasonV2(val keyword: String, val labelRes: org.jetbrains.compose.resources.StringResource) {
    AMOUNT_NOT_AVAILABLE("amount_not_available", Res.string.amount_not_available),
    INVESTMENT_RETURNS_NOT_AS_EXPECTED("investment_returns_not_as_expected", Res.string.investment_returns_not_as_expected),
    EXIT_LOAD_NOT_AS_EXPECTED("exit_load_not_as_expected", Res.string.exit_load_not_as_expected),
    SWITCH_TO_OTHER_SCHEME("switch_to_other_scheme", Res.string.switch_to_other_scheme),
    FUND_MANAGER_CHANGED("fund_manager_changed", Res.string.fund_manager_changed),
    INVESTMENT_GOAL_COMPLETE("investment_goal_complete", Res.string.investment_goal_complete),
    MANDATE_NOT_READY("mandate_not_ready", Res.string.mandate_not_ready),
    INVEST_LATER("invest_later", Res.string.invest_later),
    CUSTOMER_SUPPORT_NOT_SATISFACTORY("customer_support_not_satisfactory", Res.string.customer_support_not_satisfactory),
    AMC_SUPPORT_NOT_SATISFACTORY("amc_support_not_satisfactory", Res.string.amc_support_not_satisfactory),
}

@Composable
private fun CancelSipReasonScreenV2(
    isLoading: Boolean = false,
    selectedReason: CancelSipReasonV2?,
    onReasonSelected: (CancelSipReasonV2) -> Unit,
    onContinue: () -> Unit,
    onGoBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f)),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(Res.string.cancel_sip_reason_heading),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            val orderedReasons = listOf(
                CancelSipReasonV2.AMOUNT_NOT_AVAILABLE,
                CancelSipReasonV2.INVESTMENT_RETURNS_NOT_AS_EXPECTED,
                CancelSipReasonV2.EXIT_LOAD_NOT_AS_EXPECTED,
                CancelSipReasonV2.SWITCH_TO_OTHER_SCHEME,
                CancelSipReasonV2.FUND_MANAGER_CHANGED,
                CancelSipReasonV2.INVESTMENT_GOAL_COMPLETE,
                CancelSipReasonV2.MANDATE_NOT_READY,
                CancelSipReasonV2.INVEST_LATER,
                CancelSipReasonV2.CUSTOMER_SUPPORT_NOT_SATISFACTORY,
                CancelSipReasonV2.AMC_SUPPORT_NOT_SATISFACTORY
            )
            orderedReasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading) { onReasonSelected(reason) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == reason,
                        onClick = { onReasonSelected(reason) },
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(reason.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedReason != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.cancel_sip_continue),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
            OutlinedButton(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(Res.string.go_back),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

private fun getAnnualisedReturnPercent(goalType: GoalType, years: Int = 3): Double {
    val annualRateDecimal = when {
        goalType == GoalType.GOLD -> when (years) {
            1 -> 0.754
            3 -> 0.342
            5 -> 0.221
            7 -> 0.215
            else -> 0.215
        }
        goalType == GoalType.SILVER -> when (years) {
            1 -> 1.582
            3 -> 0.435
            5 -> 0.341
            7 -> 0.295
            else -> 0.295
        }
        goalType == GoalType.SAVINGS -> 0.075
        goalType == GoalType.FESTIVAL_SPENDS -> 0.075
        goalType == GoalType.GLOBAL_EXPOSURE -> 0.23
        goalType == GoalType.ALL_IN_ONE -> 0.175
        goalType == GoalType.INNOVATION -> 0.15
        goalType == GoalType.SENSEX -> 0.135
        else -> 0.10
    }
    return annualRateDecimal * 100.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSipSuccessBottomSheetV2(
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(Res.string.sip_cancelled_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.sip_cancelled_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.ok),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSipErrorBottomSheetV2(
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(Res.string.sip_cancellation_failed_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.sip_cancellation_failed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.sip_cancellation_failed_done),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipConfirmBottomSheetV2(
    isLoading: Boolean = false,
    mandate: MandateDisplayItem? = null,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .then(if (isLoading) Modifier.alpha(0f) else Modifier),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(Res.string.pause_sip_heading),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val isDaily = mandate?.frequency?.uppercase() == "DAILY"
                val firstDebitDateStr = mandate?.firstDebitDate ?: mandate?.nextSipDate
                val calculatedAllocationDateStr = mandate?.calculatedFirstUnitAllocationDate

                val isFirstDebitPassed = isDatePassed(firstDebitDateStr)
                val isAllocationPassed = isDatePassed(calculatedAllocationDateStr)

                val showFirstDebitNotPassed = isDaily && mandate != null && !firstDebitDateStr.isNullOrBlank() && !isFirstDebitPassed
                val showAllocationNotPassed = isDaily && mandate != null && isFirstDebitPassed && !calculatedAllocationDateStr.isNullOrBlank() && !isAllocationPassed

                val bodyText = when {
                    showAllocationNotPassed -> {
                        "Your first investment is still being processed. Waiting until the units are allocated lets you see your first SIP investment before deciding whether to pause."
                    }
                    showFirstDebitNotPassed -> {
                        "Pausing now will stop your SIP before your first investment begins."
                    }
                    else -> {
                        stringResource(Res.string.pause_sip_body)
                    }
                }

                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                SipDateDetailsDisplay(mandate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.go_back),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.pause),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipSuccessBottomSheetV2(
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(Res.string.sip_paused_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.sip_paused_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.ok),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseSipErrorBottomSheetV2(
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(Res.string.sip_pause_failed_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.sip_pause_failed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.sip_pause_failed_done),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipConfirmBottomSheetV2(
    isLoading: Boolean = false,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .then(if (isLoading) Modifier.alpha(0f) else Modifier),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(Res.string.resume_sip_heading),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.resume_sip_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.go_back),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.resume_sip),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipSuccessBottomSheetV2(
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(Res.string.sip_resumed_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(Res.string.sip_resumed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.ok),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeSipErrorBottomSheetV2(
    onDone: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.Hidden
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(Res.string.sip_resume_failed_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.sip_resume_failed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.sip_resume_failed_done),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                )
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
                        color = if (transaction.isCredit) Color(0xFF2E7D32) else Color(0xFFF44336)
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
    val displayAmount = formatRupeeAmount(if (activeTotal > 0) activeTotal else pausedTotal, 0)

    val allPaused = activePlans.isEmpty() && pausedPlans.isNotEmpty()
    val statusText = when {
        allPaused -> stringResource(Res.string.sip_all_paused)
        pausedPlans.isNotEmpty() -> stringResource(Res.string.sip_active_paused_count, activePlans.size, pausedPlans.size)
        else -> stringResource(Res.string.sip_active_count, activePlans.size)
    }
    val statusColor = if (allPaused) Color(0xFF7A5200) else Color(0xFF0B6B30)
    val statusBg = if (allPaused) Color(0xFFFFC850).copy(alpha = 0.35f) else Color(0xFF0B6B30).copy(alpha = 0.10f)

    val detailText = if (!isMonthly) {
        if (allPaused) stringResource(Res.string.daily_cadence_paused) else stringResource(Res.string.daily_cadence_active)
    } else {
        val activeDays = activePlans.mapNotNull { dayOfMonthOrdinal(it.nextSipDate) }.distinct()
        when {
            activeDays.isNotEmpty() -> stringResource(Res.string.monthly_cadence_active, activeDays.joinToString(" & "))
            else -> {
                val pausedDays = pausedPlans.mapNotNull { dayOfMonthOrdinal(it.nextSipDate) }.distinct()
                if (pausedDays.isNotEmpty()) stringResource(Res.string.monthly_cadence_paused, pausedDays.joinToString(" & ")) else stringResource(Res.string.monthly_cadence_unknown)
            }
        }
    }

    val nextActive = activePlans
        .filter { !it.nextSipDate.isNullOrBlank() && it.nextSipDate != "null" }
        .minByOrNull { it.nextSipDate!! }
    val nextText = if (nextActive != null) {
        stringResource(Res.string.sip_next_label, formatDate(nextActive.nextSipDate))
    } else {
        stringResource(Res.string.sip_no_upcoming_debit)
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
            imageVector = if (isMonthly) Icons.Default.CalendarMonth else Icons.Default.FlashOn,
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
                    text = if (isMonthly) stringResource(Res.string.per_month_suffix) else stringResource(Res.string.per_day_suffix),
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

@Composable
fun SipPlanCardV2(
    mandate: MandateDisplayItem,
    planNumber: Int,
    goalColor: Color,
    modifier: Modifier = Modifier,
    category: String = "",
    onPause: ((MandateDisplayItem) -> Unit)? = null,
    onResume: ((MandateDisplayItem) -> Unit)? = null,
    onCancel: ((MandateDisplayItem) -> Unit)? = null
) {
    val cardGradientColors = when (category) {
        "GOLD" -> listOf(Color(0xFFFFFDF7), Color(0xFFFBF6E8), Color(0xFFF5EDD4))
        "SILVER" -> listOf(Color(0xFFF8FBFD), Color(0xFFEEF4F8), Color(0xFFE2EDF4))
        "INNOVATION" -> listOf(Color(0xFFF9F7FC), Color(0xFFF1ECF8), Color(0xFFE6DDF2))
        "SENSEX" -> listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFBFDBFE))
        "FESTIVAL_SPENDS" -> listOf(Color(0xFFFFFDF8), Color(0xFFFFF5E6), Color(0xFFFFECC7))
        "CHILDRENS_EDUCATION" -> listOf(Color(0xFFF7FAFE), Color(0xFFEDF4FE), Color(0xFFDCEBFE))
        "VACATION" -> listOf(Color(0xFFFCF7FD), Color(0xFFF7EDFD), Color(0xFFEDDCFD))
        "GLOBAL_EXPOSURE" -> listOf(Color(0xFFF2FBFB), Color(0xFFE6F7F7), Color(0xFFD2EFEF))
        "MARKET_EXPLORER" -> listOf(Color(0xFFF2F9F7), Color(0xFFE4F3EE), Color(0xFFC7E7DD))
        "ALL_IN_ONE" -> listOf(Color(0xFFF5F6FB), Color(0xFFEBEDF7), Color(0xFFD7DBF0))
        "SAVINGS", "SAVINGS_PLUS" -> listOf(Color(0xFFFFFDF5), Color(0xFFF5FFF5), Color(0xFFEBFFEB))
        else -> listOf(Color(0xFFF5FBF7), Color(0xFFEAF7EE), Color(0xFFD9F0E2))
    }
    val cardBorderColor = when (category) {
        "GOLD" -> Color(0xFFD4AF37).copy(alpha = 0.28f)
        "SILVER" -> Color(0xFF6A9AB0).copy(alpha = 0.28f)
        "INNOVATION" -> Color(0xFF876DAF).copy(alpha = 0.28f)
        "SENSEX" -> Color(0xFF2346B5).copy(alpha = 0.28f)
        "FESTIVAL_SPENDS" -> Color(0xFFFF9800).copy(alpha = 0.28f)
        "CHILDRENS_EDUCATION" -> Color(0xFF2196F3).copy(alpha = 0.28f)
        "VACATION" -> Color(0xFF9C27B0).copy(alpha = 0.28f)
        "GLOBAL_EXPOSURE" -> Color(0xFF00897B).copy(alpha = 0.28f)
        "MARKET_EXPLORER" -> Color(0xFF0F6B5C).copy(alpha = 0.28f)
        "ALL_IN_ONE" -> Color(0xFF3F51B5).copy(alpha = 0.28f)
        "SAVINGS", "SAVINGS_PLUS" -> Color(0xFF009688).copy(alpha = 0.28f)

        else -> Color(0xFF27AE60).copy(alpha = 0.28f)
    }

    val shimmerTransition = rememberInfiniteTransition(label = "sipPlanCardShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sipPlanCardShimmerProgress"
    )

    val statusUpper = mandate.status?.uppercase().orEmpty()
    val isActive = (statusUpper.contains("APPROVED") || statusUpper.contains("ACTIVE")) && !statusUpper.contains("PAUSED")
    val isPaused = statusUpper.contains("PAUSED")
    val isMonthly = mandate.frequency?.uppercase() == "MONTHLY"

    val statusLabel = when {
        isActive -> stringResource(Res.string.status_active)
        isPaused -> stringResource(Res.string.status_paused)
        statusUpper.contains("PENDING") -> stringResource(Res.string.status_pending)
        statusUpper.contains("CANCELLED") -> stringResource(Res.string.status_cancelled)
        statusUpper.contains("REJECTED") -> stringResource(Res.string.status_rejected)
        statusUpper.contains("FAILED") -> stringResource(Res.string.status_failed)
        else -> statusUpper.replace("_", " ")
    }
    val statusColor = when {
        isActive -> Color(0xFF1A7A42)
        isPaused -> Color(0xFF8B6B25)
        else -> Color(0xFF616161)
    }
    val statusBg = when {
        isActive -> Color(0xFF1A7A42).copy(alpha = 0.10f)
        isPaused -> Color(0xFF8B6B25).copy(alpha = 0.10f)
        else -> Color(0xFFF5F5F5)
    }

    val sipDateOrdinal = if (isMonthly) dayOfMonthOrdinal(mandate.nextSipDate) else null
    val cadenceDesc = when {
        !isMonthly && isPaused -> stringResource(Res.string.daily_cadence_paused)
        !isMonthly -> stringResource(Res.string.daily_cadence_active)
        sipDateOrdinal != null && isPaused -> stringResource(Res.string.monthly_cadence_paused, sipDateOrdinal)
        sipDateOrdinal != null -> stringResource(Res.string.monthly_cadence_active, sipDateOrdinal)
        else -> stringResource(Res.string.monthly_cadence_unknown)
    }

    val startedText = formatDate(mandate.mandateCreatedDate ?: mandate.mandateApprovedDate).ifBlank { "—" }
    val nextDebitRaw = mandate.nextSipDate?.takeIf { it.isNotBlank() && it != "null" }
    val nextDebitText = nextDebitRaw?.let { formatDate(it) } ?: "—"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(cardGradientColors))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.Transparent),
                            start = Offset(shimmerProgress * 800f - 400f, 0f),
                            end = Offset(shimmerProgress * 800f + 400f, 300f)
                        )
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = goalColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isMonthly) Icons.Default.CalendarMonth else Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = goalColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isMonthly) stringResource(Res.string.freq_monthly_label) else stringResource(Res.string.freq_daily_label),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
                                color = goalColor
                            )
                        }
                    }
                    Surface(color = statusBg, shape = RoundedCornerShape(50)) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.7.sp),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(Res.string.plan_number_label, planNumber),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = formatRupeeAmount(mandate.amount, 0),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-1.2).sp),
                        color = Color(0xFF3E2723)
                    )
                    Text(
                        text = if (isMonthly) stringResource(Res.string.per_month_suffix) else stringResource(Res.string.per_day_suffix),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF3E2723).copy(alpha = 0.45f)
                    )
                }
                Text(
                    text = cadenceDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF3E2723).copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                HorizontalDivider(color = Color(0xFFF0EBE7))

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SipDateCell(label = stringResource(Res.string.started_label), value = startedText, modifier = Modifier.weight(1f))
                    SipDateCell(label = stringResource(Res.string.next_debit_label), value = nextDebitText, modifier = Modifier.weight(1f))
                }

                if (isMonthly && sipDateOrdinal != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD4AF37).copy(alpha = 0.07f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF8B6B25),
                                modifier = Modifier.size(15.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(Res.string.monthly_debit_date_callout_title, sipDateOrdinal),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF8B6B25)
                                )
                                Text(
                                    text = stringResource(Res.string.monthly_debit_date_callout_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF3E2723).copy(alpha = 0.4f),
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }

                if (isActive && (onPause != null || onCancel != null)) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onPause != null) {
                            SipPlanActionPillV2(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Pause,
                                label = stringResource(Res.string.pause).trim(),
                                tint = Color(0xFF8B6B25),
                                background = Color(0xFFF5F0EA),
                                onClick = { onPause(mandate) }
                            )
                        }
                        if (onCancel != null) {
                            SipPlanActionPillV2(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Cancel,
                                label = stringResource(Res.string.cancel_sip).trim(),
                                tint = Color(0xFFC0392B),
                                background = Color(0xFFFFF0F0),
                                onClick = { onCancel(mandate) }
                            )
                        }
                    }
                } else if (isPaused && (onResume != null || onCancel != null)) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onResume != null) {
                            val resumeTint = Color(0xFFFFFFFF)
                            val resumeBg = Color(0xFF3F3F46)
                            val resumeBorder = BorderStroke(1.5.dp, goalColor)
                            SipPlanActionPillV2(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.PlayArrow,
                                label = stringResource(Res.string.resume_sip).trim(),
                                tint = resumeTint,
                                background = resumeBg,
                                border = resumeBorder,
                                onClick = { onResume(mandate) }
                            )
                        }
                        if (onCancel != null) {
                            SipPlanActionPillV2(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Cancel,
                                label = stringResource(Res.string.cancel_sip).trim(),
                                tint = Color(0xFFC0392B),
                                background = Color(0xFFFFF0F0),
                                onClick = { onCancel(mandate) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SipDateCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.90f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.7.sp),
            color = Color(0xFF3E2723).copy(alpha = 0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = Color(0xFF3E2723),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SipPlanActionPillV2(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    border: BorderStroke? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = background,
        shape = RoundedCornerShape(10.dp),
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = tint
            )
        }
    }
}

@Composable
private fun SipPlansTrustStripV2(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.70f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.90f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        SipTrustLine(stringResource(Res.string.sip_trust_flexibility))
        SipTrustLine(stringResource(Res.string.sip_trust_no_lockin))
    }
}

@Composable
private fun SipTrustLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF1A7A42),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF3E2723).copy(alpha = 0.55f)
        )
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

private fun getGradientForCategory(category: String?, colorTheme: String?): Brush {
    val baseColor = getCorrelationColorForCategory(category, colorTheme)
    val colors = when (category?.uppercase()) {
        "GOLD" -> listOf(Color(0xFFD4A017), Color(0xFFF6D365), Color(0xFFFFE082))
        "SILVER" -> listOf(Color(0xFF9AA0A6), Color(0xFFC7CDD4), Color(0xFFE5E7EB))
        "SAVINGS", "SAVINGS_PLUS" -> listOf(Color(0xFF0F9D58), Color(0xFF22C55E), Color(0xFF86EFAC))
        "GLOBAL_EXPOSURE", "GLOBAL" -> listOf(Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFF5EEAD4))
        "CHILDRENS_EDUCATION" -> listOf(Color(0xFF1565C0), Color(0xFF1A73E8), Color(0xFF64B5F6))
        "FESTIVAL_SPENDS" -> listOf(Color(0xFFE65100), Color(0xFFF2994A), Color(0xFFFFB74D))
        "VACATION" -> listOf(Color(0xFF6A1B9A), Color(0xFF8844EE), Color(0xFFCE93D8))
        "ALL_IN_ONE" -> listOf(Color(0xFF283593), Color(0xFF3F51B5), Color(0xFF9FA8DA))
        "MARKET_EXPLORER" -> listOf(Color(0xFF0F6B5C), Color(0xFF148B75), Color(0xFFE4F3EE))
        "INNOVATION" -> listOf(Color(0xFF7A5F9E), Color(0xFF876DAF), Color(0xFFF1ECF8))
        "SENSEX" -> listOf(Color(0xFF0F172A), Color(0xFF2346B5), Color(0xFFEFF6FF))
        else -> listOf(baseColor.copy(alpha = 0.8f), baseColor)
    }
    return Brush.verticalGradient(colors)
}


private fun formatRupeeAmount(value: Double, decimals: Int = 1): String {
    val formatted = if (decimals == 0) {
        formatIndian(value)
    } else {
        val factor = 10.0.pow(decimals)
        val roundedValue = kotlin.math.round(value * factor) / factor
        val parts = roundedValue.toString().split(".")
        val intPart = formatIndian(parts[0].toDoubleOrNull() ?: 0.0)
        val decPart = parts.getOrNull(1)?.take(decimals)?.padEnd(decimals, '0') ?: "0".repeat(decimals)
        if (decimals > 0 && decPart.all { it == '0' }) {
            intPart
        } else {
            "$intPart.$decPart"
        }
    }
    val wordJoiner = "\u2060"
    return "₹$wordJoiner${formatted.map { "$it$wordJoiner" }.joinToString("")}"
}

private fun formatGainRupeeAmount(value: Double, decimals: Int = 1): String {
    val wordJoiner = "\u2060"
    val displayValue = abs(value)
    
    val formatted = if (decimals == 0) {
        formatIndian(displayValue)
    } else {
        val factor = 10.0.pow(decimals)
        val roundedValue = kotlin.math.round(displayValue * factor) / factor
        val parts = roundedValue.toString().split(".")
        val intPart = formatIndian(parts[0].toDoubleOrNull() ?: 0.0)
        val decPart = parts.getOrNull(1)?.take(decimals)?.padEnd(decimals, '0') ?: "0".repeat(decimals)
        if (decimals > 0 && decPart.all { it == '0' }) {
            intPart
        } else {
            "$intPart.$decPart"
        }
    }
    val amountPart = formatted.map { "$it$wordJoiner" }.joinToString("")
    return when {
        value > 0 -> "+₹$wordJoiner$amountPart"
        value < 0 -> "-₹$wordJoiner$amountPart"
        else -> "₹$wordJoiner$amountPart"
    }
}

@Composable
private fun RupeeAmountBlock(
    value: Double,
    style: TextStyle,
    color: Color,
    negativeBeforeRupee: Boolean = false,
    showPlusForPositive: Boolean = false,
    decimalPlaces: Int = 1
) {
    val isNegative = value < 0
    val isPositive = value > 0
    val displayValue = if (negativeBeforeRupee && isNegative) abs(value) else value

    val formattedNumber = if (decimalPlaces == 0) {
        formatIndian(displayValue)
    } else {
        val factor = 10.0.pow(decimalPlaces)
        val roundedValue = kotlin.math.round(displayValue * factor) / factor
        val parts = roundedValue.toString().split(".")
        val intPart = formatIndian(parts[0].toDoubleOrNull() ?: 0.0)
        val decPart = parts.getOrNull(1)?.take(decimalPlaces)?.padEnd(decimalPlaces, '0') ?: "0".repeat(decimalPlaces)
        if (decimalPlaces > 0 && decPart.all { it == '0' }) {
            intPart
        } else {
            "$intPart.$decPart"
        }
    }
    Row(
        modifier = Modifier.wrapContentWidth(Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (negativeBeforeRupee && isNegative) {
            Text(text = "-", style = style, color = color)
        }
        if (showPlusForPositive && isPositive) {
            Text(text = "+", style = style, color = color)
        }
        Text(text = "₹", style = style, color = color)
        Text(
            text = formattedNumber,
            style = style,
            color = color,
            softWrap = false
        )
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
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = debouncedClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun StatusPill(
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black.copy(alpha=0.6f)
            )
        }
    }
}

@Composable
fun DashboardTile(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color = Color.White,
    iconColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.2.dp, iconColor.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(24.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SipDateDetailsDisplay(mandate: MandateDisplayItem?, modifier: Modifier = Modifier) {
    if (mandate == null) return
    val isDaily = mandate.frequency?.uppercase() == "DAILY"
    if (!isDaily) return
    val firstDebitDateStr = mandate.firstDebitDate ?: mandate.nextSipDate
    val createdDateStr = mandate.mandateCreatedDate ?: mandate.mandateApprovedDate
    val calculatedAllocationDateStr = mandate.calculatedFirstUnitAllocationDate

    val isFirstDebitPassed = isDatePassed(firstDebitDateStr)
    val isAllocationPassed = isDatePassed(calculatedAllocationDateStr)

    val showFirstDebitNotPassed = !firstDebitDateStr.isNullOrBlank() && !isFirstDebitPassed
    val showAllocationNotPassed = isFirstDebitPassed && !calculatedAllocationDateStr.isNullOrBlank() && !isAllocationPassed

    if (showFirstDebitNotPassed || showAllocationNotPassed) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBF9F4)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF3E2723).copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showFirstDebitNotPassed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plan created on",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF3E2723).copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatDate(createdDateStr).ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3E2723)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "First debit on",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF3E2723).copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatDate(firstDebitDateStr).ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3E2723)
                        )
                    }
                } else if (showAllocationNotPassed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "First debit on",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF3E2723).copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatDate(firstDebitDateStr).ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3E2723)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "First unit allocation will be by",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF3E2723).copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatDate(calculatedAllocationDateStr).ifBlank { "—" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3E2723)
                        )
                    }
                }
            }
        }
    }
}

private fun isDatePassed(dateString: String?): Boolean {
    if (dateString.isNullOrBlank() || dateString == "null") return false
    return try {
        val datePart = dateString.substringBefore("T")
        val targetDate = LocalDate.parse(datePart)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        targetDate < today
    } catch (e: Exception) {
        false
    }
}

private fun formatExpectedDate(dateString: String?): String {
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
        "$day $monthName"
    } catch (e: Exception) {
        dateString ?: ""
    }
}

private fun getNextBusinessDay(startDate: LocalDate, businessDaysToAdd: Int): LocalDate {
    var date = startDate
    var added = 0
    while (added < businessDaysToAdd) {
        date = LocalDate.fromEpochDays(date.toEpochDays() + 1)
        if (date.dayOfWeek != kotlinx.datetime.DayOfWeek.SATURDAY && date.dayOfWeek != kotlinx.datetime.DayOfWeek.SUNDAY) {
            added++
        }
    }
    return date
}

private fun getPreviousBusinessDay(startDate: LocalDate, businessDaysToSubtract: Int): LocalDate {
    var date = startDate
    var subtracted = 0
    while (subtracted < businessDaysToSubtract) {
        date = LocalDate.fromEpochDays(date.toEpochDays() - 1)
        if (date.dayOfWeek != kotlinx.datetime.DayOfWeek.SATURDAY && date.dayOfWeek != kotlinx.datetime.DayOfWeek.SUNDAY) {
            subtracted++
        }
    }
    return date
}

@Composable
private fun SetupProgressStepRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isInProgress: Boolean,
    overrideStatusText: String? = null,
    goalColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val (badgeBg, iconOrDotColor) = when {
            isCompleted -> Pair(Color(0xFF2E7D32), Color.White)
            isInProgress -> Pair(goalColor, Color.White)
            else -> Pair(Color(0xFFECEFF1), Color(0xFF78909C))
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(badgeBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isInProgress) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(iconOrDotColor, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .border(1.5.dp, iconOrDotColor, CircleShape)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1E293B)
                )
                val statusText = overrideStatusText ?: when {
                    isCompleted -> stringResource(Res.string.status_done)
                    isInProgress -> stringResource(Res.string.status_in_progress)
                    else -> stringResource(Res.string.status_upcoming)
                }
                val statusColor = when {
                    isCompleted -> Color(0xFF2E7D32)
                    isInProgress -> goalColor
                    else -> Color(0xFF8D6E63)
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun ChecklistRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF475569)
        )
    }
}

