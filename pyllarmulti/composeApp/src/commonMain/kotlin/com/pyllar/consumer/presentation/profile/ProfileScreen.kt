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
import com.pyllar.consumer.platform.DeviceInfoProvider
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    viewModel: ProfileViewModel = koinInject(),
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onHelpSupport: () -> Unit = {},
    onBack: () -> Unit = {},
    platformActions: PlatformActions = koinInject(),
    deviceInfoProvider: DeviceInfoProvider = koinInject()
) {
    val profileState by viewModel.profileState.collectAsState()
    var showPersonalDetailsSheet by remember { mutableStateOf(false) }
    var showBankDetailsSheet by remember { mutableStateOf(false) }
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
                isLoading = profileState.isLoading,
                referredByCode = profileState.referredByCode
            )

            // Deletion Status Section
            val deletionCompletionDate = profileState.lastDeletionRequest?.requestedAt?.let { requestedAt ->
                try {
                    val instant = Instant.parse(requestedAt)
                    val completionInstant = instant.plus(30, DateTimeUnit.DAY, TimeZone.UTC)
                    val localDateTime = completionInstant.toLocalDateTime(TimeZone.currentSystemDefault())
                    val monthName = when (localDateTime.monthNumber) {
                        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                        else -> localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    }
                    "${localDateTime.dayOfMonth} $monthName ${localDateTime.year}"
                } catch (e: Exception) {
                    null
                }
            }

            if (profileState.hasPendingDeletionRequest || profileState.lastDeletionRequest != null) {
                DeletionStatusCard(
                    message = profileState.deletionRequestMessage ?: "Your account deletion request is being processed.",
                    completionDate = deletionCompletionDate
                )
            }

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

                // Bank Details Card
                ProfileOptionCard(
                    title = "Bank Details",
                    subtitle = "Your linked bank account",
                    icon = Icons.Filled.AccountBalance,
                    onClick = { showBankDetailsSheet = true }
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

                // Rate App Card
                ProfileOptionCard(
                    title = "Rate Pyllar",
                    subtitle = "Love using Pyllar? Rate us",
                    icon = Icons.Filled.Star,
                    onClick = {
                        platformActions.requestInAppReview(screenName = "ProfileScreen", silentFallback = false, trigger = "manual")
                    }
                )

                // Terms of Use Card
                ProfileOptionCard(
                    title = "Terms of Use",
                    subtitle = "Terms and conditions of using Pyllar",
                    icon = Icons.Filled.Description,
                    onClick = {
                        platformActions.openUrl("https://www.pyllar.in/terms.html")
                    }
                )

                // Privacy Policy Card
                ProfileOptionCard(
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    icon = Icons.Filled.Shield,
                    onClick = {
                        platformActions.openUrl("https://www.pyllar.in/privacy.html")
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val appVersion = deviceInfoProvider.getAppVersion() ?: "1.0.0"
                Text(
                    text = "App Version $appVersion",
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
            dob = profileState.dob,
            gender = profileState.gender,
            onDismiss = { showPersonalDetailsSheet = false }
        )
    }

    if (showBankDetailsSheet) {
        BankDetailsBottomSheet(
            accountNumber = profileState.bankAccountNumber,
            ifscCode = profileState.bankIfscCode,
            bankName = profileState.bankName,
            onDismiss = { showBankDetailsSheet = false }
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
    isLoading: Boolean,
    referredByCode: String? = null
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
            if (!referredByCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Joined via ${referredByCode.uppercase()}'s invite",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
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
            
            if (onClick != null) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsBottomSheet(
    name: String,
    email: String,
    phone: String,
    dob: String,
    gender: String,
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
            PersonalDetailRow("Date of Birth", dob.ifBlank { "Not available" })
            PersonalDetailRow("Gender", gender.ifBlank { "Not available" })
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

@Composable
fun DeletionStatusCard(
    message: String,
    completionDate: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFA726)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "Account Deletion Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFE65100)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D4037)
            )
            completionDate?.let { date ->
                Text(
                    text = "Expected completion by $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D4C41)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailsBottomSheet(
    accountNumber: String,
    ifscCode: String,
    bankName: String,
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
                "Bank Details",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            if (bankName.isNotBlank()) {
                PersonalDetailRow("Bank Name", bankName)
            }

            PersonalDetailRow(
                "Account Number",
                if (accountNumber.isNotBlank()) maskAccountNumber(accountNumber) else "Not available"
            )

            PersonalDetailRow(
                "IFSC Code",
                ifscCode.ifBlank { "Not available" }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun maskAccountNumber(accountNumber: String): String {
    if (accountNumber.length <= 4) return accountNumber
    val lastFour = accountNumber.takeLast(4)
    val maskedLength = accountNumber.length - 4
    val masked = "*".repeat(maskedLength) + lastFour
    return masked
}
