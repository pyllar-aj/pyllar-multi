package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.filterEnglishName
import com.pyllar.consumer.util.filterEnglishPan
import com.pyllar.consumer.util.platformLog
import com.pyllar.otp.OtpField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val NMV2Cream        = Color(0xFFFBF9F4)
private val NMV2CreamTint    = Color(0xFFF5EEDB)
private val NMV2BronzeInk    = Color(0xFF3E2723)
private val NMV2BronzeMuted  = Color(0xFF6D4C41)
private val NMV2GoldDeep     = Color(0xFF8B6B25)
private val NMV2GoldAccent   = Color(0xFFD4AF37)
private val NMV2Obsidian     = Color(0xFF0A2415)
private val NMV2LinkGreen    = Color(0xFF1A7A42)
private val NMV2VolatilityRed = Color(0xFFC62828)
private val NMV2SuccessGreen = Color(0xFF2E7D32)
private val NMV2FieldBorder  = Color(0xFFD7CCC8)
private val NMV2CardBorder   = Color(0xFFEFEBE9)
private val NMV2RemoveBg     = Color(0xFFFFF0F0)
private val NMV2RemoveBorder = Color(0xFFFFCDD2)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NomineeDetailsScreenV2(
    onNext: (String?) -> Unit,
    userId: String = "",
    kycAttemptId: String = "",
    investorId: String = "",
    onNavigateToHelp: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: NomineeDetailsViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Initialization
    var effectiveUserId by remember { mutableStateOf(userId) }
    var effectiveKycAttemptId by remember { mutableStateOf(kycAttemptId) }
    var effectiveInvestorId by remember { mutableStateOf(investorId) }
    var isInitialized by remember { mutableStateOf(false) }

    // OTP
    var showOtpScreen by remember { mutableStateOf(false) }
    var otpFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val otpCode = otpFieldValue.text
    var phoneNumber by remember { mutableStateOf("") }
    var hasNavigatedAfterOtp by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()

    // Form state
    var skipAddingNominee by remember { mutableStateOf(true) }
    var nominees by remember { mutableStateOf(listOf(NomineeInfo("", "", "", ""))) }

    // Date picker state
    var showNomineeDatePicker by remember { mutableStateOf<Int?>(null) }
    var nomineeDatePickerStep by remember { mutableStateOf(0) }
    var nomineeSelectedYear by remember { mutableStateOf<Int?>(null) }
    var nomineeSelectedMonth by remember { mutableStateOf<Int?>(null) }
    var nomineeSelectedDay by remember { mutableStateOf<Int?>(null) }

    // Collect from ViewModel
    val nomineeSubmissionResult by viewModel.nomineeSubmissionResult.collectAsState()
    val navigationInfo by viewModel.navigationInfo.collectAsState()
    val otpVerificationResult by viewModel.otpVerificationResult.collectAsState()
    val otpGenerationResult by viewModel.otpGenerationResult.collectAsState()

    // Initialization
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("NomineeDetailsV4")
        try {
            effectiveUserId = if (userId.isBlank()) sessionStore.getValue("current_user_id") ?: "" else userId
            effectiveKycAttemptId = if (kycAttemptId.isBlank()) sessionStore.getValue("kyc_attempt_id") ?: "" else kycAttemptId
            effectiveInvestorId = if (investorId.isBlank()) sessionStore.getValue("investor_id") ?: "" else investorId
            phoneNumber = sessionStore.getCurrentPhone()
        } finally {
            isInitialized = true
        }
    }

    // Handle submission result
    LaunchedEffect(nomineeSubmissionResult) {
        when (val result = nomineeSubmissionResult) {
            is Resource.Success<*> -> {
                PlatformAnalyticsLogger.logEvent(
                    "nominee_details_submit_success",
                    mapOf("has_nominee" to !skipAddingNominee, "screen_version" to "v4")
                )
                try {
                    val phone = sessionStore.getCurrentPhone()
                    val maskedPhone = if (phone.length > 4) "******${phone.takeLast(4)}" else "******$phone"
                    phoneNumber = maskedPhone
                    otpFieldValue = TextFieldValue("")
                    showOtpScreen = true
                    isSubmitting = false
                } catch (e: Exception) {
                    platformLog("NomineeDetailsScreenV2: Error getting phone: ${e.message}")
                    isSubmitting = false
                    onNext(navigationInfo?.nextScreen)
                }
            }
            is Resource.Error<*> -> {
                isSubmitting = false
                PlatformAnalyticsLogger.logEvent(
                    "nominee_details_submit_error",
                    mapOf("error" to (result.message ?: "unknown"), "screen_version" to "v4")
                )
            }
            is Resource.Loading<*> -> {}
            null -> {}
        }
    }

    // Safety timeout for submission
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90000)
            if (isSubmitting && nomineeSubmissionResult !is Resource.Success<*> && nomineeSubmissionResult !is Resource.Error) {
                platformLog("NomineeDetailsScreenV2: ⚠️ Safety timeout, resetting isSubmitting")
                isSubmitting = false
            }
        }
    }

    // Handle OTP verification
    LaunchedEffect(otpVerificationResult) {
        when (val result = otpVerificationResult) {
            is Resource.Success<*> -> {
                showOtpScreen = false
                if (!hasNavigatedAfterOtp) {
                    hasNavigatedAfterOtp = true
                    onNext(navigationInfo?.nextScreen)
                }
            }
            is Resource.Error<*> -> {
                otpFieldValue = TextFieldValue("")
                keyboardController?.show()
            }
            is Resource.Loading<*> -> {}
            null -> {}
        }
    }

    // Expand bottom sheet when OTP screen shown
    LaunchedEffect(showOtpScreen) {
        if (showOtpScreen) {
            focusManager.clearFocus()
            keyboardController?.hide()
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
    }

    val relationshipOptions = listOf("father", "mother", "spouse", "son", "daughter", "brother", "sister", "others")
    val relationshipDisplay = mapOf(
        "father" to "Father", "mother" to "Mother", "spouse" to "Spouse",
        "son" to "Son", "daughter" to "Daughter", "brother" to "Brother",
        "sister" to "Sister", "others" to "Others"
    )

    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize().background(NMV2Cream), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NMV2Obsidian)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(NMV2Cream)) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetSwipeEnabled = false,
            containerColor = NMV2Cream,
            sheetContainerColor = NMV2Cream,
            sheetContent = {
                if (showOtpScreen) {
                    NMV2OtpBottomSheet(
                        phoneNumber = phoneNumber,
                        otpFieldValue = otpFieldValue,
                        otpVerificationResult = otpVerificationResult,
                        otpGenerationResult = otpGenerationResult,
                        onOtpFieldValueChange = { otpFieldValue = it },
                        onVerifyOtp = { otp ->
                            scope.launch {
                                val fullPhone = sessionStore.getCurrentPhone()
                                viewModel.verifyOtp(fullPhone, otp)
                            }
                        },
                        onResendOtp = {
                            scope.launch {
                                val fullPhone = sessionStore.getCurrentPhone()
                                viewModel.generateOtp(fullPhone)
                            }
                        },
                        onDismiss = { showOtpScreen = false },
                        viewModel = viewModel
                    )
                } else {
                    Box(modifier = Modifier.height(1.dp))
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Top App Bar ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onBack() }
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = NMV2LinkGreen, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NMV2LinkGreen)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LanguageLetterButton(textColor = NMV2LinkGreen)
                            TextButton(onClick = onNavigateToHelp) {
                                Text("Help", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NMV2LinkGreen)
                            }
                        }
                    }

                    // ── Stepper ──
                    Surface(color = NMV2Cream, shadowElevation = 8.dp, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        OnboardingStepper(currentStep = 1, completedStep = 1, currentScreenRoute = ScreenNames.NOMINEE_DETAILS)
                    }

                    // ── Scrollable Content ──
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp)
                            .padding(top = 18.dp, bottom = 28.dp)
                            .imePadding(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Nominee Details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NMV2BronzeInk)

                        // ── Skip Toggle Card ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, NMV2CardBorder, RoundedCornerShape(16.dp))
                                .clickable { skipAddingNominee = !skipAddingNominee }
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(if (skipAddingNominee) NMV2Obsidian else Color.White, RoundedCornerShape(6.dp))
                                        .border(1.5.dp, if (skipAddingNominee) NMV2GoldAccent else NMV2FieldBorder, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (skipAddingNominee) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = NMV2Cream, modifier = Modifier.size(13.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("I want to skip adding nominees for now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NMV2BronzeInk)
                                    if (skipAddingNominee) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("You can add these anytime from your profile settings.", fontSize = 11.sp, color = NMV2LinkGreen, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }

                        // ── Nominee Forms ──
                        if (!skipAddingNominee) {
                            nominees.forEachIndexed { index, nominee ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .border(1.dp, NMV2CardBorder, RoundedCornerShape(16.dp))
                                        .padding(18.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (nominees.size > 1) "NOMINEE ${index + 1}" else "NOMINEE DETAILS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.13.em,
                                                color = NMV2GoldAccent
                                            )
                                            if (index > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .background(NMV2RemoveBg, RoundedCornerShape(50))
                                                        .border(1.dp, NMV2RemoveBorder, RoundedCornerShape(50))
                                                        .clickable {
                                                            nominees = nominees.toMutableList().apply { removeAt(index) }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Filled.Close, contentDescription = "Remove nominee", tint = NMV2VolatilityRed, modifier = Modifier.size(13.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Name field
                                        val nameBringIntoViewRequester = remember { BringIntoViewRequester() }
                                        var isNameFocused by remember { mutableStateOf(false) }
                                        LaunchedEffect(isNameFocused) {
                                            if (isNameFocused) { delay(350); nameBringIntoViewRequester.bringIntoView() }
                                        }
                                        Text("Nominee Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NMV2BronzeMuted, modifier = Modifier.padding(bottom = 5.dp))
                                        OutlinedTextField(
                                            value = nominee.name,
                                            onValueChange = { newValue ->
                                                val filtered = newValue.filterEnglishName(uppercase = false)
                                                    .replaceFirstChar { c -> c.uppercaseChar() }
                                                nominees = nominees.toMutableList().apply {
                                                    this[index] = this[index].copy(name = filtered)
                                                }
                                            },
                                            placeholder = { Text("Full name as on Aadhaar/PAN", color = NMV2FieldBorder.copy(alpha = 0.6f), fontSize = 14.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bringIntoViewRequester(nameBringIntoViewRequester)
                                                .onFocusChanged { isNameFocused = it.isFocused },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Ascii,
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NMV2LinkGreen,
                                                unfocusedBorderColor = NMV2FieldBorder,
                                                focusedTextColor = NMV2BronzeInk,
                                                unfocusedTextColor = NMV2BronzeInk,
                                                cursorColor = NMV2LinkGreen
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // PAN field
                                        val panBringIntoViewRequester = remember { BringIntoViewRequester() }
                                        var isPanFocused by remember { mutableStateOf(false) }
                                        LaunchedEffect(isPanFocused) {
                                            if (isPanFocused) { delay(250); panBringIntoViewRequester.bringIntoView() }
                                        }
                                        val panValid = nominee.panNumber.length == 10 &&
                                            (nominee.panNumber.length < 4 || nominee.panNumber[3] == 'P')

                                        Text("PAN Number", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NMV2BronzeMuted, modifier = Modifier.padding(bottom = 5.dp))
                                        OutlinedTextField(
                                            value = nominee.panNumber,
                                            onValueChange = { newValue ->
                                                val filtered = newValue.filterEnglishPan()
                                                if (filtered.length <= 10) {
                                                    if (filtered.length >= 4 && filtered[3] != 'P') return@OutlinedTextField
                                                    nominees = nominees.toMutableList().apply {
                                                        this[index] = this[index].copy(panNumber = filtered)
                                                    }
                                                }
                                            },
                                            placeholder = { Text("ABCPD1234E", color = NMV2FieldBorder.copy(alpha = 0.6f), fontSize = 14.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bringIntoViewRequester(panBringIntoViewRequester)
                                                .onFocusChanged { isPanFocused = it.isFocused },
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Characters,
                                                keyboardType = when {
                                                    nominee.panNumber.length < 5 -> KeyboardType.Ascii
                                                    nominee.panNumber.length < 9 -> KeyboardType.Number
                                                    else -> KeyboardType.Ascii
                                                },
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NMV2LinkGreen,
                                                unfocusedBorderColor = NMV2FieldBorder,
                                                focusedTextColor = NMV2BronzeInk,
                                                unfocusedTextColor = NMV2BronzeInk,
                                                cursorColor = NMV2LinkGreen
                                            )
                                        )
                                        if (panValid) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = NMV2SuccessGreen, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Valid PAN format", fontSize = 11.sp, color = NMV2SuccessGreen)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Relationship chips
                                        Text("Nominee's relationship to you", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NMV2BronzeMuted, modifier = Modifier.padding(bottom = 8.dp))
                                        NMV2WrapChips(
                                            options = relationshipOptions,
                                            selected = nominee.relationship,
                                            displayMap = relationshipDisplay,
                                            onSelect = { newRelationship ->
                                                nominees = nominees.toMutableList().apply {
                                                    this[index] = this[index].copy(relationship = newRelationship)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Date of Birth
                                        Text("Nominee Date of Birth", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NMV2BronzeMuted, modifier = Modifier.padding(bottom = 5.dp))
                                        val dobInteractionSource = remember { MutableInteractionSource() }
                                        LaunchedEffect(dobInteractionSource) {
                                            dobInteractionSource.interactions.collect { interaction ->
                                                if (interaction is PressInteraction.Release) {
                                                    showNomineeDatePicker = index
                                                }
                                            }
                                        }
                                        OutlinedTextField(
                                            value = nominee.dateOfBirth,
                                            onValueChange = {},
                                            placeholder = { Text("YYYY-MM-DD", color = NMV2FieldBorder.copy(alpha = 0.6f), fontSize = 14.sp) },
                                            singleLine = true,
                                            readOnly = true,
                                            interactionSource = dobInteractionSource,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Pick date", tint = NMV2GoldAccent)
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NMV2LinkGreen,
                                                unfocusedBorderColor = NMV2FieldBorder,
                                                focusedTextColor = NMV2BronzeInk,
                                                unfocusedTextColor = NMV2BronzeInk
                                            )
                                        )
                                    }
                                }
                            }

                            // Add another nominee button
                            if (nominees.size < 3) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .border(1.5.dp, NMV2GoldAccent, RoundedCornerShape(12.dp))
                                        .clickable { nominees = nominees + NomineeInfo("", "", "", "") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Brush.linearGradient(listOf(NMV2GoldAccent, NMV2GoldDeep)), RoundedCornerShape(50)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = NMV2Obsidian, modifier = Modifier.size(11.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add another nominee", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NMV2GoldDeep)
                                    }
                                }
                            }
                        }

                        // Error display
                        if (nomineeSubmissionResult is Resource.Error) {
                            val submissionError = nomineeSubmissionResult as Resource.Error
                            val errorMsg = submissionError.message ?: ""
                            val isNetworkError = submissionError.isNetworkError ||
                                errorMsg.contains("Network", ignoreCase = true) ||
                                errorMsg.contains("timeout", ignoreCase = true) ||
                                errorMsg.contains("connection", ignoreCase = true) ||
                                errorMsg.contains("NETWORK_ERROR", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x14C62828), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (isNetworkError) "Check your Internet connection and try again" else "Something went wrong. Please try again.",
                                    fontSize = 12.sp,
                                    color = NMV2VolatilityRed
                                )
                            }
                        }

                        // Validation
                        val allNomineesValid = if (skipAddingNominee) {
                            true
                        } else {
                            nominees.all { nominee ->
                                val isPanValid = nominee.panNumber.length == 10 &&
                                    (nominee.panNumber.length < 4 || nominee.panNumber[3] == 'P')
                                nominee.name.isNotBlank() && nominee.relationship.isNotBlank() &&
                                    nominee.dateOfBirth.isNotBlank() && isPanValid
                            }
                        }

                        // ── CTA Button ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(NMV2GoldAccent, NMV2GoldDeep)), RoundedCornerShape(50))
                                .padding(1.5.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isSubmitting) return@Button
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    isSubmitting = true

                                    PlatformAnalyticsLogger.logEvent(
                                        "nominee_details_submit",
                                        mapOf(
                                            "has_nominee" to !skipAddingNominee,
                                            "nominee_count" to nominees.size,
                                            "screen_version" to "v4"
                                        )
                                    )

                                    if (!skipAddingNominee && nominees.isNotEmpty()) {
                                        val validNominees = nominees.filter { it.name.isNotBlank() }
                                        viewModel.submitNomineeDetailsV2(
                                            userId = effectiveUserId,
                                            kycAttemptId = effectiveKycAttemptId,
                                            investorId = effectiveInvestorId,
                                            wantsToAddNominee = validNominees.isNotEmpty(),
                                            nominees = validNominees.ifEmpty { emptyList() }
                                        )
                                    } else {
                                        viewModel.submitNomineeDetailsV2(
                                            userId = effectiveUserId,
                                            kycAttemptId = effectiveKycAttemptId,
                                            investorId = effectiveInvestorId,
                                            wantsToAddNominee = false,
                                            nominees = emptyList()
                                        )
                                    }
                                },
                                enabled = allNomineesValid && !isSubmitting && nomineeSubmissionResult !is Resource.Loading,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NMV2Obsidian,
                                    contentColor = NMV2Cream,
                                    disabledContainerColor = NMV2Obsidian,
                                    disabledContentColor = NMV2Cream
                                )
                            ) {
                                when {
                                    nomineeSubmissionResult is Resource.Loading<*> -> {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NMV2Cream, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Submitting…", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    else -> {
                                        Text(
                                            text = if (!skipAddingNominee) "Submit" else "Continue",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text("→", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NMV2GoldAccent)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }

                // Dim overlay when OTP sheet is open
                if (showOtpScreen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .zIndex(99f)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    )
                }
            }
        }

        // Loading overlay when submitting
        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                LoadingScreen(text = "Submitting…", modifier = Modifier.fillMaxSize())
            }
        }
    }

    // ── Date Picker ──
    showNomineeDatePicker?.let { nomineeIndex ->
        HierarchicalDatePicker(
            onDateSelected = { year, month, day ->
                val formattedDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                nominees = nominees.toMutableList().apply {
                    this[nomineeIndex] = this[nomineeIndex].copy(dateOfBirth = formattedDate)
                }
                showNomineeDatePicker = null
                nomineeDatePickerStep = 0
                nomineeSelectedYear = null
                nomineeSelectedMonth = null
                nomineeSelectedDay = null
            },
            onDismiss = {
                showNomineeDatePicker = null
                nomineeDatePickerStep = 0
                nomineeSelectedYear = null
                nomineeSelectedMonth = null
                nomineeSelectedDay = null
            },
            currentStep = nomineeDatePickerStep,
            selectedYear = nomineeSelectedYear,
            selectedMonth = nomineeSelectedMonth,
            selectedDay = nomineeSelectedDay,
            onStepChange = { nomineeDatePickerStep = it },
            onYearSelected = { nomineeSelectedYear = it },
            onMonthSelected = { nomineeSelectedMonth = it },
            onDaySelected = { nomineeSelectedDay = it }
        )
    }
}

// ── Wrap chips for relationship selection ──
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NMV2WrapChips(
    options: List<String>,
    selected: String,
    displayMap: Map<String, String>,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .background(if (isSelected) NMV2Obsidian else Color.White, RoundedCornerShape(50))
                    .border(1.5.dp, if (isSelected) NMV2GoldAccent else NMV2FieldBorder, RoundedCornerShape(50))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayMap[option] ?: option,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) NMV2Cream else NMV2BronzeMuted,
                    maxLines = 1
                )
            }
        }
    }
}

// ── OTP Verification Bottom Sheet (V2 style) ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NMV2OtpBottomSheet(
    phoneNumber: String,
    otpFieldValue: TextFieldValue,
    otpVerificationResult: Resource<String>?,
    otpGenerationResult: Resource<String>?,
    onOtpFieldValueChange: (TextFieldValue) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: NomineeDetailsViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var resendTimer by remember { mutableStateOf(0) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("OtpVerificationBottomSheetV4")
        keyboardController?.show()
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NMV2Cream)
            .padding(start = 24.dp, top = 5.dp, end = 24.dp, bottom = 15.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = NMV2BronzeMuted)
            }
        }

        Text("Enter OTP", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NMV2BronzeInk, textAlign = TextAlign.Center)
        Text("OTP sent to $phoneNumber", fontSize = 12.sp, color = NMV2BronzeMuted, textAlign = TextAlign.Center)

        OtpField(
            length = 6,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = otpVerificationResult !is Resource.Loading,
            isError = otpVerificationResult is Resource.Error,
            otpFieldValue = otpFieldValue,
            onOtpFieldValueChange = { onOtpFieldValueChange(it) },
            onOtpComplete = {}
        )

        Text(
            text = stringResource(Res.string.otp_consent_message),
            fontSize = 10.sp,
            color = NMV2BronzeMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = {
                if (canResend) {
                    PlatformAnalyticsLogger.logEvent("otp_resend_clicked", mapOf("screen_version" to "v4"))
                    canResend = false
                    onOtpFieldValueChange(TextFieldValue(""))
                    keyboardController?.show()
                    onResendOtp()
                }
            },
            enabled = canResend
        ) {
            Text(
                text = if (canResend) stringResource(Res.string.resend_otp) else stringResource(Res.string.resend_otp_in_seconds, resendTimer),
                fontSize = 12.sp,
                color = if (canResend) NMV2LinkGreen else NMV2BronzeMuted
            )
        }

        if (otpVerificationResult is Resource.Error) {
            val errorMessage = (otpVerificationResult as Resource.Error).message ?: ""
            val isNetworkError = otpVerificationResult.isNetworkError ||
                errorMessage.contains("NETWORK_ERROR", ignoreCase = true) ||
                errorMessage.contains("Network", ignoreCase = true) ||
                errorMessage.contains("timeout", ignoreCase = true) ||
                errorMessage.contains("connection", ignoreCase = true)

            if (isNetworkError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x14C62828), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(stringResource(Res.string.check_internet_connection), fontSize = 12.sp, color = NMV2VolatilityRed)
                }
            } else {
                Text(
                    text = "Incorrect OTP. Please try again.",
                    color = NMV2VolatilityRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (otpGenerationResult is Resource.Success) {
            Text("New OTP sent successfully!", color = NMV2SuccessGreen, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else if (otpGenerationResult is Resource.Error) {
            val errorMsg = (otpGenerationResult as Resource.Error).message ?: ""
            val isNetErr = otpGenerationResult.isNetworkError || errorMsg.contains("NETWORK_ERROR", ignoreCase = true)
            if (isNetErr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x14C62828), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(stringResource(Res.string.check_internet_connection), fontSize = 11.sp, color = NMV2VolatilityRed)
                }
            } else {
                Text("Failed to resend OTP. Please try again.", color = NMV2VolatilityRed, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        // Verify CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(NMV2GoldAccent, NMV2GoldDeep)), RoundedCornerShape(50))
                .padding(1.5.dp)
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    PlatformAnalyticsLogger.logEvent("otp_verify_clicked", mapOf("otp_length" to otpCode.length, "screen_version" to "v4"))
                    val currentOtp = otpFieldValue.text.take(6)
                    if (currentOtp.length == 6) onVerifyOtp(currentOtp)
                },
                enabled = otpFieldValue.text.length == 6 && otpVerificationResult !is Resource.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NMV2Obsidian,
                    contentColor = NMV2Cream,
                    disabledContainerColor = NMV2Obsidian,
                    disabledContentColor = NMV2Cream
                )
            ) {
                if (otpVerificationResult is Resource.Loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NMV2Cream, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying…", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Verify OTP", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// Helper to access otpCode length in the bottom sheet
private val TextFieldValue.length: Int get() = text.length
