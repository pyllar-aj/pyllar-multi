package com.pyllar.consumer.presentation.mutualfund.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.data.remote.model.dto.CreateBasketSipRequestDto
import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.data.remote.model.dto.NavChartDataDto
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import kotlinx.coroutines.CancellationException
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

    /** One named scheme of a multi-scheme goal (the user tapped it in the "Allocates to" list). */
    fun loadSchemeDetails(userId: String, goalId: String, isin: String) {
        viewModelScope.launch {
            platformLog("🔄 loadSchemeDetails called - userId: '$userId', goalId: '$goalId', isin: '$isin'")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                fundDetails = null,
                selectedPeriod = "1Y",
                chartData = emptyList(),
                isPositiveReturn = true,
                bankAccountNumber = null,
                bankIfscCode = null,
                bankName = null
            )
            repository.getSchemeDetails(userId, goalId, isin).collect { result ->
                handleResult(result)
            }
        }
    }

    private fun friendlyError(raw: String?, fallback: String): String {
        return raw?.takeIf { it.isNotBlank() }?.toUserFriendlyErrorMessage() ?: fallback
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
                    error = null,
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
                    error = friendlyError(result.message, "Failed to load fund details. Please try again.")
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

    /** True when the amount screen loaded this goal's range from its basket (see SipAmountScreenV2ViewModel). */
    private suspend fun isBasketGoal(userPurposeId: String): Boolean = try {
        userPurposeId.isNotBlank() && sessionStore.getValue(KeyValueConstants.BASKET_GOAL_ID) == userPurposeId
    } catch (e: Exception) {
        false
    }

    /** So the mandate screen polls the basket endpoint for this mandate. */
    private suspend fun rememberBasketMandate(basketGoal: Boolean, mandateWrapper: MandateWrapper?) {
        val mandateId = mandateWrapper?.mandateId
        if (basketGoal && mandateId != null) {
            sessionStore.saveValue(
                key = KeyValueConstants.BASKET_SIP_MANDATE_ID,
                value = mandateId.toString()
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
        return try {
            val userPurposeId = try {
                sessionStore.getValue(KeyValueConstants.USER_PURPOSE_ID)
            } catch (e: Exception) {
                null
            }
            val effectiveUserInvPurpose = userPurposeId ?: ""

            val request = CreateDailySipRequestDto(
                userId = userId,
                kycAttemptId = kycAttemptId,
                investorId = investorId,
                amount = amount,
                userInvPurpose = effectiveUserInvPurpose
            )

            var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create SIP. Please try again.")

            val basketGoal = isBasketGoal(effectiveUserInvPurpose)
            val flow = if (basketGoal) {
                repository.createBasketSip(
                    CreateBasketSipRequestDto(
                        userId = userId,
                        kycAttemptId = kycAttemptId,
                        investorId = investorId,
                        amount = amount,
                        userInvPurpose = effectiveUserInvPurpose,
                        frequency = "DAILY"
                    )
                )
            } else {
                repository.createDailySip(request)
            }

            flow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isSipCreating = false)
                        try {
                            sessionStore.saveValue("sip_frequency", "daily")
                            sessionStore.saveValue("sip_installment_day", "")
                        } catch (_: Exception) {}
                        val mandateWrapper = resource.data
                        rememberBasketMandate(basketGoal, mandateWrapper)
                        finalResult = SipCreationResult.Success(
                            message = "SIP created successfully!",
                            nextScreen = resource.navigation?.nextScreen,
                            mandateWrapper = mandateWrapper
                        )
                    }
                    is Resource.Error -> {
                        val friendly = friendlyError(resource.message, "Failed to create SIP. Please try again.")
                        _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = friendly)
                        finalResult = SipCreationResult.Failure(friendly)
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSipCreating = true)
                    }
                }
            }
            finalResult
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val friendly = friendlyError(e.message, "Failed to create SIP. Please try again.")
            _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = friendly)
            SipCreationResult.Failure(friendly)
        }
    }

    suspend fun createLumpsumPurchase(
        userId: String,
        amount: Double
    ): SipCreationResult {
        _uiState.value = _uiState.value.copy(isSipCreating = true, sipError = null)
        return try {
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

            var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create purchase. Please try again.")

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
                        val friendly = friendlyError(resource.message, "Failed to create purchase. Please try again.")
                        _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = friendly)
                        finalResult = SipCreationResult.Failure(friendly)
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSipCreating = true)
                    }
                }
            }
            finalResult
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val friendly = friendlyError(e.message, "Failed to create purchase. Please try again.")
            _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = friendly)
            SipCreationResult.Failure(friendly)
        }
    }

    fun clearSipError() {
        _uiState.value = _uiState.value.copy(sipError = null)
    }

    fun loadPastPerformance(userId: String, goalType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(pastPerformanceLoading = true, pastPerformanceError = null)
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
                            pastPerformanceError = friendlyError(resource.message, "Failed to load past performance. Please try again.")
                        )
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(pastPerformanceLoading = true, pastPerformanceError = null)
                    }
                }
            }
        }
    }

    /**
     * Like [loadPastPerformance], for the goal's SIP as a whole: a goal that goes into several schemes gets their backtests blended at
     * the SIP's shares, over the shortest history among them. Lump-sum screens keep [loadPastPerformance] (a lump sum goes into one scheme).
     */
    fun loadGoalPastPerformance(userId: String, goalType: String) {
        viewModelScope.launch {
            platformLog("🔄 loadGoalPastPerformance called - userId: '$userId', goalType: '$goalType'")
            _uiState.value = _uiState.value.copy(pastPerformanceLoading = true, pastPerformanceError = null)
            repository.getGoalPastPerformance(userId, goalType).collect { resource ->
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
                            pastPerformanceError = friendlyError(resource.message, "Failed to load past performance. Please try again.")
                        )
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(pastPerformanceLoading = true, pastPerformanceError = null)
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
        return try {
            val userPurposeId = try {
                sessionStore.getValue(KeyValueConstants.USER_PURPOSE_ID)
            } catch (e: Exception) {
                null
            }
            val effectiveUserInvPurpose = userPurposeId ?: ""

            val request = com.pyllar.consumer.data.remote.model.dto.CreatePurchasePlanRequestDto(
                userId = userId,
                kycAttemptId = kycAttemptId,
                investorId = investorId,
                amount = amount,
                userInvPurpose = effectiveUserInvPurpose,
                frequency = frequency,
                installmentDay = installmentDay,
                numberOfInstallments = numberOfInstallments
            )

            var finalResult: SipCreationResult = SipCreationResult.Failure("Failed to create purchase plan. Please try again.")

            val basketGoal = isBasketGoal(effectiveUserInvPurpose)
            val flow = if (basketGoal) {
                repository.createBasketSip(
                    CreateBasketSipRequestDto(
                        userId = userId,
                        kycAttemptId = kycAttemptId,
                        investorId = investorId,
                        amount = amount,
                        userInvPurpose = effectiveUserInvPurpose,
                        frequency = frequency.uppercase(),
                        installmentDay = installmentDay
                    )
                )
            } else {
                repository.createPurchasePlan(request)
            }

            flow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isSipCreating = false)
                        try {
                            sessionStore.saveValue("sip_amount", amount.toString())
                            sessionStore.saveValue("sip_frequency", "monthly")
                            sessionStore.saveValue("sip_installment_day", installmentDay?.toString() ?: "")
                        } catch (e: Exception) {
                            platformLog("FundDetailsViewModel: ⚠️ Failed to save sip values: ${e.message}")
                        }
                        val mandateWrapper = resource.data
                        rememberBasketMandate(basketGoal, mandateWrapper)
                        finalResult = SipCreationResult.Success(
                            message = "Purchase plan created successfully!",
                            nextScreen = resource.navigation?.nextScreen,
                            mandateWrapper = mandateWrapper
                        )
                    }
                    is Resource.Error -> {
                        val friendly = friendlyError(resource.message, "Failed to create purchase plan. Please try again.")
                        _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = friendly)
                        finalResult = SipCreationResult.Failure(friendly)
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSipCreating = true)
                    }
                }
            }
            finalResult
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val friendly = friendlyError(e.message, "Failed to create purchase plan. Please try again.")
            _uiState.value = _uiState.value.copy(isSipCreating = false, sipError = friendly)
            SipCreationResult.Failure(friendly)
        }
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
        val targetPoints = when (period) {
            "1Y" -> 60
            "3Y" -> 180
            "5Y" -> 300
            else -> 150
        }
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
