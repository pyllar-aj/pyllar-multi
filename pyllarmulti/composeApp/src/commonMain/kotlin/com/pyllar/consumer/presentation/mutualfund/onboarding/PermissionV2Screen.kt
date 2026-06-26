package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.platform.PlatformActions
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.presentation.auth.permission.PermissionViewModel
import com.pyllar.consumer.presentation.auth.permission.PermissionFlowState
import com.pyllar.consumer.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pyllar.composeapp.generated.resources.*

private val V2Cream = Color(0xFFFBF9F4)
private val V2DarkBrown = Color(0xFF3E2723)
private val V2MediumBrown = Color(0xFF6D4C41)
private val V2Gold = Color(0xFFD4AF37)
private val V2GoldDark = Color(0xFF8B6B25)
private val V2DarkGreen = Color(0xFF0A2415)
private val V2MediumGreen = Color(0xFF1A7A42)
private val V2LightGreen = Color(0xFF2E7D32)
private val V2CreamTint = Color(0xFFF5EEDB)
private val V2BorderGold28 = Color(0xFF8B6B25).copy(alpha = 0.26f)
private val V2IconBgGold10 = Color(0xFF0A2415).copy(alpha = 0.07f)
private val V2IconBgGold16 = Color(0xFF0A2415).copy(alpha = 0.16f)

private fun isValidEmail(value: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
    return value.isNotBlank() && emailRegex.matches(value)
}

@Composable
fun PermissionV2Screen(
    userId: String,
    isNewUser: Boolean,
    viewModel: PermissionViewModel = koinInject(),
    platformActions: PlatformActions = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    onNavigateNext: (nextScreen: String) -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isNavigating by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    // Sync current OS permission state on entry
    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // Sync on resume (e.g. returning from Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // Handle API result — navigate or show error
    LaunchedEffect(state.updateEmailResult) {
        val result = state.updateEmailResult ?: return@LaunchedEffect
        when (result) {
            is com.pyllar.consumer.util.Resource.Success -> {
                val nav = result.navigation
                when (nav?.action) {
                    NavigationAction.STAY, NavigationAction.RETRY -> {
                        viewModel.clearResult()
                    }
                    NavigationAction.POLL -> {
                        viewModel.clearResult()
                    }
                    else -> {
                        val nextScreen = nav?.nextScreen
                        if (!nextScreen.isNullOrBlank()) {
                            isNavigating = true
                            onNavigateNext(nextScreen)
                            viewModel.clearResult()
                        } else {
                            viewModel.clearResult()
                        }
                    }
                }
            }
            is com.pyllar.consumer.util.Resource.Error -> {
                // Handled via state.serverErrorMessage display
            }
            else -> Unit
        }
    }

    val isSuccessNavigating = state.updateEmailResult is com.pyllar.consumer.util.Resource.Success &&
            state.updateEmailResult?.navigation?.action != NavigationAction.STAY &&
            state.updateEmailResult?.navigation?.action != NavigationAction.RETRY &&
            !state.updateEmailResult?.navigation?.nextScreen.isNullOrBlank()

    val showLoading = isNavigating ||
            (state.isProcessing && state.updateEmailResult is com.pyllar.consumer.util.Resource.Loading) ||
            isSuccessNavigating

    if (showLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(V2Cream),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = V2Gold)
        }
        return
    }

    var showManualInput by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(V2Cream)
            .statusBarsPadding()
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onNavigateToHelp()
                }
            ) {
                Text(
                    text = stringResource(Res.string.help),
                    style = MaterialTheme.typography.labelLarge,
                    color = V2MediumGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        HorizontalDivider(color = V2GoldDark.copy(alpha = 0.13f))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(Res.string.permission_v2_eyebrow),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = V2Gold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.permission_v2_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = V2DarkBrown,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(Res.string.permission_v2_subtitle),
                fontSize = 12.sp,
                color = V2MediumBrown,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Permission Card
            PermissionRowCardV2(
                emoji = "🔔",
                title = stringResource(Res.string.permission_v2_notif_title),
                description = stringResource(Res.string.permission_v2_notif_desc),
                isGranted = state.permissionStatus.notificationsGranted,
                onAllow = {
                    scope.launch {
                        permissionManager.requestNotifications()
                        viewModel.refreshPermissionStatus()
                    }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(Res.string.permission_v2_change_anytime),
                fontSize = 10.sp,
                color = V2GoldDark
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = V2GoldDark.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(13.dp))

            // Email Card Section
            EmailCardV2(
                email = state.email,
                showManualInput = showManualInput,
                showError = state.showEmailError,
                onEmailChange = { viewModel.updateEmail(it) },
                onToggleManualInput = { showManualInput = !showManualInput }
            )

            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(Res.string.permission_v2_email_note),
                fontSize = 10.sp,
                color = V2MediumBrown,
                lineHeight = 14.sp
            )

            if (!state.serverErrorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.serverErrorMessage!!,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CTA: gold gradient border wrapping dark green button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(V2Gold, V2GoldDark)))
                    .padding(1.5.dp)
            ) {
                Button(
                    onClick = {
                        val currentTime = currentTimeMillis()
                        if (currentTime - lastClickTime > 1000) {
                            lastClickTime = currentTime
                            if (!isValidEmail(state.email)) {
                                viewModel.triggerEmailError()
                                return@Button
                            }
                            scope.launch {
                                if (!state.permissionStatus.notificationsGranted) {
                                    permissionManager.requestNotifications()
                                    viewModel.refreshPermissionStatus()
                                    delay(300)
                                }
                                viewModel.submitEmail(userId)
                            }
                        }
                    },
                    enabled = !state.isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = V2DarkGreen,
                        contentColor = V2Cream,
                        disabledContainerColor = V2DarkGreen.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = V2Cream,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.btn_continue),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = V2Cream
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "→",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = V2Gold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(Res.string.permission_v2_trust_sebi), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = V2LightGreen)
                Text(text = " · ", fontSize = 9.sp, color = V2DarkBrown.copy(alpha = 0.25f))
                Text(text = stringResource(Res.string.permission_v2_trust_no_spam), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = V2LightGreen)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRowCardV2(
    emoji: String,
    title: String,
    description: String,
    isGranted: Boolean,
    onAllow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFFFBF9F4), Color(0xFFF5EEDB))),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, Color(0xFF8B6B25).copy(alpha = 0.26f), RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFF0A2415).copy(alpha = 0.07f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color(0xFF6D4C41),
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Text(
                text = "✓",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF0A2415))
                    .clickable { onAllow() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.permission_v2_allow),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBF9F4)
                )
            }
        }
    }
}

@Composable
private fun EmailCardV2(
    email: String,
    showManualInput: Boolean,
    showError: Boolean,
    onEmailChange: (String) -> Unit,
    onToggleManualInput: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFFFBF9F4), Color(0xFFF5EEDB))),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, Color(0xFF8B6B25).copy(alpha = 0.26f), RoundedCornerShape(14.dp))
            .padding(13.dp)
    ) {
        Text(
            text = stringResource(Res.string.communication_email),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3E2723)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (showManualInput) {
            var isEmailFocused by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(200)
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {}
                keyboardController?.show()
            }

            BasicTextField(
                value = email,
                onValueChange = onEmailChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723)),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.5.dp, if (showError) MaterialTheme.colorScheme.error else Color(0xFF8B6B25).copy(alpha = 0.36f), RoundedCornerShape(10.dp))
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isEmailFocused = focusState.isFocused
                    }
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                decorationBox = { inner ->
                    if (email.isEmpty()) {
                        Text(stringResource(Res.string.permission_v2_email_placeholder), fontSize = 12.sp, color = Color(0xFF6D4C41))
                    }
                    inner()
                }
            )
            if (showError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.permission_v2_email_invalid),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = stringResource(Res.string.permission_v2_use_detected_email),
                fontSize = 10.sp,
                color = Color(0xFF1A7A42),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onToggleManualInput() }
            )
        } else {
            EmailChooserWrapper(
                onEmailPicked = { pickedEmail ->
                    onEmailChange(pickedEmail)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.5.dp, if (showError) MaterialTheme.colorScheme.error else Color(0xFF8B6B25).copy(alpha = 0.36f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✉️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = email.ifBlank { stringResource(Res.string.select_your_email) },
                        fontSize = 12.sp,
                        fontWeight = if (email.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                        color = if (email.isNotBlank()) Color(0xFF3E2723) else Color(0xFF6D4C41),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "▾", fontSize = 12.sp, color = Color(0xFF8B6B25))
                }
            }
            if (showError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.please_select_email),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = stringResource(Res.string.permission_v2_type_different_email),
                fontSize = 10.sp,
                color = Color(0xFF1A7A42),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onToggleManualInput() }
            )
        }
    }
}
