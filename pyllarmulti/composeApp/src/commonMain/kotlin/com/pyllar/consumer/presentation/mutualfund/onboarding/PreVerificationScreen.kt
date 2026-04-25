package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.pyllar.otp.OtpField
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.domain.storage.SessionStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreVerificationScreen(
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToScreen: (String) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToKycInfo: () -> Unit = {},
    viewModel: PreVerificationViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var panNumber by remember { mutableStateOf("") }
    var panError by remember { mutableStateOf<String?>(null) }
    var autoFetchFailed by remember { mutableStateOf(false) }
    var fetchedPanName by remember { mutableStateOf<String?>(null) }

    // Auto-fill PAN from prepopulated data
    LaunchedEffect(uiState.prepopulatedData) {
        val prepopulatedPan = uiState.prepopulatedData["panNumber"]
        if (!prepopulatedPan.isNullOrBlank() && panNumber.isBlank()) {
            panNumber = prepopulatedPan
        }
    }

    val timeoutState = rememberTimeoutState("PreVerification", "checkReadiness")

    val isPanLengthValid = panNumber.length == 10
    val isFourthLetterValid = panNumber.length >= 4 && panNumber[3] == 'P'
    val isPanValid = isPanLengthValid && isFourthLetterValid
    val fourthLetterError = panNumber.length >= 4 && !isFourthLetterValid

    var readinessCheckStarted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showManualEntryForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("PreVerification")
    }

    // Handle readiness check result
    LaunchedEffect(uiState.verificationResult) {
        when (val result = uiState.verificationResult) {
            is Resource.Success -> {
                isSubmitting = false
                readinessCheckStarted = true
                PlatformAnalyticsLogger.logEvent("readiness_check_success", mapOf("pan_last4" to panNumber.takeLast(4)))
            }
            is Resource.Error -> {
                isSubmitting = false
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent("readiness_check_error", mapOf("pan_last4" to panNumber.takeLast(4), "error" to (result.message ?: "unknown")))
            }
            else -> {}
        }
    }

    // Handle server-driven navigation
    LaunchedEffect(uiState.nextScreen) {
        uiState.nextScreen?.let { screenName ->
            PlatformAnalyticsLogger.logEvent("server_navigation", mapOf("from_screen" to "pre_verification", "to_screen" to screenName))
            onNavigateToScreen(screenName)
        }
    }

    // OTP bottom sheet state
    var showOtpBottomSheet by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpSheetPhoneDisplay by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var userPhone by remember { mutableStateOf("") }
    var currentPrefillId by remember { mutableStateOf<String?>(null) } // ViewModel stores ID

    LaunchedEffect(Unit) {
        userPhone = sessionStore.getCurrentPhone()
    }

    // Handle PAN Fetch Result
    LaunchedEffect(uiState.panFetchResult) {
        val result = uiState.panFetchResult
        if (result is Resource.Success) {
            val data = result.data?.data
            if (data != null) {
                if (data.status == "OTP_GENERATED") {
                    currentPrefillId = data.prefillId.toString()
                    showOtpBottomSheet = true
                } else if (data.status == "ALREADY_VERIFIED") {
                    if (!data.panNumber.isNullOrBlank()) {
                        panNumber = data.panNumber ?: ""
                        fetchedPanName = data.fullName
                        showManualEntryForm = true
                        autoFetchFailed = false
                        viewModel.clearError()
                    }
                }
            }
            viewModel.clearPanFetchResult()
        } else if (result is Resource.Error) {
            autoFetchFailed = true
            showManualEntryForm = true
            viewModel.clearPanFetchResult()
        }
    }

    // Handle OTP Verification Result
    LaunchedEffect(uiState.panVerifyOtpResult) {
        val result = uiState.panVerifyOtpResult
        if (result is Resource.Success) {
            val data = result.data?.data
            if (data != null && data.status == "SUCCESS") {
                showOtpBottomSheet = false
                val panDetails = data.panDetails
                val personalDetails = data.personalDetails

                if (panDetails?.panNumber != null) {
                    panNumber = panDetails.panNumber
                    fetchedPanName = personalDetails?.fullName ?: ""
                    showManualEntryForm = true
                    autoFetchFailed = false
                    viewModel.clearError()
                } else {
                    autoFetchFailed = true
                    showManualEntryForm = true
                }
            }
        }
    }

    LaunchedEffect(showOtpBottomSheet) {
        if (showOtpBottomSheet) {
            otpSheetPhoneDisplay = if (userPhone.length >= 4) "******${userPhone.takeLast(4)}" else "your phone"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                TextButton(onClick = {
                    PlatformAnalyticsLogger.logEvent("pre_verification_help_clicked")
                    onNavigateToHelp()
                }) {
                    Text("Help", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
            ) {
                OnboardingStepper(
                    currentStep = if (readinessCheckStarted) 1 else 0,
                    completedStep = if (readinessCheckStarted) 1 else 0,
                    currentScreenRoute = ScreenNames.PRE_VERIFICATION
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PAN Verification",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Verify your PAN to start investing securely.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            Button(
                                onClick = {
                                    PlatformAnalyticsLogger.logEvent("pre_verification_find_my_pan_clicked")
                                    if (userPhone.isNotBlank()) {
                                        viewModel.initiatePanFetch(userPhone)
                                    } else {
                                        showOtpBottomSheet = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                if (uiState.panFetchResult is Resource.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetching...", color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Autofetch my PAN", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Don't remember your PAN? Click here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "──────── OR ────────",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )

                            if (!showManualEntryForm) {
                                Button(
                                    onClick = { showManualEntryForm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 16.dp)
                                ) {
                                    Text("Enter PAN manually", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (autoFetchFailed) {
                                        Text(
                                            text = "Auto-fetch failed. Please enter manually.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    Text(
                                        text = "PAN Number",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = panNumber,
                                        onValueChange = { newValue ->
                                            val filtered = newValue.uppercase().filter { it.isLetterOrDigit() }
                                            if (filtered.length <= 10) {
                                                panNumber = filtered
                                                panError = null
                                                viewModel.clearError()
                                            }
                                        },
                                        placeholder = { Text("ABCDE1234F") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        isError = uiState.verificationResult is Resource.Error || fourthLetterError,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    if (fourthLetterError) {
                                        Text("Invalid fourth character. Must be 'P' for individual.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }

                                    fetchedPanName?.let { name ->
                                        Text("Name: $name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    TimeoutButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            if (panNumber.isBlank()) {
                                                panError = "PAN is required"
                                                return@TimeoutButton
                                            }
                                            isSubmitting = true
                                            viewModel.checkInvestorReadiness(panNumber)
                                        },
                                        enabled = isPanValid && !isSubmitting && uiState.verificationResult !is Resource.Loading,
                                        timeoutState = timeoutState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Verify PAN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Why is PAN needed?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("PAN is required for KYC compliance as per regulatory norms for investing in mutual funds.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onNavigateToKycInfo() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Know more")
                    }
                }
            }
        }

        if (showOtpBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOtpBottomSheet = false },
                sheetState = sheetState
            ) {
                PreVerificationOtpBottomSheet(
                    phoneNumber = otpSheetPhoneDisplay,
                    otpCode = otpCode,
                    otpVerificationResult = uiState.panVerifyOtpResult,
                    onOtpCodeChange = { otpCode = it },
                    onVerifyOtp = { otpValue ->
                        val prefillIdStr = currentPrefillId
                        if (prefillIdStr != null && userPhone.isNotBlank()) {
                            viewModel.verifyOtpAndFetchPan(userPhone, prefillIdStr.toLong(), otpValue)
                        }
                    },
                    onResendOtp = { viewModel.initiatePanFetch(userPhone) }
                )
            }
        }

        if (isSubmitting || uiState.verificationResult is Resource.Loading || uiState.panFetchResult is Resource.Loading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).zIndex(10f)) {
                LoadingScreen(text = "Please wait...", modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PreVerificationOtpBottomSheet(
    phoneNumber: String,
    otpCode: String,
    otpVerificationResult: Resource<*>?,
    onOtpCodeChange: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit
) {
    var resendTimer by remember { mutableStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Enter OTP", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("OTP sent to $phoneNumber", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

        OtpField(
            length = 6,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            otpText = otpCode,
            onOtpChange = onOtpCodeChange,
            onOtpComplete = {}
        )

        TextButton(
            onClick = {
                if (canResend) {
                    canResend = false
                    onResendOtp()
                }
            },
            enabled = canResend
        ) {
            Text(if (canResend) "Resend OTP" else "Resend in $resendTimer seconds")
        }

        if (otpVerificationResult is Resource.Error) {
            Text(otpVerificationResult.message ?: "Incorrect OTP", color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = { onVerifyOtp(otpCode) },
            enabled = otpCode.length == 6 && otpVerificationResult !is Resource.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (otpVerificationResult is Resource.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Verify OTP")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
