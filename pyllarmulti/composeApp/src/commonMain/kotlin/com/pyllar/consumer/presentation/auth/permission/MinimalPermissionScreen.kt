package com.pyllar.consumer.presentation.auth.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.presentation.auth.permission.EmailInputSection
import com.pyllar.consumer.presentation.auth.permission.PermissionFlowState
import com.pyllar.consumer.presentation.auth.permission.PermissionViewModel
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

@Composable
fun MinimalPermissionScreen(
    userId: String,
    isNewUser: Boolean,
    viewModel: PermissionViewModel = koinInject(),
    onNavigateNext: (nextScreen: String) -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onShareApp: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Sync current OS permission state on entry
    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    // Sync on resume (e.g. returning from Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                com.pyllar.consumer.util.Log.d("PermissionFlow", "App resumed, refreshing status")
                viewModel.onResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // Handle API result — navigate or show error
    LaunchedEffect(state.updateEmailResult) {
        val result = state.updateEmailResult ?: return@LaunchedEffect
        com.pyllar.consumer.util.Log.d("PermissionFlow", "updateEmailResult changed: $result")
        when (result) {
            is com.pyllar.consumer.util.Resource.Success -> {
                val nav = result.navigation
                com.pyllar.consumer.util.Log.d("PermissionFlow", "Navigation info: $nav")
                when (nav?.action) {
                    NavigationAction.STAY, NavigationAction.RETRY -> {
                        com.pyllar.consumer.util.Log.d("PermissionFlow", "Server says STAY or RETRY")
                        viewModel.clearResult()
                    }
                    NavigationAction.POLL -> {
                        com.pyllar.consumer.util.Log.d("PermissionFlow", "Server says POLL")
                        viewModel.clearResult()
                    }
                    else -> {
                        val nextScreen = nav?.nextScreen
                        com.pyllar.consumer.util.Log.d("PermissionFlow", "Next screen: $nextScreen")
                        if (!nextScreen.isNullOrBlank()) {
                            com.pyllar.consumer.util.Log.d("PermissionFlow", "Triggering navigation to: $nextScreen")
                            onNavigateNext(nextScreen)
                            viewModel.clearResult()
                        } else {
                            com.pyllar.consumer.util.Log.d("PermissionFlow", "Next screen is null/blank, clearing result")
                            viewModel.clearResult()
                        }
                    }
                }
            }
            is com.pyllar.consumer.util.Resource.Error -> {
                com.pyllar.consumer.util.Log.e("PermissionFlow", "Error updating email: ${result.message}")
            }
            else -> Unit
        }
    }

    // Full-screen loading overlay while API call is in flight
    if (state.isProcessing && state.updateEmailResult is com.pyllar.consumer.util.Resource.Loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top row: Share + Help
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onShareApp) {
                Text(stringResource(Res.string.share), color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onNavigateToHelp) {
                Text(stringResource(Res.string.help), color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.lets_set_things_up),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.permissions_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Permission cards
            PermissionStatusCard(
                icon = { Text("🔔", style = MaterialTheme.typography.titleLarge) },
                title = stringResource(Res.string.notification_permission),
                description = stringResource(Res.string.notification_description),
                isGranted = state.permissionStatus.notificationsGranted
            )
            PermissionStatusCard(
                icon = { Text("📍", style = MaterialTheme.typography.titleLarge) },
                title = stringResource(Res.string.location_permission),
                description = stringResource(Res.string.location_description),
                isGranted = state.permissionStatus.locationGranted
            )

            // Email — platform-specific
            EmailInputSection(
                email = state.email,
                onEmailChange = { viewModel.updateEmail(it) },
                showError = state.showEmailError
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Please select your personal email address to receive important updates about your investments, redemptions, and holdings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )

            // Consent checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = state.isConsentChecked,
                    onCheckedChange = { viewModel.toggleConsent(it) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.email_consent_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Local permission error messages when denied/disabled
            val showLocalPermissionError = !state.permissionStatus.locationGranted || !state.permissionStatus.gpsEnabled
            val isFlowAttempted = state.permissionFlow is PermissionFlowState.Completed
            if (isFlowAttempted && showLocalPermissionError) {
                val permissionErrorMsg = when {
                    !state.permissionStatus.locationGranted && !state.permissionStatus.gpsEnabled ->
                        stringResource(Res.string.location_and_gps_required_error)
                    !state.permissionStatus.locationGranted ->
                        stringResource(Res.string.location_permission_required_error)
                    else ->
                        stringResource(Res.string.gps_required_error)
                }
                Text(
                    text = permissionErrorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            // Server error message
            if (!state.serverErrorMessage.isNullOrBlank()) {
                Text(
                    text = state.serverErrorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // CTA button
            val buttonLabel = when (state.permissionFlow) {
                is PermissionFlowState.Idle ->
                    if (state.permissionStatus.locationGranted && state.permissionStatus.gpsEnabled)
                        stringResource(Res.string.btn_continue) else stringResource(Res.string.grant_permissions)
                is PermissionFlowState.Completed ->
                    if (state.permissionStatus.locationGranted && state.permissionStatus.gpsEnabled)
                        stringResource(Res.string.btn_continue) else stringResource(Res.string.go_to_settings)
                is PermissionFlowState.RequestingNotifications -> stringResource(Res.string.requesting_notifications)
                is PermissionFlowState.RequestingLocation -> stringResource(Res.string.requesting_location)
                is PermissionFlowState.CheckingGps -> stringResource(Res.string.checking_gps)
            }
            val buttonEnabled = state.isConsentChecked &&
                    !state.isProcessing &&
                    state.permissionFlow !is PermissionFlowState.RequestingNotifications &&
                    state.permissionFlow !is PermissionFlowState.RequestingLocation &&
                    state.permissionFlow !is PermissionFlowState.CheckingGps

            Button(
                onClick = { viewModel.onGrantPermissionsTapped(userId) },
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (state.isProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.processing), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(buttonLabel, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    isGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isGranted) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
