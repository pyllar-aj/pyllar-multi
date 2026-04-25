package com.pyllar.consumer.presentation.mutualfund.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.data.remote.model.dto.NavChartDataDto
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SipCreationResult {
    data class Success(val message: String, val nextScreen: String?, val mandateWrapper: MandateWrapper?) : SipCreationResult()
    data class Failure(val message: String) : SipCreationResult()
    object SecureChannelError : SipCreationResult()
}

class FundDetailsViewModel(
    private val repository: FundDetailsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FundDetailsState())
    val uiState: StateFlow<FundDetailsState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "FundDetailsViewModel"
    }

    fun loadFundDetails(isin: String) {
        viewModelScope.launch {
            platformLog("$TAG: \uD83D\uDD04 loadFundDetails called - isin: '$isin'")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getFundDetails(isin).collect { result ->
                handleResult(result)
            }
        }
    }

    fun loadFundDetailsByGoal(userId: String, goalType: String) {
        viewModelScope.launch {
            platformLog("$TAG: \uD83D\uDD04 loadFundDetailsByGoal called - userId: '$userId', goalType: '$goalType'")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getFundDetailsByGoal(userId, goalType).collect { result ->
                platformLog("$TAG: \uD83D\uDCE5 Repository returned result for goalType: '$goalType'")
                handleResult(result)
            }
        }
    }

    private fun handleResult(result: Resource<FundDetailsResponseDto>) {
        when (result) {
            is Resource.Success -> {
                platformLog("$TAG: \u2705 Fund details loaded successfully!")
                
                val rawData = result.data?.chartData?.get(mapPeriodToKey("1Y"))
                val sampledData = sampleChartData(rawData, "1Y")
                val isPositive = calculateIsPositiveReturn(sampledData)
                
                val bankDetails = result.data?.bankDetails
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    fundDetails = result.data,
                    chartData = sampledData,
                    isPositiveReturn = isPositive,
                    bankAccountNumber = bankDetails?.accountNumber,
                    bankIfscCode = bankDetails?.ifscCode,
                    bankName = bankDetails?.bankName
                )
            }
            is Resource.Error -> {
                platformLog("$TAG: \u274C Fund details load failed: ${result.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
            is Resource.Loading -> {
                platformLog("$TAG: \u23F3 Fund details loading...")
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
        }
    }

    fun onPeriodSelected(period: String) {
        val currentDetails = _uiState.value.fundDetails
        if (currentDetails != null) {
            val rawData = currentDetails.chartData?.get(mapPeriodToKey(period))
            val sampledData = sampleChartData(rawData, period)
            val isPositive = calculateIsPositiveReturn(sampledData)
            
            _uiState.value = _uiState.value.copy(
                selectedPeriod = period,
                chartData = sampledData,
                isPositiveReturn = isPositive
            )
        }
    }

    suspend fun createSip(
        userId: String,
        kycAttemptId: String,
        investorId: String,
        amount: Double
    ): SipCreationResult {
        var userPurposeId: String? = null
        try {
            userPurposeId = sessionStore.getValue("user_inv_purpose_id")
        } catch (e: Exception) {
            platformLog("$TAG: error reading user_inv_purpose_id: ${e.message}")
        }
        val effectiveUserInvPurpose = userPurposeId ?: ""

        platformLog("$TAG: createSip: userId=$userId, amount=$amount, userPurposeId=$userPurposeId")

        val requestPayload = CreateDailySipRequestDto(
            userId = userId,
            kycAttemptId = kycAttemptId,
            investorId = investorId,
            amount = amount,
            userInvPurpose = effectiveUserInvPurpose
        )

        var finalResult: SipCreationResult = SipCreationResult.Failure("Unknown Error")

        repository.createDailySip(requestPayload).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    try {
                        sessionStore.saveValue("sip_amount", amount.toString())
                    } catch (e: Exception) {
                        platformLog("$TAG: Error saving sip amount: ${e.message}")
                    }
                    val nextScreen = resource.navigation?.nextScreen
                    val mandateWrapper = resource.data
                    
                    platformLog("$TAG: createSip: nextScreen=$nextScreen, mandateWrapper=$mandateWrapper")
                    
                    finalResult = SipCreationResult.Success("SIP created successfully!", nextScreen, mandateWrapper)
                }
                is Resource.Error -> {
                    platformLog("$TAG: createSip: error - ${resource.message}")
                    finalResult = SipCreationResult.Failure("Failed: ${resource.message}")
                }
                is Resource.Loading -> {}
            }
        }
        return finalResult
    }

    private fun mapPeriodToKey(period: String): String {
        return when (period) {
            "1Y" -> "oneYear"
            "3Y" -> "threeYear"
            "5Y" -> "fiveYear"
            else -> "oneYear"
        }
    }

    private fun sampleChartData(data: List<NavChartDataDto>?, period: String): List<NavChartDataDto> {
        if (data.isNullOrEmpty()) return emptyList()
        val targetPoints = 12
        if (data.size <= targetPoints) return data
        
        val step = (data.size - 1).toFloat() / (targetPoints - 1)
        val sampled = mutableListOf<NavChartDataDto>()
        
        for (i in 0 until targetPoints) {
            val index = (i * step).toInt().coerceIn(0, data.size - 1)
            sampled.add(data[index])
        }
        
        sampled[0] = data.first()
        sampled[sampled.size - 1] = data.last()
        
        return sampled
    }

    private fun calculateIsPositiveReturn(data: List<NavChartDataDto>): Boolean {
        if (data.size < 2) return true
        val current = data.first().nav
        val oldest = data.last().nav
        return current >= oldest
    }
}

data class FundDetailsState(
    val isLoading: Boolean = false,
    val fundDetails: FundDetailsResponseDto? = null,
    val error: String? = null,
    val selectedPeriod: String = "1Y",
    val chartData: List<NavChartDataDto> = emptyList(),
    val isPositiveReturn: Boolean = true,
    val isSipCreating: Boolean = false,
    val sipError: String? = null,
    val bankAccountNumber: String? = null,
    val bankIfscCode: String? = null,
    val bankName: String? = null
)
