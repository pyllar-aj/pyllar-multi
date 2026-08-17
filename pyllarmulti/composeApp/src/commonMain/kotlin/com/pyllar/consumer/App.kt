package com.pyllar.consumer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.presentation.auth.permission.PermissionViewModel
import com.pyllar.consumer.presentation.auth.permission.MinimalPermissionScreen
import com.pyllar.consumer.presentation.auth.phone.PhoneVerificationScreenV3
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
import com.pyllar.consumer.presentation.referral.*
import com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingScreen
import com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingViewModel
import com.pyllar.consumer.presentation.ui.theme.PyllarTheme
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import org.koin.compose.koinInject
import com.pyllar.consumer.platform.PlatformActions
import androidx.compose.ui.platform.LocalUriHandler
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.navigation.ForceUpdateManager
import com.pyllar.consumer.presentation.components.ForceUpdateDialog
import com.pyllar.consumer.presentation.components.UpdateBottomSheet
import com.pyllar.consumer.update.checkPlatformForUpdates
import com.pyllar.consumer.update.onOptionalUpdateDismissed

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
    data class UpiFetch(val userId: String) : Screen()
    data class PanFetch(val userId: String) : Screen()
    data class AdditionalKyc(val userId: String, val kycAttemptId: String) : Screen()
    data class NomineeDetails(val userId: String, val kycAttemptId: String, val investorId: String) : Screen()
    data class Signature(val userId: String, val kycAttemptId: String, val investorId: String) : Screen()
    data class BankDetails(val userId: String, val kycAttemptId: String) : Screen()
    data class KycInformation(val userId: String, val reUrl: String? = null, val kycAttemptId: String? = null, val errorMessage: String? = null) : Screen()
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
    data class WithdrawAmount(val userId: String, val schemeId: String) : Screen()
    data class WithdrawSuccess(
        val userId: String,
        val amount: Double,
        val schemeName: String,
        val bankName: String,
        val bankAccountLast4: String,
        val transactionId: String,
        val redemptionId: String,
        val redemptionGroupId: String? = null,
        val folio: String?,
        val redemptionMode: String = "NORMAL"
    ) : Screen()
    data class FundDetails(
        val isin: String,
        val userId: String,
        val goalId: String,
        val sipAmount: Double,
        val kycAttemptId: String = "",
        val investorId: String = "",
        val fromSipAmount: Boolean = false,
        val frequency: String = "daily",
        val installmentDay: Int? = null
    ) : Screen()
    data class FundDetailsViewOnly(val isin: String, val userId: String, val goalId: String) : Screen()
    data class LumpsumFundDetails(val isin: String, val userId: String, val goalId: String, val lumpsumAmount: Double, val kycAttemptId: String = "", val investorId: String = "") : Screen()
    data class SipAmountV2(val userId: String, val kycAttemptId: String, val investorId: String, val goalId: String, val fromDashboard: Boolean = false, val isExistingInvestment: Boolean = false) : Screen()
    data class LumpsumAmountV2(val userId: String, val kycAttemptId: String, val investorId: String, val goalId: String, val isExistingInvestment: Boolean = false) : Screen()
    data class LumpsumPurchaseAuth(
        val userId: String,
        val kycAttemptId: String,
        val investorId: String,
        val amount: Double,
        val paymentUrl: String,
        val paymentId: Long,
        val paymentRef: Long,
        val goalId: String = ""
    ) : Screen()
    data class MandateAuth(
        val userId: String,
        val kycAttemptId: String,
        val investorId: String,
        val amount: Double,
        val mandateUrl: String,
        val mandateId: Long,
        val mandateRef: Long,
        val goalId: String = ""
    ) : Screen()
    data class Profile(val userId: String) : Screen()
    data class Referral(val userId: String) : Screen()
    data class AccountDeletion(val userId: String) : Screen()
    data class HelpSupport(
        val userId: String,
        val showKycHelp: Boolean = false,
        val showBankHelp: Boolean = false,
        val showOnlyKycInfo: Boolean = false
    ) : Screen()
    data class NotificationWebView(val url: String, val title: String) : Screen()
    data class KycWebView(val userId: String, val url: String, val kycAttemptId: String? = null) : Screen()
    data class EsignWebView(val userId: String, val url: String, val kycAttemptId: String? = null) : Screen()
    data class PennyDropLoading(val userId: String) : Screen()
    object Home : Screen()
}

@Composable
fun App() {
    val sessionStore: com.pyllar.consumer.domain.storage.SessionStore = koinInject()
    val authRepository: com.pyllar.consumer.domain.repository.AuthRepository = koinInject()
    val onboardingRepository: com.pyllar.consumer.domain.repository.OnboardingRepository = koinInject()
    val forceUpdateManager: ForceUpdateManager = koinInject()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    PyllarTheme {
        val screenStack = remember { mutableStateListOf<Screen>() }
        val currentScreen = if (screenStack.isNotEmpty()) screenStack.last() else null
        var isInitializing by remember { mutableStateOf(true) }

        fun navigateTo(screen: Screen, clearStack: Boolean = false) {
            platformLog("AppNav: navigateTo ${screen::class.simpleName} (clearStack=$clearStack)")
            if (clearStack) screenStack.clear()
            screenStack.add(screen)
            
            // Persist last screen for resume
            scope.launch {
                val action = when (screen) {
                    is Screen.PreVerification -> ScreenNames.PRE_VERIFICATION
                    is Screen.PanKyc -> ScreenNames.PAN_KYC
                    is Screen.AdditionalKyc -> ScreenNames.ADDITIONAL_KYC
                    is Screen.NomineeDetails -> ScreenNames.NOMINEE_DETAILS
                    is Screen.Signature -> ScreenNames.SIGNATURE
                    is Screen.BankDetails -> ScreenNames.BANK_DETAILS
                    is Screen.KycInformation -> ScreenNames.KYC_INFORMATION
                    is Screen.EsignInformation -> ScreenNames.ESIGN_INFORMATION
                    is Screen.InvestmentDashboard -> ScreenNames.INVESTMENT_DASHBOARD
                    is Screen.InitialDashboard -> ScreenNames.INITIAL_DASHBOARD
                    is Screen.NameDob -> ScreenNames.NAME_DOB
                    is Screen.MinDetails -> ScreenNames.MIN_DETAILS
                    is Screen.CheckPanPopulatedDetails -> ScreenNames.CHECK_PAN_POPULATED_DETAILS
                    is Screen.UpiAccountLinking -> ScreenNames.MANDATE_AUTH // Fallback
                    is Screen.PennyDropLoading -> ScreenNames.PENNY_DROP_LOADING
                    else -> null
                }
                if (action != null) {
                    sessionStore.saveValue(KeyValueConstants.LAST_SCREEN, action)
                }

                // Proactively persist IDs for resume
                when (screen) {
                    is Screen.AdditionalKyc -> sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                    is Screen.NomineeDetails -> {
                        sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                        sessionStore.saveValue(KeyValueConstants.INVESTOR_ID, screen.investorId)
                    }
                    is Screen.Signature -> {
                        sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                        sessionStore.saveValue(KeyValueConstants.INVESTOR_ID, screen.investorId)
                    }
                    is Screen.BankDetails -> sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                    is Screen.KycInformation -> screen.kycAttemptId?.let { sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, it) }
                    is Screen.SipAmountV2 -> {
                        if (screen.kycAttemptId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                        if (screen.investorId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.INVESTOR_ID, screen.investorId)
                    }
                    is Screen.MandateAuth -> {
                        if (screen.kycAttemptId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                        if (screen.investorId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.INVESTOR_ID, screen.investorId)
                    }
                    is Screen.LumpsumAmountV2 -> {
                        if (screen.kycAttemptId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                        if (screen.investorId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.INVESTOR_ID, screen.investorId)
                    }
                    is Screen.LumpsumPurchaseAuth -> {
                        if (screen.kycAttemptId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, screen.kycAttemptId)
                        if (screen.investorId.isNotBlank()) sessionStore.saveValue(KeyValueConstants.INVESTOR_ID, screen.investorId)
                    }
                    else -> {}
                }
            }
        }

        fun navigateBack() {
            platformLog("AppNav: navigateBack. Stack size before: ${screenStack.size}")
            if (screenStack.size > 1) {
                screenStack.removeAt(screenStack.size - 1)
            } else {
                platformLog("AppNav: Cannot navigate back, stack size is 1")
            }
        }

        // Check for available updates on the current platform (no-op on Android — handled by
        // InAppUpdateManager in MainActivity; calls iTunes API on iOS).
        LaunchedEffect(Unit) {
            checkPlatformForUpdates(forceUpdateManager)
        }

        LaunchedEffect(Unit) {
            val isIos = getPlatform().name.contains("iOS", ignoreCase = true)
            if (isIos && sessionStore.getValue("force_logout_v1_done") != "true") {
                platformLog("App: Forcing user logout once as per requested configuration")
                sessionStore.logout()
                sessionStore.saveValue("force_logout_v1_done", "true")
            }
            val isLoggedIn = sessionStore.isLoggedIn()
            platformLog("App: Initializing. isLoggedIn=$isLoggedIn")
            if (isLoggedIn) {
                val userId = sessionStore.getCurrentUserId()
                PlatformAnalyticsLogger.setUserId(userId)
                val lastScreen = sessionStore.getValue(KeyValueConstants.LAST_SCREEN)
                platformLog("App: Resuming. userId=$userId, lastScreen=$lastScreen")
                if (lastScreen != null && lastScreen != ScreenNames.HOME) {
                    handleNavigation(lastScreen, userId, sessionStore = sessionStore) { 
                        navigateTo(it, clearStack = true)
                        isInitializing = false
                    }
                } else {
                    // Default to PhoneVerification if no last screen saved
                    navigateTo(Screen.PhoneVerification, clearStack = true)
                    isInitializing = false
                }
            } else {
                navigateTo(Screen.PhoneVerification, clearStack = true)
                isInitializing = false
            }
        }

        LaunchedEffect(isInitializing) {
            if (!isInitializing) {
                val isLoggedIn = sessionStore.isLoggedIn()
                if (isLoggedIn) {
                    val userId = sessionStore.getCurrentUserId()
                    com.pyllar.consumer.push.PushTokenManager.lastNotificationPayload.collect { payload ->
                        if (!payload.isNullOrBlank()) {
                            platformLog("App: Received notification payload: $payload")
                            
                            var action = payload
                            var notificationUrl: String? = null
                            var screenRoute: String? = null

                            if (payload.trim().startsWith("{") && payload.trim().endsWith("}")) {
                                try {
                                    val element = kotlinx.serialization.json.Json.parseToJsonElement(payload)
                                    if (element is kotlinx.serialization.json.JsonObject) {
                                        val act = element["action"] ?: element["screen"]
                                        if (act is kotlinx.serialization.json.JsonPrimitive) {
                                            action = act.content
                                        }
                                        val urlVal = element["url"]
                                        if (urlVal is kotlinx.serialization.json.JsonPrimitive) {
                                            notificationUrl = urlVal.content
                                        }
                                        val routeVal = element["route"]
                                        if (routeVal is kotlinx.serialization.json.JsonPrimitive) {
                                            screenRoute = routeVal.content
                                        }
                                    }
                                } catch (e: Exception) {
                                    platformLog("AppNav: Failed to parse payload JSON: ${e.message}")
                                }
                            }

                            // If action is "screen" and screenRoute is provided, use screenRoute as action
                            val targetAction = if (action.equals("screen", ignoreCase = true) && !screenRoute.isNullOrBlank()) {
                                screenRoute
                            } else {
                                action
                            }

                            handleNavigation(
                                action = targetAction,
                                userId = userId,
                                notificationUrl = notificationUrl,
                                sessionStore = sessionStore
                            ) { screen ->
                                navigateTo(screen)
                            }
                            com.pyllar.consumer.push.PushTokenManager.clearNotificationPayload()
                        }
                    }
                }
            }
        }

        // ── Update overlays ────────────────────────────────────────────────────────
        // ForceUpdateDialog uses Dialog() internally so it floats above all content
        // on both Android and iOS regardless of where it sits in the tree.
        forceUpdateManager.forceUpdateInfo?.let { info ->
            ForceUpdateDialog(
                forceUpdateInfo = info,
                onUpdateClick = {
                    try { uriHandler.openUri(info.updateUrl) } catch (_: Exception) {
                        try { uriHandler.openUri(info.webUrl) } catch (_: Exception) {}
                    }
                }
            )
        }

        // Optional update (iOS only — Android drives this via Play Core in MainActivity).
        // optionalUpdateUrl is only ever set by IosAppStoreUpdateChecker; stays null on Android.
        forceUpdateManager.optionalUpdateUrl?.let { storeUrl ->
            UpdateBottomSheet(
                onUpdateClick = {
                    try { uriHandler.openUri(storeUrl) } catch (_: Exception) {}
                    forceUpdateManager.clearOptionalUpdate()
                },
                onDismiss = {
                    onOptionalUpdateDismissed() // records timestamp for 72-hour throttle (iOS only)
                    forceUpdateManager.clearOptionalUpdate()
                }
            )
        }
        // ── End update overlays ────────────────────────────────────────────────────

        if (isInitializing) {
            com.pyllar.consumer.presentation.components.LoadingScreen(text = "Initializing...")
            return@PyllarTheme
        }

        LaunchedEffect(currentScreen) {
            platformLog("App: currentScreen changed to ${currentScreen?.let { it::class.simpleName } ?: "null"} - $currentScreen")
        }

        platformLog("App: Rendering screen: ${currentScreen?.let { it::class.simpleName } ?: "null"} (Stack size: ${screenStack.size})")
        
        Box(modifier = Modifier.fillMaxSize()) {
            val screensToRender = remember(screenStack.size) {
                val list = mutableListOf<Screen>()
                if (screenStack.isNotEmpty()) {
                    val last = screenStack.last()
                    val isLastOverlay = last is Screen.HelpSupport || last is Screen.UpiFetch || last is Screen.PanFetch
                    if (isLastOverlay && screenStack.size > 1) {
                        list.add(screenStack[screenStack.size - 2])
                    }
                    list.add(last)
                }
                list
            }

            screensToRender.forEach { screen ->
                key(screen) {
                    when (screen) {
            is Screen.PhoneVerification -> {
                val phoneVm: PhoneVerificationViewModel = koinInject()
                PhoneVerificationScreenV3(
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
                        PlatformAnalyticsLogger.setUserId(userId)
                        if (nextScreen == "minimal_permission" || nextScreen == "permission" || nextScreen.isNullOrBlank()) {
                            navigateTo(Screen.MinimalPermission(
                                userId = userId,
                                isNewUser = isNewUser,
                                nextScreen = nextScreen
                            ))
                        } else {
                            scope.launch {
                                handleNavigation(nextScreen, userId, sessionStore = sessionStore) { navigateTo(it) }
                            }
                        }
                    },
                    onNavigateBack = { navigateBack() },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport("")) }
                )
            }
            is Screen.MinimalPermission -> {
                val permVm: PermissionViewModel = koinInject()
                PermissionV2Screen(
                    userId = screen.userId,
                    isNewUser = screen.isNewUser,
                    viewModel = permVm,
                    onNavigateNext = { nextScreen ->
                        scope.launch {
                            handleNavigation(nextScreen, screen.userId, sessionStore = sessionStore) { navigateTo(it) }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.PanKyc -> {
                val panVm: PanKycViewModel = koinInject()
                PanKycScreen(
                    onPanVerified = { panValue, nextScreen, panHolderName, navigationInfo, serverResponseData ->
                        scope.launch {
                            if (!panHolderName.isNullOrBlank()) {
                                sessionStore.saveValue(KeyValueConstants.PAN_HOLDER_NAME, panHolderName)
                            }
                            sessionStore.saveValue(KeyValueConstants.PAN, panValue)
                            
                            var reUrl = ""
                            var kycAttemptId = ""
                            if (serverResponseData is com.pyllar.consumer.data.remote.model.MinimalKycResponse) {
                                reUrl = serverResponseData.reUrl ?: ""
                                kycAttemptId = serverResponseData.kycAttemptId ?: ""
                            } else if (serverResponseData is Map<*, *>) {
                                reUrl = serverResponseData["reUrl"] as? String ?: ""
                                kycAttemptId = serverResponseData["kycAttemptId"] as? String ?: ""
                            }
                            
                            if (kycAttemptId.isNotBlank()) {
                                sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, kycAttemptId)
                            }
                            if (reUrl.isNotBlank()) {
                                sessionStore.saveValue(KeyValueConstants.RE_URL, reUrl)
                            }
                            
                            if (reUrl.isNotBlank()) {
                                navigateTo(Screen.KycInformation(screen.userId, reUrl, kycAttemptId))
                            } else {
                                val nextAction = navigationInfo?.nextScreen ?: nextScreen
                                handleNavigation(
                                    action = nextAction,
                                    userId = screen.userId,
                                    kycAttemptId = kycAttemptId,
                                    sessionStore = sessionStore,
                                    reUrl = reUrl
                                ) { navigateTo(it) }
                            }
                        }
                    },
                    viewModel = panVm
                )
            }
            is Screen.PreVerification -> {
                val userInfoVm: UserInfoViewModel = koinInject()
                var effectiveUserId by remember { mutableStateOf("") }
                var effectivePhone by remember { mutableStateOf("") }
                var effectiveEmail by remember { mutableStateOf("") }
                var effectiveToken by remember { mutableStateOf("") }
                var isInitialized by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    effectiveUserId = sessionStore.getCurrentUserId()
                    effectivePhone = sessionStore.getCurrentPhone()
                    effectiveEmail = sessionStore.getCurrentEmail()
                    effectiveToken = sessionStore.getCurrentToken()
                    isInitialized = true
                }

                if (!isInitialized) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    UserInfoScreenV2(
                        viewModel = userInfoVm,
                        userId = effectiveUserId,
                        email = effectiveEmail,
                        phone = effectivePhone,
                        token = effectiveToken,
                        onNavigateToHelp = {
                            navigateTo(Screen.HelpSupport(effectiveUserId, showKycHelp = true))
                        },
                        onNavigateToUpiFetch = {
                            navigateTo(Screen.UpiFetch(effectiveUserId))
                        },
                        onNavigateToPanFetch = {
                            navigateTo(Screen.PanFetch(effectiveUserId))
                        },
                        onKycSubmitted = { name, dob, email, navigationInfo, serverData ->
                            scope.launch {
                                sessionStore.saveValue(KeyValueConstants.FULL_NAME, name)
                                sessionStore.saveValue(KeyValueConstants.DOB, dob)
                                if (email.isNotBlank()) {
                                    sessionStore.saveValue(KeyValueConstants.EMAIL, email)
                                }

                                var reUrl = ""
                                var kycAttemptId = ""

                                if (serverData is com.pyllar.consumer.data.remote.model.MinimalKycResponse) {
                                    reUrl = serverData.reUrl ?: ""
                                    kycAttemptId = serverData.kycAttemptId ?: ""
                                } else if (serverData is Map<*, *>) {
                                    reUrl = serverData["reUrl"] as? String ?: ""
                                    kycAttemptId = serverData["kycAttemptId"] as? String ?: ""
                                }

                                if (kycAttemptId.isNotBlank()) {
                                    sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, kycAttemptId)
                                }
                                if (reUrl.isNotBlank()) {
                                    sessionStore.saveValue(KeyValueConstants.RE_URL, reUrl)
                                }

                                if (reUrl.isNotBlank()) {
                                    navigateTo(Screen.KycInformation(effectiveUserId, reUrl, kycAttemptId))
                                } else if (navigationInfo != null) {
                                    handleNavigation(
                                        action = navigationInfo.nextScreen,
                                        userId = effectiveUserId,
                                        kycAttemptId = kycAttemptId,
                                        sessionStore = sessionStore,
                                        reUrl = reUrl
                                    ) { navigateTo(it) }
                                } else {
                                    navigateTo(Screen.KycInformation(effectiveUserId, null, kycAttemptId))
                                }
                            }
                        }
                    )
                }
            }
            is Screen.UpiFetch -> {
                UpiFetchSheetScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is Screen.PanFetch -> {
                CreditBureauFetchSheetScreen(
                    onNavigateBack = { navigateBack() }
                )
            }
            is Screen.AdditionalKyc -> {
                AdditionalKycScreenV2(
                    kycAttemptId = screen.kycAttemptId,
                    token = "", // Token retrieved from session in VM
                    onNext = { nextScreen, attemptId ->
                        scope.launch {
                            handleNavigation(nextScreen, screen.userId, attemptId, sessionStore = sessionStore) { navigateTo(it) }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.NomineeDetails -> {
                NomineeDetailsScreenV2(
                    onNext = { nextScreen ->
                        scope.launch {
                            handleNavigation(nextScreen ?: "", screen.userId, screen.kycAttemptId, screen.investorId, sessionStore = sessionStore) { navigateTo(it) }
                        }
                    },
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.Signature -> {
                SignatureScreenV2(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    onSignatureCompleted = { nextScreen, redirectUrl ->
                        scope.launch {
                            if (!redirectUrl.isNullOrBlank()) {
                                sessionStore.saveValue(KeyValueConstants.RE_URL, redirectUrl)
                                sessionStore.saveValue(KeyValueConstants.ESIGN_URL, redirectUrl)
                                navigateTo(Screen.EsignInformation(screen.userId))
                            } else {
                                handleNavigation(nextScreen, screen.userId, screen.kycAttemptId, screen.investorId, sessionStore = sessionStore) { navigateTo(it) }
                            }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.BankDetails -> {
                BankDetailsScreenV2(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    onNext = { nextScreen, investorId ->
                        scope.launch {
                            handleNavigation(
                                action = nextScreen ?: "",
                                userId = screen.userId,
                                kycAttemptId = screen.kycAttemptId,
                                investorId = investorId,
                                sessionStore = sessionStore
                            ) { navigateTo(it) }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId, showBankHelp = true)) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.KycInformation -> {
                var isDigiLinkLoading by remember { mutableStateOf(false) }
                var currentErrorMessage by remember { mutableStateOf(screen.errorMessage) }

                LaunchedEffect(screen.errorMessage) {
                     currentErrorMessage = screen.errorMessage
                }

                KycInformationScreenV2(
                    errorMessage = currentErrorMessage,
                    isLoading = isDigiLinkLoading,
                    onBack = { navigateBack() },
                    onProceed = {
                        if (!screen.reUrl.isNullOrBlank() && currentErrorMessage == null) {
                            navigateTo(Screen.KycWebView(screen.userId, screen.reUrl, screen.kycAttemptId))
                        } else {
                            // Retry or fresh DigiLink fetch
                            scope.launch {
                                isDigiLinkLoading = true
                                currentErrorMessage = null
                                
                                val name = sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.FULL_NAME) ?: ""
                                val email = sessionStore.getCurrentEmail()
                                val phone = sessionStore.getCurrentPhone()
                                val dob = sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.DOB) ?: ""
                                
                                platformLog("App: Retrying DigiLink for user ${screen.userId} with name=$name, dob=$dob")
                                authRepository.getDigiLink(
                                    userId = screen.userId,
                                    name = name,
                                    emailAddress = email,
                                    dateOfBirth = dob,
                                    mobileCountryCode = "+91",
                                    mobileNumber = phone.filter { it.isDigit() }.takeLast(10)
                                ).collect { result ->
                                    when (result) {
                                        is Resource.Success -> {
                                            isDigiLinkLoading = false
                                            val newReUrl = result.data?.reUrl
                                            val newAttemptId = result.data?.kycAttemptId
                                            if (!newAttemptId.isNullOrBlank()) {
                                                sessionStore.saveValue(KeyValueConstants.KYC_ATTEMPT_ID, newAttemptId)
                                            }
                                            if (!newReUrl.isNullOrBlank()) {
                                                navigateTo(Screen.KycWebView(screen.userId, newReUrl, newAttemptId))
                                            } else {
                                                currentErrorMessage = "Failed to get DigiLocker URL. Please try again."
                                            }
                                        }
                                        is Resource.Error -> {
                                            isDigiLinkLoading = false
                                            currentErrorMessage = result.message ?: "Failed to initiate DigiLocker. Please try again."
                                        }
                                        is Resource.Loading -> {
                                            isDigiLinkLoading = true
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.KycWebView -> {
                KycWebViewScreen(
                    url = screen.url,
                    onKycComplete = { status ->
                        if (status == "successful" || status == "COMPLETED") {
                            // Match Android logic: Navigate to Signature directly
                            scope.launch {
                                val kycAttemptId = screen.kycAttemptId ?: sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID)
                                val investorId = sessionStore.getValue(KeyValueConstants.INVESTOR_ID) ?: ""
                                platformLog("App: KYC Complete. Navigating to Signature with ID: $kycAttemptId, investorId: $investorId")
                                navigateTo(Screen.Signature(screen.userId, kycAttemptId ?: "", investorId))
                            }
                        } else {
                            // Match Android logic: Go back to KYC Information to retry with an error message
                            if (screenStack.size > 1) {
                                val prevIndex = screenStack.size - 2
                                val prevScreen = screenStack[prevIndex]
                                if (prevScreen is Screen.KycInformation) {
                                    // Always show the specific Aadhaar sharing error message as requested
                                    val aadhaarShareError = "KYC verification failed. Please ensure you allow sharing your Aadhaar details with Fintech Primitives during the DigiLocker verification process. You can try again by clicking the button below."
                                    screenStack[prevIndex] = prevScreen.copy(errorMessage = aadhaarShareError)
                                }
                            }
                            navigateBack()
                        }
                    },
                    onBack = { navigateBack() }
                )
            }
            is Screen.EsignWebView -> {
                EsignWebViewScreen(
                    url = screen.url,
                    onEsignComplete = {
                        // Match Android logic: Navigate to Investment Dashboard after successful e-sign
                        platformLog("App: Esign Complete. Navigating to Investment Dashboard")
                        navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                    },
                    onBack = { navigateBack() }
                )
            }
            is Screen.EsignInformation -> {
                EsignInformationScreenV2(
                    onProceed = {
                        scope.launch {
                            val esignUrl = sessionStore.getValue(KeyValueConstants.ESIGN_URL)
                            val reUrl = sessionStore.getValue(KeyValueConstants.RE_URL)
                            val finalUrl = esignUrl ?: reUrl
                            val kycAttemptId = sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID)
                            if (!finalUrl.isNullOrBlank()) {
                                platformLog("App: Found saved esign URL: $finalUrl. Navigating directly to EsignWebView.")
                                navigateTo(Screen.EsignWebView(screen.userId, finalUrl, kycAttemptId))
                            } else {
                                platformLog("App: No saved esign URL in sessionStore. Refreshing state via PreVerification.")
                                navigateTo(Screen.PreVerification(screen.userId))
                            }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.MinDetails -> {
                val minVm: MinDetailsViewModel = koinInject()
                MinDetailsScreenV2(
                    onNext = { nextScreen, kycAttemptId, confirmedEmail ->
                        scope.launch {
                            if (!kycAttemptId.isNullOrBlank()) {
                                sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.KYC_ATTEMPT_ID, kycAttemptId)
                            }
                            if (confirmedEmail.isNotBlank()) {
                                sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.EMAIL, confirmedEmail)
                            }
                            handleNavigation(nextScreen, screen.userId, kycAttemptId, null, null, sessionStore = sessionStore) { navigateTo(it) }
                        }
                    },
                    viewModel = minVm,
                    userId = screen.userId,
                    pan = screen.pan,
                    email = screen.email,
                    phone = screen.phone,
                    token = screen.token,
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.NameDob -> {
                val nameVm: NameDobViewModel = koinInject()
                NameDobScreenV2(
                    onKycSubmitted = { name, dob, confirmedEmail, navInfo, data ->
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
                            if (confirmedEmail.isNotBlank()) {
                                sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.EMAIL, confirmedEmail)
                            }
                            
                            // Save name and dob for potential DigiLink retry
                            sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.FULL_NAME, name)
                            sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.DOB, dob)

                            val nextAction = if (!reUrl.isNullOrBlank()) {
                                ScreenNames.KYC_INFORMATION
                            } else {
                                navInfo?.nextScreen ?: ScreenNames.KYC_INFORMATION
                            }
                            
                            handleNavigation(
                                action = nextAction,
                                userId = screen.userId,
                                reUrl = reUrl,
                                sessionStore = sessionStore
                            ) { navigateTo(it) }
                        }
                    },
                    userId = screen.userId,
                    pan = screen.pan,
                    email = screen.email,
                    phone = screen.phone,
                    token = screen.token,
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.CheckPanPopulatedDetails -> {
                val checkVm: CheckPanPopulatedDetailsViewModel = koinInject()
                CheckPanPopulatedDetailsScreen(
                    onSubmit = { name, gender, dob, father, marital, perm, corr ->
                        checkVm.submitDetails(screen.userId, screen.preVerificationId, name, gender, dob, father, marital, perm, corr)
                        scope.launch {
                            handleNavigation(ScreenNames.INITIAL_DASHBOARD, screen.userId, null, null, screen.preVerificationId, sessionStore = sessionStore) { navigateTo(it) }
                        }
                    }
                )
            }
            is Screen.InitialDashboard -> {
                InitialDashboardScreenV2(
                    userId = screen.userId,
                    onNavigateToOnboarding = { goalId, userId ->
                        scope.launch {
                            sessionStore.saveValue(KeyValueConstants.SELECTED_GOAL_ID, goalId)
                            navigateTo(Screen.MinimalPermission(userId = userId, isNewUser = true))
                        }
                    },
                    onNavigateToRoute = { nextScreen, preVerificationId ->
                        scope.launch {
                            if (!preVerificationId.isNullOrBlank()) {
                                sessionStore.saveValue("pre_verification_id", preVerificationId)
                                sessionStore.saveValue(KeyValueConstants.USER_PURPOSE_ID, preVerificationId)
                            }
                            handleNavigation(nextScreen, screen.userId, null, null, preVerificationId, sessionStore = sessionStore) { navigateTo(it) }
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onLogout = {
                        scope.launch {
                            sessionStore.saveValue(KeyValueConstants.LAST_SCREEN, "")
                            sessionStore.logout()
                            navigateTo(Screen.PhoneVerification, clearStack = true)
                        }
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
                InvestmentDashboardPlatformView(
                    userId = screen.userId,
                    onNavigateToSchemeDetails = { purpose ->
                        navigateTo(Screen.SchemeDetails(screen.userId, purpose))
                    },
                    onNavigateToGoal = { goalId ->
                        navigateTo(Screen.SipAmountV2(screen.userId, "", "", goalId, fromDashboard = true, isExistingInvestment = false))
                    },
                    onNavigateToWithdraw = {
                        navigateTo(Screen.Withdraw(screen.userId))
                    },
                    onNavigateToProfile = { navigateTo(Screen.Profile(screen.userId)) },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onNavigateToReferral = { navigateTo(Screen.Referral(screen.userId)) },
                    onStartKyc = { navigateTo(Screen.KycInformation(screen.userId)) },
                    onRetryKyc = { navigateTo(Screen.PreVerification(screen.userId)) }
                )
            }
            is Screen.SchemeDetails -> {
                SchemeDetailsV2Screen(
                    userId = screen.userId,
                    purpose = screen.purpose,
                    onNavigateBack = { navigateBack() },
                    onNavigateToWithdraw = { params ->
                        WithdrawParamsManager.set(params)
                        navigateTo(Screen.Withdraw(screen.userId))
                    },
                    onNavigateToAddFunds = { uid, kycId, invId, gid, isExisting ->
                        navigateTo(Screen.SipAmountV2(uid, kycId, invId, gid, fromDashboard = true, isExistingInvestment = isExisting))
                    },
                    onNavigateToLumpsum = { uid, kycId, invId, gid, isExisting ->
                        navigateTo(Screen.LumpsumAmountV2(uid, kycId, invId, gid, isExistingInvestment = isExisting))
                    },
                    onNavigateToFundDetails = { isin, uid, gid, sipAmt, kycId, invId, fromSip ->
                        if (fromSip) {
                            navigateTo(Screen.FundDetails(isin, uid, gid, sipAmt, kycAttemptId = kycId, investorId = invId, fromSipAmount = fromSip))
                        } else if (sipAmt > 0.0) {
                            navigateTo(Screen.LumpsumFundDetails(isin, uid, gid, sipAmt, kycAttemptId = kycId, investorId = invId))
                        } else {
                            navigateTo(Screen.FundDetailsViewOnly(isin, uid, gid))
                        }
                    }
                )
            }
            is Screen.Withdraw -> {
                WithdrawScreen(
                    userId = screen.userId,
                    onNavigateBack = { navigateBack() },
                    onProceed = { schemeId, _ ->
                        navigateTo(Screen.WithdrawAmount(screen.userId, schemeId ?: ""))
                    }
                )
            }
            is Screen.WithdrawAmount -> {
                WithdrawAmountScreen(
                    userId = screen.userId,
                    selectedSchemeId = screen.schemeId,
                    onNavigateBack = { navigateBack() },
                    onSubmit = { _, amount ->
                        val data = WithdrawalDataManager.getWithdrawalData()
                        if (data != null) {
                            navigateTo(Screen.WithdrawSuccess(
                                userId = screen.userId,
                                amount = data.amount,
                                schemeName = data.schemeName,
                                bankName = data.bankName,
                                bankAccountLast4 = data.bankAccountLast4,
                                transactionId = data.transactionId,
                                redemptionId = data.redemptionId,
                                redemptionGroupId = data.redemptionGroupId,
                                folio = data.folio,
                                redemptionMode = data.mode
                            ))
                        } else {
                            // Fallback if data is missing
                            navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                        }
                    }
                )
            }
            is Screen.WithdrawSuccess -> {
                WithdrawSuccessScreenV2(
                    withdrawalAmount = screen.amount,
                    schemeName = screen.schemeName,
                    bankName = screen.bankName,
                    bankAccountLast4 = screen.bankAccountLast4,
                    redemptionId = screen.redemptionId,
                    userId = screen.userId,
                    folio = screen.folio,
                    redemptionMode = screen.redemptionMode,
                    redemptionGroupId = screen.redemptionGroupId,
                    onNavigateToHome = {
                        navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                    }
                )
            }
            is Screen.FundDetails -> {
                FundDetailsScreen(
                    isin = screen.isin,
                    userId = screen.userId,
                    goalId = screen.goalId,
                    sipAmount = screen.sipAmount,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    fromSipAmount = screen.fromSipAmount,
                    frequency = screen.frequency,
                    installmentDay = screen.installmentDay,
                    onBackClick = { navigateBack() },
                    onStartKyc = { navigateTo(Screen.KycInformation(screen.userId)) },
                    onSipCreated = { amount, nextScreen, mandate ->
                        // Refresh or navigate forward after SIP creation
                        if (nextScreen == com.pyllar.consumer.navigation.ScreenNames.MANDATE_AUTH && mandate != null) {
                            navigateTo(Screen.MandateAuth(
                                userId = screen.userId,
                                kycAttemptId = screen.kycAttemptId,
                                investorId = screen.investorId,
                                amount = amount,
                                mandateUrl = mandate.uri ?: "",
                                mandateId = mandate.mandateId ?: 0L,
                                mandateRef = mandate.finMandateId ?: 0L,
                                goalId = screen.goalId
                            ))
                        } else if (nextScreen != null) {
                            scope.launch {
                                handleNavigation(nextScreen, screen.userId, screen.kycAttemptId, screen.investorId, sessionStore = sessionStore) { navigateTo(it) }
                            }
                        } else {
                            navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                        }
                    }
                )
            }
            is Screen.LumpsumFundDetails -> {
                LumpsumFundDetailsScreen(
                    isin = screen.isin,
                    userId = screen.userId,
                    goalId = screen.goalId,
                    lumpsumAmount = screen.lumpsumAmount,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    onBackClick = { navigateBack() },
                    onLumpsumCreated = { amount, nextScreen, mandate ->
                        if (mandate != null) {
                            navigateTo(Screen.LumpsumPurchaseAuth(
                                userId = screen.userId,
                                kycAttemptId = screen.kycAttemptId,
                                investorId = screen.investorId,
                                amount = amount,
                                paymentUrl = mandate.uri ?: "",
                                paymentId = mandate.mandateId ?: 0L,
                                paymentRef = mandate.finMandateId ?: 0L,
                                goalId = screen.goalId
                            ))
                        } else if (nextScreen != null) {
                            scope.launch {
                                handleNavigation(nextScreen, screen.userId, screen.kycAttemptId, screen.investorId, sessionStore = sessionStore) { navigateTo(it) }
                            }
                        } else {
                            navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                        }
                    }
                )
            }
            is Screen.SipAmountV2 -> {
                SipAmountScreenV3(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    goalId = screen.goalId,
                    isExistingInvestment = screen.isExistingInvestment,
                    onStartKyc = { navigateTo(Screen.KycInformation(screen.userId)) },
                    onSipCreated = { amount, nextScreen, mandate ->
                        if (nextScreen == com.pyllar.consumer.navigation.ScreenNames.MANDATE_AUTH && mandate != null) {
                            navigateTo(Screen.MandateAuth(
                                userId = screen.userId,
                                kycAttemptId = screen.kycAttemptId,
                                investorId = screen.investorId,
                                amount = amount,
                                mandateUrl = mandate.uri ?: "",
                                mandateId = mandate.mandateId ?: 0L,
                                mandateRef = mandate.finMandateId ?: 0L,
                                goalId = screen.goalId
                            ))
                        } else if (nextScreen != null) {
                            scope.launch {
                                handleNavigation(nextScreen, screen.userId, screen.kycAttemptId, screen.investorId, sessionStore = sessionStore) { navigateTo(it) }
                            }
                        } else {
                            navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                        }
                    },
                    onNavigateBack = { navigateBack() },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onNavigateToFundDetails = { userId, goalId, amt, kycId, invId, freq, day ->
                        navigateTo(Screen.FundDetails("", userId, goalId, amt, kycAttemptId = kycId, investorId = invId, fromSipAmount = true, frequency = freq, installmentDay = day))
                    }
                )
            }
            is Screen.LumpsumAmountV2 -> {
                LumpsumAmountScreenV3(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    goalId = screen.goalId,
                    isExistingInvestment = screen.isExistingInvestment,
                    onLumpsumCreated = { amount, nextScreen, mandate ->
                        if (mandate != null) {
                            navigateTo(Screen.LumpsumPurchaseAuth(
                                userId = screen.userId,
                                kycAttemptId = screen.kycAttemptId,
                                investorId = screen.investorId,
                                amount = amount,
                                paymentUrl = mandate.uri ?: "",
                                paymentId = mandate.mandateId ?: 0L,
                                paymentRef = mandate.finMandateId ?: 0L,
                                goalId = screen.goalId
                            ))
                        } else {
                            navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true)
                        }
                    },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onNavigateToFundDetails = { userId, goalId, amt, kycId, invId ->
                         navigateTo(Screen.LumpsumFundDetails("", userId, goalId, amt, kycAttemptId = kycId, investorId = invId))
                    },
                    onNavigateBack = { navigateBack() }
                )
            }
            is Screen.LumpsumPurchaseAuth -> {
                LumpsumPurchaseAuthScreen(
                    userId = screen.userId,
                    kycAttemptId = screen.kycAttemptId,
                    investorId = screen.investorId,
                    amount = screen.amount,
                    paymentUrl = screen.paymentUrl,
                    paymentId = screen.paymentId,
                    paymentRef = screen.paymentRef,
                    goalId = screen.goalId,
                    onGoToHome = { navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true) },
                    onNavigateBack = { navigateBack() },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.FundDetailsViewOnly -> {
                val fundDetailsVm: FundDetailsViewModel = koinInject()
                FundDetailsViewOnlyScreen(
                    isin = screen.isin,
                    userId = screen.userId,
                    goalId = screen.goalId,
                    onBackClick = { navigateBack() },
                    viewModel = fundDetailsVm
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
                    goalId = screen.goalId,
                    onGoToHome = { navigateTo(Screen.InvestmentDashboard(screen.userId), clearStack = true) },
                    onNavigateBack = { navigateBack() },
                    onNavigateToHelp = { navigateTo(Screen.HelpSupport(screen.userId)) }
                )
            }
            is Screen.Profile -> {
                ProfileScreen(
                    userId = screen.userId,
                    onLogout = { 
                        scope.launch {
                            sessionStore.saveValue(KeyValueConstants.LAST_SCREEN, "") // Clear last screen on logout
                            sessionStore.logout()
                            navigateTo(Screen.PhoneVerification, clearStack = true) 
                        }
                    },
                    onDeleteAccount = { navigateTo(Screen.AccountDeletion(screen.userId)) },
                    onHelpSupport = { navigateTo(Screen.HelpSupport(screen.userId)) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.Referral -> {
                val referralVm: ReferralViewModel = koinInject()
                val uiState by referralVm.uiState.collectAsState()
                val platformActions: PlatformActions = koinInject()
                ReferralScreen(
                    userId = screen.userId,
                    uiState = uiState,
                    onBackClick = { navigateBack() },
                    onShareClick = {
                        val text = uiState.shareMessage.ifBlank {
                            "Join me on Pyllar! Use code ${uiState.referralCode} and we both earn rewards on your first investment. ${uiState.shareUrl}"
                        }
                        platformActions.shareText(text, "Invite a friend")
                    },
                    onWhatsAppShareClick = {
                        val text = uiState.shareMessage.ifBlank {
                            "Join me on Pyllar! Use code ${uiState.referralCode} and we both earn rewards on your first investment. ${uiState.shareUrl}"
                        }
                        platformActions.openWhatsApp("", text)
                    },
                    onWithdrawClick = { amount ->
                        referralVm.requestRedemption(amount)
                    },
                    onDismissSuccessMessage = { referralVm.dismissSuccessMessage() },
                    onDismissErrorMessage = { referralVm.dismissErrorMessage() },
                    onRetryClick = { referralVm.loadAll() }
                )
            }
            is Screen.AccountDeletion -> {
                AccountDeletionScreen(
                    userId = screen.userId,
                    onBack = { navigateBack() }
                )
            }
            is Screen.HelpSupport -> {
                HelpSupportScreenV2(
                    showKycHelp = screen.showKycHelp,
                    showBankHelp = screen.showBankHelp,
                    showOnlyKycInfo = screen.showOnlyKycInfo,
                    onClose = { navigateBack() }
                )
            }
            is Screen.NotificationWebView -> {
                NotificationWebViewScreen(
                    url = screen.url,
                    title = screen.title,
                    onBack = { navigateBack() }
                )
            }
            is Screen.PennyDropLoading -> {
                PennyDropLoadingScreen(
                    userId = screen.userId,
                    onBack = { navigateBack() },
                    onComplete = { nextScreen ->
                        scope.launch {
                            if (screenStack.lastOrNull() is Screen.PennyDropLoading) {
                                screenStack.removeAt(screenStack.size - 1)
                            }
                            handleNavigation(nextScreen, screen.userId, sessionStore = sessionStore) { targetScreen ->
                                if (screenStack.lastOrNull() == targetScreen) {
                                    // Target screen is already current screen, force refresh stack item
                                    screenStack[screenStack.size - 1] = targetScreen
                                } else {
                                    navigateTo(targetScreen)
                                }
                            }
                        }
                    }
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
        }
    }
}

private suspend fun handleNavigation(
    action: String?,
    userId: String,
    kycAttemptId: String? = null,
    investorId: String? = null,
    preVerificationId: String? = null,
    reUrl: String? = null,
    notificationUrl: String? = null,
    sessionStore: com.pyllar.consumer.domain.storage.SessionStore,
    onNavigate: (Screen) -> Unit
) {
    platformLog("AppNav: handleNavigation: action='$action', userId='$userId'")
    com.pyllar.consumer.util.Log.d("AppNav", "handleNavigation: action=$action, userId=$userId")
    
    // Fetch missing values from session if not provided
    val effectiveKycId = kycAttemptId ?: sessionStore.getValue(KeyValueConstants.KYC_ATTEMPT_ID)
    val effectiveInvestorId = investorId ?: sessionStore.getValue(KeyValueConstants.INVESTOR_ID)
    val effectiveReUrl = reUrl ?: sessionStore.getValue(KeyValueConstants.RE_URL)
    val effectivePreVerificationId = preVerificationId ?: sessionStore.getValue("pre_verification_id")

    when (action) {
        ScreenNames.PRE_VERIFICATION -> {
            platformLog("AppNav: Matched PRE_VERIFICATION")
            onNavigate(Screen.PreVerification(userId))
        }
        ScreenNames.ADDITIONAL_KYC -> {
            platformLog("AppNav: Matched ADDITIONAL_KYC with kycAttemptId: $effectiveKycId")
            onNavigate(Screen.AdditionalKyc(userId, effectiveKycId ?: ""))
        }
        ScreenNames.NOMINEE_DETAILS -> {
            platformLog("AppNav: Matched NOMINEE_DETAILS")
            onNavigate(Screen.NomineeDetails(userId, effectiveKycId ?: "", effectiveInvestorId ?: ""))
        }
        ScreenNames.SIGNATURE -> {
            platformLog("AppNav: Matched SIGNATURE")
            onNavigate(Screen.Signature(userId, effectiveKycId ?: "", effectiveInvestorId ?: ""))
        }
        ScreenNames.BANK_DETAILS -> {
            platformLog("AppNav: Matched BANK_DETAILS")
            onNavigate(Screen.BankDetails(userId, effectiveKycId ?: ""))
        }
        ScreenNames.KYC_INFORMATION -> {
            platformLog("AppNav: Matched KYC_INFORMATION with reUrl: ${effectiveReUrl != null}")
            onNavigate(Screen.KycInformation(userId, effectiveReUrl, effectiveKycId))
        }
        ScreenNames.ESIGN_INFORMATION -> {
            platformLog("AppNav: Matched ESIGN_INFORMATION")
            onNavigate(Screen.EsignInformation(userId))
        }
        ScreenNames.WEB_VIEW, "web_view", "webview" -> {
            platformLog("AppNav: Matched WEB_VIEW")
            if (!notificationUrl.isNullOrBlank()) {
                onNavigate(Screen.NotificationWebView(notificationUrl, "Notification"))
            } else if (!effectiveReUrl.isNullOrBlank()) {
                onNavigate(Screen.KycWebView(userId, effectiveReUrl, effectiveKycId))
            } else {
                onNavigate(Screen.KycInformation(userId, null, effectiveKycId))
            }
        }
        ScreenNames.WEB_VIEW_ESIGN, "web_view_esign", "webview_esign" -> {
            platformLog("AppNav: Matched WEB_VIEW_ESIGN")
            if (!effectiveReUrl.isNullOrBlank()) {
                onNavigate(Screen.EsignWebView(userId, effectiveReUrl, effectiveKycId))
            } else {
                onNavigate(Screen.PreVerification(userId))
            }
        }
        ScreenNames.PAN_KYC -> {
            platformLog("AppNav: Matched PAN_KYC")
            onNavigate(Screen.PanKyc(userId, effectivePreVerificationId))
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
            onNavigate(Screen.CheckPanPopulatedDetails(userId, effectivePreVerificationId))
        }
        ScreenNames.PENNY_DROP_LOADING -> {
            platformLog("AppNav: Matched PENNY_DROP_LOADING")
            onNavigate(Screen.PennyDropLoading(userId))
        }
        ScreenNames.INITIAL_DASHBOARD -> {
            platformLog("AppNav: Matched INITIAL_DASHBOARD")
            onNavigate(Screen.InitialDashboard(userId))
        }
        ScreenNames.SIP_AMOUNT_V2 -> {
            platformLog("AppNav: Matched SIP_AMOUNT_V2 - setting fromDashboard=true")
            onNavigate(Screen.SipAmountV2(userId, effectiveKycId ?: "", effectiveInvestorId ?: "", "", true))
        }
        ScreenNames.MANDATE_AUTH -> {
            platformLog("AppNav: Matched MANDATE_AUTH")
            onNavigate(Screen.MandateAuth(userId, effectiveKycId ?: "", effectiveInvestorId ?: "", 0.0, "", 0L, 0L, ""))
        }
        ScreenNames.DASHBOARD, ScreenNames.INVESTMENT_DASHBOARD -> {
            platformLog("AppNav: Matched DASHBOARD/INVESTMENT_DASHBOARD")
            onNavigate(Screen.InvestmentDashboard(userId))
        }
        else -> {
            platformLog("AppNav: Unrecognized action '$action', staying on current screen.")
            com.pyllar.consumer.util.Log.d("AppNav", "Unrecognized notification action '$action'. Staying on current screen.")
        }
    }
}