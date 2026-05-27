package com.pyllar.consumer.presentation.ui.components

import androidx.compose.runtime.*
import com.pyllar.consumer.util.currentTimeMillis

/**
 * A helper to debounce raw click lambdas, useful for standard Button/TextButton/IconButton.
 */
@Composable
fun rememberDebouncedClick(
    delayMillis: Long = 600L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableStateOf(0L) }
    return remember(onClick) {
        {
            val currentTime = currentTimeMillis()
            if (currentTime - lastClickTime > delayMillis) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }
}
