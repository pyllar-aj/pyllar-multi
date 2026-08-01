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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.platform.UpiAppInfo
import com.pyllar.consumer.util.BackHandler
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pyllar.consumer.util.*
import org.koin.compose.koinInject
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
import com.pyllar.consumer.presentation.ui.theme.V2SubtleBorder
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun LumpsumPurchaseAuthScreen(
    userId: String = "",
    kycAttemptId: String = "",
    investorId: String = "",
    amount: Double = 0.0,
    paymentUrl: String = "",
    paymentId: Long = 0L,
    paymentRef: Long = 0L,
    goalId: String = "",
    onNavigateToHelp: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onGoToHome: () -> Unit = {},
    viewModel: LumpsumPurchaseAuthViewModel = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var upiAppClicked by remember { mutableStateOf(false) }
    
    val isFinalStatus = uiState.status != PurchaseStatus.PENDING || uiState.errorMessage != null
    
    BackHandler(enabled = false) {
        // Allow back as requested
    }

    var availableUpiApps by remember { mutableStateOf<List<UpiAppInfo>>(emptyList()) }
    var is30SecondsPassed by remember { mutableStateOf(false) }

    val goalType = remember(goalId) { identifyGoalType(goalId) }
    val (accentColor, lightBackground) = remember(goalType) { accentColorsForLumpsumGoal(goalType) }

    val tealPrimary = Color(0xFF0D7B6B)
    val tealPrimaryDark = Color(0xFF0A5F54)
    val tealSurfaceVariant = Color(0xFF0D7B6B).copy(alpha = 0.12f)

    LaunchedEffect(Unit) {
        availableUpiApps = platformActions.getInstalledUpiApps()
        if (paymentUrl.isBlank()) {
            platformLog("LumpsumPurchaseAuthScreen: Missing paymentUrl")
        }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1 && paymentId > 0L) {
            viewModel.startPaymentSync(userId, paymentId)
        } else if (selectedTabIndex == 0 && !upiAppClicked) {
            viewModel.resetPollingState()
        }
    }

    LaunchedEffect(uiState.status) {
        if (uiState.status == PurchaseStatus.SUCCESS) {
            delay(30_000L)
            is30SecondsPassed = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("One-time Purchase", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = tealPrimaryDark),
                navigationIcon = {
                    if (!isFinalStatus) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToHelp) {
                        Text("Help", color = Color.White)
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
                    uiState.errorMessage != null || uiState.status == PurchaseStatus.FAILED || uiState.status == PurchaseStatus.CANCELLED -> {
                        StatusDisplay(
                            icon = Icons.Default.Error,
                            iconTint = Color.Red,
                            title = if (uiState.status == PurchaseStatus.CANCELLED) "Payment Cancelled" else "Payment Failed",
                            description = uiState.errorMessage ?: "An error occurred. Please try again or contact support.",
                            actionText = "Go to Home",
                            onAction = onGoToHome
                        )
                    }

                    uiState.status == PurchaseStatus.SUCCESS -> {
                        LumpsumApprovedWaitingContent(
                            goalType = goalType,
                            amountDisplay = "₹${amount.toInt()}",
                            progress = 100 // Simplified progress
                        )
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
                                tint = tealPrimary
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
                                "₹${amount.toInt()}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Complete in your UPI app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Bottom Panel
            if (isFinalStatus) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isBtnEnabled = if (uiState.status == PurchaseStatus.SUCCESS) is30SecondsPassed else true
                    
                    Button(
                        onClick = onGoToHome,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isBtnEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = tealPrimary)
                    ) {
                        Text("Go to Home", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (!upiAppClicked) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(tealSurfaceVariant),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Choose App", "Scan QR").forEachIndexed { index, title ->
                                val isSelected = selectedTabIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) tealPrimary else Color.Transparent)
                                        .clickable { selectedTabIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedTabIndex == 0) {
                            UpiAppGrid(
                                apps = availableUpiApps,
                                onAppClick = { app ->
                                    upiAppClicked = true
                                    platformActions.openUpiUrl(paymentUrl, app.packageName)
                                    scope.launch {
                                        delay(1000L)
                                        viewModel.startPaymentSync(userId, paymentId)
                                    }
                                }
                            )
                        } else {
                            QrPlaceholder(paymentUrl)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LumpsumApprovedWaitingContent(goalType: GoalType, amountDisplay: String, progress: Int) {
    val (accentColor, lightBackground) = accentColorsForLumpsumGoal(goalType)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Payment Successful!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = lightBackground
            )

            Text(
                text = "Allocating units for $amountDisplay to your portfolio...",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun accentColorsForLumpsumGoal(goalType: GoalType): Pair<Color, Color> {
    return when (goalType) {
        GoalType.GOLD -> Color(0xFFC8860A) to Color(0xFFFFFBF5)
        GoalType.SILVER -> Color(0xFF6B7280) to Color(0xFFF7F8FA)
        GoalType.SAVINGS -> V2SuccessGreen to V2SubtleBorder
        GoalType.FESTIVAL_SPENDS -> Color(0xFFFF9800) to Color(0xFFFFF8E1)
        GoalType.GLOBAL_EXPOSURE -> Color(0xFF00897B) to Color(0xFFE0F2F1)
        GoalType.ALL_IN_ONE -> Color(0xFF1A237E) to Color(0xFFE8EAF6)
        GoalType.MARKET_EXPLORER -> Color(0xFF0F6B5C) to Color(0xFFE4F3EE)
        else -> V2SuccessGreen to V2SubtleBorder
    }
}
