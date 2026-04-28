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
            if (incomeSlab.isBlank()) incomeSlab = uiState["annualIncome"]?.toString() ?: ""
            if (city.isBlank()) city = uiState["city"]?.toString() ?: ""
            if (pincode.isBlank()) pincode = uiState["pincode"]?.toString() ?: ""
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

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it.uppercase() },
                    label = { Text("Father's Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                    isError = showValidationErrors && fatherName.isBlank()
                )

                Text("Gender", style = MaterialTheme.typography.bodyLarge)
                ContainedButtonBox(
                    options = genderOptions,
                    selectedOption = gender,
                    onOptionSelected = { gender = it },
                    showError = showValidationErrors && gender.isBlank()
                )

                Text("Marital Status", style = MaterialTheme.typography.bodyLarge)
                ContainedButtonBox(
                    options = maritalOptions,
                    selectedOption = maritalStatus,
                    onOptionSelected = { maritalStatus = it },
                    showError = showValidationErrors && maritalStatus.isBlank()
                )

                OutlinedTextField(
                    value = placeOfBirth,
                    onValueChange = { placeOfBirth = it },
                    label = { Text("Place of Birth") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    isError = showValidationErrors && placeOfBirth.isBlank()
                )

                ExposedDropdownFieldWithDisplay(
                    label = "Occupation Type",
                    selected = occupationType,
                    options = occupationOptions,
                    displayMap = occupationOptions.associateWith { it.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } },
                    onSelect = { occupationType = it },
                    showError = showValidationErrors && occupationType.isBlank()
                )

                OutlinedTextField(
                    value = addressLine1,
                    onValueChange = { addressLine1 = it },
                    label = { Text("Address Line 1") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = showValidationErrors && addressLine1.isBlank()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        isError = showValidationErrors && city.isBlank()
                    )
                    OutlinedTextField(
                        value = pincode,
                        onValueChange = { if (it.length <= 6) pincode = it },
                        label = { Text("Pincode") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showValidationErrors && pincode.length != 6
                    )
                }

                ExposedDropdownFieldWithDisplay(
                    label = "Annual Income",
                    selected = incomeSlab,
                    options = incomeOptions,
                    displayMap = incomeOptions.associateWith { it.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } },
                    onSelect = { incomeSlab = it },
                    showError = showValidationErrors && incomeSlab.isBlank()
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

        if (submitResult is Resource.Loading<*>) {
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
        if (submitResult != null && !submitResult!!.contains("Failed")) {
            isSubmitting = false
        } else if (submitResult != null && submitResult!!.contains("Failed")) {
            isSubmitting = false
        }
    }
}
