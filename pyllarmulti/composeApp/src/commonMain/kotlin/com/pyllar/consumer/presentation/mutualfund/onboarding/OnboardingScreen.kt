package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.domain.models.MutualFundConstants
import com.pyllar.consumer.presentation.ui.components.StandardTextFieldNewTwo
import com.pyllar.consumer.presentation.ui.theme.TrueWhite
import com.pyllar.consumer.util.Status
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingSuccess: (Int, Int) -> Unit, // investorProfileId, investmentAccountId
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    userId: String = "",
    viewModel: OnboardingViewModel // Injected from route
) {
    val scrollState = rememberScrollState()
    
    val onboardingResult by viewModel.onboardingResult.collectAsStateWithLifecycle()
    val formData by viewModel.formData.collectAsStateWithLifecycle()
    
    val isLoading = onboardingResult?.status == Status.LOADING
    
    val panError by viewModel.panError.collectAsStateWithLifecycle()
    val dobError by viewModel.dobError.collectAsStateWithLifecycle()
    val ifscError by viewModel.ifscError.collectAsStateWithLifecycle()
    val pincodeError by viewModel.pincodeError.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Handle onboarding result
    LaunchedEffect(onboardingResult) {
        onboardingResult?.let { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    result.data?.let { response ->
                        onShowMessage("Onboarding successful!")
                        onOnboardingSuccess(response.investorProfileId, response.investmentAccountId)
                    }
                }
                Status.ERROR -> {
                    onShowMessage(result.message ?: "Onboarding failed")
                }
                Status.LOADING -> {
                    // Loading state handled in UI
                }
            }
        }
    }
    
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = statusBarPadding + 16.dp)
            .padding(bottom = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("← Back")
            }
            Text(
                text = "Investor Onboarding",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.width(56.dp))
        }
        
        // Personal Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                
                StandardTextFieldNewTwo(
                    text = formData.firstName,
                    onValueChange = { viewModel.updateFormData(formData.copy(firstName = it)) },
                    hint = "First Name *"
                )
                
                StandardTextFieldNewTwo(
                    text = formData.lastName,
                    onValueChange = { viewModel.updateFormData(formData.copy(lastName = it)) },
                    hint = "Last Name *"
                )
                
                StandardTextFieldNewTwo(
                    text = formData.middleName,
                    onValueChange = { viewModel.updateFormData(formData.copy(middleName = it)) },
                    hint = "Middle Name (Optional)"
                )
                
                // Gender Dropdown
                var genderExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = formData.gender,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Gender *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        MutualFundConstants.GENDER_OPTIONS.forEach { gender ->
                            DropdownMenuItem(
                                text = { Text(gender) },
                                onClick = {
                                    viewModel.updateFormData(formData.copy(gender = gender))
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }
                
                StandardTextFieldNewTwo(
                    text = formData.panNumber,
                    onValueChange = {
                        val upper = it.uppercase().take(10)
                        viewModel.updateFormData(formData.copy(panNumber = upper))
                        viewModel.validatePan(upper)
                    },
                    hint = "PAN Number *",
                    maxLength = 10,
                    keyboardType = KeyboardType.Ascii,
                    modifier = Modifier.fillMaxWidth()
                )
                if (panError != null) {
                    Text(text = panError ?: "", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StandardTextFieldNewTwo(
                        text = formData.dateOfBirth,
                        onValueChange = {}, // Prevent manual entry
                        hint = "Date of Birth (YYYY-MM-DD) *",
                        maxLength = 10,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true },
                    )
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
                    }
                }
                if (dobError != null) {
                    Text(text = dobError ?: "", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val instant = Instant.fromEpochMilliseconds(millis)
                                    val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                                    val dateStr = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
                                    viewModel.updateFormData(formData.copy(dateOfBirth = dateStr))
                                    viewModel.validateDob(dateStr)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
        }
        
        // Address Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Address Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                
                StandardTextFieldNewTwo(
                    text = formData.addressLine1,
                    onValueChange = { viewModel.updateFormData(formData.copy(addressLine1 = it)) },
                    hint = "Address Line 1 *"
                )
                
                StandardTextFieldNewTwo(
                    text = formData.city,
                    onValueChange = { viewModel.updateFormData(formData.copy(city = it)) },
                    hint = "City *"
                )
                
                // State Dropdown
                var stateExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = stateExpanded,
                    onExpandedChange = { stateExpanded = !stateExpanded }
                ) {
                    OutlinedTextField(
                        value = formData.state,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("State *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = stateExpanded,
                        onDismissRequest = { stateExpanded = false }
                    ) {
                        MutualFundConstants.STATES.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state) },
                                onClick = {
                                    viewModel.updateFormData(formData.copy(state = state))
                                    stateExpanded = false
                                }
                            )
                        }
                    }
                }
                
                StandardTextFieldNewTwo(
                    text = formData.pincode,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }.take(6)
                        viewModel.updateFormData(formData.copy(pincode = filtered))
                        viewModel.validatePincode(filtered)
                    },
                    hint = "Pincode *",
                    keyboardType = KeyboardType.Number,
                    maxLength = 6
                )
                if (pincodeError != null) {
                    Text(text = pincodeError ?: "", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        // Professional Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Professional Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                // Occupation Dropdown
                var occupationExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = occupationExpanded,
                    onExpandedChange = { occupationExpanded = !occupationExpanded }
                ) {
                    OutlinedTextField(
                        value = formData.occupation,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Occupation *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occupationExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = occupationExpanded,
                        onDismissRequest = { occupationExpanded = false }
                    ) {
                        MutualFundConstants.OCCUPATIONS.forEach { occupation ->
                            DropdownMenuItem(
                                text = { Text(occupation) },
                                onClick = {
                                    viewModel.updateFormData(formData.copy(occupation = occupation))
                                    occupationExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Income Range Dropdown
                var incomeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = incomeExpanded,
                    onExpandedChange = { incomeExpanded = !incomeExpanded }
                ) {
                    OutlinedTextField(
                        value = formData.incomeRange,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Income Range *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = incomeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = incomeExpanded,
                        onDismissRequest = { incomeExpanded = false }
                    ) {
                        MutualFundConstants.INCOME_RANGES.forEach { income ->
                            DropdownMenuItem(
                                text = { Text(income.replace("_", " ")) },
                                onClick = {
                                    viewModel.updateFormData(formData.copy(incomeRange = income))
                                    incomeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Bank Account Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bank Account Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                StandardTextFieldNewTwo(
                    text = formData.bankAccountNumber,
                    onValueChange = { viewModel.updateFormData(formData.copy(bankAccountNumber = it)) },
                    hint = "Account Number *",
                    keyboardType = KeyboardType.Number
                )
                
                StandardTextFieldNewTwo(
                    text = formData.ifscCode,
                    onValueChange = {
                        val upper = it.uppercase().take(11)
                        viewModel.updateFormData(formData.copy(ifscCode = upper))
                        viewModel.validateIfsc(upper)
                    },
                    hint = "IFSC Code *",
                    maxLength = 11,
                    keyboardType = KeyboardType.Ascii
                )
                if (ifscError != null) {
                    Text(text = ifscError ?: "", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
                
                StandardTextFieldNewTwo(
                    text = formData.accountHolderName,
                    onValueChange = { viewModel.updateFormData(formData.copy(accountHolderName = it)) },
                    hint = "Account Holder Name *"
                )
                
                StandardTextFieldNewTwo(
                    text = formData.bankName,
                    onValueChange = { viewModel.updateFormData(formData.copy(bankName = it)) },
                    hint = "Bank Name *"
                )
                
                // Account Type Dropdown
                var accountTypeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountTypeExpanded,
                    onExpandedChange = { accountTypeExpanded = !accountTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = formData.accountType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Account Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = accountTypeExpanded,
                        onDismissRequest = { accountTypeExpanded = false }
                    ) {
                        MutualFundConstants.ACCOUNT_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    viewModel.updateFormData(formData.copy(accountType = type))
                                    accountTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Submit Button
        Button(
            onClick = { 
                val actualUserId = if (userId.isNotBlank()) userId else "temp_user_id"
                viewModel.onboardInvestor(actualUserId)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = TrueWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Text(
                    text = "Complete Onboarding",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
        
        // Error message
        if (onboardingResult?.status == Status.ERROR) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = onboardingResult?.message ?: "An error occurred",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
