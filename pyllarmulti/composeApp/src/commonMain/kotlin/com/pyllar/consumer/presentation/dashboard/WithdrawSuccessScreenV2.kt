package com.pyllar.consumer.presentation.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.BackHandler
import com.pyllar.consumer.util.formatCurrentDateV2
import com.pyllar.consumer.util.formatProcessingDateV2
import org.koin.compose.koinInject
import kotlinx.coroutines.delay
import kotlinx.datetime.*

@Composable
fun WithdrawSuccessScreenV2(
    withdrawalAmount: Double,
    schemeName: String,
    bankName: String,
    bankAccountLast4: String,
    redemptionId: String,
    userId: String,
    folio: String?,
    redemptionMode: String = "NORMAL",
    redemptionGroupId: String? = null,
    onNavigateToHome: () -> Unit,
    viewModel: WithdrawSuccessViewModelV2 = koinInject(),
    platformActions: PlatformActions = koinInject(),
    sessionStore: SessionStore = koinInject(),
    previewUiState: WithdrawSuccessV2UiState? = null
) {
    val uiState by if (previewUiState != null) {
        remember { mutableStateOf(previewUiState) }
    } else {
        viewModel.uiState.collectAsState()
    }

    BackHandler(enabled = true) { /* Prevent back — user must go home via button */ }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("WithdrawSuccessV2")
        PlatformAnalyticsLogger.logEvent(
            "withdraw_success_v2_view",
            mapOf("amount" to withdrawalAmount, "mode" to redemptionMode)
        )
    }

    LaunchedEffect(redemptionId, userId) {
        if (previewUiState == null) {
            viewModel.startPolling(userId, redemptionId, redemptionGroupId)
        }
    }

    LaunchedEffect(uiState.status) {
        if ((uiState.status == RedemptionPollStatus.SUCCEEDED) || (uiState.status == RedemptionPollStatus.SUBMITTED)){
            platformActions.playRedemptionSuccessSound()

            val lastPromptTimeStr = sessionStore.getValue("last_review_prompt_time")
            val lastPromptTime = lastPromptTimeStr?.toLongOrNull() ?: 0L
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000

            if (currentTime - lastPromptTime > thirtyDaysInMillis) {
                platformActions.requestInAppReview(
                    screenName = "WithdrawSuccessV2",
                    silentFallback = true,
                    trigger = "auto"
                )
                sessionStore.saveValue("last_review_prompt_time", currentTime.toString())
            }
        }
    }

    val currentDate = remember { formatCurrentDateV2() }
    val processingDate = remember {
        val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val daysToAdd = when (today.dayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY, kotlinx.datetime.DayOfWeek.TUESDAY, kotlinx.datetime.DayOfWeek.WEDNESDAY -> 2
            kotlinx.datetime.DayOfWeek.THURSDAY, kotlinx.datetime.DayOfWeek.FRIDAY -> 4
            kotlinx.datetime.DayOfWeek.SATURDAY -> 3
            kotlinx.datetime.DayOfWeek.SUNDAY -> 3
            else -> 3
        }
        formatProcessingDateV2(daysToAdd)
    }

    var isOneMinuteElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60000L)
        isOneMinuteElapsed = true
    }

    val scrollState = rememberScrollState()
    val isTerminal = uiState.status == RedemptionPollStatus.SUBMITTED ||
            uiState.status == RedemptionPollStatus.SUCCEEDED ||
            uiState.status == RedemptionPollStatus.FAILED ||
            uiState.status == RedemptionPollStatus.CANCELLED ||
            uiState.hasTimedOut

    Scaffold(
        bottomBar = {
            Surface(color = Color(0xFFFBF9F4), shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        PlatformAnalyticsLogger.logEvent(
                            "withdraw_success_v2_go_home_clicked",
                            mapOf("status" to uiState.status.name)
                        )
                        onNavigateToHome()
                    },
                    enabled = isTerminal || isOneMinuteElapsed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A2415),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF0A2415).copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTerminal || isOneMinuteElapsed) stringResource(Res.string.go_to_home) else stringResource(Res.string.processing),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBF9F4))
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Real-time status progress card
            RedemptionProgressCard(uiState, processingDate, redemptionMode, withdrawalAmount)

            // Processing date: NORMAL only, hidden when SUCCEEDED/FAILED/CANCELLED
            // Credit time: hidden when FAILED/CANCELLED; for SUCCEEDED, NORMAL still shows it, INSTANT hides it
            val isFailedOrCancelled = uiState.status == RedemptionPollStatus.FAILED || uiState.status == RedemptionPollStatus.CANCELLED
            val showProcessingDate = redemptionMode != "INSTANT" && uiState.status != RedemptionPollStatus.SUBMITTED && !isFailedOrCancelled
            val showCreditTime = (redemptionMode != "INSTANT" || uiState.status != RedemptionPollStatus.SUBMITTED) && !isFailedOrCancelled
            if (showProcessingDate || showCreditTime) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showProcessingDate) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE8F5E9).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF2E7D32), CircleShape))
                                Text(
                                    text = stringResource(Res.string.will_be_processed_after, processingDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    if (showCreditTime) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF5F5F5).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFFE0E0E0)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (redemptionMode == "INSTANT") {
                                        stringResource(Res.string.will_be_credited_within_30_mins)
                                    } else {
                                        stringResource(Res.string.will_be_credited_within_days, formatIndian(withdrawalAmount))
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }


            // Transaction details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFF0F0F0))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    TransactionDetailRowV2(
                        label = "Withdrawing Amount",
                        value = "₹${formatIndian(withdrawalAmount)}"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color(0xFFEEEEEE))
                    if (folio != null) {
                        TransactionDetailRowV2(label = "Folio Number", value = folio)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = Color(0xFFEEEEEE))
                    }
                    TransactionDetailRowV2(label = "Withdrawal Initiated Date", value = currentDate)
                }
            }
        }
    }
}

@Composable
private fun RedemptionProgressCard(
    uiState: WithdrawSuccessV2UiState,
    processingDate: String,
    redemptionMode: String,
    withdrawalAmount: Double
) {
    val status = uiState.status
    val accentColor = when (status) {
        RedemptionPollStatus.FAILED, RedemptionPollStatus.CANCELLED -> Color(0xFFD32F2F)
        RedemptionPollStatus.TIMED_OUT -> Color(0xFFFF9800)
        else -> Color(0xFF2E7D32)
    }

    val stepsDone = when (status) {
        RedemptionPollStatus.PENDING    -> listOf(true, false, false)
        RedemptionPollStatus.CONFIRMED  -> listOf(true, true,  false)
        RedemptionPollStatus.SUBMITTED  -> listOf(true, true,  true)
        RedemptionPollStatus.SUCCEEDED  -> listOf(true, true,  true)
        else                            -> listOf(true, false, false)
    }

    val progressPercent = when (status) {
        RedemptionPollStatus.CONFIRMED -> 50
        RedemptionPollStatus.SUBMITTED -> 100
        RedemptionPollStatus.SUCCEEDED -> 100
        else -> 20
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Animated icon
            val ringRotation by rememberInfiniteTransition(label = "redemptionRingTransition").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "redemptionRingRotation"
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = Color(0xFFF3FBF4), modifier = Modifier.size(86.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    if (status == RedemptionPollStatus.SUCCEEDED || status == RedemptionPollStatus.SUBMITTED) {
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
                    } else if (status == RedemptionPollStatus.FAILED || status == RedemptionPollStatus.CANCELLED || status == RedemptionPollStatus.TIMED_OUT) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(30.dp).align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(
                            progress = { 0.25f },
                            modifier = Modifier.size(30.dp).align(Alignment.BottomEnd).rotate(ringRotation),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.25f),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            // Title
            Text(
                text = when (status) {
                    RedemptionPollStatus.SUCCEEDED  -> stringResource(Res.string.redemption_complete)
                    RedemptionPollStatus.SUBMITTED  -> stringResource(Res.string.redemption_complete)
                    RedemptionPollStatus.FAILED     -> stringResource(Res.string.redemption_failed)
                    RedemptionPollStatus.CANCELLED  -> stringResource(Res.string.redemption_cancelled)
                    RedemptionPollStatus.TIMED_OUT  -> stringResource(Res.string.status_check_timed_out)
                    else                            -> stringResource(Res.string.processing_your_withdrawal)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = when (status) {
                    RedemptionPollStatus.SUCCEEDED  -> stringResource(Res.string.redemption_complete_msg)
                    RedemptionPollStatus.SUBMITTED  -> stringResource(Res.string.redemption_complete_msg)
                    RedemptionPollStatus.FAILED     -> stringResource(Res.string.redemption_failed_msg)
                    RedemptionPollStatus.CANCELLED  -> stringResource(Res.string.redemption_cancelled_msg)
                    RedemptionPollStatus.TIMED_OUT  -> stringResource(Res.string.status_check_timed_out_msg)
                    else                            -> stringResource(Res.string.tracking_withdrawal_real_time)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Only show steps for non-failure states
            if (status != RedemptionPollStatus.FAILED && status != RedemptionPollStatus.CANCELLED) {
                RedemptionStepRow(
                    title = stringResource(Res.string.withdrawal_initiated_step_title, "₹${formatIndian(withdrawalAmount)}"),
                    subtitle = stringResource(Res.string.withdrawal_request_received_desc),
                    isDone = stepsDone[0],
                    showConnector = true,
                    accentColor = accentColor
                )
                RedemptionStepRow(
                    title = stringResource(Res.string.request_received_step_title),
                    subtitle = stringResource(Res.string.processing_now_desc),
                    isDone = stepsDone[1],
                    showConnector = true,
                    accentColor = accentColor
                )
                RedemptionStepRow(
                    title = if (stepsDone[2]) {
                        if (redemptionMode == "INSTANT") {
                            stringResource(Res.string.credit_within_30_mins_step_title)
                        } else {
                            stringResource(Res.string.credit_by_date_step_title, processingDate)
                        }
                    } else {
                        stringResource(Res.string.on_its_way_to_bank_step_title)
                    },
                    subtitle = if (stepsDone[2]) {
                        stringResource(Res.string.funds_on_way_to_bank_desc)
                    } else if (redemptionMode == "INSTANT") {
                        stringResource(Res.string.will_be_credited_within_30_mins_desc)
                    } else {
                        stringResource(Res.string.will_be_credited_within_2_working_days_desc)
                    },
                    isDone = stepsDone[2],
                    showConnector = false,
                    accentColor = accentColor
                )

                // Progress bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3FBF4)),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(Res.string.withdrawal_progress_title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$progressPercent%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .progressSemantics(),
                            color = accentColor,
                            trackColor = accentColor.copy(alpha = 0.24f)
                        )
                        Text(
                            text = when (status) {
                                RedemptionPollStatus.SUCCEEDED -> stringResource(Res.string.redemption_complete_toast)
                                RedemptionPollStatus.SUBMITTED -> stringResource(Res.string.redemption_complete_toast)
                                RedemptionPollStatus.TIMED_OUT -> stringResource(Res.string.check_dashboard_for_updates)
                                else -> stringResource(Res.string.tracking_withdrawal_real_time)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedemptionStepRow(
    title: String,
    subtitle: String,
    isDone: Boolean,
    showConnector: Boolean,
    accentColor: Color
) {
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
                    .background(if (isDone) Color(0xFFEFEBE9) else accentColor.copy(alpha = 0.14f))
                    .border(0.8.dp, if (isDone) Color(0xFF2E7D32) else accentColor.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                } else {
                    Icon(imageVector = Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
            }
            if (showConnector) {
                Box(modifier = Modifier.width(1.dp).height(26.dp).background(accentColor.copy(alpha = 0.25f)))
            }
        }
        Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun TransactionDetailRowV2(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6D4C41))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Color(0xFF3E2723))
    }
}
