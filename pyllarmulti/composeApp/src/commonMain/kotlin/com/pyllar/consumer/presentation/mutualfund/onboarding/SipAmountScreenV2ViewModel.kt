package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pyllar.consumer.presentation.mutualfund.details.SipCreationResult

data class InvestmentLimitsState(
    val isLoading: Boolean = false,
    val minAmount: Long = 101,
    val maxAmount: Long = 500,
    val defaultAmount: Long? = null,
    val error: String? = null
)

//sealed class SipCreationResult {
//    data class Success(val message: String, val nextScreen: String?, val mandateWrapper: MandateWrapper?) : SipCreationResult()
//    data class Failure(val error: String) : SipCreationResult()
//    object SecureChannelError : SipCreationResult()
//}

class SipAmountScreenV2ViewModel(
    private val fundDetailsRepository: FundDetailsRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _limitsState = MutableStateFlow(InvestmentLimitsState())
    val limitsState: StateFlow<InvestmentLimitsState> = _limitsState.asStateFlow()

    fun fetchInvestmentLimits(userInvestmentPurposeId: String) {
        viewModelScope.launch {
            _limitsState.value = _limitsState.value.copy(isLoading = true, error = null)
            
            try {
                platformLog("SipAmountScreenV2ViewModel: \uD83C\uDFAF fetchInvestmentLimits called")
                
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
                                error = null
                            )
                        }
                        is Resource.Error -> {
                            _limitsState.value = InvestmentLimitsState(
                                isLoading = false,
                                minAmount = 101L,
                                maxAmount = 500L,
                                defaultAmount = 101L,
                                error = result.message
                            )
                        }
                        is Resource.Loading -> {
                            // loading already set
                        }
                    }
                }
            } catch (e: Exception) {
                platformLog("SipAmountScreenV2ViewModel: \uD83D\uDCA5 Exception fetching investment limits: ${e.message}")
                _limitsState.value = InvestmentLimitsState(
                    isLoading = false,
                    minAmount = 101L,
                    maxAmount = 500L,
                    error = e.message
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
            val userPurposeId = sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.USER_PURPOSE_ID) ?: ""
            val effectiveUserInvPurpose = userPurposeId.ifBlank { userInvPurpose ?: "" }

            val requestPayload = CreateDailySipRequestDto(
                userId = userId,
                kycAttemptId = kycAttemptId,
                investorId = investorId,
                amount = amount,
                userInvPurpose = effectiveUserInvPurpose
            )

            platformLog("SipAmountScreenV2ViewModel: 🚀 createSip initiated for userId='$userId'")
            var finalResult: SipCreationResult = SipCreationResult.Failure("Unknown error")
            
            fundDetailsRepository.createDailySip(requestPayload).collect { result ->
                platformLog("SipAmountScreenV2ViewModel: 📥 Received repository result: ${result::class.simpleName}")
                when (result) {
                    is Resource.Success -> {
                        platformLog("SipAmountScreenV2ViewModel: ✅ Success received")
                        try {
                            sessionStore.saveValue("sip_amount", amount.toString())
                        } catch (e: Exception) {
                            platformLog("SipAmountScreenV2ViewModel: ⚠️ Failed to save sip amount: ${e.message}")
                        }
                        val mandateWrapper = result.data
                        val nextScreen = result.navigation?.nextScreen
                        finalResult = SipCreationResult.Success("SIP created successfully!", nextScreen, mandateWrapper)
                    }
                    is Resource.Error -> {
                        platformLog("SipAmountScreenV2ViewModel: ❌ Error received: ${result.message}")
                        finalResult = SipCreationResult.Failure(result.message ?: "Failed to create SIP")
                    }
                    is Resource.Loading -> {
                        platformLog("SipAmountScreenV2ViewModel: ⏳ Loading...")
                    }
                }
            }
            platformLog("SipAmountScreenV2ViewModel: 🏁 createSip returning: ${finalResult::class.simpleName}")
            finalResult
        } catch (e: Exception) {
            platformLog("SipAmountScreenV2ViewModel: \u274C Exception: ${e.message}")
            SipCreationResult.Failure("Network error: ${e.message}")
        }
    }
}
