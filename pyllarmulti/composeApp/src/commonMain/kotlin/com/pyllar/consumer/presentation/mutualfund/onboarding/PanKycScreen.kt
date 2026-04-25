package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.TimeoutButton
import com.pyllar.consumer.presentation.ui.components.rememberTimeoutState
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val MaterialTheme.lightGreyBackground: Color
    @Composable
    get() = Color(0xFFF5F5F5)

@Composable
fun PanKycScreen(
    onPanVerified: (String, String?, String?, NavigationInfo?, Any?) -> Unit,
    viewModel: PanKycViewModel // Injected
) {
    val panCheckResult by viewModel.panCheckResult.collectAsStateWithLifecycle()
    val pan by viewModel.pan.collectAsStateWithLifecycle()
    
    // Timeout state for the verify button
    val timeoutState = rememberTimeoutState("PanKyc", "verify")
    
    // PAN validation rules
    val isPanLengthValid = pan.length == 10
    val isFourthLetterValid = pan.length >= 4 && pan[3] == 'P'
    val isPanValid = isPanLengthValid && isFourthLetterValid
    val fourthLetterError = if (pan.length >= 4 && !isFourthLetterValid) "Only Individual PANs (4th letter 'P') are allowed." else null

    var panVerified by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Timeout handling - reset isSubmitting after 90 seconds if API doesn't complete
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(90000) // 90 seconds
            if (isSubmitting && panCheckResult !is Resource.Success && panCheckResult !is Resource.Error) {
                platformLog("⚠️ Safety timeout: API call took too long, resetting isSubmitting")
                isSubmitting = false
            }
        }
    }
    
    // Handle PAN verification result
    LaunchedEffect(panCheckResult) {
        when (val result = panCheckResult) {
            is Resource.Success -> {
                // API call completed, reset submitting state
                isSubmitting = false
                result.data?.let { panData ->
                    panVerified = true
                    
                    scope.launch {
                        // Extract nextScreen from the Resource navigation
                        val nextScreen = result.navigation?.nextScreen
                        // Extract panHolderName from the PAN check result
                        val panHolderName = panData.panHolderName
                        // Pass full navigation info and server response data
                        onPanVerified(pan, nextScreen, panHolderName, result.navigation, panData)
                    }
                }
            }
            is Resource.Error -> {
                // API call completed (with error), reset submitting state
                isSubmitting = false
                // Trigger timeout when API call fails
                timeoutState.triggerTimeout()
            }
            is Resource.Loading -> {
                // Loading state - isSubmitting is set when button is clicked
            }
            null -> { /* No result yet */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(32.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()))
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
                    currentStep = if (panVerified) 1 else 0, 
                    completedStep = if (panVerified) 1 else 0,
                    currentScreenRoute = ScreenNames.PAN_KYC
                )
            }
        
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.lightGreyBackground)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
                    .imePadding(), 
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
            Spacer(modifier = Modifier.height(32.dp))
            // Main Card/Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PAN KYC Verification",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )


                    OutlinedTextField(
                        value = pan,
                        onValueChange = { newValue ->
                            // Filter to only allow letters and digits (no dots, spaces, or special chars)
                            val filtered = newValue.filter { it.isLetterOrDigit() }.uppercase().filterIndexed { index, c ->
                                when (index) {
                                    in 0..4 -> c.isLetter()   // first 5 must be letters
                                    in 5..8 -> c.isDigit()    // next 4 must be digits
                                    9 -> c.isLetter()         // last must be a letter
                                    else -> false             // reject extra chars
                                }
                            }
                            viewModel.updatePan(filtered.take(10)) // PAN max length = 10
                        },
                        label = { Text("Enter PAN Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        isError = panCheckResult is Resource.Error || fourthLetterError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    
                    (panCheckResult as? Resource.Error)?.let { error ->
                        Text(
                            text = "Verification Failed",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (fourthLetterError != null) {
                        Text(
                            text = fourthLetterError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TimeoutButton(
                        onClick = { 
                            // Prevent double submission
                            if (isSubmitting) {
                                platformLog("Submission already in progress, ignoring click")
                                return@TimeoutButton
                            }
                            
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            
                            // Set submitting state
                            isSubmitting = true
                            
                            viewModel.checkPan() 
                        },
                        enabled = isPanValid && !isSubmitting && panCheckResult !is Resource.Loading,
                        timeoutState = timeoutState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (panCheckResult) {
                            is Resource.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying...")
                            }
                            else -> Text("Verify PAN")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    when (val result = panCheckResult) {
                        is Resource.Success -> {
                            Text(
                                text = "PAN Verified Successfully",
                                color = Color.Green,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is Resource.Error -> {
                            val isNetworkError = result.message?.contains("Network", ignoreCase = true) == true ||
                                               result.message?.contains("timeout", ignoreCase = true) == true ||
                                               result.message?.contains("connection", ignoreCase = true) == true
                            Text(
                                text = if (isNetworkError) {
                                    "Unable to connect to secure server"
                                } else {
                                    "Verification Failed"
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> { /* No message for other states */ }
                    }
                }
            }
        }
        }
        
        // Loading overlay when submitting - blocks all interactions
        if (isSubmitting || panCheckResult is Resource.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
                    .zIndex(10f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Block all clicks while submitting
                    }
            ) {
                LoadingScreen(
                    text = "Submitting, please wait...",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
