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
import androidx.compose.material.icons.outlined.*
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

import com.pyllar.consumer.presentation.ui.theme.*

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
        if (accountNumber.isBlank()) accountNumber = (prefillData["accountNumber"] ?: "").replace("\"", "").replace("'", "").filter { it.isDigit() }
        if (ifscCode.isBlank()) ifscCode = (prefillData["ifscCode"] ?: "").replace("\"", "").replace("'", "").filter { it.isLetterOrDigit() }.uppercase().take(11)
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
                accountNumber = (details.bankAccount ?: accountNumber).replace("\"", "").replace("'", "").filter { it.isDigit() }
                ifscCode = (details.ifsc ?: ifscCode).replace("\"", "").replace("'", "").filter { it.isLetterOrDigit() }.uppercase().take(11)
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
                            Surface(color = V2SuccessGreen, shape = RoundedCornerShape(4.dp)) {
                                Text("FASTEST", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("We will fetch your bank details securely using a ₹1 verification transaction via UPI.", style = MaterialTheme.typography.bodySmall)
                        
                        // Info banner (green theme) - Parity with Android
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = V2SubtleBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Info",
                                    tint = V2SuccessGreen,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = "₹1 will be debited and refunded within 2 working days.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = V2SuccessGreen
                                )
                            }
                        }
                        
                        Button(
                            onClick = { 
                                viewModel.initiateBankVerification(userId)
                                showPaymentSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            //enabled = initiateResult !is Resource.Loading
                            enabled = false
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
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text("  OR  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Manual Entry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Enter Bank Details Manually", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        
                        OutlinedTextField(
                            value = accountNumber.replace("\"", "").replace("'", "").filter { it.isDigit() },
                            onValueChange = { newValue ->
                                accountNumber = newValue.replace("\"", "").replace("'", "").filter { it.isDigit() }
                            },
                            label = { Text("Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPolling,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = ifscCode.replace("\"", "").replace("'", "").filter { it.isLetterOrDigit() }.uppercase().take(11),
                            onValueChange = { newValue ->
                                ifscCode = newValue.replace("\"", "").replace("'", "").filter { it.isLetterOrDigit() }.uppercase().take(11)
                            },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isPolling,
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
                            enabled = !isPolling && accountNumber.length > 8 && ifscCode.length == 11
                        ) {
                            Text("Submit Manual Details")
                        }
                    }
                }

                // Attention card (lighter green theme) - Parity with Android
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text(text = "⚠️", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ATTENTION",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF33691E)
                            )
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.Start) {
                                Text(text = "• ", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF33691E))
                                Text(
                                    text = "Use your own individual savings account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF33691E),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.Start) {
                                Text(text = "• ", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF33691E))
                                Text(
                                    text = "Do not use a joint account or someone else's account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF33691E),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Why is bank verification needed? - Parity with Android
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Why is bank verification needed?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "To ensure seamless investments and withdrawals, we must verify that the bank account belongs to you as per SEBI regulations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onNavigateToHelp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Know More")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    var selectedUpiTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Choose UPI App", "Scan QR Code")

    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showPaymentSheet = false 
                selectedUpiTabIndex = 0
            },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Complete Transaction",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pay ₹1 to verify your bank account.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "It will be refunded automatically within 2 days.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedUpiTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedUpiTabIndex = index },
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

                if (selectedUpiTabIndex == 0) {
                    // App List Tab
                    if (initiateResult is Resource.Success) {
                        val upiUrl = initiateResult?.data?.paymentLinks?.get("upi")
                        if (upiUrl != null) {
                            rpdVerificationId = initiateResult?.data?.verificationId
                            Button(
                                onClick = { uriHandler.openUri(upiUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open UPI App")
                            }
                        } else {
                            Text("No UPI link found.", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (initiateResult is Resource.Loading) {
                        CircularProgressIndicator()
                    } else if (initiateResult is Resource.Error) {
                        Text(initiateResult?.message ?: "Failed to initiate", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // QR Code Tab
                    if (initiateResult is Resource.Success) {
                        val upiUrl = initiateResult?.data?.paymentLinks?.get("upi")
                        if (upiUrl != null) {
                            Card(
                                modifier = Modifier.size(220.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    // Placeholder for QR Code
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            modifier = Modifier.size(100.dp),
                                            tint = Color.Black
                                        )
                                        Text(
                                            "QR Code Placeholder",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                            Text(
                                "Scan this QR with any UPI app to pay ₹1",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
                
                if (rpdVerificationId != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Waiting for payment...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    }
}
