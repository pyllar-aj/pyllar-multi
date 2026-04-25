package com.pyllar.consumer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.auth.permission.PermissionViewModel
import com.pyllar.consumer.presentation.auth.permission.MinimalPermissionScreen
import com.pyllar.consumer.presentation.auth.phone.PhoneVerificationScreen
import com.pyllar.consumer.presentation.auth.phone.PhoneVerificationViewModel
import com.pyllar.consumer.presentation.auth.phone.OtpVerificationScreen
import com.pyllar.consumer.presentation.auth.phone.OtpVerificationViewModel
import com.pyllar.consumer.presentation.dashboard.InitialDashboardScreen
import com.pyllar.consumer.presentation.dashboard.InitialDashboardViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.*
import com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingScreen
import com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingViewModel
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.util.Resource
import org.koin.compose.koinInject

sealed class Screen {
    object PhoneVerification : Screen()
    data class OtpVerification(val phoneNumber: String, val otpRef: String?) : Screen()
    data class MinimalPermission(
        val userId: String,
        val isNewUser: Boolean,
        val nextScreen: String? = null
    ) : Screen()
    data class PanKyc(val userId: String, val preVerificationId: String?) : Screen()
    data class MinDetails(
        val userId: String,
        val pan: String,
        val email: String,
        val phone: String,
        val token: String
    ) : Screen()
    data class NameDob(
        val userId: String,
        val pan: String,
        val email: String,
        val phone: String,
        val token: String
    ) : Screen()
    data class CheckPanPopulatedDetails(val userId: String, val preVerificationId: String?) : Screen()
    data class InitialDashboard(val userId: String) : Screen()
    data class UpiAccountLinking(val userId: String) : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.PhoneVerification) }

        when (val screen = currentScreen) {
            is Screen.PhoneVerification -> {
                val phoneVm: PhoneVerificationViewModel = koinInject()
                PhoneVerificationScreen(
                    viewModel = phoneVm,
                    onPhoneVerified = { number ->
                        val authToken = phoneVm.verificationResult.value
                        val ref = if (authToken is Resource.Success) {
                            authToken.data?.otpRef
                        } else null
                        currentScreen = Screen.OtpVerification(number, ref)
                    }
                )
            }
            is Screen.OtpVerification -> {
                val otpVm: OtpVerificationViewModel = koinInject()
                LaunchedEffect(screen) {
                    otpVm.setPhoneNumber(screen.phoneNumber)
                    otpVm.setOtpRef(screen.otpRef)
                }
                OtpVerificationScreen(
                    phoneNumber = screen.phoneNumber,
                    viewModel = otpVm,
                    onNavigateToPermissionScreen = { isNewUser, nextScreen, userId ->
                        currentScreen = Screen.MinimalPermission(
                            userId = userId,
                            isNewUser = isNewUser,
                            nextScreen = nextScreen
                        )
                    },
                    onNavigateBack = {
                        currentScreen = Screen.PhoneVerification
                    }
                )
            }
            is Screen.MinimalPermission -> {
                val permVm: PermissionViewModel = koinInject()
                MinimalPermissionScreen(
                    userId = screen.userId,
                    isNewUser = screen.isNewUser,
                    viewModel = permVm,
                    onNavigateNext = { nextScreen ->
                        handleNavigation(nextScreen, screen.userId, null) { currentScreen = it }
                    }
                )
            }
            is Screen.PanKyc -> {
                val panVm: PanKycViewModel = koinInject()
                PanKycScreen(
                    onPanVerified = { pan, nextScreen, _, _, _ ->
                        // In a real app, we'd fetch email/phone/token from a session or previous step
                        // For this migration, we use placeholders if not available
                        currentScreen = Screen.MinDetails(
                            userId = screen.userId,
                            pan = pan,
                            email = "", 
                            phone = "",
                            token = ""
                        )
                    },
                    viewModel = panVm
                )
            }
            is Screen.MinDetails -> {
                val minVm: MinDetailsViewModel = koinInject()
                MinDetailsScreen(
                    onNext = { nextScreen, _ ->
                        handleNavigation(nextScreen, screen.userId, null) { currentScreen = it }
                    },
                    viewModel = minVm,
                    userId = screen.userId,
                    pan = screen.pan,
                    email = screen.email,
                    phone = screen.phone,
                    token = screen.token
                )
            }
            is Screen.NameDob -> {
                val nameVm: NameDobViewModel = koinInject()
                NameDobScreen(
                    onKycSubmitted = { _, _, _, _ ->
                        handleNavigation(ScreenNames.INITIAL_DASHBOARD, screen.userId, null) { currentScreen = it }
                    },
                    userId = screen.userId,
                    pan = screen.pan,
                    email = screen.email,
                    phone = screen.phone,
                    token = screen.token
                )
            }
            is Screen.CheckPanPopulatedDetails -> {
                val checkVm: CheckPanPopulatedDetailsViewModel = koinInject()
                CheckPanPopulatedDetailsScreen(
                    onSubmit = { name, gender, dob, father, marital, perm, corr ->
                        checkVm.submitDetails(screen.userId, screen.preVerificationId, name, gender, dob, father, marital, perm, corr)
                        handleNavigation(ScreenNames.INITIAL_DASHBOARD, screen.userId, screen.preVerificationId) { currentScreen = it }
                    }
                )
            }
            is Screen.InitialDashboard -> {
                InitialDashboardScreen(
                    userId = screen.userId,
                    onNavigateToOnboarding = { _, _ -> /* Handled via server action */ },
                    onNavigateToRoute = { _ -> /* Navigation logic */ }
                )
            }
            is Screen.UpiAccountLinking -> {
                val upiVm: UpiAccountLinkingViewModel = koinInject()
                UpiAccountLinkingScreen(
                    viewModel = upiVm,
                    onAccountLinked = { /* Linked */ },
                    onNavigateBack = { /* Back */ }
                )
            }
        }
    }
}

private fun handleNavigation(
    action: String?,
    userId: String,
    preVerificationId: String?,
    onNavigate: (Screen) -> Unit
) {
    when (action) {
        ScreenNames.PAN_KYC -> onNavigate(Screen.PanKyc(userId, preVerificationId))
        ScreenNames.MIN_DETAILS -> onNavigate(Screen.MinDetails(userId, "", "", "", ""))
        ScreenNames.NAME_DOB -> onNavigate(Screen.NameDob(userId, "", "", "", ""))
        ScreenNames.CHECK_PAN_POPULATED_DETAILS -> onNavigate(Screen.CheckPanPopulatedDetails(userId, preVerificationId))
        ScreenNames.INITIAL_DASHBOARD, ScreenNames.DASHBOARD -> onNavigate(Screen.InitialDashboard(userId))
        else -> onNavigate(Screen.InitialDashboard(userId))
    }
}