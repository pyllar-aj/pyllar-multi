package com.pyllar.consumer.presentation.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.presentation.ui.components.StandardTextFieldNewTwo
import com.pyllar.consumer.presentation.ui.theme.TrueWhite
import com.pyllar.consumer.util.Resource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToOtp: (phoneNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = koinInject()
    val brush = Brush.linearGradient(
        colors = listOf(TrueWhite, TrueWhite),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    )

    var phoneNumber by remember { mutableStateOf("") }
    val loginResult by authViewModel.loginResult.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is Resource.Success -> {
                result.data?.let {
                    onNavigateToOtp("+91$phoneNumber")
                }
            }
            is Resource.Error -> {
                errorMessage = result.message ?: "Failed to send OTP"
            }
            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        StandardTextFieldNewTwo(
            text = phoneNumber,
            onValueChange = {
                errorMessage = null
                phoneNumber = if (it.isNotEmpty() && it.first() != '0')
                    it.filter { char -> char.isDigit() } else ""
            },
            hint = "Mobile Number",
            maxLength = 10,
            keyboardType = KeyboardType.Number,
            isFlagVisible = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = null
                authViewModel.sendOtpParams(OtpRegistrationRequest(phoneNumber, ""))
            },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.isNotEmpty() && phoneNumber.length == 10
                    && loginResult !is Resource.Loading
        ) {
            if (loginResult is Resource.Loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = TrueWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sending...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TrueWhite)
                    )
                }
            } else {
                Text(
                    text = "Get Verification Code",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TrueWhite),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
