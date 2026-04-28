package com.pyllar.consumer.presentation.support

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.presentation.ui.components.StandardTextFieldNewTwo
import com.pyllar.consumer.util.AppConstants
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    userId: String,
    viewModel: HelperCodeViewModel = koinInject(),
    platformActions: PlatformActions = koinInject(),
    onBack: () -> Unit = {},
    showKycHelp: Boolean = false,
    showBankHelp: Boolean = false,
    showOnlyKycInfo: Boolean = false
) {
    val state by viewModel.helperCodeState.collectAsState()
    var helperCodeInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KYC Help Information Section
            if (showKycHelp) {
                item {
                    KycHelpInformationCard()
                }
            }

            // Bank Account Help Information Section
            if (showBankHelp) {
                item {
                    BankHelpInformationCard()
                }
            }

            // Support Categories
            if (!showOnlyKycInfo) {
                item {
                    Text(
                        text = "Choose a query and chat with us",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    SupportCategoryCard(
                        title = "KYC Verification",
                        description = "Get help with KYC verification, or approval issues",
                        onClick = {
                            platformActions.openWhatsApp(
                                AppConstants.SUPPORT_WHATSAPP_NUMBER,
                                "Hello, I need help with KYC verification."
                            )
                        }
                    )
                }

                item {
                    SupportCategoryCard(
                        title = "Bank Account",
                        description = "Assistance with bank account verification or linking",
                        onClick = {
                            platformActions.openWhatsApp(
                                AppConstants.SUPPORT_WHATSAPP_NUMBER,
                                "Hello, I need help with my bank account verification."
                            )
                        }
                    )
                }

                item {
                    SupportCategoryCard(
                        title = "SIP & Transactions",
                        description = "Support for SIP creation or payment issues",
                        onClick = {
                            platformActions.openWhatsApp(
                                AppConstants.SUPPORT_WHATSAPP_NUMBER,
                                "Hello, I need help with SIP-related queries."
                            )
                        }
                    )
                }

                item {
                    SupportCategoryCard(
                        title = "Redemption",
                        description = "Chat with us regarding withdrawal or redemption",
                        onClick = {
                            platformActions.openWhatsApp(
                                AppConstants.SUPPORT_WHATSAPP_NUMBER,
                                "Hello, I need help with redemption-related queries."
                            )
                        }
                    )
                }
            }

            // Referral / Helper code card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Relationship Manager Code",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        if (state.isSubmitted && state.helperCode.isNotBlank()) {
                            Text("Your code:", style = MaterialTheme.typography.bodyMedium)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    state.helperCode,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        } else {
                            StandardTextFieldNewTwo(
                                text = helperCodeInput,
                                onValueChange = { helperCodeInput = it.uppercase() },
                                hint = "Enter code",
                                keyboardType = KeyboardType.Text
                            )

                            state.errorMessage?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = { viewModel.submitHelperCode(userId, helperCodeInput) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = helperCodeInput.isNotBlank() && !state.isSubmitting
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Submit Code")
                                }
                            }
                        }
                    }
                }
            }

            // FAQ
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Frequently Asked Questions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        FaqItem("What is a SIP?",
                            "A Systematic Investment Plan (SIP) allows you to invest a fixed amount regularly in mutual funds.")
                        FaqItem("How do I start investing?",
                            "Complete your KYC, then create your first SIP from the Investments screen.")
                    }
                }
            }
        }
    }
}

@Composable
fun SupportCategoryCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tap to chat \u2192",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.Message,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun KycHelpInformationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("KYC \u2013 Help Information", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("KYC (Know Your Customer) is mandatory as per SEBI and government regulations.", style = MaterialTheme.typography.bodySmall)
            
            HorizontalDivider()
            
            Text("Process:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Text("\u2022 PAN Verification: Enter your name and DOB exactly as on PAN card.", style = MaterialTheme.typography.bodySmall)
            Text("\u2022 Personal Details: Address, income, and nominee info.", style = MaterialTheme.typography.bodySmall)
            Text("\u2022 Bank Verification: For SIPs and withdrawals.", style = MaterialTheme.typography.bodySmall)
            Text("\u2022 DigiLocker: Secure Aadhaar e-Sign if required.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BankHelpInformationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bank Account Setup \u2013 Help", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text("We need your bank details to process SIPs and credit withdrawals securely.", style = MaterialTheme.typography.bodySmall)
            
            HorizontalDivider()
            
            Text("Requirements:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Text("\u2022 Individual savings account in your name.", style = MaterialTheme.typography.bodySmall)
            Text("\u2022 Active and UPI-enabled.", style = MaterialTheme.typography.bodySmall)
            Text("\u2022 Accurate IFSC and account number.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(question, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(answer, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}
