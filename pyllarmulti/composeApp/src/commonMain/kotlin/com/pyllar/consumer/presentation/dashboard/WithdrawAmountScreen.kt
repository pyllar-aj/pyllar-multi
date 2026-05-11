package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    var otpCode by remember { mutableStateOf("") }
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
                mode = withdrawMode
            )
            viewModel.createRedemption(request)
        } else if (otpVerificationResult is Resource.Error) {
            isVerifyingOtp = false
            otpCode = ""
            otpValidationError = otpVerificationResult?.message
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
                    userId = userId,
                    schemeId = selectedSchemeId ?: "",
                    isin = selectedScheme?.isin ?: "",
                    folio = selectedScheme?.folioNo
                )
            )
            
            onSubmit(selectedSchemeId ?: "", effectiveRedemptionAmount)
        } else if (redemptionResult is Resource.Error) {
            isVerifyingOtp = false
            otpValidationError = redemptionResult?.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                                if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                    withdrawalAmount = it
                                    withdrawAll = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("0.00", color = Color.Gray.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            enabled = !withdrawAll,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Available to Withdraw Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available to withdraw", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "₹${formatIndian(withdrawableAmount)}", 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
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
                        modifier = Modifier.fillMaxWidth().clickable { withdrawAll = !withdrawAll }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(checked = withdrawAll, onCheckedChange = { withdrawAll = it })
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
                    enabled = isValidAmount,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
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
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).zIndex(50f)) {
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
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
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).zIndex(60f)) {
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Verify OTP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showOtpScreen = false }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                            }
                            Text("Sent to $phoneNumber", style = MaterialTheme.typography.bodyMedium)
                            
                            OtpField(
                                length = 6,
                                modifier = Modifier.fillMaxWidth(),
                                otpText = otpCode,
                                onOtpChange = { otpCode = it; otpValidationError = null },
                                onOtpComplete = {},
                                isError = otpValidationError != null
                            )

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
