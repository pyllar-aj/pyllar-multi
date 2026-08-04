package com.pyllar.consumer.presentation.mutualfund.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.presentation.dashboard.formatIndian

@Composable
fun FundHeader(
    details: FundDetailsResponseDto,
    showNavChip: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Regular",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Growth",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (showNavChip) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "NAV: ₹${formatIndian(details.currentNav ?: 0.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// V2 color variables for BankDetailsCard
private val V2CreamTint = Color(0xFFF5EEDB)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2FieldBorder = Color(0xFFD7CCC8)

@Composable
fun BankDetailsCard(
    accountNumber: String?,
    ifscCode: String?,
    bankName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = V2CreamTint
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, V2FieldBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Bank Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = V2BronzeInk
            )
            
            if (bankName != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = V2BronzeMuted
                    )
                    Text(
                        text = bankName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = V2BronzeInk
                    )
                }
            }
            
            if (accountNumber != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = V2BronzeMuted
                    )
                    Text(
                        text = maskAccountNumber(accountNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = V2BronzeInk
                    )
                }
            }
            
            if (ifscCode != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "IFSC",
                        style = MaterialTheme.typography.bodyMedium,
                        color = V2BronzeMuted
                    )
                    Text(
                        text = maskIfscCode(ifscCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = V2BronzeInk
                    )
                }
            }
        }
    }
}

fun maskAccountNumber(accountNumber: String): String {
    return if (accountNumber.length > 4) {
        "X".repeat(accountNumber.length - 4) + accountNumber.takeLast(4)
    } else {
        accountNumber
    }
}

fun maskIfscCode(ifscCode: String): String {
    return if (ifscCode.length >= 6) {
        ifscCode.take(4) + "XXX" + ifscCode.takeLast(2)
    } else {
        ifscCode
    }
}
