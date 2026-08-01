package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.compottie.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import androidx.compose.ui.graphics.painter.Painter
import pyllar.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import pyllar.composeapp.generated.resources.Res
import com.pyllar.consumer.data.remote.model.dto.MandateStatus
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.platform.UpiAppInfo
import com.pyllar.consumer.util.BackHandler
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pyllar.consumer.util.*
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
import com.pyllar.consumer.presentation.ui.theme.V2SubtleBorder
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
    
    val sessionStore: SessionStore = koinInject()
    var sipFrequency by remember { mutableStateOf("daily") }
    var sipInstallmentDay by remember { mutableStateOf("") }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var upiAppClicked by remember { mutableStateOf(false) }
    
    // Allow back button as requested
    BackHandler(enabled = false) {
        // Do nothing
    }
    var availableUpiApps by remember { mutableStateOf<List<UpiAppInfo>>(emptyList()) }
    var showMoreUpiAppsSheet by remember { mutableStateOf(false) }
    var is30SecondsPassed by remember { mutableStateOf(false) }

    val goalType = remember(goalId) { identifyGoalType(goalId) }

    LaunchedEffect(Unit) {
        platformLog("MandateAuthScreen: 📋 Received Parameters - mandateId: $mandateId, mandateRef: $mandateRef, mandateUrl: $mandateUrl")
        availableUpiApps = platformActions.getInstalledUpiApps()
        try {
            sipFrequency = sessionStore.getValue("sip_frequency") ?: "daily"
            sipInstallmentDay = sessionStore.getValue("sip_installment_day") ?: ""
            platformLog("MandateAuthScreen: Loaded sipFrequency='$sipFrequency', sipInstallmentDay='$sipInstallmentDay'")
            
            val storedTabIndex = sessionStore.getValue("selected_tab_index_${mandateId}")
            if (storedTabIndex != null) {
                selectedTabIndex = storedTabIndex.toInt()
            }
            
            val mandateStatusVal = sessionStore.getValue("mandate_status_${mandateId}")
            if (mandateStatusVal == "APPROVED") {
                viewModel.startMandateSync(userId, mandateId, mandateRef)
            } else {
                val clicked = sessionStore.getValue("upi_app_clicked_${mandateId}") == "true"
                if (clicked) {
                    upiAppClicked = true
                    viewModel.startMandateSync(userId, mandateId, mandateRef)
                }
            }
        } catch (e: Exception) {
            platformLog("MandateAuthScreen: Failed to load state: ${e.message}")
        }
    }

    LaunchedEffect(selectedTabIndex) {
        scope.launch {
            try {
                sessionStore.saveValue("selected_tab_index_${mandateId}", selectedTabIndex.toString())
            } catch (_: Exception) {}
        }
        if (selectedTabIndex == 1 && mandateId > 0L && mandateRef > 0L) {
            viewModel.startMandateSync(userId, mandateId, mandateRef)
        } else if (selectedTabIndex == 0 && !upiAppClicked) {
            viewModel.resetPollingState()
        }
    }

    LaunchedEffect(uiState.mandateStatus) {
        if (uiState.mandateStatus == MandateStatus.APPROVED) {
            scope.launch {
                try {
                    sessionStore.saveValue("mandate_status_${mandateId}", "APPROVED")
                } catch (_: Exception) {}
            }
            delay(30_000L)
            is30SecondsPassed = true
        }
    }

    LaunchedEffect(uiState.mandateStatus, mandateRef, userId) {
        if (uiState.mandateStatus == MandateStatus.APPROVED && mandateRef > 0L) {
            viewModel.startPlanPollingAfterApproval(userId = userId, mandateRef = mandateRef)
        }
    }

    val isFinalStatus = uiState.mandateStatus != null && 
        (uiState.mandateStatus == MandateStatus.APPROVED || 
         uiState.mandateStatus == MandateStatus.REJECTED || 
         uiState.mandateStatus == MandateStatus.CANCELLED)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIP Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isFinalStatus) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToHelp) {
                        Text("Help", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
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
                            description = uiState.error ?: "An error occurred. Please try again.",
                            actionText = "Go to Home",
                            onAction = onGoToHome
                        )
                    }

                    uiState.mandateStatus != null && isFinalStatus -> {
                        val status = uiState.mandateStatus!!
                        if (status == MandateStatus.APPROVED) {
                            MandateApprovedWaitingContent(
                                mandateUrl = mandateUrl,
                                progress = uiState.planSetupProgress,
                                isPlanReady = uiState.isPlanReady,
                                isPlanResponseResolved = uiState.planPollingResolved,
                                goalType = goalType,
                                sipFrequency = sipFrequency,
                                sipInstallmentDay = sipInstallmentDay,
                                amount = amount
                            )
                        } else {
                            StatusDisplay(
                                icon = Icons.Default.Error,
                                iconTint = Color.Red,
                                title = "SIP ${status.name}",
                                description = "Please try again or contact support.",
                                actionText = "Go to Home",
                                onAction = onGoToHome
                            )
                        }
                    }

                    upiAppClicked || (selectedTabIndex == 0 && uiState.isLoading) -> {
                        LoadingDisplay()
                    }

                    else -> {
                        // Pyllar Logo & Brand
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Security, 
                                contentDescription = null, 
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Pyllar",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Amount section
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Amount", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "₹1.00",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Refunded within 2 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Bottom Buttons for Final State
            if (isFinalStatus || uiState.error != null) {
                val status = uiState.mandateStatus
                val isBtnEnabled = if (status == MandateStatus.APPROVED) {
                    uiState.planPollingResolved || uiState.planPollingTimedOut || is30SecondsPassed
                } else true

                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onGoToHome,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isBtnEnabled
                    ) {
                        Text("Go to Home", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (uiState.error == null) {
                // Tabbed Bottom Panel - Keep visible during syncing to allow switching methods
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                                Text("Choose UPI App", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                            }
                            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                                Text("Scan QR Code", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedTabIndex == 0) {
                            UpiAppGrid(
                                apps = availableUpiApps,
                                onAppClick = { app ->
                                    upiAppClicked = true
                                    scope.launch {
                                        try {
                                            sessionStore.saveValue("upi_app_clicked_${mandateId}", "true")
                                        } catch (_: Exception) {}
                                    }
                                    platformActions.openUpiUrl(mandateUrl, app.packageName)
                                    scope.launch {
                                        delay(1000L) // Shorter delay before showing loading
                                        viewModel.startMandateSync(userId, mandateId, mandateRef)
                                    }
                                },
                                onMoreClick = { showMoreUpiAppsSheet = true }
                            )
                        } else {
                            QrPlaceholder(mandateUrl, description = "Scan this QR with any UPI app")
                        }
                    }
                }
            }
        }
    }

    if (showMoreUpiAppsSheet) {
        MoreUpiAppsBottomSheet(
            apps = availableUpiApps,
            onDismiss = { showMoreUpiAppsSheet = false },
            onAppClick = { app ->
                showMoreUpiAppsSheet = false
                upiAppClicked = true
                scope.launch {
                    try {
                        sessionStore.saveValue("upi_app_clicked_${mandateId}", "true")
                    } catch (_: Exception) {}
                }
                platformActions.openUpiUrl(mandateUrl, app.packageName)
                scope.launch {
                    delay(1000L)
                    viewModel.startMandateSync(userId, mandateId, mandateRef)
                }
            }
        )
    }
}

@Composable
private fun MoreUpiAppsCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("More...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreUpiAppsBottomSheet(apps: List<UpiAppInfo>, onDismiss: () -> Unit, onAppClick: (UpiAppInfo) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Choose UPI App", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
            ) {
                items(apps) { app ->
                    UpiAppCard(app = app, onClick = { onAppClick(app) })
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MandateApprovedWaitingContent(
    mandateUrl: String,
    progress: Int,
    isPlanReady: Boolean,
    isPlanResponseResolved: Boolean,
    goalType: GoalType,
    sipFrequency: String = "daily",
    sipInstallmentDay: String = "",
    amount: Double
) {
    val accentColor = when (goalType) {
        GoalType.GOLD -> Color(0xFFC8860A)
        GoalType.SILVER -> Color(0xFF6B7280)
        GoalType.SAVINGS -> V2SuccessGreen
        GoalType.FESTIVAL_SPENDS -> Color(0xFFFF9800)
        GoalType.ALL_IN_ONE -> Color(0xFF7B1FA2)
        GoalType.GLOBAL_EXPOSURE -> Color(0xFF2196F3)
        GoalType.MARKET_EXPLORER -> Color(0xFF0F6B5C)
        else -> MaterialTheme.colorScheme.primary
    }
    val lightBackground = V2SubtleBorder
    
    val sipStartDay = remember { getInvestmentStatus() }
    val isMonthly = sipFrequency.lowercase() == "monthly"
    val amountVal = amount.toString()

    val step2Title = if (isPlanReady) {
        "Saving plan created"
    } else {
        if (isMonthly) "Setting up your monthly plan" else "Setting up your saving plan"
    }

    val step2Subtitle = if (isPlanReady) {
        if (isMonthly) "₹$amountVal/month saving is registered" else "₹$amountVal/day saving is registered"
    } else {
        if (isMonthly) "Registering ₹$amountVal/month saving with your bank" else "Registering ₹$amountVal/day saving with your bank"
    }

    val step3Title = if (isPlanReady) {
        if (isMonthly) "Monthly saving is ready!" else "Daily saving is ready!"
    } else {
        if (isMonthly) "Monthly saving ready" else "Daily saving ready"
    }

    val step3Subtitle = if (isMonthly) {
        val dayText = if (sipInstallmentDay.isNotBlank()) {
            try {
                val day = sipInstallmentDay.toInt()
                val suffix = ordinalSuffix(day)
                "$day$suffix of every month"
            } catch (_: Exception) {
                "chosen day of every month"
            }
        } else {
            "chosen day of every month"
        }
        if (isPlanReady) {
            "Your monthly SIP of ₹$amountVal will start on $dayText"
        } else {
            "First debit of ₹$amountVal will happen on $dayText"
        }
    } else {
        if (isPlanReady) {
            if (sipStartDay == "Tomorrow") "First ₹$amountVal deducted tomorrow" else "First ₹$amountVal deducted on $sipStartDay"
        } else {
            if (sipStartDay == "Tomorrow") "First ₹$amountVal deduction pending — first debit tomorrow" else "First ₹$amountVal deduction pending — first debit on $sipStartDay"
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (isMonthly) "Setting up your Pyllar saving..." else "Setup in Progress...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = accentColor,
                    trackColor = lightBackground
                )

                Text(
                    text = if (isPlanReady) "Your investment plan is ready!" else "Allocating units to your portfolio...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                WaitingStepRow("SIP Approved", if (isMonthly) "Your monthly SIP is authorized." else "Your daily SIP is authorized.", true, true, accentColor)
                WaitingStepRow(step2Title, step2Subtitle, isPlanReady, true, accentColor)
                WaitingStepRow(step3Title, step3Subtitle, isPlanReady, false, accentColor)
            }
        }
    }
}

private fun ordinalSuffix(value: Int): String {
    return if (value in 11..13) {
        "th"
    } else when (value % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

@Composable
private fun WaitingStepRow(title: String, subtitle: String, isDone: Boolean, showConnector: Boolean, accentColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) V2SuccessGreen else accentColor
            )
            if (showConnector) {
                Box(modifier = Modifier.width(2.dp).height(20.dp).background(accentColor.copy(alpha = 0.3f)))
            }
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
