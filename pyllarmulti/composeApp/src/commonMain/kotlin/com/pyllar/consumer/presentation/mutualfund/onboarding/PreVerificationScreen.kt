package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.pyllar.consumer.util.platformLog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
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
import pyllar.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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

    var panFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val panNumber = panFieldValue.text
    var panError by remember { mutableStateOf<String?>(null) }
    var autoFetchFailed by remember { mutableStateOf(false) }
    var isPhoneMissing by remember { mutableStateOf(false) }
    var fetchedPanName by remember { mutableStateOf<String?>(null) }

    // Auto-fill PAN from prepopulated data
    LaunchedEffect(uiState.prepopulatedData) {
        val prepopulatedPan = uiState.prepopulatedData["panNumber"]
        if (!prepopulatedPan.isNullOrBlank() && panFieldValue.text.isBlank()) {
            panFieldValue = TextFieldValue(
                text = prepopulatedPan,
                selection = TextRange(prepopulatedPan.length)
            )
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
    LaunchedEffect(uiState.verificationResult, uiState.verificationStatus) {
        platformLog("PreVerificationScreen: \uD83D\uDCC4 Status change: ${uiState.verificationStatus}")
        
        when (val result = uiState.verificationResult) {
            is Resource.Success -> {
                platformLog("PreVerificationScreen: \u2705 SUCCESS - Result: ${result.data?.status}, Msg: ${result.data?.message}")
                platformLog("PreVerificationScreen: \uD83E\uDDF3 Navigation Info: ${result.data?.navigation?.nextScreen ?: "None"} (Action: ${result.data?.navigation?.action ?: "None"})")
                
                // Keep isSubmitting true if we are still polling
                if (uiState.verificationStatus != VerificationStatus.IN_PROGRESS) {
                    isSubmitting = false
                }
                readinessCheckStarted = true
                PlatformAnalyticsLogger.logEvent("readiness_check_success", mapOf("pan_last4" to panNumber.takeLast(4)))
            }
            is Resource.Error -> {
                platformLog("PreVerificationScreen: \u274C ERROR - Msg: ${result.message}")
                isSubmitting = false
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent("readiness_check_error", mapOf("pan_last4" to panNumber.takeLast(4), "error" to (result.message ?: "unknown")))
            }
            is Resource.Loading -> {
                platformLog("PreVerificationScreen: \u23F3 LOADING...")
            }
            else -> {}
        }
    }

    // Handle server-driven navigation
    LaunchedEffect(uiState.nextScreen) {
        uiState.nextScreen?.let { screenName ->
            platformLog("PreVerificationScreen: \uD83D\uDE80 NAVIGATING to screen: $screenName")
            keyboardController?.hide()
            focusManager.clearFocus()
            PlatformAnalyticsLogger.logEvent("server_navigation", mapOf("from_screen" to "pre_verification", "to_screen" to screenName))
            onNavigateToScreen(screenName)
        }
    }

    // OTP bottom sheet state
    var showOtpBottomSheet by remember { mutableStateOf(false) }
    var otpFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val otpCode = otpFieldValue.text
    var otpSheetPhoneDisplay by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var userPhone by remember { mutableStateOf("") }
    var currentPrefillId by remember { mutableStateOf<String?>(null) } // ViewModel stores ID

    LaunchedEffect(Unit) {
        userPhone = sessionStore.getCurrentPhone()
        platformLog("PreVerificationScreen: \uD83D\uDCF1 Fetched userPhone from sessionStore: '$userPhone'")
    }

    // Timeout handling - reset isSubmitting after 90 seconds if API doesn't complete
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90000) // 90 seconds
            if (isSubmitting && uiState.verificationResult !is Resource.Success && uiState.verificationResult !is Resource.Error) {
                platformLog("PreVerificationScreen: \u26A0\uFE0F Safety timeout: API call took too long, resetting isSubmitting and triggering button timeout")
                isSubmitting = false
                timeoutState.triggerTimeout()
            }
        }
    }

    // Handle PAN Fetch Result
    LaunchedEffect(uiState.panFetchResult) {
        val result = uiState.panFetchResult
        if (result is Resource.Success) {
            val data = result.data
            if (data != null) {
                if (data.status == "OTP_GENERATED") {
                    currentPrefillId = data.prefillId.toString()
                    showOtpBottomSheet = true
                } else if (data.status == "ALREADY_VERIFIED") {
                    if (!data.panNumber.isNullOrBlank()) {
                        panFieldValue = TextFieldValue(
                            text = data.panNumber,
                            selection = TextRange(data.panNumber.length)
                        )
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
            val data = result.data
            if (data != null && data.status == "SUCCESS") {
                showOtpBottomSheet = false
                val panDetails = data.panDetails
                val personalDetails = data.personalDetails

                if (panDetails?.panNumber != null) {
                    panFieldValue = TextFieldValue(
                        text = panDetails.panNumber,
                        selection = TextRange(panDetails.panNumber.length)
                    )
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
            Spacer(modifier = Modifier.height(32.dp))
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
                PlatformAnalyticsLogger.logEvent("pre_verification_help_clicked", mapOf("screen" to "pre_verification", "button_location" to "top_right"))
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
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
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
                                    platformLog("PreVerificationScreen: 🔵 Autofetch button clicked. current state userPhone='$userPhone'")
                                    scope.launch {
                                        // Fetch latest phone from session store in case LaunchedEffect hasn't completed or state is stale
                                        val latestPhone = if (userPhone.isNotBlank()) userPhone else {
                                            val fetched = sessionStore.getCurrentPhone()
                                            platformLog("PreVerificationScreen: 📱 Real-time phone fetch from sessionStore: '$fetched'")
                                            if (fetched.isNotBlank()) userPhone = fetched
                                            fetched
                                        }

                                        PlatformAnalyticsLogger.logEvent("pre_verification_find_my_pan_clicked", mapOf("has_phone" to latestPhone.isNotBlank()))
                                        
                                        if (latestPhone.isNotBlank()) {
                                            platformLog("PreVerificationScreen: 🚀 Initiating PAN fetch for $latestPhone")
                                            isPhoneMissing = false
                                            currentPrefillId = null
                                            otpFieldValue = TextFieldValue("")
                                            viewModel.clearPanVerifyOtpResult()
                                            viewModel.initiatePanFetch(latestPhone)
                                        } else {
                                            platformLog("PreVerificationScreen: ⚠️ Phone is blank in both state and sessionStore, showing manual form")
                                            isPhoneMissing = true
                                            autoFetchFailed = true
                                            showManualEntryForm = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.panFetchResult !is Resource.Loading,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                if (uiState.panFetchResult is Resource.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetching...", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
                                    val panBringIntoViewRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                                    var isPanFocused by remember { mutableStateOf(false) }
                                    
                                    LaunchedEffect(isPanFocused) {
                                        if (isPanFocused) {
                                            delay(400)
                                            panBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                    if (autoFetchFailed) {
                                        Text(
                                            text = if (isPhoneMissing) "Phone number not found. Please enter PAN manually." else "Auto-fetch failed. Please enter manually.",
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
                                        value = panFieldValue,
                                        onValueChange = { newValue ->
                                            val newText = newValue.text.uppercase().filter { it.isLetterOrDigit() }
                                                .filterIndexed { index, c ->
                                                    when (index) {
                                                        in 0..4 -> c.isLetter()
                                                        in 5..8 -> c.isDigit()
                                                        9 -> c.isLetter()
                                                        else -> false
                                                    }
                                                }
                                            
                                            if (newText.length <= 10) {
                                                // Adjust selection if text changed externally (filtering)
                                                val selection = if (newText.length < newValue.text.length) {
                                                    TextRange(newText.length)
                                                } else {
                                                    newValue.selection
                                                }
                                                
                                                panFieldValue = newValue.copy(
                                                    text = newText,
                                                    selection = selection
                                                )
                                                panError = null
                                                autoFetchFailed = false
                                                isPhoneMissing = false
                                                viewModel.clearError()
                                                if (fetchedPanName != null) {
                                                    fetchedPanName = null
                                                }
                                            }
                                        },
                                        placeholder = { Text("ABCDE1234F") },
                                        singleLine = true,
                                        keyboardOptions = when (panNumber.length) {
                                            in 0..4, 9 -> KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
                                            in 5..8 -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                            else -> KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
                                        },
                                        isError = uiState.verificationResult is Resource.Error || fourthLetterError,
                                        modifier = Modifier.fillMaxWidth()
                                            .bringIntoViewRequester(panBringIntoViewRequester)
                                            .onFocusChanged { isPanFocused = it.isFocused },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    if (fourthLetterError) {
                                        Text("Invalid fourth character. Must be 'P' for individual.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }

                                    fetchedPanName?.let { name ->
                                        Text("Name: $name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    TimeoutButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            if (panNumber.isBlank()) {
                                                panError = "PAN is required"
                                                return@TimeoutButton
                                            } else if (!panNumber.matches(Regex("^[A-Z]{3}P[A-Z]{1}[0-9]{4}[A-Z]{1}$"))) {
                                                panError = "Invalid PAN format. Example: ABCDE1234F"
                                                return@TimeoutButton
                                            }
                                            isSubmitting = true
                                            PlatformAnalyticsLogger.logEvent("pre_verification_verify_pan_clicked", mapOf(
                                                "pan_source" to if (fetchedPanName != null) "find_my_pan" else "manual",
                                                "pan_length" to panNumber.length
                                            ))
                                            viewModel.checkInvestorReadiness(panNumber)
                                        },
                                        enabled = isPanValid && !isSubmitting && uiState.verificationResult !is Resource.Loading && uiState.verificationStatus != VerificationStatus.IN_PROGRESS,
                                        timeoutState = timeoutState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isSubmitting || uiState.verificationResult is Resource.Loading || uiState.verificationStatus == VerificationStatus.IN_PROGRESS) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("Checking...", fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text(if (fetchedPanName != null) "Verify" else "Verify PAN", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                     if (uiState.verificationStatus == VerificationStatus.IN_PROGRESS) {
                                         Spacer(modifier = Modifier.height(12.dp))
                                         Column(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalAlignment = Alignment.CenterHorizontally
                                         ) {
                                             Text(
                                                 text = "Verification in progress...",
                                                 style = MaterialTheme.typography.bodyMedium,
                                                 color = MaterialTheme.colorScheme.primary
                                             )
                                             uiState.serverMessage?.let { message ->
                                                 Text(
                                                     text = message,
                                                     style = MaterialTheme.typography.bodySmall,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                     textAlign = TextAlign.Center,
                                                     modifier = Modifier.padding(top = 4.dp)
                                                 )
                                             }
                                         }
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
                    otpFieldValue = otpFieldValue,
                    otpVerificationResult = uiState.panVerifyOtpResult,
                    onOtpFieldValueChange = { otpFieldValue = it },
                    onVerifyOtp = { otpValue ->
                        PlatformAnalyticsLogger.logEvent("find_my_pan_otp_verify_clicked", mapOf("otp_length" to otpValue.length))
                        val prefillIdStr = currentPrefillId
                        if (prefillIdStr != null && userPhone.isNotBlank()) {
                            viewModel.verifyOtpAndFetchPan(userPhone, prefillIdStr.toLong(), otpValue)
                        }
                    },
                    onResendOtp = { 
                        currentPrefillId = null
                        otpFieldValue = TextFieldValue("")
                        viewModel.clearPanVerifyOtpResult()
                        viewModel.initiatePanFetch(userPhone) 
                    },
                    onDismiss = {
                        otpFieldValue = TextFieldValue("")
                        currentPrefillId = null
                        viewModel.clearPanVerifyOtpResult()
                        showOtpBottomSheet = false
                    }
                )
            }
        }

        // Only show full-screen overlay for initial loading or PAN fetch, not during polling
        val showOverlay = (isSubmitting && uiState.verificationStatus != VerificationStatus.IN_PROGRESS) || 
                          (uiState.verificationResult is Resource.Loading && uiState.verificationStatus != VerificationStatus.IN_PROGRESS) || 
                          uiState.panFetchResult is Resource.Loading
        
        if (showOverlay) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).zIndex(10f)) {
                LoadingScreen(text = "Please wait...", modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PreVerificationOtpBottomSheet(
    phoneNumber: String,
    otpFieldValue: TextFieldValue,
    otpVerificationResult: Resource<*>?,
    onOtpFieldValueChange: (TextFieldValue) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onDismiss: () -> Unit
) {
    var resendTimer by remember { mutableStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    var isResent by remember { mutableStateOf(false) }

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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close"
                )
            }
        }
        Text("Enter OTP", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("OTP sent to $phoneNumber", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

        OtpField(
            length = 6,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            otpFieldValue = otpFieldValue,
            onOtpFieldValueChange = onOtpFieldValueChange,
            onOtpComplete = {}
        )

        Text(
            text = org.jetbrains.compose.resources.stringResource(Res.string.pan_fetch_otp_consent_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        TextButton(
            onClick = {
                if (canResend) {
                    canResend = false
                    isResent = true
                    onResendOtp()
                }
            },
            enabled = canResend
        ) {
            Text(if (canResend) "Resend OTP" else "Resend in $resendTimer seconds")
        }

        if (otpVerificationResult !is Resource.Loading && isResent && otpVerificationResult !is Resource.Error) {
             Text("OTP resent successfully!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
        }

        if (otpVerificationResult is Resource.Error) {
            val errorMessage = otpVerificationResult.message ?: ""
            val isNetworkError = otpVerificationResult.isNetworkError ||
                errorMessage.contains("NETWORK_ERROR", ignoreCase = true) ||
                errorMessage.contains("Network", ignoreCase = true) ||
                errorMessage.contains("timeout", ignoreCase = true) ||
                errorMessage.contains("connection", ignoreCase = true) ||
                errorMessage.contains("Failed to connect", ignoreCase = true) ||
                errorMessage.contains("IOException", ignoreCase = true)
            if (isNetworkError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = org.jetbrains.compose.resources.stringResource(Res.string.check_internet_connection),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = org.jetbrains.compose.resources.stringResource(Res.string.incorrect_otp_message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = { onVerifyOtp(otpFieldValue.text) },
            enabled = otpFieldValue.text.length == 6 && otpVerificationResult !is Resource.Loading,
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
