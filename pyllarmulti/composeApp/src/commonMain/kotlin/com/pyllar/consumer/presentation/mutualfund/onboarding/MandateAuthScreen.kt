package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.data.remote.model.dto.MandateStatus
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandateAuthScreen(
    userId: String = "",
    kycAttemptId: String = "",
    investorId: String = "",
    amount: Double = 0.0,
    mandateUrl: String = "",
    mandateId: Long = 0L,
    mandateRef: Long = 0L,
    onNavigateToHelp: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onGoToHome: () -> Unit = {},
    viewModel: MandateAuthModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var upiAppClicked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        platformLog("MandateAuthScreen: \uD83D\uDCCB Received Parameters - mandateId: $mandateId, mandateRef: $mandateRef")
        if (mandateUrl.isNotBlank()) {
            platformLog("MandateAuthScreen: ✅ Received UPI mandate URL")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mandate Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToHelp) {
                        Text("Help", color = MaterialTheme.colorScheme.primary)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when {
                uiState.error != null -> {
                    StatusDisplay(
                        icon = Icons.Default.Error,
                        iconTint = Color.Red,
                        title = "Verification Timeout",
                        description = uiState.error ?: "An error occurred. Please try again.",
                        actionText = "Go to Home",
                        onAction = onGoToHome
                    )
                }

                uiState.mandateStatus != null -> {
                    val status = uiState.mandateStatus!!
                    val isSuccess = status == MandateStatus.APPROVED
                    
                    StatusDisplay(
                        icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        iconTint = if (isSuccess) Color(0xFF4CAF50) else Color.Red,
                        title = if (isSuccess) "Mandate Approved" else "Mandate ${status.name}",
                        description = if (isSuccess) "Your daily SIP is now set up successfully." else "Please try again or contact support.",
                        actionText = "Go to Home",
                        onAction = onGoToHome
                    )
                    
                    if (isSuccess) {
                        WhatsNextSection()
                    }
                }

                upiAppClicked || uiState.isLoading -> {
                    LoadingDisplay()
                }

                else -> {
                    MandateDetailsCard(amount = amount)
                    
                    Text(
                        "Choose a UPI app to complete the setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            upiAppClicked = true
                            platformLog("MandateAuthScreen: Launching UPI URL: $mandateUrl")
                            // In a real app, we'd use a platform-specific launcher
                            // For this KMP migration, we simulate the launch and start polling
                            scope.launch {
                                viewModel.startMandateSync(userId, mandateId, mandateRef)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Open UPI App", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Text(
                        "Ensure you use the same bank account linked with Pyllar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatusDisplay(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(80.dp)
        )
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(actionText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoadingDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp)
            Text("\uD83D\uDE80", fontSize = 48.sp)
        }
        Text(
            "Verifying Mandate...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Please do not close the app or go back. This may take a moment.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MandateDetailsCard(amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mandate Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SIP Amount", style = MaterialTheme.typography.bodyLarge)
                Text("₹$amount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Frequency", style = MaterialTheme.typography.bodyLarge)
                Text("Daily", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun WhatsNextSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("What's Next?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            WhatsNextItem("\u2705", "Order Placed", "Your investment order has been sent to the fund house.")
            WhatsNextItem("\uD83D\uDCC8", "Allocation", "Units will be allocated to your portfolio by tomorrow 8 AM.")
        }
    }
}

@Composable
fun WhatsNextItem(icon: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(icon, fontSize = 20.sp)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
