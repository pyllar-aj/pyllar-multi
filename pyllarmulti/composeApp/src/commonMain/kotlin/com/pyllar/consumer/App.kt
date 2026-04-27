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
import com.pyllar.consumer.presentation.dashboard.*
import com.pyllar.consumer.presentation.mutualfund.details.*
import com.pyllar.consumer.presentation.mutualfund.onboarding.*
import com.pyllar.consumer.presentation.home.*
import com.pyllar.consumer.presentation.notification.*
import com.pyllar.consumer.presentation.profile.*
import com.pyllar.consumer.presentation.support.*
import com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingScreen
import com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingViewModel
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
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
    data class PreVerification(val userId: String) : Screen()
    data class AdditionalKyc(val userId: String, val kycAttemptId: String) : Screen()
    data class NomineeDetails(val userId: String, val kycAttemptId: String, val investorId: String) : Screen()
    data class BankDetails(val userId: String, val kycAttemptId: String) : Screen()
    data class KycInformation(val userId: String) : Screen()
    data class EsignInformation(val userId: String) : Screen()
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
    data class InvestmentDashboard(val userId: String) : Screen()
    data class SchemeDetails(val userId: String, val purpose: String) : Screen()
    data class Withdraw(val userId: String) : Screen()
    data class FundDetails(val isin: String, val userId: String, val goalId: String, val sipAmount: Double) : Screen()
    data class SipAmountV2(val userId: String, val kycAttemptId: String, val investorId: String, val goalId: String) : Screen()
    data class MandateAuth(
        val userId: String,
        val kycAttemptId: String,
        val investorId: String,
        val amount: Double,
        val mandateUrl: String,
        val mandateId: Long,
        val mandateRef: Long
    ) : Screen()
    data class Profile(val userId: String) : Screen()
    data class AccountDeletion(val userId: String) : Screen()
    data class HelpSupport(val userId: String) : Screen()
    data class NotificationWebView(val url: String, val title: String) : Screen()
    object Home : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.PhoneVerification) }

        platformLog("App: Rendering screen: ${currentScreen::class.simpleName}")
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
            is Screen.PreVerification -> {
                PreVerificationScreen(
                    onNavigateNext = { /* handled via screen result */ },
                    onNavigateBack = { currentScreen = Screen.PhoneVerification },
                    onNavigateToScreen = { nextScreen ->
                        handleNavigation(nextScreen, screen.userId, null) { currentScreen = it }
                    },
                    onNavigateToHelp = { /* Open Help */ }
                )
            }
            is Screen.AdditionalKyc -> {
                AdditionalKycScreen(
                    kycAttemptId = screen.kycAttemptId,
                    token = "", // Token retrieved from session in VM
                    onNext = { nextScreen, attemptId ->
                        handleNavigation(nextScreen, screen.userId, null, attemptId) { currentScreen = it }
                    },
                    onNavigateToHelp = { /* Open Help */ }
                )
            }
            is Screen.NomineeDetails -> {
                NomineeDetailsScreen(
                    onNext = { nextScreen ->
                        handleNavigation(nextScreen ?: "", screen.userId, null) { currentScreen = it }
                    },
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    onNavigateToHelp = { /* Open Help */ }
                )
            }
            is Screen.BankDetails -> {
                BankDetailsScreen(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    onNext = { nextScreen, investorId ->
                        handleNavigation(nextScreen ?: "", screen.userId, null) { currentScreen = it }
                    },
                    onNavigateToHelp = { /* Open Help */ }
                )
            }
            is Screen.KycInformation -> {
                KycInformationScreen(
                    onProceed = {
                        // Normally this would trigger the actual KYC flow (WebView/DigiLocker)
                        handleNavigation("MIN_DETAILS", screen.userId, null) { currentScreen = it }
                    },
                    onNavigateToHelp = { /* Open Help */ }
                )
            }
            is Screen.EsignInformation -> {
                EsignInformationScreen(
                    onProceed = {
                        // Normally this would trigger the E-sign flow
                        handleNavigation("MANDATE_AUTH", screen.userId, null) { currentScreen = it }
                    },
                    onNavigateToHelp = { /* Open Help */ }
                )
            }
            is Screen.MinDetails -> {
                val minVm: MinDetailsViewModel = koinInject()
                MinDetailsScreen(
                    onNext = { nextScreen, kycAttemptId ->
                        handleNavigation(nextScreen, screen.userId, null, kycAttemptId) { currentScreen = it }
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
                    onNavigateToOnboarding = { _, _ -> /* Fallback */ },
                    onNavigateToRoute = { nextScreen, preVerificationId ->
                        handleNavigation(nextScreen, screen.userId, preVerificationId) { currentScreen = it }
                    }
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
            is Screen.InvestmentDashboard -> {
                InvestmentDashboardV2Screen(
                    userId = screen.userId,
                    onNavigateToSchemeDetails = { purpose ->
                        currentScreen = Screen.SchemeDetails(screen.userId, purpose)
                    },
                    onNavigateToGoal = { goalId ->
                        // Navigate to SipAmountV2 - in a real app we'd fetch kycAttemptId/investorId from session
                        currentScreen = Screen.SipAmountV2(screen.userId, "", "", goalId)
                    },
                    onNavigateToWithdraw = {
                        currentScreen = Screen.Withdraw(screen.userId)
                    },
                    onNavigateToProfile = { currentScreen = Screen.Profile(screen.userId) }
                )
            }
            is Screen.SchemeDetails -> {
                SchemeDetailsScreen(
                    userId = screen.userId,
                    purpose = screen.purpose,
                    onNavigateBack = { currentScreen = Screen.InvestmentDashboard(screen.userId) },
                    onNavigateToWithdraw = { params ->
                        WithdrawParamsManager.set(params)
                        currentScreen = Screen.Withdraw(screen.userId)
                    }
                )
            }
            is Screen.Withdraw -> {
                WithdrawScreen(
                    userId = screen.userId,
                    onNavigateBack = { currentScreen = Screen.InvestmentDashboard(screen.userId) },
                    onProceed = { _, _ -> /* Proceed */ }
                )
            }
            is Screen.FundDetails -> {
                FundDetailsScreen(
                    isin = screen.isin,
                    userId = screen.userId,
                    goalId = screen.goalId,
                    sipAmount = screen.sipAmount,
                    onBackClick = { currentScreen = Screen.InitialDashboard(screen.userId) }
                )
            }
            is Screen.SipAmountV2 -> {
                SipAmountScreenV2(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    goalId = screen.goalId,
                    onSipCreated = { amount, url, id, ref ->
                        currentScreen = Screen.MandateAuth(
                            userId = screen.userId,
                            kycAttemptId = screen.kycAttemptId,
                            investorId = screen.investorId,
                            amount = amount,
                            mandateUrl = url ?: "",
                            mandateId = id ?: 0L,
                            mandateRef = ref ?: 0L
                        )
                    },
                    onNavigateBack = { currentScreen = Screen.InitialDashboard(screen.userId) }
                )
            }
            is Screen.MandateAuth -> {
                MandateAuthScreen(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    amount = screen.amount,
                    mandateUrl = screen.mandateUrl,
                    mandateId = screen.mandateId,
                    mandateRef = screen.mandateRef,
                    onGoToHome = { currentScreen = Screen.InvestmentDashboard(screen.userId) },
                    onNavigateBack = { currentScreen = Screen.SipAmountV2(screen.userId, screen.kycAttemptId, screen.investorId, "") }
                )
            }
            is Screen.Profile -> {
                ProfileScreen(
                    userId = screen.userId,
                    onLogout = { currentScreen = Screen.PhoneVerification },
                    onDeleteAccount = { currentScreen = Screen.AccountDeletion(screen.userId) },
                    onHelpSupport = { currentScreen = Screen.HelpSupport(screen.userId) },
                    onBack = { currentScreen = Screen.InvestmentDashboard(screen.userId) }
                )
            }
            is Screen.AccountDeletion -> {
                AccountDeletionScreen(
                    userId = screen.userId,
                    onBack = { currentScreen = Screen.Profile(screen.userId) }
                )
            }
            is Screen.HelpSupport -> {
                HelpSupportScreen(
                    userId = screen.userId,
                    onBack = { currentScreen = Screen.Profile(screen.userId) }
                )
            }
            is Screen.NotificationWebView -> {
                NotificationWebViewScreen(
                    url = screen.url,
                    title = screen.title,
                    onBack = { currentScreen = Screen.InvestmentDashboard("") } // userId might be needed
                )
            }
            is Screen.Home -> {
                HomeScreen(
                    onNavigateToMutualFund = { currentScreen = Screen.InitialDashboard("") }
                )
            }
        }
    }
}

private fun handleNavigation(
    action: String?,
    userId: String,
    preVerificationId: String?,
    kycAttemptId: String? = null,
    onNavigate: (Screen) -> Unit
) {
    platformLog("AppNav: handleNavigation: action='$action', userId='$userId'")
    com.pyllar.consumer.util.Log.d("AppNav", "handleNavigation: action=$action, userId=$userId")
    when (action) {
        ScreenNames.PRE_VERIFICATION -> {
            platformLog("AppNav: Matched PRE_VERIFICATION")
            onNavigate(Screen.PreVerification(userId))
        }
        ScreenNames.ADDITIONAL_KYC -> {
            platformLog("AppNav: Matched ADDITIONAL_KYC with kycAttemptId: $kycAttemptId")
            onNavigate(Screen.AdditionalKyc(userId, kycAttemptId ?: ""))
        }
        ScreenNames.NOMINEE_DETAILS -> {
            platformLog("AppNav: Matched NOMINEE_DETAILS")
            onNavigate(Screen.NomineeDetails(userId, "", ""))
        }
        ScreenNames.BANK_DETAILS -> {
            platformLog("AppNav: Matched BANK_DETAILS")
            onNavigate(Screen.BankDetails(userId, ""))
        }
        ScreenNames.KYC_INFORMATION -> {
            platformLog("AppNav: Matched KYC_INFORMATION")
            onNavigate(Screen.KycInformation(userId))
        }
        ScreenNames.ESIGN_INFORMATION -> {
            platformLog("AppNav: Matched ESIGN_INFORMATION")
            onNavigate(Screen.EsignInformation(userId))
        }
        ScreenNames.PAN_KYC -> {
            platformLog("AppNav: Matched PAN_KYC")
            onNavigate(Screen.PanKyc(userId, preVerificationId))
        }
        ScreenNames.MIN_DETAILS -> {
            platformLog("AppNav: Matched MIN_DETAILS")
            onNavigate(Screen.MinDetails(userId, "", "", "", ""))
        }
        ScreenNames.NAME_DOB -> {
            platformLog("AppNav: Matched NAME_DOB")
            onNavigate(Screen.NameDob(userId, "", "", "", ""))
        }
        ScreenNames.CHECK_PAN_POPULATED_DETAILS -> {
            platformLog("AppNav: Matched CHECK_PAN_POPULATED_DETAILS")
            onNavigate(Screen.CheckPanPopulatedDetails(userId, preVerificationId))
        }
        ScreenNames.INITIAL_DASHBOARD -> {
            platformLog("AppNav: Matched INITIAL_DASHBOARD")
            onNavigate(Screen.InitialDashboard(userId))
        }
        ScreenNames.SIP_AMOUNT_V2 -> {
            platformLog("AppNav: Matched SIP_AMOUNT_V2")
            onNavigate(Screen.SipAmountV2(userId, "", "", ""))
        }
        ScreenNames.MANDATE_AUTH -> {
            platformLog("AppNav: Matched MANDATE_AUTH")
            onNavigate(Screen.MandateAuth(userId, "", "", 0.0, "", 0L, 0L))
        }
        ScreenNames.DASHBOARD, ScreenNames.INVESTMENT_DASHBOARD -> {
            platformLog("AppNav: Matched DASHBOARD/INVESTMENT_DASHBOARD")
            onNavigate(Screen.InvestmentDashboard(userId))
        }
        else -> {
            platformLog("AppNav: Defaulting to dashboard for action: '$action'")
            com.pyllar.consumer.util.Log.d("AppNav", "Defaulting navigation to dashboard for action: $action")
            onNavigate(Screen.InvestmentDashboard(userId))
        }
    }
}