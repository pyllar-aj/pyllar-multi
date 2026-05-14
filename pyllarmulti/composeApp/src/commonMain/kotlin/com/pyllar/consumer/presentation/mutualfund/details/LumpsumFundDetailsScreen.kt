package com.pyllar.consumer.presentation.mutualfund.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.CompanyAllocationDto
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.presentation.dashboard.KycPendingBottomSheet
import com.pyllar.consumer.presentation.dashboard.formatIndian
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LumpsumFundDetailsScreen(
    isin: String = "",
    userId: String = "",
    goalId: String = "",
    lumpsumAmount: Double = 0.0,
    kycAttemptId: String = "",
    investorId: String = "",
    onBackClick: () -> Unit,
    onLumpsumCreated: (Double, String?, com.pyllar.consumer.data.remote.model.dto.MandateWrapper?) -> Unit = { _, _, _ -> },
    viewModel: FundDetailsViewModel = koinInject(),
    dashboardViewModel: InvestmentDashboardV2ViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    
    val timeoutState = rememberTimeoutState("LumpsumFundDetails", "invest")
    var isInvestLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showKycPendingBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("LumpsumFundDetails")
    }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            dashboardViewModel.loadDashboardData(userId)
        }
    }

    LaunchedEffect(isin, userId, goalId) {
        if (isin.isNotBlank()) {
            viewModel.loadFundDetails(isin)
        } else if (userId.isNotBlank() && goalId.isNotBlank()) {
            viewModel.loadFundDetailsByGoal(userId, goalId)
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.fundDetails?.fundName ?: "Fund Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            LumpsumFundDetailsBottomBar(
                isLoading = isInvestLoading,
                isLoadingDashboard = dashboardState.isLoading,
                isEnabled = state.fundDetails != null,
                timeoutState = timeoutState,
                accountNumber = state.bankAccountNumber,
                ifscCode = state.bankIfscCode,
                bankName = state.bankName,
                lumpsumAmount = lumpsumAmount,
                onInvestClick = {
                    if (lumpsumAmount <= 0) {
                        errorMessage = "Invalid amount"
                        return@LumpsumFundDetailsBottomBar
                    }
                    
                    val currentKycStatus = dashboardState.kycStatus
                    com.pyllar.consumer.util.platformLog("LumpsumFundDetails: Checking KYC status before investment: $currentKycStatus")
                    
                    val isKycPending = currentKycStatus.equals("PENDING", ignoreCase = true) ||
                            currentKycStatus.equals("IN_PROGRESS", ignoreCase = true) ||
                            currentKycStatus.equals("EXPIRED", ignoreCase = true) ||
                            currentKycStatus.equals("REJECTED", ignoreCase = true)
                    
                    if (isKycPending) {
                        com.pyllar.consumer.util.platformLog("LumpsumFundDetails: Blocking investment due to pending/rejected KYC status: $currentKycStatus")
                        showKycPendingBottomSheet = true
                        return@LumpsumFundDetailsBottomBar
                    }

                    if (isInvestLoading) return@LumpsumFundDetailsBottomBar
                    isInvestLoading = true

                    coroutineScope.launch {
                        errorMessage = null
                        com.pyllar.consumer.util.platformLog("LumpsumFundDetails: Starting createLumpsumPurchase for amount: $lumpsumAmount")
                        val result = viewModel.createLumpsumPurchase(userId, lumpsumAmount)
                        com.pyllar.consumer.util.platformLog("LumpsumFundDetails: createLumpsumPurchase result: $result")
                        isInvestLoading = false
                        
                        when (result) {
                            is SipCreationResult.LumpsumSuccess -> {
                                com.pyllar.consumer.util.platformLog("LumpsumFundDetails: Success. nextScreen: ${result.nextScreen}, hasData: ${result.lumpsumData != null}")
                                val mappedData = result.lumpsumData?.let { data ->
                                    com.pyllar.consumer.data.remote.model.dto.MandateWrapper(
                                        finMandateId = data.old_id ?: 0L,
                                        mandateId = data.payment_id ?: 0L,
                                        uri = data.token_url
                                    )
                                }
                                onLumpsumCreated(lumpsumAmount, result.nextScreen, mappedData)
                            }
                            is SipCreationResult.Failure -> {
                                com.pyllar.consumer.util.platformLog("LumpsumFundDetails: Failure: ${result.message}")
                                errorMessage = result.message
                            }
                            else -> {
                                com.pyllar.consumer.util.platformLog("LumpsumFundDetails: Unexpected result type")
                                errorMessage = "Unexpected error"
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else {
                state.fundDetails?.let { details ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        FundHeader(details)
                        
                        FundChartSection(
                            state = state,
                            onPeriodSelected = { viewModel.onPeriodSelected(it) }
                        )

                        LumpsumFundMetricsGrid(details)
                        
                        val riskLevel = remember(details.riskLevel) {
                            getLumpsumRiskLevel(details.riskLevel)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Riskometer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LumpsumSemiCircleRiskometer(
                                    riskLevel = riskLevel,
                                    size = 80.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = riskLevel.label.replace("\n", " "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        LumpsumCompanyAllocationSection(details.companyAllocation)
                    }
                }
            }
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

@Composable
fun LumpsumFundMetricsGrid(details: FundDetailsResponseDto) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider()
        Text("Fund Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.fillMaxWidth()) {
            LumpsumMetricItem(
                label = "Expense Ratio",
                value = details.expenseRatio?.let { "$it%" } ?: "-",
                modifier = Modifier.weight(1f)
            )
            LumpsumMetricItem(
                label = "Exit Load",
                value = details.exitLoad?.let { "$it%" } ?: "-",
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            LumpsumMetricItem(
                label = "Fund Size (AUM)",
                value = details.aum?.let { "₹${formatIndian(it)} Cr" } ?: "-",
                modifier = Modifier.weight(1f)
            )
            LumpsumMetricItem(
                label = "Lock-in period",
                value = "-",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LumpsumMetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LumpsumFundDetailsBottomBar(
    isLoading: Boolean,
    isLoadingDashboard: Boolean,
    isEnabled: Boolean,
    timeoutState: com.pyllar.consumer.presentation.ui.components.TimeoutState,
    accountNumber: String?,
    ifscCode: String?,
    bankName: String?,
    lumpsumAmount: Double,
    onInvestClick: () -> Unit
) {
    Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accountNumber != null || ifscCode != null) {
                BankDetailsCard(accountNumber, ifscCode, bankName)
            }
            
            TimeoutButton(
                onClick = onInvestClick,
                enabled = !isLoading && !isLoadingDashboard && isEnabled,
                timeoutState = timeoutState,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isLoading || isLoadingDashboard) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        if (lumpsumAmount > 0) "Invest ₹${formatIndian(lumpsumAmount)} one-time" else "Invest Now",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LumpsumCompanyAllocationSection(companyAllocation: List<CompanyAllocationDto>?) {
    val validRows = companyAllocation?.filter { it.company != null && it.allocation != null }.orEmpty()
    if (validRows.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val displayRows = if (expanded) validRows else validRows.take(3)
    val hasMore = validRows.size > 3

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Companies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Company", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Allocation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        
        displayRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.company ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${row.allocation}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
        
        if (hasMore && !expanded) {
            Text(
                "View all",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { expanded = true }
            )
        }
    }
}

enum class LumpsumRiskLevel(val label: String, val color: Color) {
    LOW("Low", Color(0xFF4CAF50)),
    LOW_TO_MODERATE("Low to\nModerate", Color(0xFF8BC34A)),
    MODERATE("Moderate", Color(0xFFFFC107)),
    MODERATELY_HIGH("Moderately\nHigh", Color(0xFFFF9800)),
    HIGH("High", Color(0xFFFF5722)),
    VERY_HIGH("Very\nHigh", Color(0xFFF44336))
}

fun getLumpsumRiskLevel(level: String?): LumpsumRiskLevel {
    return when(level?.uppercase()?.replace(" ", "_")) {
        "LOW" -> LumpsumRiskLevel.LOW
        "LOW_TO_MODERATE" -> LumpsumRiskLevel.LOW_TO_MODERATE
        "MODERATE" -> LumpsumRiskLevel.MODERATE
        "MODERATELY_HIGH" -> LumpsumRiskLevel.MODERATELY_HIGH
        "HIGH" -> LumpsumRiskLevel.HIGH
        "VERY_HIGH" -> LumpsumRiskLevel.VERY_HIGH
        else -> LumpsumRiskLevel.MODERATE
    }
}

@Composable
fun LumpsumSemiCircleRiskometer(
    riskLevel: LumpsumRiskLevel,
    size: androidx.compose.ui.unit.Dp = 100.dp
) {
    Box(
        modifier = Modifier.size(size, size / 2 + 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.size(size, size / 2)) {
            val radius = size.toPx() / 2
            val levels = LumpsumRiskLevel.entries
            val sweepPerSegment = 180f / levels.size
            
            levels.forEachIndexed { index, level ->
                drawArc(
                    color = level.color,
                    startAngle = 180f + (index * sweepPerSegment),
                    sweepAngle = sweepPerSegment,
                    useCenter = true,
                    size = androidx.compose.ui.geometry.Size(size.toPx(), size.toPx())
                )
            }
            
            // Draw Needle
            val targetAngle = 180f + (riskLevel.ordinal * sweepPerSegment) + (sweepPerSegment / 2)
            val angleRad = (targetAngle * (kotlin.math.PI / 180f)).toFloat()
            val needleLength = radius * 0.7f
            val endX = radius + needleLength * cos(angleRad)
            val endY = radius + needleLength * sin(angleRad)
            
            drawLine(
                color = Color.Black,
                start = Offset(radius, radius),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            drawCircle(color = Color.Black, radius = 4.dp.toPx(), center = Offset(radius, radius))
        }
    }
}
