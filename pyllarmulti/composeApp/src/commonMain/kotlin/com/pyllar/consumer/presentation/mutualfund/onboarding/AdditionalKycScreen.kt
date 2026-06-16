package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.*
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.platform.PlatformActions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalKycScreen(
    kycAttemptId: String,
    token: String,
    onNext: (String?, String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    viewModel: AdditionalKycViewModel = koinInject(),
    locationProvider: com.pyllar.consumer.platform.LocationProvider = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val scope = rememberCoroutineScope()
    var fatherName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var locationStatus by remember { mutableStateOf<String?>(null) }

    var locationPermissionStatus by remember { mutableStateOf(permissionManager.checkStatus()) }
    var hasAttemptedLocationAutofill by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            locationPermissionStatus = permissionManager.checkStatus()
            delay(1000)
        }
    }


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
    
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    val fatherNameFocusRequester = remember { FocusRequester() }
    val placeOfBirthFocusRequester = remember { FocusRequester() }
    val addressLine1FocusRequester = remember { FocusRequester() }
    val addressLine2FocusRequester = remember { FocusRequester() }
    val addressLine3FocusRequester = remember { FocusRequester() }
    val cityFocusRequester = remember { FocusRequester() }
    val pincodeFocusRequester = remember { FocusRequester() }
    val monthlyIncomeFocusRequester = remember { FocusRequester() }

    val validationMessage = when {
        residentialStatus == "no" -> "We do not support Non-Resident Individuals at this time. We plan to include this feature in future updates."
        nationality == "no" -> "We do not support non-Indian citizens at this time. We plan to include this feature in future updates."
        politicallyExposed == "yes" -> "We do not support Politically Exposed Persons at this time. We plan to include this feature in future updates."
        else -> null
    }

    val missingFields = remember(
        fatherName, gender, maritalStatus, occupationType, placeOfBirth,
        addressLine1, addressLine2, addressLine3, city, pincode,
        residentialStatus, monthlyIncome, nationality, politicallyExposed
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
        if (pincode.length != 6 || !pincode.all { it.isDigit() }) list.add("Pincode")
        val parsedIncome = monthlyIncome.toDoubleOrNull() ?: 0.0
        if (monthlyIncome.isBlank() || parsedIncome <= 0.0) {
            list.add("Monthly Income")
        }
        if (residentialStatus != "yes") list.add("Residential Status")
        if (nationality != "yes") list.add("Nationality")
        if (politicallyExposed != "no") list.add("Politically Exposed Person")
        list
    }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var touchedFields by remember { mutableStateOf(setOf<String>()) }
    var visitedFields by remember { mutableStateOf(setOf<String>()) }
    var currentFocusedField by remember { mutableStateOf<String?>(null) }

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
            if (laterFields.any { it in visitedFields }) return true
        }
        return false
    }

    fun filterAddress(newValue: String): String {
        if (newValue.length > 32) return newValue.take(32)
        val filtered = newValue.filter { it.isLetterOrDigit() || it == ',' || it.isWhitespace() }
        val trimmed = filtered.trimStart()
        if (trimmed.isEmpty()) return ""
        val firstChar = trimmed[0]
        return if (firstChar.isLetter()) trimmed.replaceFirstChar { it.uppercase() }
        else if (firstChar.isDigit()) trimmed
        else ""
    }

    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val uiState by viewModel.prefillData.collectAsState()
    val isLoadingData by viewModel.isLoadingScreenData.collectAsState()
    val submitResult by viewModel.submitResult.collectAsState()

    LaunchedEffect(locationPermissionStatus, isLoadingData) {
        if (!isLoadingData && locationPermissionStatus.locationGranted && locationPermissionStatus.gpsEnabled) {
            locationStatus = null
            if (!hasAttemptedLocationAutofill && (city.isBlank() || pincode.isBlank())) {
                hasAttemptedLocationAutofill = true
                platformLog("AdditionalKycScreen: Location is enabled. Fetching location coordinates...")
                val coords = locationProvider.getCurrentLocation()
                if (coords != null) {
                    val address = locationProvider.reverseGeocode(coords.latitude, coords.longitude)
                    if (address != null) {
                        platformLog("AdditionalKycScreen: Location-based autofill: city=${address.city}, pincode=${address.pincode}")
                        if (city.isBlank() && address.city.isNotBlank()) {
                            city = address.city
                        }
                        if (pincode.isBlank() && address.pincode.isNotBlank()) {
                            pincode = address.pincode
                        }
                    }
                }
            }
        } else if (locationStatus != null) {
            if (!locationPermissionStatus.locationGranted) {
                locationStatus = "Location permission is required to verify your address. Please grant location access."
            } else if (!locationPermissionStatus.gpsEnabled) {
                locationStatus = "Location services/GPS are disabled. Please enable location services in Settings."
            }
        }
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("AdditionalKyc")
    }

    LaunchedEffect(uiState) {
        if (uiState.isNotEmpty()) {
            fun cleanPrefill(vararg keys: String): String {
                for (key in keys) {
                    val value = uiState[key]?.toString()?.trim()
                    if (value != null && value.isNotBlank() && !value.equals("null", ignoreCase = true)) {
                        return value
                    }
                }
                return ""
            }

            val rawFatherName = cleanPrefill("fatherName", "father_name")
            if (fatherName.isBlank() && rawFatherName.isNotBlank()) {
                fatherName = rawFatherName.uppercase()
            }

            val rawGender = cleanPrefill("gender")
            if (gender.isBlank() && rawGender.isNotBlank()) {
                gender = rawGender.lowercase()
            }

            val rawMaritalStatus = cleanPrefill("maritalStatus", "marital_status")
            if (maritalStatus.isBlank() && rawMaritalStatus.isNotBlank()) {
                maritalStatus = rawMaritalStatus.lowercase()
            }

            val rawOccupationType = cleanPrefill("occupationType", "occupation_type")
            if (occupationType.isBlank() && rawOccupationType.isNotBlank()) {
                occupationType = rawOccupationType.lowercase()
            }

            val rawPlaceOfBirth = cleanPrefill("placeOfBirth", "place_of_birth")
            if (placeOfBirth.isBlank() && rawPlaceOfBirth.isNotBlank()) {
                placeOfBirth = rawPlaceOfBirth
            }

            if (monthlyIncome.isBlank()) {
                val rawMonthly = cleanPrefill("monthlyIncome", "monthly_income")
                val cleanMonthly = rawMonthly.replace("\"", "").replace("'", "").filter { it.isDigit() }
                if (cleanMonthly.isNotBlank()) {
                    val doubleVal = cleanMonthly.toDoubleOrNull()
                    if (doubleVal != null) {
                        monthlyIncome = if (doubleVal % 1.0 == 0.0) doubleVal.toLong().toString() else doubleVal.toString()
                    } else {
                        monthlyIncome = cleanMonthly
                    }
                }
            }

            val rawCity = cleanPrefill("city", "address_city", "addressCity")
            if (city.isBlank() && rawCity.isNotBlank()) {
                city = rawCity
            }

            val rawPincode = cleanPrefill("pincode", "address_pincode", "addressPincode")
            if (pincode.isBlank() && rawPincode.isNotBlank()) {
                pincode = rawPincode.replace("\"", "").replace("'", "").filter { it.isDigit() }.take(6)
            }

            val rawAddressLine1 = cleanPrefill("addressLine1", "address_line1")
            val rawAddressLine2 = cleanPrefill("addressLine2", "address_line2")
            val rawAddressLine3 = cleanPrefill("addressLine3", "address_line3")

            if (addressLine1.isBlank() && rawAddressLine1.isNotBlank()) {
                addressLine1 = rawAddressLine1
            }
            if (addressLine2.isBlank() && rawAddressLine2.isNotBlank()) {
                addressLine2 = rawAddressLine2
            }
            if (addressLine3.isBlank() && rawAddressLine3.isNotBlank()) {
                addressLine3 = rawAddressLine3
            }

            // Fallback: If address line fields are still blank, check if there is a single long address string
            if (addressLine1.isBlank()) {
                val rawAddress = cleanPrefill("address_line", "addressLine", "address")
                if (rawAddress.isNotBlank()) {
                    // Split the long address into max 3 lines of 32 characters
                    val words = rawAddress.split(" ")
                    val lines = mutableListOf<String>()
                    var currentLine = StringBuilder()
                    for (word in words) {
                        if (currentLine.isEmpty()) {
                            currentLine.append(word)
                        } else if (currentLine.length + 1 + word.length <= 32) {
                            currentLine.append(" ").append(word)
                        } else {
                            lines.add(currentLine.toString())
                            currentLine = StringBuilder(word)
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    if (lines.size > 0) addressLine1 = filterAddress(lines[0])
                    if (lines.size > 1) addressLine2 = filterAddress(lines[1])
                    if (lines.size > 2) addressLine3 = filterAddress(lines.drop(2).joinToString(" "))
                }
            }

            // Handle yes/no status fields with boolean / string mapping
            val rawResidential = cleanPrefill("residentialStatus", "residential_status")
            if (rawResidential.isNotBlank()) {
                residentialStatus = if (rawResidential == "yes" || rawResidential == "true") "yes" else "no"
            }

            val rawNationality = cleanPrefill("nationality", "nationality_country", "nationalityCountry")
            if (rawNationality.isNotBlank()) {
                nationality = if (rawNationality == "yes" || rawNationality == "true" || rawNationality.uppercase() == "IN" || rawNationality.lowercase() == "india") "yes" else "no"
            }

            val rawPoliticallyExposed = cleanPrefill("politicallyExposed", "politically_exposed", "isPoliticallyExposed", "is_politically_exposed")
            if (rawPoliticallyExposed.isNotBlank()) {
                politicallyExposed = if (rawPoliticallyExposed == "yes" || rawPoliticallyExposed == "true") "yes" else "no"
            }
        }
    }

    // Clear validation errors when all required fields are valid
    LaunchedEffect(
        fatherName, gender, maritalStatus, occupationType, placeOfBirth,
        addressLine1, addressLine2, addressLine3, city, pincode,
        residentialStatus, monthlyIncome, nationality, politicallyExposed, isConfirmed
    ) {
        val parsedIncome = monthlyIncome.toDoubleOrNull() ?: 0.0
        val isFormValid = fatherName.isNotBlank() &&
                gender.isNotBlank() &&
                maritalStatus.isNotBlank() &&
                occupationType.isNotBlank() &&
                placeOfBirth.isNotBlank() &&
                addressLine1.isNotBlank() &&
                addressLine2.isNotBlank() &&
                addressLine3.isNotBlank() &&
                city.isNotBlank() &&
                pincode.length == 6 && pincode.all { it.isDigit() } &&
                residentialStatus == "yes" &&
                nationality == "yes" &&
                politicallyExposed == "no" &&
                parsedIncome > 0.0 &&
                isConfirmed &&
                validationMessage == null
        
        if (isFormValid && showValidationErrors) {
            showValidationErrors = false
        }
    }

    val genderOptions = listOf("male", "female", "transgender")
    val maritalOptions = listOf("married", "unmarried", "others")
    val occupationOptions = listOf("business", "professional", "retired", "housewife", "student", "public_sector", "private_sector", "government_sector", "others")
    val incomeOptions = listOf("upto_1lakh", "above_1lakh_upto_5lakh", "above_5lakh_upto_10lakh", "above_10lakh_upto_25lakh", "above_25lakh_upto_1cr", "above_1cr")

    if (isLoadingData) {
        LoadingScreen(text = "Loading details...", modifier = Modifier.fillMaxSize())
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
                OnboardingStepper(currentStep = 1, completedStep = 1, currentScreenRoute = ScreenNames.ADDITIONAL_KYC)
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
                Text(
                    text = "Just a bit more...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                val isFatherNameError = shouldShowError("fatherName", fatherName)
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it.uppercase() },
                    label = { Text("Father's Name") },
                    modifier = Modifier.fillMaxWidth().focusRequester(fatherNameFocusRequester).onFocusChanged { onFieldFocusChanged("fatherName", it.isFocused) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    isError = isFatherNameError,
                    supportingText = if (isFatherNameError) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                val isGenderError = shouldShowError("gender", gender)
                Text("Gender", style = MaterialTheme.typography.bodyLarge, color = if (isGenderError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ContainedButtonBox(
                    options = genderOptions,
                    selectedOption = gender,
                    onOptionSelected = { 
                        gender = it
                        onFieldInteracted("gender")
                    },
                    showError = isGenderError
                )

                val isMaritalError = shouldShowError("maritalStatus", maritalStatus)
                Text("Marital Status", style = MaterialTheme.typography.bodyLarge, color = if (isMaritalError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ContainedButtonBox(
                    options = maritalOptions,
                    selectedOption = maritalStatus,
                    onOptionSelected = { 
                        maritalStatus = it
                        onFieldInteracted("maritalStatus")
                    },
                    showError = isMaritalError
                )

                val isPlaceError = shouldShowError("placeOfBirth", placeOfBirth)
                OutlinedTextField(
                    value = placeOfBirth,
                    onValueChange = { placeOfBirth = it },
                    label = { Text("Place of Birth") },
                    modifier = Modifier.fillMaxWidth().focusRequester(placeOfBirthFocusRequester).onFocusChanged { onFieldFocusChanged("placeOfBirth", it.isFocused) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    isError = isPlaceError,
                    supportingText = if (isPlaceError) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                val isOccupationError = shouldShowError("occupationType", occupationType)
                ExposedDropdownFieldWithDisplay(
                    label = "Occupation Type",
                    selected = occupationType,
                    options = occupationOptions,
                    displayMap = occupationOptions.associateWith { it.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } },
                    onSelect = { 
                        occupationType = it
                        onFieldInteracted("occupationType")
                    },
                    showError = isOccupationError
                )

                val isAddress1Error = shouldShowError("addressLine1", addressLine1)
                OutlinedTextField(
                    value = addressLine1,
                    onValueChange = { addressLine1 = filterAddress(it) },
                    label = { Text("Address Line 1") },
                    modifier = Modifier.fillMaxWidth().focusRequester(addressLine1FocusRequester).onFocusChanged { onFieldFocusChanged("addressLine1", it.isFocused) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = isAddress1Error,
                    supportingText = if (isAddress1Error) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                val isAddress2Error = shouldShowError("addressLine2", addressLine2)
                OutlinedTextField(
                    value = addressLine2,
                    onValueChange = { addressLine2 = filterAddress(it) },
                    label = { Text("Address Line 2") },
                    modifier = Modifier.fillMaxWidth().focusRequester(addressLine2FocusRequester).onFocusChanged { onFieldFocusChanged("addressLine2", it.isFocused) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = isAddress2Error,
                    supportingText = if (isAddress2Error) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                val isAddress3Error = shouldShowError("addressLine3", addressLine3)
                OutlinedTextField(
                    value = addressLine3,
                    onValueChange = { addressLine3 = filterAddress(it) },
                    label = { Text("Address Line 3") },
                    modifier = Modifier.fillMaxWidth().focusRequester(addressLine3FocusRequester).onFocusChanged { onFieldFocusChanged("addressLine3", it.isFocused) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = isAddress3Error,
                    supportingText = if (isAddress3Error) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isCityError = shouldShowError("city", city)
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f).focusRequester(cityFocusRequester).onFocusChanged { onFieldFocusChanged("city", it.isFocused) },
                        isError = isCityError,
                        supportingText = if (isCityError) { { Text("Required", color = MaterialTheme.colorScheme.error) } } else null
                    )
                    val isPincodeError = shouldShowError("pincode", pincode, { it.length == 6 && it.all { it.isDigit() } })
                    OutlinedTextField(
                        value = pincode.replace("\"", "").replace("'", "").filter { it.isDigit() }.take(6),
                        onValueChange = { newValue ->
                            val digitsOnly = newValue.replace("\"", "").replace("'", "").filter { it.isDigit() }
                            if (digitsOnly.length <= 6) {
                                pincode = digitsOnly
                            }
                        },
                        label = { Text("Pincode") },
                        modifier = Modifier.weight(1f).focusRequester(pincodeFocusRequester).onFocusChanged { onFieldFocusChanged("pincode", it.isFocused) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isPincodeError,
                        supportingText = if (isPincodeError) { { Text("Invalid", color = MaterialTheme.colorScheme.error) } } else null
                    )
                }

                val isIncomeError = shouldShowError("monthlyIncome", monthlyIncome, { it.isNotBlank() && (it.toDoubleOrNull() ?: 0.0) > 0.0 })
                OutlinedTextField(
                    value = monthlyIncome.replace("\"", "").replace("'", "").filter { it.isDigit() },
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.replace("\"", "").replace("'", "").filter { it.isDigit() }
                        val parsed = digitsOnly.toDoubleOrNull() ?: 0.0
                        if (parsed <= 10000000.0) { // cap at 1 crore
                            monthlyIncome = digitsOnly
                        }
                        onFieldInteracted("monthlyIncome")
                    },
                    label = { Text(stringResource(Res.string.monthly_income)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(monthlyIncomeFocusRequester).onFocusChanged { onFieldFocusChanged("monthlyIncome", it.isFocused) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    isError = isIncomeError,
                    supportingText = if (isIncomeError) { { Text(stringResource(Res.string.monthly_income_invalid), color = MaterialTheme.colorScheme.error) } } else null
                )

                val isResidentialStatusError = shouldShowError("residentialStatus", residentialStatus) { it == "yes" }
                Text("Are you an Indian Resident?", style = MaterialTheme.typography.bodyLarge, color = if (isResidentialStatusError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ContainedButtonBox(
                    options = listOf("yes", "no"),
                    selectedOption = residentialStatus,
                    onOptionSelected = { 
                        residentialStatus = it
                        onFieldInteracted("residentialStatus")
                    },
                    showError = isResidentialStatusError
                )

                val isNationalityError = shouldShowError("nationality", nationality) { it == "yes" }
                Text("Are you an Indian National?", style = MaterialTheme.typography.bodyLarge, color = if (isNationalityError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ContainedButtonBox(
                    options = listOf("yes", "no"),
                    selectedOption = nationality,
                    onOptionSelected = { 
                        nationality = it
                        onFieldInteracted("nationality")
                    },
                    showError = isNationalityError
                )

                val isPoliticallyExposedError = shouldShowError("politicallyExposed", politicallyExposed) { it == "no" }
                Text("Are you a Politically Exposed Person?", style = MaterialTheme.typography.bodyLarge, color = if (isPoliticallyExposedError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ContainedButtonBox(
                    options = listOf("yes", "no"),
                    selectedOption = politicallyExposed,
                    onOptionSelected = { 
                        politicallyExposed = it
                        onFieldInteracted("politicallyExposed")
                    },
                    showError = isPoliticallyExposedError
                )

                // Validation Summary for missing fields
                if (isConfirmed && missingFields.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Please fill in the required fields:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            missingFields.forEach { field ->
                                Text(
                                    text = "• $field",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                val isConfirmedError = shouldShowError("isConfirmed", if (isConfirmed) "yes" else "no") { it == "yes" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isConfirmed,
                        onCheckedChange = {
                            isConfirmed = it
                            onFieldInteracted("isConfirmed")
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (isConfirmedError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "I confirm that the above details are correct and I am a tax resident of India.",
                        color = if (isConfirmedError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isConfirmedError) {
                    Text(
                        text = "Field is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }

                if (validationMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = validationMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (locationStatus != null) {
                    val isPermissionMissing = locationStatus == "Location permission is required to verify your address. Please grant location access."
                    val isGpsOff = locationStatus == "Location services/GPS are disabled. Please enable location services in Settings."
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = locationStatus!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isPermissionMissing || isGpsOff) {
                                Text(
                                    text = stringResource(Res.string.location_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Button(
                                    onClick = { platformActions.openAppSettings() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Settings", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        visitedFields = fieldOrder.toSet()
                        touchedFields = fieldOrder.toSet()

                        val parsedIncome = monthlyIncome.toDoubleOrNull() ?: 0.0
                        val isFormValid = fatherName.isNotBlank() &&
                                gender.isNotBlank() &&
                                maritalStatus.isNotBlank() &&
                                occupationType.isNotBlank() &&
                                placeOfBirth.isNotBlank() &&
                                addressLine1.isNotBlank() &&
                                addressLine2.isNotBlank() &&
                                addressLine3.isNotBlank() &&
                                city.isNotBlank() &&
                                pincode.length == 6 && pincode.all { it.isDigit() } &&
                                residentialStatus == "yes" &&
                                nationality == "yes" &&
                                politicallyExposed == "no" &&
                                parsedIncome > 0.0 &&
                                isConfirmed &&
                                validationMessage == null

                        if (!isFormValid) {
                            showValidationErrors = true
                            val firstMissing = fieldOrder.firstOrNull { field ->
                                when (field) {
                                    "fatherName" -> fatherName.isBlank()
                                    "placeOfBirth" -> placeOfBirth.isBlank()
                                    "addressLine1" -> addressLine1.isBlank()
                                    "addressLine2" -> addressLine2.isBlank()
                                    "addressLine3" -> addressLine3.isBlank()
                                    "city" -> city.isBlank()
                                    "pincode" -> pincode.length != 6 || !pincode.all { it.isDigit() }
                                    "monthlyIncome" -> monthlyIncome.isBlank() || (monthlyIncome.toDoubleOrNull() ?: 0.0) <= 0.0
                                    else -> false
                                }
                            }
                            if (firstMissing != null) {
                                when (firstMissing) {
                                    "fatherName" -> fatherNameFocusRequester.requestFocus()
                                    "placeOfBirth" -> placeOfBirthFocusRequester.requestFocus()
                                    "addressLine1" -> addressLine1FocusRequester.requestFocus()
                                    "addressLine2" -> addressLine2FocusRequester.requestFocus()
                                    "addressLine3" -> addressLine3FocusRequester.requestFocus()
                                    "city" -> cityFocusRequester.requestFocus()
                                    "pincode" -> pincodeFocusRequester.requestFocus()
                                    "monthlyIncome" -> monthlyIncomeFocusRequester.requestFocus()
                                }
                            } else {
                                scope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                            }
                            return@Button
                        }
                        
                        if (latitude == null || longitude == null) {
                            scope.launch {
                                val status = permissionManager.checkStatus()
                                if (!status.locationGranted) {
                                    platformLog("AdditionalKycScreen: Location permission not granted - requesting permission")
                                    locationStatus = "Location permission is required to verify your address. Please grant location access."
                                    val granted = permissionManager.requestLocation()
                                    if (!granted) {
                                        return@launch
                                    }
                                }

                                val updatedStatus = permissionManager.checkStatus()
                                if (!updatedStatus.gpsEnabled) {
                                    platformLog("AdditionalKycScreen: Location services are disabled")
                                    locationStatus = "Location services/GPS are disabled. Please enable location services in Settings."
                                    return@launch
                                }

                                isSubmitting = true
                                isFetchingLocation = true
                                locationStatus = null

                                val coords = locationProvider.getCurrentLocation()
                                latitude = coords?.latitude
                                longitude = coords?.longitude
                                isFetchingLocation = false

                                if (latitude == null || longitude == null) {
                                    platformLog("AdditionalKycScreen: Failed to fetch location coordinates")
                                    locationStatus = "Unable to determine your location. Please check your GPS signal and try again."
                                    isSubmitting = false
                                    return@launch
                                }

                                platformLog("AdditionalKycScreen: 📍 Fetched Location - Lat: $latitude, Lon: $longitude")

                                // Proceed with submission after location is fetched
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
                                    longitude = longitude,
                                    latitude = latitude
                                )
                            }
                        } else {
                            isSubmitting = true
                            platformLog("AdditionalKycScreen: 📍 Using Cached Location - Lat: $latitude, Lon: $longitude")
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
                                longitude = longitude,
                                latitude = latitude
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = validationMessage == null && !isSubmitting && !isFetchingLocation,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSubmitting || isFetchingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Submit Details", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (submitResult?.isLoading == true) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).zIndex(10f)) {
                LoadingScreen(text = "Submitting...", modifier = Modifier.fillMaxSize())
            }
        }
    }
    
    val nextScr by viewModel.nextScreen.collectAsState()

    LaunchedEffect(nextScr) {
        if (nextScr != null) {
            onNext(nextScr, kycAttemptId)
        }
    }

    LaunchedEffect(submitResult) {
        if (submitResult is Resource.Success || submitResult is Resource.Error) {
            isSubmitting = false
        }
    }
}
