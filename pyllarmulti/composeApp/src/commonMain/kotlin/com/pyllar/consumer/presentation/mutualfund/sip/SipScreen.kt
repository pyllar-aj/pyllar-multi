package com.pyllar.consumer.presentation.mutualfund.sip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.pyllar.consumer.domain.models.SipFormData
import com.pyllar.consumer.domain.models.MutualFundConstants
import com.pyllar.consumer.presentation.ui.components.StandardTextFieldNewTwo
import com.pyllar.consumer.presentation.ui.theme.TrueWhite
import com.pyllar.consumer.util.Resource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipScreen(
    userId: String,
    viewModel: SipViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    val formData by viewModel.formData.collectAsState()
    val sipResult by viewModel.sipResult.collectAsState()
    val isLoading = sipResult is Resource.Loading

    var frequencyExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create SIP", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount
            StandardTextFieldNewTwo(
                text = formData.amount,
                onValueChange = { viewModel.updateFormData(formData.copy(amount = it)) },
                hint = "SIP Amount (₹) *",
                keyboardType = KeyboardType.Number
            )

            // Quick amount chips
            Text("Quick Amount", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("500", "1000", "2000", "5000", "10000").forEach { amount ->
                    item {
                        FilterChip(
                            onClick = { viewModel.updateFormData(formData.copy(amount = amount)) },
                            label = { Text("₹$amount") },
                            selected = formData.amount == amount
                        )
                    }
                }
            }

            // Frequency dropdown
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = !frequencyExpanded }
            ) {
                OutlinedTextField(
                    value = formData.frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { frequencyExpanded = false }) {
                    MutualFundConstants.SIP_FREQUENCIES.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq) },
                            onClick = {
                                viewModel.updateFormData(formData.copy(frequency = freq))
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }

            // Start Date
            StandardTextFieldNewTwo(
                text = formData.startDate,
                onValueChange = { viewModel.updateFormData(formData.copy(startDate = it)) },
                hint = "Start Date (YYYY-MM-DD) *"
            )

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "SIP Information",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    listOf(
                        "• Minimum SIP amount: ₹500",
                        "• SIP will be processed on the selected date",
                        "• You can modify or cancel SIP anytime"
                    ).forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Submit button
            Button(
                onClick = { viewModel.createSip(userId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TrueWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating SIP...")
                    }
                } else {
                    Text("Create SIP", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                }
            }

            // Result feedback
            when (val result = sipResult) {
                is Resource.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            "SIP created successfully! ID: ${result.data?.sipId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is Resource.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            result.message ?: "An error occurred",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
