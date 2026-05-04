package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsignWebViewScreen(
    url: String,
    onEsignComplete: () -> Unit,
    onBack: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("EsignWebView")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("eSign Document", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        EsignPlatformWebView(
            url = url,
            modifier = Modifier.fillMaxSize().padding(padding),
            onEsignComplete = onEsignComplete,
            onEsignCancel = onBack
        )
    }
}
