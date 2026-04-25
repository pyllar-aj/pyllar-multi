package com.pyllar.consumer.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pyllar.consumer.util.TimeoutConfig
import kotlinx.coroutines.delay

/**
 * A composable that manages timeout state for buttons
 * This can be used to track when a button should be temporarily disabled after API failures
 * or when loading states persist for too long
 */
@Composable
fun rememberTimeoutState(
    screen: String,
    action: String,
    timeoutSeconds: Long? = null,
    loadingTimeoutSeconds: Long? = null
): TimeoutState {
    val timeout = timeoutSeconds ?: TimeoutConfig.getTimeout(screen, action)
    val loadingTimeout = loadingTimeoutSeconds ?: TimeoutConfig.getLoadingTimeout(screen, action)
    var timeRemaining by remember { mutableStateOf(0L) }
    var isTimeoutActive by remember { mutableStateOf(false) }
    var isLoadingTracking by remember { mutableStateOf(false) }
    
    // In KMP we use a simple generic counter or similar.
    // For System.currentTimeMillis() we can use kotlinx.datetime or just standard time if available
    // but a counter works just fine for UI state. We'll use a Long representing seconds passed.
    var loadingSecondsPassed by remember { mutableStateOf(0L) }
    
    // Countdown timer (only active when timeout is triggered)
    LaunchedEffect(isTimeoutActive, timeRemaining) {
        if (isTimeoutActive && timeRemaining > 0) {
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
            }
            isTimeoutActive = false
        }
    }
    
    // Loading timeout tracker
    LaunchedEffect(isLoadingTracking, loadingSecondsPassed) {
        if (isLoadingTracking) {
            delay(1000)
            loadingSecondsPassed++
            
            if (loadingSecondsPassed >= loadingTimeout) {
                timeRemaining = timeout
                isTimeoutActive = true
                isLoadingTracking = false
                loadingSecondsPassed = 0L
            }
        }
    }
    
    return remember {
        TimeoutState(
            isTimeoutActive = { isTimeoutActive },
            timeRemaining = { timeRemaining },
            triggerTimeout = {
                timeRemaining = timeout
                isTimeoutActive = true
                // Stop loading tracking when manually triggering timeout
                isLoadingTracking = false
                loadingSecondsPassed = 0L
            },
            startLoadingTracking = {
                isLoadingTracking = true
                loadingSecondsPassed = 0L
            },
            stopLoadingTracking = {
                isLoadingTracking = false
                loadingSecondsPassed = 0L
            }
        )
    }
}

/**
 * State class for managing timeout functionality
 */
class TimeoutState(
    val isTimeoutActive: () -> Boolean,
    val timeRemaining: () -> Long,
    val triggerTimeout: () -> Unit,
    val startLoadingTracking: () -> Unit,
    val stopLoadingTracking: () -> Unit
)

/**
 * A button component that is immediately clickable but can be temporarily disabled
 * after API failures, with a configurable timeout to re-enable it
 *
 * @param onClick The action to perform when button is clicked
 * @param enabled Whether the button should be enabled (in addition to timeout)
 * @param timeoutState The timeout state from rememberTimeoutState
 * @param modifier Modifier for the button
 * @param content The button content
 */
@Composable
fun TimeoutButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    timeoutState: TimeoutState,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    var wasEnabled by remember { mutableStateOf(enabled) }
    var isButtonDisabled by remember { mutableStateOf(false) }
    var disabledSecondsPassed by remember { mutableStateOf(0L) }
    
    LaunchedEffect(enabled) {
        if (!enabled && wasEnabled) {
            isButtonDisabled = true
            disabledSecondsPassed = 0L
            timeoutState.startLoadingTracking()
        } else if (enabled && !wasEnabled) {
            isButtonDisabled = false
            disabledSecondsPassed = 0L
            timeoutState.stopLoadingTracking()
        }
        wasEnabled = enabled
    }
    
    // Additional safety timeout: if button remains disabled for too long, force timeout
    LaunchedEffect(isButtonDisabled, disabledSecondsPassed) {
        if (isButtonDisabled) {
            delay(1000)
            disabledSecondsPassed++
            if (disabledSecondsPassed >= 35) { // 35 seconds
                timeoutState.triggerTimeout()
            }
        }
    }
    
    // Force enable button if timeout is active, regardless of other conditions
    val finalEnabled = if (timeoutState.isTimeoutActive()) {
        true // Always enable when timeout is active
    } else {
        enabled && !timeoutState.isTimeoutActive()
    }
    
    Button(
        onClick = onClick,
        enabled = finalEnabled,
        modifier = modifier
    ) {
        when {
            // Show "Retry" text when timeout is active (button is enabled for retry)
            timeoutState.isTimeoutActive() -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Retry",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            // Always show custom content - screens handle loading vs validation states
            else -> {
                content()
            }
        }
    }
}

/**
 * A button component that shows a countdown and becomes clickable after timeout
 * with custom loading content
 */
@Composable
fun TimeoutButtonWithCustomLoading(
    onClick: () -> Unit,
    enabled: Boolean = true,
    timeoutState: TimeoutState,
    modifier: Modifier = Modifier,
    loadingContent: @Composable (Long) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    var wasEnabled by remember { mutableStateOf(enabled) }
    var isButtonDisabled by remember { mutableStateOf(false) }
    var disabledSecondsPassed by remember { mutableStateOf(0L) }
    
    LaunchedEffect(enabled) {
        if (!enabled && wasEnabled) {
            isButtonDisabled = true
            disabledSecondsPassed = 0L
            timeoutState.startLoadingTracking()
        } else if (enabled && !wasEnabled) {
            isButtonDisabled = false
            disabledSecondsPassed = 0L
            timeoutState.stopLoadingTracking()
        }
        wasEnabled = enabled
    }
    
    LaunchedEffect(isButtonDisabled, disabledSecondsPassed) {
        if (isButtonDisabled) {
            delay(1000)
            disabledSecondsPassed++
            if (disabledSecondsPassed >= 35) {
                timeoutState.triggerTimeout()
            }
        }
    }
    
    val finalEnabled = if (timeoutState.isTimeoutActive()) {
        true
    } else {
        enabled && !timeoutState.isTimeoutActive()
    }
    
    Button(
        onClick = onClick,
        enabled = finalEnabled,
        modifier = modifier
    ) {
        if (timeoutState.isTimeoutActive()) {
            loadingContent(timeoutState.timeRemaining())
        } else {
            content()
        }
    }
}
