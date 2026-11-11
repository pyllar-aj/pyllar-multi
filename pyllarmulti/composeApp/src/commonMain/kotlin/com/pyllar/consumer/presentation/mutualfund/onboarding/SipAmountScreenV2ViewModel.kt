package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.data.remote.model.dto.CreateBasketSipRequestDto
import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.InvestmentLimitsResponseDto
import com.pyllar.consumer.data.remote.model.dto.LimitsSchemeDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pyllar.consumer.presentation.mutualfund.details.SipCreationResult
import kotlin.concurrent.Volatile

data class InvestmentLimitsState(
    val isLoading: Boolean = false,
    val minAmount: Long = 101,
    val maxAmount: Long = 500,
    val defaultAmount: Long? = null,
    val error: String? = null,
    /** Basket goals: the schemes the SIP goes into (empty = show the single scheme card from fund details). */
    val schemes: List<LimitsSchemeDto> = emptyList()
)

class SipAmountScreenV2ViewModel(
    private val fundDetailsRepository: FundDetailsRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _limitsState = MutableStateFlow(InvestmentLimitsState())
    val limitsState: StateFlow<InvestmentLimitsState> = _limitsState.asStateFlow()

    /** True once the goal's limits came from its basket: its SIP is then started through the basket API. */
    @Volatile
    private var isBasketGoal = false

    fun fetchInvestmentLimits(userInvestmentPurposeId: String) {
        viewModelScope.launch {
            _limitsState.value = _limitsState.value.copy(isLoading = true, error = null)
            
            // Goals with a basket take their range from it; any other goal (or any failure) uses the older limits below.
            isBasketGoal = false
            try { sessionStore.saveValue(KeyValueConstants.BASKET_GOAL_ID, "") } catch (_: Exception) {}

            try {
                var basketLimits: InvestmentLimitsResponseDto? = null
                fundDetailsRepository.getBasketInvestmentLimits(userInvestmentPurposeId).collect { result ->
                    if (result is Resource.Success) {
                        basketLimits = result.data
                    }
                }
                if (basketLimits?.min != null && basketLimits?.max != null) {
                    val limits = basketLimits!!
                    isBasketGoal = true
                    sessionStore.saveValue(KeyValueConstants.BASKET_GOAL_ID, userInvestmentPurposeId)
                    platformLog("SipAmountScreenV2ViewModel: ✅ Basket limits - min: ${limits.min}, max: ${limits.max}, default: ${limits.defaultAmount}")
                    _limitsState.value = InvestmentLimitsState(
                        isLoading = false,
                        minAmount = limits.min ?: 101L,
                        maxAmount = limits.max ?: 500L,
                        defaultAmount = limits.defaultAmount ?: limits.min ?: 101L,
                        error = null,
                        schemes = limits.schemes.orEmpty()
                    )
                    return@launch
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                platformLog("SipAmountScreenV2ViewModel: ⚠️ Basket limits unavailable, using older limits: ${e.message}")
            }

            try {
                platformLog("SipAmountScreenV2ViewModel: 🎯 fetchInvestmentLimits called")
                
                fundDetailsRepository.getInvestmentLimits(userInvestmentPurposeId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val response = result.data
                            val minAmount = response?.min ?: 101L
                            val maxAmount = response?.max ?: 500L
                            val defaultAmount = response?.defaultAmount ?: 101L
                            
                            _limitsState.value = InvestmentLimitsState(
                                isLoading = false,
                                minAmount = minAmount,
                                maxAmount = maxAmount,
                                defaultAmount = defaultAmount,
                                error = null,
                                schemes = emptyList()
                            )
                        }
                        is Resource.Error -> {
                            _limitsState.value = InvestmentLimitsState(
                                isLoading = false,
                                minAmount = 101L,
                                maxAmount = 500L,
                                defaultAmount = 101L,
                                error = result.message?.takeIf { it.isNotBlank() }?.toUserFriendlyErrorMessage(),
                                schemes = emptyList()
                            )
                        }
                        is Resource.Loading -> {
                            // loading already set
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                platformLog("SipAmountScreenV2ViewModel: 💥 Exception fetching investment limits: ${e.message}")
                _limitsState.value = InvestmentLimitsState(
                    isLoading = false,
                    minAmount = 101L,
                    maxAmount = 500L,
                    error = e.message?.takeIf { it.isNotBlank() }?.toUserFriendlyErrorMessage(),
                    schemes = emptyList()
                )
            }
        }
    }

    suspend fun createSip(
        userId: String,
        kycAttemptId: String,
        investorId: String,
        amount: Double,
        userInvPurpose: String? = null
    ): SipCreationResult {
        return try {
            val userPurposeId = sessionStore.getValue(KeyValueConstants.USER_PURPOSE_ID) ?: ""
            val effectiveUserInvPurpose = userPurposeId.ifBlank { userInvPurpose ?: "" }

            platformLog("SipAmountScreenV2ViewModel: 🚀 createSip initiated for userId='$userId', basketGoal=$isBasketGoal")
            var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create SIP. Please try again.")

            val flow = if (isBasketGoal) {
                val basketRequest = CreateBasketSipRequestDto(
                    userId = userId,
                    kycAttemptId = kycAttemptId,
                    investorId = investorId,
                    amount = amount,
                    userInvPurpose = effectiveUserInvPurpose,
                    frequency = "DAILY"
                )
                fundDetailsRepository.createBasketSip(basketRequest)
            } else {
                val requestPayload = CreateDailySipRequestDto(
                    userId = userId,
                    kycAttemptId = kycAttemptId,
                    investorId = investorId,
                    amount = amount,
                    userInvPurpose = effectiveUserInvPurpose
                )
                fundDetailsRepository.createDailySip(requestPayload)
            }

            flow.collect { result ->
                platformLog("SipAmountScreenV2ViewModel: 📥 Received repository result: ${result::class.simpleName}")
                when (result) {
                    is Resource.Success -> {
                        platformLog("SipAmountScreenV2ViewModel: ✅ Success received")
                        try {
                            sessionStore.saveValue("sip_amount", amount.toString())
                            sessionStore.saveValue("sip_frequency", "daily")
                            sessionStore.saveValue("sip_installment_day", "")
                        } catch (e: Exception) {
                            platformLog("SipAmountScreenV2ViewModel: ⚠️ Failed to save sip values: ${e.message}")
                        }
                        val mandateWrapper = result.data
                        val nextScreen = result.navigation?.nextScreen

                        if (isBasketGoal && mandateWrapper?.mandateId != null) {
                            sessionStore.saveValue(
                                key = KeyValueConstants.BASKET_SIP_MANDATE_ID,
                                value = mandateWrapper.mandateId.toString()
                            )
                        }

                        finalResult = SipCreationResult.Success("SIP created successfully!", nextScreen, mandateWrapper)
                    }
                    is Resource.Error -> {
                        platformLog("SipAmountScreenV2ViewModel: ❌ Error received: ${result.message}")
                        val friendly = result.message?.takeIf { it.isNotBlank() }?.toUserFriendlyErrorMessage()
                            ?: "Failed to create SIP. Please try again."
                        finalResult = SipCreationResult.Failure(friendly)
                    }
                    is Resource.Loading -> {
                        platformLog("SipAmountScreenV2ViewModel: ⏳ Loading...")
                    }
                }
            }
            platformLog("SipAmountScreenV2ViewModel: 🏁 createSip returning: ${finalResult::class.simpleName}")
            finalResult
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            platformLog("SipAmountScreenV2ViewModel: ❌ Exception: ${e.message}")
            val friendly = e.message?.takeIf { it.isNotBlank() }?.toUserFriendlyErrorMessage()
                ?: "Failed to create SIP. Please try again."
            SipCreationResult.Failure(friendly)
        }
    }
}
