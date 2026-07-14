package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.ui.components.LanguageLetterButton
import com.pyllar.consumer.platform.PlatformActions
import org.koin.compose.koinInject

// ── V2 premium palette (matching KycInformationScreenV2.kt / SignatureScreenV2.kt) ──
private val V2Cream = Color(0xFFFBF9F4)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2GoldDeep = Color(0xFF8B6B25)
private val V2GoldAccent = Color(0xFFD4AF37)
private val V2Obsidian = Color(0xFF0A2415)
private val V2LinkGreen = Color(0xFF1A7A42)
private val V2CardBorder = Color(0xFFEFEBE9)
private val V2BorderGold20 = Color(0x338B6B25)

@Composable
fun EsignInformationScreenV2(
    onProceed: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    platformActions: PlatformActions = koinInject()
) {
    val scrollState = rememberScrollState()

    // Log screen view
    LaunchedEffect(Unit) {
        PlatformAnalyticsLogger.logScreenView("EsignInformationV2")
    }

    Box(modifier = Modifier.fillMaxSize().background(V2Cream)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header: Pyllar Money wordmark + Share + LanguageLetterButton + Help
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pyllar ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = V2Obsidian,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Money",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = V2GoldAccent,
                        letterSpacing = (-0.5).sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent(
                                "share_app_clicked",
                                mapOf("screen_name" to "EsignInformation", "screen_version" to "v2")
                            )
                            platformActions.shareText(
                                "Start your wealth building journey with Pyllar Money! Download now.",
                                "Share Pyllar"
                            )
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = V2LinkGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    LanguageLetterButton(textColor = V2LinkGreen)
                    TextButton(onClick = onNavigateToHelp) {
                        Text(
                            text = "Help",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = V2LinkGreen
                        )
                    }
                }
            }
            HorizontalDivider(color = V2BorderGold20, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 18.dp, bottom = 28.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = "eSign Your Application",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = V2BronzeInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subtitle
                Text(
                    text = "Perform your Aadhaar-based eSign to complete your KYC.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = V2BronzeMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OTP Verification Card styled premium
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, V2CardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(V2Obsidian),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = V2GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "OTP-Based Verification",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = V2BronzeInk
                            )
                            Text(
                                text = "You'll receive an OTP on your Aadhaar-linked mobile number to complete your KYC. After this, you can start investing.",
                                fontSize = 12.sp,
                                color = V2BronzeMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Proceed Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(listOf(V2GoldAccent, V2GoldDeep)), RoundedCornerShape(50))
                        .padding(1.5.dp)
                ) {
                    Button(
                        onClick = {
                            PlatformAnalyticsLogger.logEvent(
                                "esign_information_proceed_clicked"
                            )
                            onProceed()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = V2Obsidian,
                            contentColor = V2Cream,
                            disabledContainerColor = V2Obsidian,
                            disabledContentColor = V2Cream
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Proceed to eSign",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text("→", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = V2GoldAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
