package com.pyllar.consumer.presentation.auth.phone

import com.pyllar.consumer.getPlatform
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

// ── Design tokens ────────────────────────────────────────────────────────────

private val Cream = Color(0xFFFBF9F4)
private val BrandGreen = Color(0xFF0A2415)
private val AccentGold = Color(0xFFD4AF37)
private val ReviewBrown = Color(0xFF3E2723)
private val SubtitleBrown = Color(0xFF6D4C41)
private val DividerBeige = Color(0xFFEFEBE9)
private val MarqueeDot = Color(0xFFC8BDB5)
private val AvatarGoldDark = Color(0xFF8B6B25)

// Option C: Obsidian top section
private val ObsidianGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0D2E1A), Color(0xFF0A2415), Color(0xFF051510))
)
private val AmbientGoldGlow = Brush.radialGradient(
    colors = listOf(AccentGold.copy(alpha = 0.10f), Color.Transparent)
)
private val HandleBarGradient = Brush.horizontalGradient(
    colors = listOf(AccentGold, AvatarGoldDark)
)
private val AvatarGradient = Brush.linearGradient(
    colors = listOf(AccentGold, AvatarGoldDark),
    start = Offset(0f, 0f),
    end = Offset(30f, 30f)
)

// ── Review data ───────────────────────────────────────────────────────────────

private data class ReviewData(val text: String, val name: String)

private fun firstNameFrom(fullName: String): String {
    val first = fullName.trim().split(Regex("\\s+")).firstOrNull()?.trimEnd('.', ',') ?: return "?"
    return first.replaceFirstChar { it.uppercaseChar() }
}

private val REVIEWS = listOf(
    ReviewData(
        "Simple, user-friendly, and helps me save money regularly without any hassle. Clear interface, reliable service. Has made managing my savings easier and more organised.",
        "Hemang"
    ),
    ReviewData(
        "The Gold SIP and Silver Investment options make smart investing simple, while the auto debit feature helps me stay consistent. A secure platform with useful goal planning.",
        "Hemanth H R"
    ),
    ReviewData(
        "Pyllar has made daily saving simple and motivating. The goal planning feature helps me stay focused, and the wealth tracker gives a clear view of my progress.",
        "Jenul Abedin"
    ),
    ReviewData(
        "A great app for SIP and daily investments! Clean and easy to use. Tracking returns, goals, and performance is smooth. Perfect for disciplined daily investing.",
        "anubhav choudhary"
    ),
    ReviewData(
        "Simple and easy to use. Great for building a consistent savings habit, with helpful reminders. Highly recommended!",
        "Ramyasai Sumala"
    ),
    ReviewData(
        "Finally an investment app that doesn't confuse me. Small daily savings, that grows. Really happy with Pyllar.",
        "jawahar .c.s"
    ),
    ReviewData(
        "Like the fact that I can invest in gold at smaller amounts and on a daily basis. Smooth UI as well.",
        "Saritha Shivarudraiah"
    ),
)

// ── Language helpers (mirrors V2) ─────────────────────────────────────────────

private data class LanguageOptionV3(val displayName: String, val continueText: String, val languageTag: String)

private val LANGUAGE_OPTIONS_V3 = listOf(
    LanguageOptionV3("English", "Continue", "en"),
    LanguageOptionV3("हिंदी", "जारी रखें", "hi"),
    LanguageOptionV3("தமிழ்", "தொடரவும்", "ta"),
    LanguageOptionV3("മലയാളം", "തുടരുക", "ml"),
    LanguageOptionV3("ಕನ್ನಡ", "ಮುಂದುವರಿಸಿ", "kn"),
    LanguageOptionV3("తెలుగు", "కొనసాగించండి", "te")
)

@Composable
private fun LanguageDialogV3(
    selectedIndex: Int,
    onLanguageSelected: (Int) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                        .background(Color(0xFFF5F5F5), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 36.dp, end = 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.choose_preferred_language),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        ),
                        color = Color(0xFF2D2D2D),
                        textAlign = TextAlign.Center,
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(LANGUAGE_OPTIONS_V3) { index, option ->
                            val isSelected = selectedIndex == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.4f)
                                    .clickable { onLanguageSelected(index) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) BrandGreen.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) BrandGreen else Color(0xFFE0E0E0),
                                ),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        ),
                                        color = if (isSelected) BrandGreen else Color(0xFF2D2D2D),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = LANGUAGE_OPTIONS_V3[selectedIndex].continueText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneVerificationScreenV3(
    onPhoneVerified: (String) -> Unit = {},
    viewModel: PhoneVerificationViewModel? = null,
    isScreenVisible: Boolean = true,
) {
    if (viewModel == null) return

    val verificationResult by viewModel.verificationResult.collectAsStateWithLifecycle()
    val phoneNumber by viewModel.phoneNumber.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("PhoneVerificationV4")
    }

    val timeoutState = rememberTimeoutState("PhoneVerification", "continue")
    var isSubmitting by remember { mutableStateOf(false) }
    var showLocalLoading by remember { mutableStateOf(false) }
    var localErrorText by remember { mutableStateOf<String?>(null) }

    val buttonEnabled = verificationResult !is Resource.Loading<*> && phoneNumber.length == 10

    LaunchedEffect(verificationResult) {
        when (verificationResult) {
            is Resource.Loading -> timeoutState.startLoadingTracking()
            is Resource.Error -> timeoutState.stopLoadingTracking()
            is Resource.Success -> timeoutState.stopLoadingTracking()
            null -> {}
        }
    }

    val finalEnabled = if (timeoutState.isTimeoutActive()) true else buttonEnabled

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Language popup
    val hasLangPref by viewModel.hasLanguagePreference.collectAsStateWithLifecycle()
    val languagePopupShown by viewModel.languagePopupShown.collectAsStateWithLifecycle()
    
    // Auto-dialog on KMP only if not iOS (as per native spec comments, iOS locale behaves differently)
    val isIos = remember { getPlatform().name.contains("iOS", ignoreCase = true) }
    var showLanguagePopup by rememberSaveable(hasLangPref, languagePopupShown, isIos) {
        mutableStateOf(!hasLangPref && !languagePopupShown && !isIos)
    }

    LaunchedEffect(isScreenVisible) {
        if (!isScreenVisible) showLanguagePopup = false
    }

    LaunchedEffect(showLanguagePopup, isScreenVisible) {
        if (!showLanguagePopup && isScreenVisible) {
            delay(300)
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {}
            keyboardController?.show()
        }
    }

    var selectedLanguageIndex by remember { mutableIntStateOf(0) }

    if (showLanguagePopup && isScreenVisible) {
        LanguageDialogV3(
            selectedIndex = selectedLanguageIndex,
            onLanguageSelected = { selectedLanguageIndex = it },
            onContinue = {
                val option = LANGUAGE_OPTIONS_V3[selectedLanguageIndex]
                coroutineScope.launch {
                    viewModel.saveLanguagePreference(option.languageTag)
                    viewModel.markLanguagePopupShown()
                    showLanguagePopup = false
                }
            },
            onDismiss = {
                coroutineScope.launch {
                    viewModel.saveLanguagePreference("en")
                    viewModel.markLanguagePopupShown()
                }
                showLanguagePopup = false
            },
        )
    }

    LaunchedEffect(verificationResult) {
        when (verificationResult) {
            is Resource.Success -> {
                isSubmitting = false
                PlatformAnalyticsLogger.logEvent("phone_verify_success", mapOf("phone_last4" to phoneNumber.takeLast(4)))
                onPhoneVerified(phoneNumber)
            }
            is Resource.Error -> {
                isSubmitting = false
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent("phone_verify_error", mapOf("phone_last4" to phoneNumber.takeLast(4)))
            }
            is Resource.Loading -> {}
            null -> {}
        }
    }

    // Rotating review
    var reviewIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            reviewIndex = (reviewIndex + 1) % REVIEWS.size
        }
    }

    val invalidPhoneNumberMsg = stringResource(Res.string.invalid_phone_number)

    if (!showLanguagePopup) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.Transparent,
                ) {
                    Button(
                        onClick = click@{
                            if (isSubmitting) return@click
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            val firstChar = phoneNumber.firstOrNull()
                            if (firstChar in listOf('1', '2', '3', '4')) {
                                isSubmitting = true
                                showLocalLoading = true
                                localErrorText = null
                                coroutineScope.launch {
                                    delay(1500)
                                    showLocalLoading = false
                                    isSubmitting = false
                                    localErrorText = invalidPhoneNumberMsg
                                }
                                return@click
                            }
                            isSubmitting = true
                            PlatformAnalyticsLogger.logEvent(
                                "phone_verify_attempt",
                                mapOf("phone_last4" to phoneNumber.takeLast(4)),
                            )
                            viewModel.verifyPhoneNumber()
                        },
                        enabled = finalEnabled && !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, AccentGold),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White,
                            disabledContainerColor = BrandGreen.copy(alpha = 0.6f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f),
                        ),
                    ) {
                        if (verificationResult is Resource.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (timeoutState.isTimeoutActive()) {
                                    stringResource(Res.string.retry)
                                } else {
                                    stringResource(Res.string.login_v3_get_started)
                                },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
            ) {
                // ── Obsidian hero section ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianGradient)
                        .padding(bottom = 26.dp), // compensates for cream card overlap
                ) {
                    // Ambient gold glow behind review card
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp)
                            .background(AmbientGoldGlow, CircleShape),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp)
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // App bar: white Pyllar + gold Money, SEBI dimmed on dark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row {
                                Text(
                                    text = "Pyllar ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp,
                                )
                                Text(
                                    text = "Money",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGold,
                                    letterSpacing = (-0.5).sp,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.45f),
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = stringResource(Res.string.login_v3_sebi_badge),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.45f),
                                )
                            }
                        }

                        // Review card — gold-glass on obsidian
                        ReviewCard(reviewIndex = reviewIndex)

                        // Dots — white inactive on dark
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            REVIEWS.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.5.dp)
                                        .size(6.dp)
                                        .background(
                                            color = if (index == reviewIndex) AccentGold else Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape,
                                        ),
                                )
                            }
                        }
                    }
                }

                // ── Cream login card — floats up 14dp over obsidian ───────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-14).dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            clip = false,
                            ambientColor = Color(0xFF051510).copy(alpha = 0.32f),
                            spotColor = Color(0xFF051510).copy(alpha = 0.32f),
                        )
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Cream)
                        .padding(horizontal = 22.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Gold pill handle bar at the top edge
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(
                                brush = HandleBarGradient,
                                shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp),
                            ),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = stringResource(Res.string.login_to_begin),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReviewBrown,
                            letterSpacing = (-0.3).sp,
                        )
                        Text(
                            text = stringResource(Res.string.login_v3_subtitle),
                            fontSize = 13.sp,
                            color = SubtitleBrown,
                        )
                    }

                    // Phone input + error + marquee ───────────────────────────
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Phone input
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()
                            val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

                            LaunchedEffect(isFocused, imeVisible) {
                                if (isFocused && imeVisible) {
                                    delay(300)
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                    delay(250)
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFocused) BrandGreen else DividerBeige,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(text = "🇮🇳", fontSize = 17.sp)
                                    Text(
                                        text = stringResource(Res.string.country_code_91),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ReviewBrown,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(DividerBeige),
                                )
                                BasicTextField(
                                    value = phoneNumber,
                                    onValueChange = { newValue ->
                                        var digits = newValue.filter { it.isDigit() }
                                        // Keyboard autofill may inject +91 prefix → strip it
                                        if (digits.length == 12 && digits.startsWith("91")) {
                                            digits = digits.drop(2)
                                        }
                                        val filtered = digits.take(10)
                                        localErrorText = null
                                        viewModel.updatePhoneNumber(filtered)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 14.dp)
                                        .focusRequester(focusRequester),
                                    interactionSource = interactionSource,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = ReviewBrown,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (phoneNumber.isEmpty()) {
                                                Text(
                                                    text = stringResource(Res.string.enter_phone_number),
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF9E9E9E),
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                )
                            }
                        }

                        // Error message
                        if (localErrorText != null) {
                            Text(
                                text = localErrorText!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (verificationResult is Resource.Error) {
                            val error = verificationResult as Resource.Error<*>
                            val errorMsg = error.message ?: ""
                            val isNetworkError = errorMsg.contains("Network", ignoreCase = true) ||
                                    errorMsg.contains("timeout", ignoreCase = true) ||
                                    errorMsg.contains("connection", ignoreCase = true) ||
                                    errorMsg.contains("Failed to connect", ignoreCase = true) ||
                                    errorMsg.contains("IOException", ignoreCase = true)
                            Text(
                                text = if (isNetworkError) {
                                    stringResource(Res.string.check_internet_connection)
                                } else {
                                    errorMsg.ifBlank { stringResource(Res.string.an_error_occurred) }
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Partner marquee
                        V3TrustMarquee()
                    }
                }
            }
        }
    }

    if (showLocalLoading) {
        LoadingScreen(
            text = stringResource(Res.string.processing),
            modifier = Modifier.fillMaxSize().background(Cream)
        )
    }
}

// ── Review card (gold-glass on obsidian) ─────────────────────────────────────

@Composable
private fun ReviewCard(reviewIndex: Int) {
    Crossfade(targetState = reviewIndex, label = "review") { index ->
        val review = REVIEWS[index]
        val firstName = firstNameFrom(review.name)
        val initial = firstName.firstOrNull() ?: '?'
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AccentGold.copy(alpha = 0.07f))
                .border(1.dp, AccentGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top gold shimmer line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AccentGold.copy(alpha = 0.6f),
                                    Color.Transparent,
                                )
                            )
                        ),
                )

                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Quote — white on dark
                    Text(
                        text = "“${review.text}”",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal,
                        minLines = 3,
                    )

                    // Reviewer row: avatar + name + stars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(AvatarGradient, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initial.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = firstName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                            Text(
                                text = stringResource(Res.string.login_v3_verified_play_store),
                                fontSize = 10.sp,
                                color = AccentGold.copy(alpha = 0.65f),
                            )
                        }

                        Text(
                            text = "★★★★★",
                            fontSize = 13.sp,
                            color = AccentGold,
                            letterSpacing = (-0.5).sp,
                        )
                    }
                }
            }
        }
    }
}

// ── Trust marquee ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V3TrustMarquee() {
    val scrollState = rememberScrollState()
    val isHeapConstrained = remember {
        // maxMemory limit checking fallback
        false
    }
    val rowModifier = if (isHeapConstrained) {
        Modifier.fillMaxWidth().horizontalScroll(scrollState)
    } else {
        Modifier.fillMaxWidth().basicMarquee(iterations = Int.MAX_VALUE, velocity = 45.dp)
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(2) { V3TrustContent() }
    }
}

@Composable
private fun V3TrustContent() {
    Image(
        painter = painterResource(Res.drawable.ondc),
        contentDescription = "ONDC",
        modifier = Modifier.height(28.dp).width(28.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.sebi),
        contentDescription = stringResource(Res.string.login_v3_sebi_badge),
        modifier = Modifier.height(22.dp).width(80.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.amfi),
        contentDescription = "AMFI",
        modifier = Modifier.height(18.dp).width(70.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.axis),
        contentDescription = stringResource(Res.string.content_description_axis_mf),
        modifier = Modifier.height(22.dp).width(75.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.invesco),
        contentDescription = stringResource(Res.string.content_description_invesco),
        modifier = Modifier.height(18.dp).width(75.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.nippon),
        contentDescription = stringResource(Res.string.content_description_nippon),
        modifier = Modifier.height(18.dp).width(75.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.aditya),
        contentDescription = stringResource(Res.string.content_description_aditya_birla),
        modifier = Modifier.height(16.dp).width(75.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()

    Image(
        painter = painterResource(Res.drawable.edelweiss),
        contentDescription = stringResource(Res.string.content_description_edelweiss),
        modifier = Modifier.height(18.dp).width(75.dp),
        contentScale = ContentScale.Fit,
    )
    MarqueeSeparator()
}

@Composable
private fun MarqueeSeparator() {
    Text(text = "✦", fontSize = 10.sp, color = MarqueeDot)
}
