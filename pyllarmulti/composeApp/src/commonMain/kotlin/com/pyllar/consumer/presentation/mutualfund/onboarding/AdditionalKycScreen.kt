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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalKycScreen(
    kycAttemptId: String,
    token: String,
    onNext: (String?, String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    viewModel: AdditionalKycViewModel = koinInject()
) {
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
    var incomeSlab by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("yes") }
    var politicallyExposed by remember { mutableStateOf("no") }
    var isConfirmed by remember { mutableStateOf(false) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var touchedFields by remember { mutableStateOf(setOf<String>()) }
    var visitedFields by remember { mutableStateOf(setOf<String>()) }
    var currentFocusedField by remember { mutableStateOf<String?>(null) }

    val fieldOrder = listOf(
        "fatherName", "gender", "maritalStatus", "placeOfBirth", "occupationType",
        "addressLine1", "addressLine2", "addressLine3", "city", "pincode",
        "incomeSlab", "residentialStatus", "nationality", "politicallyExposed", "isConfirmed"
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
    val scope = rememberCoroutineScope()

    val uiState by viewModel.prefillData.collectAsState()
    val isLoadingData by viewModel.isLoadingScreenData.collectAsState()
    val submitResult by viewModel.submitResult.collectAsState()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("AdditionalKyc")
    }

    LaunchedEffect(uiState) {
        if (uiState.isNotEmpty()) {
            if (fatherName.isBlank()) fatherName = uiState["fatherName"]?.toString()?.uppercase() ?: ""
            if (gender.isBlank()) gender = uiState["gender"]?.toString() ?: ""
            if (maritalStatus.isBlank()) maritalStatus = uiState["maritalStatus"]?.toString() ?: ""
            if (occupationType.isBlank()) occupationType = uiState["occupationType"]?.toString() ?: ""
            if (placeOfBirth.isBlank()) placeOfBirth = uiState["placeOfBirth"]?.toString() ?: ""
            if (incomeSlab.isBlank()) incomeSlab = uiState["incomeSlab"]?.toString() ?: uiState["annualIncome"]?.toString() ?: ""
            if (city.isBlank()) city = uiState["city"]?.toString() ?: ""
            if (pincode.isBlank()) pincode = uiState["pincode"]?.toString() ?: ""
            if (addressLine1.isBlank()) addressLine1 = uiState["addressLine1"]?.toString() ?: ""
            if (addressLine2.isBlank()) addressLine2 = uiState["addressLine2"]?.toString() ?: ""
            if (addressLine3.isBlank()) addressLine3 = uiState["addressLine3"]?.toString() ?: ""
            
            // Handle yes/no status fields
            val resStat = uiState["residentialStatus"]?.toString()
            if (resStat != null) residentialStatus = resStat
            
            val natStat = uiState["nationality"]?.toString()
            if (natStat != null) nationality = natStat
            
            val pepStat = uiState["politicallyExposed"]?.toString()
            if (pepStat != null) politicallyExposed = pepStat
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
                    modifier = Modifier.fillMaxWidth().onFocusChanged { onFieldFocusChanged("fatherName", it.isFocused) },
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
                    modifier = Modifier.fillMaxWidth().onFocusChanged { onFieldFocusChanged("placeOfBirth", it.isFocused) },
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
                    modifier = Modifier.fillMaxWidth().onFocusChanged { onFieldFocusChanged("addressLine1", it.isFocused) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = isAddress1Error,
                    supportingText = if (isAddress1Error) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                val isAddress2Error = shouldShowError("addressLine2", addressLine2)
                OutlinedTextField(
                    value = addressLine2,
                    onValueChange = { addressLine2 = filterAddress(it) },
                    label = { Text("Address Line 2") },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { onFieldFocusChanged("addressLine2", it.isFocused) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = isAddress2Error,
                    supportingText = if (isAddress2Error) { { Text("Field is required", color = MaterialTheme.colorScheme.error) } } else null
                )

                val isAddress3Error = shouldShowError("addressLine3", addressLine3)
                OutlinedTextField(
                    value = addressLine3,
                    onValueChange = { addressLine3 = filterAddress(it) },
                    label = { Text("Address Line 3") },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { onFieldFocusChanged("addressLine3", it.isFocused) },
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
                        modifier = Modifier.weight(1f).onFocusChanged { onFieldFocusChanged("city", it.isFocused) },
                        isError = isCityError,
                        supportingText = if (isCityError) { { Text("Required", color = MaterialTheme.colorScheme.error) } } else null
                    )
                    val isPincodeError = shouldShowError("pincode", pincode, { it.length == 6 && it.all { it.isDigit() } })
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { if (it.length <= 6) pincode = it },
                        label = { Text("Pincode") },
                        modifier = Modifier.weight(1f).onFocusChanged { onFieldFocusChanged("pincode", it.isFocused) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isPincodeError,
                        supportingText = if (isPincodeError) { { Text("Invalid", color = MaterialTheme.colorScheme.error) } } else null
                    )
                }

                val isIncomeError = shouldShowError("incomeSlab", incomeSlab)
                ExposedDropdownFieldWithDisplay(
                    label = "Annual Income",
                    selected = incomeSlab,
                    options = incomeOptions,
                    displayMap = incomeOptions.associateWith { it.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } },
                    onSelect = { 
                        incomeSlab = it
                        onFieldInteracted("incomeSlab")
                    },
                    showError = isIncomeError
                )

                Text("Are you an Indian Resident?", style = MaterialTheme.typography.bodyLarge)
                ContainedButtonBox(
                    options = listOf("yes", "no"),
                    selectedOption = residentialStatus,
                    onOptionSelected = { 
                        residentialStatus = it
                        onFieldInteracted("residentialStatus")
                    }
                )

                Text("Are you an Indian National?", style = MaterialTheme.typography.bodyLarge)
                ContainedButtonBox(
                    options = listOf("yes", "no"),
                    selectedOption = nationality,
                    onOptionSelected = { 
                        nationality = it
                        onFieldInteracted("nationality")
                    }
                )

                Text("Are you a Politically Exposed Person?", style = MaterialTheme.typography.bodyLarge)
                ContainedButtonBox(
                    options = listOf("yes", "no"),
                    selectedOption = politicallyExposed,
                    onOptionSelected = { 
                        politicallyExposed = it
                        onFieldInteracted("politicallyExposed")
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isConfirmed, onCheckedChange = { isConfirmed = it })
                    Text("I confirm that the above details are correct and I am a tax resident of India.")
                }

                Button(
                    onClick = {
                        if (fatherName.isBlank() || gender.isBlank() || maritalStatus.isBlank() || 
                            occupationType.isBlank() || placeOfBirth.isBlank() || city.isBlank() || 
                            pincode.length != 6 || incomeSlab.isBlank() || !isConfirmed) {
                            showValidationErrors = true
                            return@Button
                        }
                        isSubmitting = true
                        viewModel.submitAdditionalKyc(
                            kycAttemptId = kycAttemptId,
                            token = token,
                            maritalStatus = maritalStatus,
                            occupationType = occupationType,
                            fatherName = fatherName,
                            annualIncome = incomeSlab,
                            isPoliticallyExposed = politicallyExposed == "yes",
                            nationalityCountry = if (nationality == "yes") "IN" else "OTHERS",
                            placeOfBirth = placeOfBirth,
                            gender = gender,
                            addressLine1 = addressLine1,
                            addressLine2 = addressLine2,
                            addressLine3 = addressLine3,
                            city = city,
                            pincode = pincode,
                            longitude = null,
                            latitude = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp)
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
