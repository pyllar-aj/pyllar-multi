package com.pyllar.consumer.presentation.auth.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.presentation.ui.theme.V2Cream
import com.pyllar.consumer.presentation.ui.theme.V2Obsidian
import com.pyllar.consumer.presentation.ui.theme.V2SuccessGreen
import com.pyllar.consumer.presentation.ui.theme.V2SubtleBorder
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.pyllar.consumer.getPlatform

private data class AssetBannerData(
    val title: String,
    val targetGrams: Float,
    val unit: String,
    val pillText: String,
    val accentColor: Color
)

@Composable
private fun LanguageSelectionDialog(
    selectedIndex: Int,
    languageOptions: List<LanguageOption>,
    onLanguageSelected: (Int) -> Unit,
    onContinue: (LanguageOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onContinue(languageOptions[selectedIndex]) },
                colors = ButtonDefaults.buttonColors(containerColor = V2Obsidian)
            ) {
                Text(
                    text = languageOptions[selectedIndex].continueText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .background(Color(0xFFF5F5F5), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Choose your preferred language",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF2D2D2D),
                        textAlign = TextAlign.Center
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(languageOptions) { index, option ->
                            val isSelected = selectedIndex == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.4f)
                                    .clickable { onLanguageSelected(index) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) V2Obsidian.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) V2Obsidian else Color(0xFFE0E0E0)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) V2Obsidian else Color(0xFF2D2D2D),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun PhoneVerificationScreenV2(
    viewModel: PhoneVerificationViewModel,
    onPhoneVerified: (String) -> Unit = {},
    isScreenVisible: Boolean = true,
    currentLanguageTag: String = "en",
    languageOptions: List<LanguageOption> = listOf(
        LanguageOption("en", "English", "Continue"),
        LanguageOption("hi", "हिंदी", "जारी रखें"),
        LanguageOption("ta", "தமிழ்", "தொடரவும்"),
        LanguageOption("ml", "മലയാളം", "തുടരുക"),
        LanguageOption("kn", "ಕನ್ನಡ", "ಮുಂದುವರಿಸಿ"),
        LanguageOption("te", "తెలుగు", "కొనసాగించండి")
    ),
    onLanguageConfirmed: (LanguageOption) -> Unit = {}
) {
    val verificationResult: Resource<AuthToken>? =
        viewModel.verificationResult.collectAsStateWithLifecycle().value
    val phoneNumber: String =
        viewModel.phoneNumber.collectAsStateWithLifecycle().value
    val hasLangPref = viewModel.hasLanguagePreference.collectAsStateWithLifecycle().value
    val languagePopupShown = viewModel.languagePopupShown.collectAsStateWithLifecycle().value

    val timeoutState = rememberTimeoutState("PhoneVerification", "continue")

    var isSubmitting by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val isIos = remember { getPlatform().name.contains("iOS", ignoreCase = true) }
    var showLanguagePopup by rememberSaveable(hasLangPref, languagePopupShown, isIos) {
        mutableStateOf(!hasLangPref && !languagePopupShown && !isIos)
    }
    val coroutineScope = rememberCoroutineScope()

    val initialIndex = remember(currentLanguageTag, languageOptions) {
        languageOptions.indexOfFirst { it.languageTag == currentLanguageTag }.takeIf { it >= 0 } ?: 0
    }
    var selectedLanguageIndex by remember { mutableIntStateOf(initialIndex) }

    LaunchedEffect(isScreenVisible) {
        if (!isScreenVisible) showLanguagePopup = false
    }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("PhoneVerification")
    }

    val goldTitle = stringResource(Res.string.login_v2_if_started_ago)
    val goldPill = stringResource(Res.string.login_v2_gold_pill)
    val silverPill = stringResource(Res.string.login_v2_silver_pill)

    val assets = remember(goldTitle, goldPill, silverPill) {
        listOf(
            AssetBannerData(
                title = goldTitle,
                targetGrams = 15.8f,
                unit = "g",
                pillText = goldPill,
                accentColor = Color(0xFFF5C518)
            ),
            AssetBannerData(
                title = goldTitle,
                targetGrams = 1.24f,
                unit = "kg",
                pillText = silverPill,
                accentColor = Color(0xFFE0E0E0)
            )
        )
    }

    var currentAssetIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            isVisible = false
            delay(500)
            currentAssetIndex = (currentAssetIndex + 1) % assets.size
            isVisible = true
            delay(500)
        }
    }

    val counterAnim = remember { Animatable(0f) }
    val cubicOutEasing = remember { CubicBezierEasing(0.215f, 0.610f, 0.355f, 1.0f) }
    LaunchedEffect(currentAssetIndex) {
        counterAnim.snapTo(0f)
        counterAnim.animateTo(
            targetValue = assets[currentAssetIndex].targetGrams,
            animationSpec = tween(durationMillis = 2200, easing = cubicOutEasing)
        )
    }

    LaunchedEffect(verificationResult) {
        when (verificationResult) {
            is Resource.Success -> {
                isSubmitting = false
                PlatformAnalyticsLogger.logEvent(
                    "phone_verify_success",
                    mapOf("phone_last4" to phoneNumber.takeLast(4))
                )
                onPhoneVerified(phoneNumber)
            }
            is Resource.Error -> {
                isSubmitting = false
                timeoutState.triggerTimeout()
                PlatformAnalyticsLogger.logEvent(
                    "phone_verify_error",
                    mapOf("phone_last4" to phoneNumber.takeLast(4))
                )
            }
            is Resource.Loading -> { /* tracked in card LaunchedEffect */ }
            null -> {}
        }
    }

    if (showLanguagePopup && isScreenVisible) {
        LanguageSelectionDialog(
            selectedIndex = selectedLanguageIndex,
            languageOptions = languageOptions,
            onLanguageSelected = { index -> selectedLanguageIndex = index },
            onContinue = { option ->
                coroutineScope.launch {
                    viewModel.saveLanguagePreference(option.languageTag)
                    viewModel.markLanguagePopupShown()
                    showLanguagePopup = false
                    onLanguageConfirmed(option)
                }
            },
            onDismiss = {
                viewModel.markLanguagePopupShown()
                showLanguagePopup = false
            }
        )
    }

    if (!showLanguagePopup) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
        ) {
            // Split background: top half forest green, bottom half soft light green
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = V2Obsidian,
                    size = Size(size.width, size.height * 0.5f)
                )
                drawRect(
                    color = V2SubtleBorder,
                    topLeft = Offset(0f, size.height * 0.5f),
                    size = Size(size.width, size.height * 0.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = 80.dp.toPx(),
                    center = Offset(size.width * 0.1f, size.height * 0.15f),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 60.dp.toPx(),
                    center = Offset(size.width * 0.85f, size.height * 0.2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                val currentAsset = assets[currentAssetIndex]

                // Hero: rolling asset banner
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                        initialOffsetY = { -20 },
                        animationSpec = tween(500)
                    ),
                    exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(
                        targetOffsetY = { 20 },
                        animationSpec = tween(500)
                    ),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top date pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.daily_since_format, "Jan '23"),
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Animated counter
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCounterValue(counterAnim.value),
                                color = currentAsset.accentColor,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 44.sp
                            )
                            Text(
                                text = currentAsset.unit,
                                color = currentAsset.accentColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Asset type + worth labels
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (currentAssetIndex == 0) stringResource(Res.string.login_v2_gold_equivalent) else stringResource(Res.string.login_v2_silver_equivalent),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentAssetIndex == 0) stringResource(Res.string.login_v2_gold_worth) else stringResource(Res.string.login_v2_silver_worth),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Bottom advantage pill
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = currentAsset.pillText,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Login card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "Login to begin",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 22.sp
                            ),
                            color = Color(0xFF2D2D2D),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Phone input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+91",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF2D2D2D),
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }

                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                                        viewModel.updatePhoneNumber(newValue)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .focusRequester(focusRequester),
                                interactionSource = interactionSource,
                                placeholder = {
                                    if (!isFocused) {
                                        Text(
                                            text = "Enter phone number",
                                            color = Color(0xFF9E9E9E)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = V2Obsidian,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedTextColor = Color(0xFF2D2D2D),
                                    unfocusedTextColor = Color(0xFF2D2D2D),
                                    cursorColor = V2Obsidian
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                            )
                        }

                        val buttonEnabled = verificationResult !is Resource.Loading && phoneNumber.length == 10
                        val emeraldGreen = V2Obsidian

                        LaunchedEffect(verificationResult) {
                            when (verificationResult) {
                                is Resource.Loading -> timeoutState.startLoadingTracking()
                                is Resource.Error, is Resource.Success, null -> timeoutState.stopLoadingTracking()
                            }
                        }

                        val finalEnabled = if (timeoutState.isTimeoutActive()) true
                                           else buttonEnabled && !timeoutState.isTimeoutActive()

                        Button(
                            onClick = click@{
                                if (isSubmitting) return@click
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                isSubmitting = true
                                PlatformAnalyticsLogger.logEvent(
                                    "phone_verify_attempt",
                                    mapOf("phone_last4" to phoneNumber.takeLast(4))
                                )
                                viewModel.verifyPhoneNumber()
                            },
                            enabled = finalEnabled && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldGreen,
                                contentColor = Color.White,
                                disabledContainerColor = emeraldGreen.copy(alpha = 0.6f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            if (verificationResult is Resource.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (timeoutState.isTimeoutActive()) "Retry" else "Send OTP",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        // Error message
                        if (verificationResult is Resource.Error) {
                            val error = verificationResult as Resource.Error<*>
                            val errorMsg = error.message ?: ""
                            val isNetworkError = error.isNetworkError ||
                                errorMsg.contains("Network", ignoreCase = true) ||
                                errorMsg.contains("timeout", ignoreCase = true) ||
                                errorMsg.contains("connection", ignoreCase = true) ||
                                errorMsg.contains("Failed to connect", ignoreCase = true) ||
                                errorMsg.contains("IOException", ignoreCase = true)
                            Text(
                                text = if (isNetworkError) {
                                    "Please check your internet connection and try again."
                                } else {
                                    errorMsg.ifBlank { "Something went wrong. Please try again." }
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Trust signals row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.width(90.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = V2SuccessGreen,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                )
                                Text(
                                    text = "SEBI registered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(Color(0xFFE0E0E0))
                                    .padding(top = 2.dp)
                            )

                            Row(
                                modifier = Modifier.width(90.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = V2SuccessGreen,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                )
                                Text(
                                    text = "3 lakh+ saving daily",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(Color(0xFFE0E0E0))
                                    .padding(top = 2.dp)
                            )

                            Row(
                                modifier = Modifier.width(90.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = V2SuccessGreen,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                )
                                Text(
                                    text = "Free to join",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCounterValue(value: Float): String {
    val intPart = value.toLong()
    val decPart = ((value - intPart) * 100).toLong().coerceAtLeast(0L)
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}
