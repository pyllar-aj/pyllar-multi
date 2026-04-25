package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pyllar.consumer.util.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinDetailsScreen(
    onNext: (String?, String?) -> Unit, // Accept (nextScreen, kycAttemptId)
    onNavigateToHelp: () -> Unit = {},
    viewModel: MinDetailsViewModel, // Injected
    userId: String,
    pan: String,
    email: String,
    phone: String,
    token: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    // Optimized state management to prevent flickering
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var displayPan by remember { mutableStateOf(pan) } 
    var panHolderName by remember { mutableStateOf<String?>(null) }
    var isNameEditable by remember { mutableStateOf(true) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var isPolling by remember { mutableStateOf(false) }
    var pollMessage by remember { mutableStateOf<String?>(null) }
    var preVerificationId by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var pollingStartTime by remember { mutableStateOf<Long?>(null) }
    
    // UI state - stable to prevent unnecessary recomposition
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerStep by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    val scrollState = rememberScrollState()
    val timeoutState = rememberTimeoutState("MinDetails", "continue")
    
    // Fresh ViewModel state observation
    val minDetailsState by viewModel.minDetailsState.collectAsStateWithLifecycle()
    val prefillData by viewModel.prefillData.collectAsStateWithLifecycle()

    // Auto-fill from prepopulated data (MinDetails)
    LaunchedEffect(prefillData) {
        val prepopulatedName = prefillData["name"] as? String
        if (!prepopulatedName.isNullOrBlank() && name.isBlank()) {
            name = prepopulatedName
        }
        
        val prepopulatedDob = prefillData["dob"] as? String
        if (!prepopulatedDob.isNullOrBlank() && dob.isBlank()) {
            dob = prepopulatedDob
            // Reset pickers state if needed
            selectedYear = dob.substringBefore("-").toIntOrNull()
        }
        
        // Update PAN from API if available
        val prepopulatedPan = prefillData["pan"] as? String
        if (!prepopulatedPan.isNullOrBlank() && displayPan.isBlank()) {
            displayPan = prepopulatedPan
        }
    }
    
    fun submitMinDetails() {
        if (name.isBlank() || dob.isBlank()) {
            if (dob.isBlank()) {
                dobError = "Please select your date of birth"
            }
            return
        }
        
        if (isSubmitting) return
        
        nameError = null
        dobError = null
        isSubmitting = true
        
        scope.launch {
            viewModel.submitMinimalDetails(
                userId = userId,
                name = name,
                panNumber = pan,
                dateOfBirth = dob,
                emailAddress = email,
                mobileCountryCode = "+91",
                mobileNumber = phone.takeLast(10),
                token = token,
                preVerificationId = preVerificationId
            )
        }
    }

    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90000) 
            if (isSubmitting && minDetailsState !is Resource.Success && minDetailsState !is Resource.Error) {
                isSubmitting = false
            }
        }
    }
    
    LaunchedEffect(minDetailsState) {
        when (val state = minDetailsState) {
            is Resource.Success -> {
                isSubmitting = false
                val navigation = state.navigation
                val navigationAction = navigation?.action
                val nextScreen = navigation?.nextScreen
                
                when (navigationAction) {
                    NavigationAction.POLL -> {
                        val receivedPreVerificationId = try {
                            navigation?.params?.get("preVerificationId") as? String
                        } catch (e: Exception) { null }
                        
                        if (receivedPreVerificationId != null) {
                            preVerificationId = receivedPreVerificationId
                        }
                        
                        val delayMsFromParams = try {
                            when {
                                navigation?.params?.get("delayMs") is Number -> (navigation.params["delayMs"] as Number).toLong()
                                navigation?.params?.get("delay_seconds") is Number -> ((navigation.params["delay_seconds"] as Number).toLong() * 1000L)
                                navigation?.params?.get("retry_after_sec") is Number -> ((navigation.params["retry_after_sec"] as Number).toLong() * 1000L)
                                navigation?.params?.get("retry_after_ms") is Number -> (navigation.params["retry_after_ms"] as Number).toLong()
                                navigation?.params?.get("poll_interval_ms") is Number -> (navigation.params["poll_interval_ms"] as Number).toLong()
                                else -> null
                            }
                        } catch (e: Exception) { null }
                        val delayMs = delayMsFromParams ?: 5000L
                        
                        pollMessage = navigation?.params?.get("message") as? String ?: "Verification in progress. Please wait..."
                        
                        if (!isPolling) {
                            isPolling = true
                        }
                    }
                    NavigationAction.NAVIGATE -> {
                        val kycAttemptId = state.data?.kycAttemptId ?: ""
                        onNext(nextScreen, kycAttemptId)
                    }
                    null -> { }
                    else -> { }
                }
            }
            is Resource.Error -> {
                isSubmitting = false
                timeoutState.triggerTimeout()
                
                if (isPolling) {
                    isPolling = false
                    pollMessage = null
                }
                
                val nameFieldError = state.fieldErrors?.find { it.field == "name" }
                val dobFieldError = state.fieldErrors?.find { it.field == "dateOfBirth" || it.field == "dob" }
                
                nameError = nameFieldError?.message
                dobError = dobFieldError?.message
            }
            is Resource.Loading -> { }
            null -> { }
        }
    }

    LaunchedEffect(isPolling) {
        pollingStartTime = currentTimeMillis() // KMP-safe on both platforms
        var retryCount = 0
        var currentDelayMs = 5000L
        while (isPolling) {
            delay(currentDelayMs)
            
            if (!isPolling) break
            val elapsed = pollingStartTime?.let { currentTimeMillis() - it } ?: 0L
            
            viewModel.submitMinimalDetails(
                userId = userId,
                name = name,
                panNumber = pan,
                dateOfBirth = dob,
                emailAddress = email,
                mobileCountryCode = "+91",
                mobileNumber = phone.takeLast(10),
                token = token,
                preVerificationId = preVerificationId
            )
            
            val latestState = minDetailsState
            if (latestState is Resource.Success && latestState.navigation?.action == NavigationAction.POLL) {
                val newDelay = try {
                    latestState.navigation?.params?.get("delayMs") as? Number
                } catch (e: Exception) { null }
                if (newDelay != null) {
                    currentDelayMs = newDelay.toLong()
                }
            }
        }
    }
    
    val showLoadingOverlay = (minDetailsState is Resource.Loading || isPolling) || isSubmitting

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onNavigateToHelp) {
                    Text(
                        text = "Help",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .zIndex(2f)
            ) {
                OnboardingStepper(
                    currentStep = 0, 
                    completedStep = if (minDetailsState is Resource.Success) 1 else 0,
                    currentScreenRoute = ScreenNames.MIN_DETAILS
                )
            }
        
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Personal Details",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    if (isNameEditable) {
                        name = it.uppercase()
                        nameError = null
                    }
                },
                label = { Text("Full Name") },
                singleLine = true,
                readOnly = !isNameEditable,
                enabled = isNameEditable,
                isError = nameError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(0.95f),
                colors = if (!isNameEditable) {
                    OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextFieldDefaults.colors()
                }
            )
            Text(
                text = "As per PAN record",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp)
            )
            
            if (!isNameEditable && panHolderName != null) {
                Text(
                    text = "Name pre-filled from PAN verification",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (nameError != null) {
                Text(
                    text = nameError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    label = { Text("Date of Birth") },
                    singleLine = true,
                    readOnly = true,
                    isError = dobError != null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is PressInteraction.Release) {
                                        showDatePicker = true
                                    }
                                }
                            }
                        }
                )
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Pick Date")
                }
            }
            
            if (dobError != null) {
                Text(
                    text = dobError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (showDatePicker) {
                MinDetailsDatePicker(
                    onDateSelected = { year, month, day ->
                        dob = "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                        dobError = null
                        showDatePicker = false
                        datePickerStep = 0
                        selectedYear = null
                        selectedMonth = null
                        selectedDay = null
                    },
                    onDismiss = {
                        showDatePicker = false
                        datePickerStep = 0
                        selectedYear = null
                        selectedMonth = null
                        selectedDay = null
                    },
                    currentStep = datePickerStep,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    selectedDay = selectedDay,
                    onStepChange = { step -> datePickerStep = step },
                    onYearSelected = { year -> selectedYear = year },
                    onMonthSelected = { month -> selectedMonth = month },
                    onDaySelected = { day -> selectedDay = day }
                )
            }
            
            if (displayPan.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth(0.95f)) {
                    OutlinedTextField(
                        value = displayPan,
                        onValueChange = {},
                        label = { Text("PAN Number") },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "PAN verified from earlier step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isPolling && pollMessage != null && !showLoadingOverlay) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = pollMessage!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            minDetailsState?.let { state ->
                if (state is Resource.Error) {
                    val errorMsg = state.message ?: ""
                    val isNetworkError = state.isNetworkError ||
                                       errorMsg.contains("Network", ignoreCase = true) ||
                                       errorMsg.contains("timeout", ignoreCase = true) ||
                                       errorMsg.contains("connection", ignoreCase = true) ||
                                       errorMsg.contains("Failed to connect", ignoreCase = true) ||
                                       errorMsg.contains("IOException", ignoreCase = true)
                    
                    val errorMessage = if (isNetworkError) {
                        "Check your Internet connection and try again"
                    } else if (state.fieldErrors.isNullOrEmpty()) {
                        "Something went wrong. Please try again."
                    } else {
                        "Please check the highlighted fields and try again."
                    }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            TimeoutButton(
                onClick = { submitMinDetails() },
                enabled = name.isNotBlank() && dob.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                timeoutState = timeoutState,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Text("Continue")
            }
        }
        }
        
        if (showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
            ) {
                LoadingScreen(
                    text = if (isPolling && pollMessage != null) pollMessage!! else "Submitting, please wait...",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MinDetailsDatePicker(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
    currentStep: Int,
    selectedYear: Int?,
    selectedMonth: Int?,
    selectedDay: Int?,
    onStepChange: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onDaySelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (currentStep) {
                            0 -> "Year"
                            1 -> "Month"
                            2 -> "Day"
                            else -> "Date"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    
                    if (currentStep > 0) {
                        TextButton(
                            onClick = { onStepChange(currentStep - 1) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Back")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (currentStep) {
                    0 -> YearPickerForMinDetails(
                        onYearSelected = { year ->
                            onYearSelected(year)
                            onStepChange(1)
                        }
                    )
                    1 -> MonthPickerForMinDetails(
                        selectedYear = selectedYear,
                        onMonthSelected = { month ->
                            onMonthSelected(month)
                            onStepChange(2)
                        }
                    )
                    2 -> DayPickerForMinDetails(
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        onDaySelected = { day ->
                            onDaySelected(day)
                            if (selectedYear != null && selectedMonth != null) {
                                onDateSelected(selectedYear, selectedMonth, day)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun YearPickerForMinDetails(onYearSelected: (Int) -> Unit) {
    val currentYear = getCurrentYear()
    val maxYear = currentYear - 18
    val years = (1950..maxYear).toList().reversed()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(years.size) { index ->
            val year = years[index]
            Card(
                modifier = Modifier
                    .aspectRatio(1.5f)
                    .clickable { onYearSelected(year) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonthPickerForMinDetails(
    selectedYear: Int?,
    onMonthSelected: (Int) -> Unit
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedYear != null) {
            Text(
                text = "Year: $selectedYear",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val maxYearVal = getCurrentYear() - 18
            val maxMonthVal = getCurrentMonth()
            val allowedMonthsCount = if (selectedYear != null && selectedYear == maxYearVal) maxMonthVal else months.size
            items(allowedMonthsCount) { index ->
                Card(
                    modifier = Modifier
                        .aspectRatio(1.5f)
                        .clickable { onMonthSelected(index + 1) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = months[index],
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayPickerForMinDetails(
    selectedYear: Int?,
    selectedMonth: Int?,
    onDaySelected: (Int) -> Unit
) {
    val nowYear = getCurrentYear()
    val nowMonth = getCurrentMonth()
    val nowDay = getCurrentDay()
    val maxYear = nowYear - 18

    // Basic days in month calculation for KMP
    val computedDaysInMonth = if (selectedYear != null && selectedMonth != null) {
        when (selectedMonth) {
            2 -> if (selectedYear % 4 == 0 && (selectedYear % 100 != 0 || selectedYear % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    } else {
        31
    }
    
    val daysInMonth = if (
        selectedYear != null && selectedMonth != null &&
        selectedYear == maxYear && selectedMonth == nowMonth
    ) {
        minOf(computedDaysInMonth, nowDay)
    } else computedDaysInMonth
    
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedYear != null && selectedMonth != null) {
            Text(
                text = "${monthNames[selectedMonth - 1]} $selectedYear",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(daysInMonth) { index ->
                val day = index + 1
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onDaySelected(day) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
