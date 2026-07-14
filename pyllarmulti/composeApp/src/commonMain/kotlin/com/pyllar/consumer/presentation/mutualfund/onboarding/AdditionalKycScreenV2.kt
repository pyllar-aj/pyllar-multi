package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.*
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.filterEnglishAddress
import com.pyllar.consumer.util.filterEnglishName
import com.pyllar.consumer.util.filterEnglishPan
import com.pyllar.consumer.util.filterEnglishTitleCase
import com.pyllar.consumer.util.filterEnglishUppercase
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.platform.LocationProvider
import com.pyllar.consumer.platform.PermissionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val AKV2Cream = Color(0xFFFBF9F4)
private val AKV2CreamTint = Color(0xFFF5EEDB)
private val AKV2BronzeInk = Color(0xFF3E2723)
private val AKV2BronzeMuted = Color(0xFF6D4C41)
private val AKV2GoldDeep = Color(0xFF8B6B25)
private val AKV2GoldAccent = Color(0xFFD4AF37)
private val AKV2Obsidian = Color(0xFF0A2415)
private val AKV2LinkGreen = Color(0xFF1A7A42)
private val AKV2VolatilityRed = Color(0xFFC62828)
private val AKV2FieldBorder = Color(0xFFD7CCC8)
private val AKV2CardBorder = Color(0xFFEFEBE9)
private val AKV2DeclarationsBorder = Color(0x428B6B25)
private val AKV2ErrorBg = Color(0x14C62828)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdditionalKycScreenV2(
    kycAttemptId: String,
    token: String,
    onNext: (String?, String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AdditionalKycViewModel = koinInject(),
    locationProvider: LocationProvider = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val scope = rememberCoroutineScope()
    var fatherName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf("") }
    var occupationType by remember { mutableStateOf("") }
    var placeOfBirth by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var addressLine3 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var residentialStatus by remember { mutableStateOf("yes") }
    var monthlyIncome by remember { mutableStateOf("") }

    var nationality by remember { mutableStateOf("yes") }
    var politicallyExposed by remember { mutableStateOf("no") }
    var isConfirmed by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var hasAttemptedLocationPopulation by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var touchedFields by remember { mutableStateOf(setOf<String>()) }
    var visitedFields by remember { mutableStateOf(setOf<String>()) }
    var currentFocusedField by remember { mutableStateOf<String?>(null) }

    var locationPermissionStatus by remember { mutableStateOf(PermissionStatus(false, false, false)) }
    LaunchedEffect(Unit) {
        // Fetch initial status on Main thread
        val initialStatus = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            permissionManager.checkStatus()
        }
        locationPermissionStatus = initialStatus

        while (true) {
            delay(1000)
            val newStatus = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                permissionManager.checkStatus()
            }
            if (locationPermissionStatus != newStatus) {
                locationPermissionStatus = newStatus
            }
        }
    }

    val missingFields = remember(
        fatherName, gender, maritalStatus, occupationType, placeOfBirth,
        addressLine1, addressLine2, addressLine3, city, pincode,
        residentialStatus, monthlyIncome, nationality, politicallyExposed, isConfirmed,
        locationPermissionStatus
    ) {
        val list = mutableListOf<String>()
        if (fatherName.isBlank()) list.add("Father's Name")
        if (gender.isBlank()) list.add("Gender")
        if (maritalStatus.isBlank()) list.add("Marital Status")
        if (occupationType.isBlank()) list.add("Occupation Type")
        if (placeOfBirth.isBlank()) list.add("Place of Birth")
        if (addressLine1.isBlank()) list.add("Address Line 1")
        if (addressLine2.isBlank()) list.add("Address Line 2")
        if (addressLine3.isBlank()) list.add("Address Line 3")
        if (city.isBlank()) list.add("City")
        if (pincode.isBlank() || !((pincode.length == 6 && pincode.all { it.isDigit() }))) {
            list.add("Pincode")
        }
        val incomeVal = monthlyIncome.toDoubleOrNull() ?: 0.0
        if (monthlyIncome.isBlank() || incomeVal <= 0.0) {
            list.add("Monthly Income")
        }

        if (residentialStatus != "yes") list.add("Residential Status")
        if (nationality != "yes") list.add("Nationality")
        if (politicallyExposed != "no") list.add("Politically Exposed Person")
        if (!isConfirmed) list.add("Declarations Confirmation")
        if (isConfirmed && !locationPermissionStatus.locationGranted) {
            list.add("Location Permission")
        }
        list
    }

    val fieldOrder = listOf(
        "fatherName", "gender", "maritalStatus", "placeOfBirth", "occupationType",
        "addressLine1", "addressLine2", "addressLine3", "city", "pincode",
        "monthlyIncome", "residentialStatus", "nationality", "politicallyExposed", "isConfirmed"
    )

    fun onFieldFocusChanged(fieldName: String, isFocused: Boolean) {
        if (isFocused) {
            currentFocusedField = fieldName
            visitedFields = visitedFields + fieldName
        } else {
            if (currentFocusedField == fieldName) {
                touchedFields = touchedFields + fieldName
                currentFocusedField = null
            }
        }
    }

    fun onFieldInteracted(fieldName: String) {
        visitedFields = visitedFields + fieldName
        touchedFields = touchedFields + fieldName
    }

    fun shouldShowError(fieldName: String, fieldValue: String, isValid: (String) -> Boolean = { it.isNotBlank() }): Boolean {
        if (isValid(fieldValue)) return false
        if (showValidationErrors) return true
        if (fieldName in touchedFields) return true

        val fieldIndex = fieldOrder.indexOf(fieldName)
        if (fieldIndex >= 0) {
            val laterFields = fieldOrder.drop(fieldIndex + 1)
            if (laterFields.any { it in visitedFields }) {
                return true
            }
        }
        return false
    }

    fun isPincodeValid(pincodeValue: String): Boolean {
        return pincodeValue.isNotBlank() && pincodeValue.length == 6 && pincodeValue.all { it.isDigit() }
    }

    val timeoutState = rememberTimeoutState("AdditionalKyc", "submit")

    val nextScreen by viewModel.nextScreen.collectAsStateWithLifecycle()
    val prefillData by viewModel.prefillData.collectAsStateWithLifecycle()
    val isLoadingScreenData by viewModel.isLoadingScreenData.collectAsStateWithLifecycle()

    // Smart Fill Logic
    LaunchedEffect(prefillData) {
        if (prefillData.isNotEmpty()) {
            platformLog("AdditionalKycScreenV2: ⚡ [Smart Fill] Applying prefill data: $prefillData")

            fun fillIfEmpty(current: String, key: String, update: (String) -> Unit) {
                if (current.isBlank()) {
                    val value = prefillData[key]?.toString()
                    if (!value.isNullOrBlank() && !value.equals("null", ignoreCase = true)) {
                        update(value)
                    }
                }
            }

            fillIfEmpty(fatherName, "fatherName") { fatherName = it.filterEnglishUppercase() }
            fillIfEmpty(gender, "gender") { gender = it.lowercase() }
            fillIfEmpty(maritalStatus, "maritalStatus") { maritalStatus = it.lowercase() }
            fillIfEmpty(occupationType, "occupationType") { occupationType = it.lowercase() }
            fillIfEmpty(placeOfBirth, "placeOfBirth") { placeOfBirth = it.filterEnglishTitleCase(20) }
            fillIfEmpty(monthlyIncome, "monthlyIncome") {
                val stripped = it.filter { c -> c.isDigit() || c == '.' }
                val clean = stripped.toDoubleOrNull()
                    ?.toLong()
                    ?.takeIf { v -> v > 0 }
                    ?.toString()
                    ?: stripped.filter { c -> c.isDigit() }.takeIf { s -> (s.toLongOrNull() ?: 0L) > 0 }
                if (!clean.isNullOrEmpty()) monthlyIncome = clean
            }
            fillIfEmpty(addressLine1, "addressLine1") { addressLine1 = it.filterEnglishAddress() }
            fillIfEmpty(addressLine2, "addressLine2") { addressLine2 = it.filterEnglishAddress() }
            fillIfEmpty(addressLine3, "addressLine3") { addressLine3 = it.filterEnglishAddress() }
            fillIfEmpty(city, "city") { city = it.filterEnglishTitleCase(50) }
            fillIfEmpty(pincode, "pincode") {
                val clean = it.filter { c -> c.isDigit() }.take(6)
                if (clean.isNotEmpty()) pincode = clean
            }

            if (prefillData.containsKey("isPoliticallyExposed")) {
                val isPep = prefillData["isPoliticallyExposed"].toString().toBoolean()
                if (isPep && politicallyExposed == "no") {
                    politicallyExposed = "yes"
                }
            }

            if (prefillData.containsKey("nationalityCountry")) {
                val country = prefillData["nationalityCountry"]?.toString()
                if (country != "IN" && country != "India" && nationality == "yes") {
                    nationality = "no"
                }
            }
        }
    }

    val genderOptions = listOf("male", "female", "transgender")
    val genderDisplayMap = mapOf(
        "male" to "Male",
        "female" to "Female",
        "transgender" to "Transgender"
    )
    val maritalOptions = listOf("married", "unmarried", "others")
    val maritalDisplayMap = mapOf(
        "married" to "Married",
        "unmarried" to "Unmarried",
        "others" to "Others"
    )
    val occupationOptions = listOf("business", "professional", "retired", "housewife", "student", "public_sector", "private_sector", "government_sector", "others")
    val occupationDisplayMap = occupationOptions.associateWith {
        it.replace("_", " ").split(" ").joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }
    }
    val yesNoOptions = listOf("yes", "no")
    val yesNoDisplayMap = mapOf("yes" to "Yes", "no" to "No")
    val pepOptions = listOf("no", "yes")
    val pepDisplayMap = mapOf("no" to "No", "yes" to "Yes — I am a PEP")

    val scrollState = rememberScrollState()
    val submitResult by viewModel.submitResult.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("AdditionalKyc")
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logEvent(
            "kyc_additional_open",
            mapOf(
                "kyc_attempt_id_present" to kycAttemptId.isNotBlank(),
                "kyc_attempt_id" to kycAttemptId,
                "screen_version" to "v4"
            )
        )
    }

    LaunchedEffect(
        fatherName, gender, maritalStatus, occupationType, placeOfBirth,
        addressLine1, addressLine2, addressLine3, city, pincode,
        residentialStatus, monthlyIncome, nationality, politicallyExposed, isConfirmed,
        locationPermissionStatus
    ) {
        val incomeVal = monthlyIncome.toDoubleOrNull() ?: 0.0
        val isFormValid = fatherName.isNotBlank() &&
                gender.isNotBlank() &&
                maritalStatus.isNotBlank() &&
                occupationType.isNotBlank() &&
                placeOfBirth.isNotBlank() &&
                addressLine1.isNotBlank() &&
                addressLine2.isNotBlank() &&
                addressLine3.isNotBlank() &&
                city.isNotBlank() &&
                isPincodeValid(pincode) &&
                residentialStatus.isNotBlank() &&
                incomeVal > 0.0 &&
                nationality.isNotBlank() &&
                politicallyExposed.isNotBlank() &&
                isConfirmed &&
                residentialStatus == "yes" &&
                nationality == "yes" &&
                politicallyExposed == "no" &&
                locationPermissionStatus.locationGranted &&
                validationMessage == null

        val isActuallyValid = isFormValid && missingFields.isEmpty() && validationMessage == null
        if (isActuallyValid && showValidationErrors) {
            showValidationErrors = false
        }
    }

    // Auto-populate location details
    LaunchedEffect(locationPermissionStatus, isLoadingScreenData) {
        if (isLoadingScreenData) return@LaunchedEffect
        if (hasAttemptedLocationPopulation) return@LaunchedEffect

        val apiProvidedCity = prefillData["city"]?.toString()?.isNotBlank() == true
        val apiProvidedPincode = prefillData["pincode"]?.toString()?.isNotBlank() == true

        if (apiProvidedCity && apiProvidedPincode) {
            hasAttemptedLocationPopulation = true
            return@LaunchedEffect
        }

        if (city.isNotBlank() && pincode.isNotBlank()) {
            hasAttemptedLocationPopulation = true
            return@LaunchedEffect
        }

        hasAttemptedLocationPopulation = true

        if (locationPermissionStatus.locationGranted && locationPermissionStatus.gpsEnabled) {
            try {
                platformLog("AdditionalKycScreenV2: Location is enabled. Fetching location coordinates for auto-populate...")
                val coords = locationProvider.getCurrentLocation()
                if (coords != null) {
                    val address = locationProvider.reverseGeocode(coords.latitude, coords.longitude)
                    if (address != null) {
                        platformLog("AdditionalKycScreenV2: Location-based autofill: city=${address.city}, pincode=${address.pincode}")
                        if (city.isBlank() && address.city.isNotBlank()) {
                            city = address.city.filterEnglishTitleCase(50)
                        }
                        val sanitizedAutoPincode = address.pincode.filter { it.isDigit() }.take(6)
                        if (pincode.isBlank() && sanitizedAutoPincode.isNotBlank()) {
                            pincode = sanitizedAutoPincode
                        }
                    }
                }
            } catch (e: Exception) {
                platformLog("AdditionalKycScreenV2: Auto-populate location fetch error: ${e.message}")
            }
        }
    }

    LaunchedEffect(locationPermissionStatus) {
        if (locationPermissionStatus.locationGranted && locationPermissionStatus.gpsEnabled) {
            locationStatus = null
        }
    }

    val isProfileCompleted = submitResult?.data?.contains("success", true) == true || submitResult is Resource.Success
    val completedStep = if (isProfileCompleted) 2 else 1

    if (isLoadingScreenData) {
        Box(modifier = Modifier.fillMaxSize().background(AKV2Cream)) {
            LoadingScreen(text = "Loading...", modifier = Modifier.fillMaxSize())
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(AKV2Cream)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // ── Top bar ──
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AKV2LinkGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AKV2LinkGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent("share_app_clicked", mapOf("screen_name" to "AdditionalKyc", "screen_version" to "v4"))
                            platformActions.shareText("Gold, Silver & much more starting at ₹21! ✨\n\nSmall daily steps ➡️ Big rewards. Join me and build a consistent saving habit for your goals with Pyllar.\n\nStart today: https://pyllar.in/download.html", "Share Pyllar")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = AKV2LinkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    LanguageLetterButton(textColor = AKV2LinkGreen)
                    TextButton(onClick = onNavigateToHelp) {
                        Text("Help", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AKV2LinkGreen)
                    }
                }
            }

            Surface(color = AKV2Cream, shadowElevation = 8.dp, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                OnboardingStepper(currentStep = 1, completedStep = completedStep, currentScreenRoute = ScreenNames.ADDITIONAL_KYC)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 28.dp)
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
            ) {
                Text(
                    text = "Just a bit more...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AKV2BronzeInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please verify and provide the following details to proceed.",
                    fontSize = 11.sp,
                    color = AKV2BronzeMuted,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(18.dp))

                if (isProfileCompleted) {
                    Text("Details submitted successfully", color = AKV2LinkGreen, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── PERSONAL DETAILS CARD ──
                AKV2SectionLabel("PERSONAL DETAILS")
                Spacer(modifier = Modifier.height(6.dp))
                AKV2Card {
                    val isFatherNameError = shouldShowError("fatherName", fatherName)
                    AKV2Field(
                        label = "Father's Name",
                        value = fatherName,
                        onValueChange = { fatherName = it.filterEnglishUppercase() },
                        placeholder = "e.g. RAKESH SHARMA",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                        isError = isFatherNameError,
                        errorText = if (isFatherNameError) "Field is required" else null,
                        onFocusChanged = { onFieldFocusChanged("fatherName", it) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val isGenderError = shouldShowError("gender", gender)
                    AKV2FieldLabel("Gender", isGenderError)
                    Spacer(modifier = Modifier.height(8.dp))
                    AKV2ChipRow(
                        options = genderOptions,
                        selected = gender,
                        displayMap = genderDisplayMap,
                        onSelect = {
                            keyboardController?.hide(); focusManager.clearFocus()
                            gender = it; onFieldInteracted("gender")
                        }
                    )
                    if (isGenderError) AKV2InlineError("Field is required")
                    Spacer(modifier = Modifier.height(14.dp))

                    val isMaritalStatusError = shouldShowError("maritalStatus", maritalStatus)
                    AKV2FieldLabel("Marital Status", isMaritalStatusError)
                    Spacer(modifier = Modifier.height(8.dp))
                    AKV2ChipRow(
                        options = maritalOptions,
                        selected = maritalStatus,
                        displayMap = maritalDisplayMap,
                        onSelect = {
                            keyboardController?.hide(); focusManager.clearFocus()
                            maritalStatus = it; onFieldInteracted("maritalStatus")
                        }
                    )
                    if (isMaritalStatusError) AKV2InlineError("Field is required")
                    Spacer(modifier = Modifier.height(14.dp))

                    val isPlaceOfBirthError = shouldShowError("placeOfBirth", placeOfBirth)
                    AKV2Field(
                        label = "Place of Birth",
                        value = placeOfBirth,
                        onValueChange = { placeOfBirth = it.filterEnglishTitleCase(20) },
                        placeholder = "City or Town",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        isError = isPlaceOfBirthError,
                        errorText = if (isPlaceOfBirthError) "Field is required" else null,
                        onFocusChanged = { onFieldFocusChanged("placeOfBirth", it) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val isOccupationError = shouldShowError("occupationType", occupationType)
                    AKV2FieldLabel("Occupation Type", isOccupationError)
                    Spacer(modifier = Modifier.height(8.dp))
                    AKV2WrapChips(
                        options = occupationOptions,
                        selected = occupationType,
                        displayMap = occupationDisplayMap,
                        onSelect = {
                            keyboardController?.hide(); focusManager.clearFocus()
                            occupationType = it; onFieldInteracted("occupationType")
                        }
                    )
                    if (isOccupationError) AKV2InlineError("Field is required")
                    Spacer(modifier = Modifier.height(14.dp))

                    val isMonthlyIncomeError = shouldShowError("monthlyIncome", monthlyIncome) {
                        it.isNotBlank() && it.toDoubleOrNull() != null && it.toDouble() > 0
                    }
                    AKV2Field(
                        label = stringResource(Res.string.monthly_income),
                        value = monthlyIncome,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && (newValue.toLongOrNull() ?: 0L) <= 10_000_000L) {
                                monthlyIncome = newValue
                            }
                        },
                        placeholder = "50,000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        isError = isMonthlyIncomeError,
                        errorText = if (isMonthlyIncomeError) stringResource(Res.string.monthly_income_invalid) else null,
                        prefix = "₹",
                        onFocusChanged = { onFieldFocusChanged("monthlyIncome", it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── ADDRESS CARD ──
                AKV2SectionLabel("ADDRESS DETAILS")
                Spacer(modifier = Modifier.height(6.dp))
                AKV2Card {
                    val isAddressLine1Error = shouldShowError("addressLine1", addressLine1)
                    AKV2Field(
                        label = "Address Line 1",
                        value = addressLine1,
                        onValueChange = { addressLine1 = it.filterEnglishAddress() },
                        placeholder = "House / Flat / Street",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        isError = isAddressLine1Error,
                        errorText = if (isAddressLine1Error) "Field is required" else null,
                        onFocusChanged = { onFieldFocusChanged("addressLine1", it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isAddressLine2Error = shouldShowError("addressLine2", addressLine2)
                    AKV2Field(
                        label = "Address Line 2",
                        value = addressLine2,
                        onValueChange = { addressLine2 = it.filterEnglishAddress() },
                        placeholder = "Locality / Area",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        isError = isAddressLine2Error,
                        errorText = if (isAddressLine2Error) "Field is required" else null,
                        onFocusChanged = { onFieldFocusChanged("addressLine2", it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isAddressLine3Error = shouldShowError("addressLine3", addressLine3)
                    AKV2Field(
                        label = "Address Line 3",
                        value = addressLine3,
                        onValueChange = { addressLine3 = it.filterEnglishAddress() },
                        placeholder = "Landmark",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        isError = isAddressLine3Error,
                        errorText = if (isAddressLine3Error) "Field is required" else null,
                        onFocusChanged = { onFieldFocusChanged("addressLine3", it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val isCityError = shouldShowError("city", city)
                        Box(modifier = Modifier.weight(1f)) {
                            AKV2Field(
                                label = "City",
                                value = city,
                                onValueChange = { city = it.filterEnglishTitleCase(50) },
                                placeholder = "Mumbai",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                isError = isCityError,
                                errorText = if (isCityError) "Field is required" else null,
                                onFocusChanged = { onFieldFocusChanged("city", it) }
                            )
                        }
                        val pincodeValidNow = pincode.isBlank() || (pincode.length == 6 && pincode.all { it.isDigit() })
                        val isPincodeError = shouldShowError("pincode", pincode) { it.isNotBlank() && it.length == 6 && it.all { char -> char.isDigit() } } || (pincode.isNotBlank() && !pincodeValidNow)
                        Box(modifier = Modifier.weight(1f)) {
                            AKV2Field(
                                label = "Pincode",
                                value = pincode,
                                onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) pincode = it },
                                placeholder = "400001",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                isError = isPincodeError,
                                errorText = if (isPincodeError) {
                                    if (pincode.isBlank()) "Field is required" else "Pincode must be 6 digits"
                                } else null,
                                onFocusChanged = { onFieldFocusChanged("pincode", it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── DECLARATIONS CARD ──
                AKV2SectionLabel("DECLARATIONS")
                Spacer(modifier = Modifier.height(6.dp))
                AKV2DeclarationsCard {
                    Text("Are you a resident Indian?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AKV2BronzeInk)
                    Spacer(modifier = Modifier.height(8.dp))
                    AKV2ChipRow(
                        options = yesNoOptions,
                        selected = residentialStatus,
                        displayMap = yesNoDisplayMap,
                        onSelect = {
                            keyboardController?.hide(); focusManager.clearFocus()
                            residentialStatus = it
                            onFieldInteracted("residentialStatus")
                            validationMessage = if (it == "no") "We do not support Non-Resident Individuals at this time. We plan to include this feature in future updates." else if (nationality == "no") "We do not support non-Indian citizens at this time. We plan to include this feature in future updates." else if (politicallyExposed == "yes") "We do not support Politically Exposed Persons at this time. We plan to include this feature in future updates." else null
                        }
                    )
                    if (residentialStatus == "no") {
                        AKV2InlineError("We do not support Non-Resident Individuals at this time. We plan to include this feature in future updates.")
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Is your nationality Indian?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AKV2BronzeInk)
                    Spacer(modifier = Modifier.height(8.dp))
                    AKV2ChipRow(
                        options = yesNoOptions,
                        selected = nationality,
                        displayMap = yesNoDisplayMap,
                        onSelect = {
                            keyboardController?.hide(); focusManager.clearFocus()
                            nationality = it
                            onFieldInteracted("nationality")
                            validationMessage = if (residentialStatus == "no") "We do not support Non-Resident Individuals at this time. We plan to include this feature in future updates." else if (it == "no") "We do not support non-Indian citizens at this time. We plan to include this feature in future updates." else if (politicallyExposed == "yes") "We do not support Politically Exposed Persons at this time. We plan to include this feature in future updates." else null
                        }
                    )
                    if (nationality == "no") {
                        AKV2InlineError("We do not support non-Indian citizens at this time. We plan to include this feature in future updates.")
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Are you a Politically Exposed Person?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AKV2BronzeInk)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Politically Exposed Persons are individuals who are or have been entrusted with prominent public functions, e.g., Heads of States, senior politicians, etc.", fontSize = 11.sp, color = AKV2BronzeMuted, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AKV2ChipRow(
                        options = pepOptions,
                        selected = politicallyExposed,
                        displayMap = pepDisplayMap,
                        dangerValue = "yes",
                        onSelect = {
                            keyboardController?.hide(); focusManager.clearFocus()
                            politicallyExposed = it
                            onFieldInteracted("politicallyExposed")
                            validationMessage = if (residentialStatus == "no") "We do not support Non-Resident Individuals at this time. We plan to include this feature in future updates." else if (nationality == "no") "We do not support non-Indian citizens at this time. We plan to include this feature in future updates." else if (it == "yes") "We do not support Politically Exposed Persons at this time. We plan to include this feature in future updates." else null
                        }
                    )
                    if (politicallyExposed == "yes") {
                        AKV2InlineError("We do not support Politically Exposed Persons at this time. We plan to include this feature in future updates.")
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    val isConfirmedError = shouldShowError("isConfirmed", if (isConfirmed) "yes" else "no") { it == "yes" }
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(if (isConfirmed) AKV2Obsidian else Color.White, RoundedCornerShape(5.dp))
                                .border(1.5.dp, if (isConfirmed) AKV2GoldAccent else AKV2FieldBorder, RoundedCornerShape(5.dp))
                                .clickable {
                                    keyboardController?.hide(); focusManager.clearFocus()
                                    isConfirmed = !isConfirmed
                                    onFieldInteracted("isConfirmed")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isConfirmed) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = AKV2Cream, modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "I confirm that the above details are correct and I am a tax resident of India.",
                            fontSize = 11.sp,
                            color = AKV2BronzeMuted,
                            lineHeight = 17.sp
                        )
                    }
                    if (isConfirmedError) AKV2InlineError("Field is required")
                }

                if (isConfirmed && locationStatus == null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // ── LOCATION ACCESS CARD ──
                    AKV2SectionLabel("LOCATION PERMISSION")
                    Spacer(modifier = Modifier.height(6.dp))
                    AKV2Card {
                        Text(
                            text = stringResource(Res.string.location_description),
                            fontSize = 11.sp,
                            color = AKV2BronzeMuted,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (locationPermissionStatus.locationGranted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Allowed",
                                    tint = AKV2LinkGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Allowed",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AKV2LinkGreen
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val granted = permissionManager.requestLocation()
                                            if (!granted) {
                                                locationStatus = "Location permission is required to verify your address. Please grant location access."
                                            } else {
                                                locationStatus = null
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AKV2Obsidian),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text(
                                        text = "Allow Location Access",
                                        fontWeight = FontWeight.Bold,
                                        color = AKV2GoldAccent,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = { platformActions.openAppSettings() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AKV2BronzeInk),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text(
                                        text = "Open App Settings",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (validationMessage != null) {
                    AKV2ErrorBanner(text = validationMessage!!)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (showValidationErrors && missingFields.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AKV2ErrorBg, RoundedCornerShape(12.dp))
                            .border(1.dp, AKV2VolatilityRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Please fill in the required fields:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AKV2VolatilityRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            missingFields.forEach { field ->
                                Text("• $field", fontSize = 11.sp, color = AKV2VolatilityRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Location status card
                if (locationStatus != null) {
                    val isPermissionMissing = locationStatus?.contains("permission", ignoreCase = true) == true
                    val isGpsOff = locationStatus?.contains("GPS", ignoreCase = true) == true
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AKV2ErrorBg, RoundedCornerShape(12.dp))
                            .border(1.dp, AKV2VolatilityRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(locationStatus!!, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AKV2VolatilityRed)
                            if (isPermissionMissing || isGpsOff) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(Res.string.location_description), fontSize = 11.sp, color = AKV2VolatilityRed.copy(alpha = 0.85f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { platformActions.openAppSettings() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AKV2Obsidian),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Open Settings",
                                        fontWeight = FontWeight.Bold,
                                        color = AKV2GoldAccent
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val submitResultValue = submitResult
                val apiErrorValue = apiError
                val isNetworkError = (submitResultValue is Resource.Error && (submitResultValue.message?.contains("Network", ignoreCase = true) == true ||
                        submitResultValue.message?.contains("timeout", ignoreCase = true) == true ||
                        submitResultValue.message?.contains("connection", ignoreCase = true) == true ||
                        submitResultValue.message == "NETWORK_ERROR")) ||
                        apiErrorValue == "NETWORK_ERROR"
                if (isNetworkError) {
                    AKV2ErrorBanner(text = stringResource(Res.string.check_internet_connection))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── CTA ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(AKV2GoldAccent, AKV2GoldDeep)), RoundedCornerShape(50))
                        .padding(1.5.dp)
                ) {
                    val parsedIncome = monthlyIncome.toDoubleOrNull() ?: 0.0
                    val isFormValidWithoutLocation = fatherName.isNotBlank() &&
                            gender.isNotBlank() &&
                            maritalStatus.isNotBlank() &&
                            occupationType.isNotBlank() &&
                            placeOfBirth.isNotBlank() &&
                            addressLine1.isNotBlank() &&
                            addressLine2.isNotBlank() &&
                            addressLine3.isNotBlank() &&
                            city.isNotBlank() &&
                            isPincodeValid(pincode) &&
                            residentialStatus.isNotBlank() &&
                            parsedIncome > 0.0 &&
                            nationality.isNotBlank() &&
                            politicallyExposed.isNotBlank() &&
                            isConfirmed &&
                            residentialStatus == "yes" &&
                            nationality == "yes" &&
                            politicallyExposed == "no" &&
                            validationMessage == null

                    val isFormValid = isFormValidWithoutLocation && locationPermissionStatus.locationGranted

                    TimeoutButton(
                        onClick = {
                            visitedFields = fieldOrder.toSet()
                            touchedFields = fieldOrder.toSet()

                            if (!isFormValidWithoutLocation) {
                                showValidationErrors = true
                                // Scroll to the first missing field
                                scope.launch {
                                    val incomeVal = monthlyIncome.toDoubleOrNull() ?: 0.0
                                    val targetScrollPosition = when {
                                        fatherName.isBlank() -> 0
                                        gender.isBlank() -> (scrollState.maxValue * 0.15f).toInt()
                                        maritalStatus.isBlank() -> (scrollState.maxValue * 0.25f).toInt()
                                        placeOfBirth.isBlank() -> (scrollState.maxValue * 0.35f).toInt()
                                        occupationType.isBlank() -> (scrollState.maxValue * 0.45f).toInt()
                                        monthlyIncome.isBlank() || incomeVal <= 0.0 -> (scrollState.maxValue * 0.55f).toInt()
                                        addressLine1.isBlank() -> (scrollState.maxValue * 0.65f).toInt()
                                        addressLine2.isBlank() -> (scrollState.maxValue * 0.70f).toInt()
                                        addressLine3.isBlank() -> (scrollState.maxValue * 0.75f).toInt()
                                        city.isBlank() -> (scrollState.maxValue * 0.80f).toInt()
                                        pincode.isBlank() || pincode.length != 6 || !pincode.all { it.isDigit() } -> (scrollState.maxValue * 0.80f).toInt()
                                        residentialStatus != "yes" || nationality != "yes" || politicallyExposed != "no" || !isConfirmed -> (scrollState.maxValue * 0.90f).toInt()
                                        else -> 0
                                    }
                                    scrollState.animateScrollTo(targetScrollPosition)
                                }
                                return@TimeoutButton
                            }

                            if (isSubmitting) {
                                return@TimeoutButton
                            }

                            scope.launch {
                                apiError = null
                                PlatformAnalyticsLogger.logEvent(
                                    "kyc_additional_submit_attempt",
                                    mapOf("kyc_attempt_id_present" to kycAttemptId.isNotBlank(), "screen_version" to "v4")
                                )

                                 val status = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                     permissionManager.checkStatus()
                                 }
                                 if (!status.locationGranted) {
                                     locationStatus = "Location permission is required to verify your address. Please grant location access."
                                     val granted = permissionManager.requestLocation()
                                     if (!granted) {
                                         return@launch
                                     }
                                 }

                                 val updatedStatus = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                     permissionManager.checkStatus()
                                 }
                                 if (!updatedStatus.gpsEnabled) {
                                     locationStatus = "Location services/GPS are disabled. Please enable location services in Settings."
                                     return@launch
                                 }

                                isSubmitting = true
                                isFetchingLocation = true
                                locationStatus = null

                                try {
                                    val coords = locationProvider.getCurrentLocation()
                                    val finalLat = coords?.latitude
                                    val finalLon = coords?.longitude
                                    isFetchingLocation = false

                                    if (finalLat != null && finalLon != null) {
                                        if (city.isBlank() || pincode.isBlank()) {
                                            try {
                                                val address = locationProvider.reverseGeocode(finalLat, finalLon)
                                                if (address != null) {
                                                    if (city.isBlank() && address.city.isNotBlank()) {
                                                        city = address.city.filterEnglishTitleCase(50)
                                                    }
                                                    val sanitizedPincode = address.pincode.filter { it.isDigit() }.take(6)
                                                    if (pincode.isBlank() && sanitizedPincode.isNotBlank()) pincode = sanitizedPincode
                                                }
                                            } catch (e: Exception) {
                                                platformLog("AdditionalKycScreenV2: Reverse geocoding error: ${e.message}")
                                            }
                                        }

                                        if (city.isBlank() || !isPincodeValid(pincode)) {
                                            locationStatus = "Unable to determine your location. Please check your GPS signal and try again."
                                            isSubmitting = false
                                            return@launch
                                        }

                                        viewModel.submitAdditionalKyc(
                                            kycAttemptId = kycAttemptId,
                                            token = token,
                                            maritalStatus = maritalStatus,
                                            occupationType = occupationType,
                                            fatherName = fatherName,
                                            monthlyIncome = parsedIncome,
                                            isPoliticallyExposed = politicallyExposed == "yes",
                                            nationalityCountry = if (nationality == "yes") "IN" else "OTHERS",
                                            placeOfBirth = placeOfBirth,
                                            gender = gender,
                                            addressLine1 = addressLine1,
                                            addressLine2 = addressLine2,
                                            addressLine3 = addressLine3,
                                            city = city,
                                            pincode = pincode,
                                            longitude = finalLon,
                                            latitude = finalLat
                                        )
                                        PlatformAnalyticsLogger.logEvent(
                                            "kyc_additional_submit_success",
                                            mapOf("kyc_attempt_id_present" to kycAttemptId.isNotBlank(), "screen_version" to "v4")
                                        )
                                    } else {
                                        locationStatus = "Unable to determine your location. Please check your GPS signal and try again."
                                        PlatformAnalyticsLogger.logEvent("kyc_additional_location_unavailable", mapOf("screen_version" to "v4"))
                                        isSubmitting = false
                                    }
                                } catch (e: Exception) {
                                    platformLog("AdditionalKycScreenV2: Location fetch error: ${e.message}")
                                    locationStatus = "An error occurred while fetching your location. Please try again."
                                    PlatformAnalyticsLogger.logEvent("kyc_additional_location_error", mapOf("message" to (e.message ?: "unknown"), "screen_version" to "v4"))
                                    isFetchingLocation = false
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        timeoutState = timeoutState,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AKV2Obsidian,
                            contentColor = AKV2Cream,
                            disabledContainerColor = AKV2Obsidian,
                            disabledContentColor = AKV2Cream
                        )
                    ) {
                        if (isSubmitting || isFetchingLocation) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AKV2Cream, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submitting...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(stringResource(Res.string.btn_continue), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("→", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AKV2GoldAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(62.dp))
            }
        }

        if (isSubmitting || isFetchingLocation || submitResult is Resource.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { }
            ) {
                LoadingScreen(text = "Submitting, please wait...", modifier = Modifier.fillMaxSize())
            }
        }
    }

    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90000)
            if (isSubmitting && submitResult == null) {
                isSubmitting = false
                apiError = "NETWORK_ERROR"
            }
        } else {
            apiError = null
        }
    }

    LaunchedEffect(submitResult) {
        val result = submitResult
        if (result != null) {
            apiError = null
            if (result is Resource.Success) {
                isSubmitting = false
                val nextScr = nextScreen ?: "bank_details"
                onNext(nextScr, kycAttemptId)
            } else if (result is Resource.Error) {
                isSubmitting = false
                timeoutState.triggerTimeout()
            }
        }
    }
}

@Composable
private fun AKV2SectionLabel(text: String) {
    Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.13.em, color = AKV2GoldAccent, modifier = Modifier.padding(start = 2.dp))
}

@Composable
private fun AKV2FieldLabel(text: String, isError: Boolean) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isError) AKV2VolatilityRed else AKV2BronzeMuted)
}

@Composable
private fun AKV2InlineError(text: String) {
    Text(text = text, fontSize = 11.sp, color = AKV2VolatilityRed, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun AKV2ErrorBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AKV2ErrorBg, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(text = text, fontSize = 12.sp, color = AKV2VolatilityRed)
    }
}

@Composable
private fun AKV2Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, AKV2CardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun AKV2DeclarationsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(AKV2Cream, AKV2CreamTint)), RoundedCornerShape(16.dp))
            .border(1.dp, AKV2DeclarationsBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun AKV2Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    isError: Boolean,
    errorText: String?,
    onFocusChanged: (Boolean) -> Unit,
    prefix: String? = null
) {
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AKV2BronzeMuted, modifier = Modifier.padding(bottom = 5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AKV2FieldBorder, fontSize = 14.sp) },
            leadingIcon = if (prefix != null) {
                { Text(prefix, color = AKV2BronzeMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AKV2LinkGreen,
                unfocusedBorderColor = AKV2FieldBorder,
                errorBorderColor = AKV2VolatilityRed,
                focusedTextColor = AKV2BronzeInk,
                unfocusedTextColor = AKV2BronzeInk,
                cursorColor = AKV2LinkGreen
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) }
        )
        if (errorText != null) AKV2InlineError(errorText)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AKV2ChipRow(
    options: List<String>,
    selected: String,
    displayMap: Map<String, String>,
    dangerValue: String? = null,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            val isDanger = isSelected && dangerValue == option
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .background(
                        when {
                            isDanger -> Color(0xFF7B1C1C)
                            isSelected -> AKV2Obsidian
                            else -> Color.White
                        },
                        RoundedCornerShape(50)
                    )
                    .border(
                        1.5.dp,
                        when {
                            isDanger -> AKV2VolatilityRed
                            isSelected -> AKV2GoldAccent
                            else -> AKV2FieldBorder
                        },
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayMap[option] ?: option,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) AKV2Cream else AKV2BronzeMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AKV2WrapChips(
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
                    .background(if (isSelected) AKV2Obsidian else Color.White, RoundedCornerShape(50))
                    .border(1.5.dp, if (isSelected) AKV2GoldAccent else AKV2FieldBorder, RoundedCornerShape(50))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayMap[option] ?: option,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) AKV2Cream else AKV2BronzeMuted,
                    maxLines = 1
                )
            }
        }
    }
}
