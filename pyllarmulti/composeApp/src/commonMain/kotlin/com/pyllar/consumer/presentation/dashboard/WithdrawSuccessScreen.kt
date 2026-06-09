package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Schedule
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import com.pyllar.consumer.platform.PlatformActions
import pyllar.composeapp.generated.resources.*
import com.pyllar.consumer.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSuccessScreen(
    withdrawalAmount: Double,
    schemeName: String,
    bankName: String,
    bankAccountLast4: String,
    transactionId: String,
    folio: String?,
    redemptionMode: String = "NORMAL",
    platformActions: PlatformActions = koinInject(),
    onNavigateToHome: () -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        platformActions.requestInAppReview(
            screenName = "WithdrawSuccess",
            silentFallback = true,
            trigger = "auto"
        )
    }

    Scaffold(containerColor = V2Cream) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Withdrawal Success Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = V2SubtleBorder.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, V2SuccessGreen.copy(alpha = 0.3f)),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(V2SuccessGreen, CircleShape)
                    )
                    Text(
                        text = stringResource(Res.string.withdrawal_success_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = V2SuccessGreen
                    )
                }
            }

            // Success Icon Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Outer glow
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = V2SubtleBorder.copy(alpha = 0.5f)
                ) {}
                
                // Inner green circle with checkmark
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = V2SuccessGreen
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Amount and Subtext
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${formatIndian(withdrawalAmount)}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.Black
                )
                Text(
                    text = stringResource(Res.string.withdrawal_initiated),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Transaction Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, V2SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    TransactionDetailRow(
                        label = "Withdrawing Amount",
                        value = "₹${formatIndian(withdrawalAmount)}"
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = V2SubtleBorder
                    )

                    if (folio != null) {
                        TransactionDetailRow(
                            label = "Folio Number",
                            value = folio
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = V2SubtleBorder
                        )
                    }

                //    TransactionDetailRow(
                //        label = "Transaction ID",
                //        value = transactionId
                //    )
                }
            }

            // Status Information Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Credit status
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFFE0E0E0)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = if (redemptionMode == "INSTANT") {
                                stringResource(Res.string.will_be_credited_within_30_mins)
                            } else {
                                stringResource(Res.string.will_be_credited_within_days, formatIndian(withdrawalAmount))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Go to Home Button
            Button(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = V2Obsidian,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.go_to_home),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = valueColor
        )
    }
}
