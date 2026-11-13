package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.platform.PlatformActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.kyc_cams_unavailable

/**
 * The "Start KYC" provider sheet plus what its two choices do, shared by every screen that can start KYC.
 * DigiLocker keeps the existing in-app flow ([onStartDigiLocker]); CAMS fetches its URL from the backend and opens it in
 * Chrome/browser (not our WebView), after telling the backend the user started.
 *
 * Always keep this composed and drive the sheet with [visible]: the sheet dismisses itself on a choice, and the CAMS URL
 * fetch must outlive that dismissal.
 */
@Composable
fun KycProviderSheetHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    onStartDigiLocker: () -> Unit,
    onCamsOpened: () -> Unit = {},
    camsInProgress: Boolean = false,
    viewModel: KycProviderViewModel = koinInject(),
    platformActions: PlatformActions = koinInject()
) {
    val scope = rememberCoroutineScope()
    val camsUnavailable = stringResource(Res.string.kyc_cams_unavailable)
    var isFetchingCamsUrl by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3500)
            toastMessage = null
        }
    }

    if (visible) {
        KycOptionSelectionBottomSheet(
            onDismiss = onDismiss,
            camsInProgress = camsInProgress,
            onDigiLockerSelected = {
                PlatformAnalyticsLogger.logEvent("kyc_provider_selected", mapOf("provider" to "DIGILOCKER"))
                onStartDigiLocker()
            },
            onCamsSelected = {
                PlatformAnalyticsLogger.logEvent("kyc_provider_selected", mapOf("provider" to "CAMS_AB"))
                if (isFetchingCamsUrl) return@KycOptionSelectionBottomSheet
                scope.launch {
                    isFetchingCamsUrl = true
                    val url = try {
                        viewModel.fetchCamsKycUrl()
                    } finally {
                        isFetchingCamsUrl = false
                    }
                    if (url != null) {
                        PlatformAnalyticsLogger.logEvent("cams_kyc_started", emptyMap())
                        viewModel.recordCamsStarted()
                        platformActions.openInBrowser(url) { opened ->
                            if (opened) {
                                onCamsOpened()
                            } else {
                                toastMessage = camsUnavailable
                            }
                        }
                    } else {
                        toastMessage = camsUnavailable
                    }
                }
            }
        )
    }

    if (isFetchingCamsUrl) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Box(modifier = Modifier.padding(24.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    if (toastMessage != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0A2415),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = toastMessage.orEmpty(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

