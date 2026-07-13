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
import com.pyllar.consumer.util.toUserFriendlyErrorMessage

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

                // Withdrawal already in progress for this fund
                val pendingWithdrawalAmount = selectedScheme?.redemptionInProgress ?: 0.0
                if (pendingWithdrawalAmount > 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, V2SubtleBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                    .background(V2GoldDeep.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = V2GoldDeep,
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
                                    color = V2Ink
                                )
                                Text(
                                    text = org.jetbrains.compose.resources.stringResource(Res.string.withdrawal_in_progress_card_subtitle),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                                    color = V2InkSoft
                                )
                            }
                        }
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
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(16.dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Confirm Withdrawal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Amount: ₹${formatIndian(effectiveRedemptionAmount)}")
                            Text("Fund: ${selectedScheme?.schemeName ?: "Unknown"}")
                         //   Text("Bank: $bankName (**$bankAccountLast4)")
                            
                            Button(
                                onClick = {
                                    isConfirming = true
                                    scope.launch {
                                        val finalUserId = if (userId.isBlank()) sessionStore.getCurrentUserId() else userId
                                        viewModel.generateRedemptionOtp(finalUserId)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !isConfirming
                            ) {
                                if (isConfirming) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                else Text("CONFIRM & SEND OTP")
                            }
                            TextButton(onClick = { showConfirmationSheet = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("Cancel")
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
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(16.dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
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

