package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.pyllar.consumer.navigation.ScreenNames

@Composable
fun OnboardingStepper(currentStep: Int, completedStep: Int, currentScreenRoute: String? = null) {
    val steps = listOf("KYC", "Account", "SIP")
    val green = Color(0xFF367658) // New green color #3e8765
    val darkGrey = Color(0xFF222222)
    
    // Map routes to step indices based on actual screen usage patterns
    val actualCurrentStep = when {
        currentScreenRoute?.contains(ScreenNames.PRE_VERIFICATION) == true -> 0 // KYC step (alternative to pan_kyc)
        currentScreenRoute?.contains(ScreenNames.PAN_KYC) == true -> 0 // KYC step 
        currentScreenRoute?.contains(ScreenNames.NAME_DOB) == true -> 0 // Still KYC step
        currentScreenRoute?.contains(ScreenNames.ADDITIONAL_KYC) == true -> 1 // Account step  
        currentScreenRoute?.contains(ScreenNames.BANK_DETAILS) == true -> 1 // Account step
        currentScreenRoute?.contains(ScreenNames.SIGNATURE) == true -> 2 // SIP step
        currentScreenRoute?.contains(ScreenNames.SIP_AMOUNT) == true -> 2 // SIP step
        else -> currentStep // Fallback to provided step
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, label ->
            val isCompleted = index < completedStep
            val isCurrent = index == actualCurrentStep
            val color = if (isCompleted || isCurrent) green else darkGrey
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Text(
                    text = label,
                    color = color,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (index < steps.lastIndex) {
                Divider(
                    color = if (index < completedStep) green else darkGrey,
                    thickness = 2.dp,
                    modifier = Modifier
                        .weight(0.2f)
                        .height(2.dp)
                )
            }
        }
    }
}
