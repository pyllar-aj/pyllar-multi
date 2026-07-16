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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BDV2InfoBg)
                                    .border(1.dp, BDV2CardInfoBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        PlatformAnalyticsLogger.logEvent("bank_details_upi_promo_card_click", emptyMap())
                                        showUpiBankSheet = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(BDV2Obsidian),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "⚡",
                                            fontSize = 16.sp,
                                            color = BDV2GoldAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        val fullText = stringResource(Res.string.upi_promo_skip_entire_form)
                                        val parts = remember(fullText) { fullText.split("?") }
                                        val titleText = parts.getOrNull(0)?.let { "$it?" } ?: "Have your UPI ID?"
                                        val subtitleText = parts.getOrNull(1)?.trim() ?: "Skip this form entirely"

                                        Text(
                                            text = titleText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BDV2BronzeInk
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitleText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BDV2GoldDeep
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BDV2Obsidian)
                                        .clickable {
                                            PlatformAnalyticsLogger.logEvent("bank_details_upi_promo_button_click", emptyMap())
                                            showUpiBankSheet = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.upi_promo_try_it_btn),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BDV2GoldAccent
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
    userId: String,
    onDismiss: () -> Unit,
    onContinue: (accountNumber: String, ifscCode: String) -> Unit
) {
    var upi by remember { mutableStateOf("") }
    val upiPattern = remember { Regex("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$") }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    val fetchState by viewModel.upiBankFetchState.collectAsState()
    val isFetching = fetchState is BankDetailsViewModel.UpiBankFetchState.Fetching
    val successState = fetchState as? BankDetailsViewModel.UpiBankFetchState.Success
    val errorState = fetchState as? BankDetailsViewModel.UpiBankFetchState.Error

    val upiBringIntoViewRequester = remember { BringIntoViewRequester() }
    var isUpiFocused by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(isUpiFocused) {
        if (isUpiFocused) {
            delay(300)
            upiBringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(successState) {
        val state = successState
        if (state != null) {
            onContinue(state.accountNumber ?: "", state.ifscCode ?: "")
        }
    }

    Box(modifier = Modifier.fillMaxSize().zIndex(20f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x7A140C08))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(BDV2Cream)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(BDV2GoldAccent.copy(alpha = 0.3f))
                    )
                }

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
                                text = stringResource(Res.string.upi_promo_skip_form_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BDV2Obsidian
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.upi_promo_enter_upi_id_bank_details),
                            fontSize = 13.sp,
                            color = BDV2BronzeMuted,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(BDV2GoldDeep.copy(alpha = 0.1f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                ) {
                    Text(
                        text = "YOUR UPI ID",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BDV2BronzeInk,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val fieldBorderColor = when {
                        localValidationError != null -> BDV2VolatilityRed
                        errorState != null -> BDV2VolatilityRed
                        successState != null -> BDV2SuccessGreen
                        else -> BDV2FieldBorder
                    }

                    OutlinedTextField(
                        value = upi,
                        onValueChange = {
                            upi = it
                            if (fetchState !is BankDetailsViewModel.UpiBankFetchState.Idle) {
                                viewModel.resetUpiBankFetchState()
                            }
                            localValidationError = if (it.trim().isNotEmpty() && (it.contains("@") || it.length > 5) && !upiPattern.matches(it.trim())) {
                                "Invalid UPI ID format"
                            } else {
                                null
                            }
                        },
                        placeholder = { Text("yourname@okaxis", color = BDV2FieldBorder, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(upiBringIntoViewRequester)
                            .onFocusChanged { isUpiFocused = it.isFocused },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = fieldBorderColor,
                            unfocusedBorderColor = fieldBorderColor
                        )
                    )

                    val isNetError = errorState?.message?.let { msg ->
                        val lower = msg.lowercase()
                        lower.contains("unable to resolve host") ||
                        lower.contains("connect") ||
                        lower.contains("timeout") ||
                        lower.contains("network")
                    } == true

                    val hintText = when {
                        isNetError -> stringResource(Res.string.check_internet_connection)
                        localValidationError != null -> localValidationError!!
                        errorState != null -> stringResource(Res.string.upi_fetch_failed_error)
                        successState != null -> stringResource(Res.string.upi_fetch_success_hint)
                        else -> "e.g. yourname@okicici · yourname@ybl"
                    }
                    val hintColor = when {
                        isNetError || errorState != null || localValidationError != null -> BDV2VolatilityRed
                        successState != null -> BDV2SuccessGreen
                        else -> BDV2BronzeMuted
                    }
                    Text(
                        text = hintText,
                        fontSize = 12.sp,
                        color = hintColor,
                        modifier = Modifier.padding(top = 8.dp).heightIn(min = 18.dp)
                    )

                    if (successState != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(1.dp, BDV2SuccessGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .background(BDV2SuccessGreen.copy(alpha = 0.07f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.upi_fetch_details_found_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BDV2SuccessGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Account: ${successState.accountNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BDV2BronzeInk
                            )
                            Text(
                                text = "IFSC: ${successState.ifscCode}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BDV2BronzeInk
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hasReachedLimit = errorState?.message?.let { msg ->
                        val lower = msg.lowercase()
                        lower.contains("attempt") ||
                        lower.contains("limit") ||
                        lower.contains("exceed") ||
                        // lower.contains("different upi") ||
                        lower.contains("verify up to")
                    } == true && successState == null
                    val isValidUpi = upi.trim().isNotEmpty() && upiPattern.matches(upi.trim())
                    val canFetch = (isValidUpi && !isFetching && !hasReachedLimit)
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (successState != null) {
                                onContinue(successState.accountNumber ?: "", successState.ifscCode ?: "")
                            } else {
                                if (isValidUpi) {
                                    PlatformAnalyticsLogger.logEvent("bank_details_fetch_my_details_click", emptyMap())
                                    viewModel.fetchBankDetailsViaUpi(userId, upi.trim())
                                }
                            }
                        },
                        enabled = canFetch || successState != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BDV2Obsidian, contentColor = Color.White)
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(Res.string.upi_fetch_loading), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        } else {
                            val label = if (errorState != null) stringResource(Res.string.try_again) else if (successState != null) stringResource(Res.string.user_info_btn_confirm_continue) else stringResource(Res.string.btn_fetch_my_details)
                            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.5.dp, BDV2FieldBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = BDV2BronzeMuted)
                    ) {
                        Text(text = stringResource(Res.string.btn_fill_form_manually), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
