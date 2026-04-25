package com.pyllar.consumer.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeletionScreen(
    userId: String,
    viewModel: ProfileViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    val profileState by viewModel.profileState.collectAsState()
    var showConfirmation by remember { mutableStateOf(false) }

    val requestAlreadySubmitted = profileState.hasPendingDeletionRequest || profileState.lastDeletionRequest != null

    LaunchedEffect(userId) {
        if (userId.isBlank()) viewModel.clearDeletionMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Delete Your Account",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

            Text("If you choose to delete your account, all your personal data will be permanently removed.",
                style = MaterialTheme.typography.bodyMedium)

            Text("This action is irreversible. Please make sure you want to proceed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(8.dp))

            profileState.deletionRequestMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            profileState.deletionRequestError?.let { error ->
                Text(error, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error)
            }

            if (profileState.hasPendingDeletionRequest) {
                profileState.lastDeletionRequest?.requestId?.let { requestId ->
                    Text("Pending request ID: $requestId", style = MaterialTheme.typography.bodySmall)
                }
                Text("Your request will be processed within 30 days.",
                    style = MaterialTheme.typography.bodySmall)
            }

            if (profileState.isDeletionRequestInProgress) {
                CircularProgressIndicator()
            }

            if (!requestAlreadySubmitted) {
                Button(
                    onClick = { showConfirmation = true },
                    enabled = userId.isNotBlank() && !profileState.isDeletionRequestInProgress,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Request Account Deletion")
                }
            } else {
                Text("Your deletion request has been submitted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Go Back")
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Account Deletion") },
            text = { Text("Are you sure you want to permanently delete your Pyllar account? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmation = false
                    if (userId.isNotBlank()) viewModel.requestAccountDeletion(userId)
                }) { Text("Yes, Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Cancel") }
            }
        )
    }
}
