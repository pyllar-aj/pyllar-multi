package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun KycInformationScreen(
    onProceed: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onOpenWebSignIn: () -> Unit = {},
    errorMessage: String? = null
) {
    val scrollState = rememberScrollState()
    var showLoading by remember { mutableStateOf(false) }
    var instructionsExpanded by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("KycInformation")
    }

    if (showLoading) {
        LoadingScreen(text = "Connecting to DigiLocker...")
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageLetterButton(textColor = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onNavigateToHelp) {
                    Text("Help", color = MaterialTheme.colorScheme.primary)
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp).zIndex(2f)
            ) {
                OnboardingStepper(currentStep = 0, completedStep = 0, currentScreenRoute = ScreenNames.KYC_INFORMATION)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Complete Your Verification",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                InformationCard(
                    icon = Icons.Filled.Security,
                    title = "Secure Verification",
                    description = "Your documents are securely verified via DigiLocker, a government-approved platform."
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { instructionsExpanded = !instructionsExpanded }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Icon(if (instructionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                }

                if (instructionsExpanded) {
                    Text("1. Verify your Aadhaar with your registered mobile number.")
                    Text("2. Grant permission to access your documents.")
                    
                    Button(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent("kyc_info_web_signin_clicked")
                            onOpenWebSignIn()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Complete via Web Sign-in", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Watch our guide on how to complete KYC:")
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))
                            .clickable { uriHandler.openUri("https://youtu.be/U_U5PVeYGGs") },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
                            // Placeholder for video thumbnail
                            Icon(
                                Icons.Filled.PlayCircleFilled,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp).align(Alignment.Center)
                            )
                            Text(
                                "Watch Guide",
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Verification Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(errorMessage, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Button(
                    onClick = {
                        PlatformAnalyticsLogger.logEvent("kyc_info_proceed_clicked")
                        showLoading = true
                        scope.launch {
                            delay(500)
                            onProceed()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Proceed to DigiLocker", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InformationCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
