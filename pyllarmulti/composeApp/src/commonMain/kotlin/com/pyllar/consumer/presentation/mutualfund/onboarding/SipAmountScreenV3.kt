package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.domain.storage.InMemorySessionStore
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.presentation.mutualfund.details.BankDetailsCard
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.details.FundHeader
import com.pyllar.consumer.presentation.mutualfund.details.SipCreationResult
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.presentation.ui.theme.*
import com.pyllar.consumer.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TrustStrip
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*
import kotlin.math.roundToInt

enum class SipFrequency { DAILY, MONTHLY }

private val SipHeroObsidian = Color(0xFF0A2415)
private val SipInk = Color(0xFF3E2723)
private val SipSubtleBorder = Color(0xFFEFEBE9)
private val SipGold = Color(0xFFD4AF37)
private val SipGoldDeep = Color(0xFF8B6B25)

private const val MONTHLY_MIN = 1000f
private const val MONTHLY_MAX = 15000f
private const val MONTHLY_DEFAULT = 5000f
private const val MONTHLY_POPULAR_AMOUNT = 5000
private val MONTHLY_PRESETS = listOf(1000, 5000, 15000)
private const val PAST_PERF_BAR_MAX_HEIGHT_DP = 68

private data class SipGoalTheme(
    val accentColor: Color,
    val eyebrowColor: Color,
    val shimmerColor: Color,
    val dividerColor: Color,
    val barSelectedGradient: List<Color>
)

private fun sipGoalTheme(goalType: GoalType): SipGoalTheme = when (goalType) {
    GoalType.GOLD -> SipGoalTheme(
        accentColor = SipGold,
        eyebrowColor = SipGold.copy(alpha = 0.6f),
        shimmerColor = Color(0xFFFFE082).copy(alpha = 0.10f),
        dividerColor = SipGold.copy(alpha = 0.18f),
        barSelectedGradient = listOf(SipGold, Color(0xFFFFF3A0))
    )
    GoalType.SILVER -> SipGoalTheme(
        accentColor = Color(0xFFC8DCE8),
        eyebrowColor = Color(0xFFB4D2E4).copy(alpha = 0.65f),
        shimmerColor = Color(0xFFC8DCF0).copy(alpha = 0.10f),
        dividerColor = Color(0xFFB4D2E4).copy(alpha = 0.18f),
        barSelectedGradient = listOf(Color(0xFFA8C8DC), Color(0xFFE8F4FA))
    )
    else -> SipGoalTheme(
        accentColor = Color(0xFF4ADE80),
        eyebrowColor = Color(0xFF4ADE80).copy(alpha = 0.65f),
        shimmerColor = Color(0xFF64F0A0).copy(alpha = 0.10f),
        dividerColor = Color(0xFF4ADE80).copy(alpha = 0.18f),
        barSelectedGradient = listOf(Color(0xFF27AE60), Color(0xFFA8F5C8))
    )
}

private fun formatGrams(grams: Double): String = when {
    grams >= 1000 -> "${formatDecimal(grams / 1000, 2)} kg"
    grams >= 100 -> "${grams.roundToInt()}g"
    grams >= 10 -> "${formatDecimal(grams, 1)}g"
    else -> "${formatDecimal(grams, 2)}g"
}

private fun ordinalSuffix(n: Int): String {
    if (n in 11..13) return "${n}th"
    return when (n % 10) {
        1 -> "${n}st"
        2 -> "${n}nd"
        3 -> "${n}rd"
        else -> "${n}th"
    }
}

private fun maxDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

private fun getMonthlySipStartInfo(sipDate: Int): Pair<String, String> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val title = "SIP runs on the ${ordinalSuffix(sipDate)} every month"
    
    val firstDebitDate = if (today.dayOfMonth < sipDate) {
        val maxDays = maxDaysInMonth(today.year, today.monthNumber)
        val clampedDay = sipDate.coerceAtMost(maxDays)
        LocalDate(today.year, today.monthNumber, clampedDay)
    } else {
        val nextMonthNum = if (today.monthNumber == 12) 1 else today.monthNumber + 1
        val nextYear = if (today.monthNumber == 12) today.year + 1 else today.year
        val maxDays = maxDaysInMonth(nextYear, nextMonthNum)
        val clampedDay = sipDate.coerceAtMost(maxDays)
        LocalDate(nextYear, nextMonthNum, clampedDay)
    }
    
    val monthName = getMonthName(firstDebitDate.monthNumber)
    val subtitle = "First debit: ${ordinalSuffix(firstDebitDate.dayOfMonth)} $monthName"
    return title to subtitle
}

private fun calculateYearsDifference(startYearMonth: String?, endYearMonth: String?): Int {
    if (startYearMonth.isNullOrBlank() || endYearMonth.isNullOrBlank()) return 5
    return try {
        val startParts = startYearMonth.split("-")
        val endParts = endYearMonth.split("-")
        val startYear = startParts[0].toInt()
        val startMonth = startParts[1].toInt()
        val endYear = endParts[0].toInt()
        val endMonth = endParts[1].toInt()
        val diffMonths = (endYear - startYear) * 12 + (endMonth - startMonth)
        val yrs = (diffMonths / 12.0).roundToInt()
        if (yrs > 0) yrs else 1
    } catch (e: Exception) {
        5
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SipAmountScreenV3(
    userId: String,
    kycAttemptId: String = "",
    investorId: String = "",
    goalId: String = "",
    isExistingInvestment: Boolean = false,
    onSipCreated: (Double, String?, MandateWrapper?) -> Unit = { _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    onForceLogout: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToFundDetails: (userId: String, goalId: String, amount: Double, kycAttemptId: String, investorId: String, frequency: String, installmentDay: Int?) -> Unit = { _, _, _, _, _, _, _ -> },
    viewModel: SipAmountScreenV2ViewModel = koinInject(),
    fundDetailsViewModel: FundDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject(),
    sessionStore: SessionStore = koinInject(),
    inMemorySessionStore: InMemorySessionStore = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val limitsState by viewModel.limitsState.collectAsState()
    val fundDetailsState by fundDetailsViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current

    var effectiveUserId by remember(userId) { mutableStateOf(userId) }
    var effectiveKycAttemptId by remember(kycAttemptId) { mutableStateOf(kycAttemptId) }
    var effectiveInvestorId by remember(investorId) { mutableStateOf(investorId) }
    var effectiveGoalId by remember(goalId) { mutableStateOf(goalId) }
    var isFetchingIds by remember { mutableStateOf(false) }
    var isInitializing by remember { mutableStateOf(true) }
    var isInitTxnLoading by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<String?>(null) }
    var showUnexpectedErrorDialog by remember { mutableStateOf(false) }
    var showTrustStripInfoDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

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

    var monthlyAmount by remember { mutableStateOf(MONTHLY_DEFAULT) }
    var monthlyAmountText by remember {
        val initial = MONTHLY_DEFAULT.toInt().toString()
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    var isMonthlyCustomMode by remember { mutableStateOf(false) }

    var sipDate by remember { mutableStateOf(5) }
    var frequency by remember { mutableStateOf(SipFrequency.DAILY) }
    val isMonthly = frequency == SipFrequency.MONTHLY

    LaunchedEffect(userId, goalId, kycAttemptId, investorId) {
        if (userId.isNotBlank()) effectiveUserId = userId
        if (goalId.isNotBlank()) effectiveGoalId = goalId
        if (kycAttemptId.isNotBlank()) effectiveKycAttemptId = kycAttemptId
        if (investorId.isNotBlank()) effectiveInvestorId = investorId
    }

    LaunchedEffect(Unit) {
        try {
            isFetchingIds = true
            if (userId.isBlank()) {
                val storedUserId = sessionStore.getCurrentUserId()
                if (storedUserId.isNotBlank()) effectiveUserId = storedUserId
            }
            if (goalId.isBlank()) {
                val storedGoalId = sessionStore.getValue(KeyValueConstants.SELECTED_GOAL_ID) ?: ""
                if (storedGoalId.isNotBlank()) effectiveGoalId = storedGoalId
            }
            if (effectiveKycAttemptId.isBlank() || effectiveInvestorId.isBlank()) {
                val storedKycId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                val storedInvId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""
                if (effectiveKycAttemptId.isBlank() && storedKycId.isNotBlank()) effectiveKycAttemptId = storedKycId
                if (effectiveInvestorId.isBlank() && storedInvId.isNotBlank()) effectiveInvestorId = storedInvId
            }

            val savedGoalId = inMemorySessionStore.getValue("selected_sip_goal_id") ?: ""
            if (savedGoalId.isNotBlank() && savedGoalId == effectiveGoalId) {
                inMemorySessionStore.getValue("selected_sip_amount")?.toFloatOrNull()?.let {
                    amount = it
                    val textVal = it.toInt().toString()
                    amountText = TextFieldValue(textVal, TextRange(textVal.length))
                }
                inMemorySessionStore.getValue("selected_sip_monthly_amount")?.toFloatOrNull()?.let {
                    monthlyAmount = it
                    val textVal = it.toInt().toString()
                    monthlyAmountText = TextFieldValue(textVal, TextRange(textVal.length))
                }
                inMemorySessionStore.getValue("selected_sip_frequency")?.let {
                    if (it == "monthly") frequency = SipFrequency.MONTHLY
                    else if (it == "daily") frequency = SipFrequency.DAILY
                }
                inMemorySessionStore.getValue("selected_sip_date")?.toIntOrNull()?.let {
                    sipDate = it
                }
            } else {
                inMemorySessionStore.saveValue("selected_sip_amount", "")
                inMemorySessionStore.saveValue("selected_sip_monthly_amount", "")
                inMemorySessionStore.saveValue("selected_sip_frequency", "")
                inMemorySessionStore.saveValue("selected_sip_date", "")
                inMemorySessionStore.saveValue("selected_sip_goal_id", effectiveGoalId)
            }
        } finally {
            isFetchingIds = false
            isInitializing = false
        }
    }


    var showDetailsBottomSheet by remember { mutableStateOf(false) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    var isSheetLoading by remember { mutableStateOf(false) }
    var sheetError by remember { mutableStateOf<String?>(null) }

    var resolvedGoalType by remember(effectiveGoalId) { mutableStateOf(identifyGoalType(effectiveGoalId)) }
    val theme = remember(resolvedGoalType) { sipGoalTheme(resolvedGoalType) }

    val areDetailsLoaded = !fundDetailsState.isLoading &&
            fundDetailsState.fundDetails != null &&
            !fundDetailsState.pastPerformanceLoading &&
            fundDetailsState.pastPerformance != null &&
            !isInitTxnLoading &&
            !dashboardState.isLoading

    LaunchedEffect(effectiveUserId) {
        if (effectiveUserId.isNotBlank()) {
            dashboardViewModel.loadDashboardData(effectiveUserId)
        }
    }

    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveUserId.isNotBlank() && effectiveGoalId.isNotBlank()) {
            fundDetailsViewModel.loadFundDetailsByGoal(effectiveUserId, effectiveGoalId)
            fundDetailsViewModel.loadPastPerformance(effectiveUserId, effectiveGoalId)
        }
    }

    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveUserId.isNotBlank() && effectiveGoalId.isNotBlank()) {
            isInitTxnLoading = true
            try {
                when (val result = dashboardViewModel.initGoalTxn(effectiveUserId, effectiveGoalId)) {
                    is Resource.Success -> {
                        result.data?.let { response ->
                            if (response.investmentPurpose.isNotBlank()) {
                                resolvedGoalType = identifyGoalType(response.investmentPurpose)
                            }
                            if (response.userPurposeId.isNotBlank()) {
                                sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                                viewModel.fetchInvestmentLimits(response.userPurposeId)
                            }
                        }
                    }
                    is Resource.Error -> {
                        viewModel.fetchInvestmentLimits(effectiveGoalId)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                viewModel.fetchInvestmentLimits(effectiveGoalId)
            } finally {
                isInitTxnLoading = false
            }
        } else if (effectiveGoalId.isNotBlank()) {
            viewModel.fetchInvestmentLimits(effectiveGoalId)
        }
    }

    LaunchedEffect(targetAmount, limitsState.isLoading) {
        if (!limitsState.isLoading) {
            val restoredAmount = inMemorySessionStore.getValue("selected_sip_amount")?.toFloatOrNull()
            if (restoredAmount != null) {
                amount = restoredAmount
                val newText = restoredAmount.toInt().toString()
                amountText = TextFieldValue(newText, TextRange(newText.length))
            } else {
                amount = targetAmount
                val newText = targetAmount.toInt().toString()
                amountText = TextFieldValue(newText, TextRange(newText.length))
            }
        }
    }

    val currentAmount = if (isMonthly) monthlyAmount else amount
    val currentMin = if (isMonthly) MONTHLY_MIN else minAmount
    val currentMax = if (isMonthly) MONTHLY_MAX else maxAmount

    val pastPerformance = fundDetailsState.pastPerformance
    val pastPerformanceLoading = fundDetailsState.pastPerformanceLoading
    val baseUnitAmount = pastPerformance?.baseUnitAmount ?: 100.0

    fun scalePastPerformanceValue(baselineValue: Double): Double =
        baselineValue * (currentAmount.toDouble() / baseUnitAmount)

    val navMilestones = pastPerformance?.milestones.orEmpty()
    val latestMilestone = navMilestones.find { it.latest }
    val navCurrentVal = latestMilestone?.let {
        scalePastPerformanceValue(if (isMonthly) it.monthlyBaselinePortfolioValue else it.dailyBaselinePortfolioValue)
    } ?: 0.0
    val navInvestedVal = latestMilestone?.let {
        scalePastPerformanceValue(if (isMonthly) it.monthlyBaselineInvestedValue else it.dailyBaselineInvestedValue)
    } ?: 0.0
    val navGainVal = navCurrentVal - navInvestedVal
    val navGainPct = if (navInvestedVal > 0) ((navGainVal / navInvestedVal) * 100).roundToInt() else 0

    val gramsValueLabel = pastPerformance?.metalName?.let {
        val baselineGrams = latestMilestone?.let {
            if (isMonthly) it.monthlyBaselineGrams else it.dailyBaselineGrams
        }
        baselineGrams?.let { grams -> formatGrams(scalePastPerformanceValue(grams)) }
    }
    val gramsSuffixLabel = pastPerformance?.metalName?.let { metalName ->
        stringResource(Res.string.grams_accumulated_suffix, metalName)
    }

    val shimmerTransition = rememberInfiniteTransition()
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scrollState = rememberScrollState()
    val isFetching = isInitializing || isInitTxnLoading || limitsState.isLoading || fundDetailsState.isLoading || isFetchingIds

    val handleBack: () -> Unit = {
        coroutineScope.launch {
            inMemorySessionStore.saveValue("selected_sip_amount", "")
            inMemorySessionStore.saveValue("selected_sip_monthly_amount", "")
            inMemorySessionStore.saveValue("selected_sip_frequency", "")
            inMemorySessionStore.saveValue("selected_sip_date", "")
            inMemorySessionStore.saveValue("selected_sip_goal_id", "")
        }
        onNavigateBack()
    }

    com.pyllar.consumer.util.BackHandler {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 2.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Pyllar ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SipHeroObsidian, letterSpacing = (-0.5).sp)
                        Text(text = "Money", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SipGold, letterSpacing = (-0.5).sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "Gold, Silver & much more starting at ₹21! ✨\n\nSmall daily steps ➡️ Big rewards. Join me and build a consistent saving habit for your goals with Pyllar.\n\nStart today: https://pyllar.in/download.html"
                        platformActions.shareText(shareText)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.content_description_share_icon), tint = MaterialTheme.colorScheme.primary)
                    }
                    LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onNavigateToHelp) {
                        Text(stringResource(Res.string.help), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700), SipGoldDeep)))
                        .padding(1.5.dp)
                ) {
                    Button(
                        onClick = {
                            if (isSheetLoading) return@Button
                            showDetailsBottomSheet = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isSheetLoading && !isInitTxnLoading && areDetailsLoaded,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SipHeroObsidian,
                            contentColor = Color.White,
                            disabledContainerColor = SipHeroObsidian.copy(alpha = 0.55f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isSheetLoading || !areDetailsLoaded) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSheetLoading) stringResource(Res.string.submitting) else "Fetching details...")
                        } else {
                            Text(stringResource(Res.string.start_sip))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                TrustStrip(onInfoClick = { showTrustStripInfoDialog = true })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (limitsState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.9f))
                        .zIndex(10f)
                ) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                        .padding(bottom = 24.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ── PAST PERFORMANCE CARD ────────────────────────────────
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(SipHeroObsidian)
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color.Transparent, theme.shimmerColor, Color.Transparent),
                                                start = Offset(shimmerProgress * 600f - 300f, 0f),
                                                end = Offset(shimmerProgress * 600f + 300f, 400f)
                                            )
                                        )
                                )
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column {
                                            val yrsLabel = if (!pastPerformanceLoading && pastPerformance != null) {
                                                val yrs = calculateYearsDifference(pastPerformance.startYearMonth, pastPerformance.asOfYearMonth)
                                                "$yrs YR PAST PERFORMANCE"
                                            } else {
                                                stringResource(Res.string.past_performance_label)
                                            }
                                            Text(
                                                text = yrsLabel,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.4.sp,
                                                color = theme.eyebrowColor
                                            )
                                            if (!pastPerformanceLoading && pastPerformance != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isMonthly) {
                                                        stringResource(Res.string.past_performance_amount_monthly, currentAmount.toInt())
                                                    } else {
                                                        stringResource(Res.string.past_performance_amount_daily, currentAmount.toInt())
                                                    },
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
//                                                Spacer(modifier = Modifier.height(4.dp))
//                                                Text(
//                                                    text = stringResource(Res.string.past_performance_since_label, pastPerformance.startLabel),
//                                                    fontSize = 12.sp,
//                                                    fontWeight = FontWeight.Medium,
//                                                    color = Color.White.copy(alpha = 0.85f)
//                                                )
                                            }
                                        }
                                        if (!pastPerformanceLoading && pastPerformance != null) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = formatRupeesShort(navCurrentVal),
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.accentColor
                                                )
                                                Text(
                                                    text = stringResource(Res.string.past_performance_actual_returns, navGainPct),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6FCF97)
                                                )
                                            }
                                        }
                                    }
                                    if (!pastPerformanceLoading && pastPerformance != null && gramsValueLabel != null && gramsSuffixLabel != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = gramsValueLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = theme.accentColor
                                            )
                                            Text(
                                                text = " $gramsSuffixLabel",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = theme.accentColor.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (pastPerformanceLoading || pastPerformance == null) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(150.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = theme.accentColor, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(theme.dividerColor)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val maxMilestoneVal = navMilestones.maxOfOrNull {
                                            scalePastPerformanceValue(if (isMonthly) it.monthlyBaselinePortfolioValue else it.dailyBaselinePortfolioValue)
                                        } ?: 1.0
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            navMilestones.forEach { milestone ->
                                                val milestoneValue = scalePastPerformanceValue(
                                                    if (isMonthly) milestone.monthlyBaselinePortfolioValue else milestone.dailyBaselinePortfolioValue
                                                )
                                                val barHeight = (((milestoneValue / maxMilestoneVal) * PAST_PERF_BAR_MAX_HEIGHT_DP))
                                                    .coerceAtLeast(14.0).dp
                                                PastPerformanceBar(
                                                    dateLabel = milestone.dateLabel,
                                                    valueLabel = formatRupeesShort(milestoneValue),
                                                    barHeightDp = barHeight,
                                                    isLatest = milestone.latest,
                                                    theme = theme,
                                                    modifier = Modifier.weight(if (milestone.latest) 1.6f else 1f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(Res.string.past_performance_footnote, pastPerformance.fundLabel),
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ── FREQUENCY TOGGLE ─────────────────────────────────────
                        SipFrequencyToggle(
                            frequency = frequency,
                            onFrequencyChange = { newFrequency ->
                                frequency = newFrequency
                                isMonthlyCustomMode = false
                                coroutineScope.launch {
                                    inMemorySessionStore.saveValue("selected_sip_frequency", if (newFrequency == SipFrequency.MONTHLY) "monthly" else "daily")
                                }
                            }
                        )

                        // ── AMOUNT CARD ───────────────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                var titleTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var rangeTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                val density = LocalDensity.current
                                val goalTitle = "${getGoalDisplayName(resolvedGoalType)} SIP Amount"
                                val rangeLabel = stringResource(Res.string.sip_amount_range_dynamic, currentMin.toInt(), currentMax.toInt())

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val scopeMaxWidth = this.maxWidth
                                    val needsWrap = remember(titleTextLayoutResult, rangeTextLayoutResult, scopeMaxWidth) {
                                        if (titleTextLayoutResult == null || rangeTextLayoutResult == null) {
                                            false
                                        } else {
                                            val titleWidthPx = titleTextLayoutResult!!.size.width
                                            val rangeWidthPx = rangeTextLayoutResult!!.size.width
                                            val spacingPx = with(density) { 16.dp.toPx() }
                                            val containerWidthPx = with(density) { scopeMaxWidth.toPx() }
                                            (titleWidthPx + rangeWidthPx + spacingPx) > containerWidthPx
                                        }
                                    }
                                    if (needsWrap) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = goalTitle,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SipInk,
                                                onTextLayout = { titleTextLayoutResult = it }
                                            )
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                Text(
                                                    text = rangeLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SipInk.copy(alpha = 0.42f),
                                                    onTextLayout = { rangeTextLayoutResult = it }
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = goalTitle,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SipInk,
                                                modifier = Modifier.weight(1f, fill = false),
                                                onTextLayout = { titleTextLayoutResult = it }
                                            )
                                            Text(
                                                text = rangeLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SipInk.copy(alpha = 0.42f),
                                                onTextLayout = { rangeTextLayoutResult = it }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (!isMonthly) {
                                    DailyAmountSection(
                                        amount = amount,
                                        onAmountChange = {
                                            amount = it
                                            coroutineScope.launch {
                                                inMemorySessionStore.saveValue("selected_sip_amount", it.toString())
                                            }
                                        },
                                        amountText = amountText,
                                        onAmountTextChange = {
                                            amountText = it
                                            val numeric = it.text.filter { c -> c.isDigit() }.toFloatOrNull()
                                            if (numeric != null) {
                                                coroutineScope.launch {
                                                    inMemorySessionStore.saveValue("selected_sip_amount", numeric.toString())
                                                }
                                            }
                                        },
                                        minAmount = minAmount,
                                        maxAmount = maxAmount,
                                        defaultAmount = defaultAmount,
                                        goalType = resolvedGoalType,
                                        accentColor = theme.accentColor,
                                        coroutineScope = coroutineScope,
                                        focusManager = focusManager,
                                        keyboardController = keyboardController,
                                        scrollState = scrollState
                                    )
                                } else {
                                    MonthlyAmountSection(
                                        monthlyAmount = monthlyAmount,
                                        onMonthlyAmountChange = {
                                            monthlyAmount = it
                                            coroutineScope.launch {
                                                inMemorySessionStore.saveValue("selected_sip_monthly_amount", it.toString())
                                            }
                                        },
                                        monthlyAmountText = monthlyAmountText,
                                        onMonthlyAmountTextChange = {
                                            monthlyAmountText = it
                                            val numeric = it.text.filter { c -> c.isDigit() }.toFloatOrNull()
                                            if (numeric != null) {
                                                coroutineScope.launch {
                                                    inMemorySessionStore.saveValue("selected_sip_monthly_amount", numeric.toString())
                                                }
                                            }
                                        },
                                        isMonthlyCustomMode = isMonthlyCustomMode,
                                        onMonthlyCustomModeChange = { isMonthlyCustomMode = it },
                                        accentColor = theme.accentColor,
                                        coroutineScope = coroutineScope,
                                        focusManager = focusManager,
                                        keyboardController = keyboardController,
                                        scrollState = scrollState
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SipSubtleBorder))
                                    Spacer(modifier = Modifier.height(18.dp))

                                    MonthlyDatePicker(
                                        selectedDate = sipDate,
                                        accentColor = theme.accentColor,
                                        onDateSelected = {
                                            sipDate = it
                                            coroutineScope.launch {
                                                inMemorySessionStore.saveValue("selected_sip_date", it.toString())
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // ── SIP START INFO CARD ───────────────────────────────────
                        if (!isMonthly) {
                            SipStartInfoCard(
                                title = "SIP starts ${getInvestmentStatus()}",
                                subtitle = "First Debit by ${getNextAllocationDayName()}, 8 am"
                            )
                        } else {
                            val (monthlyTitle, monthlySubtitle) = getMonthlySipStartInfo(sipDate)
                            SipStartInfoCard(title = monthlyTitle, subtitle = monthlySubtitle)
                        }

                        // ── SCHEME CARD ──────────────────────────────────────────
                        SipSchemeCard(
                            fundName = fundDetailsState.fundDetails?.fundName,
                            category = fundDetailsState.fundDetails?.category,
                            isLoading = fundDetailsState.isLoading,
                            enabled = areDetailsLoaded,
                            onClick = {
                                val currentFreq = if (isMonthly) "monthly" else "daily"
                                val currentInstalmentDay = if (isMonthly) sipDate else null
                                onNavigateToFundDetails(
                                    effectiveUserId,
                                    effectiveGoalId,
                                    currentAmount.toDouble(),
                                    effectiveKycAttemptId,
                                    effectiveInvestorId,
                                    currentFreq,
                                    currentInstalmentDay
                                )
                            }
                        )

                        // ── TRUST CHECKLIST ───────────────────────────────────────
                        SipTrustChecklist()

                        if (submitResult != null) {
                            val errorMsg = submitResult!!
                            val isNetworkError = errorMsg.contains("Network", ignoreCase = true) ||
                                    errorMsg.contains("timeout", ignoreCase = true) ||
                                    errorMsg.contains("connection", ignoreCase = true) ||
                                    errorMsg.contains("Failed to connect", ignoreCase = true) ||
                                    errorMsg.contains("IOException", ignoreCase = true)
                            val displayMessage = if (errorMsg.contains("success", true)) {
                                "SIP created successfully!"
                            } else if (isNetworkError) {
                                stringResource(Res.string.check_internet_connection)
                            } else {
                                "Failed to create SIP. Please try again."
                            }
                            Text(
                                text = displayMessage,
                                color = if (submitResult!!.contains("success", true)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showUnexpectedErrorDialog) {
            AlertDialog(
                onDismissRequest = { showUnexpectedErrorDialog = false },
                title = { Text(stringResource(Res.string.unexpected_error_title)) },
                text = { Text(stringResource(Res.string.unexpected_error_message)) },
                confirmButton = {
                    TextButton(onClick = { showUnexpectedErrorDialog = false }) {
                        Text(stringResource(Res.string.retry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showUnexpectedErrorDialog = false
                        onForceLogout()
                    }) {
                        Text(stringResource(Res.string.logout_and_login))
                    }
                }
            )
        }

        if (showTrustStripInfoDialog) {
            AlertDialog(
                onDismissRequest = { showTrustStripInfoDialog = false },
                title = { Text(stringResource(Res.string.about_amcs)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(Res.string.truststrip_info_paragraph_1))
                        Text(stringResource(Res.string.truststrip_info_paragraph_2))
                        Text(stringResource(Res.string.truststrip_info_paragraph_3))
                        Text(stringResource(Res.string.truststrip_info_paragraph_4))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTrustStripInfoDialog = false }) {
                        Text(stringResource(Res.string.ok))
                    }
                }
            )
        }
    }

    if (showDetailsBottomSheet) {
        FundDetailsBottomSheet(
            amount = currentAmount.toDouble(),
            goalType = resolvedGoalType,
            fundDetailsState = fundDetailsState,
            isMonthly = isMonthly,
            sipDate = sipDate,
            isSheetLoading = isSheetLoading,
            sheetError = sheetError,
            areDetailsLoaded = areDetailsLoaded,
            onConfirm = {
                val kycStatus = dashboardState.kycStatus
                val isKycPending = !dashboardState.isLoading &&
                        (kycStatus.equals("PENDING", ignoreCase = true) ||
                         kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                         kycStatus.equals("EXPIRED", ignoreCase = true))

                if (isKycPending) {
                    showDetailsBottomSheet = false
                    showKycPendingBottomSheet = true
                    return@FundDetailsBottomSheet
                }

                coroutineScope.launch {
                    isSheetLoading = true
                    sheetError = null
                    try {
                        val result = if (isMonthly) {
                            fundDetailsViewModel.createPurchasePlan(
                                userId = effectiveUserId,
                                kycAttemptId = effectiveKycAttemptId,
                                investorId = effectiveInvestorId,
                                amount = monthlyAmount.toDouble(),
                                frequency = "monthly",
                                installmentDay = sipDate
                            )
                        } else {
                            viewModel.createSip(
                                userId = effectiveUserId,
                                kycAttemptId = effectiveKycAttemptId,
                                investorId = effectiveInvestorId,
                                amount = amount.toDouble()
                            )
                        }

                        when (result) {
                            is SipCreationResult.Success -> {
                                isSheetLoading = false
                                showDetailsBottomSheet = false
                                onSipCreated(currentAmount.toDouble(), result.nextScreen, result.mandateWrapper)
                            }
                            is SipCreationResult.Failure -> {
                                isSheetLoading = false
                                sheetError = result.message
                            }
                            else -> {
                                isSheetLoading = false
                                sheetError = "Unexpected operation result."
                            }
                        }
                    } catch (e: Exception) {
                        isSheetLoading = false
                        sheetError = "An unexpected error occurred: ${e.message}"
                    }
                }
            },
            onDismiss = { showDetailsBottomSheet = false },
            showDisclaimerDialog = { showDisclaimerDialog = true },
            uriHandler = uriHandler
        )
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text(stringResource(Res.string.disclaimer)) },
            text = {
                Column {
                    Text(stringResource(Res.string.gold_silver_price_disclaimer))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(Res.string.mutual_fund_risk_disclaimer))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.amfi_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showKycPendingBottomSheet) {
        com.pyllar.consumer.presentation.dashboard.KycPendingBottomSheet(
            onDismiss = { showKycPendingBottomSheet = false },
            onRetryKyc = { showKycPendingBottomSheet = false },
            kycStatus = dashboardState.kycStatus
        )
    }
}

@Composable
private fun SipFrequencyToggle(
    frequency: SipFrequency,
    onFrequencyChange: (SipFrequency) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9999.dp))
            .background(Color(0xFFEFEBE9))
            .padding(3.dp)
    ) {
        SipFrequencyTab(
            label = stringResource(Res.string.sip_frequency_daily),
            isSelected = frequency == SipFrequency.DAILY,
            onClick = { onFrequencyChange(SipFrequency.DAILY) },
            modifier = Modifier.weight(1f)
        )
        SipFrequencyTab(
            label = stringResource(Res.string.sip_frequency_monthly),
            isSelected = frequency == SipFrequency.MONTHLY,
            onClick = { onFrequencyChange(SipFrequency.MONTHLY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SipFrequencyTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) SipHeroObsidian else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else SipInk.copy(alpha = 0.42f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DailyAmountSection(
    amount: Float,
    onAmountChange: (Float) -> Unit,
    amountText: TextFieldValue,
    onAmountTextChange: (TextFieldValue) -> Unit,
    minAmount: Float,
    maxAmount: Float,
    defaultAmount: Float,
    goalType: GoalType,
    accentColor: Color,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val chipAmounts = remember(minAmount.toInt(), goalType, defaultAmount.toInt(), maxAmount.toInt()) {
        val min = minAmount.toInt()
        val default = defaultAmount.toInt()
        val max = maxAmount.toInt()
        val secondChip = if (default != min) default else min + 100
        listOf(min, secondChip, max)
    }

    val isCustom = remember(amount.toInt(), chipAmounts) { amount.toInt() !in chipAmounts }
    var isCustomMode by remember { mutableStateOf(isCustom) }
    var shouldRequestFocus by remember { mutableStateOf(false) }
    LaunchedEffect(amount.toInt(), chipAmounts) {
        isCustomMode = amount.toInt() !in chipAmounts
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    var isAmountFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isAmountFocused) {
        if (isAmountFocused) {
            delay(150)
            scrollState.animateScrollTo(density.run { 150.dp.roundToPx() })
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isAmountFocused) {
            focusManager.clearFocus()
            isCustomMode = false
        }
    }

    if (isCustomMode) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                val filteredText = newValue.text.filter { it.isDigit() }
                if (filteredText.isNotEmpty()) {
                    val newAmount = filteredText.toIntOrNull()
                    if (newAmount != null && newAmount in minAmount.toInt()..maxAmount.toInt()) {
                        onAmountTextChange(TextFieldValue(filteredText, newValue.selection))
                        onAmountChange(newAmount.toFloat())
                    } else if (newAmount != null && newAmount > maxAmount.toInt()) {
                        val maxText = maxAmount.toInt().toString()
                        onAmountTextChange(TextFieldValue(maxText, TextRange(maxText.length)))
                        onAmountChange(maxAmount)
                    } else if (newAmount != null && newAmount < minAmount.toInt()) {
                        onAmountTextChange(TextFieldValue(filteredText, newValue.selection))
                    }
                } else {
                    onAmountTextChange(TextFieldValue("", TextRange(0)))
                }
            },
            textStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = SipInk),
            prefix = { Text("₹", fontSize = 28.sp, color = SipInk.copy(alpha = 0.38f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    val wasFocused = isAmountFocused
                    isAmountFocused = focusState.isFocused
                    if (focusState.isFocused && !wasFocused) {
                        val text = amountText.text
                        onAmountTextChange(TextFieldValue(text, TextRange(text.length)))
                    }
                    if (!focusState.isFocused && wasFocused) {
                        keyboardController?.hide()
                        val currentInput = amountText.text.toIntOrNull()
                        if (currentInput == null) {
                            val newText = amount.toInt().toString()
                            onAmountTextChange(TextFieldValue(newText, TextRange(newText.length)))
                        } else if (currentInput < minAmount.toInt()) {
                            val newText = minAmount.toInt().toString()
                            onAmountTextChange(TextFieldValue(newText, TextRange(newText.length)))
                            onAmountChange(minAmount)
                        }
                    }
                },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().clickable {
                shouldRequestFocus = true
                isCustomMode = true
            },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "₹${amount.toInt()}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = SipInk)
        }
    }

    Text(
        text = "/day",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = SipInk.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
        textAlign = TextAlign.Center
    )

    val popularAmount = remember(defaultAmount.toInt(), minAmount.toInt(), chipAmounts) {
        val default = defaultAmount.toInt()
        if (default != minAmount.toInt() && default in chipAmounts) default else null
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chipAmounts.forEach { preset ->
            SipPresetChip(
                label = "₹$preset",
                isSelected = !isCustomMode && amount.toInt() == preset,
                isPopular = preset == popularAmount,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    isCustomMode = false
                    onAmountChange(preset.toFloat())
                    val newText = preset.toString()
                    onAmountTextChange(TextFieldValue(newText, TextRange(newText.length)))
                }
            )
        }
        SipPresetChip(
            label = "Custom",
            isSelected = isCustomMode,
            isPopular = false,
            accentColor = accentColor,
            modifier = Modifier.weight(1f),
            onClick = {
                shouldRequestFocus = true
                isCustomMode = true
            }
        )
    }

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus && isCustomMode) {
            delay(300)
            coroutineScope.launch {
                focusRequester.requestFocus()
                bringIntoViewRequester.bringIntoView()
                scrollState.animateScrollTo(scrollState.maxValue)
                delay(150)
                bringIntoViewRequester.bringIntoView()
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            shouldRequestFocus = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MonthlyAmountSection(
    monthlyAmount: Float,
    onMonthlyAmountChange: (Float) -> Unit,
    monthlyAmountText: TextFieldValue,
    onMonthlyAmountTextChange: (TextFieldValue) -> Unit,
    isMonthlyCustomMode: Boolean,
    onMonthlyCustomModeChange: (Boolean) -> Unit,
    accentColor: Color,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    scrollState: androidx.compose.foundation.ScrollState
) {
    LaunchedEffect(monthlyAmount.toInt()) {
        onMonthlyCustomModeChange(monthlyAmount.toInt() !in MONTHLY_PRESETS)
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    var isAmountFocused by remember { mutableStateOf(false) }
    var shouldMonthlyRequestFocus by remember { mutableStateOf(false) }
    LaunchedEffect(isAmountFocused) {
        if (isAmountFocused) {
            delay(150)
            scrollState.animateScrollTo(density.run { 150.dp.roundToPx() })
        }
    }

    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isAmountFocused) {
            focusManager.clearFocus()
            onMonthlyCustomModeChange(false)
        }
    }

    if (isMonthlyCustomMode) {
        OutlinedTextField(
            value = monthlyAmountText,
            onValueChange = { newValue ->
                val filteredText = newValue.text.filter { it.isDigit() }
                if (filteredText.isNotEmpty()) {
                    val newAmount = filteredText.toIntOrNull()
                    if (newAmount != null && newAmount in MONTHLY_MIN.toInt()..MONTHLY_MAX.toInt()) {
                        onMonthlyAmountTextChange(TextFieldValue(filteredText, newValue.selection))
                        onMonthlyAmountChange(newAmount.toFloat())
                    } else if (newAmount != null && newAmount > MONTHLY_MAX.toInt()) {
                        val maxText = MONTHLY_MAX.toInt().toString()
                        onMonthlyAmountTextChange(TextFieldValue(maxText, TextRange(maxText.length)))
                        onMonthlyAmountChange(MONTHLY_MAX)
                    } else if (newAmount != null && newAmount < MONTHLY_MIN.toInt()) {
                        onMonthlyAmountTextChange(TextFieldValue(filteredText, newValue.selection))
                    }
                } else {
                    onMonthlyAmountTextChange(TextFieldValue("", TextRange(0)))
                }
            },
            textStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = SipInk),
            prefix = { Text("₹", fontSize = 28.sp, color = SipInk.copy(alpha = 0.38f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    val wasFocused = isAmountFocused
                    isAmountFocused = focusState.isFocused
                    if (focusState.isFocused && !wasFocused) {
                        val text = monthlyAmountText.text
                        onMonthlyAmountTextChange(TextFieldValue(text, TextRange(text.length)))
                    }
                    if (!focusState.isFocused && wasFocused) {
                        keyboardController?.hide()
                        val currentInput = monthlyAmountText.text.toIntOrNull()
                        if (currentInput == null) {
                            val newText = monthlyAmount.toInt().toString()
                            onMonthlyAmountTextChange(TextFieldValue(newText, TextRange(newText.length)))
                        } else if (currentInput < MONTHLY_MIN.toInt()) {
                            val newText = MONTHLY_MIN.toInt().toString()
                            onMonthlyAmountTextChange(TextFieldValue(newText, TextRange(newText.length)))
                            onMonthlyAmountChange(MONTHLY_MIN)
                        }
                    }
                },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().clickable {
                shouldMonthlyRequestFocus = true
                onMonthlyCustomModeChange(true)
            },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "₹${monthlyAmount.toInt()}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = SipInk)
        }
    }

    Text(
        text = "/month",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = SipInk.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
        textAlign = TextAlign.Center
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MONTHLY_PRESETS.forEach { preset ->
            SipPresetChip(
                label = formatRupees(preset.toDouble()),
                isSelected = !isMonthlyCustomMode && monthlyAmount.toInt() == preset,
                isPopular = preset == MONTHLY_POPULAR_AMOUNT,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                onClick = {
                    onMonthlyCustomModeChange(false)
                    onMonthlyAmountChange(preset.toFloat())
                    val newText = preset.toString()
                    onMonthlyAmountTextChange(TextFieldValue(newText, TextRange(newText.length)))
                }
            )
        }
        SipPresetChip(
            label = "Custom",
            isSelected = isMonthlyCustomMode,
            isPopular = false,
            accentColor = accentColor,
            modifier = Modifier.weight(1f),
            onClick = {
                shouldMonthlyRequestFocus = true
                onMonthlyCustomModeChange(true)
            }
        )
    }

    LaunchedEffect(shouldMonthlyRequestFocus) {
        if (shouldMonthlyRequestFocus && isMonthlyCustomMode) {
            delay(300)
            coroutineScope.launch {
                focusRequester.requestFocus()
                bringIntoViewRequester.bringIntoView()
                scrollState.animateScrollTo(scrollState.maxValue)
                delay(150)
                bringIntoViewRequester.bringIntoView()
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            shouldMonthlyRequestFocus = false
        }
    }
}

@Composable
private fun MonthlyDatePicker(
    selectedDate: Int,
    accentColor: Color,
    onDateSelected: (Int) -> Unit
) {
    val popularDates = remember { setOf(1, 5, 10, 15, 20, 25) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(text = stringResource(Res.string.sip_date_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SipInk)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = stringResource(Res.string.sip_date_subtitle), fontSize = 11.sp, color = SipInk.copy(alpha = 0.45f))
            }
            Text(text = ordinalSuffix(selectedDate), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 7) {
                    val date = row * 7 + col + 1
                    val isSelected = date == selectedDate
                    val isPopular = date in popularDates
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                when {
                                    isSelected -> SipHeroObsidian
                                    isPopular -> SipGold.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    isSelected -> SipGold
                                    isPopular -> SipGold.copy(alpha = 0.35f)
                                    else -> SipSubtleBorder
                                },
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.toString(),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else if (isPopular) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                    isSelected -> SipGold
                                    isPopular -> SipGoldDeep
                                    else -> SipInk.copy(alpha = 0.55f)
                            }
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(Res.string.sip_dates_skipped_footnote),
            fontSize = 10.sp,
            color = SipInk.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SipPresetChip(
    label: String,
    isSelected: Boolean,
    isPopular: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.height(60.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) accentColor.copy(alpha = 0.10f) else Color.White)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) accentColor else SipSubtleBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) SipGoldDeep else SipInk
            )
        }
        if (isPopular) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
                    .clip(RoundedCornerShape(80))
                    .background(SipGold)
                    .padding(horizontal = 9.dp, vertical = 1.dp)
            ) {
                Text(text = "Popular", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun PastPerformanceBar(
    dateLabel: String,
    valueLabel: String,
    barHeightDp: Dp,
    isLatest: Boolean,
    theme: SipGoalTheme,
    modifier: Modifier = Modifier
) {
    val topRadius = if (isLatest) 6.dp else 4.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valueLabel,
            fontSize = if (isLatest) 10.sp else 9.sp,
            fontWeight = if (isLatest) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isLatest) theme.accentColor else Color.White.copy(alpha = 0.48f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
                .then(
                    if (isLatest) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius),
                            ambientColor = theme.accentColor,
                            spotColor = theme.accentColor
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(RoundedCornerShape(topStart = topRadius, topEnd = topRadius, bottomStart = 2.dp, bottomEnd = 2.dp))
                .background(Brush.verticalGradient(colors = theme.barSelectedGradient))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateLabel,
            fontSize = 9.sp,
            fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium,
            color = if (isLatest) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun SipStartInfoCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F8E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SipInk)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = subtitle, fontSize = 12.sp, color = SipInk.copy(alpha = 0.55f), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SipSchemeCard(
    fundName: String?,
    category: String?,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SipSubtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.allocates_to),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = SipInk.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, SipSubtleBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        val logo = getFundLogo(fundName)
                        if (logo != null) {
                            Image(
                                painter = painterResource(logo),
                                contentDescription = stringResource(Res.string.content_description_fund_logo),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fundName ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SipInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!category.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(text = category, fontSize = 11.sp, color = SipInk.copy(alpha = 0.5f))
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SipInk.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun getFundLogo(fundName: String?): org.jetbrains.compose.resources.DrawableResource? {
    val name = fundName ?: return null
    return when {
        name.contains("Invesco", true) -> Res.drawable.invesco
        name.contains("Aditya", true) -> Res.drawable.aditya
        name.contains("Axis", true) -> Res.drawable.axis_lo
        name.contains("Nippon", true) -> Res.drawable.nippon
        else -> null
    }
}

@Composable
private fun SipTrustChecklist() {
    val items = listOf(
        stringResource(Res.string.lumpsum_trust_safe_sebi),
        stringResource(Res.string.sip_trust_flexibility)
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        items.forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = line,
                    fontSize = 12.sp,
                    color = SipInk.copy(alpha = 0.58f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FundDetailsBottomSheet(
    amount: Double,
    goalType: GoalType,
    fundDetailsState: com.pyllar.consumer.presentation.mutualfund.details.FundDetailsState,
    isMonthly: Boolean,
    sipDate: Int,
    isSheetLoading: Boolean,
    sheetError: String?,
    areDetailsLoaded: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    showDisclaimerDialog: () -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val frequencyLabel = if (isMonthly) "Monthly" else "Daily"
            val title = when (goalType) {
                GoalType.GOLD -> "Gold $frequencyLabel SIP"
                GoalType.SILVER -> "Silver $frequencyLabel SIP"
                GoalType.SAVINGS, GoalType.SAVINGS_PLUS -> "$frequencyLabel SIP"
                GoalType.FESTIVAL_SPENDS -> "$frequencyLabel SIP"
                GoalType.GLOBAL_EXPOSURE -> "Global Exposure $frequencyLabel SIP"
                GoalType.ALL_IN_ONE -> "All-in-One $frequencyLabel SIP"
                else -> "$frequencyLabel SIP"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    fundDetailsState.fundDetails?.fundName?.let { fundName ->
                        Text(
                            text = stringResource(Res.string.powered_by, fundName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                fundDetailsState.fundDetails?.fundName?.let { fundName ->
                    val logo = getFundLogo(fundName)
                    if (logo != null) {
                        Image(
                            painter = painterResource(logo),
                            contentDescription = stringResource(Res.string.content_description_fund_logo),
                            modifier = Modifier.size(60.dp).padding(start = 16.dp)
                        )
                    }
                }
            }

            if (fundDetailsState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (fundDetailsState.fundDetails != null) {
                    FundHeader(fundDetailsState.fundDetails!!)
                }

                val bankDetails = fundDetailsState.fundDetails?.bankDetails
                if (bankDetails != null) {
                    BankDetailsCard(
                        accountNumber = bankDetails.accountNumber,
                        ifscCode = bankDetails.ifscCode,
                        bankName = bankDetails.bankName
                    )
                } else if (fundDetailsState.bankAccountNumber != null) {
                    BankDetailsCard(
                        accountNumber = fundDetailsState.bankAccountNumber,
                        ifscCode = fundDetailsState.bankIfscCode,
                        bankName = fundDetailsState.bankName
                    )
                }

                if (sheetError != null) {
                    Text(
                        text = sheetError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = onConfirm,
                    enabled = !isSheetLoading && areDetailsLoaded,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SipHeroObsidian,
                        contentColor = Color.White,
                        disabledContainerColor = SipHeroObsidian.copy(alpha = 0.55f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    if (isSheetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submitting...")
                    } else if (!areDetailsLoaded) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading details...")
                    } else {
                        Text("Invest ₹${amount.toInt()}/${if (isMonthly) "month" else "day"}")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val schemeDocUrl = fundDetailsState.fundDetails?.schemeDocumentUrl
                    if (!schemeDocUrl.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.clickable { uriHandler.openUri(schemeDocUrl) }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.scheme_documents),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = stringResource(Res.string.disclaimer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { showDisclaimerDialog() }.padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun formatRupees(amount: Double): String {
    val integerPart = amount.toInt().toString()
    val length = integerPart.length
    if (length <= 3) return "₹$integerPart"
    val lastThree = integerPart.substring(length - 3)
    val remaining = integerPart.substring(0, length - 3)
    val builder = StringBuilder()
    var i = remaining.length - 1
    var count = 0
    while (i >= 0) {
        builder.append(remaining[i])
        count++
        if (count == 2 && i > 0) {
            builder.append(",")
            count = 0
        }
        i--
    }
    return "₹${builder.reverse()},$lastThree"
}

