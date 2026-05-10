package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.TransactionDetailsResponseDto
import com.pyllar.consumer.data.remote.requests.TransactionDetailsRequest
import com.pyllar.consumer.domain.repository.DashboardRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SchemeDetailsParams(
    val isin: String?,
    val folioNumber: String?,
    val schemeName: String?,
    val currentValue: Double,
    val investmentInProgress: Double,
    val investedAmount: Double = 0.0,
    val selectedTab: Int = 0, // 0 = Transactions, 1 = Mandates
    val goalName: String? = null,
    val unitsInGm: Double? = null,
    val category: String? = null,
    val colorTheme: String? = null,
    val profit: Double = 0.0,
    val realizedProfit: Double = 0.0,
    val unrealizedProfit: Double = 0.0,
    val redeemableAmount: Double = 0.0,
    val redemptionInProgress: Double = 0.0,
    val userPurposeId: String? = null
)

object SchemeDetailsParamsManager {
    private var params: SchemeDetailsParams? = null

    fun set(params: SchemeDetailsParams) {
        platformLog("📝 Setting params: isin=${params.isin}, folio=${params.folioNumber}, schemeName=${params.schemeName}")
        this.params = params
    }

    fun get(): SchemeDetailsParams? {
        platformLog("📖 Getting params: ${if (params != null) "FOUND" else "NULL"}")
        return params
    }

    fun clear() {
        platformLog("🗑️ Clearing params")
        params = null
    }
}

data class TransactionDisplayItem(
    val transactionId: String?,
    val transactionType: String?,
    val amount: Double,
    val date: String?,
    val state: String,
    val isCredit: Boolean,
    val allottedUnits: Double,
    val sortDate: String?
)

data class MandateDisplayItem(
    val mandateId: Long?,
    val amount: Double,
    val nextSipDate: String?,
    val status: String?,
    val frequency: String?,
    val planId: String?,
    val mandateApprovedDate: String?,
    val mandateCancelledDate: String?,
    val mandateCreatedDate: String? = null,
    val firstUnitAllocationDate: String? = null
)

data class SchemeDetailsState(
    val schemeName: String? = null,
    val goalName: String? = null,
    val unitsInGm: Double? = null,
    val category: String? = null,
    val colorTheme: String? = null,
    val folioNumber: String? = null,
    val isin: String? = null,
    val currentValue: Double = 0.0,
    val investmentInProgress: Double = 0.0,
    val investedAmount: Double = 0.0,
    val totalUnitsAllotted: Double = 0.0,
    val totalValue: Double = 0.0,
    val cummulativeValue: Double = 0.0, // currentValue + investmentInProgress
    val totalGain: Double = 0.0,
    val withdrawnGain: Double = 0.0,
    val availableGain: Double = 0.0,
    val redeemableAmount: Double = 0.0,
    val redemptionInProgress: Double = 0.0,
    val transactions: List<TransactionDisplayItem> = emptyList(),
    val mandates: List<MandateDisplayItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed class CancelSipResult {
    object Success : CancelSipResult()
    data class Error(val message: String) : CancelSipResult()
}

sealed class PauseSipResult {
    object Success : PauseSipResult()
    data class Error(val message: String) : PauseSipResult()
}

sealed class ResumeSipResult {
    object Success : ResumeSipResult()
    data class Error(val message: String) : ResumeSipResult()
}

class SchemeDetailsViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchemeDetailsState())
    val uiState: StateFlow<SchemeDetailsState> = _uiState.asStateFlow()

    private val _cancelSipResult = MutableStateFlow<CancelSipResult?>(null)
    val cancelSipResult: StateFlow<CancelSipResult?> = _cancelSipResult.asStateFlow()

    private val _pauseSipResult = MutableStateFlow<PauseSipResult?>(null)
    val pauseSipResult: StateFlow<PauseSipResult?> = _pauseSipResult.asStateFlow()

    private val _resumeSipResult = MutableStateFlow<ResumeSipResult?>(null)
    val resumeSipResult: StateFlow<ResumeSipResult?> = _resumeSipResult.asStateFlow()

    private val _cancelSipLoading = MutableStateFlow(false)
    val cancelSipLoading: StateFlow<Boolean> = _cancelSipLoading.asStateFlow()

    private val _pauseSipLoading = MutableStateFlow(false)
    val pauseSipLoading: StateFlow<Boolean> = _pauseSipLoading.asStateFlow()

    private val _resumeSipLoading = MutableStateFlow(false)
    val resumeSipLoading: StateFlow<Boolean> = _resumeSipLoading.asStateFlow()

    fun clearState() {
        platformLog("🧹 Clearing SchemeDetailsViewModel state")
        _uiState.value = SchemeDetailsState()
    }

    fun loadTransactions(userId: String, purpose: String, schemeParams: SchemeDetailsParams? = null) {
        viewModelScope.launch {
            platformLog("🔄 Loading transactions for userId: $userId, purpose: $purpose")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val request = TransactionDetailsRequest(
                    userId = userId,
                    userInvestmentPurposeId = purpose
                )

                dashboardRepository.getTransactions(request).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val response = result.data
                            if (response != null) {
                                val state = mapResponseToState(response, schemeParams)
                                _uiState.value = state.copy(isLoading = false)
                                platformLog("✅ Transactions loaded successfully")
                            } else {
                                platformLog("⚠️ Response data is null")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "No transaction data available"
                                )
                            }
                        }
                        is Resource.Error -> {
                            platformLog("Error loading transactions: ${result.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Failed to load transactions"
                            )
                        }
                        is Resource.Loading -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("Exception loading transactions: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }

    private fun mapResponseToState(response: TransactionDetailsResponseDto, schemeParams: SchemeDetailsParams? = null): SchemeDetailsState {
        val purchasePlans = response.purchasePlans.orEmpty()
        val firstPlan = purchasePlans.firstOrNull()?.purchasePlan
        val schemeName = schemeParams?.schemeName?.takeIf { it.isNotBlank() } 
            ?: firstPlan?.scheme?.takeIf { it.isNotBlank() } 
            ?: "Unknown Scheme"
        val folioNumber = schemeParams?.folioNumber ?: firstPlan?.folioNumber
        val isin = schemeParams?.isin
        
        val currentValue = schemeParams?.currentValue ?: 0.0
        val investmentInProgress = schemeParams?.investmentInProgress ?: 0.0
        val investedAmount = schemeParams?.investedAmount ?: 0.0
        
        val allTransactions = purchasePlans.flatMap { planWithTx ->
            planWithTx.transactions.orEmpty()
        }
        
        var calculatedCurrentValue = currentValue
        var calculatedInvestmentInProgress = investmentInProgress
        var calculatedInvestedAmount = investedAmount
        
        val totalUnitsAllotted = purchasePlans.sumOf { planWithTx ->
            planWithTx.unitsAllotted?.toDouble() ?: 0.0
        }
        
        var totalValue = calculatedCurrentValue
        
        if (schemeParams == null) {
            val firstPlanWithTx = purchasePlans.firstOrNull()
            if (firstPlanWithTx?.investedAmount != null) {
                calculatedInvestedAmount = firstPlanWithTx.investedAmount.toDouble()
            }
            if (firstPlanWithTx?.totalValue != null) {
                totalValue = firstPlanWithTx.totalValue.toDouble()
                calculatedCurrentValue = totalValue
            } else {
                allTransactions.forEach { tx ->
                    val amount = tx.amount?.toDouble() ?: 0.0
                    when (tx.state?.uppercase()) {
                        "SUCCESSFUL", "COMPLETED", "SUCCESS" -> {
                            calculatedCurrentValue += amount
                            calculatedInvestedAmount += amount
                        }
                        "SUBMITTED", "PENDING", "IN_PROGRESS" -> {
                            calculatedInvestmentInProgress += amount
                        }
                    }
                }
                totalValue = calculatedCurrentValue
            }
        } else {
            calculatedInvestedAmount = investedAmount
            calculatedCurrentValue = currentValue
            totalValue = currentValue
        }
        
        val displayTransactions = purchasePlans.flatMap { planWithTx ->
            planWithTx.transactions.orEmpty().map { tx ->
                val amount = tx.amount?.toDouble() ?: 0.0
                val transactionTypeUpper = tx.transactionType?.uppercase() ?: ""
                val isCredit = when {
                    transactionTypeUpper == "PURCHASE" -> true
                    transactionTypeUpper == "REDEMPTION" -> false
                    else -> true
                }
                
                val mappedState = when {
                    tx.state != null -> {
                        when (tx.state.uppercase()) {
                            "SUCCESSFUL", "COMPLETED", "SUCCESS" -> "SUCCESS"
                            "SUBMITTED", "CONFIRMED", "PENDING", "IN_PROGRESS" -> "SUBMITTED"
                            "FAILED", "CANCELLED", "REVERSED" -> "FAILED"
                            else -> tx.state.uppercase()
                        }
                    }
                    !isCredit -> "FAILED"
                    else -> "UNKNOWN"
                }
                
                val originalDateString = tx.scheduledOn ?: tx.tradedOn
                val finalDate = if (!isCredit && originalDateString == null) {
                    null 
                } else {
                    formatDate(originalDateString)
                }
                
                val units = tx.units?.toDouble() ?: tx.allottedUnits?.toDouble() ?: 0.0
                
                TransactionDisplayItem(
                    transactionId = tx.transactionId,
                    transactionType = tx.transactionType,
                    amount = kotlin.math.abs(amount),
                    date = finalDate,
                    state = mappedState,
                    isCredit = isCredit,
                    allottedUnits = units,
                    sortDate = originalDateString
                )
            }
        }.sortedByDescending { transaction ->
            transaction.sortDate ?: ""
        }
        
        val displayMandates = response.planSummaryDtos?.map { planSummary ->
            MandateDisplayItem(
                mandateId = planSummary.mandateId,
                amount = planSummary.amount?.toDouble() ?: 0.0,
                nextSipDate = planSummary.nextSipDate,
                status = planSummary.status,
                frequency = planSummary.frequency,
                planId = planSummary.planId,
                mandateApprovedDate = planSummary.mandateApprovedDate,
                mandateCancelledDate = planSummary.mandateCancelledDate,
                mandateCreatedDate = planSummary.mandateCreatedDate,
                firstUnitAllocationDate = planSummary.firstUnitAllocationDate
            )
        }?.sortedByDescending { it.mandateCreatedDate ?: "" } ?: emptyList()
        
        val cummulativeValue = calculatedCurrentValue + calculatedInvestmentInProgress
        
        return SchemeDetailsState(
            schemeName = schemeName,
            goalName = schemeParams?.goalName,
            unitsInGm = schemeParams?.unitsInGm,
            category = schemeParams?.category,
            colorTheme = schemeParams?.colorTheme,
            folioNumber = folioNumber,
            isin = isin,
            currentValue = calculatedCurrentValue,
            investmentInProgress = calculatedInvestmentInProgress,
            investedAmount = calculatedInvestedAmount,
            totalUnitsAllotted = totalUnitsAllotted,
            totalValue = totalValue,
            cummulativeValue = cummulativeValue,
            totalGain = schemeParams?.profit ?: 0.0,
            withdrawnGain = schemeParams?.realizedProfit ?: 0.0,
            availableGain = schemeParams?.unrealizedProfit ?: 0.0,
            redeemableAmount = schemeParams?.redeemableAmount ?: 0.0,
            redemptionInProgress = schemeParams?.redemptionInProgress ?: 0.0,
            transactions = displayTransactions,
            mandates = displayMandates,
            isLoading = false,
            errorMessage = null
        )
    }

    fun cancelSip(userId: String, planId: String?, mandateId: Long?, reason: String?) {
        viewModelScope.launch {
            _cancelSipLoading.value = true
            try {
                platformLog("🛑 Cancelling SIP for user: $userId, planId: $planId, mandateId: $mandateId")
                dashboardRepository.cancelSip(userId, planId, mandateId, reason).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            platformLog("✅ SIP cancelled successfully")
                            _cancelSipResult.value = CancelSipResult.Success
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ SIP cancellation failed: ${result.message}")
                            _cancelSipResult.value = CancelSipResult.Error(result.message ?: "Failed to cancel SIP")
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception cancelling SIP: ${e.message}")
                _cancelSipResult.value = CancelSipResult.Error(e.message ?: "Something went wrong")
            } finally {
                _cancelSipLoading.value = false
            }
        }
    }

    fun pauseSip(userId: String, planId: String?, mandateId: Long? = null) {
        viewModelScope.launch {
            _pauseSipLoading.value = true
            try {
                platformLog("⏸️ Pausing SIP for user: $userId, planId: $planId")
                dashboardRepository.pauseSip(userId, planId, mandateId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            platformLog("✅ SIP paused successfully")
                            _pauseSipResult.value = PauseSipResult.Success
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ SIP pause failed: ${result.message}")
                            _pauseSipResult.value = PauseSipResult.Error(result.message ?: "Failed to pause SIP")
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception pausing SIP: ${e.message}")
                _pauseSipResult.value = PauseSipResult.Error(e.message ?: "Something went wrong")
            } finally {
                _pauseSipLoading.value = false
            }
        }
    }

    fun resumeSip(userId: String, planId: String?, mandateId: Long?) {
        viewModelScope.launch {
            _resumeSipLoading.value = true
            try {
                platformLog("▶️ Resuming SIP for user: $userId, planId: $planId, mandateId: $mandateId")
                dashboardRepository.resumeSip(userId, planId, mandateId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            platformLog("✅ SIP resumed successfully")
                            _resumeSipResult.value = ResumeSipResult.Success
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ SIP resume failed: ${result.message}")
                            _resumeSipResult.value = ResumeSipResult.Error(result.message ?: "Failed to resume SIP")
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception resuming SIP: ${e.message}")
                _resumeSipResult.value = ResumeSipResult.Error(e.message ?: "Something went wrong")
            } finally {
                _resumeSipLoading.value = false
            }
        }
    }

    fun clearCancelSipResult() { _cancelSipResult.value = null }
    fun clearPauseSipResult() { _pauseSipResult.value = null }
    fun clearResumeSipResult() { _resumeSipResult.value = null }

    // A simplified format date since KMP doesn't easily support full SimpleDateFormat 
    // without kotlinx.datetime. Using original format for KMP right now, or basic splitting.
    private fun formatDate(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null
        return dateString.split("T").firstOrNull() ?: dateString
    }
}
