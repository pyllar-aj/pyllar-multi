package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.presentation.ui.components.HierarchicalDatePicker

@Composable
fun CheckPanPopulatedDetailsScreen(
    initialPan: String = "",
    initialName: String = "",
    initialGender: String = "",
    initialDob: String = "",
    initialFatherName: String = "",
    initialMaritalStatus: String = "",
    initialPermanentAddress: String = "",
    initialCorrespondenceAddress: String = "",
    onSubmit: (name: String, gender: String, dateOfBirth: String, fatherName: String, 
              maritalStatus: String, permanentAddress: String, correspondenceAddress: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Initialize fields with initial data
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(initialGender) }
    var dateOfBirth by remember { mutableStateOf(initialDob) }
    var fatherName by remember { mutableStateOf(initialFatherName) }
    var maritalStatus by remember { mutableStateOf(initialMaritalStatus) }
    var permanentAddress by remember { mutableStateOf(initialPermanentAddress) }
    var correspondenceAddress by remember { mutableStateOf(initialCorrespondenceAddress) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerStep by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    // Helper to check if form is valid
    val isFormValid = remember(name, gender, dateOfBirth, maritalStatus) {
        name.isNotBlank() && 
        gender.isNotBlank() && 
        dateOfBirth.isNotBlank() && 
        maritalStatus.isNotBlank()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        
        // Header
        Text(
            text = "Verify Your Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Please review and confirm the details populated from your PAN card",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // PAN validation status
        if (initialPan.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = "PAN Verified Successfully",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "PAN: $initialPan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Form fields
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            isError = name.isBlank()
        )
        
        OutlinedTextField(
            value = gender,
            onValueChange = { gender = it },
            label = { Text("Gender") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            isError = gender.isBlank()
        )
        
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { dateOfBirth = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
                }
            },
            interactionSource = remember { MutableInteractionSource() }.also { src ->
                LaunchedEffect(src) {
                    src.interactions.collect { if (it is PressInteraction.Release) showDatePicker = true }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = dateOfBirth.isBlank()
        )
        
        if (showDatePicker) {
            HierarchicalDatePicker(
                onDateSelected = { y: Int, m: Int, d: Int ->
                    dateOfBirth = "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                    showDatePicker = false
                    datePickerStep = 0; selectedYear = null; selectedMonth = null; selectedDay = null
                },
                onDismiss = {
                    showDatePicker = false
                    datePickerStep = 0; selectedYear = null; selectedMonth = null; selectedDay = null
                },
                currentStep = datePickerStep,
                selectedYear = selectedYear, selectedMonth = selectedMonth, selectedDay = selectedDay,
                onStepChange = { step: Int -> datePickerStep = step },
                onYearSelected = { year: Int -> selectedYear = year },
                onMonthSelected = { month: Int -> selectedMonth = month },
                onDaySelected = { day: Int -> selectedDay = day }
            )
        }
        
        OutlinedTextField(
            value = fatherName,
            onValueChange = { fatherName = it },
            label = { Text("Father's Name (Optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = maritalStatus,
            onValueChange = { maritalStatus = it },
            label = { Text("Marital Status") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            isError = maritalStatus.isBlank()
        )
        
        OutlinedTextField(
            value = permanentAddress,
            onValueChange = { permanentAddress = it },
            label = { Text("Permanent Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            minLines = 2,
            maxLines = 4
        )
        
        OutlinedTextField(
            value = correspondenceAddress,
            onValueChange = { correspondenceAddress = it },
            label = { Text("Correspondence Address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            minLines = 2,
            maxLines = 4
        )
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    // Reset to original state data
                    name = initialName
                    gender = initialGender
                    dateOfBirth = initialDob
                    fatherName = initialFatherName
                    maritalStatus = initialMaritalStatus
                    permanentAddress = initialPermanentAddress
                    correspondenceAddress = initialCorrespondenceAddress
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
            
            Button(
                onClick = {
                    onSubmit(name, gender, dateOfBirth, fatherName, maritalStatus, permanentAddress, correspondenceAddress)
                },
                enabled = isFormValid,
                modifier = Modifier.weight(2f)
            ) {
                Text("Continue")
            }
        }
    }
}
