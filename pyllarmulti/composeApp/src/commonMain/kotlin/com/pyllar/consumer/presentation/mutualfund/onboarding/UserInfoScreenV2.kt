package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

private val V2Cream = Color(0xFFFBF9F4)
private val V2CreamTint = Color(0xFFF5EEDB)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2GoldDeep = Color(0xFF8B6B25)
private val V2GoldAccent = Color(0xFFD4AF37)
private val V2Obsidian = Color(0xFF0A2415)
private val V2SuccessGreen = Color(0xFF2E7D32)
private val V2ErrorRed = Color(0xFFC62828)
private val V2BorderGold28 = Color(0x478B6B25)
private val V2BorderGold22 = Color(0x388B6B25)
private val V2BorderGold20 = Color(0x338B6B25)
private val V2WarmGreyBorder = Color(0xFFD7CCC8)
private val V2MutedText = Color(0xFFB0A89A)
private val V2BadgeGreen10 = Color(0x1A2E7D32)
private val V2ScrimOverlay = Color(0x7A1E120C)
private val V2VerifyOverlay = Color(0xE0FBF9F4)
private val V2InfoBg = Color(0x12D4AF37)
private val V2InfoBorder = Color(0x478B6B25)
private val V2PrivacyBg = Color(0x0A0A2415)
private val V2PrivacyBorder = Color(0x120A2415)

private fun isValidEmail(value: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
    if (value.isBlank() || !emailRegex.matches(value)) return false
    val lastDot = value.lastIndexOf('.')
    if (lastDot == -1 || lastDot >= value.length - 2) return false
    val tld = value.substring(lastDot + 1)
    return tld.length in 2..6 && tld.all { it.isLetter() }
}

private fun formatDobDisplay(dobIso: String): String {
    return try {
        val parts = dobIso.split("-")
        if (parts.size != 3) return ""
        val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val day = parts[2].toInt()
        val month = months[parts[1].toInt() - 1]
        "$day $month ${parts[0]}"
    } catch (e: Exception) {
        ""
    }
}

// ── Date utilities (KMP-safe) ─────────────────────────
private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun getDaysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreenV2(
    onKycSubmitted: (String, String, String, NavigationInfo?, Any?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToUpiFetch: () -> Unit = {},
    onNavigateToPanFetch: () -> Unit = {},
    viewModel: UserInfoViewModel = koinInject(),
    userId: String,
    email: String,
    phone: String,
    token: String,
    sessionStore: SessionStore = koinInject(),
    showUpiOverride: Boolean = false,
    showPanOverride: Boolean = false,
    showBottomSheetPreview: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val timeoutState = rememberTimeoutState("UserInfoV2", "continue")

    val nameEmptyError = stringResource(Res.string.user_info_name_empty_error)
    val dobEmptyError = stringResource(Res.string.user_info_dob_empty_error)
    val emailEmptyError = stringResource(Res.string.user_info_email_empty_error)
    val panEmptyError = stringResource(Res.string.user_info_pan_empty_error)
    val panIncompleteError = stringResource(Res.string.user_info_pan_incomplete_error)
    val panInvalidFourthCharacterError = stringResource(Res.string.pan_invalid_fourth_character)
    val verificationFailedTryAgain = stringResource(Res.string.verification_failed_try_again)
    val checkInternetConnection = stringResource(Res.string.check_internet_connection)

    var name by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

    var namePrefilled by remember { mutableStateOf(false) }
    var panPrefilled by remember { mutableStateOf(false) }

    var detectedEmail by remember { mutableStateOf(email) }
    var emailMode by remember { mutableStateOf(if (email.isNotBlank()) "chip" else "manual") }
    var manualEmail by remember { mutableStateOf("") }
    var confirmedEmail by remember { mutableStateOf("") }
    val nameFocusRequester = remember { FocusRequester() }
    val panFocusRequester = remember { FocusRequester() }
    val dobFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    var isNameFocused by remember { mutableStateOf(false) }
    var isPanFocused by remember { mutableStateOf(false) }
    var isDobFocused by remember { mutableStateOf(false) }
    var isEmailFocused by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var panError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var genericError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerStep by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    var showUpi by remember { mutableStateOf(showUpiOverride) }
    var showPan by remember { mutableStateOf(showPanOverride) }
    var showBottomSheetInPreview by remember { mutableStateOf(false) }

    // Observe prefilled data from SessionStore
    LaunchedEffect(Unit) {
        while (true) {
            val pName = sessionStore.getValue("prefilledName") ?: ""
            val pDob = sessionStore.getValue("prefilledDob") ?: ""
            val pPan = sessionStore.getValue("prefilledPan") ?: ""
            if (pName.isNotBlank()) {
                name = pName.filterEnglishName().uppercase()
                namePrefilled = true
                nameError = null
                sessionStore.saveValue("prefilledName", "")
            }
            if (pDob.isNotBlank()) {
                dob = pDob
                dobError = null
                try {
                    val parts = pDob.split("-")
                    if (parts.size == 3) {
                        selectedYear = parts[0].toIntOrNull()
                        selectedMonth = parts[1].toIntOrNull()
                    }
                } catch (e: Exception) {}
                sessionStore.saveValue("prefilledDob", "")
            }
            if (pPan.isNotBlank()) {
                pan = pPan.filterEnglishPan().uppercase()
                panPrefilled = true
                panError = null
                sessionStore.saveValue("prefilledPan", "")
            }
            delay(500)
        }
    }

    LaunchedEffect(isNameFocused) {
        if (isNameFocused) {
            for (i in 1..8) {
                delay(120)
                scrollState.animateScrollTo((scrollState.maxValue * 0.2f).toInt())
            }
        }
    }

    LaunchedEffect(isPanFocused) {
        if (isPanFocused) {
            for (i in 1..8) {
                delay(120)
                scrollState.animateScrollTo(scrollState.maxValue / 2)
            }
        }
    }

    LaunchedEffect(isDobFocused) {
        if (isDobFocused) {
            for (i in 1..8) {
                delay(120)
                scrollState.animateScrollTo((scrollState.maxValue * 0.75f).toInt())
            }
        }
    }

    LaunchedEffect(isEmailFocused) {
        if (isEmailFocused) {
            for (i in 1..8) {
                delay(120)
                scrollState.animateScrollTo((scrollState.maxValue * 0.8f).toInt())
            }
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("UserInfoV2")
    }

    LaunchedEffect(Unit) {
        val storedPanHolderName = sessionStore.getValue(KeyValueConstants.PAN_HOLDER_NAME) ?: ""
        if (storedPanHolderName.isNotBlank() && name.isBlank()) {
            name = storedPanHolderName.filterEnglishName().uppercase()
            namePrefilled = true
        }
    }

    val prefillData by viewModel.prefillData.collectAsState()
    LaunchedEffect(prefillData) {
        if (!showUpiOverride) {
            (prefillData["showUpi"] as? String)?.toBooleanStrictOrNull()?.let { showUpi = it }
        }
        if (!showPanOverride) {
            (prefillData["showFindPan"] as? String)?.toBooleanStrictOrNull()?.let { showPan = it }
        }
        val prepopulatedName = prefillData["name"] as? String
        if (!prepopulatedName.isNullOrBlank() && name.isBlank()) {
            name = prepopulatedName.filterEnglishName().uppercase()
            namePrefilled = true
        }
        val prepopulatedPan = prefillData["pan"] as? String
        if (!prepopulatedPan.isNullOrBlank() && pan.isBlank()) {
            pan = prepopulatedPan.filterEnglishPan().uppercase()
            panPrefilled = true
        }
        val prepopulatedDob = prefillData["dob"] as? String
        if (!prepopulatedDob.isNullOrBlank() && dob.isBlank()) {
            dob = prepopulatedDob
            try {
                val parts = dob.split("-")
                if (parts.size == 3) {
                    selectedYear = parts[0].toIntOrNull()
                    selectedMonth = parts[1].toIntOrNull()
                }
            } catch (e: Exception) { /* ignore parse error */ }
        }
        val prepopulatedEmail = prefillData["email"] as? String
        if (!prepopulatedEmail.isNullOrBlank() && detectedEmail.isBlank()) {
            detectedEmail = prepopulatedEmail
            emailMode = "chip"
        }
    }

    var hasSubmitted by remember { mutableStateOf(false) }
    val submitState by viewModel.submitState.collectAsState()
    val isBusy = hasSubmitted ||
        submitState is UserInfoViewModel.SubmitState.CheckingPan ||
        submitState is UserInfoViewModel.SubmitState.SubmittingDetails

    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is UserInfoViewModel.SubmitState.Success -> {
                sessionStore.saveValue(KeyValueConstants.PAN, pan)
                PlatformAnalyticsLogger.logEvent("user_info_submit_success", mapOf("pan_last4" to pan.takeLast(4)))
                onKycSubmitted(name, dob, confirmedEmail, state.navigation, state.data)
            }
            is UserInfoViewModel.SubmitState.Failed -> {
                hasSubmitted = false
                timeoutState.triggerTimeout()
                nameError = null
                panError = null
                dobError = null
                emailError = null
                genericError = null

                var hasSpecificError = false
                var firstErrorField: String? = null
                state.fieldErrors?.forEach { fieldError ->
                    when {
                        fieldError.field.equals("name", ignoreCase = true) -> {
                            nameError = fieldError.message; hasSpecificError = true
                            if (firstErrorField == null) firstErrorField = "name"
                        }
                        fieldError.field.equals("dateOfBirth", ignoreCase = true) ||
                            fieldError.field.equals("dob", ignoreCase = true) -> {
                            dobError = fieldError.message; hasSpecificError = true
                            if (firstErrorField == null) firstErrorField = "dob"
                        }
                        fieldError.field.equals("panNumber", ignoreCase = true) ||
                            fieldError.field.equals("pan", ignoreCase = true) -> {
                            panError = fieldError.message; hasSpecificError = true
                            if (firstErrorField == null) firstErrorField = "pan"
                        }
                        fieldError.field.equals("emailAddress", ignoreCase = true) -> {
                            emailError = fieldError.message; hasSpecificError = true
                            if (firstErrorField == null) firstErrorField = "email"
                        }
                    }
                }
                if (hasSpecificError) {
                    when (firstErrorField) {
                        "name" -> nameFocusRequester.requestFocus()
                        "pan" -> panFocusRequester.requestFocus()
                        "dob" -> dobFocusRequester.requestFocus()
                        "email" -> emailFocusRequester.requestFocus()
                    }
                }
                if (!hasSpecificError) {
                    val rawMsg = state.message ?: verificationFailedTryAgain
                    val friendlyMsg = if (rawMsg.contains("HTTP", ignoreCase = true)) {
                        checkInternetConnection
                    } else {
                        rawMsg
                    }
                    if (state.stage == UserInfoViewModel.Stage.PAN) {
                        panError = friendlyMsg
                    } else {
                        genericError = friendlyMsg
                    }
                }
                PlatformAnalyticsLogger.logEvent(
                    "user_info_submit_error",
                    mapOf("stage" to state.stage.name, "pan_last4" to pan.takeLast(4))
                )
            }
            else -> {}
        }
    }

    val isPanLengthValid = pan.length == 10
    val isFourthLetterValid = pan.length >= 4 && pan[3] == 'P'
    val panValid = isPanLengthValid && isFourthLetterValid
    val panFourthError = pan.length >= 4 && !isFourthLetterValid
    val nameValid = name.trim().length >= 2
    val dobValid = dob.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    fun effectiveEmail(): String = if (emailMode == "chip") detectedEmail else manualEmail
    val emailValid = isValidEmail(effectiveEmail())
    val canSubmit = !isBusy
    val hasPrefilled = namePrefilled || panPrefilled

    fun submit() {
        if (isBusy) return
        keyboardController?.hide()
        focusManager.clearFocus()
        nameError = null
        panError = null
        dobError = null
        emailError = null
        genericError = null

        if (!nameValid) {
            nameError = nameEmptyError
            nameFocusRequester.requestFocus()
            return
        }
        if (pan.isBlank()) {
            panError = panEmptyError
            panFocusRequester.requestFocus()
            return
        }
        if (pan.length < 10) {
            if (pan.length >= 4 && pan[3] != 'P') {
                panError = panInvalidFourthCharacterError
            } else {
                panError = panIncompleteError
            }
            panFocusRequester.requestFocus()
            return
        }
        if (!panValid) {
            panError = panInvalidFourthCharacterError
            panFocusRequester.requestFocus()
            return
        }
        if (!dobValid) {
            dobError = dobEmptyError
            dobFocusRequester.requestFocus()
            return
        }
        if (!emailValid) {
            emailError = emailEmptyError
            emailFocusRequester.requestFocus()
            return
        }

        PlatformAnalyticsLogger.logEvent("user_info_submit_attempt", mapOf("pan_last4" to pan.takeLast(4)))

        hasSubmitted = true
        scope.launch {
            val sessionUserId = sessionStore.getCurrentUserId()
            val sessionPhone = sessionStore.getCurrentPhone()

            val finalUserId = sessionUserId.ifBlank { userId }
            val finalEmail = effectiveEmail().ifBlank { email }
            val finalPhone = sessionPhone.takeLast(10).ifBlank { phone.takeLast(10) }

            sessionStore.saveUserSession(userId = finalUserId, email = finalEmail)
            confirmedEmail = finalEmail

            viewModel.submit(
                userId = finalUserId,
                name = name,
                pan = pan,
                dob = dob,
                emailAddress = finalEmail,
                mobileCountryCode = "+91",
                mobileNumber = finalPhone,
                token = token
            )
        }
    }

    val isLoadingPrefill by viewModel.isLoadingPrefill.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(V2Cream)) {
        if (isLoadingPrefill) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(V2Cream)
                    .zIndex(15f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = V2GoldAccent)
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar: wordmark + language + help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Pyllar ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = V2Obsidian, letterSpacing = (-0.5).sp)
                    Text(text = "Money", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = V2GoldAccent, letterSpacing = (-0.5).sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageLetterButton(textColor = V2SuccessGreen)
                    TextButton(onClick = onNavigateToHelp) {
                        Text(text = stringResource(Res.string.help), style = MaterialTheme.typography.labelLarge, color = V2SuccessGreen)
                    }
                }
            }

            Surface(color = V2Cream, shadowElevation = 8.dp, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)) {
                OnboardingStepper(currentStep = 0, completedStep = 0, currentScreenRoute = ScreenNames.PRE_VERIFICATION)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Skip form card (UPI promo)
                if (showUpi) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(V2InfoBg)
                            .border(1.dp, V2InfoBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                onNavigateToUpiFetch()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(V2Obsidian),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡",
                                    fontSize = 16.sp,
                                    color = V2GoldAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val fullText = stringResource(Res.string.upi_promo_skip_entire_form)
                                val parts = remember(fullText) { fullText.split("?") }
                                val titleText = parts.getOrNull(0)?.let { "$it?" } ?: "Have your UPI ID?"
                                val subtitleText = parts.getOrNull(1)?.trim() ?: "Skip this form entirely"
                                
                                Text(
                                    text = titleText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = V2BronzeInk
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitleText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = V2GoldDeep
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(V2Obsidian)
                                .clickable {
                                    onNavigateToUpiFetch()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.upi_promo_try_it_btn),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = V2GoldAccent
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(V2WarmGreyBorder.copy(alpha = 0.5f))
                        )
                        Text(
                            text = "OR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = V2MutedText,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(V2WarmGreyBorder.copy(alpha = 0.5f))
                        )
                    }
                }

                if (hasPrefilled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(V2InfoBg, RoundedCornerShape(12.dp))
                            .border(1.dp, V2InfoBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ℹ️", fontSize = 15.sp)
                        Column {
                            Text(
                                text = "Personal Details Prefilled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = V2GoldDeep
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "We've fetched your name and PAN. Please verify or update if needed.",
                                fontSize = 12.sp,
                                color = V2BronzeMuted,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                if (genericError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, V2ErrorRed, RoundedCornerShape(8.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Error", tint = V2ErrorRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = genericError!!, color = V2ErrorRed, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Personal Details",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = V2Obsidian
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = if (showPan) "Start with your name — we'll try to find your PAN automatically." else "Confirm details to setup your investment account",
                            fontSize = 13.sp,
                            color = V2BronzeMuted,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Divider(color = V2BorderGold20, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // ── Full name ──
                        FieldLabelRow(
                            label = if (showPan) "FULL NAME AS PER PAN" else "Full Name",
                            showPrefilledChip = namePrefilled
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newValue ->
                                name = newValue.filterEnglishName().uppercase()
                                namePrefilled = false
                                nameError = null
                            },
                            placeholder = {
                                Text(
                                    text = if (showPan) "e.g. RAHUL KUMAR SHARMA" else stringResource(Res.string.user_info_name_placeholder),
                                    color = V2MutedText
                                )
                            },
                            singleLine = true,
                            isError = nameError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(nameFocusRequester)
                                .onFocusChanged { isNameFocused = it.isFocused },
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(isError = nameError != null, isPrefilled = namePrefilled, isValid = nameValid)
                        )
                        FieldFootnote(
                            error = nameError,
                            hint = if (showPan) {
                                "Enter your Full name exactly as on your PAN card"
                            } else if (namePrefilled) {
                                "Pre-filled from verification records"
                            } else {
                                "As per your official documents"
                            }
                        )

                        if (showPan) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val lettersCount = name.filter { it.isLetter() }.length
                                    if (lettersCount < 4) {
                                        nameError = "Enter your Full name exactly as on your PAN card"
                                    } else {
                                        scope.launch {
                                            sessionStore.saveValue(KeyValueConstants.PAN_HOLDER_NAME, name)
                                            onNavigateToPanFetch()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFDFBF7),
                                    contentColor = V2GoldDeep
                                ),
                                border = BorderStroke(1.dp, V2GoldDeep),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = V2GoldDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Find my PAN",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = V2GoldDeep
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "→",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = V2GoldDeep
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "We'll match your name against your verified mobile number",
                                fontSize = 12.sp,
                                color = V2BronzeMuted,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Divider(color = V2BorderGold20, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // ── PAN number ──
                        FieldLabelRow(label = "PAN Number", showVerifiedChip = panPrefilled)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pan,
                            onValueChange = { newValue ->
                                val raw = newValue.filterEnglishPan().uppercase()
                                val filtered = buildString {
                                    raw.take(10).forEachIndexed { index, c ->
                                        when {
                                            index < 5 && c.isLetter() -> append(c)
                                            index in 5..8 && c.isDigit() -> append(c)
                                            index == 9 && c.isLetter() -> append(c)
                                        }
                                    }
                                }
                                pan = filtered
                                panPrefilled = false
                                panError = null
                            },
                            placeholder = { Text("e.g. ABCDE1234F", color = V2MutedText) },
                            singleLine = true,
                            isError = panError != null || panFourthError,
                            keyboardOptions = when (pan.length) {
                                in 0..4, 9 -> KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
                                in 5..8 -> KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                else -> KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
                            },
                            trailingIcon = {
                                if (panValid) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(V2SuccessGreen, CircleShape),
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
                                .focusRequester(panFocusRequester)
                                .onFocusChanged { isPanFocused = it.isFocused },
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(isError = panError != null || panFourthError, isPrefilled = panPrefilled, isValid = panValid)
                        )
                        FieldFootnote(
                            error = panError ?: if (panFourthError) stringResource(Res.string.pan_invalid_fourth_character) else null,
                            hint = if (panPrefilled) "PAN verified" else "10-digit Permanent Account Number"
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        Divider(color = V2BorderGold20, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // ── Date of birth ──
                        FieldLabelRow(label = "Date of Birth")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .border(1.5.dp, if (dobError != null) V2ErrorRed else if (dobValid) V2SuccessGreen else V2WarmGreyBorder, RoundedCornerShape(12.dp))
                                .focusRequester(dobFocusRequester)
                                .focusable()
                                .onFocusChanged { isDobFocused = it.isFocused }
                                .clickable { focusManager.clearFocus(); showDatePicker = true; datePickerStep = 0 }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (dobValid) formatDobDisplay(dob) else stringResource(Res.string.user_info_dob_placeholder),
                                fontSize = 15.sp,
                                fontWeight = if (dobValid) FontWeight.Medium else FontWeight.Normal,
                                color = if (dobValid) V2BronzeInk else V2MutedText
                            )
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick date", tint = V2MutedText, modifier = Modifier.size(18.dp))
                        }
                        FieldFootnote(error = dobError, hint = "You must be 18 or older to invest")

                        Spacer(modifier = Modifier.height(18.dp))
                        Divider(color = V2BorderGold20, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // ── Email ──
                        FieldLabelRow(label = "Email Address", showPrefilledChip = emailMode == "chip" && detectedEmail.isNotBlank())
                        Spacer(modifier = Modifier.height(8.dp))
                        if (emailMode == "chip") {
                            OutlinedTextField(
                                value = detectedEmail,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = V2GoldAccent,
                                    disabledContainerColor = V2CreamTint,
                                    disabledTextColor = V2BronzeInk
                                )
                            )
                            Text(
                                text = "Type different email address",
                                fontSize = 12.sp,
                                color = V2SuccessGreen,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clickable {
                                        emailMode = "manual"
                                        manualEmail = ""
                                        emailError = null
                                    }
                            )
                        } else {
                            OutlinedTextField(
                                value = manualEmail,
                                onValueChange = { manualEmail = it; emailError = null },
                                placeholder = { Text("e.g. name@email.com", color = V2MutedText) },
                                singleLine = true,
                                isError = emailError != null,
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(20.dp)
                                                .background(V2WarmGreyBorder)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        GoogleAccountPickerButton(
                                            onEmailPicked = { 
                                                manualEmail = it
                                                emailError = null
                                            }
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(emailFocusRequester)
                                    .onFocusChanged { isEmailFocused = it.isFocused },
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors(isError = emailError != null, isPrefilled = false, isValid = isValidEmail(manualEmail))
                            )
                            if (detectedEmail.isNotBlank()) {
                                Text(
                                    text = "Use detected account instead",
                                    fontSize = 12.sp,
                                    color = V2SuccessGreen,
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clickable { emailMode = "chip"; emailError = null }
                                )
                            }
                        }
                        FieldFootnote(error = emailError, hint = null)
                    }
                }

                // Privacy note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(V2PrivacyBg, RoundedCornerShape(10.dp))
                        .border(1.dp, V2PrivacyBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(stringResource(Res.string.email_usage_note), fontSize = 11.sp, color = V2BronzeMuted, lineHeight = 17.sp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (canSubmit) 1f else 0.52f)
                        .background(Brush.horizontalGradient(listOf(V2GoldAccent, V2GoldDeep)), RoundedCornerShape(14.dp))
                        .padding(2.dp)
                ) {
                    TimeoutButton(
                        onClick = { submit() },
                        enabled = canSubmit,
                        timeoutState = timeoutState,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = V2Obsidian,
                            contentColor = Color.White,
                            disabledContainerColor = V2Obsidian,
                            disabledContentColor = Color.White
                        ),
                        shape = RoundedCornerShape(11.dp)
                    ) {
                        Text(
                            text = if (hasPrefilled) stringResource(Res.string.user_info_btn_confirm_continue) else stringResource(Res.string.btn_continue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Date picker bottom sheet ──
        AnimatedVisibility(
            visible = showDatePicker,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(V2ScrimOverlay)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        showDatePicker = false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = showDatePicker,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180))
                ) {
                    UserInfoDateSheetV2(
                        currentStep = datePickerStep,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        onStepChange = { datePickerStep = it },
                        onYearSelected = { selectedYear = it; datePickerStep = 1 },
                        onMonthSelected = { selectedMonth = it; datePickerStep = 2 },
                        onDaySelected = { day ->
                            val y = selectedYear
                            val m = selectedMonth
                            if (y != null && m != null) {
                                dob = "$y-${m.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                                dobError = null
                            }
                            showDatePicker = false
                            datePickerStep = 0
                        },
                        onDismiss = {
                            showDatePicker = false
                            datePickerStep = 0
                        }
                    )
                }
            }
        }

        // ── Verifying overlay ──
        if (isBusy) {
            val message = when (val state = submitState) {
                is UserInfoViewModel.SubmitState.CheckingPan -> "Checking PAN..."
                is UserInfoViewModel.SubmitState.SubmittingDetails -> "Submitting details, please wait..."
                else -> "Submitting details, please wait..."
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(V2VerifyOverlay)
                    .zIndex(10f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
                contentAlignment = Alignment.Center
            ) {
                LoadingScreen(text = message)
            }
        }

        // Bottom Sheet Mode for Preview
        if (showBottomSheetInPreview) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x7A140C08)) // Dark dimmed scrim
                    .clickable { showBottomSheetInPreview = false }
            ) {
                UpiFetchSheetScreen(
                    onNavigateBack = { showBottomSheetInPreview = false }
                )
            }
        }
    }
}

@Composable
private fun FieldLabelRow(label: String, showPrefilledChip: Boolean = false, showVerifiedChip: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = V2BronzeInk)
        if (showPrefilledChip) {
            Chip(text = "✓ PRE-FILLED")
        }
        if (showVerifiedChip) {
            Chip(text = "✓ VERIFIED")
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(modifier = Modifier.background(V2BadgeGreen10, RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = V2SuccessGreen)
    }
}

@Composable
private fun FieldFootnote(error: String?, hint: String?) {
    if (error != null) {
        Text(
            text = error,
            color = V2ErrorRed,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    } else if (hint != null) {
        Text(
            text = hint,
            color = V2BronzeMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun fieldColors(isError: Boolean, isPrefilled: Boolean, isValid: Boolean): TextFieldColors {
    val borderColor = when {
        isError -> V2ErrorRed
        isPrefilled -> V2GoldAccent
        isValid -> V2SuccessGreen
        else -> V2WarmGreyBorder
    }
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = borderColor,
        unfocusedBorderColor = borderColor,
        errorBorderColor = V2ErrorRed,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )
}

@Composable
private fun UserInfoDateSheetV2(
    currentStep: Int,
    selectedYear: Int?,
    selectedMonth: Int?,
    onStepChange: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .background(V2Cream, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).background(V2BorderGold22, RoundedCornerShape(99.dp)))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DATE OF BIRTH", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.sp, color = V2GoldAccent)
                Spacer(modifier = Modifier.height(3.dp))
                val label = when (currentStep) {
                    0 -> "Select Year"
                    1 -> "Select Month (${selectedYear ?: ""})"
                    else -> "Select Day (${months.getOrNull((selectedMonth ?: 1) - 1) ?: ""}, ${selectedYear ?: ""})"
                }
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = V2BronzeInk)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentStep > 0) {
                    Text(
                        text = "← Back",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = V2SuccessGreen,
                        modifier = Modifier.clickable { onStepChange(currentStep - 1) }
                    )
                }
                Box(
                    modifier = Modifier.size(28.dp).background(V2BorderGold22, CircleShape).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = V2BronzeMuted, modifier = Modifier.size(13.dp))
                }
            }
        }

        Divider(color = V2BorderGold20, thickness = 1.dp)

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (currentStep >= i) V2GoldAccent else V2BorderGold20, RoundedCornerShape(99.dp))
                )
            }
        }

        when (currentStep) {
            0 -> UserInfoYearGridV2(onYearSelected = onYearSelected)
            1 -> UserInfoMonthGridV2(selectedYear = selectedYear, onMonthSelected = onMonthSelected)
            else -> UserInfoDayGridV2(selectedYear = selectedYear, selectedMonth = selectedMonth, onDaySelected = onDaySelected)
        }
    }
}

@Composable
private fun UserInfoYearGridV2(onYearSelected: (Int) -> Unit) {
    val years = (1950..2008).toList().reversed() // Match age limit

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(years) { year ->
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, V2BorderGold22, RoundedCornerShape(10.dp))
                    .clickable { onYearSelected(year) },
                contentAlignment = Alignment.Center
            ) {
                Text(year.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
            }
        }
    }
}

@Composable
private fun UserInfoMonthGridV2(selectedYear: Int?, onMonthSelected: (Int) -> Unit) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(months.size) { index ->
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, V2BorderGold22, RoundedCornerShape(10.dp))
                    .clickable { onMonthSelected(index + 1) },
                contentAlignment = Alignment.Center
            ) {
                Text(months[index], fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
            }
        }
    }
}

@Composable
private fun UserInfoDayGridV2(selectedYear: Int?, selectedMonth: Int?, onDaySelected: (Int) -> Unit) {
    val daysInMonth = if (selectedYear != null && selectedMonth != null) {
        getDaysInMonth(selectedYear, selectedMonth)
    } else 31

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items((1..daysInMonth).toList()) { day ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.White, RoundedCornerShape(9.dp))
                    .border(1.dp, V2BorderGold22, RoundedCornerShape(9.dp))
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(day.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
            }
        }
    }
}
