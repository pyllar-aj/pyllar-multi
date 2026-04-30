package com.pyllar.consumer

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import com.pyllar.consumer.presentation.ui.theme.PyllarTheme
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
    data class KycInformation(val userId: String, val reUrl: String? = null) : Screen()
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
    data class FundDetails(val isin: String, val userId: String, val goalId: String, val sipAmount: Double, val fromSipAmount: Boolean = false) : Screen()
    data class SipAmountV2(val userId: String, val kycAttemptId: String, val investorId: String, val goalId: String, val fromDashboard: Boolean = false) : Screen()
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
    data class HelpSupport(
        val userId: String,
        val showKycHelp: Boolean = false,
        val showBankHelp: Boolean = false,
        val showOnlyKycInfo: Boolean = false
    ) : Screen()
    data class NotificationWebView(val url: String, val title: String) : Screen()
    object Home : Screen()
}

@Composable
fun App() {
    val sessionStore: com.pyllar.consumer.domain.storage.SessionStore = koinInject()
    val scope = rememberCoroutineScope()
    PyllarTheme {
        val screenStack = remember { mutableStateListOf<Screen>(Screen.PhoneVerification) }
        val currentScreen = screenStack.last()

        fun navigateTo(screen: Screen, clearStack: Boolean = false) {
            platformLog("AppNav: navigateTo ${screen::class.simpleName} (clearStack=$clearStack)")
            if (clearStack) screenStack.clear()
            screenStack.add(screen)
        }

        fun navigateBack() {
            platformLog("AppNav: navigateBack. Stack size before: ${screenStack.size}")
            if (screenStack.size > 1) {
                screenStack.removeAt(screenStack.size - 1)
            } else {
                platformLog("AppNav: Cannot navigate back, stack size is 1")
            }
        }

        LaunchedEffect(currentScreen) {
            platformLog("App: currentScreen changed to ${currentScreen::class.simpleName} - $currentScreen")
        }

        platformLog("App: Rendering screen: ${currentScreen::class.simpleName} (Stack size: ${screenStack.size})")
        
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
                        navigateTo(Screen.OtpVerification(number, ref))
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
                        navigateTo(Screen.MinimalPermission(
                            userId = userId,
                            isNewUser = isNewUser,
                            nextScreen = nextScreen
                        ))
                    },
                    onNavigateBack = { navigateBack() }
                )
            }
            is Screen.MinimalPermission -> {
                val permVm: PermissionViewModel = koinInject()
                MinimalPermissionScreen(
                    userId = screen.userId,
                    isNewUser = screen.isNewUser,
                    viewModel = permVm,
                    onNavigateNext = { nextScreen ->
                        handleNavigation(nextScreen, screen.userId, null) { navigateTo(it) }
                    }
                )
            }
            is Screen.PanKyc -> {
                val panVm: PanKycViewModel = koinInject()
                PanKycScreen(
                    onPanVerified = { pan, nextScreen, _, _, _ ->
                        navigateTo(Screen.MinDetails(
                            userId = screen.userId,
                            pan = pan,
                            email = "", 
                            phone = "",
                            token = ""
                        ))
                    },
                    viewModel = panVm
                )
            }
            is Screen.PreVerification -> {
                PreVerificationScreen(
                    onNavigateNext = { /* handled via screen result */ },
                    onNavigateBack = { navigateBack() },
                    onNavigateToScreen = { nextScreen ->
                        handleNavigation(nextScreen, screen.userId, null) { navigateTo(it) }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showKycHelp = true)) },
                    onNavigateToKycInfo = { navigateTo(Screen.HelpSupport(screen.userId, showKycHelp = true)) }
                )
            }
            is Screen.AdditionalKyc -> {
                AdditionalKycScreen(
                    kycAttemptId = screen.kycAttemptId,
                    token = "", // Token retrieved from session in VM
                    onNext = { nextScreen, attemptId ->
                        handleNavigation(nextScreen, screen.userId, null, attemptId) { navigateTo(it) }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showKycHelp = true)) }
                )
            }
            is Screen.NomineeDetails -> {
                NomineeDetailsScreen(
                    onNext = { nextScreen ->
                        handleNavigation(nextScreen ?: "", screen.userId, null) { navigateTo(it) }
                    },
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showKycHelp = true)) }
                )
            }
            is Screen.BankDetails -> {
                BankDetailsScreen(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    onNext = { nextScreen, investorId ->
                        handleNavigation(
                            action = nextScreen ?: "",
                            userId = screen.userId,
                            kycAttemptId = screen.kycAttemptId,
                            investorId = investorId
                        ) { navigateTo(it) }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showBankHelp = true)) }
                )
            }
            is Screen.KycInformation -> {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                KycInformationScreen(
                    onProceed = {
                        if (!screen.reUrl.isNullOrBlank()) {
                            uriHandler.openUri(screen.reUrl)
                        } else {
                            handleNavigation(ScreenNames.MIN_DETAILS, screen.userId) { navigateTo(it) }
                        }
                    },
                    onOpenWebSignIn = {
                        if (!screen.reUrl.isNullOrBlank()) uriHandler.openUri(screen.reUrl)
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showKycHelp = true)) }
                )
            }
            is Screen.EsignInformation -> {
                EsignInformationScreen(
                    onProceed = {
                        handleNavigation("MANDATE_AUTH", screen.userId, null, null, null) { navigateTo(it) }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showKycHelp = true)) }
                )
            }
            is Screen.MinDetails -> {
                val minVm: MinDetailsViewModel = koinInject()
                MinDetailsScreen(
                    onNext = { nextScreen, kycAttemptId ->
                        handleNavigation(nextScreen, screen.userId, kycAttemptId, null, null) { navigateTo(it) }
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
                    onKycSubmitted = { _, _, navInfo, data ->
                        val reUrl = navInfo?.getParam("reUrl") ?: (data as? com.pyllar.consumer.data.remote.model.MinimalKycResponse)?.reUrl
                        
                        scope.launch {
                            val kycAttemptId = navInfo?.getParam("kycAttemptId") 
                                ?: (data as? com.pyllar.consumer.data.remote.model.MinimalKycResponse)?.kycAttemptId
                            
                            if (!kycAttemptId.isNullOrBlank()) {
                                sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.KYC_ATTEMPT_ID, kycAttemptId)
                            }
                            if (!reUrl.isNullOrBlank()) {
                                sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.RE_URL, reUrl)
                            }

                            val nextAction = if (!reUrl.isNullOrBlank()) {
                                ScreenNames.KYC_INFORMATION
                            } else {
                                navInfo?.nextScreen ?: ScreenNames.KYC_INFORMATION
                            }
                            
                            handleNavigation(
                                action = nextAction,
                                userId = screen.userId,
                                reUrl = reUrl
                            ) { navigateTo(it) }
                        }
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
                        handleNavigation(ScreenNames.INITIAL_DASHBOARD, screen.userId, null, null, screen.preVerificationId) { navigateTo(it) }
                    }
                )
            }
            is Screen.InitialDashboard -> {
                InitialDashboardScreen(
                    userId = screen.userId,
                    onNavigateToOnboarding = { _, _ -> /* Fallback */ },
                    onNavigateToRoute = { nextScreen, preVerificationId ->
                        handleNavigation(nextScreen, screen.userId, null, null, preVerificationId) { navigateTo(it) }
                    }
                )
            }
            is Screen.UpiAccountLinking -> {
                val upiVm: UpiAccountLinkingViewModel = koinInject()
                UpiAccountLinkingScreen(
                    viewModel = upiVm,
                    onAccountLinked = { /* Linked */ },
                    onNavigateBack = { navigateBack() }
                )
            }
            is Screen.InvestmentDashboard -> {
                InvestmentDashboardV2Screen(
                    userId = screen.userId,
                    onNavigateToSchemeDetails = { purpose ->
                        navigateTo(Screen.SchemeDetails(screen.userId, purpose))
                    },
                    onNavigateToGoal = { goalId ->
                        navigateTo(Screen.SipAmountV2(screen.userId, "", "", goalId, fromDashboard = true))
                    },
                    onNavigateToWithdraw = {
                        navigateTo(Screen.Withdraw(screen.userId))
                    },
                    onNavigateToProfile = { navigateTo(Screen.Profile(screen.userId)) },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.SchemeDetails -> {
                SchemeDetailsScreen(
                    userId = screen.userId,
                    purpose = screen.purpose,
                    onNavigateBack = { navigateBack() },
                    onNavigateToWithdraw = { params ->
                        WithdrawParamsManager.set(params)
                        navigateTo(Screen.Withdraw(screen.userId))
                    }
                )
            }
            is Screen.Withdraw -> {
                WithdrawScreen(
                    userId = screen.userId,
                    onNavigateBack = { navigateBack() },
                    onProceed = { _, _ -> /* Proceed */ }
                )
            }
            is Screen.FundDetails -> {
                FundDetailsScreen(
                    isin = screen.isin,
                    userId = screen.userId,
                    goalId = screen.goalId,
                    sipAmount = screen.sipAmount,
                    onBackClick = { navigateBack() }
                )
            }
            is Screen.SipAmountV2 -> {
                SipAmountScreenV2(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    goalId = screen.goalId,
                    onSipCreated = { amount, url, id, ref ->
                        navigateTo(Screen.MandateAuth(
                            userId = screen.userId,
                            kycAttemptId = screen.kycAttemptId,
                            investorId = screen.investorId,
                            amount = amount,
                            mandateUrl = url ?: "",
                            mandateId = id ?: 0L,
                            mandateRef = ref ?: 0L
                        ))
                    },
                    onNavigateBack = { navigateBack() },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onNavigateToFundDetails = { userId, goalId, amt, kycId, invId ->
                        navigateTo(Screen.FundDetails("", userId, goalId, amt, fromSipAmount = true))
                    }
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
                    onGoToHome = { navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true) },
                    onNavigateBack = { navigateBack() }
                )
            }
            is Screen.Profile -> {
                ProfileScreen(
                    userId = screen.userId,
                    onLogout = { navigateTo(Screen.PhoneVerification, clearStack = true) },
                    onDeleteAccount = { navigateTo(Screen.AccountDeletion(screen.userId)) },
                    onHelpSupport = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.AccountDeletion -> {
                AccountDeletionScreen(
                    userId = screen.userId,
                    onBack = { navigateBack() }
                )
            }
            is Screen.HelpSupport -> {
                HelpSupportScreen(
                    userId = screen.userId,
                    showKycHelp = screen.showKycHelp,
                    showBankHelp = screen.showBankHelp,
                    showOnlyKycInfo = screen.showOnlyKycInfo,
                    onBack = { navigateBack() }
                )
            }
            is Screen.NotificationWebView -> {
                NotificationWebViewScreen(
                    url = screen.url,
                    title = screen.title,
                    onBack = { navigateBack() }
                )
            }
            is Screen.Home -> {
                HomeScreen(
                    onNavigateToMutualFund = { navigateTo(Screen.InitialDashboard(""), clearStack = true) }
                )
            }
        }
    }
}

private fun handleNavigation(
    action: String?,
    userId: String,
    kycAttemptId: String? = null,
    investorId: String? = null,
    preVerificationId: String? = null,
    reUrl: String? = null,
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
            onNavigate(Screen.NomineeDetails(userId, kycAttemptId ?: "", investorId ?: ""))
        }
        ScreenNames.BANK_DETAILS -> {
            platformLog("AppNav: Matched BANK_DETAILS")
            onNavigate(Screen.BankDetails(userId, kycAttemptId ?: ""))
        }
        ScreenNames.KYC_INFORMATION -> {
            platformLog("AppNav: Matched KYC_INFORMATION with reUrl: ${reUrl != null}")
            onNavigate(Screen.KycInformation(userId, reUrl))
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
            platformLog("AppNav: Matched SIP_AMOUNT_V2 - setting fromDashboard=true")
            onNavigate(Screen.SipAmountV2(userId, "", "", "", true))
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
            platformLog("AppNav: Defaulting to PRE_VERIFICATION for action: '$action'")
            com.pyllar.consumer.util.Log.d("AppNav", "Defaulting navigation to PRE_VERIFICATION for action: $action")
            onNavigate(Screen.PreVerification(userId))
        }
    }
}