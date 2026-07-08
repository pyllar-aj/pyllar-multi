package com.pyllar.consumer.presentation.mutualfund.details

import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*
import kotlin.math.roundToInt
import kotlin.math.pow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.presentation.dashboard.formatIndian
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.pyllar.consumer.data.remote.model.dto.NavChartDataDto
import com.pyllar.consumer.data.remote.model.dto.FundReturnsDto
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.clickable
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.presentation.dashboard.KycPendingBottomSheet
import com.pyllar.consumer.presentation.ui.theme.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val V2Cream = Color(0xFFFBF9F4)
private val V2CreamTint = Color(0xFFF5EEDB)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2GoldDeep = Color(0xFF8B6B25)
private val V2GoldAccent = Color(0xFFD4AF37)
private val V2Obsidian = Color(0xFF0A2415)
private val V2LinkGreen = Color(0xFF1A7A42)
private val V2VolatilityRed = Color(0xFFC62828)
private val V2SuccessGreen = Color(0xFF2E7D32)
private val V2FieldBorder = Color(0xFFD7CCC8)
private val V2CardBorder = Color(0xFFEFEBE9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundDetailsScreen(
    isin: String = "",
    userId: String = "",
    goalId: String = "",
    sipAmount: Double = 0.0,
    kycAttemptId: String = "",
    investorId: String = "",
    fromSipAmount: Boolean = false,
    frequency: String = "daily",
    installmentDay: Int? = null,
    onBackClick: () -> Unit = {},
    onSipCreated: (Double, String?, com.pyllar.consumer.data.remote.model.dto.MandateWrapper?) -> Unit = { _, _, _ -> },
    viewModel: FundDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("FundDetails")
    }

    LaunchedEffect(isin, userId, goalId) {
        if (userId.isNotBlank()) {
            dashboardViewModel.loadDashboardData(userId)
        }
        
        if (isin.isNotBlank()) {
            viewModel.loadFundDetails(isin)
        } else if (userId.isNotBlank() && goalId.isNotBlank()) {
            viewModel.loadFundDetailsByGoal(userId, goalId)
        }
    }

    if (state.sipError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSipError() },
            title = { Text("Investment Failed") },
            text = { Text(state.sipError ?: "An unexpected error occurred. Please try again.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearSipError()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("Disclaimer") },
            text = {
                Column {
                    Text(stringResource(Res.string.disclaimer_popup_content))
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

    val v2ColorScheme = MaterialTheme.colorScheme.copy(
        background = V2Cream,
        surface = V2Cream,
        surfaceVariant = Color.White,
        inverseSurface = Color.White, // for cardBackground
        onSurface = V2BronzeInk,
        onSurfaceVariant = V2BronzeMuted,
        primary = Color(0xFF26533E), // Dark forest green
        primaryContainer = Color.White,
        onPrimaryContainer = V2BronzeInk
    )

    MaterialTheme(colorScheme = v2ColorScheme) {
        Scaffold(
            containerColor = V2Cream,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.fundDetails?.fundName ?: "Fund Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = V2BronzeInk,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = V2BronzeInk
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = V2Cream,
                        titleContentColor = V2BronzeInk,
                        navigationIconContentColor = V2BronzeInk
                    )
                )
            },
        bottomBar = {
            if (fromSipAmount) {
                FundDetailsBottomBar(
                    isLoading = state.isSipCreating,
                    isFetching = state.isLoading,
                    isEnabled = state.fundDetails != null,
                    sipAmount = sipAmount,
                    accountNumber = state.bankAccountNumber,
                    ifscCode = state.bankIfscCode,
                    bankName = state.bankName,
                    onInvestClick = {
                        if (sipAmount <= 0) return@FundDetailsBottomBar
                        
                        // Check KYC status before proceeding
                        val kycStatus = dashboardState.kycStatus
                        val isKycPending = !dashboardState.isLoading &&
                                (kycStatus.equals("PENDING", ignoreCase = true) ||
                                 kycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                                 kycStatus.equals("EXPIRED", ignoreCase = true))
                        
                        if (isKycPending) {
                            showKycPendingBottomSheet = true
                            return@FundDetailsBottomBar
                        }

                        coroutineScope.launch {
                            val result = if (frequency.equals("monthly", ignoreCase = true)) {
                                viewModel.createPurchasePlan(
                                    userId = userId,
                                    kycAttemptId = kycAttemptId,
                                    investorId = investorId,
                                    amount = sipAmount,
                                    frequency = "monthly",
                                    installmentDay = installmentDay
                                )
                            } else {
                                viewModel.createSip(
                                    userId = userId,
                                    kycAttemptId = kycAttemptId,
                                    investorId = investorId,
                                    amount = sipAmount
                                )
                            }
                            if (result is SipCreationResult.Success) {
                                onSipCreated(sipAmount, result.nextScreen, result.mandateWrapper)
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showKycPendingBottomSheet) {
                KycPendingBottomSheet(
                    onDismiss = { showKycPendingBottomSheet = false },
                    onRetryKyc = { showKycPendingBottomSheet = false },
                    kycStatus = dashboardState.kycStatus
                )
            }
            
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(state.error ?: "Failed to load fund details", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else {
                state.fundDetails?.let { details ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        FundHeader(details)
                        
                        FundChartSection(
                            state = state,
                            onPeriodSelected = { viewModel.onPeriodSelected(it) }
                        )

                        // Riskometer section - semi-circle matching native
                        val riskLevelObj = remember(details.riskLevel) {
                            getLumpsumRiskLevel(details.riskLevel)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.riskometer),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = V2BronzeInk
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LumpsumSemiCircleRiskometer(
                                    riskLevel = riskLevelObj,
                                    size = 80.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = riskLevelObj.label.replace("\n", " "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = V2BronzeInk
                                )
                            }
                        }

                        LumpsumCompanyAllocationSection(companyAllocation = details.companyAllocation)

                        FundMetricsGrid(details)

                        FundDescriptionSection(details.fundName ?: "")

                        // Footer Links (Scheme Docs & Disclaimer)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!details.schemeDocumentUrl.isNullOrBlank()) {
                                Text(
                                    text = stringResource(Res.string.scheme_documents),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = V2LinkGreen,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable {
                                            uriHandler.openUri(details.schemeDocumentUrl)
                                        }
                                        .padding(4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Text(
                                text = stringResource(Res.string.disclaimer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = V2BronzeMuted,
                                modifier = Modifier
                                    .clickable {
                                        showDisclaimerDialog = true
                                    }
                                    .padding(4.dp)
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
fun FundMetricsGrid(details: FundDetailsResponseDto) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(color = V2FieldBorder)
        Text(
            text = "Fund Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = V2BronzeInk
        )
        
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricItem(
                label = "Expense Ratio",
                value = details.expenseRatio?.let { "$it%" } ?: "-",
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "Exit Load",
                value = if (details.exitLoad != null && details.exitLoadPeriodDays != null && details.exitLoadPeriodDays > 0) {
                    "${details.exitLoad}% if exited within ${details.exitLoadPeriodDays} days"
                } else {
                    details.exitLoad?.let { "$it%" } ?: "-"
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricItem(
                label = "Fund Size (AUM)",
                value = details.aum?.let { "₹${formatDecimal(it)} Cr" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
            MetricItem(
                label = "Lock-in period",
                value = "-",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.6f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FundDetailsBottomBar(
    isLoading: Boolean,
    isFetching: Boolean,
    isEnabled: Boolean,
    sipAmount: Double,
    accountNumber: String?,
    ifscCode: String?,
    bankName: String?,
    onInvestClick: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp, 
        shadowElevation = 8.dp,
        color = V2Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bank Details (if available)
            if (accountNumber != null || ifscCode != null) {
                BankDetailsCard(
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    bankName = bankName
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SIP Amount", style = MaterialTheme.typography.labelSmall, color = V2BronzeMuted)
                    Text("₹$sipAmount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = V2BronzeInk)
                }
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .background(Brush.linearGradient(listOf(V2GoldAccent, V2GoldDeep)), RoundedCornerShape(50))
                        .padding(1.5.dp)
                ) {
                    Button(
                        onClick = onInvestClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading && !isFetching && isEnabled,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = V2Obsidian,
                            contentColor = V2Cream,
                            disabledContainerColor = V2Obsidian,
                            disabledContentColor = V2Cream
                        )
                    ) {
                        if (isLoading || isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = V2Cream)
                        } else {
                            Text("Invest Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun formatDecimal(value: Double, decimals: Int = 2): String {
    val factor = 10.0.pow(decimals)
    val roundedValue = kotlin.math.round(value * factor) / factor
    val intPart = roundedValue.toLong()
    val fracPart = kotlin.math.abs(kotlin.math.round((roundedValue - intPart) * factor).toLong())
    val fracStr = fracPart.toString().padStart(decimals, '0')
    return "$intPart.$fracStr"
}

private fun formatChartDate(dateString: String): String {
    return try {
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val year = parts[0].takeLast(2)
            val month = when (parts[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "May"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Aug"
                "09" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> parts[1]
            }
            val day = parts[2].toInt().toString()
            "$day $month '$year"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun FundChartSection(
    state: FundDetailsState,
    onPeriodSelected: (String) -> Unit
) {
    val category = state.fundDetails?.category?.uppercase() ?: ""
    val fundName = state.fundDetails?.fundName?.uppercase() ?: ""
    val isGold = category.contains("GOLD") || fundName.contains("GOLD")
    val isSilver = category.contains("SILVER") || fundName.contains("SILVER")
    
    val positiveColor = when {
        isGold -> Color(0xFFD4AF37) // Gold
        isSilver -> Color(0xFF6A9AB0) // Blue-grey silverish
        else -> V2SuccessGreen
    }
    val negativeColor = V2VolatilityRed  // Red
    val lineColor = if (state.isPositiveReturn || isGold || isSilver) positiveColor else negativeColor
    val statusColor = if (state.isPositiveReturn) V2SuccessGreen else V2VolatilityRed

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("1Y", "3Y", "5Y").forEach { period ->
                val selected = state.selectedPeriod == period
                FilterChip(
                    selected = selected,
                    onClick = { onPeriodSelected(period) },
                    label = { 
                        Text(
                            text = period,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        val returns = state.fundDetails?.returns
        if (returns != null) {
            val returnVal = when (state.selectedPeriod) {
                "1Y" -> returns.oneYear
                "3Y" -> returns.threeYear
                "5Y" -> returns.fiveYear
                else -> 0.0
            } ?: 0.0
            val returnColor = if (returnVal >= 0) V2SuccessGreen else V2VolatilityRed
            val sign = if (returnVal >= 0) "+" else ""
            
            Text(
                text = "$sign${formatDecimal(returnVal)}% CAGR (${state.selectedPeriod})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = returnColor
            )
        }

        if (state.chartData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No chart data available", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            SimpleNavChart(
                data = state.chartData,
                lineColor = lineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // Start & Current NAV indicator below the chart
            val currentNav = state.chartData.firstOrNull()?.nav
            val oldestNav = state.chartData.lastOrNull()?.nav
            if (currentNav != null && oldestNav != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.start_nav),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${formatDecimal(oldestNav)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(Res.string.current_nav),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${formatDecimal(currentNav)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleNavChart(
    data: List<NavChartDataDto>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val reversedData = remember(data) { data.reversed() }
    if (reversedData.size < 2) return

    var touchX by remember { mutableStateOf<Float?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(reversedData) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position
                            val isPressed = event.changes.any { it.pressed }
                            if (position != null && isPressed) {
                                touchX = position.x
                            } else {
                                touchX = null
                                selectedIndex = null
                            }
                        }
                    }
                }
        ) {
            val minNav = reversedData.minOf { it.nav }.toFloat()
            val maxNav = reversedData.maxOf { it.nav }.toFloat()
            val navRange = maxNav - minNav

            val minYVal = if (navRange > 0) (minNav - navRange * 0.2f) else (minNav * 0.8f)
            val maxYVal = if (navRange > 0) (maxNav + navRange * 0.05f) else (maxNav * 1.02f)
            val yRange = maxYVal - minYVal
            
            val width = size.width
            val height = size.height
            val padding = 8.dp.toPx()
            
            val usableHeight = height - (2 * padding)

            val myPath = Path()
            val fillPath = Path()

            val points = reversedData.mapIndexed { index, point ->
                val x = (index.toFloat() / (reversedData.size - 1)) * width
                val y = padding + usableHeight - ((point.nav.toFloat() - minYVal) / yRange.coerceAtLeast(0.1f)) * usableHeight
                Offset(x, y)
            }

            points.forEachIndexed { index, point ->
                if (index == 0) {
                    myPath.moveTo(point.x, point.y)
                    fillPath.moveTo(point.x, height)
                    fillPath.lineTo(point.x, point.y)
                } else {
                    myPath.lineTo(point.x, point.y)
                    fillPath.lineTo(point.x, point.y)
                }
                
                if (index == reversedData.size - 1) {
                    fillPath.lineTo(point.x, height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)
                ),
                style = Fill
            )

            val strokeWidth = 3.dp.toPx()
            drawPath(
                path = myPath,
                color = lineColor,
                style = Stroke(width = strokeWidth)
            )

            touchX?.let { tx ->
                val relativeX = tx
                val index = (relativeX / width * (reversedData.size - 1))
                    .roundToInt()
                    .coerceIn(0, reversedData.size - 1)
                
                selectedIndex = index
                val selectedPoint = points[index]

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(selectedPoint.x, padding),
                    end = Offset(selectedPoint.x, height - padding),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = selectedPoint
                )

                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = selectedPoint
                )
            }
        }

        selectedIndex?.let { index ->
            val point = reversedData[index]
            val formattedNav = formatDecimal(point.nav)
            val formattedDate = formatChartDate(point.date)

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NAV: ₹$formattedNav",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun RiskometerSection(riskLevel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Riskometer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 12.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            ) {
                val progress = when(riskLevel.uppercase()) {
                    "LOW" -> 0.2f
                    "MODERATELY_LOW" -> 0.4f
                    "MODERATE" -> 0.6f
                    "MODERATELY_HIGH" -> 0.8f
                    "HIGH" -> 0.9f
                    "VERY_HIGH" -> 1.0f
                    else -> 0.5f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(getRiskColor(riskLevel), CircleShape)
                )
            }
            Text(
                riskLevel.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = getRiskColor(riskLevel)
            )
        }
        Text(
            "This fund has ${riskLevel.lowercase().replace("_", " ")} risk",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.7f)
        )
    }
}

fun getRiskColor(riskLevel: String): Color {
    return when(riskLevel.uppercase()) {
        "LOW", "MODERATELY_LOW" -> V2SuccessGreen
        "MODERATE" -> Color(0xFFFFC107)
        "MODERATELY_HIGH" -> Color(0xFFFF9800)
        "HIGH", "VERY_HIGH" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

@Composable
fun FundDescriptionSection(name: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("About the Fund", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "$name is an open-ended equity scheme that seeks to generate long-term capital appreciation by investing in a diversified portfolio of companies.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alpha(0.8f)
        )
    }
}
