package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*
import org.koin.compose.koinInject

private val V2Cream = Color(0xFFFBF9F4)
private val V2BronzeInk = Color(0xFF3E2723)
private val V2BronzeMuted = Color(0xFF6D4C41)
private val V2GoldDeep = Color(0xFF8B6B25)
private val V2GoldAccent = Color(0xFFD4AF37)
private val V2Obsidian = Color(0xFF0A2415)
private val V2SuccessGreen = Color(0xFF2E7D32)
private val V2ErrorRed = Color(0xFFC62828)
private val V2WarmGreyBorder = Color(0xFFD7CCC8)
private val V2MutedText = Color(0xFFB0A89A)
private val ScrimColor = Color(0x7A140C08) // 48% opacity dark scrim

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun UpiFetchSheetScreen(
    navController: NavController,
    viewModel: UpiFetchViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val upiBringIntoViewRequester = remember { BringIntoViewRequester() }
    var isUpiFocused by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(isUpiFocused) {
        if (isUpiFocused) {
            kotlinx.coroutines.delay(300)
            upiBringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(uiState.fetchSuccess) {
        if (uiState.fetchSuccess) {
            val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            savedStateHandle?.set("prefilledName", uiState.resolvedName)
            savedStateHandle?.set("prefilledDob", uiState.resolvedDob)
            savedStateHandle?.set("prefilledPan", uiState.resolvedPan)
            navController.popBackStack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed Background mockup representing the form underneath
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(V2Cream)
                .alpha(0.55f)
        ) {
            // Header Mockup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Pyllar", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = V2Obsidian)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "Money", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = V2GoldAccent)
                }
                Row {
                    Text(text = "EN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A7A42), modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "Help", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A7A42), modifier = Modifier.padding(horizontal = 8.dp))
                }
            }

            // Stepper Mockup
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepperStepMock(number = "1", text = "Your details", isActive = true)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(26.dp).height(1.5.dp).background(V2WarmGreyBorder))
                    Spacer(modifier = Modifier.width(4.dp))
                    StepperStepMock(number = "2", text = "Bank account", isActive = false)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(26.dp).height(1.5.dp).background(V2WarmGreyBorder))
                    Spacer(modifier = Modifier.width(4.dp))
                    StepperStepMock(number = "3", text = "KYC", isActive = false)
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(26.dp).height(1.5.dp).background(V2WarmGreyBorder))
                    Spacer(modifier = Modifier.width(4.dp))
                    StepperStepMock(number = "4", text = "e-Sign", isActive = false)
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEFEBE9)))
            }

            // Form Content Mockup
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0x478B6B25), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(Res.string.upi_promo_skip_form), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = V2BronzeInk)
                    }
                    Box(
                        modifier = Modifier
                            .border(1.5.dp, V2GoldAccent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = stringResource(Res.string.upi_promo_try_it_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = V2GoldDeep)
                    }
                }

                // Details Card mockup
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFEFEBE9), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Text(text = "Personal details", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = V2Obsidian)
                    Text(text = "Takes about 2 minutes to complete.", fontSize = 12.sp, color = V2BronzeMuted)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1A8B6B25)))
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(text = "FULL NAME AS PER PAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = V2BronzeInk)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.5.dp, V2WarmGreyBorder, RoundedCornerShape(12.dp))
                            .background(Color.White)
                    )
                }
            }
        }

        // Scrim Color Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        )

        // Bottom Sheet Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(V2Cream)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag handle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(V2GoldAccent.copy(alpha = 0.3f))
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.upi_promo_skip_form_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = V2Obsidian
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.upi_promo_enter_upi_id_user_info),
                            fontSize = 13.sp,
                            color = V2BronzeMuted,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(V2GoldDeep.copy(alpha = 0.1f))
                            .clickable {
                                navController.popBackStack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = V2BronzeMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.dp)
                        .background(V2GoldDeep.copy(alpha = 0.12f))
                )

                // UPI input section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                ) {
                    Text(
                        text = "YOUR UPI ID",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = V2BronzeInk,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Text Field Container
                    val fieldBorderColor = when {
                        uiState.fetchError -> V2ErrorRed
                        uiState.fetchSuccess || (uiState.upi.trim().length > 2 && uiState.upi.contains("@")) -> V2SuccessGreen
                        else -> V2WarmGreyBorder
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, fieldBorderColor, RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextFieldMock(
                                value = uiState.upi,
                                onValueChange = { viewModel.onUpiInputChanged(it) },
                                placeholder = "yourname@okaxis",
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(upiBringIntoViewRequester)
                                    .onFocusChanged { isUpiFocused = it.isFocused }
                            )

                            if (uiState.fetchSuccess) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(V2SuccessGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            } else if (uiState.isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = V2GoldAccent,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    val isNetError = uiState.errorMessage?.let { msg ->
                        val lower = msg.lowercase()
                        lower.contains("unable to resolve host") ||
                        lower.contains("connect") ||
                        lower.contains("timeout") ||
                        lower.contains("network")
                    } == true

                    // Hint / Info text
                    val hintText = when {
                        isNetError -> stringResource(Res.string.check_internet_connection)
                        uiState.fetchError -> uiState.errorMessage ?: stringResource(Res.string.upi_fetch_failed_error)
                        uiState.fetchSuccess -> stringResource(Res.string.upi_fetch_success_hint)
                        uiState.upi.trim().isNotEmpty() && !uiState.upi.contains("@") -> "Format: name@bankhandle"
                        else -> "e.g. yourname@okicici · yourname@ybl"
                    }
                    val hintColor = when {
                        uiState.fetchError -> V2ErrorRed
                        uiState.fetchSuccess -> V2SuccessGreen
                        else -> V2MutedText
                    }

                    Text(
                        text = hintText,
                        fontSize = 12.sp,
                        color = hintColor,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(min = 18.dp)
                    )

                    // Success Card
                    AnimatedVisibility(
                        visible = uiState.fetchSuccess,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(250))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .border(1.dp, V2SuccessGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .background(V2SuccessGreen.copy(alpha = 0.07f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(V2SuccessGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(Res.string.upi_fetch_details_found_title),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = V2SuccessGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (!uiState.resolvedName.isNullOrBlank()) {
                                    Text(
                                        text = "Name: ${uiState.resolvedName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = V2BronzeInk,
                                        lineHeight = 17.sp
                                    )
                                }
                                if (!uiState.resolvedDob.isNullOrBlank()) {
                                    Text(
                                        text = "DOB: ${uiState.resolvedDob}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = V2BronzeInk,
                                        lineHeight = 17.sp
                                    )
                                }
                                if (!uiState.resolvedPan.isNullOrBlank()) {
                                    Text(
                                        text = "PAN: ${uiState.resolvedPan}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = V2BronzeInk,
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.upi_fetch_details_found_body),
                                    fontSize = 12.sp,
                                    color = V2BronzeMuted,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // CTA Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hasReachedLimit = uiState.errorMessage?.let { msg ->
                        val lower = msg.lowercase()
                        lower.contains("attempt") ||
                        lower.contains("limit") ||
                        lower.contains("exceed") ||
                        lower.contains("different upi") ||
                        lower.contains("verify up to")
                    } == true && !uiState.fetchSuccess
                    val canFetch = uiState.upi.trim().isNotEmpty() && !uiState.isFetching && !hasReachedLimit
                    val fetchOpacity by animateFloatAsState(if (canFetch || uiState.fetchSuccess) 1f else 0.42f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(fetchOpacity)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(V2GoldAccent, V2GoldDeep)
                                )
                            )
                            .padding(1.5.dp)
                    ) {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                if (uiState.fetchSuccess) {
                                    val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                                    savedStateHandle?.set("prefilledName", uiState.resolvedName)
                                    savedStateHandle?.set("prefilledDob", uiState.resolvedDob)
                                    savedStateHandle?.set("prefilledPan", uiState.resolvedPan)
                                    navController.popBackStack()
                                } else {
                                    viewModel.fetchDetails()
                                }
                            },
                            enabled = canFetch || uiState.fetchSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = V2Obsidian,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (uiState.isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.upi_fetch_loading),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val label = if (uiState.fetchError) stringResource(Res.string.try_again) else if (uiState.fetchSuccess) stringResource(Res.string.user_info_btn_confirm_continue) else stringResource(Res.string.btn_fetch_my_details)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "→",
                                        fontSize = 17.sp,
                                        color = V2GoldAccent
                                    )
                                }
                            }
                        }
                    }

                    // Secondary CTA
                    Button(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.5.dp, V2WarmGreyBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = V2BronzeMuted
                        )
                    ) {
                        Text(
                            text = stringResource(Res.string.btn_fill_form_manually),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun StepperStepMock(number: String, text: String, isActive: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isActive) V2Obsidian else Color.White)
                .border(1.5.dp, if (isActive) V2Obsidian else V2WarmGreyBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) V2GoldAccent else V2MutedText
            )
        }
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) V2Obsidian else V2MutedText
        )
    }
}

@Composable
fun BasicTextFieldMock(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = V2MutedText,
                fontSize = 15.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = V2BronzeInk,
                fontSize = 15.sp
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
