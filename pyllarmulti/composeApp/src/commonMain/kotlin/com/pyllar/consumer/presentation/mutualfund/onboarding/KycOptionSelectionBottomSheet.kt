package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

// Exact color palette matching the UI design
private val V2SheetBg = Color(0xFFFAF7F2)
private val V2ForestGreen = Color(0xFF0F3822)
private val V2ForestGreenButton = Color(0xFF0D2C1B)
private val V2ButtonGoldBorder = Color(0xFFC5A028)
private val V2BronzeMuted = Color(0xFF755C4C)
private val V2SelectedCardBg = Color(0xFFFBF4E2)
private val V2SelectedCardBorder = Color(0xFF997A2E)
private val V2UnselectedCardBorder = Color(0xFFEBE6DD)
private val V2UnselectedTagBg = Color(0xFFEFE8DB)
private val V2UnselectedTagText = Color(0xFF42281D)
private val V2RadioUnselected = Color(0xFFD6D1C7)
private val V2DragHandleColor = Color(0xFFD6CFC7)

enum class KycMethod {
    DIGILOCKER,
    CAMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycOptionSelectionBottomSheet(
    onDismiss: () -> Unit,
    onDigiLockerSelected: () -> Unit,
    onCamsSelected: () -> Unit,
    modifier: Modifier = Modifier,
    camsInProgress: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = V2SheetBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = V2DragHandleColor
            )
        },
        modifier = modifier
    ) {
        KycOptionSelectionContent(
            onDigiLockerSelected = {
                onDismiss()
                onDigiLockerSelected()
            },
            onCamsSelected = {
                onDismiss()
                onCamsSelected()
            },
            camsInProgress = camsInProgress
        )
    }
}

@Composable
fun KycOptionSelectionContent(
    onDigiLockerSelected: () -> Unit,
    onCamsSelected: () -> Unit,
    modifier: Modifier = Modifier,
    camsInProgress: Boolean = false
) {
    var selectedMethod by remember { mutableStateOf(KycMethod.DIGILOCKER) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(Res.string.kyc_selection_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = V2ForestGreen,
                    fontSize = 24.sp
                )
            )
            Text(
                text = stringResource(Res.string.kyc_selection_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = V2BronzeMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )
            if (camsInProgress) {
                Text(
                    text = stringResource(Res.string.kyc_cams_in_progress_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = V2ForestGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Option 1: DigiLocker KYC
        KycSelectableOptionCard(
            badgeText = stringResource(Res.string.kyc_option_digilocker_badge),
            title = stringResource(Res.string.kyc_option_digilocker_title),
            description = stringResource(Res.string.kyc_option_digilocker_desc),
            isSelected = selectedMethod == KycMethod.DIGILOCKER,
            onSelect = { selectedMethod = KycMethod.DIGILOCKER },
            iconContent = {
                DigiLockerCardIcon()
            }
        )

        // Option 2: CAMS KYC
        KycSelectableOptionCard(
            badgeText = stringResource(Res.string.kyc_option_cams_badge),
            title = stringResource(Res.string.kyc_option_cams_title),
            description = stringResource(Res.string.kyc_option_cams_desc),
            isSelected = selectedMethod == KycMethod.CAMS,
            onSelect = { selectedMethod = KycMethod.CAMS },
            iconContent = {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = Color(0xFF4A4A4A),
                    modifier = Modifier.size(26.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom CTA Button
        Button(
            onClick = {
                when (selectedMethod) {
                    KycMethod.DIGILOCKER -> onDigiLockerSelected()
                    KycMethod.CAMS -> onCamsSelected()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.5.dp, V2ButtonGoldBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = V2ForestGreenButton,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (selectedMethod == KycMethod.DIGILOCKER) {
                    stringResource(Res.string.kyc_btn_start_with_digilocker)
                } else {
                    stringResource(Res.string.kyc_btn_start_with_cams)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun KycSelectableOptionCard(
    badgeText: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Pill Tag above card
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isSelected) V2ForestGreen else V2UnselectedTagBg
        ) {
            Text(
                text = badgeText,
                color = if (isSelected) Color.White else V2UnselectedTagText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        // Option Card
        Surface(
            onClick = onSelect,
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) V2SelectedCardBg else Color.White,
            border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) V2SelectedCardBorder else V2UnselectedCardBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left icon container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFEFE8DB), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and description
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = V2ForestGreen,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = V2BronzeMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Radio Button indicator
                CustomRadioButton(isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun CustomRadioButton(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Box(
            modifier = modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(4.dp, V2ForestGreen, CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, V2RadioUnselected, CircleShape)
        )
    }
}

@Composable
private fun DigiLockerCardIcon() {
    Box(
        modifier = Modifier
            .size(34.dp, 22.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFFAFAFA))
            .border(0.8.dp, Color(0xFFDCD6CC), RoundedCornerShape(3.dp))
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emblem circle on the left
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5A93C))
            )
            Spacer(modifier = Modifier.width(3.dp))
            // Lines representing text on card
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFFB0A89A))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.dp)
                        .background(Color(0xFFB0A89A))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .background(Color(0xFFD0C8BC))
                )
            }
        }
    }
}

