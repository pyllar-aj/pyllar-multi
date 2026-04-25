package com.pyllar.consumer.presentation.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.presentation.ui.components.StandardTextFieldNewTwo
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    userId: String,
    viewModel: HelperCodeViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.helperCodeState.collectAsState()
    var helperCodeInput by remember { mutableStateOf("") }

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Contact card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Contact Us",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "📧 customercare@pyllar.in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "We typically respond within 24–48 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Referral / Helper code card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Referral Code",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    if (state.isSubmitted && state.helperCode.isNotBlank()) {
                        Text("Your referral code:", style = MaterialTheme.typography.bodyMedium)
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
                        Text(
                            "If you have a referral code, enter it below.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        StandardTextFieldNewTwo(
                            text = helperCodeInput,
                            onValueChange = { helperCodeInput = it.uppercase() },
                            hint = "Enter referral code",
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

            // FAQ
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Frequently Asked Questions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    FaqItem("What is a SIP?",
                        "A Systematic Investment Plan (SIP) allows you to invest a fixed amount regularly in mutual funds.")
                    FaqItem("How do I start investing?",
                        "Complete your KYC, then create your first SIP from the Investments screen.")
                    FaqItem("Is my money safe?",
                        "Pyllar uses SEBI-registered mutual funds. Your investments are held with reputed AMCs.")
                }
            }
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
