package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.*
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailsScreen(
    userId: String,
    kycAttemptId: String,
    onNext: (String?, String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    viewModel: BankDetailsViewModel = koinInject()
) {
    val scope = rememberCoroutineScope()
    val prefillData by viewModel.prefillData.collectAsState()
    val submitResult by viewModel.submitResult.collectAsState()
    val initiateResult by viewModel.initiateResult.collectAsState()
    val statusResult by viewModel.statusResult.collectAsState()

    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var verificationMode by remember { mutableStateOf("UPI") } // "UPI" or "MANUAL"
    var showPaymentSheet by remember { mutableStateOf(false) }
    var rpdVerificationId by remember { mutableStateOf<String?>(null) }
    var isPolling by remember { mutableStateOf(false) }
    var pollMessage by remember { mutableStateOf<String?>(null) }
    var currentDelayMs by remember { mutableStateOf(5000L) }
    
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("BankDetails")
    }

    LaunchedEffect(prefillData) {
        if (accountNumber.isBlank()) accountNumber = prefillData["accountNumber"] ?: ""
        if (ifscCode.isBlank()) ifscCode = prefillData["ifscCode"] ?: ""
    }

    LaunchedEffect(submitResult) {
        when (val result = submitResult) {
            is Resource.Success -> {
                val nav = result.navigation
                if (nav?.action == com.pyllar.consumer.data.remote.model.dto.NavigationAction.POLL) {
                    isLoading = false
                    isPolling = true
                    pollMessage = nav.getMessage() ?: "Verifying bank details..."
                    currentDelayMs = nav.getParam("delayMs")?.toLongOrNull() ?: 5000L
                } else {
                    isLoading = false
                    isPolling = false
                    onNext(nav?.nextScreen ?: ScreenNames.NOMINEE_DETAILS, result.data?.investorId)
                }
            }
            is Resource.Error -> {
                isLoading = false
                isPolling = false
                // Do NOT navigate to "error" which defaults to Dashboard. 
                // Stay on screen to show the error message.
            }
            else -> {}
        }
    }

    // UPI Polling
    LaunchedEffect(rpdVerificationId) {
        if (rpdVerificationId == null) return@LaunchedEffect
        while (rpdVerificationId != null) {
            delay(5000)
            viewModel.pollVerificationStatus(userId, rpdVerificationId!!)
        }
    }

    // Manual Submission Polling
    LaunchedEffect(isPolling) {
        if (!isPolling) return@LaunchedEffect
        while (isPolling) {
            delay(currentDelayMs)
            if (!isPolling) break
            platformLog("BankDetailsScreen: 🔄 Polling manual submission status")
            viewModel.submitBankDetails(userId, accountNumber, ifscCode)
        }
    }

    LaunchedEffect(statusResult) {
        if (statusResult is Resource.Success) {
            val details = statusResult?.data?.bankDetails
            if (details != null) {
                accountNumber = details.bankAccount ?: accountNumber
                ifscCode = details.ifsc ?: ifscCode
                rpdVerificationId = null
                showPaymentSheet = false
                verificationMode = "MANUAL"
                PlatformAnalyticsLogger.logEvent("bank_verified_via_upi")
            }
        } else if (statusResult is Resource.Error) {
            rpdVerificationId = null
            showPaymentSheet = false
        }
    }

    if (isLoading) {
        LoadingScreen(text = "Validating your bank account...")
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onNavigateToHelp) {
                    Text("Help", color = MaterialTheme.colorScheme.primary)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
            ) {
                OnboardingStepper(currentStep = 1, completedStep = 1, currentScreenRoute = ScreenNames.BANK_DETAILS)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .imePadding()
                    .clickable { 
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Bank Account Details", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Choose how you want to verify your bank account.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // UPI Verification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Verify via UPI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(4.dp)) {
                                Text("FASTEST", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("We will fetch your bank details securely using a ₹1 verification transaction via UPI.", style = MaterialTheme.typography.bodySmall)
                        
                        Button(
                            onClick = { 
                                viewModel.initiateBankVerification(userId)
                                showPaymentSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = initiateResult !is Resource.Loading
                        ) {
                            if (initiateResult is Resource.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("Verify with UPI")
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(modifier = Modifier.weight(1f))
                    Text("  OR  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Divider(modifier = Modifier.weight(1f))
                }

                // Manual Entry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Enter Bank Details Manually", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it.filter { it.isDigit() } },
                            label = { Text("Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = confirmAccountNumber,
                            onValueChange = { confirmAccountNumber = it.filter { it.isDigit() } },
                            label = { Text("Confirm Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = confirmAccountNumber.isNotEmpty() && confirmAccountNumber != accountNumber,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase().take(11) },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done)
                        )

                        if (submitResult is Resource.Error) {
                            Text(submitResult?.message ?: "Error submitting details", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        if (isPolling && pollMessage != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pollMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Button(
                            onClick = { 
                                isLoading = true
                                viewModel.submitBankDetails(userId, accountNumber, ifscCode)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPolling && accountNumber.length > 8 && accountNumber == confirmAccountNumber && ifscCode.length == 11
                        ) {
                            Text("Submit Manual Details")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showPaymentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPaymentSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select Payment App", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Pay ₹1 to verify your bank account. It will be refunded automatically.", textAlign = TextAlign.Center)
                    
                    if (initiateResult is Resource.Success) {
                        val upiUrl = initiateResult?.data?.paymentLinks?.get("upi")
                        if (upiUrl != null) {
                            rpdVerificationId = initiateResult?.data?.verificationId
                            Button(onClick = { uriHandler.openUri(upiUrl) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Open UPI App")
                            }
                        } else {
                            Text("No UPI payment link available.", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (initiateResult is Resource.Loading) {
                        CircularProgressIndicator()
                    } else if (initiateResult is Resource.Error) {
                        Text(initiateResult?.message ?: "Failed to initiate payment", color = MaterialTheme.colorScheme.error)
                    }
                    
                    if (rpdVerificationId != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Waiting for payment confirmation...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
