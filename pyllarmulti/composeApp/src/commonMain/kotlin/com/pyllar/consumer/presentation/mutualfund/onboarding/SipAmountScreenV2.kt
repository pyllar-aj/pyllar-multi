package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SipAmountScreenV2(
    userId: String,
    kycAttemptId: String = "",
    investorId: String = "",
    goalId: String = "",
    onSipCreated: (amount: Double, mandateUrl: String?, mandateId: Long?, mandateRef: Long?) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: SipAmountScreenV2ViewModel = koinInject()
) {
    val limitsState by viewModel.limitsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var amount by remember { mutableStateOf(101f) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        platformLog("SipAmountScreenV2: Loading limits for user $userId")
        // In a real app, we'd fetch the userPurposeId first
        viewModel.fetchInvestmentLimits("default_purpose")
    }
    
    LaunchedEffect(limitsState) {
        if (!limitsState.isLoading) {
            amount = limitsState.defaultAmount?.toFloat() ?: limitsState.minAmount.toFloat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set SIP Amount", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
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
            // Projection Card
            ProjectionCard(amount = amount.toDouble())

            // Amount Selection
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Select Daily SIP Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min: ₹${limitsState.minAmount}", style = MaterialTheme.typography.bodySmall)
                    Text("Max: ₹${limitsState.maxAmount}", style = MaterialTheme.typography.bodySmall)
                }

                Slider(
                    value = amount,
                    onValueChange = { amount = it },
                    valueRange = limitsState.minAmount.toFloat()..limitsState.maxAmount.toFloat(),
                    steps = ((limitsState.maxAmount - limitsState.minAmount) / 10).toInt()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "₹${amount.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(101f, 251f, 501f).forEach { valOpt ->
                    if (valOpt >= limitsState.minAmount && valOpt <= limitsState.maxAmount) {
                        FilterChip(
                            selected = amount == valOpt,
                            onClick = { amount = valOpt },
                            label = { Text("₹${valOpt.toInt()}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        // Simulate SIP creation
                        platformLog("SipAmountScreenV2: Creating SIP for amount ${amount.toInt()}")
                        // In a real app, this would call viewModel.createDailySip
                        onSipCreated(amount.toDouble(), "upi://mandate?pa=pyllar@okicici&pn=Pyllar&am=${amount.toInt()}&tr=TXN123", 123L, 456L)
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Continue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            
            Text(
                "You can change or stop your SIP anytime.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ProjectionCard(amount: Double) {
    val projectedValue = amount * 365 * 7 * 1.12 // Simple 12% annual return simulation over 7 years
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF06D688), Color(0xFF105E26))
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "₹${amount.toInt()} / day could become",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${(projectedValue / 100000).toInt()}.${((projectedValue % 100000) / 1000).toInt()}L",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
                
                Text(
                    "in 7 years @ 12% p.a.*",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Simplified growth bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    GrowthBar(1, 0.2f)
                    GrowthBar(3, 0.4f)
                    GrowthBar(5, 0.7f)
                    GrowthBar(7, 1.0f)
                }
            }
        }
    }
}

@Composable
fun GrowthBar(year: Int, heightFactor: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(80.dp * heightFactor)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )
        Text("${year}Y", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
