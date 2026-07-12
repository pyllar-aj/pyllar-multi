package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.components.LoadingScreen
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── V2 Premium palette (matching PreVerificationScreenV2/NameDobScreenV2) ──
private val V2Cream = Color(0xFFFBF9F4)
private val V2DarkBrown = Color(0xFF3E2723)
private val V2InkSoft = Color(0xFF6D4C41)
private val V2Gold = Color(0xFFD4AF37)
private val V2GoldDark = Color(0xFF8B6B25)
private val V2DarkGreen = Color(0xFF0A2415)
private val V2MediumGreen = Color(0xFF1A7A42)
private val V2SubtleBorder = Color(0xFFEFEBE9)

@Composable
fun KycInformationScreenV2(
    onProceed: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    errorMessage: String? = null
) {
    val scrollState = rememberScrollState()
    var showLoading by remember { mutableStateOf(false) }
    var instructionsExpanded by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("KycInformationV2")
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(V2Cream)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                LanguageLetterButton(textColor = V2MediumGreen)
                TextButton(onClick = onNavigateToHelp) {
                    Text("Help", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = V2MediumGreen)
                }
            }

            // Stepper navigation in V2: flat, seamless integration
            Surface(
                color = V2Cream,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
            ) {
                OnboardingStepper(currentStep = 0, completedStep = 0, currentScreenRoute = ScreenNames.KYC_INFORMATION)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Complete Your KYC Verification",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                    color = V2DarkGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                InformationCard(
                    icon = Icons.Filled.Security,
                    title = "Secure Verification",
                    description = "We use DigiLocker to securely verify your Aadhaar details."
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { instructionsExpanded = !instructionsExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = V2DarkGreen
                    )
                    Icon(
                        imageVector = if (instructionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (instructionsExpanded) "Collapse" else "Expand",
                        tint = V2DarkGreen
                    )
                }

                if (instructionsExpanded) {
                    Text(
                        text = "1. To get started, verify your Aadhaar using DigiLocker.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = V2InkSoft,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "2. Watch this short guide to complete your account setup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = V2InkSoft,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri("https://youtu.be/U_U5PVeYGGs") },
                        border = BorderStroke(1.dp, V2SubtleBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Video placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircleFilled,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Watch Guide",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Verification Failed",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                KycV2GradientButton(
                    text = "Proceed to DigiLocker",
                    onClick = {
                        PlatformAnalyticsLogger.logEvent("kyc_information_proceed_clicked")
                        showLoading = true
                        scope.launch {
                            delay(500)
                            onProceed()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                LoadingScreen(text = "Connecting to DigiLocker...")
            }
        }
    }
}

@Composable
private fun InformationCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V2SubtleBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = V2MediumGreen, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = V2DarkGreen)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = V2InkSoft)
            }
        }
    }
}

@Composable
private fun KycV2GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.horizontalGradient(listOf(V2Gold, V2GoldDark)))
            .padding(1.5.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = V2DarkGreen,
                contentColor = Color.White,
                disabledContainerColor = V2DarkGreen.copy(alpha = 0.7f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(11.5.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth().height(49.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
