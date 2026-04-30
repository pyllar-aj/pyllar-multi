package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycWebViewScreen(
    url: String,
    onKycComplete: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    var isStatusHandled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("KycWebView")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KYC Verification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        KycPlatformWebView(
            url = url,
            modifier = Modifier.fillMaxSize().padding(padding),
            onStatusDetected = { status ->
                if (!isStatusHandled) {
                    isStatusHandled = true
                    onKycComplete(status)
                }
            }
        )
    }
}
