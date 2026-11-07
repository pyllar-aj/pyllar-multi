@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pyllar.consumer.presentation.loan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.presentation.dashboard.formatIndian
import com.pyllar.consumer.util.BackHandler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private val Cream = Color(0xFFFBF9F4)
private val Obsidian = Color(0xFF0A2415)
private val DeepGreen = Color(0xFF11381E)
private val Gold = Color(0xFFD4AF37)
private val DarkGold = Color(0xFF8B6B25)
private val Brown = Color(0xFF3E2723)
private val BrownMuted = Color(0xFF6D4C41)
private val Hairline = Color(0xFFEFEBE9)
private val FieldBorder = Color(0xFFE0E0E0)
private val ErrorRed = Color(0xFFB00020)

private val PAN_RE = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
private val MOBILE_RE = Regex("^[6-9][0-9]{9}$")
private val EMAIL_RE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val PINCODE_RE = Regex("^[1-9][0-9]{5}$")
private val INCOME_RE = Regex("^[1-9][0-9]*$")

private val STEP_TITLES = listOf(
    "Tell us about you" to "Quick & simple",
    "Where do you live?" to "Your address",
    "How do you earn?" to "Work & income",
    "What is it for?" to "Almost there"
)

// Values are exactly what Aarthik accepts for endUse; anything else is rejected with HTTP 400.
private val END_USES = listOf(
    "travel" to "Travel", "education" to "Education", "health" to "Medical",
    "consumerDurablePurchase" to "Appliance or gadget", "other" to "Other"
)

/** Returns an error message for the field, or null when it is valid. */
private fun requiredError(value: String) = if (value.isBlank()) "Required" else null

private fun patternError(value: String, re: Regex, message: String) =
    if (value.isBlank()) "Required" else if (!re.matches(value)) message else null

/** Errors for the fields shown on [step], keyed by field name. */
private fun stepErrors(step: Int, v: LoanFormValues): Map<String, String> {
    val e = mutableMapOf<String, String?>()
    when (step) {
        0 -> {
            e["panName"] = requiredError(v.panName)
            e["pan"] = patternError(v.pan, PAN_RE, "Enter a valid 10-character PAN")
            e["dob"] = requiredError(v.dob)
            e["contactNumber"] = patternError(v.contactNumber, MOBILE_RE, "Enter a valid 10-digit mobile number")
            e["email"] = patternError(v.email, EMAIL_RE, "Enter a valid email")
        }
        1 -> {
            e["addressL1"] = requiredError(v.addressL1)
            e["addressL2"] = requiredError(v.addressL2)
            e["pincode"] = patternError(v.pincode, PINCODE_RE, "Enter a valid 6-digit pincode")
            e["city"] = requiredError(v.city)
            e["state"] = requiredError(v.state)
        }
        2 -> {
            if (v.employmentType == "SALARIED") {
                e["companyName"] = requiredError(v.companyName)
                e["officialEmail"] = patternError(v.officialEmail, EMAIL_RE, "Enter a valid work email")
            }
            e["income"] = patternError(v.income, INCOME_RE, "Enter your income")
        }
    }
    return e.mapNotNull { (k, msg) -> msg?.let { k to it } }.toMap()
}

@Composable
fun LoanApplicationScreen(
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit = {},
    onSubmitted: (applicationId: String) -> Unit,
    viewModel: LoanApplicationViewModel = koinInject()
) {
    val ui by viewModel.uiState.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var tried by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ui.submittedApplicationId) {
        ui.submittedApplicationId?.let {
            onSubmitted(it)
            viewModel.onSubmittedHandled()
        }
    }

    val goBack = {
        if (step > 0) {
            step -= 1
            tried = false
        } else {
            onBackClick()
        }
    }
    BackHandler(enabled = step > 0) { goBack() }

    val v = ui.values
    val errors = if (tried) stepErrors(step, v) else emptyMap()
    val last = step == 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = goBack,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Hairline),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Brown)
                    }
                }
                Text(
                    "Help",
                    modifier = Modifier.clickable(onClick = onHelpClick),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A7A42)
                )
            }

            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text("Loan application", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen)
                Text(
                    "STEP ${step + 1} OF 4",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, color = DarkGold
                )
                Text(
                    STEP_TITLES[step].first,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold, color = DeepGreen
                )
                Text(
                    STEP_TITLES[step].second,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold
                )
                Row(
                    modifier = Modifier.padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(if (i <= step) Gold else Hairline, CircleShape)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    when (step) {
                        0 -> {
                            TextInput("Name as on PAN", v.panName, "John Doe", errors["panName"]) { x -> viewModel.update { it.copy(panName = x) } }
                            TextInput("PAN", v.pan, "ABCPE1234F", errors["pan"], maxLen = 10, letterSpacing = 1.sp) { x ->
                                viewModel.update { it.copy(pan = x.uppercase()) }
                            }
                            DateInput("Date of birth", v.dob, errors["dob"]) { x -> viewModel.update { it.copy(dob = x) } }
                            Chips("Gender", listOf("male" to "Male", "female" to "Female", "other" to "Other"), v.gender) { x ->
                                viewModel.update { it.copy(gender = x) }
                            }
                            TextInput("Mobile number", v.contactNumber, "98765 43210", errors["contactNumber"],
                                prefix = "+91", maxLen = 10, keyboard = KeyboardType.Number) { x ->
                                viewModel.update { it.copy(contactNumber = x.filter(Char::isDigit)) }
                            }
                            TextInput("Email", v.email, "john_doe@foobar.com", errors["email"], keyboard = KeyboardType.Email) { x ->
                                viewModel.update { it.copy(email = x) }
                            }
                        }
                        1 -> {
                            TextInput("Address line 1", v.addressL1, "12 MG Road", errors["addressL1"]) { x -> viewModel.update { it.copy(addressL1 = x) } }
                            TextInput("Address line 2", v.addressL2, "Near Clock Tower", errors["addressL2"]) { x -> viewModel.update { it.copy(addressL2 = x) } }
                            TextInput("Pincode", v.pincode, "360001", errors["pincode"], maxLen = 6, keyboard = KeyboardType.Number) { x ->
                                viewModel.update { it.copy(pincode = x.filter(Char::isDigit)) }
                            }
                            TextInput("City", v.city, "Rajkot", errors["city"]) { x -> viewModel.update { it.copy(city = x) } }
                            TextInput("State", v.state, "Gujarat", errors["state"]) { x -> viewModel.update { it.copy(state = x) } }
                        }
                        2 -> {
                            Chips("Employment type", listOf("SALARIED" to "Salaried", "SELF_EMPLOYED" to "Self-employed"), v.employmentType) { x ->
                                viewModel.update { it.copy(employmentType = x) }
                            }
                            if (v.employmentType == "SALARIED") {
                                TextInput("Company name", v.companyName, "Acme Pvt. Ltd.", errors["companyName"]) { x -> viewModel.update { it.copy(companyName = x) } }
                                TextInput("Official email", v.officialEmail, "john@acme.com", errors["officialEmail"], keyboard = KeyboardType.Email) { x ->
                                    viewModel.update { it.copy(officialEmail = x) }
                                }
                            }
                            Chips("Income is", listOf("MONTHLY" to "Monthly", "ANNUAL" to "Yearly"), v.incomeType) { x ->
                                viewModel.update { it.copy(incomeType = x) }
                            }
                            val incomeHint = v.income.toLongOrNull()?.let {
                                "₹" + formatIndian(it.toDouble()) +
                                    if (v.incomeType == "MONTHLY") " per month" else " per year"
                            }
                            TextInput("Your income", v.income, "75,000", errors["income"], hint = incomeHint,
                                prefix = "₹", maxLen = 9, keyboard = KeyboardType.Number) { x ->
                                viewModel.update { it.copy(income = x.filter(Char::isDigit)) }
                            }
                        }
                        else -> Chips("What will you use the loan for?", END_USES, v.endUse) { x -> viewModel.update { it.copy(endUse = x) } }
                    }
                }

                if (last) {
                    ui.consentTexts.filter { !it.requiresAcceptance }.forEach { notice ->
                        Text(notice.body, fontSize = 13.sp, lineHeight = 19.sp, color = BrownMuted)
                    }
                    when {
                        ui.consentsLoading -> Text("Loading consent…", fontSize = 13.sp, color = BrownMuted)
                        ui.consentsFailed -> {
                            Text("We could not load the consent text. Please try again.", fontSize = 14.sp, color = ErrorRed)
                            TextButton(onClick = viewModel::loadConsents) { Text("Try again") }
                        }
                        else -> ui.consentTexts.tickable().forEach { consent ->
                            val ticked = consent.id in ui.acceptedConsentIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setConsent(consent.id, !ticked) }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = ticked,
                                    onCheckedChange = { viewModel.setConsent(consent.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = Obsidian),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(consent.body, fontSize = 14.sp, lineHeight = 21.sp, color = Brown)
                            }
                        }
                    }
                    if (tried && !consentsComplete(ui.consentTexts, ui.acceptedConsentIds)) {
                        Text("Please accept to continue.", fontSize = 13.sp, color = ErrorRed)
                    }
                }

                ui.errorMessage?.let { Text(it, fontSize = 14.sp, color = ErrorRed) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Cream)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
        ) {
            Button(
                onClick = {
                    val bad = stepErrors(step, v).isNotEmpty() || (last && !consentsComplete(ui.consentTexts, ui.acceptedConsentIds))
                    if (bad) {
                        tried = true
                    } else if (last) {
                        viewModel.submit()
                    } else {
                        step += 1
                        tried = false
                    }
                },
                enabled = !ui.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Gold),
                colors = ButtonDefaults.buttonColors(containerColor = Obsidian, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (ui.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (last) "Check eligibility" else "Continue", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BrownMuted)
}

@Composable
private fun FieldMessage(error: String?, hint: String?) {
    when {
        error != null -> Text(error, fontSize = 13.sp, color = ErrorRed)
        hint != null -> Text(hint, fontSize = 13.sp, color = BrownMuted)
    }
}

@Composable
private fun TextInput(
    label: String,
    value: String,
    placeholder: String,
    error: String?,
    hint: String? = null,
    prefix: String = "",
    maxLen: Int = 100,
    keyboard: KeyboardType = KeyboardType.Text,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(if (error != null) 2.dp else 1.dp, if (error != null) ErrorRed else FieldBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (prefix.isNotEmpty()) {
                Text(prefix, fontWeight = FontWeight.SemiBold, color = BrownMuted)
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 17.sp, color = Color(0xFFA1887F))
                }
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= maxLen) onChange(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    textStyle = TextStyle(
                        fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Brown, letterSpacing = letterSpacing
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        FieldMessage(error, hint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInput(label: String, value: String, error: String?, onPicked: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val displayDate = if (value.isEmpty()) {
        "DD / MM / YYYY"
    } else {
        try {
            val d = LocalDate.parse(value)
            val day = d.dayOfMonth.toString().padStart(2, '0')
            val month = d.monthNumber.toString().padStart(2, '0')
            val year = d.year.toString().padStart(4, '0')
            "$day / $month / $year"
        } catch (_: Exception) {
            value
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(if (error != null) 2.dp else 1.dp, if (error != null) ErrorRed else FieldBorder, RoundedCornerShape(12.dp))
                .clickable { open = true }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                displayDate,
                fontSize = 17.sp, fontWeight = FontWeight.Medium,
                color = if (value.isEmpty()) Color(0xFFA1887F) else Brown
            )
        }
        FieldMessage(error, null)
    }
    if (open) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val initialDate = runCatching { LocalDate.parse(value) }.getOrNull() ?: LocalDate(1995, 1, 1)
        val initialMillis = initialDate.atStartOfDayInUtc()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = (now.year - 100)..(now.year - 18)
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        val formatted = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
                        onPicked(formatted)
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

private fun LocalDate.atStartOfDayInUtc(): Long {
    val dt = LocalDateTime(year, monthNumber, dayOfMonth, 0, 0, 0, 0)
    return dt.toInstant(TimeZone.UTC).toEpochMilliseconds()
}

@Composable
private fun Chips(label: String, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                val on = value == selected
                Surface(
                    onClick = { onPick(value) },
                    shape = CircleShape,
                    color = if (on) Obsidian else Color.White,
                    border = BorderStroke(if (on) 2.dp else 1.dp, if (on) Gold else FieldBorder),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else Brown, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

