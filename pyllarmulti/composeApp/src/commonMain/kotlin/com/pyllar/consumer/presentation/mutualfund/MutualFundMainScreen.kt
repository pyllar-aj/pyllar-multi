package com.pyllar.consumer.presentation.mutualfund

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MutualFundMainScreen(
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSip: () -> Unit = {},
    onNavigateToPortfolio: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Investments",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = { showLogoutDialog = true }) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }

        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mutual Fund Investment",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Start your investment journey with systematic investment plans (SIPs) and lumpsum investments",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Action Buttons
        Button(
            onClick = onNavigateToOnboarding,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Investor Onboarding", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }

        OutlinedButton(
            onClick = onNavigateToSip,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Create SIP", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }

        OutlinedButton(
            onClick = onNavigateToPortfolio,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("View Portfolio", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }

        // Info Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📈 Why SIP?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "• Disciplined investment approach\n• Rupee cost averaging benefits\n• Power of compounding\n• Flexibility to start with small amounts",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎯 Getting Started", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "1. Complete investor onboarding\n2. Create your first SIP\n3. Monitor your portfolio\n4. Adjust as needed",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
