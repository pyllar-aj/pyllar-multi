package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.presentation.ui.theme.lightGreyBackground
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pyllar.consumer.util.*
import org.koin.compose.koinInject

// ── Date utilities (KMP-safe, no java.util.Calendar) ─────────────────────────

/** Returns whether [year] is a leap year. */
private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

/** Days in [month] (1-based) of [year]. */
private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (isLeapYear(year)) 29 else 28
    else -> 30
}

/** Naïve cut-off year for 18+ (current year - 18). */
private val CUTOFF_YEAR = getCurrentYear() - 18

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameDobScreen(
    onKycSubmitted: (name: String, dob: String, navigationInfo: NavigationInfo?, data: Any?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    userId: String,
    pan: String,
    email: String,
    phone: String,
    token: String
) {
    val viewModel: NameDobViewModel = koinInject()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val timeoutState = rememberTimeoutState("NameDob", "continue")

    // ── Form state ────────────────────────────────────────────────────────────
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var displayPan by remember { mutableStateOf(pan) }
    var isNameEditable by remember { mutableStateOf(true) }

    // ── Date picker state ─────────────────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerStep by remember { mutableStateOf(0) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // ── Error & loading state ─────────────────────────────────────────────────
    var nameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var genericError by remember { mutableStateOf<String?>(null) }
    var isPolling by remember { mutableStateOf(false) }
    var pollMessage by remember { mutableStateOf<String?>(null) }
    var preVerificationId by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var pollingStartTime by remember { mutableStateOf<Long?>(null) }

    val POLLING_TIMEOUT_MS = 2 * 60 * 1000L
    val MAX_POLLING_RETRIES = 24

    val kycResult by viewModel.kycResult.collectAsState()
    val prefillData by viewModel.prefillData.collectAsState()

    // ── Analytics ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("NameDob")
    }

    // ── Auto-fill from ViewModel ──────────────────────────────────────────────
    LaunchedEffect(prefillData) {
        prefillData["name"]?.let { if (it.isNotBlank() && name.isBlank()) name = it }
        prefillData["dob"]?.let { prepopDob ->
            if (prepopDob.isNotBlank() && dob.isBlank()) {
                dob = prepopDob
                val parts = prepopDob.split("-")
                if (parts.size == 3) {
                    selectedYear = parts[0].toIntOrNull()
                    selectedMonth = parts[1].toIntOrNull()
                    selectedDay = parts[2].toIntOrNull()
                }
            }
        }
        prefillData["pan"]?.let { if (it.isNotBlank() && displayPan.isBlank()) displayPan = it }
    }

    // ── Polling loop ──────────────────────────────────────────────────────────
    LaunchedEffect(isPolling) {
        if (!isPolling) { pollingStartTime = null; return@LaunchedEffect }
        pollingStartTime = currentTimeMillis() // KMP-safe on both platforms
        var retryCount = 0
        while (isPolling) {
            delay(5_000)
            if (!isPolling) break
            val elapsed = pollingStartTime?.let { currentTimeMillis() - it } ?: 0L
            if (elapsed >= POLLING_TIMEOUT_MS || retryCount >= MAX_POLLING_RETRIES) {
                isPolling = false
                pollMessage = null
                genericError = "Verification is taking longer than expected. Please try again."
                timeoutState.triggerTimeout()
                break
            }
            retryCount++
            viewModel.createMinimalKyc(
                userId = userId, name = name, panNumber = pan, dateOfBirth = dob,
                emailAddress = email, mobileCountryCode = "+91",
                mobileNumber = phone.takeLast(10), token = token,
                preVerificationId = preVerificationId
            )
        }
    }

    // ── Submission timeout ────────────────────────────────────────────────────
    LaunchedEffect(isSubmitting) {
        if (!isSubmitting) return@LaunchedEffect
        delay(POLLING_TIMEOUT_MS)
        if (isSubmitting) {
            isSubmitting = false
            genericError = "Request is taking too long. Please try again."
            timeoutState.triggerTimeout()
        }
    }

    // ── Result handler ────────────────────────────────────────────────────────
    LaunchedEffect(kycResult) {
        val result = kycResult ?: return@LaunchedEffect
        when (result) {
            is Resource.Success -> {
                val navInfo = result.navigation
                if (navInfo?.action == NavigationAction.POLL) {
                    isPolling = true
                    isSubmitting = false
                    pollMessage = navInfo.params?.get("message") as? String ?: "Verifying details..."
                    preVerificationId = navInfo.params?.get("preVerificationId") as? String
                        ?: result.data?.kycAttemptId
                } else {
                    isPolling = false; isSubmitting = false; pollMessage = null; pollingStartTime = null
                    onKycSubmitted(name, dob, navInfo, result.data)
                }
            }
            is Resource.Error -> {
                isPolling = false; isSubmitting = false; pollMessage = null; pollingStartTime = null
                timeoutState.triggerTimeout()
                nameError = null; dobError = null; genericError = null
                var hasSpecific = false
                result.fieldErrors?.forEach { fe ->
                    when {
                        fe.field.equals("name", ignoreCase = true) -> { nameError = fe.message; hasSpecific = true }
                        fe.field.lowercase() in listOf("dateofbirth", "dob", "date_of_birth") -> { dobError = fe.message; hasSpecific = true }
                    }
                }
                if (!hasSpecific) genericError = result.message ?: "Verification failed. Please try again."
            }
            is Resource.Loading -> Unit
        }
    }

    fun submitKyc() {
        if (isSubmitting) return
        nameError = null; dobError = null; genericError = null
        isSubmitting = true
        scope.launch {
            viewModel.createMinimalKyc(
                userId = userId, name = name, panNumber = pan, dateOfBirth = dob,
                emailAddress = email, mobileCountryCode = "+91",
                mobileNumber = phone.takeLast(10), token = token,
                preVerificationId = preVerificationId
            )
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Help row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onNavigateToHelp) {
                    Text("Help", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Stepper
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp, tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
            ) {
                OnboardingStepper(currentStep = 0, completedStep = 0, currentScreenRoute = ScreenNames.NAME_DOB)
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f).fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Personal Details",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Generic error
                if (genericError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(0.95f)
                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(genericError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Name field
                Column(modifier = Modifier.fillMaxWidth(0.95f)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            if (isNameEditable) {
                                name = it.filter { c -> c.isLetter() || c == ' ' }.uppercase()
                                nameError = null
                            }
                        },
                        label = { Text("Full Name") },
                        singleLine = true,
                        readOnly = !isNameEditable,
                        enabled = isNameEditable,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = nameError != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (!isNameEditable) OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.lightGreyBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) else OutlinedTextFieldDefaults.colors()
                    )
                    if (nameError != null) {
                        Text(nameError!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp))
                    }
                }

                Text(
                    text = "Please ensure the name matches your PAN records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // DOB field
                Column(modifier = Modifier.fillMaxWidth(0.95f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            label = { Text("Date of Birth") },
                            singleLine = true, readOnly = true,
                            isError = dobError != null,
                            modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            interactionSource = remember { MutableInteractionSource() }.also { src ->
                                LaunchedEffect(src) {
                                    src.interactions.collect { if (it is PressInteraction.Release) showDatePicker = true }
                                }
                            }
                        )
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
                        }
                    }
                    if (dobError != null) {
                        Text(dobError!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp))
                    }
                }

                if (showDatePicker) {
                    HierarchicalDatePicker(
                        onDateSelected = { y, m, d ->
                            dob = "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                            dobError = null
                            showDatePicker = false
                            datePickerStep = 0; selectedYear = null; selectedMonth = null; selectedDay = null
                        },
                        onDismiss = {
                            showDatePicker = false
                            datePickerStep = 0; selectedYear = null; selectedMonth = null; selectedDay = null
                        },
                        currentStep = datePickerStep,
                        selectedYear = selectedYear, selectedMonth = selectedMonth, selectedDay = selectedDay,
                        onStepChange = { datePickerStep = it },
                        onYearSelected = { selectedYear = it },
                        onMonthSelected = { selectedMonth = it },
                        onDaySelected = { selectedDay = it }
                    )
                }

                // PAN (read-only)
                if (displayPan.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth(0.95f)) {
                        OutlinedTextField(
                            value = displayPan, onValueChange = {},
                            label = { Text("PAN Number") },
                            singleLine = true, readOnly = true, enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = MaterialTheme.lightGreyBackground,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text("PAN verified from earlier step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TimeoutButton(
                    onClick = { submitKyc() },
                    enabled = name.isNotBlank() && dob.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                            && !isPolling && !isSubmitting,
                    timeoutState = timeoutState,
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Text(if (isPolling) pollMessage ?: "Verifying..." else "Continue")
                }
            }
        }

        // Loading overlay
        if (isPolling || isSubmitting) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                LoadingScreen(text = pollMessage ?: "Submitting, please wait...")
            }
        }
    }
}

// ── Hierarchical date picker (KMP, no java.util.Calendar) ────────────────────

@Composable
fun HierarchicalDatePicker(
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
            modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (currentStep) { 0 -> "Year"; 1 -> "Month"; 2 -> "Day"; else -> "Date" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (currentStep > 0) {
                        TextButton(onClick = { onStepChange(currentStep - 1) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                            Text("Back")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (currentStep) {
                    0 -> KmpYearPicker(onYearSelected = { y -> onYearSelected(y); onStepChange(1) })
                    1 -> KmpMonthPicker(selectedYear = selectedYear, onMonthSelected = { m -> onMonthSelected(m); onStepChange(2) })
                    2 -> KmpDayPicker(selectedYear = selectedYear, selectedMonth = selectedMonth,
                        onDaySelected = { d ->
                            onDaySelected(d)
                            if (selectedYear != null && selectedMonth != null) onDateSelected(selectedYear, selectedMonth, d)
                        })
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun KmpYearPicker(onYearSelected: (Int) -> Unit) {
    val years = (1950..CUTOFF_YEAR).toList().reversed()
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(years) { year ->
            Card(modifier = Modifier.aspectRatio(1.5f).clickable { onYearSelected(year) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(year.toString(), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun KmpMonthPicker(selectedYear: Int?, onMonthSelected: (Int) -> Unit) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val count = if (selectedYear != null && selectedYear >= CUTOFF_YEAR) {
        // Limit months for current edge year (approximate — server validates authoratively)
        minOf(months.size, 12)
    } else months.size

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedYear != null) {
            Text("Year: $selectedYear", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until count).toList()) { idx ->
                Card(modifier = Modifier.aspectRatio(1.5f).clickable { onMonthSelected(idx + 1) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(months[idx], style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun KmpDayPicker(selectedYear: Int?, selectedMonth: Int?, onDaySelected: (Int) -> Unit) {
    val days = if (selectedYear != null && selectedMonth != null)
        daysInMonth(selectedYear, selectedMonth) else 31
    val monthNames = listOf("January","February","March","April","May","June",
        "July","August","September","October","November","December")

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedYear != null && selectedMonth != null) {
            Text("${monthNames[selectedMonth - 1]} $selectedYear",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items((1..days).toList()) { day ->
                Card(modifier = Modifier.aspectRatio(1f).clickable { onDaySelected(day) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(day.toString(), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DigiLockerLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Connecting to secure verification...",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 32.dp))
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Please wait", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
