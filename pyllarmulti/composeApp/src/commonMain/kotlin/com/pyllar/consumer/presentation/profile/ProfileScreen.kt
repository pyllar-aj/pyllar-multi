package com.pyllar.consumer.presentation.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.pyllar.consumer.platform.PlatformActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    viewModel: ProfileViewModel = koinInject(),
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
    onBack: () -> Unit = {},
    platformActions: PlatformActions = koinInject()
) {
    val profileState by viewModel.profileState.collectAsState()
    var showPersonalDetailsSheet by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2E7D32))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Section with Dark Green Background
            UserProfileHeader(
                userName = profileState.name,
                email = profileState.email,
                phoneNumber = profileState.phoneNumber,
                isLoading = profileState.isLoading
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Details Card
                ProfileOptionCard(
                    title = "Personal Details",
                    subtitle = "Your profile key information",
                    icon = Icons.Filled.Person,
                    onClick = { showPersonalDetailsSheet = true }
                )

                // Manage Account Card
                ProfileOptionCard(
                    title = "Manage Account",
                    subtitle = "Logout and other options",
                    icon = Icons.Filled.Settings,
                    onClick = { showManageSheet = true }
                )

                // Need Help? Card
                ProfileOptionCard(
                    title = "Need Help?",
                    subtitle = "Write to us regarding your queries",
                    icon = Icons.Filled.Help,
                    onClick = { onHelpSupport() }
                )

                // Share App Card
                ProfileOptionCard(
                    title = "Share Pyllar",
                    subtitle = "Tell your friends about Pyllar",
                    icon = Icons.Filled.Share,
                    onClick = {
                        platformActions.shareText("Build your wealth with Pyllar! https://pyllar.in", "Share Pyllar")
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "App Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPersonalDetailsSheet) {
        PersonalDetailsBottomSheet(
            name = profileState.name,
            email = profileState.email,
            phone = profileState.phoneNumber,
            onDismiss = { showPersonalDetailsSheet = false }
        )
    }

    if (showManageSheet) {
        ManageAccountBottomSheet(
            onLogoutClick = {
                showManageSheet = false
                viewModel.logout { onLogout() }
            },
            onDeleteAccountClick = {
                showManageSheet = false
                onDeleteAccount()
            },
            onDismiss = { showManageSheet = false }
        )
    }
}

@Composable
fun UserProfileHeader(
    userName: String,
    email: String,
    phoneNumber: String,
    isLoading: Boolean
) {
    val firstLetter = if (userName.isNotBlank()) userName.take(1).uppercase() else "U"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2E7D32),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color.White
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = firstLetter,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
        } else {
            Text(
                text = userName.ifBlank { "Pyllar User" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            if (phoneNumber.isNotBlank()) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ProfileOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsBottomSheet(
    name: String,
    email: String,
    phone: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Personal Details",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            
            PersonalDetailRow("Name", name.ifBlank { "Not available" })
            PersonalDetailRow("Email", email.ifBlank { "Not available" })
            PersonalDetailRow("Phone", phone.ifBlank { "Not available" })
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PersonalDetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountBottomSheet(
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Manage Account", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onLogoutClick() }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Logout from this device", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onDeleteAccountClick() }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete account", tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Delete this account", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

