package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.*
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.otp.OtpField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NomineeDetailsScreen(
    onNext: (String?) -> Unit,
    userId: String = "",
    kycAttemptId: String = "",
    investorId: String = "",
    onNavigateToHelp: () -> Unit = {},
    viewModel: NomineeDetailsViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val scope = rememberCoroutineScope()
    val nomineeSubmissionResult by viewModel.nomineeSubmissionResult.collectAsState()
    val navigationInfo by viewModel.navigationInfo.collectAsState()
    val otpVerificationResult by viewModel.otpVerificationResult.collectAsState()
    val otpGenerationResult by viewModel.otpGenerationResult.collectAsState()

    // Screen state
    var showOtpScreen by remember { mutableStateOf(false) }
    var otpFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val otpCode = otpFieldValue.text
    var phoneNumber by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var hasNavigatedAfterOtp by remember { mutableStateOf(false) }

    // Initialization state
    var effectiveUserId by remember { mutableStateOf(userId) }
    var effectiveKycAttemptId by remember { mutableStateOf(kycAttemptId) }
    var effectiveInvestorId by remember { mutableStateOf(investorId) }
    var isInitialized by remember { mutableStateOf(false) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Form state
    var skipAddingNominee by remember { mutableStateOf(true) }
    var nominees by remember { mutableStateOf(listOf(NomineeInfo("", "", "", ""))) }

    // Date picker state
    var showNomineeDatePicker by remember { mutableStateOf<Int?>(null) }
    var datePickerStep by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    // Combined initialization
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("NomineeDetails")
        platformLog("NomineeDetailsScreen: \uD83D\uDCCB Received Parameters: userId='$userId', kycId='$kycAttemptId', invId='$investorId'")
        
        try {
            effectiveUserId = if (userId.isBlank()) sessionStore.getValue("current_user_id") ?: "" else userId
            effectiveKycAttemptId = if (kycAttemptId.isBlank()) sessionStore.getValue("kyc_attempt_id") ?: "" else kycAttemptId
            effectiveInvestorId = if (investorId.isBlank()) sessionStore.getValue("investor_id") ?: "" else investorId
            phoneNumber = sessionStore.getCurrentPhone()
            
            platformLog("NomineeDetailsScreen: \u2705 Effective Parameters: userId='$effectiveUserId', kycId='$effectiveKycAttemptId', invId='$effectiveInvestorId'")
        } finally {
            isInitialized = true
        }
    }

    // Handle Submission API response
    LaunchedEffect(nomineeSubmissionResult) {
        when (val result = nomineeSubmissionResult) {
            is Resource.Success -> {
                isSubmitting = false
                platformLog("NomineeDetailsScreen: \u2705 Submission success, showing OTP screen")
                showOtpScreen = true
            }
            is Resource.Error -> {
                isSubmitting = false
                platformLog("NomineeDetailsScreen: \u274C Submission error: ${result.message}")
            }
            else -> {}
        }
    }

    // Handle OTP verification result
    LaunchedEffect(otpVerificationResult) {
        if (otpVerificationResult is Resource.Success && !hasNavigatedAfterOtp) {
            hasNavigatedAfterOtp = true
            showOtpScreen = false
            platformLog("NomineeDetailsScreen: \uD83D\uDE80 OTP verified, navigating to next: ${navigationInfo?.nextScreen}")
            onNext(navigationInfo?.nextScreen)
        } else if (otpVerificationResult is Resource.Error) {
            otpFieldValue = TextFieldValue("")
            keyboardController?.show()
        }
    }

    // Expand bottom sheet when OTP screen is triggered
    LaunchedEffect(showOtpScreen) {
        if (showOtpScreen) {
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
    }

    // Relationship mapping
    val relationshipOptions = listOf("father", "mother", "spouse", "son", "daughter", "brother", "sister", "others")
    val relationshipDisplay = relationshipOptions.associateWith { 
        if (it == "others") "Others" else it.replaceFirstChar { char -> char.uppercaseChar() } 
    }

    if (!isInitialized) {
        LoadingScreen(text = "Initializing...")
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = 0.dp,
            sheetContent = {
                if (showOtpScreen) {
                    OtpVerificationBottomSheet(
                        maskedPhoneNumber = if (phoneNumber.length >= 4) "******${phoneNumber.takeLast(4)}" else phoneNumber,
                        otpFieldValue = otpFieldValue,
                        otpVerificationResult = otpVerificationResult,
                        otpGenerationResult = otpGenerationResult,
                        onOtpFieldValueChange = { otpFieldValue = it },
                        onVerifyOtp = { otpValue ->
                            scope.launch {
                                val fullPhone = sessionStore.getCurrentPhone()
                                viewModel.verifyOtp(fullPhone, otpValue)
                            }
                        },
                        onResendOtp = {
                            scope.launch {
                                val fullPhone = sessionStore.getCurrentPhone()
                                viewModel.generateOtp(fullPhone)
                            }
                        },
                        onDismiss = { showOtpScreen = false }
                    )
                } else {
                    Box(modifier = Modifier.height(1.dp))
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Share Logic */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                    LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onNavigateToHelp) {
                        Text("Help", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Progress Stepper
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
                ) {
                    OnboardingStepper(
                        currentStep = 1, 
                        completedStep = 1, 
                        currentScreenRoute = ScreenNames.NOMINEE_DETAILS
                    )
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp)
                        .imePadding()
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { 
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Nominee Details", 
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    // Skip Toggle
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { skipAddingNominee = !skipAddingNominee },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = skipAddingNominee, onCheckedChange = { skipAddingNominee = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Do you wish to add a nominee?", style = MaterialTheme.typography.bodyLarge)
                        }

                        if (skipAddingNominee) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.width(60.dp))
                                Text(
                                    "You can add these anytime from your profile settings.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Nominee Forms
                    if (!skipAddingNominee) {
                        nominees.forEachIndexed { index, nominee ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            NomineeFormSection(
                                index = index,
                                nominee = nominee,
                                totalNominees = nominees.size,
                                relationshipOptions = relationshipOptions,
                                relationshipDisplay = relationshipDisplay,
                                onUpdate = { updated ->
                                    nominees = nominees.toMutableList().apply { this[index] = updated }
                                },
                                onRemove = {
                                    nominees = nominees.toMutableList().apply { removeAt(index) }
                                },
                                showDatePicker = { showNomineeDatePicker = index }
                            )
                        }

                        if (nominees.size < 3) {
                            OutlinedButton(
                                onClick = { nominees = nominees + NomineeInfo("", "", "", "") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add another nominee")
                            }
                        }
                    }

                    // Error Display
                    if (nomineeSubmissionResult is Resource.Error) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                nomineeSubmissionResult?.message ?: "Something went wrong. Please try again.",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            isSubmitting = true
                            viewModel.submitNomineeDetailsV2(
                                userId = effectiveUserId,
                                kycAttemptId = effectiveKycAttemptId,
                                investorId = effectiveInvestorId,
                                wantsToAddNominee = !skipAddingNominee,
                                nominees = if (skipAddingNominee) emptyList() else nominees
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (!skipAddingNominee) "Submit" else "Continue", 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Hierarchical Date Picker (Shared UX with NameDobScreen)
    if (showNomineeDatePicker != null) {
        val currentNomineeIndex = showNomineeDatePicker!!
        HierarchicalDatePicker(
            onDateSelected = { y: Int, m: Int, d: Int ->
                val formattedDate = "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                nominees = nominees.toMutableList().apply {
                    this[currentNomineeIndex] = this[currentNomineeIndex].copy(dateOfBirth = formattedDate)
                }
                showNomineeDatePicker = null
                datePickerStep = 0; selectedYear = null; selectedMonth = null; selectedDay = null
            },
            onDismiss = {
                showNomineeDatePicker = null
                datePickerStep = 0; selectedYear = null; selectedMonth = null; selectedDay = null
            },
            currentStep = datePickerStep,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDay = selectedDay,
            onStepChange = { step: Int -> datePickerStep = step },
            onYearSelected = { year: Int -> selectedYear = year },
            onMonthSelected = { month: Int -> selectedMonth = month },
            onDaySelected = { day: Int -> selectedDay = day }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NomineeFormSection(
    index: Int,
    nominee: NomineeInfo,
    totalNominees: Int,
    relationshipOptions: List<String>,
    relationshipDisplay: Map<String, String>,
    onUpdate: (NomineeInfo) -> Unit,
    onRemove: () -> Unit,
    showDatePicker: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val nameBringIntoViewRequester = remember { BringIntoViewRequester() }
    val panBringIntoViewRequester = remember { BringIntoViewRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (totalNominees > 1) "Nominee ${index + 1}" else "Primary Nominee", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (index > 0) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Nominee Name
        OutlinedTextField(
            value = nominee.name,
            onValueChange = { onUpdate(nominee.copy(name = it)) },
            label = { Text("Nominee Name") },
            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(nameBringIntoViewRequester),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
        )

        // PAN Number with Validation
        OutlinedTextField(
            value = nominee.panNumber,
            onValueChange = { input ->
                val filtered = input.filter { it.isLetterOrDigit() }.uppercase()
                if (filtered.length <= 10) {
                    // PAN validation: 4th character must be 'P' for Individual
                    if (filtered.length >= 4 && filtered[3] != 'P') {
                        // Reject or handle error
                    } else {
                        onUpdate(nominee.copy(panNumber = filtered))
                    }
                }
            },
            label = { Text("Enter PAN Number") },
            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(panBringIntoViewRequester),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = when {
                    nominee.panNumber.length < 5 -> KeyboardType.Text
                    nominee.panNumber.length < 9 -> KeyboardType.Number
                    else -> KeyboardType.Text
                },
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            singleLine = true
        )

        // Relationship
        ExposedDropdownFieldWithDisplay(
            label = "Nominee's relationship to you",
            selected = nominee.relationship,
            options = relationshipOptions,
            displayMap = relationshipDisplay,
            onSelect = { onUpdate(nominee.copy(relationship = it)) }
        )

        // Date of Birth
        OutlinedTextField(
            value = nominee.dateOfBirth,
            onValueChange = { onUpdate(nominee.copy(dateOfBirth = it)) },
            label = { Text("Nominee Date of Birth") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = { 
                IconButton(onClick = showDatePicker) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                }
            },
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { src ->
                LaunchedEffect(src) {
                    src.interactions.collect { if (it is PressInteraction.Release) showDatePicker() }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

@Composable
private fun OtpVerificationBottomSheet(
    maskedPhoneNumber: String,
    otpFieldValue: TextFieldValue,
    otpVerificationResult: Resource<String>?,
    otpGenerationResult: Resource<String>?,
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Verify Nominee Addition", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.error) }
        }
        
        Text("OTP sent to $maskedPhoneNumber", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

        OtpField(
            length = 6,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            enabled = otpVerificationResult !is Resource.Loading,
            isError = otpVerificationResult is Resource.Error,
            otpFieldValue = otpFieldValue,
            onOtpFieldValueChange = onOtpFieldValueChange,
            onOtpComplete = { 
                // Don't auto-submit to match Android UX
            }
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
                    isResent = true
                    onOtpFieldValueChange(TextFieldValue("")) // Clear previous OTP input
                    onResendOtp() 
                } 
            },
            enabled = canResend
        ) {
            Text(if (canResend) "Resend OTP" else "Resend in $resendTimer seconds")
        }

        if (otpVerificationResult is Resource.Error) {
            Text(otpVerificationResult.message ?: "Incorrect OTP. Please try again.", color = MaterialTheme.colorScheme.error)
        }
        
        if (otpGenerationResult is Resource.Success && isResent) {
            Text("OTP resent successfully!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        } else if (otpGenerationResult is Resource.Error) {
            Text(otpGenerationResult.message ?: "Failed to resend OTP.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { onVerifyOtp(otpFieldValue.text) },
            enabled = otpFieldValue.text.length == 6 && otpVerificationResult !is Resource.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (otpVerificationResult is Resource.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Verify OTP", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
