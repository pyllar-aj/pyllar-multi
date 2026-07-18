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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
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
    val canWithdraw: Boolean? = true,
    val instantRedemptionValue: Double? = null,
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

    fun toJson(params: SchemeDetailsParams): String {
        return try {
            Json.encodeToString(params)
        } catch (e: Exception) {
            ""
        }
    }

    fun fromJson(json: String?): SchemeDetailsParams? {
        if (json.isNullOrBlank()) return null
        return try {
            Json.decodeFromString<SchemeDetailsParams>(json)
        } catch (e: Exception) {
            null
        }
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
    val firstUnitAllocationDate: String? = null,
    val calculatedFirstUnitAllocationDate: String? = null
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
    val canWithdraw: Boolean = true,
    val redemptionInProgress: Double = 0.0,
    val redeemableAmount: Double = 0.0,
    val instantRedemptionValue: Double? = null,
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

enum class CancelSipReason(val keyword: String, val label: String) {
    AMOUNT_NOT_AVAILABLE("amount_not_available", "Amount not available in bank"),
    INVESTMENT_RETURNS_NOT_AS_EXPECTED("returns_not_as_expected", "Investment returns not as expected"),
    EXIT_LOAD_NOT_AS_EXPECTED("exit_load_not_as_expected", "Exit load not as expected"),
    SWITCH_TO_OTHER_SCHEME("switch_to_other_scheme", "Switching to another scheme"),
    FUND_MANAGER_CHANGED("fund_manager_changed", "Fund manager changed"),
    INVESTMENT_GOAL_COMPLETE("investment_goal_complete", "Investment goal complete"),
    MANDATE_NOT_READY("mandate_not_ready", "Mandate not ready"),
    INVEST_LATER("invest_later", "I'll invest later"),
    CUSTOMER_SUPPORT_NOT_SATISFACTORY("customer_support_not_satisfactory", "Customer support not satisfactory"),
    AMC_SUPPORT_NOT_SATISFACTORY("amc_support_not_satisfactory", "AMC support not satisfactory"),
    OTHER("other", "Other reason")
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

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun loadTransactions(userId: String, purpose: String, schemeParams: SchemeDetailsParams? = null) {
        viewModelScope.launch {
            platformLog("🔄 Loading transactions for userId: $userId, purpose: $purpose")
            _uiState.value = mergeParamsOnError(_uiState.value, schemeParams).copy(isLoading = true, errorMessage = null)

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
                                _uiState.value = mergeParamsOnError(_uiState.value, schemeParams).copy(
                                    isLoading = false,
                                    errorMessage = "No transaction data available"
                                )
                            }
                        }
                        is Resource.Error -> {
                            platformLog("Error loading transactions: ${result.message}")
                            val errorMsg = result.message ?: ""
                            val isNetworkError = result.isNetworkError ||
                                errorMsg.contains("Failed to connect", ignoreCase = true) ||
                                errorMsg.contains("connection", ignoreCase = true) ||
                                errorMsg.contains("Internet", ignoreCase = true) ||
                                errorMsg.contains("timeout", ignoreCase = true)
                            _uiState.value = mergeParamsOnError(_uiState.value, schemeParams).copy(
                                isLoading = false,
                                errorMessage = if (isNetworkError) {
                                    "Unable to connect to server. Please check your internet connection and try again."
                                } else {
                                    errorMsg.ifBlank { "Failed to load transactions" }
                                }
                            )
                        }
                        is Resource.Loading -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("Exception loading transactions: ${e.message}")
                val errorMsg = e.message ?: ""
                val isNetworkError = errorMsg.contains("Failed to connect", ignoreCase = true) ||
                    errorMsg.contains("connection", ignoreCase = true) ||
                    errorMsg.contains("Internet", ignoreCase = true) ||
                    errorMsg.contains("timeout", ignoreCase = true)
                _uiState.value = mergeParamsOnError(_uiState.value, schemeParams).copy(
                    isLoading = false,
                    errorMessage = if (isNetworkError) {
                        "Unable to connect to server. Please check your internet connection and try again."
                    } else {
                        "Failed to load transactions: ${errorMsg.ifBlank { "Unknown error" }}"
                    }
                )
            }
        }
    }

    private fun mergeParamsOnError(state: SchemeDetailsState, schemeParams: SchemeDetailsParams?): SchemeDetailsState {
        if (schemeParams == null) return state
        return state.copy(
            schemeName = schemeParams.schemeName?.takeIf { it.isNotBlank() } ?: state.schemeName,
            goalName = schemeParams.goalName?.takeIf { it.isNotBlank() } ?: state.goalName,
            folioNumber = schemeParams.folioNumber?.takeIf { it.isNotBlank() } ?: state.folioNumber,
            isin = schemeParams.isin?.takeIf { it.isNotBlank() } ?: state.isin,
            unitsInGm = schemeParams.unitsInGm ?: state.unitsInGm,
            category = schemeParams.category?.takeIf { it.isNotBlank() } ?: state.category,
            colorTheme = schemeParams.colorTheme?.takeIf { it.isNotBlank() } ?: state.colorTheme,
            currentValue = schemeParams.currentValue,
            investmentInProgress = schemeParams.investmentInProgress,
            investedAmount = schemeParams.investedAmount,
            totalValue = schemeParams.currentValue,
            cummulativeValue = schemeParams.currentValue + schemeParams.investmentInProgress,
            totalGain = schemeParams.profit,
            withdrawnGain = schemeParams.realizedProfit,
            availableGain = schemeParams.unrealizedProfit,
            canWithdraw = schemeParams.canWithdraw ?: state.canWithdraw,
            redemptionInProgress = schemeParams.redemptionInProgress,
            redeemableAmount = schemeParams.redeemableAmount,
            instantRedemptionValue = schemeParams.instantRedemptionValue
        )
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
            val createdDate = planSummary.mandateCreatedDate ?: planSummary.mandateApprovedDate
            val calculatedAllocationDate = calculateFirstUnitAllocationDate(createdDate)
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
                firstUnitAllocationDate = planSummary.firstUnitAllocationDate,
                calculatedFirstUnitAllocationDate = calculatedAllocationDate
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
            canWithdraw = schemeParams?.canWithdraw ?: true,
            redeemableAmount = schemeParams?.redeemableAmount ?: 0.0,
            redemptionInProgress = schemeParams?.redemptionInProgress ?: 0.0,
            instantRedemptionValue = schemeParams?.instantRedemptionValue,
            transactions = displayTransactions,
            mandates = displayMandates,
            isLoading = false,
            errorMessage = null
        )
    }

    fun cancelSip(userId: String, planId: String?, mandateId: Long?, reason: String?) {
        viewModelScope.launch {
            if (mandateId == null) {
                _cancelSipResult.value = CancelSipResult.Error("SIP cancellation failed: mandateId is null")
                return@launch
            }
            _cancelSipLoading.value = true
            try {
                platformLog("🛑 Cancelling SIP for user: $userId, planId: $planId, mandateId: $mandateId")
                val request = com.pyllar.consumer.data.remote.requests.SipActionRequest(
                    userId = userId,
                    planId = planId,
                    mandateId = mandateId,
                    action = "CANCEL",
                    reason = reason ?: "User Request"
                )
                dashboardRepository.sipAction(request).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val actionId = result.data
                            if (!actionId.isNullOrBlank()) {
                                platformLog("🔄 Starting polling for actionId: $actionId")
                                startPollingActionStatus(userId, actionId, "CANCEL")
                            } else {
                                platformLog("✅ SIP cancelled (no actionId)")
                                _cancelSipLoading.value = false
                                _cancelSipResult.value = CancelSipResult.Success
                            }
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ SIP cancellation failed: ${result.message}")
                            _cancelSipLoading.value = false
                            _cancelSipResult.value = CancelSipResult.Error(result.message ?: "Failed to cancel SIP")
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception cancelling SIP: ${e.message}")
                _cancelSipLoading.value = false
                _cancelSipResult.value = CancelSipResult.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun pauseSip(userId: String, planId: String?, mandateId: Long? = null) {
        viewModelScope.launch {
            if (mandateId == null) {
                _pauseSipResult.value = PauseSipResult.Error("SIP pause failed: mandateId is null")
                return@launch
            }
            _pauseSipLoading.value = true
            try {
                platformLog("⏸️ Pausing SIP for user: $userId, planId: $planId, mandateId: $mandateId")
                val request = com.pyllar.consumer.data.remote.requests.SipActionRequest(
                    userId = userId,
                    planId = planId,
                    mandateId = mandateId,
                    action = "PAUSE",
                    reason = "pause"
                )
                dashboardRepository.sipAction(request).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val actionId = result.data
                            if (!actionId.isNullOrBlank()) {
                                platformLog("🔄 Starting polling for actionId: $actionId")
                                startPollingActionStatus(userId, actionId, "PAUSE")
                            } else {
                                platformLog("✅ SIP paused (no actionId)")
                                _pauseSipLoading.value = false
                                _pauseSipResult.value = PauseSipResult.Success
                            }
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ SIP pause failed: ${result.message}")
                            _pauseSipLoading.value = false
                            _pauseSipResult.value = PauseSipResult.Error(result.message ?: "Failed to pause SIP")
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception pausing SIP: ${e.message}")
                _pauseSipLoading.value = false
                _pauseSipResult.value = PauseSipResult.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resumeSip(userId: String, planId: String?, mandateId: Long?) {
        viewModelScope.launch {
            if (mandateId == null) {
                _resumeSipResult.value = ResumeSipResult.Error("SIP resume failed: mandateId is null")
                return@launch
            }
            _resumeSipLoading.value = true
            try {
                platformLog("▶️ Resuming SIP for user: $userId, planId: $planId, mandateId: $mandateId")
                val request = com.pyllar.consumer.data.remote.requests.SipActionRequest(
                    userId = userId,
                    planId = planId,
                    mandateId = mandateId,
                    action = "RESUME",
                    reason = "resume"
                )
                dashboardRepository.sipAction(request).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val actionId = result.data
                            if (!actionId.isNullOrBlank()) {
                                platformLog("🔄 Starting polling for actionId: $actionId")
                                startPollingActionStatus(userId, actionId, "RESUME")
                            } else {
                                platformLog("✅ SIP resumed (no actionId)")
                                _resumeSipLoading.value = false
                                _resumeSipResult.value = ResumeSipResult.Success
                            }
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ SIP resume failed: ${result.message}")
                            _resumeSipLoading.value = false
                            _resumeSipResult.value = ResumeSipResult.Error(result.message ?: "Failed to resume SIP")
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception resuming SIP: ${e.message}")
                _resumeSipLoading.value = false
                _resumeSipResult.value = ResumeSipResult.Error(e.message ?: "Something went wrong")
            }
        }
    }

    private suspend fun startPollingActionStatus(userId: String, actionId: String, action: String) {
        val startTime = platformTime()
        val timeoutMillis = 3 * 60 * 1000L // 3 minutes
        
        while (platformTime() - startTime < timeoutMillis) {
            try {
                val pollRequest = com.pyllar.consumer.data.remote.requests.ActionPollRequest(
                    userId = userId,
                    actionId = actionId,
                    action = action
                )
                
                platformLog("📡 Polling status for $action...")
                var shouldContinue = true
                dashboardRepository.pollActionStatus(pollRequest).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val navAction = result.navigation?.action
                            platformLog("📥 Poll response action: $navAction")
                            
                            if (navAction == com.pyllar.consumer.data.remote.model.dto.NavigationAction.STAY || navAction == null) {
                                platformLog("✅ Polling finished with STAY for $action")
                                finalizePollingInternal(action, true)
                                shouldContinue = false
                            } else if (navAction == com.pyllar.consumer.data.remote.model.dto.NavigationAction.POLL) {
                                // Continue polling
                            } else {
                                platformLog("⚠️ Unexpected action during poll: $navAction")
                                finalizePollingInternal(action, true)
                                shouldContinue = false
                            }
                        }
                        is Resource.Error<*> -> {
                            platformLog("❌ Poll API error: ${result.message}")
                            finalizePollingInternal(action, true)
                            shouldContinue = false
                        }
                        is Resource.Loading<*> -> { }
                    }
                }
                if (!shouldContinue) return
                kotlinx.coroutines.delay(2000)
            } catch (e: Exception) {
                platformLog("❌ Exception during polling: ${e.message}")
                finalizePollingInternal(action, true)
                return
            }
        }
        platformLog("⏱ Polling timed out for $action")
        finalizePollingInternal(action, true)
    }

    private fun finalizePollingInternal(action: String, isSuccess: Boolean) {
        when (action) {
            "PAUSE" -> {
                _pauseSipLoading.value = false
                _pauseSipResult.value = if (isSuccess) PauseSipResult.Success else PauseSipResult.Error("SIP pause failed")
            }
            "RESUME" -> {
                _resumeSipLoading.value = false
                _resumeSipResult.value = if (isSuccess) ResumeSipResult.Success else ResumeSipResult.Error("SIP resume failed")
            }
            "CANCEL" -> {
                _cancelSipLoading.value = false
                _cancelSipResult.value = if (isSuccess) CancelSipResult.Success else CancelSipResult.Error("SIP cancellation failed")
            }
        }
    }

    // Helper for time in KMP (can be overridden or use expect/actual if needed, but for now simple)
    private fun platformTime(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    fun clearCancelSipResult() { _cancelSipResult.value = null }
    fun clearPauseSipResult() { _pauseSipResult.value = null }
    fun clearResumeSipResult() { _resumeSipResult.value = null }

    private fun formatDate(dateString: String?): String? {
        if (dateString.isNullOrBlank()) return null
        return dateString.split("T").firstOrNull() ?: dateString
    }

    private fun calculateFirstUnitAllocationDate(createdDateStr: String?): String? {
        if (createdDateStr.isNullOrBlank() || createdDateStr == "null") return null
        return try {
            val datePart = createdDateStr.substringBefore("T")
            val localDate = LocalDate.parse(datePart)
            val daysToAdd = when (localDate.dayOfWeek) {
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.SUNDAY -> 4
                DayOfWeek.SATURDAY -> 5
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY -> 6
                else -> 4
            }
            val allocationDate = localDate.plus(daysToAdd, DateTimeUnit.DAY)
            allocationDate.toString()
        } catch (e: Exception) {
            null
        }
    }
}
