package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.data.remote.model.dto.MandateStatus
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandateAuthScreen(
    userId: String = "",
    kycAttemptId: String = "",
    investorId: String = "",
    amount: Double = 0.0,
    mandateUrl: String = "",
    mandateId: Long = 0L,
    mandateRef: Long = 0L,
    goalId: String = "",
    onNavigateToHelp: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onGoToHome: () -> Unit = {},
    viewModel: MandateAuthModel = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var upiAppClicked by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showShareSheet by remember { mutableStateOf(false) }

    val mandateProductType = remember(goalId, mandateUrl) {
        resolveMandateProductType(goalId, mandateUrl)
    }

    var is30SecondsPassed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.mandateStatus) {
        if (uiState.mandateStatus == MandateStatus.APPROVED) {
            delay(30_000L)
            is30SecondsPassed = true
        }
    }

    LaunchedEffect(uiState.mandateStatus, mandateRef, userId) {
        if (uiState.mandateStatus == MandateStatus.APPROVED && mandateRef > 0L) {
            viewModel.startPlanPollingAfterApproval(userId = userId, mandateRef = mandateRef)
        }
    }

    // Start/stop polling when switching tabs — mirrors Android behaviour
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && mandateId > 0L && mandateRef > 0L) {
            platformLog("MandateAuthScreen: QR tab selected, starting mandate sync")
            viewModel.startMandateSync(userId, mandateId, mandateRef)
        } else if (selectedTabIndex == 0 && !upiAppClicked) {
            platformLog("MandateAuthScreen: UPI tab selected, stopping QR polling")
            viewModel.resetPollingState()
        }
    }

    LaunchedEffect(Unit) {
        platformLog("MandateAuthScreen: mandateId=$mandateId, mandateRef=$mandateRef, goalId=$goalId")
        if (mandateUrl.isNotBlank()) {
            platformLog("MandateAuthScreen: received UPI mandate URL")
        }
    }

    val mandateStatus = uiState.mandateStatus
    val isShowingMandateResponse = uiState.error != null ||
            (mandateStatus != null && isFinalStatus(mandateStatus))
    val isBackDisabled = isShowingMandateResponse

    val parsedAmount = remember(mandateUrl) {
        extractAmountFromMandateUri(mandateUrl) ?: amount.takeIf { it > 0 }?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else formatTwoDecimals(it)
        }
    }

    // Custom header + body layout (no Scaffold) matching Android structure
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Help / language row — hidden while polling
            if (!upiAppClicked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateToHelp) {
                        Text(
                            text = "Help",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Header bar
            Surface(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isBackDisabled) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mandate Setup",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // White main content area
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        when {
                            uiState.error != null -> {
                                StatusDisplay(
                                    icon = Icons.Default.Error,
                                    iconTint = Color.Red,
                                    title = "Verification Timeout",
                                    description = uiState.error ?: "An error occurred. Please try again."
                                )
                            }

                            mandateStatus != null && isFinalStatus(mandateStatus) -> {
                                if (mandateStatus != MandateStatus.APPROVED) {
                                    StatusDisplay(
                                        icon = Icons.Default.Error,
                                        iconTint = Color.Red,
                                        title = "Mandate ${mandateStatus.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        description = "Please try again or contact support."
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MandateApprovedWaitingContent(
                                        mandateUrl = mandateUrl,
                                        progress = uiState.planSetupProgress,
                                        isPlanReady = uiState.isPlanReady,
                                        isPlanResponseResolved = uiState.planPollingResolved,
                                        productType = mandateProductType
                                    )
                                }
                            }

                            upiAppClicked || (selectedTabIndex == 0 && (uiState.isLoading || uiState.requiresPolling)) -> {
                                LoadingDisplay()
                            }

                            mandateUrl.isNotBlank() -> {
                                // Amount display — mirrors Android's parsedUpiData section
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Pyllar",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (parsedAmount != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Daily SIP Amount",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        )
                                        Text(
                                            text = "₹ $parsedAmount",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 32.sp
                                            )
                                        )
                                        Text(
                                            text = "Ensure you use the bank account linked with Pyllar.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            else -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Loading mandate details…",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // Fixed bottom actions (shown only after final response)
                    if (isShowingMandateResponse) {
                        val status = uiState.mandateStatus
                        val isBtnEnabled = if (status == MandateStatus.APPROVED) {
                            uiState.planPollingResolved || uiState.planPollingTimedOut || is30SecondsPassed
                        } else true

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (status == MandateStatus.APPROVED && isBtnEnabled) {
                                Button(
                                    onClick = { showShareSheet = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Text("Share with Family", fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = onGoToHome,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                ),
                                enabled = isBtnEnabled
                            ) {
                                Text("Go to Home", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(6.dp))
                        Text("Secured by Pyllar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }

            // Fixed bottom panel — tabs + UPI / QR (hidden after final response or while loading)
            if (mandateUrl.isNotBlank() && !isShowingMandateResponse) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val tabs = listOf("UPI App", "QR Code")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTabIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedTabIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (selectedTabIndex == 0) {
                            // UPI App tab
                            Button(
                                onClick = {
                                    upiAppClicked = true
                                    platformLog("MandateAuthScreen: launching UPI URL")
                                    if (mandateUrl.isNotBlank()) {
                                        platformActions.openUrl(mandateUrl)
                                    }
                                    scope.launch {
                                        delay(10_000L)
                                        viewModel.startMandateSync(userId, mandateId, mandateRef)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Open UPI App", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // QR Code tab — placeholder (no ZXing in commonMain)
                            Card(
                                modifier = Modifier.size(200.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = Color.Gray
                                        )
                                        Text("Scan QR code in UPI app", style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        val amountStr = parsedAmount ?: amount.toString()
        val gName = when (mandateProductType) {
            MandateProductType.GOLD -> "Pyllar Gold"
            MandateProductType.SILVER -> "Pyllar Silver"
            MandateProductType.SAVINGS -> "Pyllar Savings"
            MandateProductType.FESTIVAL_SPENDS -> "Festivals"
            MandateProductType.ALL_IN_ONE -> "All-in-One"
            MandateProductType.GLOBAL_EXPOSURE -> "Global Exposure"
        }
        val accentCol = when (mandateProductType) {
            MandateProductType.GOLD -> Color(0xFFC8860A)
            MandateProductType.SILVER -> Color(0xFF6B7280)
            MandateProductType.SAVINGS -> Color(0xFF4CAF50)
            MandateProductType.FESTIVAL_SPENDS -> Color(0xFFFF9800)
            MandateProductType.ALL_IN_ONE -> Color(0xFF7B1FA2)
            MandateProductType.GLOBAL_EXPOSURE -> Color(0xFF2196F3)
        }
        ShareWithFamilyBottomSheet(
            onDismiss = { showShareSheet = false },
            amount = amountStr,
            goalName = gName,
            accentColor = accentCol,
            platformActions = platformActions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareWithFamilyBottomSheet(
    onDismiss: () -> Unit,
    amount: String,
    goalName: String,
    accentColor: Color,
    platformActions: PlatformActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareMessage = "I just started a ₹$amount daily SIP in $goalName with Pyllar! 🚀 Start your investment journey: https://pyllar.in"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Share with Family",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Invite your family to start saving daily",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Share card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = accentColor, modifier = Modifier.size(40.dp))
                    Text(goalName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accentColor)
                    Text("₹$amount / day", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Daily SIP • via Pyllar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Button(
                onClick = { platformActions.shareText(shareMessage, "Share via WhatsApp") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Text("Share on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = { platformActions.shareText(shareMessage) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusDisplay(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(80.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoadingDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp)
            Text("🔒", fontSize = 48.sp)
        }
        Text("Verifying Mandate…", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Please do not close the app or go back. This may take a moment.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MandateApprovedWaitingContent(
    mandateUrl: String,
    progress: Int,
    isPlanReady: Boolean,
    isPlanResponseResolved: Boolean,
    productType: MandateProductType
) {
    val amountText = remember(mandateUrl) { extractAmountFromMandateUri(mandateUrl) }
    val safeProgress = remember(progress) { progress.coerceIn(20, 100) }

    val accentColor = when (productType) {
        MandateProductType.GOLD -> Color(0xFFC8860A)
        MandateProductType.SILVER -> Color(0xFF6B7280)
        MandateProductType.SAVINGS -> Color(0xFF4CAF50)
        MandateProductType.FESTIVAL_SPENDS -> Color(0xFFFF9800)
        MandateProductType.ALL_IN_ONE -> Color(0xFF7B1FA2)
        MandateProductType.GLOBAL_EXPOSURE -> Color(0xFF2196F3)
    }
    val lightBackground = when (productType) {
        MandateProductType.GOLD -> Color(0xFFFFFBF5)
        MandateProductType.SILVER -> Color(0xFFF7F8FA)
        MandateProductType.SAVINGS -> Color(0xFFF3FBF4)
        MandateProductType.FESTIVAL_SPENDS -> Color(0xFFFFF8E1)
        MandateProductType.ALL_IN_ONE -> Color(0xFFFBF3FF)
        MandateProductType.GLOBAL_EXPOSURE -> Color(0xFFE3F2FD)
    }
    val title = when (productType) {
        MandateProductType.GOLD -> "Your Gold is being secured"
        MandateProductType.SILVER -> "Your Silver is being secured"
        MandateProductType.SAVINGS -> "Your Savings are being secured"
        MandateProductType.FESTIVAL_SPENDS -> "Your Festivals are being secured"
        MandateProductType.ALL_IN_ONE -> "Your All-in-One is being secured"
        MandateProductType.GLOBAL_EXPOSURE -> "Your Global Exposure is being secured"
    }

    val step2Done = isPlanReady
    val step3Done = isPlanReady
    val sipStartDay = remember { getInvestmentStatus() }

    // Spinning ring animation (same as Android)
    val ringRotation by rememberInfiniteTransition(label = "starRingTransition").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRingRotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(shape = CircleShape, color = lightBackground, modifier = Modifier.size(86.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        if (safeProgress >= 100) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(30.dp).align(Alignment.BottomEnd)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            CircularProgressIndicator(
                                progress = { 0.25f },
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.BottomEnd)
                                    .rotate(ringRotation),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.25f),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Sit back! We are setting up your automated daily savings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WaitingStepRow("Mandate setup success", "UPI mandate authorized by you", true, true, accentColor)

                val step2Title = if (step2Done) "Order plan created" else "Creating order plan"
                val step2Sub = if (step2Done) {
                    "₹${amountText ?: "your"} daily SIP plan is ready"
                } else {
                    "Allocating ₹${amountText ?: "your amount"} to your daily SIP"
                }
                WaitingStepRow(step2Title, step2Sub, step2Done, true, accentColor)

                val step3Title = if (step3Done) "SIP starts $sipStartDay" else "Daily SIP starting"
                val step3Sub = stepThreeSubtitleForSipStartDay(step3Done, amountText, sipStartDay)
                WaitingStepRow(step3Title, step3Sub, step3Done, false, accentColor)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = lightBackground),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Setting up your plan…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                "$safeProgress%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { safeProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp)),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.24f)
                        )
                        Text(
                            text = when {
                                isPlanReady -> "All done! Your SIP is active."
                                isPlanResponseResolved -> "Response received, finalising…"
                                else -> "Setting up your daily SIP…"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text(
                    text = "Your first SIP will be debited automatically once the plan is ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun WaitingStepRow(title: String, subtitle: String, isDone: Boolean, showConnector: Boolean, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isDone) Color(0xFFE8F5E9) else accentColor.copy(alpha = 0.14f))
                    .border(0.8.dp, if (isDone) Color(0xFF4CAF50) else accentColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isDone) Color(0xFF2E7D32) else accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (showConnector) {
                Box(modifier = Modifier.width(1.dp).height(26.dp).background(accentColor.copy(alpha = 0.25f)))
            }
        }
        Column(
            modifier = Modifier.padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

private fun stepThreeSubtitleForSipStartDay(step3Done: Boolean, amountText: String?, sipStartDay: String): String {
    val amount = amountText ?: "your"
    return if (step3Done) {
        "Your ₹$amount daily SIP will start from $sipStartDay"
    } else {
        "Starting your ₹$amount daily SIP from $sipStartDay"
    }
}

private enum class MandateProductType { GOLD, SILVER, SAVINGS, FESTIVAL_SPENDS, ALL_IN_ONE, GLOBAL_EXPOSURE }

private fun resolveMandateProductType(goalId: String, mandateUrl: String): MandateProductType {
    val trimmed = goalId.trim()
    if (trimmed.isNotEmpty()) {
        return when (identifyGoalType(trimmed)) {
            GoalType.GOLD -> MandateProductType.GOLD
            GoalType.SILVER -> MandateProductType.SILVER
            GoalType.SAVINGS -> MandateProductType.SAVINGS
            GoalType.FESTIVAL_SPENDS -> MandateProductType.FESTIVAL_SPENDS
            GoalType.ALL_IN_ONE -> MandateProductType.ALL_IN_ONE
            GoalType.GLOBAL_EXPOSURE -> MandateProductType.GLOBAL_EXPOSURE
            else -> MandateProductType.SAVINGS
        }
    }
    val lower = mandateUrl.lowercase()
    return when {
        "gold" in lower -> MandateProductType.GOLD
        "silver" in lower -> MandateProductType.SILVER
        else -> MandateProductType.SAVINGS
    }
}

private fun extractAmountFromMandateUri(mandateUrl: String): String? {
    return try {
        val params = mandateUrl.substringAfter("?").split("&")
        // fam = fixed amount mandate (UPI mandate spec); fall back to am
        val amParam = params.find { it.startsWith("fam=") } ?: params.find { it.startsWith("am=") }
        val raw = amParam?.substringAfter("=") ?: return null
        raw.toDoubleOrNull()?.let { v ->
            if (v % 1.0 == 0.0) v.toInt().toString() else formatTwoDecimals(v)
        } ?: raw
    } catch (_: Exception) {
        null
    }
}

private fun isFinalStatus(status: MandateStatus): Boolean {
    return status == MandateStatus.APPROVED || status == MandateStatus.REJECTED || status == MandateStatus.CANCELLED
}

private fun formatTwoDecimals(value: Double): String {
    val rounded = kotlin.math.round(value * 100).toInt()
    val intPart = rounded / 100
    val fracPart = kotlin.math.abs(rounded % 100)
    return "$intPart.${fracPart.toString().padStart(2, '0')}"
}
