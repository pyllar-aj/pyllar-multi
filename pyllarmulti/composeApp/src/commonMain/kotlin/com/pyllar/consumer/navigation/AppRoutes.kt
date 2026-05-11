package com.pyllar.consumer.navigation

private fun encodeUrl(url: String): String {
    return buildString {
        for (c in url) {
            when {
                c.isLetterOrDigit() || c in "-_.~" -> append(c)
                else -> {
                    val bytes = c.toString().encodeToByteArray()
                    for (b in bytes) {
                        append('%')
                        append(
                            (b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
                        )
                    }
                }
            }
        }
    }
}

sealed class AppRoutes(val route: String) {
    // Auth Routes
    object PhoneVerification : AppRoutes("phone_verification")
    object OtpVerification : AppRoutes("otp_verification/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp_verification/$phoneNumber"
    }
    object Login : AppRoutes("login_screen")
    object Signup : AppRoutes("signup_screen")

    // Main Routes
    object Home : AppRoutes("home")
    object MutualFund : AppRoutes("mutual_fund")

    // Onboarding Routes
    object IntroSteps : AppRoutes("intro_steps")
    object Permission : AppRoutes("permission")
    object MinimalPermission : AppRoutes("minimal_permission")
    object PreVerification : AppRoutes("pre_verification")
    object PanKyc : AppRoutes("pan_kyc")
    object NameDob : AppRoutes("name_dob?email={email}") {
        fun createRoute(email: String): String {
            return if (email.isNotBlank()) "name_dob?email=$email" else "name_dob"
        }
    }
    object KycInformation : AppRoutes("kyc_information")
    object MinDetails : AppRoutes("min_details")
    object CheckPanPopulatedDetails : AppRoutes("check_pan_populated_details")
    object WebView : AppRoutes("webview")
    object NotificationWebView : AppRoutes("notification_webview/{url}") {
        fun createRoute(url: String): String {
            val encoded = encodeUrl(url)
            return "notification_webview/$encoded"
        }
    }
    object WebViewEsign : AppRoutes("webview_esign")
    object AdditionalKyc : AppRoutes("additional_kyc/{kycAttemptId}") {
        fun createRoute(kycAttemptId: String): String {
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            return "additional_kyc/$safeKycAttemptId"
        }
    }
    object BankDetails : AppRoutes("bank_details/{userId}/{kycAttemptId}") {
        fun createRoute(userId: String, kycAttemptId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            return "bank_details/$safeUserId/$safeKycAttemptId"
        }
    }
    object Signature : AppRoutes("signature/{userId}/{kycAttemptId}/{investorId}") {
        fun createRoute(userId: String, kycAttemptId: String, investorId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            return "signature/$safeUserId/$safeKycAttemptId/$safeInvestorId"
        }
    }
    object EsignInformation : AppRoutes("esign_information")
    object NomineeDetails : AppRoutes("nominee_details/{userId}/{kycAttemptId}/{investorId}") {
        fun createRoute(userId: String, kycAttemptId: String, investorId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            return "nominee_details/$safeUserId/$safeKycAttemptId/$safeInvestorId"
        }
    }
    object SipAmount : AppRoutes("sip_amount/{userId}/{kycAttemptId}/{investorId}/{goalId}?isExistingInvestment={isExistingInvestment}") {
        fun createRoute(
            userId: String,
            kycAttemptId: String,
            investorId: String,
            goalId: String = "",
            isExistingInvestment: Boolean = false
        ): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            val safeGoalId = if (goalId.isBlank()) "" else goalId
            val queryParam = if (isExistingInvestment) "?isExistingInvestment=true" else ""
            return "sip_amount/$safeUserId/$safeKycAttemptId/$safeInvestorId/$safeGoalId$queryParam"
        }
    }
    object SipAmountV2 : AppRoutes("sip_amount_v2/{userId}/{kycAttemptId}/{investorId}/{goalId}?isExistingInvestment={isExistingInvestment}") {
        fun createRoute(
            userId: String,
            kycAttemptId: String,
            investorId: String,
            goalId: String = "",
            isExistingInvestment: Boolean = false
        ): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            val safeGoalId = if (goalId.isBlank()) "" else goalId
            val queryParam = if (isExistingInvestment) "?isExistingInvestment=true" else ""
            return "sip_amount_v2/$safeUserId/$safeKycAttemptId/$safeInvestorId/$safeGoalId$queryParam"
        }
    }
    object LumpsumAmountV2 : AppRoutes("lumpsum_amount_v2/{userId}/{kycAttemptId}/{investorId}/{goalId}?isExistingInvestment={isExistingInvestment}") {
        fun createRoute(
            userId: String,
            kycAttemptId: String,
            investorId: String,
            goalId: String = "",
            isExistingInvestment: Boolean = false
        ): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            val safeGoalId = if (goalId.isBlank()) "" else goalId
            val queryParam = if (isExistingInvestment) "?isExistingInvestment=true" else ""
            return "lumpsum_amount_v2/$safeUserId/$safeKycAttemptId/$safeInvestorId/$safeGoalId$queryParam"
        }
    }
    object LumpsumPurchaseAuth : AppRoutes("lumpsum_purchase_auth/{userId}/{kycAttemptId}/{investorId}/{amount}/{paymentUrl}/{paymentId}/{paymentRef}/{goalId}") {
        fun createRoute(
            userId: String,
            kycAttemptId: String,
            investorId: String,
            amount: Double,
            paymentUrl: String,
            paymentId: Long,
            paymentRef: Long,
            goalId: String = ""
        ): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            val safeGoalId = if (goalId.isBlank()) "" else goalId
            val encodedUrl = encodeUrl(paymentUrl)
            return "lumpsum_purchase_auth/$safeUserId/$safeKycAttemptId/$safeInvestorId/$amount/$encodedUrl/$paymentId/$paymentRef/$safeGoalId"
        }
    }
    object MandateAuth : AppRoutes("mandate_auth/{userId}/{kycAttemptId}/{investorId}/{amount}/{mandateUrl}/{mandateId}/{mandateRef}") {
        fun createRoute(
            userId: String,
            kycAttemptId: String,
            investorId: String,
            amount: Double,
            mandateUrl: String,
            mandateId: Long,
            mandateRef: Long
        ): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeKycAttemptId = if (kycAttemptId.isBlank()) "" else kycAttemptId
            val safeInvestorId = if (investorId.isBlank()) "" else investorId
            val encodedUrl = encodeUrl(mandateUrl)
            return "mandate_auth/$safeUserId/$safeKycAttemptId/$safeInvestorId/$amount/$encodedUrl/$mandateId/$mandateRef"
        }
    }
    object Dashboard : AppRoutes("dashboard/{userId}?sipAmount={sipAmount}&username={username}") {
        fun createRoute(userId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            return "dashboard/$safeUserId"
        }

        @Deprecated("Use createRoute(userId) instead. Server provides all data based on userId.")
        fun createRoute(userId: String, sipAmount: Double = 0.0, username: String = ""): String {
            return createRoute(userId)
        }
    }
    object InvestmentDashboard : AppRoutes("investment_dashboard/{userId}") {
        fun createRoute(userId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            return "investment_dashboard/$safeUserId"
        }
    }
    object InitialDashboard : AppRoutes("initial_dashboard/{userId}") {
        fun createRoute(userId: String = ""): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            return "initial_dashboard/$safeUserId"
        }
    }
    object Withdraw : AppRoutes("withdraw/{userId}") {
        fun createRoute(userId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            return "withdraw/$safeUserId"
        }
    }
    object WithdrawAmount : AppRoutes("withdraw_amount/{userId}/{schemeId}") {
        fun createRoute(userId: String, schemeId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safeSchemeId = if (schemeId.isBlank()) "" else schemeId
            return "withdraw_amount/$safeUserId/$safeSchemeId"
        }
    }
    object WithdrawSuccess : AppRoutes("withdraw_success")
    object SchemeDetails : AppRoutes("scheme_details/{userId}/{purpose}") {
        fun createRoute(userId: String, purpose: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            val safePurpose = if (purpose.isBlank()) "" else purpose
            return "scheme_details/$safeUserId/$safePurpose"
        }
    }
    object Profile : AppRoutes("profile/{userId}") {
        fun createRoute(userId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            return "profile/$safeUserId"
        }
    }

    object AccountDeletion : AppRoutes("account_deletion?userId={userId}") {
        fun createRoute(userId: String): String {
            val safeUserId = if (userId.isBlank()) "" else userId
            return "account_deletion?userId=$safeUserId"
        }
    }

    object HelpSupport : AppRoutes("help_support?showKycHelp={showKycHelp}&showBankHelp={showBankHelp}&showOnlyKycInfo={showOnlyKycInfo}") {
        fun createRoute(
            showKycHelp: Boolean = false,
            showBankHelp: Boolean = false,
            showOnlyKycInfo: Boolean = false
        ): String {
            val params = mutableListOf<String>()
            if (showKycHelp) params.add("showKycHelp=true")
            if (showBankHelp) params.add("showBankHelp=true")
            if (showOnlyKycInfo) params.add("showOnlyKycInfo=true")
            return if (params.isNotEmpty()) "help_support?${params.joinToString("&")}" else "help_support"
        }
    }

    // UPI routes — cross-platform (VPA-based flow, works on both Android and iOS)
    object UpiAccountLinking : AppRoutes("upi_account_linking")
    object UpiMandateSetup : AppRoutes("upi_mandate_setup/{sipAmount}/{fundName}") {
        fun createRoute(sipAmount: String, fundName: String): String {
            val safeSipAmount = if (sipAmount.isBlank()) "0" else sipAmount
            val safeFundName = encodeUrl(fundName)
            return "upi_mandate_setup/$safeSipAmount/$safeFundName"
        }
    }
}

