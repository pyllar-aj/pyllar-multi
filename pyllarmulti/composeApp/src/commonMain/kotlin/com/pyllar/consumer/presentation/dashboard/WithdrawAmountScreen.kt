package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.RedemptionRequest
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.otp.OtpField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*
import com.pyllar.consumer.presentation.ui.theme.*
import com.pyllar.consumer.getPlatform
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawAmountScreen(
    userId: String = "",
    selectedSchemeId: String? = null,
    onNavigateBack: () -> Unit = {},
    onSubmit: (String, Double) -> Unit = { _, _ -> },
    viewModel: WithdrawAmountViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    val redemptionResult by viewModel.redemptionResult.collectAsState()
    val isCreatingRedemption by viewModel.isCreatingRedemption.collectAsState()
    val otpVerificationResult by viewModel.otpVerificationResult.collectAsState()
    val otpGenerationResult by viewModel.otpGenerationResult.collectAsState()
    
    var withdrawalAmount by remember { mutableStateOf("") }
    var withdrawAll by remember { mutableStateOf(false) }
    var showConfirmationSheet by remember { mutableStateOf(false) }
    var isConfirming by remember { mutableStateOf(false) }
    var showOtpScreen by remember { mutableStateOf(false) }
    var otpFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val otpCode = otpFieldValue.text
    var phoneNumber by remember { mutableStateOf("") }
    var otpValidationError by remember { mutableStateOf<String?>(null) }
    var isVerifyingOtp by remember { mutableStateOf(false) }
    
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val isIos = remember { getPlatform().name.contains("iOS", ignoreCase = true) }
    
    // Bank Details State
    var bankName by remember { mutableStateOf("") }
    var bankAccountLast4 by remember { mutableStateOf("") }
    
    val selectedScheme = remember { WithdrawSchemeManager.get() }
    val withdrawMode = remember { WithdrawSchemeManager.getMode() }
    
    val withdrawableAmount = remember(selectedScheme, withdrawMode) {
        selectedScheme?.let { scheme ->
            if (withdrawMode == "INSTANT") {
                ((scheme.instantRedemptionValue ?: 0.0) - scheme.redemptionInProgress).coerceAtLeast(0.0)
            } else {
                (scheme.redeemableAmount - scheme.redemptionInProgress).coerceAtLeast(0.0)
            }
        } ?: 0.0
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("WithdrawAmount")
        val phone = sessionStore.getCurrentPhone()
        phoneNumber = if (phone.length > 4) "******${phone.takeLast(4)}" else phone
        
        bankName = sessionStore.getValue("bank_name") ?: "Your Bank"
        val accNo = sessionStore.getValue("account_number") ?: ""
        bankAccountLast4 = if (accNo.length >= 4) accNo.takeLast(4) else accNo
    }

    LaunchedEffect(withdrawAll, withdrawableAmount) {
        if (withdrawAll) {
            withdrawalAmount = (withdrawableAmount).toString()
        } else if (withdrawalAmount == withdrawableAmount.toString()) {
            withdrawalAmount = ""
        }
    }

    val effectiveRedemptionAmount = if (withdrawAll) withdrawableAmount else (withdrawalAmount.toDoubleOrNull() ?: 0.0)
    val isValidAmount = effectiveRedemptionAmount > 0 && effectiveRedemptionAmount <= withdrawableAmount

    // Handle OTP Logic
    LaunchedEffect(otpGenerationResult) {
        if (otpGenerationResult is Resource.Success) {
            showConfirmationSheet = false
            showOtpScreen = true
        }
    }

    LaunchedEffect(otpVerificationResult) {
        if (otpVerificationResult is Resource.Success) {
            otpValidationError = null
            val request = RedemptionRequest(
                userId = userId.ifBlank { sessionStore.getCurrentUserId() },
                isin = selectedScheme?.isin ?: "",
                folioNumber = selectedScheme?.folioNo ?: "",
                amount = effectiveRedemptionAmount,
                mode = if (withdrawMode?.uppercase() == "INSTANT") "instant" else "normal",
                redeemAll = withdrawAll
            )
            viewModel.createRedemption(request)
        } else if (otpVerificationResult is Resource.Error) {
            isVerifyingOtp = false
            otpFieldValue = TextFieldValue("")
            otpValidationError = (otpVerificationResult?.message ?: "").toUserFriendlyErrorMessage()
        }
    }

    LaunchedEffect(redemptionResult) {
        if (redemptionResult is Resource.Success) {
            val response = (redemptionResult as Resource.Success).data
            isVerifyingOtp = false
            showOtpScreen = false
            
            // Populate WithdrawalDataManager for the success screen
            WithdrawalDataManager.setWithdrawalData(
                WithdrawalData(
                    amount = effectiveRedemptionAmount,
                    schemeName = selectedScheme?.schemeName ?: "Investment",
                    bankName = bankName,
                    bankAccountLast4 = bankAccountLast4,
                    bankAccountNumber = "", // Not needed for success screen display usually
                    bankAccountIfscCode = "",
                    transactionId = response?.transactionId ?: "Pending",
                    redemptionId = response?.redemptionId ?: response?.transactionId ?: "Pending",
                    redemptionGroupId = response?.redemptionGroupId,
                    userId = userId,
                    schemeId = selectedSchemeId ?: "",
                    isin = selectedScheme?.isin ?: "",
                    folio = selectedScheme?.folioNo,
                    mode = withdrawMode ?: "NORMAL"
                )
            )
            
            onSubmit(selectedSchemeId ?: "", effectiveRedemptionAmount)
        } else if (redemptionResult is Resource.Error) {
            isVerifyingOtp = false
            otpValidationError = (redemptionResult?.message ?: "").toUserFriendlyErrorMessage()
        }
    }

    Scaffold(
        containerColor = V2Cream,
        topBar = {
            TopAppBar(
                title = { Text("Withdraw", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !showOtpScreen) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Enter withdrawal amount",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Amount Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("₹", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = withdrawalAmount,
                            onValueChange = { 
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    withdrawalAmount = it
                                    withdrawAll = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("0", color = Color.Gray.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !withdrawAll && !showOtpScreen,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Available to Withdraw Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = V2SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Available to withdraw ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "₹${formatIndian(withdrawableAmount)}", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = V2SuccessGreen
                            )
                        }

                        if (withdrawalAmount.isNotBlank() && !withdrawAll) {
                            val enteredAmount = withdrawalAmount.toDoubleOrNull() ?: 0.0
                            if (enteredAmount > withdrawableAmount) {
                                Text(
                                    text = org.jetbrains.compose.resources.stringResource(Res.string.amount_cannot_exceed, "₹${formatIndian(withdrawableAmount)}"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Withdrawal already in progress for this fund
                val pendingWithdrawalAmount = selectedScheme?.redemptionInProgress ?: 0.0
                if (pendingWithdrawalAmount > 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = V2Obsidian),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = org.jetbrains.compose.resources.stringResource(
                                        Res.string.withdrawal_in_progress_card_title,
                                        formatIndianWithDecimals(pendingWithdrawalAmount)
                                    ),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                    color = Color.White
                                )
                                Text(
                                    text = org.jetbrains.compose.resources.stringResource(Res.string.withdrawal_in_progress_card_subtitle),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Withdraw All Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !showOtpScreen) { withdrawAll = !withdrawAll }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = withdrawAll,
                            onCheckedChange = { withdrawAll = it },
                            enabled = !showOtpScreen,
                            colors = CheckboxDefaults.colors(checkedColor = V2Obsidian)
                        )
                        Text("Withdraw all from this fund", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                // Bank Info Placeholder
                Text(
                    if (withdrawMode == "INSTANT") "Money will be credited to $bankName within 30 mins."
                    else "Money will be credited to $bankName in 1-2 business days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { showConfirmationSheet = true },
                    enabled = isValidAmount && !showOtpScreen,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V2Obsidian)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(1.dp))
                        Text("PROCEED", fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }

            // Confirmation Overlay / Bottom Sheet (KMP simple version)
            if (showConfirmationSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            enabled = true,
                            onClick = {},
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .zIndex(50f)
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .imePadding()
                            .clickable(enabled = true, onClick = {}, interactionSource = remember { MutableInteractionSource() }, indication = null),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFAF7)) // Crisp premium background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = org.jetbrains.compose.resources.stringResource(
                                            Res.string.withdrawing_amount,
                                            "₹${formatIndian(effectiveRedemptionAmount)}"
                                        ).replace("Withdrawing", "Withdraw"),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = org.jetbrains.compose.resources.stringResource(Res.string.confirm_your_withdrawal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666)
                                    )
                                }

                                IconButton(
                                    onClick = { showConfirmationSheet = false },
                                    enabled = !isConfirming,
                                    modifier = Modifier.background(Color(0xFFF2EFEA), CircleShape).size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color(0xFF333333),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Details Card
                            val exitLoadDetails = getExitLoadDetails(selectedScheme?.schemeName)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFECE7E2)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Exit Load Row
                                    if (exitLoadDetails != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(Color(0xFFF5EFEB), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info, // Fallback for tag/percent
                                                    contentDescription = null,
                                                    tint = Color(0xFF4A3E3D),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = org.jetbrains.compose.resources.stringResource(Res.string.exit_load_approx),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = Color(0xFF666666)
                                                )
                                                Text(
                                                    text = exitLoadDetails.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF0F3A20) // Deep Premium Green
                                                )
                                                if (!exitLoadDetails.description.isNullOrBlank()) {
                                                    Text(
                                                        text = exitLoadDetails.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF888888)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFECE7E2), thickness = 1.dp)
                                    }

                                    // Estimated Credit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color(0xFFF5EFEB), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = Color(0xFF4A3E3D),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            val isInstant = withdrawMode?.uppercase() == "INSTANT"
                                            Text(
                                                text = if (isInstant) "Estimated credit" else "Estimated credit date",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = Color(0xFF666666)
                                            )
                                            Text(
                                                text = if (isInstant) "Within 30 minutes" else getProcessingDate(),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF0F3A20) // Deep Premium Green
                                            )
                                        }
                                    }
                                }
                            }

                            // Folio Row
                            val folioNumber = selectedScheme?.folioNo
                            if (!folioNumber.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFF5EFEB), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4A3E3D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = org.jetbrains.compose.resources.stringResource(Res.string.folios),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF333333)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = folioNumber,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF1A1A1A)
                                    )
                                }
                            }

                            // Disclaimer Banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F5F1)), // Light premium greenish-gray banner
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = org.jetbrains.compose.resources.stringResource(Res.string.once_processed_cannot_be_reversed),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            // Confirm Button
                            Button(
                                onClick = {
                                    isConfirming = true
                                    scope.launch {
                                        val finalUserId = if (userId.isBlank()) sessionStore.getCurrentUserId() else userId
                                        viewModel.generateRedemptionOtp(finalUserId)
                                    }
                                },
                                enabled = !isConfirming,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = V2Obsidian,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFB0BEC5),
                                    disabledContentColor = Color.White
                                )
                            ) {
                                if (isConfirming) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(modifier = Modifier.width(1.dp))
                                        Text(
                                            text = "CONFIRM & SEND OTP",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // OTP Overlay
            if (showOtpScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            enabled = true,
                            onClick = {},
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .zIndex(60f)
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .run { if (isIos) this else imePadding() },
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Verify OTP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showOtpScreen = false }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                            }
                            Text("Sent to $phoneNumber", style = MaterialTheme.typography.bodyMedium)
                            
                            var resendTimer by remember { mutableStateOf(30) }
                            var canResend by remember { mutableStateOf(false) }
                            var isResent by remember { mutableStateOf(false) }
                            
                            // Initialize isResent to false when screen first appears
                            LaunchedEffect(Unit) {
                                isResent = false
                            }

                            LaunchedEffect(canResend) {
                                if (!canResend) {
                                    resendTimer = 30
                                    while (resendTimer > 0) {
                                        delay(1000)
                                        resendTimer--
                                    }
                                    canResend = true
                                }
                            }

                            OtpField(
                                 length = 6,
                                 modifier = Modifier.fillMaxWidth(),
                                 otpFieldValue = otpFieldValue,
                                 onOtpFieldValueChange = { otpFieldValue = it; otpValidationError = null },
                                 onOtpComplete = {},
                                 isError = otpValidationError != null
                             )

                            Text(
                                text = org.jetbrains.compose.resources.stringResource(Res.string.otp_consent_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )

                            TextButton(
                                onClick = {
                                     if (canResend) {
                                         canResend = false
                                         otpFieldValue = TextFieldValue("")
                                        isResent = true
                                        scope.launch {
                                            val finalUserId = if (userId.isBlank()) sessionStore.getCurrentUserId() else userId
                                            viewModel.generateRedemptionOtp(finalUserId)
                                        }
                                    }
                                },
                                enabled = canResend
                            ) {
                                Text(if (canResend) "Resend OTP" else "Resend in $resendTimer seconds")
                            }

                            if (otpGenerationResult is Resource.Success && !canResend && isResent) {
                                Text("OTP resent successfully!", color = V2HelpText, style = MaterialTheme.typography.bodySmall)
                            }

                            if (otpValidationError != null) {
                                Text(otpValidationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    isVerifyingOtp = true
                                    scope.launch {
                                        val fullPhone = sessionStore.getCurrentPhone()
                                        viewModel.verifyRedemptionOtp(userId.ifBlank { sessionStore.getCurrentUserId() }, fullPhone, otpCode, null)
                                    }
                                },
                                enabled = otpCode.length == 6 && !isVerifyingOtp,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                if (isVerifyingOtp) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                else Text("VERIFY & WITHDRAW")
                            }
                        }
                    }
                }
            }

            // Loading Overlay
            if (isCreatingRedemption) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).zIndex(100f)) {
                    LoadingScreen(text = "Processing Withdrawal...", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

fun formatIndianWithDecimals(value: Double): String {
    val parts = value.toString().split(".")
    val integerPart = parts[0].toDoubleOrNull() ?: 0.0
    val decimalPart = parts.getOrNull(1)?.take(2)?.padEnd(2, '0') ?: "00"
    return "${formatIndian(integerPart)}.$decimalPart"
}

// Helper function to get exit load text based on scheme name
private fun getExitLoadText(schemeName: String?): String {
    if (schemeName.isNullOrBlank()) {
        return "0"
    }

    // Normalize scheme name: remove special characters, convert to uppercase, and trim
    val normalizedName = schemeName
        .replace(Regex("[–—]"), "-") // Replace em/en dashes with regular dash
        .replace(Regex("[^A-Za-z0-9\\s-]"), "") // Remove special characters except spaces and dashes
        .trim()
        .uppercase()

    return when {
        // Nippon India Gold Savings Fund
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("GOLD SAVINGS") ->
            "1% within 15 days; Nil thereafter"

        // Aditya Birla Sun Life Gold Fund
        normalizedName.contains("ADITYA BIRLA") &&
                normalizedName.contains("GOLD FUND") &&
                !normalizedName.contains("SILVER") ->
            "1% within 15 days; Nil thereafter"

        // Axis Gold Fund
        normalizedName.contains("AXIS") &&
                normalizedName.contains("GOLD FUND") ->
            "1% within 15 days; Nil thereafter"


        // Invesco India Smallcap
        normalizedName.contains("INVESCO INDIA") &&
                (normalizedName.contains("SMALLCAP") || normalizedName.contains("SMALL CAP")) ->
            "1% if redeemed within 1 year"

        // Nippon India Multi Asset
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("MULTI ASSET") ->
            "1% if redeemed within 1 year"

        // Aditya Birla Sun Life Multi Asset Allocation
        normalizedName.contains("ADITYA BIRLA") &&
                normalizedName.contains("MULTI ASSET ALLOCATION") ->
            "1% if redeemed within 1 year"

        // Aditya Birla Sun Life Liquid Fund
        normalizedName.contains("ADITYA BIRLA") &&
                normalizedName.contains("LIQUID FUND") ->
            "Graded (0.007% to 0%) for 7 days."

        // Nippon India Liquid Fund
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("LIQUID FUND") ->
            "Graded load (0.0070%–0.0045%) for days 1–6; Nil after 7 days"

        // Nippon India Flexi Cap
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("FLEXI CAP") ->
            "1% for units > 10% within 1 year"

        // Invesco India Flexi Cap
        normalizedName.contains("INVESCO INDIA") &&
                normalizedName.contains("FLEXI CAP") ->
            "1% if redeemed within 1 year"

        // Nippon India Low Duration
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("LOW DURATION") ->
            "NIL"

        // Aditya Birla Sun Life Large Cap Fund
        normalizedName.contains("ADITYA BIRLA") &&
                normalizedName.contains("LARGE CAP FUND") ->
            "1% if redeemed within 90 days"

        // Nippon India Large Cap
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("LARGE CAP") ->
            "1% if redeemed within 7 days"

        // Invesco India Midcap
        normalizedName.contains("INVESCO INDIA") &&
                normalizedName.contains("MIDCAP") ->
            "1% if redeemed within 1 year"

        // Axis Silver
        normalizedName.contains("AXIS") &&
                normalizedName.contains("SILVER") ->
            "0.25% within 7 days; Nil thereafter"

        // Aditya Birla Sun Life Silver ETF FOF – Regular Growth
        normalizedName.contains("ADITYA BIRLA") &&
                normalizedName.contains("SILVER") ->
            "0.5% if redeemed within 30 days"

        // Nippon India Silver ETF Fund of Fund – Regular Growth
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("SILVER ETF") &&
                normalizedName.contains("FUND OF FUND") ->
            "1% if redeemed within 15 days"

        // Nippon India Growth Fund
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("GROWTH FUND") ->
            "1% if redeemed within 30 days"

        // Nippon India Ultra Short
        normalizedName.contains("NIPPON INDIA") &&
                normalizedName.contains("ULTRA SHORT") ->
            "NIL"

        else -> "0"
    }
}

// Helper function to get processing date
private fun getProcessingDate(): String {
    return try {
        val now = Clock.System.now()
        val futureInstant = now.plus(2, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        val localDateTime = futureInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val monthName = when (localDateTime.monthNumber) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
            7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
            else -> "Jan"
        }
        val dayOfWeek = when (localDateTime.dayOfWeek.name) {
            "MONDAY" -> "Mon"
            "TUESDAY" -> "Tue"
            "WEDNESDAY" -> "Wed"
            "THURSDAY" -> "Thu"
            "FRIDAY" -> "Fri"
            "SATURDAY" -> "Sat"
            "SUNDAY" -> "Sun"
            else -> ""
        }
        val paddedDay = localDateTime.dayOfMonth.toString().padStart(2, '0')
        "$paddedDay $monthName ${localDateTime.year}, $dayOfWeek"
    } catch (e: Exception) {
        ""
    }
}

private data class ExitLoadDetails(
    val title: String,
    val description: String? = null
)

private fun getExitLoadDetails(schemeName: String?): ExitLoadDetails? {
    if (schemeName.isNullOrBlank()) return null
    val text = getExitLoadText(schemeName)
    if (text == "0" || text == "NIL" || text.uppercase() == "NIL") return null
    
    // Custom beautiful mapping for exit load display like the image
    return when {
        text.contains("Graded") && text.contains("Aditya Birla") -> ExitLoadDetails("0.007% to 0%", "Graded for the first 7 days")
        text.contains("Graded") && text.contains("Nippon") -> ExitLoadDetails("0.0070% to 0.0045%", "Graded load for days 1–6; Nil after 7 days")
        text.contains("1%") && text.contains("15 days") -> ExitLoadDetails("1% within 15 days", "Nil thereafter")
        text.contains("1%") && text.contains("30 days") -> ExitLoadDetails("1% within 30 days", "Nil thereafter")
        text.contains("1%") && text.contains("90 days") -> ExitLoadDetails("1% within 90 days", "Nil thereafter")
        text.contains("1%") && text.contains("1 year") -> ExitLoadDetails("1% if redeemed", "Within 1 year")
        text.contains("0.25%") -> ExitLoadDetails("0.25% within 7 days", "Nil thereafter")
        text.contains("0.5%") -> ExitLoadDetails("0.5% within 30 days", "Nil thereafter")
        else -> {
            if (text.contains(";")) {
                val parts = text.split(";")
                ExitLoadDetails(parts[0].trim(), parts.getOrNull(1)?.trim())
            } else {
                ExitLoadDetails(text)
            }
        }
    }
}

