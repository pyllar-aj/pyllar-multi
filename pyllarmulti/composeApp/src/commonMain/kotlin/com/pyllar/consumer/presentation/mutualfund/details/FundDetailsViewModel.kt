package com.pyllar.consumer.presentation.mutualfund.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.data.remote.model.dto.NavChartDataDto
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class SipCreationResult {
    data class Success(val message: String, val nextScreen: String?, val mandateWrapper: MandateWrapper?) : SipCreationResult()
    data class LumpsumSuccess(val message: String, val nextScreen: String?, val lumpsumData: com.pyllar.consumer.data.remote.model.dto.LumpsumPurchaseResponseData?) : SipCreationResult()
    data class Failure(val message: String) : SipCreationResult()
    object SecureChannelError : SipCreationResult()
}

class FundDetailsViewModel(
    private val repository: FundDetailsRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(FundDetailsState())
    val uiState: StateFlow<FundDetailsState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "FundDetailsViewModel"
    }

    fun loadFundDetails(isin: String) {
        viewModelScope.launch {
            platformLog("🔄 loadFundDetails called - isin: '$isin'")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getFundDetails(isin).collect { result ->
                handleResult(result)
            }
        }
    }

    fun loadFundDetailsByGoal(userId: String, goalType: String) {
        viewModelScope.launch {
            platformLog("🔄 loadFundDetailsByGoal called - userId: '$userId', goalType: '$goalType'")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getFundDetailsByGoal(userId, goalType).collect { result ->
                handleResult(result)
            }
        }
    }

    private fun handleResult(result: Resource<FundDetailsResponseDto>) {
        when (result) {
            is Resource.Success -> {
                val data = result.data
                val rawData = data?.chartData?.get(mapPeriodToKey("1Y"))
                val sampledData = sampleChartData(rawData, "1Y")
                val isPositive = calculateIsPositiveReturn(sampledData)
                
                val bankDetails = data?.bankDetails
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    fundDetails = data,
                    chartData = sampledData,
                    isPositiveReturn = isPositive,
                    bankAccountNumber = bankDetails?.accountNumber,
                    bankIfscCode = bankDetails?.ifscCode,
                    bankName = bankDetails?.bankName
                )
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
            is Resource.Loading -> {
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
        _uiState.value = _uiState.value.copy(isSipCreating = true, sipError = null)
        
        val userPurposeId = try {
            sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.USER_PURPOSE_ID)
        } catch (e: Exception) {
            null
        }
        
        val request = CreateDailySipRequestDto(
            userId = userId,
            kycAttemptId = kycAttemptId,
            investorId = investorId,
            amount = amount,
            userInvPurpose = userPurposeId ?: ""
        )
        
        var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create SIP")
        
        repository.createDailySip(request).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = false)
                    finalResult = SipCreationResult.Success(
                        message = "SIP created successfully!",
                        nextScreen = resource.navigation?.nextScreen,
                        mandateWrapper = resource.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = resource.message)
                    finalResult = SipCreationResult.Failure(resource.message ?: "Failed to create SIP")
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = true)
                }
            }
        }
        return finalResult
    }

    suspend fun createLumpsumPurchase(
        userId: String,
        amount: Double
    ): SipCreationResult {
        _uiState.value = _uiState.value.copy(isSipCreating = true, sipError = null)
        
        val userPurposeId = try {
            sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.USER_PURPOSE_ID)
        } catch (e: Exception) {
            null
        }
        
        val request = com.pyllar.consumer.data.remote.model.dto.CreateLumpsumPurchaseRequestDto(
            userId = userId,
            amount = amount,
            userInvPurpose = userPurposeId ?: ""
        )
        
        var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create purchase")
        
        repository.createLumpsumPurchase(request).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = false)
                    finalResult = SipCreationResult.LumpsumSuccess(
                        message = "Purchase created successfully!",
                        nextScreen = resource.navigation?.nextScreen,
                        lumpsumData = resource.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = resource.message)
                    finalResult = SipCreationResult.Failure(resource.message ?: "Failed to create purchase")
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = true)
                }
            }
        }
        return finalResult
    }

    fun clearSipError() {
        _uiState.value = _uiState.value.copy(sipError = null)
    }

    fun loadPastPerformance(userId: String, goalType: String) {
        viewModelScope.launch {
            repository.getPastPerformance(userId, goalType).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            pastPerformanceLoading = false,
                            pastPerformance = resource.data,
                            pastPerformanceError = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            pastPerformanceLoading = false,
                            pastPerformanceError = resource.message
                        )
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(pastPerformanceLoading = true)
                    }
                }
            }
        }
    }

    suspend fun createPurchasePlan(
        userId: String,
        kycAttemptId: String,
        investorId: String,
        amount: Double,
        frequency: String,
        installmentDay: Int? = null,
        numberOfInstallments: Int? = null
    ): SipCreationResult {
        _uiState.value = _uiState.value.copy(isSipCreating = true, sipError = null)
        
        val userPurposeId = try {
            sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.USER_PURPOSE_ID)
        } catch (e: Exception) {
            null
        }
        
        val request = com.pyllar.consumer.data.remote.model.dto.CreatePurchasePlanRequestDto(
            userId = userId,
            kycAttemptId = kycAttemptId,
            investorId = investorId,
            amount = amount,
            userInvPurpose = userPurposeId ?: "",
            frequency = frequency,
            installmentDay = installmentDay,
            numberOfInstallments = numberOfInstallments
        )
        
        var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create purchase plan")
        
        repository.createPurchasePlan(request).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = false)
                    try {
                        sessionStore.saveValue("sip_amount", amount.toString())
                    } catch (e: Exception) {
                        platformLog("FundDetailsViewModel: ⚠️ Failed to save sip amount: ${e.message}")
                    }
                    finalResult = SipCreationResult.Success(
                        message = "Purchase plan created successfully!",
                        nextScreen = resource.navigation?.nextScreen,
                        mandateWrapper = resource.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = resource.message)
                    finalResult = SipCreationResult.Failure(resource.message ?: "Failed to create purchase plan")
                }
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isSipCreating = true)
                }
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
    val bankName: String? = null,
    val pastPerformance: com.pyllar.consumer.data.remote.model.dto.PastPerformanceResponseDto? = null,
    val pastPerformanceLoading: Boolean = false,
    val pastPerformanceError: String? = null
)
