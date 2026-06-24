package com.pyllar.consumer.presentation.auth.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.domain.models.AuthUserDTO
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.otp.OtpField
import com.pyllar.consumer.presentation.ui.theme.V2Cream
import com.pyllar.consumer.presentation.ui.theme.V2Obsidian
import com.pyllar.consumer.presentation.ui.theme.V2Ink
import com.pyllar.consumer.presentation.ui.theme.V2HelpText
import kotlinx.coroutines.delay

/**
 * Shared, platform-agnostic OTP verification screen that depends only on:
 * - Shared ViewModel (`OtpVerificationViewModel`) in commonMain.
 * - Callback-based APIs for navigation, help, sharing, and opening URLs.
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    viewModel: OtpVerificationViewModel,
    onNavigateToPermissionScreen: (Boolean, String?, String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {}
) {
    val verificationResult: Resource<AuthUserDTO>? =
        viewModel.verificationResult.collectAsStateWithLifecycle().value
    val otp: String = viewModel.otp.collectAsStateWithLifecycle().value

    SmsRetrieverEffect(
        phoneNumber = phoneNumber,
        onOtpReceived = { code ->
            viewModel.updateOtp(code)
        }
    )

    val timeoutState = rememberTimeoutState("OtpVerification", "verify")
    var isSubmitting by remember { mutableStateOf(false) }

    var isChecked by remember { mutableStateOf(true) }

    var otpFieldValue by remember { 
        mutableStateOf(TextFieldValue(otp, selection = TextRange(otp.length))) 
    }
    
    // Sync with ViewModel if needed
    LaunchedEffect(otp) {
        if (otp != otpFieldValue.text) {
            otpFieldValue = TextFieldValue(otp, selection = TextRange(otp.length))
        }
    }

    var resendSeconds by remember { mutableStateOf(30) }
    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000)
            resendSeconds--
        }
    }

    LaunchedEffect(verificationResult) {
        when (val result = verificationResult) {
            is Resource.Success -> {
                isSubmitting = false
                result.data?.let { authUser ->
                    val nextScreen = result.navigation?.nextScreen
                    onNavigateToPermissionScreen(authUser.newUser, nextScreen, authUser.userId)
                }
            }
            is Resource.Error -> {
                isSubmitting = false
                timeoutState.triggerTimeout()
            }
            is Resource.Loading -> Unit
            null -> Unit
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Show keyboard again when OTP verification fails
    LaunchedEffect(verificationResult) {
        if (verificationResult is Resource.Error) {
            viewModel.updateOtp("")
            delay(100)
            keyboardController?.show()
        }
    }

    val primaryColor = V2Obsidian
    val emeraldGreen = V2Obsidian

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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    V2Obsidian,
                    Color(0xFF103620),
                    V2Cream
                ),
                startY = 0f,
                endY = size.height
            )
            drawRect(brush = gradient)
        }

        // Back button (navigation handled by host)
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(text = "Back", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }

        // Share + Help row (callbacks implemented by host)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onShareApp,
                modifier = Modifier.size(40.dp)
            ) {
                Text(text = "Share", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onNavigateToHelp) {
                Text(
                    text = "Help",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.95f)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter the OTP",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp
                    ),
                    color = primaryColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "We have sent an OTP to $phoneNumber",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF2D2D2D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OtpField(
                    length = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = verificationResult !is Resource.Loading,
                    isError = verificationResult is Resource.Error,
                    otpFieldValue = otpFieldValue,
                    onOtpFieldValueChange = { newValue ->
                        if (newValue.text.length <= 6 && newValue.text.all { ch -> ch.isDigit() }) {
                            otpFieldValue = newValue
                            viewModel.updateOtp(newValue.text)
                        }
                    },
                    onOtpComplete = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = emeraldGreen,
                            uncheckedColor = Color(0xFF9E9E9E),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "I authorise Pyllar Money to validate my bank details via the penny drop mechanism. An amount of Rs. 0.01 may be credited to your primary bank/UPI that is linked to the mobile number shared.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color(0xFF2D2D2D)
                    )
                }

                if (verificationResult is Resource.Error) {
                    val errorMsg = verificationResult.message.orEmpty()
                    val isNetworkError = verificationResult.isNetworkError ||
                        errorMsg.contains("Network", ignoreCase = true) ||
                        errorMsg.contains("timeout", ignoreCase = true) ||
                        errorMsg.contains("connection", ignoreCase = true) ||
                        errorMsg.contains("Failed to connect", ignoreCase = true) ||
                        errorMsg.contains("IOException", ignoreCase = true)

                    Text(
                        text = if (isNetworkError) {
                            "Please check your internet connection and try again."
                        } else {
                            "The OTP you entered is incorrect. Please try again."
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (resendSeconds > 0) {
                    Text(
                        text = "Resend OTP in ${resendSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )
                } else {
                    TextButton(
                        onClick = {
                            resendSeconds = 30
                            viewModel.updateOtp("")
                            viewModel.resendOtp()
                            keyboardController?.show()
                        }
                    ) {
                        Text(
                            text = "Resend OTP",
                            style = MaterialTheme.typography.bodySmall,
                            color = V2HelpText
                        )
                    }
                }

                LaunchedEffect(verificationResult) {
                    when (verificationResult) {
                        is Resource.Loading -> timeoutState.startLoadingTracking()
                        is Resource.Error,
                        is Resource.Success,
                        null -> timeoutState.stopLoadingTracking()
                    }
                }

                val buttonEnabled = otp.length == 6 && isChecked && verificationResult !is Resource.Loading
                val finalEnabled = if (timeoutState.isTimeoutActive()) {
                    isChecked
                } else {
                    buttonEnabled && !timeoutState.isTimeoutActive()
                }

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        isSubmitting = true
                        viewModel.verifyOtp()
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
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (timeoutState.isTimeoutActive()) "Retry" else "Confirm",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        val linkColor = Color(0xFFEFFDEE)
        val annotatedText = remember(linkColor) {
            buildAnnotatedString {
                append("By proceeding, I agree to Pyllar's ")
                pushStringAnnotation(tag = "URL", annotation = "https://www.pyllar.in/terms.html")
                withStyle(style = SpanStyle(color = linkColor)) {
                    append("T&C")
                }
                pop()
                append(" and ")
                pushStringAnnotation(tag = "URL", annotation = "https://www.pyllar.in/privacy.html")
                withStyle(style = SpanStyle(color = linkColor)) {
                    append("Privacy Policy")
                }
                pop()
            }
        }
        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onOpenUrl(annotation.item)
                    }
            }
        )
    }
}
