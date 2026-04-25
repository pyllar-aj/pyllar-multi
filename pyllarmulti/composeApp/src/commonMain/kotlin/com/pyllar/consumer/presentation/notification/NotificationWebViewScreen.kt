package com.pyllar.consumer.presentation.notification

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Notification/WebView screen stub for commonMain.
 *
 * The full WebView implementation is Android-specific (android.webkit.WebView).
 * The expect/actual pattern is used: this composable delegates to
 * [PlatformWebView] which has a real impl in androidMain and a stub in iosMain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationWebViewScreen(
    url: String,
    title: String = "Notification",
    notificationId: String? = null,
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (url.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No content URL provided.")
            }
        } else {
            PlatformWebView(
                url = url,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

/** Expect declaration for a platform WebView composable. */
@Composable
expect fun PlatformWebView(url: String, modifier: Modifier = Modifier)
