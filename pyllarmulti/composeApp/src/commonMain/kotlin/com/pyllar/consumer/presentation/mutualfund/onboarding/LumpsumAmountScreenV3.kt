package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
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
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.domain.storage.InMemorySessionStore
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.presentation.mutualfund.details.BankDetailsCard
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsState
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.details.FundHeader
import com.pyllar.consumer.presentation.mutualfund.details.SipCreationResult
import com.pyllar.consumer.presentation.dashboard.getFundLogo
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.presentation.dashboard.KycPendingBottomSheet
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.presentation.ui.components.TrustStrip
import com.pyllar.consumer.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.*
import kotlin.math.roundToInt

private val LumpsumHeroObsidian = Color(0xFF0A2415)
private val LumpsumInk = Color(0xFF3E2723)
private val LumpsumSubtleBorder = Color(0xFFEFEBE9)
private val LumpsumGold = Color(0xFFD4AF37)
private val LumpsumGoldDeep = Color(0xFF8B6B25)
private const val PAST_PERF_BAR_MAX_HEIGHT_DP = 60

private data class LumpsumGoalTheme(
    val accentColor: Color,
    val eyebrowColor: Color,
    val shimmerColor: Color,
    val dividerColor: Color,
    val barSelectedGradient: List<Color>
)

private fun lumpsumGoalTheme(goalType: GoalType): LumpsumGoalTheme = when (goalType) {
    GoalType.GOLD -> LumpsumGoalTheme(
        accentColor = LumpsumGold,
        eyebrowColor = LumpsumGold.copy(alpha = 0.6f),
        shimmerColor = Color(0xFFFFE082).copy(alpha = 0.10f),
        dividerColor = LumpsumGold.copy(alpha = 0.18f),
        barSelectedGradient = listOf(LumpsumGold, Color(0xFFFFF3A0))
    )
    GoalType.SILVER -> LumpsumGoalTheme(
        accentColor = Color(0xFFC8DCE8),
        eyebrowColor = Color(0xFFB4D2E4).copy(alpha = 0.65f),
        shimmerColor = Color(0xFFC8DCF0).copy(alpha = 0.10f),
        dividerColor = Color(0xFFB4D2E4).copy(alpha = 0.18f),
        barSelectedGradient = listOf(Color(0xFFA8C8DC), Color(0xFFE8F4FA))
    )
    else -> LumpsumGoalTheme(
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LumpsumAmountScreenV3(
    userId: String,
    kycAttemptId: String,
    investorId: String,
    goalId: String = "",
    isExistingInvestment: Boolean = false,
    onLumpsumCreated: (Double, String?, MandateWrapper?) -> Unit = { _, _, _ -> },
    onForceLogout: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToFundDetails: (userId: String, goalId: String, amount: Double, kycAttemptId: String, investorId: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    sessionStore: SessionStore = koinInject(),
    inMemorySessionStore: InMemorySessionStore = koinInject(),
    fundDetailsViewModel: FundDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val minAmount = 1000f
    val maxAmount = 50000f
    val targetAmount = 5000f

    var amount by remember { mutableStateOf(targetAmount) }
    var amountText by remember {
        mutableStateOf(TextFieldValue(targetAmount.toInt().toString(), TextRange(targetAmount.toInt().toString().length)))
    }

    var isLoading by remember { mutableStateOf(false) }
    var isInitTxnLoading by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<String?>(null) }
    var showUnexpectedErrorDialog by remember { mutableStateOf(false) }
    var showTrustStripInfoDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val timeoutState = rememberTimeoutState("LumpsumAmountV3", "continue")

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var effectiveUserId by remember { mutableStateOf(userId) }
    var effectiveGoalId by remember { mutableStateOf(goalId) }
    var effectiveKycAttemptId by remember { mutableStateOf(kycAttemptId) }
    var effectiveInvestorId by remember { mutableStateOf(investorId) }

    LaunchedEffect(Unit) {
        try {
            if (effectiveUserId.isBlank()) {
                effectiveUserId = sessionStore.getValue(KeyValueConstants.USER_ID) ?: ""
            }
            if (effectiveGoalId.isBlank()) {
                effectiveGoalId = sessionStore.getValue(KeyValueConstants.SELECTED_GOAL_ID) ?: goalId
            }
            if (effectiveKycAttemptId.isBlank()) {
                effectiveKycAttemptId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
            }
            if (effectiveInvestorId.isBlank()) {
                effectiveInvestorId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""
            }

            val savedGoalId = inMemorySessionStore.getValue("selected_lumpsum_goal_id") ?: ""
            if (savedGoalId.isNotBlank() && savedGoalId == effectiveGoalId) {
                inMemorySessionStore.getValue("selected_lumpsum_amount")?.toFloatOrNull()?.let {
                    amount = it
                    val textVal = it.toInt().toString()
                    amountText = TextFieldValue(textVal, TextRange(textVal.length))
                }
            } else {
                inMemorySessionStore.saveValue("selected_lumpsum_amount", "")
                inMemorySessionStore.saveValue("selected_lumpsum_goal_id", effectiveGoalId)
            }
        } catch (e: Exception) {
            platformLog("LumpsumAmountScreenV3: Error fetching stored IDs: ${e.message}")
        }
    }

    val goalType = remember(effectiveGoalId) { identifyGoalType(effectiveGoalId) }
    val theme = remember(goalType) { lumpsumGoalTheme(goalType) }

    val fundDetailsState by fundDetailsViewModel.uiState.collectAsState()
    var showDetailsBottomSheet by remember { mutableStateOf(false) }

    val dashboardState by dashboardViewModel.dashboardState.collectAsState()

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
        }
    }

    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        if (effectiveUserId.isNotBlank() && effectiveGoalId.isNotBlank()) {
            fundDetailsViewModel.loadPastPerformance(effectiveUserId, effectiveGoalId)
        }
    }

    LaunchedEffect(effectiveUserId, effectiveGoalId) {
        PlatformAnalyticsLogger.logScreenView("LumpsumAmountV3")

        if (effectiveGoalId.isNotBlank()) {
            isInitTxnLoading = true
            coroutineScope.launch {
                try {
                    val resolvedUserId = if (effectiveUserId.isNotBlank()) effectiveUserId else {
                        try {
                            sessionStore.getCurrentUserId().takeIf { it.isNotBlank() } ?: userId
                        } catch (e: Exception) {
                            userId
                        }
                    }

                    if (resolvedUserId.isNotBlank()) {
                        when (val result = dashboardViewModel.initGoalTxn(userId = resolvedUserId, goalId = effectiveGoalId)) {
                            is Resource.Success -> {
                                result.data?.let { response ->
                                    if (response.userPurposeId.isNotBlank()) {
                                        sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, response.userPurposeId)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    platformLog("LumpsumAmountScreenV3: Exception calling initGoalTxn: ${e.message}")
                } finally {
                    isInitTxnLoading = false
                }
            }
        }
    }

    val handleBack: () -> Unit = {
        coroutineScope.launch {
            inMemorySessionStore.saveValue("selected_lumpsum_amount", "")
            inMemorySessionStore.saveValue("selected_lumpsum_goal_id", "")
        }
        onNavigateBack()
    }

    BackHandler {
        handleBack()
    }

    val pastPerformance = fundDetailsState.pastPerformance
    val pastPerformanceLoading = fundDetailsState.pastPerformanceLoading
    val baseUnitAmount = pastPerformance?.baseUnitAmount ?: 100.0
    fun scalePastPerformanceValue(baselineValue: Double): Double =
        baselineValue * (amount.toDouble() / baseUnitAmount)
    val navMilestones = pastPerformance?.milestones.orEmpty()
    val latestMilestone = navMilestones.find { it.latest }
    val navCurrentVal = latestMilestone?.let {
        scalePastPerformanceValue(it.onetimeBaselinePortfolioValue ?: 0.0)
    } ?: 0.0
    val navInvestedVal = latestMilestone?.let {
        scalePastPerformanceValue(it.onetimeBaselineInvestedValue ?: 0.0)
    } ?: 0.0
    val navGainVal = navCurrentVal - navInvestedVal
    val navGainPct = if (navInvestedVal > 0) ((navGainVal / navInvestedVal) * 100).roundToInt() else 0
    val gramsValueLabel = pastPerformance?.metalName?.let {
        val baselineGrams = latestMilestone?.onetimeBaselineGrams
        baselineGrams?.let { grams -> formatGrams(scalePastPerformanceValue(grams)) }
    }
    val gramsSuffixLabel = pastPerformance?.metalName?.let { metalName ->
        "of $metalName purchase power"
    }

    val shimmerTransition = rememberInfiniteTransition(label = "lumpsumHeroShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lumpsumHeroShimmerProgress"
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding().padding(top = 2.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Pyllar ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = LumpsumHeroObsidian, letterSpacing = (-0.5).sp)
                        Text(text = "Money", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = LumpsumGold, letterSpacing = (-0.5).sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent("share_app_clicked", mapOf("screen_name" to "LumpsumAmountV3"))
                            platformActions.shareText("Start your investment journey with Pyllar! https://pyllar.in")
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
                    LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onNavigateToHelp) {
                        Text(
                            text = "Help",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = LumpsumHeroObsidian,
                    navigationIconContentColor = LumpsumHeroObsidian,
                    actionIconContentColor = LumpsumHeroObsidian
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                                .background(LumpsumHeroObsidian)
                                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)
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
                                Text(
                                    text = "PAST PERFORMANCE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                    color = theme.eyebrowColor
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                if (pastPerformanceLoading || pastPerformance == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = theme.accentColor,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(Res.string.lumpsum_invested_label).uppercase(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.6.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = formatRupeesShort(navInvestedVal),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "on ${pastPerformance.startLabel}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = stringResource(Res.string.worth_today).uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.6.sp,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
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
                                            if (gramsValueLabel != null && gramsSuffixLabel != null) {
                                                Row(modifier = Modifier.padding(top = 4.dp)) {
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
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(theme.dividerColor)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val maxMilestoneVal = navMilestones.maxOfOrNull {
                                        scalePastPerformanceValue(it.onetimeBaselinePortfolioValue ?: 0.0)
                                    } ?: 1.0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        navMilestones.forEach { milestone ->
                                            val milestoneValue = scalePastPerformanceValue(milestone.onetimeBaselinePortfolioValue ?: 0.0)
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

                    // ── LUMPSUM AMOUNT CARD ───────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            var titleTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            var rangeTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            val goalTitle = "${getGoalDisplayName(goalType)} Amount"
                            val rangeLabel = "Range: ₹${minAmount.toInt()} - ₹${maxAmount.toInt()}"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = goalTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LumpsumInk,
                                    modifier = Modifier.weight(1f, fill = false),
                                    onTextLayout = { titleTextLayoutResult = it }
                                )
                                Text(
                                    text = rangeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LumpsumInk.copy(alpha = 0.42f),
                                    onTextLayout = { rangeTextLayoutResult = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LumpsumAmountSection(
                                amount = amount,
                                onAmountChange = {
                                    amount = it
                                    coroutineScope.launch {
                                        inMemorySessionStore.saveValue("selected_lumpsum_amount", it.toString())
                                        inMemorySessionStore.saveValue("selected_lumpsum_goal_id", effectiveGoalId)
                                    }
                                },
                                amountText = amountText,
                                onAmountTextChange = {
                                    amountText = it
                                    val numeric = it.text.filter { c -> c.isDigit() }.toFloatOrNull()
                                    if (numeric != null) {
                                        coroutineScope.launch {
                                            inMemorySessionStore.saveValue("selected_lumpsum_amount", numeric.toString())
                                            inMemorySessionStore.saveValue("selected_lumpsum_goal_id", effectiveGoalId)
                                        }
                                    }
                                },
                                minAmount = minAmount,
                                maxAmount = maxAmount,
                                accentColor = theme.accentColor,
                                coroutineScope = coroutineScope,
                                focusManager = focusManager,
                                keyboardController = keyboardController,
                                scrollState = scrollState
                            )
                        }
                    }

                    // ── SCHEME CARD ───────────────────────────────────────────
                    LumpsumSchemeCard(
                        fundName = fundDetailsState.fundDetails?.fundName,
                        category = fundDetailsState.fundDetails?.category,
                        isLoading = fundDetailsState.isLoading,
                        enabled = areDetailsLoaded,
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

//                    // ── PROCESS INFO CARD ─────────────────────────────────────
//                    LumpsumStartInfoCard(
//                        title = "Processed by ${getInvestmentStatus()}, 8 AM",
//                        subtitle = "Units allocated at next NAV · No recurring debit — one-time only"
//                    )

                    // ── TRUST CHECKLIST ───────────────────────────────────────
                    LumpsumTrustChecklist()

                    if (submitResult != null) {
                        Text(
                            text = submitResult!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // ── BOTTOM CTA ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700), LumpsumGoldDeep)))
                        .padding(1.5.dp)
                ) {
                    TimeoutButton(
                        onClick = {
                            if (amount < minAmount || amount > maxAmount) {
                                coroutineScope.launch {
                                    submitResult = org.jetbrains.compose.resources.getString(
                                        Res.string.lumpsum_amount_range_error,
                                        minAmount.toInt(),
                                        maxAmount.toInt()
                                    )
                                }
                                return@TimeoutButton
                            }

                            val isKycPending = dashboardState.kycStatus.equals("PENDING", ignoreCase = true) ||
                                    dashboardState.kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                                    dashboardState.kycStatus.equals("EXPIRED", ignoreCase = true)

                            if (isKycPending) {
                                showKycPendingBottomSheet = true
                                return@TimeoutButton
                            }

                            if (isLoading) return@TimeoutButton
                            isLoading = true
                            coroutineScope.launch {
                                submitResult = null
                                showUnexpectedErrorDialog = false

                                PlatformAnalyticsLogger.logEvent(
                                    "lumpsum_create_attempt_redirect",
                                    mapOf("amount" to amount.toInt(), "user_id_present" to userId.isNotBlank())
                                )

                                showDetailsBottomSheet = true
                                isLoading = false
                            }
                        },
                        enabled = !isLoading && !isInitTxnLoading && areDetailsLoaded,
                        timeoutState = timeoutState,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LumpsumHeroObsidian,
                            contentColor = Color.White,
                            disabledContainerColor = LumpsumHeroObsidian.copy(alpha = 0.55f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isLoading || !areDetailsLoaded) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isLoading) "Submitting..." else "Fetching details...")
                        } else {
                            Text(stringResource(Res.string.lumpsum_invest_one_time_cta), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                TrustStrip(onInfoClick = { showTrustStripInfoDialog = true })
            }
        }

        if (showUnexpectedErrorDialog) {
            AlertDialog(
                onDismissRequest = { showUnexpectedErrorDialog = false },
                title = { Text("Unexpected error") },
                text = { Text("Something went wrong while processing your request. Please try again or log in again.") },
                confirmButton = {
                    TextButton(onClick = { showUnexpectedErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showTrustStripInfoDialog) {
            AlertDialog(
                onDismissRequest = { showTrustStripInfoDialog = false },
                title = { Text("About AMCs") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Investments are made in mutual funds managed by respective Asset Management Companies.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTrustStripInfoDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Fund Details & Confirmation Bottom Sheet
        if (showDetailsBottomSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showDetailsBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                val sheetTimeoutState = rememberTimeoutState("LumpsumAmountV3Sheet", "confirm")
                var isSheetLoading by remember { mutableStateOf(false) }
                var sheetError by remember { mutableStateOf<String?>(null) }
                val sheetCoroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val title = when (goalType) {
                        GoalType.GOLD -> "Gold One-time Investment"
                        GoalType.SILVER -> "Silver One-time Investment"
                        GoalType.SAVINGS, GoalType.SAVINGS_PLUS -> "One-time Investment"
                        GoalType.FESTIVAL_SPENDS -> "One-time Investment"
                        GoalType.GLOBAL_EXPOSURE -> "Global Exposure One-time Investment"
                        GoalType.ALL_IN_ONE -> "All-in-One One-time Investment"
                        GoalType.MARKET_EXPLORER -> "Market Explorer One-time Investment"
                        GoalType.INNOVATION -> "Innovation One-time Investment"
                        GoalType.SENSEX -> "Sensex One-time Investment"
                        else -> "One-time Investment"
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
                                    text = "powered by $fundName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        fundDetailsState.fundDetails?.fundName?.let { fundName ->
                            val logo = getFundLogo(fundName)
                            Image(
                                painter = painterResource(logo),
                                contentDescription = "Fund Logo",
                                modifier = Modifier.size(60.dp).padding(start = 16.dp)
                            )
                        }
                    }

                    if (fundDetailsState.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        if (fundDetailsState.fundDetails != null) {
                            FundHeader(fundDetailsState.fundDetails!!, showNavChip = false)
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
                                text = sheetError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        TimeoutButton(
                            onClick = {
                                val isKycPending = !dashboardState.isLoading &&
                                        (dashboardState.kycStatus.equals("PENDING", ignoreCase = true) ||
                                                dashboardState.kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                                                dashboardState.kycStatus.equals("EXPIRED", ignoreCase = true) ||
                                                dashboardState.kycStatus.equals("UNLINKED", ignoreCase = true))
                                if (isKycPending) {
                                    showDetailsBottomSheet = false
                                    showKycPendingBottomSheet = true
                                } else {
                                    sheetCoroutineScope.launch {
                                        isSheetLoading = true
                                        sheetError = null

                                        val resolvedUserId = if (effectiveUserId.isNotBlank()) effectiveUserId else {
                                            try {
                                                sessionStore.getCurrentUserId().takeIf { it.isNotBlank() } ?: userId
                                            } catch (e: Exception) {
                                                userId
                                            }
                                        }

                                        val result = fundDetailsViewModel.createLumpsumPurchase(
                                            userId = resolvedUserId,
                                            amount = amount.toDouble()
                                        )

                                        when (result) {
                                            is SipCreationResult.LumpsumSuccess -> {
                                                isSheetLoading = false
                                                showDetailsBottomSheet = false
                                                val mappedData = result.lumpsumData?.let { data ->
                                                    MandateWrapper(
                                                        finMandateId = data.old_id ?: 0L,
                                                        mandateId = data.payment_id ?: 0L,
                                                        uri = data.token_url
                                                    )
                                                }
                                                onLumpsumCreated(
                                                    amount.toDouble(),
                                                    result.nextScreen ?: effectiveGoalId.ifBlank { goalId },
                                                    mappedData
                                                )
                                            }
                                            is SipCreationResult.Failure -> {
                                                isSheetLoading = false
                                                sheetError = result.message
                                            }
                                            is SipCreationResult.SecureChannelError -> {
                                                isSheetLoading = false
                                                sheetError = "Secure channel error. Please try again."
                                            }
                                            else -> {
                                                isSheetLoading = false
                                                sheetError = "Unexpected operation result."
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSheetLoading && areDetailsLoaded,
                            timeoutState = sheetTimeoutState,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isSheetLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submitting...")
                            } else if (dashboardState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking...")
                            } else if (!areDetailsLoaded) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading details...")
                            } else {
                                Text("Invest ₹${amount.toInt()} one-time", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val schemeDocUrl = fundDetailsState.fundDetails?.schemeDocumentUrl
                            if (!schemeDocUrl.isNullOrBlank()) {
                                Text(
                                    text = "Scheme Documents",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { uriHandler.openUri(schemeDocUrl) }.padding(4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Text(
                                text = "Disclaimer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { showDisclaimerDialog = true }.padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (showDisclaimerDialog) {
            AlertDialog(
                onDismissRequest = { showDisclaimerDialog = false },
                title = { Text("Disclaimer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Gold prices are subject to market fluctuations and may vary from day to day. Silver prices are subject to market fluctuations and may vary from day to day.")
                        Text("Mutual Fund investments are subject to market risks. Please read all scheme related documents carefully.")
                        Text(
                            text = "Pyllar Fintech Private Limited is an AMFI registered Mutual Fund distributor (ARN No: 341847)",
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
            KycPendingBottomSheet(
                onDismiss = { showKycPendingBottomSheet = false },
                onRetryKyc = { showKycPendingBottomSheet = false },
                kycStatus = dashboardState.kycStatus
            )
        }
    }
}
}
// ── Helper composables ──────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LumpsumAmountSection(
    amount: Float,
    onAmountChange: (Float) -> Unit,
    amountText: TextFieldValue,
    onAmountTextChange: (TextFieldValue) -> Unit,
    minAmount: Float,
    maxAmount: Float,
    accentColor: Color,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val chipAmounts = listOf(1000, 5000, 10000)

    val isCustom = remember(amount.toInt(), chipAmounts) { amount.toInt() !in chipAmounts }
    var isCustomMode by remember { mutableStateOf(isCustom) }
    var shouldRequestFocus by remember { mutableStateOf(false) }
    LaunchedEffect(amount.toInt(), chipAmounts) {
        isCustomMode = amount.toInt() !in chipAmounts
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    var isAmountFocused by remember { mutableStateOf(false) }

    val density = LocalDensity.current
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
            textStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = LumpsumInk),
            prefix = { Text("₹", fontSize = 28.sp, color = LumpsumInk.copy(alpha = 0.38f)) },
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
                        coroutineScope.launch {
                            delay(300)
                            bringIntoViewRequester.bringIntoView()
                            scrollState.animateScrollTo(scrollState.maxValue)
                            delay(150)
                            bringIntoViewRequester.bringIntoView()
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                    if (!focusState.isFocused && wasFocused) {
                        keyboardController?.hide()
                        val currentInput = amountText.text.toIntOrNull()
                        if (currentInput == null) {
                            val newText = amount.toInt().toString()
                            onAmountTextChange(TextFieldValue(newText, TextRange(newText.length)) )
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
            Text(text = "₹${amount.toInt()}", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = LumpsumInk)
        }
    }

    Text(
        text = stringResource(Res.string.lumpsum_one_time_amount_label),
        fontSize = 15.sp,
        color = LumpsumInk.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
        textAlign = TextAlign.Center
    )

    val popularAmount = 5000

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chipAmounts.forEach { preset ->
            LumpsumPresetChip(
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
        LumpsumPresetChip(
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

@Composable
private fun LumpsumSchemeCard(
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
        border = androidx.compose.foundation.BorderStroke(1.dp, LumpsumSubtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ALLOCATES TO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = LumpsumInk.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, LumpsumSubtleBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        val logo = getFundLogo(fundName)
                        Image(
                            painter = painterResource(logo),
                            contentDescription = fundName,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fundName ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LumpsumInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!category.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(text = category, fontSize = 11.sp, color = LumpsumInk.copy(alpha = 0.5f))
                    }
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = LumpsumInk.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun LumpsumPresetChip(
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
                    color = if (isSelected) accentColor else LumpsumSubtleBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) LumpsumGoldDeep else LumpsumInk
            )
        }
        if (isPopular) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
                    .clip(RoundedCornerShape(80))
                    .background(LumpsumGold)
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
    theme: LumpsumGoalTheme,
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
private fun LumpsumTrustChecklist() {
    val items = listOf(
        stringResource(Res.string.lumpsum_trust_safe_sebi),
        stringResource(Res.string.lumpsum_trust_withdraw_anytime),
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
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(10.dp)
                    )
                }
                Text(
                    text = line,
                    fontSize = 12.sp,
                    color = LumpsumInk.copy(alpha = 0.58f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun LumpsumStartInfoCard(title: String, subtitle: String) {
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
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LumpsumInk)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = subtitle, fontSize = 12.sp, color = LumpsumInk.copy(alpha = 0.55f), lineHeight = 18.sp)
            }
        }
    }
}
