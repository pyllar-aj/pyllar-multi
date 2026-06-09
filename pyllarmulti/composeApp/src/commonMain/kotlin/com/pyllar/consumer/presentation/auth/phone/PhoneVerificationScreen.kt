package com.pyllar.consumer.presentation.auth.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.presentation.ui.theme.*
import kotlinx.coroutines.delay
import com.pyllar.consumer.getPlatform

/**
 * Shared representation of a language option for the language popup.
 * Host platforms provide the actual options and apply locale changes.
 */
data class LanguageOption(
    val languageTag: String,
    val displayName: String,
    val continueText: String
)

@Composable
private fun LanguageSelectionDialog(
    selectedIndex: Int,
    languageOptions: List<LanguageOption>,
    onLanguageSelected: (Int) -> Unit,
    onContinue: (LanguageOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(onClick = { onContinue(languageOptions[selectedIndex]) }) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun PhoneVerificationScreen(
    viewModel: PhoneVerificationViewModel,
    onPhoneVerified: (String) -> Unit = {},
    /** When false (e.g. user navigated to OTP/Dashboard), do not show the language popup so it does not appear on top of other screens. */
    isScreenVisible: Boolean = true,
    /** Language the host platform considers current (e.g. "en", "hi"). */
    currentLanguageTag: String = "en",
    /** Languages the host platform supports, in display-ready form. */
    languageOptions: List<LanguageOption> = listOf(
        LanguageOption("en", "English", "Continue"),
        LanguageOption("hi", "हिंदी", "जारी रखें"),
        LanguageOption("ta", "தமிழ்", "தொடரவும்"),
        LanguageOption("ml", "മലയാളം", "തുടരുക"),
        LanguageOption("kn", "ಕನ್ನಡ", "ಮುಂದುವರಿಸಿ"),
        LanguageOption("te", "తెలుగు", "కొనసాగించండి")
    ),
    /**
     * Callback when the user confirms a language choice.
     * The host (Android/iOS) is responsible for applying the locale and any platform-specific changes.
     */
    onLanguageConfirmed: (LanguageOption) -> Unit = {}
) {
    val verificationResult: Resource<AuthToken>? =
        viewModel.verificationResult.collectAsStateWithLifecycle().value
    val phoneNumber: String =
        viewModel.phoneNumber.collectAsStateWithLifecycle().value

    // Timeout state for the continue button
    val timeoutState = rememberTimeoutState("PhoneVerification", "continue")

    // Prevent double submit: disable button immediately on first tap until result
    var isSubmitting by remember { mutableStateOf(false) }

    val isIos = remember { getPlatform().name.contains("iOS", ignoreCase = true) }
    var showLanguagePopup by rememberSaveable(hasLangPref, isIos) { mutableStateOf(!hasLangPref && !isIos) }

    // Guard: hide popup as soon as this screen is no longer the visible destination (e.g. navigation to OTP, or resuming to Dashboard).
    LaunchedEffect(isScreenVisible) {
        if (!isScreenVisible) showLanguagePopup = false
    }

    val initialIndex = remember(currentLanguageTag, languageOptions) {
        languageOptions.indexOfFirst { it.languageTag == currentLanguageTag }.takeIf { it >= 0 } ?: 0
    }
    var selectedLanguageIndex by remember { mutableIntStateOf(initialIndex) }

    // Only show when this screen is the current destination (auto-display on load; guard prevents showing during navigation or when resuming to Dashboard).
    if (showLanguagePopup && isScreenVisible) {
        LanguageSelectionDialog(
            selectedIndex = selectedLanguageIndex,
            languageOptions = languageOptions,
            onLanguageSelected = { index -> 
                selectedLanguageIndex = index
            },
            onContinue = { option ->
                viewModel.saveLanguagePreference(option.languageTag)
                showLanguagePopup = false
                onLanguageConfirmed(option)
            }
        )
    }

    LaunchedEffect(verificationResult) {
        when (verificationResult) {
            is Resource.Success -> {
                isSubmitting = false
                onPhoneVerified(phoneNumber)
            }
            is Resource.Error -> {
                isSubmitting = false
                // Trigger timeout when API call fails
                timeoutState.triggerTimeout()
            }
            is Resource.Loading -> {
                // Loading state is now automatically tracked by TimeoutButton
            }
            null -> { /* No result yet */ }
        }
    }

    // Rolling headline texts
    val rollingTexts = listOf(
        "Grow your wealth with smart investing",
        "Safe, simple and transparent investing",
        "Invest in mutual funds in minutes"
    )
    var currentTextIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(true) }

    // Auto-rotate text every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            isVisible = false
            delay(500) // Fade out duration
            currentTextIndex = (currentTextIndex + 1) % rollingTexts.size
            isVisible = true
            delay(500) // Fade in duration
        }
    }

    if (!showLanguagePopup) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        ) {
        // Gradient Background: Emerald Green → Deep Blue
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    V2Obsidian, // Dark obsidian (top)
                    Color(0xFF1B7A4A), // Medium emerald
                    Color(0xFF2D9A5F), // Lighter emerald
                    Color(0xFF3FAF73), // Light emerald
                    Color(0xFF4FC387), // Very light emerald
                    Color(0xFF003200),
                    Color(0xFF002800)
                ),
                startY = 0f,
                endY = size.height
            )
            drawRect(brush = gradient)
        }

        // Floating elements (blurred circles)
        FloatingElements()

        // Top Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Rolling headline on left
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Use fully qualified name to avoid RowScope extension conflict
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                            initialOffsetY = { -20 },
                            animationSpec = tween(500)
                        ),
                        exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(
                            targetOffsetY = { 20 },
                            animationSpec = tween(500)
                        )
                    ) {
                        Text(
                            text = rollingTexts[currentTextIndex],
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 28.sp
                        )
                    }
                    
                    // Pagination dots (3 dots) - always visible, smoothly animated
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        rollingTexts.forEachIndexed { index, _ ->
                            val isActive = index == currentTextIndex
                            val targetSize by animateDpAsState(
                                targetValue = if (isActive) 8.dp else 6.dp,
                                animationSpec = tween(durationMillis = 300),
                                label = "dot_size"
                            )
                            val targetAlpha by animateFloatAsState(
                                targetValue = if (isActive) 1f else 0.4f,
                                animationSpec = tween(durationMillis = 300),
                                label = "dot_alpha"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(targetSize)
                                    .background(
                                        color = Color.White.copy(alpha = targetAlpha),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Abstract fintech illustration on right
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.Center
                )
                {
                }
            }
        }


        // Center Section - Login Card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.95f)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
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

                // Phone Input with Country Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Country Code Selector
                    Surface(
                        modifier = Modifier
                            .width(80.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5F5F5),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🇮🇳",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "+91",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF2D2D2D)
                            )
                        }
                    }

                    // Phone Number Input
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { newValue ->
                            if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                                viewModel.updatePhoneNumber(newValue)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        placeholder = {
                            Text(
                                text = "Enter phone number",
                                color = Color(0xFF9E9E9E)
                            )
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
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp
                        )
                    )
                }

                // Continue Button (Emerald Green) with timeout tracking
                val buttonEnabled = verificationResult !is Resource.Loading && phoneNumber.length == 10
                val emeraldGreen = V2Obsidian

                // Track loading state for timeout
                LaunchedEffect(verificationResult) {
                    when (verificationResult) {
                        is Resource.Loading -> {
                            timeoutState.startLoadingTracking()
                        }
                        is Resource.Error,
                        is Resource.Success,
                        null -> {
                            timeoutState.stopLoadingTracking()
                        }
                    }
                }
                
                val finalEnabled = if (timeoutState.isTimeoutActive()) {
                    true // Always enable when timeout is active (for retry)
                } else {
                    buttonEnabled && !timeoutState.isTimeoutActive()
                }
                
                Button(
                    onClick = click@{
                        if (isSubmitting) return@click
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        isSubmitting = true
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
                            text = if (timeoutState.isTimeoutActive()) "Retry" else "Continue",
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
                            errorMsg.ifBlank { "Something went wrong. Please try again."
                            }
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
}

@Composable
fun FloatingElements() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    
    // Floating circle 1
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )

    // Floating circle 2
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Blurred circle 1 (top-left)
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = 80.dp.toPx(),
            center = Offset(
                size.width * 0.1f + offset1 * 20f,
                size.height * 0.15f + offset1 * 30f
            )
        )

        // Blurred circle 2 (top-right)
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = 60.dp.toPx(),
            center = Offset(
                size.width * 0.85f + offset2 * 15f,
                size.height * 0.2f - offset2 * 25f
            )
        )

        // Blurred circle 3 (bottom-center)
        drawCircle(
            color = Color(0xFF4FC387).copy(alpha = 0.15f),
            radius = 100.dp.toPx(),
            center = Offset(
                size.width * 0.5f + offset1 * 25f,
                size.height * 0.8f - offset2 * 20f
            )
        )
    }
}
