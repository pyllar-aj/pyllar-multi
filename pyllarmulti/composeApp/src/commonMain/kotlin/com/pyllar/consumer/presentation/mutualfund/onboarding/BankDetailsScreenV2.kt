package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*

// ── V2 visual language: cream surface, dark bronze ink, luxury gold accents ──
private val BDV2Cream = Color(0xFFFBF9F4)
private val BDV2CreamTint = Color(0xFFF5EEDB)
private val BDV2BronzeInk = Color(0xFF3E2723)
private val BDV2BronzeMuted = Color(0xFF6D4C41)
private val BDV2GoldDeep = Color(0xFF8B6B25)
private val BDV2GoldAccent = Color(0xFFD4AF37)
private val BDV2Obsidian = Color(0xFF0A2415)
private val BDV2SuccessGreen = Color(0xFF2E7D32)
private val BDV2LinkGreen = Color(0xFF1A7A42)
private val BDV2VolatilityRed = Color(0xFFC62828)
private val BDV2FieldBorder = Color(0xFFD7CCC8)
private val BDV2CardBorder = Color(0xFFEFEBE9)
private val BDV2InfoBorder = Color(0x268B6B25)
private val BDV2InfoBg = Color(0x12D4AF37)
private val BDV2CardInfoBorder = Color(0x478B6B25)
private val BDV2MutedText = Color(0xFFB0A89A)

private fun validateIfsc(code: String): Boolean =
    Regex("^[A-Z]{4}0[A-Z0-9]{6}$").matches(code)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BankDetailsScreenV2(
    userId: String,
    kycAttemptId: String,
    onNext: (String?, String?) -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: BankDetailsViewModel = koinInject(),
    sessionStore: SessionStore = koinInject()
) {
    val scope = rememberCoroutineScope()
    val prefillData by viewModel.prefillData.collectAsState()
    val submitResult by viewModel.submitResult.collectAsState()

    var accountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var isIfscValid by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isPolling by remember { mutableStateOf(false) }
    var pollMessage by remember { mutableStateOf<String?>(null) }
    var pollDelayMs by remember { mutableStateOf(5000L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var showUpiBankSheet by remember { mutableStateOf(false) }
    var bankDetailsAutoFetched by remember { mutableStateOf(false) }

    val timeoutState = rememberTimeoutState("BankDetails", "submit")
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val pollingFallbackMessage = stringResource(Res.string.verification_in_progress_please_wait)
    val genericErrorMessage = stringResource(Res.string.failed_try_again)

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("BankDetailsV4")
    }

    LaunchedEffect(prefillData) {
        val prepopulatedAccount = prefillData["accountNumber"]
        val prepopulatedIfsc = prefillData["ifscCode"]

        if (!prepopulatedAccount.isNullOrBlank() && accountNumber.isBlank()) {
            accountNumber = prepopulatedAccount.filter { it.isDigit() }
        }
        if (!prepopulatedIfsc.isNullOrBlank() && ifscCode.isBlank()) {
            val filtered = prepopulatedIfsc.uppercase().filter { it.isLetterOrDigit() }.take(11)
            ifscCode = filtered
            isIfscValid = filtered.length == 11 && validateIfsc(filtered)
        }
        bankDetailsAutoFetched = !prepopulatedAccount.isNullOrBlank() && !prepopulatedIfsc.isNullOrBlank()
    }

    LaunchedEffect(submitResult) {
        when (val result = submitResult) {
            is Resource.Success -> {
                val navigation = result.navigation
                when (navigation?.action) {
                    NavigationAction.POLL -> {
                        isSubmitting = false
                        isPolling = true
                        pollMessage = navigation.getMessage() ?: pollingFallbackMessage
                        pollDelayMs = (navigation.params?.get("delayMs") as? JsonPrimitive)?.longOrNull
                            ?: (navigation.params?.get("delay_seconds") as? JsonPrimitive)?.longOrNull?.let { it * 1000L }
                            ?: (navigation.params?.get("retry_after_sec") as? JsonPrimitive)?.longOrNull?.let { it * 1000L }
                            ?: (navigation.params?.get("retry_after_ms") as? JsonPrimitive)?.longOrNull
                            ?: (navigation.params?.get("poll_interval_ms") as? JsonPrimitive)?.longOrNull
                            ?: 5000L
                    }
                    else -> {
                        isSubmitting = false
                        isPolling = false
                        isCompleted = true
                        val investorId = result.data?.investorId
                        PlatformAnalyticsLogger.logEvent(
                            "bank_details_submit_success",
                            mapOf("investor_id_present" to (investorId != null), "screen_version" to "v4")
                        )
                        PlatformAnalyticsLogger.logEvent(
                            "pyllar_bank_added",
                            mapOf("funnel_step" to "bank")
                        )
                        onNext(navigation?.nextScreen ?: ScreenNames.NOMINEE_DETAILS, investorId)
                    }
                }
            }
            is Resource.Error -> {
                isSubmitting = false
                isPolling = false
                errorMessage = (result.message ?: genericErrorMessage).toUserFriendlyErrorMessage()
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent("bank_details_submit_error", mapOf("screen_version" to "v4"))
            }
            is Resource.Loading -> {
                isSubmitting = true
                errorMessage = null
            }
            null -> {}
        }
    }

    LaunchedEffect(isPolling) {
        if (!isPolling) return@LaunchedEffect
        while (isPolling) {
            delay(pollDelayMs)
            if (!isPolling) break
            platformLog("BankDetailsScreenV2: polling bank submission")
            val effectiveUserId = sessionStore.getCurrentUserId().ifBlank { userId }
            viewModel.submitBankDetails(effectiveUserId, accountNumber, ifscCode)
        }
    }

    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90000)
            if (isSubmitting && submitResult !is Resource.Success && submitResult !is Resource.Error) {
                isSubmitting = false
                timeoutState.triggerTimeout()
            }
        }
    }

    val completedStep = if (isCompleted) 3 else 2

    Box(modifier = Modifier.fillMaxSize().background(BDV2Cream)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
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
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        tint = BDV2LinkGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.back),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BDV2LinkGreen
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageLetterButton(textColor = BDV2LinkGreen)
                    TextButton(onClick = {
                        PlatformAnalyticsLogger.logEvent(
                            "bank_details_help_clicked",
                            mapOf(
                                "screen" to "bank_details",
                                "button_location" to "top_right",
                                "screen_version" to "v4"
                            )
                        )
                        onNavigateToHelp()
                    }) {
                        Text(
                            stringResource(Res.string.help),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BDV2LinkGreen
                        )
                    }
                }
            }

            Surface(
                color = BDV2Cream,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                OnboardingStepper(
                    currentStep = 1,
                    completedStep = completedStep,
                    currentScreenRoute = ScreenNames.BANK_DETAILS
                )
            }

            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(Res.string.bank_account_details),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BDV2BronzeInk
                        )
                        Text(
                            text = stringResource(Res.string.bank_screen_subtitle),
                            fontSize = 11.sp,
                            color = BDV2BronzeMuted,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // ⚡ Skip the form card - only shown when bank details weren't auto-fetched
                        if (!bankDetailsAutoFetched) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFFDF2), RoundedCornerShape(24.dp))
                                    .border(1.5.dp, BDV2GoldAccent, RoundedCornerShape(24.dp))
                                    .padding(vertical = 24.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(BDV2Obsidian),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 24.sp,
                                        color = BDV2GoldAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(Res.string.verify_with_upi_id),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BDV2BronzeInk,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(Res.string.verify_with_upi_id_desc),
                                    fontSize = 13.sp,
                                    color = BDV2BronzeMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        PlatformAnalyticsLogger.logEvent("bank_details_upi_promo_click", mapOf("target" to "button"))
                                        showUpiBankSheet = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .border(BorderStroke(1.dp, BDV2GoldAccent), RoundedCornerShape(50)),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BDV2Obsidian,
                                        contentColor = BDV2Cream
                                    )
                                ) {
                                    Text(
                                        text = stringResource(Res.string.btn_choose_your_upi_id),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(BDV2FieldBorder.copy(alpha = 0.9f))
                                )
                                Text(
                                    text = "OR",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BDV2MutedText,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(BDV2FieldBorder.copy(alpha = 0.9f))
                                )
                            }
                        }

                        BDV2Card {
                            Text(
                                stringResource(Res.string.enter_bank_details),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BDV2BronzeInk
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val accountBringIntoViewRequester = remember { BringIntoViewRequester() }
                            val ifscBringIntoViewRequester = remember { BringIntoViewRequester() }
                            var isAccountFocused by remember { mutableStateOf(false) }
                            var isIfscFocused by remember { mutableStateOf(false) }

                            LaunchedEffect(isAccountFocused) {
                                if (isAccountFocused) {
                                    delay(400)
                                    accountBringIntoViewRequester.bringIntoView()
                                    delay(300)
                                    accountBringIntoViewRequester.bringIntoView()
                                }
                            }
                            LaunchedEffect(isIfscFocused) {
                                if (isIfscFocused) {
                                    delay(400)
                                    ifscBringIntoViewRequester.bringIntoView()
                                    delay(300)
                                    ifscBringIntoViewRequester.bringIntoView()
                                }
                            }

                            Text(
                                stringResource(Res.string.account_number),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BDV2BronzeMuted,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                                placeholder = {
                                    Text(
                                        stringResource(Res.string.enter_your_account_number),
                                        color = BDV2FieldBorder,
                                        fontSize = 14.sp
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !isPolling,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(accountBringIntoViewRequester)
                                    .onFocusChanged { isAccountFocused = it.isFocused },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BDV2LinkGreen,
                                    unfocusedBorderColor = BDV2FieldBorder,
                                    focusedTextColor = BDV2BronzeInk,
                                    unfocusedTextColor = BDV2BronzeInk,
                                    cursorColor = BDV2LinkGreen
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                stringResource(Res.string.ifsc_code),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BDV2BronzeMuted,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = {
                                    val filtered = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(11)
                                    ifscCode = filtered
                                    isIfscValid = filtered.length == 11 && validateIfsc(filtered)
                                },
                                placeholder = { Text("e.g. HDFC0001234", color = BDV2FieldBorder, fontSize = 14.sp) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Done
                                ),
                                isError = !isIfscValid && ifscCode.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                enabled = !isPolling,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(ifscBringIntoViewRequester)
                                    .onFocusChanged { isIfscFocused = it.isFocused },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BDV2LinkGreen,
                                    unfocusedBorderColor = BDV2FieldBorder,
                                    errorBorderColor = BDV2VolatilityRed,
                                    focusedTextColor = BDV2BronzeInk,
                                    unfocusedTextColor = BDV2BronzeInk,
                                    cursorColor = BDV2LinkGreen
                                ),
                                supportingText = {
                                    when {
                                        !isIfscValid && ifscCode.isNotBlank() -> Text(
                                            stringResource(Res.string.ifsc_code_not_valid),
                                            color = BDV2VolatilityRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            errorMessage?.let { message ->
                                Text(
                                    text = message,
                                    color = BDV2VolatilityRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(listOf(BDV2GoldAccent, BDV2GoldDeep)),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(1.5.dp)
                            ) {
                                TimeoutButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        PlatformAnalyticsLogger.logEvent(
                                            "bank_details_submit_attempt",
                                            mapOf(
                                                "ifsc_present" to ifscCode.isNotBlank(),
                                                "account_number_length" to accountNumber.length,
                                                "screen_version" to "v4"
                                            )
                                        )
                                        scope.launch {
                                            isSubmitting = true
                                            errorMessage = null
                                            viewModel.clearResults()
                                            val effectiveUserId = sessionStore.getCurrentUserId().ifBlank { userId }
                                            viewModel.submitBankDetails(
                                                userId = effectiveUserId,
                                                accountNumber = accountNumber,
                                                ifscCode = ifscCode
                                            )
                                        }
                                    },
                                    enabled = !isSubmitting && !isPolling &&
                                        accountNumber.isNotBlank() && ifscCode.isNotBlank() && isIfscValid,
                                    timeoutState = timeoutState,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BDV2Obsidian,
                                        contentColor = BDV2Cream,
                                        disabledContainerColor = BDV2Obsidian,
                                        disabledContentColor = BDV2Cream
                                    )
                                ) {
                                    Text(
                                        stringResource(Res.string.verify_and_continue),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text("→", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BDV2GoldAccent)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Text("⚠️", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        stringResource(Res.string.attention),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E)
                                    )
                                }
                                Text(
                                    text = "${stringResource(Res.string.bullet)} ${stringResource(Res.string.use_individual_savings_account)}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF33691E),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "${stringResource(Res.string.bullet)} ${stringResource(Res.string.do_not_use_joint_account)}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF33691E)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, BDV2CardBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(Res.string.why_bank_verification_needed),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BDV2LinkGreen,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    stringResource(Res.string.bank_verification_compliance),
                                    fontSize = 12.sp,
                                    color = BDV2BronzeMuted,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BDV2TrustBadge(stringResource(Res.string.bank_grade_security))
                            BDV2TrustDot()
                            BDV2TrustBadge(stringResource(Res.string.sebi_registered_plain))
                            BDV2TrustDot()
                            BDV2TrustBadge(stringResource(Res.string.amfi_registered))
                        }

                        OutlinedButton(
                            onClick = onNavigateToHelp,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = BDV2LinkGreen
                            ),
                            border = BorderStroke(1.dp, BDV2FieldBorder)
                        ) {
                            Text(text = stringResource(Res.string.know_more), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (isSubmitting || isPolling) {
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
                    text = if (isPolling && pollMessage != null) {
                        pollMessage!!
                    } else {
                        stringResource(Res.string.submitting_please_wait)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showUpiBankSheet) {
            UpiBankDetailsFetchSheet(
                viewModel = viewModel,
                sessionStore = sessionStore,
                userId = userId,
                onDismiss = {
                    showUpiBankSheet = false
                    viewModel.resetUpiBankFetchState()
                },
                onContinue = { fetchedAccountNumber, fetchedIfsc ->
                    accountNumber = fetchedAccountNumber
                    ifscCode = fetchedIfsc
                    isIfscValid = fetchedIfsc.length == 11 && validateIfsc(fetchedIfsc)
                    showUpiBankSheet = false
                    viewModel.resetUpiBankFetchState()
                }
            )
        }
    }
}

@Composable
private fun BDV2Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, BDV2CardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun RowScope.BDV2TrustBadge(text: String) {
    Text(text = text, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = BDV2SuccessGreen)
}

@Composable
private fun BDV2TrustDot() {
    Text(text = " · ", fontSize = 10.sp, color = BDV2BronzeInk.copy(alpha = 0.2f))
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun UpiBankDetailsFetchSheet(
    viewModel: BankDetailsViewModel,
    sessionStore: SessionStore,
    userId: String,
    onDismiss: () -> Unit,
    onContinue: (accountNumber: String, ifscCode: String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var userPhone by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        userPhone = sessionStore.getCurrentPhone()
        userEmail = sessionStore.getCurrentEmail()
    }

    var step by remember { mutableStateOf(1) }
    var selectedPrefix by remember { mutableStateOf("") } // "mobile" or "email"
    var selectedBankHandle by remember { mutableStateOf("") }

    val fetchState by viewModel.upiBankFetchState.collectAsState()
    val isFetching = fetchState is BankDetailsViewModel.UpiBankFetchState.Fetching
    val successState = fetchState as? BankDetailsViewModel.UpiBankFetchState.Success
    val errorState = fetchState as? BankDetailsViewModel.UpiBankFetchState.Error

    LaunchedEffect(successState) {
        val state = successState
        if (state != null) {
            PlatformAnalyticsLogger.logEvent("bank_details_upi_fetch_success", emptyMap())
            onContinue(state.accountNumber ?: "", state.ifscCode ?: "")
        }
    }

    val formattedPhone = remember(userPhone) {
        val clean = userPhone.replace(" ", "")
        if (clean.length == 10) {
            clean.substring(0, 5) + " " + clean.substring(5)
        } else {
            userPhone.ifBlank { "98765 43210" }
        }
    }
    val emailPrefix = remember(userEmail) {
        val clean = userEmail.substringBefore("@")
        clean.ifBlank { "rahul.k" }
    }

    val prefixValue = remember(selectedPrefix, formattedPhone, emailPrefix) {
        if (selectedPrefix == "mobile") {
            formattedPhone.replace(" ", "")
        } else if (selectedPrefix == "email") {
            emailPrefix
        } else {
            ""
        }
    }

    var upiInput by remember { mutableStateOf("") }

    val chooseBankPlaceholder = stringResource(Res.string.upi_choose_bank_placeholder)

    // Sync upiInput when prefix or bank handle changes
    LaunchedEffect(prefixValue, selectedBankHandle, chooseBankPlaceholder) {
        if (prefixValue.isNotEmpty()) {
            val handle = if (selectedBankHandle.isNotEmpty()) {
                selectedBankHandle.removePrefix("@")
            } else {
                chooseBankPlaceholder
            }
            upiInput = "$prefixValue@$handle"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
    ) {
        // Scrim Color Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x7A140C08))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        )

        // Bottom Sheet Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(BDV2Cream)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag handle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(BDV2GoldAccent.copy(alpha = 0.3f))
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.upi_build_sheet_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BDV2Obsidian
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.upi_build_sheet_subtitle),
                            fontSize = 13.sp,
                            color = BDV2BronzeMuted,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                    // Close button - only this can close the sheet
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BDV2GoldDeep.copy(alpha = 0.1f))
                            .clickable {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = BDV2BronzeMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.dp)
                        .background(BDV2GoldDeep.copy(alpha = 0.12f))
                )

                // Step 1: Prefix Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.upi_step_choose_prefix),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BDV2BronzeMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mobile card
                        val isMobileSelected = selectedPrefix == "mobile"
                        val mobileBorderColor = if (isMobileSelected) BDV2SuccessGreen else BDV2CardBorder
                        val mobileBgColor = if (isMobileSelected) Color(0xFFF1F8E9) else Color.White
                        val mobileStrokeWidth = if (isMobileSelected) 1.5.dp else 1.dp

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(mobileBgColor)
                                .border(mobileStrokeWidth, mobileBorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedPrefix = "mobile"
                                    step = 2
                                }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.upi_prefix_mobile),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BDV2GoldDeep
                                    )
                                    if (isMobileSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(BDV2SuccessGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formattedPhone,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BDV2BronzeInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.upi_prefix_mobile_subtitle),
                                    fontSize = 11.sp,
                                    color = BDV2BronzeMuted
                                )
                            }
                        }

                        // Email card
                        val isEmailSelected = selectedPrefix == "email"
                        val emailBorderColor = if (isEmailSelected) BDV2SuccessGreen else BDV2CardBorder
                        val emailBgColor = if (isEmailSelected) Color(0xFFF1F8E9) else Color.White
                        val emailStrokeWidth = if (isEmailSelected) 1.5.dp else 1.dp

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(emailBgColor)
                                .border(emailStrokeWidth, emailBorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedPrefix = "email"
                                    step = 2
                                }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.upi_prefix_email),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BDV2GoldDeep
                                    )
                                    if (isEmailSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(BDV2SuccessGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = emailPrefix,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BDV2BronzeInk
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(Res.string.upi_prefix_email_subtitle),
                                    fontSize = 11.sp,
                                    color = BDV2BronzeMuted
                                )
                            }
                        }
                    }
                }

                // Step 2 Section
                if (step == 2) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.upi_step_choose_bank),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BDV2BronzeMuted,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = stringResource(Res.string.upi_popular_banks),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BDV2MutedText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Popular bank list Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("@okicici", "@ybl", "@okaxis", "@paytm").forEach { handle ->
                                val isSelected = selectedBankHandle == handle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) BDV2Obsidian else Color.White)
                                        .border(1.dp, if (isSelected) BDV2GoldAccent else BDV2FieldBorder, RoundedCornerShape(50))
                                        .clickable { selectedBankHandle = handle }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = handle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BDV2Cream else BDV2BronzeInk
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(Res.string.upi_all_banks),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BDV2MutedText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // All banks lists Grid
                        val row1 = listOf("@oksbi", "@okhdfc", "@upi", "@axisbank")
                        val row2 = listOf("@kotak", "@indus", "@aubank", "@ibl")

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row1.forEach { handle ->
                                    val isSelected = selectedBankHandle == handle
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) BDV2Obsidian else Color.White)
                                            .border(1.dp, if (isSelected) BDV2GoldAccent else BDV2FieldBorder, RoundedCornerShape(50))
                                            .clickable { selectedBankHandle = handle }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = handle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BDV2Cream else BDV2BronzeInk
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row2.forEach { handle ->
                                    val isSelected = selectedBankHandle == handle
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) BDV2Obsidian else Color.White)
                                            .border(1.dp, if (isSelected) BDV2GoldAccent else BDV2FieldBorder, RoundedCornerShape(50))
                                            .clickable { selectedBankHandle = handle }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = handle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BDV2Cream else BDV2BronzeInk
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // YOUR UPI ID Title
                        Text(
                            text = stringResource(Res.string.verify_with_upi_id),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BDV2BronzeInk,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Editable input box
                        val outlineBorderColor = if (errorState != null) BDV2VolatilityRed else BDV2GoldAccent

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.5.dp, outlineBorderColor, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = upiInput,
                                onValueChange = {
                                    upiInput = it
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = BDV2BronzeInk,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (errorState?.message != null) {
                            Text(
                                text = errorState!!.message!!,
                                color = BDV2VolatilityRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Bottom Buttons CTA section
                val canSubmit = selectedPrefix.isNotEmpty() && selectedBankHandle.isNotEmpty() && upiInput.isNotEmpty() && !upiInput.contains(chooseBankPlaceholder)
                val opacity = if (canSubmit) 1f else 0.42f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(opacity)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BDV2GoldAccent, BDV2GoldDeep)
                                )
                            )
                            .padding(1.5.dp)
                    ) {
                        Button(
                            onClick = {
                                if (canSubmit) {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    PlatformAnalyticsLogger.logEvent("bank_details_fetch_my_details_click", emptyMap())
                                    viewModel.fetchBankDetailsViaUpi(userId, upiInput.trim())
                                }
                            },
                            enabled = canSubmit && !isFetching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BDV2Obsidian,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.upi_fetch_loading),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(Res.string.btn_fetch_my_details),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "→",
                                        fontSize = 17.sp,
                                        color = BDV2GoldAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
