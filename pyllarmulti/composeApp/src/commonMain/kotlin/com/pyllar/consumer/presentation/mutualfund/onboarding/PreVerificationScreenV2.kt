package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.filterEnglishPan
import com.pyllar.consumer.util.platformLog
import com.pyllar.otp.OtpField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*

/*
 * V2 Premium palette for this screen — cream/gold/obsidian, matching the
 * rest of the V2 onboarding screens (NameDobScreenV2, AdditionalKycScreenV2, etc.)
 */
private val V2Cream = Color(0xFFFBF9F4)
private val V2CreamWarm = Color(0xFFF5F0E8)
private val V2DarkBrown = Color(0xFF3E2723)
private val V2InkSoft = Color(0xFF6D4C41)
private val V2Gold = Color(0xFFD4AF37)
private val V2GoldDark = Color(0xFF8B6B25)
private val V2DarkGreen = Color(0xFF0A2415)
private val V2MediumGreen = Color(0xFF1A7A42)
private val V2LightGreen = Color(0xFF2E7D32)
private val V2SubtleBorder = Color(0xFFEFEBE9)
private val V2WarmGreyBorder = Color(0xFFD7CCC8)
private val V2MutedText = Color(0xFFB0A89A)
private val V2ErrorRed = Color(0xFFC62828)
private val V2SuccessBg = Color(0xFFF1F8E9)
private val V2SuccessBorder = Color(0xFFC5E1A5)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PreVerificationScreenV2(
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
    var panAutoFetched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("PreVerificationV4")
    }

    // Safety timeout: reset isSubmitting after 90 seconds
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90_000L)
            if (isSubmitting && uiState.verificationResult !is Resource.Success && uiState.verificationResult !is Resource.Error) {
                platformLog("PreVerificationScreenV2: ⚠️ Safety timeout: API took too long, resetting")
                isSubmitting = false
                timeoutState.triggerTimeout()
            }
        }
    }

    // Handle readiness check result
    LaunchedEffect(uiState.verificationResult, uiState.verificationStatus) {
        when (val result = uiState.verificationResult) {
            is Resource.Success -> {
                if (uiState.verificationStatus != VerificationStatus.IN_PROGRESS) {
                    isSubmitting = false
                }
                readinessCheckStarted = true
                PlatformAnalyticsLogger.logEvent("readiness_check_success", mapOf("pan_last4" to panNumber.takeLast(4)))
            }
            is Resource.Error -> {
                isSubmitting = false
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent(
                    "readiness_check_error",
                    mapOf("pan_last4" to panNumber.takeLast(4), "error" to (result.message ?: "unknown"))
                )
            }
            is Resource.Loading -> {}
            else -> {}
        }
    }

    // Handle server-driven navigation
    LaunchedEffect(uiState.nextScreen) {
        uiState.nextScreen?.let { screenName ->
            platformLog("PreVerificationScreenV2: 🚀 Navigating to: $screenName")
            PlatformAnalyticsLogger.logEvent(
                "server_navigation",
                mapOf("from_screen" to "pre_verification", "to_screen" to screenName)
            )
            PlatformAnalyticsLogger.logEvent(
                "pre_verification_navigated_to_next_screen",
                mapOf("to_screen" to screenName)
            )
            onNavigateToScreen(screenName)
        }
    }

    // OTP bottom sheet state
    var showOtpBottomSheet by remember { mutableStateOf(false) }
    var otpFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val otpCode = otpFieldValue.text
    var otpSheetPhoneDisplay by remember { mutableStateOf("") }
    var otpAttemptsRemaining by remember { mutableStateOf(3) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var userPhone by remember { mutableStateOf("") }
    var currentPrefillId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userPhone = sessionStore.getCurrentPhone()
    }

    // Handle PAN Fetch Result (Autofetch Consent Initiation)
    LaunchedEffect(uiState.panFetchResult) {
        val result = uiState.panFetchResult
        if (result is Resource.Success) {
            val data = result.data
            if (data != null) {
                when (data.status) {
                    "OTP_GENERATED" -> {
                        PlatformAnalyticsLogger.logEvent(
                            "find_my_pan_otp_required",
                            mapOf("prefill_id_present" to (data.prefillId != null))
                        )
                        currentPrefillId = data.prefillId?.toString()
                        otpSheetPhoneDisplay = if (userPhone.length >= 4) "******${userPhone.takeLast(4)}" else "your phone"
                        otpAttemptsRemaining = 3
                        showOtpBottomSheet = true
                    }
                    "ALREADY_VERIFIED" -> {
                        PlatformAnalyticsLogger.logEvent(
                            "find_my_pan_fetched",
                            mapOf("source" to "already_verified", "pan_filled" to (!data.panNumber.isNullOrBlank()))
                        )
                        if (!data.panNumber.isNullOrBlank()) {
                            panFieldValue = TextFieldValue(
                                text = data.panNumber,
                                selection = TextRange(data.panNumber.length)
                            )
                            fetchedPanName = data.fullName ?: ""
                            showManualEntryForm = true
                            autoFetchFailed = false
                            panAutoFetched = true
                            showOtpBottomSheet = false
                            viewModel.clearError()
                        }
                    }
                }
            }
            viewModel.clearPanFetchResult()
        } else if (result is Resource.Error) {
            PlatformAnalyticsLogger.logEvent(
                "find_my_pan_not_fetched",
                mapOf("error" to (result.message ?: "unknown"))
            )
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
                val panDetails = data.panDetails
                val personalDetails = data.personalDetails
                if (panDetails?.panNumber != null) {
                    // V2 behaviour: keep sheet open to show "PAN found!" confirmation card
                    PlatformAnalyticsLogger.logEvent(
                        "find_my_pan_otp_verified_pan_fetched",
                        mapOf("name_fetched" to (!personalDetails?.fullName.isNullOrBlank()))
                    )
                } else {
                    PlatformAnalyticsLogger.logEvent("find_my_pan_otp_verified_pan_not_fetched", emptyMap())
                    showOtpBottomSheet = false
                    autoFetchFailed = true
                    showManualEntryForm = true
                }
            }
        } else if (result is Resource.Error) {
            val errorMsg = result.message ?: ""
            val isNetworkError = result.isNetworkError ||
                errorMsg.contains("Network", ignoreCase = true) ||
                errorMsg.contains("timeout", ignoreCase = true) ||
                errorMsg.contains("connection", ignoreCase = true) ||
                errorMsg.contains("Failed to connect", ignoreCase = true) ||
                errorMsg.contains("IOException", ignoreCase = true)
            if (!isNetworkError) {
                otpAttemptsRemaining = (otpAttemptsRemaining - 1).coerceAtLeast(1)
            }
            PlatformAnalyticsLogger.logEvent(
                "find_my_pan_otp_verify_failed",
                mapOf("error" to (result.message ?: "unknown"))
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── App bar: "Pyllar Money" wordmark + Language + Help ──
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pyllar ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = V2DarkGreen,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Money",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = V2Gold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageLetterButton(textColor = V2MediumGreen)
                    TextButton(onClick = {
                        PlatformAnalyticsLogger.logEvent(
                            "pre_verification_help_clicked",
                            mapOf("screen" to "pre_verification", "button_location" to "top_right")
                        )
                        onNavigateToHelp()
                    }) {
                        Text(
                            text = stringResource(Res.string.help),
                            style = MaterialTheme.typography.labelLarge,
                            color = V2MediumGreen
                        )
                    }
                }
            }

            // ── Progress stepper ──
            Surface(
                color = V2Cream,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
            ) {
                OnboardingStepper(
                    currentStep = if (readinessCheckStarted) 1 else 0,
                    completedStep = if (readinessCheckStarted) 1 else 0,
                    currentScreenRoute = ScreenNames.PRE_VERIFICATION
                )
            }

            // ── Main scrollable content ──
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(V2Cream)
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 24.dp,
                    bottom = 32.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Main PAN card ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, V2SubtleBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title
                            Text(
                                text = stringResource(Res.string.pan_verification_title),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = V2DarkGreen,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Subtitle
                            Text(
                                text = stringResource(Res.string.pan_verification_subtitle),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = V2InkSoft,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // ── 1. Autofetch my PAN ──
                            PreVerificationV2GradientButton(
                                text = if (uiState.panFetchResult is Resource.Loading)
                                    stringResource(Res.string.pan_fetching)
                                else
                                    stringResource(Res.string.autofetch_my_pan),
                                onClick = {
                                    PlatformAnalyticsLogger.logEvent(
                                        "pre_verification_find_my_pan_clicked",
                                        mapOf("has_phone" to userPhone.isNotBlank())
                                    )
                                    scope.launch {
                                        val latestPhone = if (userPhone.isNotBlank()) userPhone else {
                                            val fetched = sessionStore.getCurrentPhone()
                                            if (fetched.isNotBlank()) userPhone = fetched
                                            fetched
                                        }
                                        if (latestPhone.isNotBlank()) {
                                            isPhoneMissing = false
                                            currentPrefillId = null
                                            otpFieldValue = TextFieldValue("")
                                            viewModel.clearPanVerifyOtpResult()
                                            viewModel.initiatePanFetch(latestPhone)
                                        } else {
                                            isPhoneMissing = true
                                            autoFetchFailed = true
                                            showManualEntryForm = true
                                        }
                                    }
                                },
                                enabled = !panAutoFetched && uiState.panFetchResult !is Resource.Loading,
                                loading = uiState.panFetchResult is Resource.Loading,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = stringResource(Res.string.dont_remember_pan_click_here),
                                style = MaterialTheme.typography.bodySmall,
                                color = V2MutedText,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            // ── OR divider ──
                            Text(
                                text = stringResource(Res.string.pan_or_divider),
                                style = MaterialTheme.typography.bodyMedium,
                                color = V2MutedText,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )

                            // ── 2. Enter PAN manually / Manual form ──
                            if (!showManualEntryForm) {
                                PreVerificationV2GradientButton(
                                    text = stringResource(Res.string.enter_pan_manually),
                                    onClick = {
                                        PlatformAnalyticsLogger.logEvent(
                                            "pre_verification_enter_pan_manually_clicked",
                                            emptyMap()
                                        )
                                        showManualEntryForm = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // Manual PAN entry form
                                val panBringIntoViewRequester = remember { BringIntoViewRequester() }
                                var isPanFocused by remember { mutableStateOf(false) }

                                LaunchedEffect(isPanFocused) {
                                    if (isPanFocused) {
                                        for (i in 1..8) {
                                            delay(120)
                                            lazyListState.animateScrollToItem(0, 20000)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Error if autofetch failed
                                    if (autoFetchFailed) {
                                        Text(
                                            text = if (isPhoneMissing)
                                                stringResource(Res.string.phone_number_not_found)
                                            else
                                                stringResource(Res.string.pan_autofetch_failed),
                                            color = V2ErrorRed,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        )
                                    }

                                    // PAN Number label
                                    Text(
                                        text = stringResource(Res.string.pan_number_label),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        ),
                                        color = V2DarkBrown,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    val panFieldBorderColor = when {
                                        fourthLetterError -> V2ErrorRed
                                        isPanValid -> V2LightGreen
                                        else -> V2WarmGreyBorder
                                    }

                                    OutlinedTextField(
                                        value = panFieldValue,
                                        onValueChange = { newValue ->
                                            val newText = newValue.text.filterEnglishPan()
                                            if (newText.length <= 10) {
                                                val selection = if (newText.length < newValue.text.length) {
                                                    TextRange(newText.length)
                                                } else {
                                                    newValue.selection
                                                }
                                                panFieldValue = newValue.copy(text = newText, selection = selection)
                                                panError = null
                                                autoFetchFailed = false
                                                isPhoneMissing = false
                                                viewModel.clearError()
                                                if (fetchedPanName != null) fetchedPanName = null
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                stringResource(Res.string.pan_hint),
                                                color = V2MutedText.copy(alpha = 0.5f)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = when (panNumber.length) {
                                            in 0..4, 9 -> KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
                                            in 5..8 -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                            else -> KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
                                        },
                                        isError = uiState.verificationResult is Resource.Error || fourthLetterError,
                                        trailingIcon = {
                                            if (isPanValid) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(V2LightGreen, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bringIntoViewRequester(panBringIntoViewRequester)
                                            .onFocusChanged { isPanFocused = it.isFocused },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = panFieldBorderColor,
                                            unfocusedBorderColor = panFieldBorderColor,
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        )
                                    )

                                    if (fourthLetterError) {
                                        Text(
                                            text = stringResource(Res.string.pan_invalid_fourth_character),
                                            color = V2ErrorRed,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    // Fetched PAN name confirmation chip
                                    fetchedPanName?.let { name ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(V2SuccessBg)
                                                .border(1.dp, V2SuccessBorder, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = V2LightGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = V2LightGreen
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Verify PAN button (gold-bordered dark-green)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (isPanValid) 1f else 0.5f)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(Brush.horizontalGradient(listOf(V2Gold, V2GoldDark)))
                                            .padding(1.5.dp)
                                    ) {
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
                                                PlatformAnalyticsLogger.logEvent(
                                                    "pre_verification_verify_pan_clicked",
                                                    mapOf(
                                                        "pan_source" to if (fetchedPanName != null) "find_my_pan" else "manual",
                                                        "pan_length" to panNumber.length
                                                    )
                                                )
                                                viewModel.checkInvestorReadiness(panNumber)
                                            },
                                            enabled = isPanValid && !isSubmitting && uiState.verificationResult !is Resource.Loading && uiState.verificationStatus != VerificationStatus.IN_PROGRESS,
                                            timeoutState = timeoutState,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = V2DarkGreen,
                                                contentColor = Color.White,
                                                disabledContainerColor = V2DarkGreen.copy(alpha = 0.7f),
                                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                                            ),
                                            shape = RoundedCornerShape(11.5.dp),
                                            modifier = Modifier.fillMaxWidth().height(49.dp)
                                        ) {
                                            when {
                                                isSubmitting || uiState.verificationResult is Resource.Loading || uiState.verificationStatus == VerificationStatus.IN_PROGRESS -> {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        stringResource(Res.string.readiness_checking),
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                else -> Text(
                                                    text = if (fetchedPanName != null)
                                                        stringResource(Res.string.verify)
                                                    else
                                                        stringResource(Res.string.verify_pan),
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Result messages
                                    when (val result = uiState.verificationResult) {
                                        is Resource.Success -> {
                                            Text(
                                                text = stringResource(Res.string.in_progress_ellipsis),
                                                color = V2MediumGreen,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            uiState.serverMessage?.let { message ->
                                                Text(
                                                    text = message,
                                                    color = V2InkSoft,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )
                                            }
                                        }
                                        is Resource.Error -> {
                                            val errorMsg = result.message ?: ""
                                            val isNetworkError = result.isNetworkError ||
                                                errorMsg.contains("Network", ignoreCase = true) ||
                                                errorMsg.contains("timeout", ignoreCase = true) ||
                                                errorMsg.contains("connection", ignoreCase = true) ||
                                                errorMsg.contains("Failed to connect", ignoreCase = true) ||
                                                errorMsg.contains("IOException", ignoreCase = true)
                                            Text(
                                                text = if (isNetworkError)
                                                    stringResource(Res.string.check_internet_connection)
                                                else
                                                    stringResource(Res.string.verification_failed_try_again),
                                                color = V2ErrorRed,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }

                    // ── Why PAN is needed info card ──
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(V2Cream, V2CreamWarm)))
                            .border(1.dp, V2SubtleBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(V2DarkGreen.copy(alpha = 0.07f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Help,
                                        contentDescription = null,
                                        tint = V2DarkGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.why_is_pan_needed),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = V2DarkGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(Res.string.pan_compliance_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = V2InkSoft
                            )
                        }
                    }

                    // ── Know more button ──
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent(
                                "pre_verification_know_more_clicked",
                                mapOf("screen" to "pre_verification", "button_location" to "bottom")
                            )
                            onNavigateToKycInfo()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = V2MediumGreen
                        ),
                        border = BorderStroke(width = 1.dp, color = V2WarmGreyBorder)
                    ) {
                        Text(
                            text = stringResource(Res.string.know_more),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // ── OTP Bottom Sheet ──
        if (showOtpBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    otpFieldValue = TextFieldValue("")
                    currentPrefillId = null
                    viewModel.clearPanVerifyOtpResult()
                    showOtpBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = V2Cream
            ) {
                PreVerificationV2OtpSheet(
                    phoneNumber = otpSheetPhoneDisplay.ifEmpty { "your phone" },
                    otpFieldValue = otpFieldValue,
                    otpVerificationResult = uiState.panVerifyOtpResult,
                    attemptsRemaining = otpAttemptsRemaining,
                    onOtpFieldValueChange = { otpFieldValue = it },
                    onVerifyOtp = { otpValue ->
                        PlatformAnalyticsLogger.logEvent(
                            "find_my_pan_otp_verify_clicked",
                            mapOf("otp_length" to otpValue.length)
                        )
                        val prefillIdStr = currentPrefillId
                        if (prefillIdStr != null && userPhone.isNotBlank()) {
                            viewModel.verifyOtpAndFetchPan(
                                mobileNumber = userPhone,
                                prefillId = prefillIdStr.toLong(),
                                otp = otpValue
                            )
                        }
                    },
                    onDismiss = {
                        otpFieldValue = TextFieldValue("")
                        currentPrefillId = null
                        viewModel.clearPanVerifyOtpResult()
                        showOtpBottomSheet = false
                    },
                    onResendOtp = {
                        if (userPhone.isNotBlank()) {
                            currentPrefillId = null
                            otpAttemptsRemaining = 3
                            otpFieldValue = TextFieldValue("")
                            viewModel.clearPanVerifyOtpResult()
                            viewModel.initiatePanFetch(userPhone, force = true)
                        }
                    },
                    onContinueWithPan = {
                        val data = (uiState.panVerifyOtpResult as? Resource.Success)?.data
                        val panDetails = data?.panDetails
                        val personalDetails = data?.personalDetails
                        if (panDetails?.panNumber != null) {
                            panFieldValue = TextFieldValue(
                                text = panDetails.panNumber,
                                selection = TextRange(panDetails.panNumber.length)
                            )
                            fetchedPanName = personalDetails?.fullName ?: ""
                            showManualEntryForm = true
                            autoFetchFailed = false
                            panAutoFetched = true
                            viewModel.clearError()
                        }
                        otpFieldValue = TextFieldValue("")
                        currentPrefillId = null
                        viewModel.clearPanVerifyOtpResult()
                        showOtpBottomSheet = false
                    },
                    onEnterPanManually = {
                        otpFieldValue = TextFieldValue("")
                        currentPrefillId = null
                        viewModel.clearPanVerifyOtpResult()
                        showOtpBottomSheet = false
                        showManualEntryForm = true
                        autoFetchFailed = false
                    }
                )
            }
        }

        // ── Loading overlays ──
        val showPanFetchOverlay = uiState.panFetchResult is Resource.Loading
        if (showPanFetchOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                LoadingScreen(
                    text = stringResource(Res.string.fetching_pan_securely),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val showSubmitOverlay = (isSubmitting && uiState.verificationStatus != VerificationStatus.IN_PROGRESS) ||
            (uiState.verificationResult is Resource.Loading && uiState.verificationStatus != VerificationStatus.IN_PROGRESS)
        if (showSubmitOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                LoadingScreen(
                    text = stringResource(Res.string.submitting_please_wait),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Gold-gradient-border CTA button used across the PreVerificationScreenV2 flow.
 * Mirrors the pattern from PermissionV2Screen and Android PreVerificationScreenV2.
 */
@Composable
private fun PreVerificationV2GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.horizontalGradient(listOf(V2Gold, V2GoldDark)))
            .padding(1.5.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = V2DarkGreen,
                contentColor = Color.White,
                disabledContainerColor = V2DarkGreen.copy(alpha = 0.7f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(11.5.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth().height(49.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * OTP Verification Bottom Sheet for PreVerificationScreenV2 (Autofetch my PAN).
 * V2 premium restyle with:
 * - Explicit "PAN found!" confirmation before closing
 * - Client-side decrementing attempts counter
 * - Resend OTP with 30-second countdown
 * - "Enter PAN manually instead" fallback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreVerificationV2OtpSheet(
    phoneNumber: String,
    otpFieldValue: TextFieldValue,
    otpVerificationResult: Resource<*>?,
    attemptsRemaining: Int,
    onOtpFieldValueChange: (TextFieldValue) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onDismiss: () -> Unit,
    onResendOtp: () -> Unit = {},
    onContinueWithPan: () -> Unit = {},
    onEnterPanManually: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val otpCode = otpFieldValue.text

    var resendTimer by remember { mutableStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    // Extract success PAN data
    val successData = (otpVerificationResult as? Resource.Success<*>)?.data
    val successPanNumber = (successData as? com.pyllar.consumer.data.remote.dto.PanVerifyOtpDataDto)
        ?.takeIf { it.status == "SUCCESS" }?.panDetails?.panNumber
    val successPanName = (successData as? com.pyllar.consumer.data.remote.dto.PanVerifyOtpDataDto)
        ?.personalDetails?.fullName
    val isSuccess = successPanNumber != null

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("PreVerificationOtpBottomSheetV4")
        keyboardController?.show()
    }

    LaunchedEffect(canResend) {
        if (!canResend) {
            resendTimer = 30
            while (resendTimer > 0) {
                delay(1000L)
                resendTimer--
            }
            canResend = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V2Cream)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, top = 8.dp, end = 22.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.verify_to_find_pan),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = V2DarkGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.otp_sent_to_phone, phoneNumber),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = V2InkSoft
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.content_description_close),
                    tint = V2MutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // OTP field
        OtpField(
            length = 6,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            otpFieldValue = otpFieldValue,
            onOtpFieldValueChange = { newValue -> onOtpFieldValueChange(newValue) },
            onOtpComplete = {
                // No auto-submit — user must tap verify button
            },
            autoFocus = true
        )

        // Error message
        if (otpVerificationResult is Resource.Error) {
            val errorMessage = (otpVerificationResult as Resource.Error<*>).message ?: ""
            val isNetworkError = (otpVerificationResult as Resource.Error<*>).isNetworkError ||
                errorMessage.contains("NETWORK_ERROR", ignoreCase = true) ||
                errorMessage.contains("Network", ignoreCase = true) ||
                errorMessage.contains("timeout", ignoreCase = true) ||
                errorMessage.contains("connection", ignoreCase = true) ||
                errorMessage.contains("Failed to connect", ignoreCase = true) ||
                errorMessage.contains("IOException", ignoreCase = true)
            Spacer(modifier = Modifier.height(10.dp))
            if (isNetworkError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = stringResource(Res.string.check_internet_connection),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.incorrect_otp_attempts_remaining, attemptsRemaining),
                    color = V2ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Success: PAN found card
        if (isSuccess) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(V2SuccessBg)
                    .border(1.dp, V2SuccessBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(V2LightGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.pan_found_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = V2LightGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = successPanNumber ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = V2DarkBrown
                        )
                        if (!successPanName.isNullOrBlank()) {
                            Text(
                                text = successPanName,
                                style = MaterialTheme.typography.bodySmall,
                                color = V2InkSoft
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(V2LightGreen)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.pan_verified_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Resend row (only shown when not yet succeeded)
        if (!isSuccess) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canResend) {
                    Text(
                        text = stringResource(Res.string.resend_otp_prompt),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = V2MutedText
                    )
                    TextButton(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent("otp_resend_clicked", emptyMap())
                            canResend = false
                            onOtpFieldValueChange(TextFieldValue(""))
                            keyboardController?.show()
                            onResendOtp()
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.resend_otp),
                            color = V2MediumGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.resend_otp_in_seconds, resendTimer),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = V2MutedText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary CTA
        if (isSuccess) {
            PreVerificationV2GradientButton(
                text = stringResource(Res.string.continue_with_this_pan),
                onClick = onContinueWithPan,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (otpCode.length == 6) 1f else 0.45f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.horizontalGradient(listOf(V2Gold, V2GoldDark)))
                    .padding(1.5.dp)
            ) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        PlatformAnalyticsLogger.logEvent("otp_verify_clicked", mapOf("otp_length" to otpCode.length))
                        val currentOtp = otpCode.take(6)
                        if (currentOtp.length == 6) onVerifyOtp(currentOtp)
                    },
                    enabled = otpCode.length == 6 && otpVerificationResult !is Resource.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = V2DarkGreen,
                        contentColor = Color.White,
                        disabledContainerColor = V2DarkGreen,
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(11.5.dp),
                    modifier = Modifier.fillMaxWidth().height(49.dp)
                ) {
                    if (otpVerificationResult is Resource.Loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.verifying_dots))
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.verify_otp_label),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "Enter PAN manually instead" fallback
        OutlinedButton(
            onClick = onEnterPanManually,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = V2InkSoft
            ),
            border = BorderStroke(1.dp, V2WarmGreyBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.enter_pan_manually_instead),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
