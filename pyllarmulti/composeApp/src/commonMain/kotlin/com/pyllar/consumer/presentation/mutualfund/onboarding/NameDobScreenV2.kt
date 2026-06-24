package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.filterEnglishName
import com.pyllar.consumer.util.currentTimeMillis
import com.pyllar.consumer.util.getCurrentYear
import com.pyllar.consumer.util.getCurrentMonth
import com.pyllar.consumer.util.getCurrentDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import pyllar.composeapp.generated.resources.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val V2Cream = Color(0xFFFBF9F4)
private val V2CreamTint = Color(0xFFF5EEDB)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2GoldDeep = Color(0xFF8B6B25)
private val V2GoldAccent = Color(0xFFD4AF37)
private val V2Obsidian = Color(0xFF0A2415)
private val V2SuccessGreen = Color(0xFF2E7D32)
private val V2LinkGreen = Color(0xFF1A7A42)
private val V2BorderGold28 = Color(0x478B6B25)
private val V2BorderGold14 = Color(0x248B6B25)
private val V2BorderGold20 = Color(0x338B6B25)
private val V2BorderGold22 = Color(0x388B6B25)
private val V2IconBgGold10 = Color(0x1A8B6B25)
private val V2IconBgGold16 = Color(0x29D4AF37)
private val V2PlaceholderGold42 = Color(0x6B8B6B25)
private val V2PlaceholderGold45 = Color(0x738B6B25)
private val V2BadgeGreen10 = Color(0x1A2E7D32)
private val V2PrivacyBg = Color(0x0A0A2415)
private val V2PrivacyBorder = Color(0x120A2415)
private val V2ScrimOverlay = Color(0x7A1E120C)
private val V2VerifyOverlay = Color(0xE0FBF9F4)

private fun isValidEmail(value: String): Boolean =
    value.isNotBlank() && Regex("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+").matches(value)

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

private fun formatDobDisplay(dobIso: String): String {
    return try {
        val parts = dobIso.split("-")
        if (parts.size != 3) return ""
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val day = parts[2].toInt()
        val month = months[parts[1].toInt() - 1]
        "$day $month ${parts[0]}"
    } catch (e: Exception) {
        ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameDobScreenV2(
    onKycSubmitted: (name: String, dob: String, email: String, navigationInfo: NavigationInfo?, data: Any?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: NameDobViewModel = koinInject(),
    userId: String,
    pan: String,
    email: String,
    phone: String,
    token: String,
    sessionStore: com.pyllar.consumer.domain.storage.SessionStore = koinInject()
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var namePrefilled by remember { mutableStateOf(false) }
    var nameEditing by remember { mutableStateOf(true) }
    val nameFocusRequester = remember { FocusRequester() }

    var dob by remember { mutableStateOf("") }
    var displayPan by remember { mutableStateOf(pan) }

    val timeoutState = rememberTimeoutState("NameDob", "continue")

    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerStep by remember { mutableStateOf(0) } // 0: year, 1: month, 2: day
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    // Contact section
    var detectedEmail by remember { mutableStateOf(email) }
    var emailMode by remember { mutableStateOf(if (email.isNotBlank()) "chip" else "manual") }
    var manualEmail by remember { mutableStateOf("") }
    val emailFocusRequester = remember { FocusRequester() }

    // Error handling
    var nameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var genericError by remember { mutableStateOf<String?>(null) }

    var isEmailFocused by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(isEmailFocused) {
        if (isEmailFocused) {
            for (i in 1..8) {
                delay(120)
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    // Polling / submission states
    var isPolling by remember { mutableStateOf(false) }
    var pollMessage by remember { mutableStateOf<String?>(null) }
    var pollDelayMs by remember { mutableStateOf<Long?>(null) }
    var preVerificationId by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var confirmedEmail by remember { mutableStateOf("") }
    var pollingStartTime by remember { mutableStateOf<Long?>(null) }

    val POLLING_TIMEOUT_MS = 2 * 60 * 1000L
    val SUBMISSION_TIMEOUT_MS = 2 * 60 * 1000L
    val MAX_POLLING_RETRIES = 24

    val kycResult by viewModel.kycResult.collectAsState()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("NameDobV4")
    }

    LaunchedEffect(Unit) {
        val storedPanHolderName = sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.FULL_NAME) ?: ""
        if (storedPanHolderName.isNotBlank()) {
            name = storedPanHolderName.filterEnglishName()
            namePrefilled = true
        }
    }

    val prefillData by viewModel.prefillData.collectAsState()

    LaunchedEffect(prefillData) {
        val prepopulatedName = prefillData["name"] as? String
        if (!prepopulatedName.isNullOrBlank() && name.isBlank()) {
            name = prepopulatedName.filterEnglishName()
            namePrefilled = true
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

        val prepopulatedPan = prefillData["pan"] as? String
        if (!prepopulatedPan.isNullOrBlank() && displayPan.isBlank()) {
            displayPan = prepopulatedPan
        }

        val prepopulatedEmail = prefillData["email"] as? String
        if (!prepopulatedEmail.isNullOrBlank() && detectedEmail.isBlank()) {
            detectedEmail = prepopulatedEmail
            emailMode = "chip"
        }
    }

    LaunchedEffect(isPolling) {
        if (!isPolling) {
            pollingStartTime = null
            return@LaunchedEffect
        }

        pollingStartTime = currentTimeMillis()
        var retryCount = 0

        while (isPolling) {
            delay(pollDelayMs ?: 5000L)
            if (!isPolling) break

            val elapsedTime = pollingStartTime?.let { currentTimeMillis() - it } ?: 0L
            if (elapsedTime >= POLLING_TIMEOUT_MS) {
                isPolling = false
                pollMessage = null
                genericError = "Verification is taking longer than expected. Please try again."
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent(
                    "kyc_polling_timeout",
                    mapOf("elapsed_ms" to elapsedTime, "retry_count" to retryCount, "screen_version" to "v4")
                )
                break
            }

            retryCount++
            if (retryCount > MAX_POLLING_RETRIES) {
                isPolling = false
                pollMessage = null
                genericError = "Maximum retry limit reached."
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent(
                    "kyc_polling_max_retries",
                    mapOf("retry_count" to retryCount, "screen_version" to "v4")
                )
                break
            }

            viewModel.createMinimalKyc(
                userId = userId,
                name = name,
                panNumber = pan,
                dateOfBirth = dob,
                emailAddress = confirmedEmail.ifBlank { email },
                mobileCountryCode = "+91",
                mobileNumber = phone.takeLast(10),
                token = token,
                preVerificationId = preVerificationId
            )
        }
    }

    LaunchedEffect(isSubmitting) {
        if (!isSubmitting) return@LaunchedEffect

        delay(SUBMISSION_TIMEOUT_MS)

        if (isSubmitting) {
            isSubmitting = false
            genericError = "Request is taking too long. Please try again."
            timeoutState.triggerTimeout()
            PlatformAnalyticsLogger.logEvent(
                "kyc_submission_timeout",
                mapOf("timeout_ms" to SUBMISSION_TIMEOUT_MS, "screen_version" to "v4")
            )
        }
    }

    LaunchedEffect(kycResult) {
        val result = kycResult
        if (result == null) return@LaunchedEffect

        when (result) {
            is Resource.Success<*> -> {
                val navigationInfo = result.navigation
                val action = navigationInfo?.action
                val serverData = result.data

                if (action == NavigationAction.POLL) {
                    isPolling = true
                    isSubmitting = false
                    val params = navigationInfo.params
                    pollMessage = (params?.get("message") as? JsonPrimitive)?.contentOrNull ?: "Verifying details…"
                    preVerificationId = (params?.get("preVerificationId") as? JsonPrimitive)?.contentOrNull

                    pollDelayMs = (params?.get("delayMs") as? JsonPrimitive)?.longOrNull
                        ?: (params?.get("delay_seconds") as? JsonPrimitive)?.longOrNull?.let { it * 1000L }
                        ?: (params?.get("retry_after_sec") as? JsonPrimitive)?.longOrNull?.let { it * 1000L }
                        ?: (params?.get("retry_after_ms") as? JsonPrimitive)?.longOrNull
                        ?: (params?.get("poll_interval_ms") as? JsonPrimitive)?.longOrNull

                    if (preVerificationId == null && serverData != null) {
                        preVerificationId = serverData.kycAttemptId
                    }
                } else {
                    isPolling = false
                    isSubmitting = true
                    pollMessage = null
                    pollingStartTime = null

                    onKycSubmitted(name, dob, confirmedEmail, navigationInfo, serverData)
                }
            }
            is Resource.Error<*> -> {
                isPolling = false
                isSubmitting = false
                pollMessage = null
                pollingStartTime = null
                timeoutState.triggerTimeout()

                val fieldErrors = result.fieldErrors
                var hasSpecificError = false

                nameError = null
                dobError = null
                emailError = null
                genericError = null

                if (!fieldErrors.isNullOrEmpty()) {
                    val nameFieldError = fieldErrors.find { it.field.equals("name", ignoreCase = true) }
                    val dobFieldError = fieldErrors.find {
                        it.field.equals("dateOfBirth", ignoreCase = true) || it.field.equals("dob", ignoreCase = true)
                    }
                    val emailFieldError = fieldErrors.find { it.field.equals("emailAddress", ignoreCase = true) }

                    if (nameFieldError != null) {
                        nameError = nameFieldError.message
                        hasSpecificError = true
                    }
                    if (dobFieldError != null) {
                        dobError = dobFieldError.message
                        hasSpecificError = true
                    }
                    if (emailFieldError != null) {
                        emailError = emailFieldError.message
                        hasSpecificError = true
                    }
                }

                if (!hasSpecificError) {
                    genericError = result.message ?: "Verification failed. Please try again."
                }

                PlatformAnalyticsLogger.logEvent(
                    "kyc_minimal_submit_error",
                    mapOf(
                        "has_name_error" to (nameError != null),
                        "has_dob_error" to (dobError != null),
                        "has_email_error" to (emailError != null),
                        "has_generic_error" to (genericError != null),
                        "screen_version" to "v4"
                    )
                )
            }
            is Resource.Loading<*> -> { /* managed by isSubmitting/isPolling UI */ }
        }
    }

    fun effectiveEmail(): String = if (emailMode == "chip") detectedEmail else manualEmail

    fun submitKyc() {
        if (isSubmitting) return
        nameError = null
        dobError = null
        emailError = null
        genericError = null

        isSubmitting = true

        scope.launch {
            val sessionUserId = sessionStore.getCurrentUserId()
            val sessionPhone = sessionStore.getCurrentPhone()

            val finalUserId = sessionUserId.ifBlank { userId }
            val finalEmail = effectiveEmail().ifBlank { email }
            val finalPhone = sessionPhone.takeLast(10).ifBlank { phone.takeLast(10) }

            sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.USER_ID, finalUserId)
            sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.EMAIL, finalEmail)
            confirmedEmail = finalEmail

            PlatformAnalyticsLogger.logEvent(
                "kyc_minimal_submit_attempt",
                mapOf("pan_last4" to pan.takeLast(4), "screen_version" to "v2")
            )

            viewModel.createMinimalKyc(
                userId = finalUserId,
                name = name,
                panNumber = pan,
                dateOfBirth = dob,
                emailAddress = finalEmail,
                mobileCountryCode = "+91",
                mobileNumber = finalPhone,
                token = token,
                preVerificationId = preVerificationId
            )
        }
    }

    val nameValid = name.trim().length >= 2
    val dobValid = dob.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    val emailValid = if (emailMode == "chip") detectedEmail.isNotBlank() else isValidEmail(manualEmail)
    val canContinue = nameValid && dobValid && emailValid && !isSubmitting && !isPolling

    val subtitleText = when {
        nameEditing -> stringResource(Res.string.name_hint_type_as_appears)
        name.isBlank() -> stringResource(Res.string.name_hint_enter_name_prefilled_dob_pan)
        namePrefilled -> stringResource(Res.string.name_hint_prefilled_pan_tap_to_edit)
        else -> stringResource(Res.string.name_hint_tap_to_edit_dob_needed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(V2Cream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageLetterButton(textColor = V2LinkGreen)
                    TextButton(onClick = { focusManager.clearFocus(); onNavigateToHelp() }) {
                        Text(
                            text = stringResource(Res.string.help),
                            style = MaterialTheme.typography.labelLarge,
                            color = V2LinkGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Surface(color = V2Cream, shadowElevation = 8.dp, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                OnboardingStepper(currentStep = 0, completedStep = 0, currentScreenRoute = ScreenNames.NAME_DOB)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 28.dp)
                    .imePadding()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
            ) {
                Text(
                    text = stringResource(Res.string.personal_details).uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.14.em,
                    color = V2GoldAccent
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = stringResource(Res.string.confirm_your_details),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = V2BronzeInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitleText, fontSize = 11.sp, color = V2BronzeMuted, lineHeight = 17.sp)
                Spacer(modifier = Modifier.height(20.dp))

                if (genericError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = genericError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // ── CARD 1: Verified (PAN) ──
                SectionLabel(stringResource(Res.string.verified_label))
                Spacer(modifier = Modifier.height(6.dp))
                VerifiedPanCard(pan = displayPan)

                Spacer(modifier = Modifier.height(16.dp))

                // ── CARD 2: Your details ──
                SectionLabel(stringResource(Res.string.your_details_label))
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(V2Cream, V2CreamTint)), RoundedCornerShape(16.dp))
                        .border(1.dp, V2BorderGold28, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // Name row
                    if (nameEditing) {
                        var hasBeenFocused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x12D4AF37))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IdentityIconBox(emoji = "👤", bg = V2IconBgGold16)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.full_name_letters_only), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.09.em, color = V2GoldAccent)
                                Spacer(modifier = Modifier.height(3.dp))
                                BasicTextField(
                                    value = name,
                                    onValueChange = { newValue ->
                                        name = newValue.filterEnglishName()
                                        namePrefilled = false
                                        nameError = null
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(nameFocusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                hasBeenFocused = true
                                            } else if (hasBeenFocused) {
                                                nameEditing = false
                                                name = name.trim().uppercase()
                                            }
                                        }
                                )
                            }
                        }
                        LaunchedEffect(Unit) {
                            delay(100)
                            nameFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nameEditing = true }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IdentityIconBox(emoji = "👤", bg = V2IconBgGold10)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.full_name).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.09.em, color = V2GoldDeep)
                                Spacer(modifier = Modifier.height(2.dp))
                                if (name.isNotBlank()) {
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
                                } else {
                                    Text(stringResource(Res.string.enter_your_full_name), fontSize = 13.sp, color = V2PlaceholderGold42)
                                }
                            }
                            if (namePrefilled) {
                                Box(
                                    modifier = Modifier
                                        .background(V2BadgeGreen10, RoundedCornerShape(99.dp))
                                        .padding(horizontal = 9.dp, vertical = 3.dp)
                                ) {
                                    Text(stringResource(Res.string.pan_checked_badge), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = V2SuccessGreen)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = V2GoldDeep, modifier = Modifier.size(13.dp).alpha(0.6f))
                        }
                    }
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                    }

                    Divider(color = V2BorderGold14, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // DOB row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusManager.clearFocus(); showDatePicker = true; datePickerStep = 0 }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IdentityIconBox(emoji = "📅", bg = V2IconBgGold16)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.date_of_birth_p).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.09.em, color = V2GoldDeep)
                            Spacer(modifier = Modifier.height(2.dp))
                            if (dobValid) {
                                Text(formatDobDisplay(dob), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
                            } else {
                                Text(stringResource(Res.string.tap_to_select), fontSize = 13.sp, color = V2PlaceholderGold45)
                            }
                        }
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(Res.string.pick_date), tint = V2GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    if (dobError != null) {
                        Text(dobError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                    }

                    Divider(color = V2BorderGold14, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // Email row
                    if (emailMode == "chip") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IdentityIconBox(emoji = "✉️", bg = V2IconBgGold10)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.email).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.09.em, color = V2GoldDeep)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(detectedEmail, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
                            }
                        }
                        Text(
                            text = stringResource(Res.string.type_different_email),
                            fontSize = 11.sp,
                            color = V2LinkGreen,
                            modifier = Modifier
                                .clickable {
                                    emailMode = "manual"
                                    manualEmail = ""
                                    emailError = null
                                }
                                .padding(start = 16.dp, bottom = 12.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IdentityIconBox(emoji = "✉️", bg = V2IconBgGold10)
                            BasicTextField(
                                value = manualEmail,
                                onValueChange = { manualEmail = it; emailError = null },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(emailFocusRequester)
                                    .onFocusChanged { isEmailFocused = it.isFocused },
                                decorationBox = { inner ->
                                    if (manualEmail.isEmpty()) {
                                        Text(stringResource(Res.string.email_placeholder_demo), fontSize = 13.sp, color = V2PlaceholderGold42)
                                    }
                                    inner()
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .height(22.dp)
                                    .width(1.dp)
                                    .background(V2BorderGold28)
                            )
                            GoogleAccountPickerButton(
                                onEmailPicked = {
                                    manualEmail = it
                                    emailError = null
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(V2IconBgGold16)
                                    .border(1.dp, V2BorderGold28, CircleShape)
                            )
                        }
                        if (detectedEmail.isNotBlank()) {
                            Text(
                                text = stringResource(Res.string.use_detected_account_instead),
                                fontSize = 11.sp,
                                color = V2LinkGreen,
                                modifier = Modifier
                                    .clickable { emailMode = "chip"; emailError = null }
                                    .padding(start = 16.dp, bottom = 12.dp)
                            )
                        }
                    }
                    if (emailError != null) {
                        Text(
                            emailError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(18.dp))

                // CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (canContinue) 1f else 0.42f)
                        .background(Brush.linearGradient(listOf(V2GoldAccent, V2GoldDeep)), RoundedCornerShape(50))
                        .padding(1.5.dp)
                ) {
                    TimeoutButton(
                        onClick = { submitKyc() },
                        enabled = canContinue,
                        timeoutState = timeoutState,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = V2Obsidian,
                            contentColor = V2Cream,
                            disabledContainerColor = V2Obsidian,
                            disabledContentColor = V2Cream
                        )
                    ) {
                        Text(
                            text = if (isPolling) (pollMessage ?: stringResource(Res.string.verifying_dots)) else if (isSubmitting) stringResource(Res.string.verifying_dots) else stringResource(Res.string.btn_continue),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isSubmitting && !isPolling) {
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("→", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = V2GoldAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trust strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrustBadge(stringResource(Res.string.permission_v2_trust_sebi))
                    TrustDot()
                    TrustBadge(stringResource(Res.string.trust_data_encrypted))
                    TrustDot()
                    TrustBadge(stringResource(Res.string.permission_v2_trust_no_spam))
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
                    NameDobV2DateSheet(
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
                                val formattedMonth = m.toString().padStart(2, '0')
                                val formattedDay = day.toString().padStart(2, '0')
                                dob = "$y-$formattedMonth-$formattedDay"
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
        if (isPolling || isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(V2VerifyOverlay)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
                contentAlignment = Alignment.Center
            ) {
                LoadingScreen(text = pollMessage ?: stringResource(Res.string.submitting_please_wait))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em, color = V2GoldDeep, modifier = Modifier.padding(start = 2.dp))
}

@Composable
private fun VerifiedPanCard(pan: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(V2Cream, V2CreamTint)), RoundedCornerShape(16.dp))
            .border(1.dp, V2BorderGold28, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IdentityIconBox(emoji = "🪪", bg = V2IconBgGold10)
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.pan_number_title), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.09.em, color = V2GoldDeep)
            Spacer(modifier = Modifier.height(2.dp))
            Text(pan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk, letterSpacing = 0.05.em)
        }
        Box(modifier = Modifier.background(V2BadgeGreen10, RoundedCornerShape(99.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
            Text(stringResource(Res.string.verified), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = V2SuccessGreen)
        }
    }
}

@Composable
private fun IdentityIconBox(emoji: String, bg: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(bg, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 14.sp)
    }
}

@Composable
private fun RowScope.TrustBadge(text: String) {
    Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = V2SuccessGreen)
}

@Composable
private fun TrustDot() {
    Text(text = " · ", fontSize = 10.sp, color = V2BronzeInk.copy(alpha = 0.2f))
}

@Composable
private fun NameDobV2DateSheet(
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
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).background(V2BorderGold22, RoundedCornerShape(99.dp)))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.date_of_birth_p).uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.em, color = V2GoldAccent)
                Spacer(modifier = Modifier.height(3.dp))
                val label = when (currentStep) {
                    0 -> stringResource(Res.string.select_year)
                    1 -> stringResource(Res.string.select_month_format, selectedYear ?: "")
                    else -> stringResource(Res.string.select_day_format, months.getOrNull((selectedMonth ?: 1) - 1) ?: "", selectedYear ?: "")
                }
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = V2BronzeInk)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentStep > 0) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = V2LinkGreen,
                        modifier = Modifier.clickable { onStepChange(currentStep - 1) }
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(V2BorderGold14, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.cancel), tint = V2BronzeMuted, modifier = Modifier.size(13.dp))
                }
            }
        }

        Divider(color = V2BorderGold14, thickness = 1.dp)

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
            0 -> NameDobV2YearGrid(onYearSelected = onYearSelected)
            1 -> NameDobV2MonthGrid(selectedYear = selectedYear, onMonthSelected = onMonthSelected)
            else -> NameDobV2DayGrid(selectedYear = selectedYear, selectedMonth = selectedMonth, onDaySelected = onDaySelected)
        }
    }
}

@Composable
private fun NameDobV2YearGrid(onYearSelected: (Int) -> Unit) {
    val maxYear = getCurrentYear() - 18
    val years = (1950..maxYear).toList().reversed()

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
                    .background(Brush.linearGradient(listOf(V2Cream, V2CreamTint)), RoundedCornerShape(10.dp))
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
private fun NameDobV2MonthGrid(selectedYear: Int?, onMonthSelected: (Int) -> Unit) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val maxYear = getCurrentYear() - 18
    val maxMonthForMaxYear = getCurrentMonth()
    val allowedMonthsCount = if (selectedYear != null && selectedYear == maxYear) maxMonthForMaxYear else months.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(allowedMonthsCount) { index ->
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .background(Brush.linearGradient(listOf(V2Cream, V2CreamTint)), RoundedCornerShape(10.dp))
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
private fun NameDobV2DayGrid(selectedYear: Int?, selectedMonth: Int?, onDaySelected: (Int) -> Unit) {
    val maxYear = getCurrentYear() - 18
    val maxMonth = getCurrentMonth()
    val maxDay = getCurrentDay()

    val computedDaysInMonth = if (selectedYear != null && selectedMonth != null) {
        daysInMonth(selectedYear, selectedMonth)
    } else 31

    val daysInMonth = if (selectedYear != null && selectedMonth != null && selectedYear == maxYear && selectedMonth == maxMonth) {
        minOf(computedDaysInMonth, maxDay)
    } else computedDaysInMonth

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
                    .background(Brush.linearGradient(listOf(V2Cream, V2CreamTint)), RoundedCornerShape(9.dp))
                    .border(1.dp, V2BorderGold22, RoundedCornerShape(9.dp))
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(day.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = V2BronzeInk)
            }
        }
    }
}
