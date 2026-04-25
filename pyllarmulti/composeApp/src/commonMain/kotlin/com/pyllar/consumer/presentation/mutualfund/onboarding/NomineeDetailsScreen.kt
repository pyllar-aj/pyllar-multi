package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pyllar.otp.OtpField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
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

    var showOtpScreen by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var skipAddingNominee by remember { mutableStateOf(true) }
    var nominees by remember { mutableStateOf(listOf(NomineeInfo("", "", "", ""))) }

    var effectiveUserId by remember { mutableStateOf(userId) }
    var effectiveKycAttemptId by remember { mutableStateOf(kycAttemptId) }
    var effectiveInvestorId by remember { mutableStateOf(investorId) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("NomineeDetails")
        if (effectiveUserId.isBlank()) effectiveUserId = sessionStore.getValue("current_user_id") ?: ""
        if (effectiveKycAttemptId.isBlank()) effectiveKycAttemptId = sessionStore.getValue("kyc_attempt_id") ?: ""
        if (effectiveInvestorId.isBlank()) effectiveInvestorId = sessionStore.getValue("investor_id") ?: ""
        phoneNumber = sessionStore.getCurrentPhone()
    }

    LaunchedEffect(nomineeSubmissionResult) {
        if (nomineeSubmissionResult is Resource.Success) {
            isSubmitting = false
            showOtpScreen = true
        } else if (nomineeSubmissionResult is Resource.Error) {
            isSubmitting = false
        }
    }

    LaunchedEffect(otpVerificationResult) {
        if (otpVerificationResult is Resource.Success) {
            showOtpScreen = false
            onNext(navigationInfo?.nextScreen)
        }
    }

    LaunchedEffect(showOtpScreen) {
        if (showOtpScreen) {
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
    }

    val relationshipOptions = listOf("father", "mother", "spouse", "son", "daughter", "brother", "sister", "others")
    val relationshipDisplay = relationshipOptions.associateWith { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = 0.dp,
            sheetContent = {
                if (showOtpScreen) {
                    OtpVerificationBottomSheet(
                        phoneNumber = if (phoneNumber.length >= 4) "******${phoneNumber.takeLast(4)}" else phoneNumber,
                        otpCode = otpCode,
                        otpVerificationResult = otpVerificationResult,
                        onOtpCodeChange = { otpCode = it },
                        onVerifyOtp = { viewModel.verifyOtp(phoneNumber, it) },
                        onResendOtp = { viewModel.generateOtp(phoneNumber) }
                    )
                } else {
                    Box(modifier = Modifier.height(1.dp))
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
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
                    OnboardingStepper(currentStep = 1, completedStep = 1, currentScreenRoute = ScreenNames.NOMINEE_DETAILS)
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
                    Text("Nominee Details", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { skipAddingNominee = !skipAddingNominee },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = skipAddingNominee, onCheckedChange = { skipAddingNominee = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Do you wish to add a nominee?", style = MaterialTheme.typography.bodyLarge)
                    }

                    if (skipAddingNominee) {
                        Text(
                            "You can add these anytime from your profile settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    } else {
                        nominees.forEachIndexed { index, nominee ->
                            if (index > 0) Divider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Nominee ${index + 1}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                if (index > 0) {
                                    IconButton(onClick = { nominees = nominees.toMutableList().apply { removeAt(index) } }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = nominee.name,
                                onValueChange = { val value = it; nominees = nominees.toMutableList().apply { this[index] = this[index].copy(name = value) } },
                                label = { Text("Nominee Name") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                            )

                            OutlinedTextField(
                                value = nominee.panNumber,
                                onValueChange = { val value = it.uppercase().take(10); nominees = nominees.toMutableList().apply { this[index] = this[index].copy(panNumber = value) } },
                                label = { Text("PAN Number") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next)
                            )

                            ExposedDropdownFieldWithDisplay(
                                label = "Relationship",
                                selected = nominee.relationship,
                                options = relationshipOptions,
                                displayMap = relationshipDisplay,
                                onSelect = { value -> nominees = nominees.toMutableList().apply { this[index] = this[index].copy(relationship = value) } }
                            )

                            OutlinedTextField(
                                value = nominee.dateOfBirth,
                                onValueChange = { val value = it; nominees = nominees.toMutableList().apply { this[index] = this[index].copy(dateOfBirth = value) } },
                                label = { Text("Date of Birth (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Date") }
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

                    if (nomineeSubmissionResult is Resource.Error) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                nomineeSubmissionResult?.message ?: "Something went wrong",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isSubmitting = true
                            viewModel.submitNomineeDetailsV2(
                                userId = effectiveUserId,
                                kycAttemptId = effectiveKycAttemptId,
                                investorId = effectiveInvestorId,
                                wantsToAddNominee = !skipAddingNominee,
                                nominees = nominees
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Submit Details", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun OtpVerificationBottomSheet(
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
        Text("Verify Nominee Addition", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("OTP sent to $phoneNumber", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

        OtpField(
            length = 6,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            otpText = otpCode,
            onOtpChange = onOtpCodeChange,
            onOtpComplete = {}
        )

        TextButton(
            onClick = { if (canResend) { canResend = false; onResendOtp() } },
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
