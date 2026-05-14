package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.*
import com.pyllar.consumer.data.remote.requests.GoalSelectionRequest
import com.pyllar.consumer.data.remote.requests.TransactionDetailsRequest
import com.pyllar.consumer.domain.repository.DashboardRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class WithdrawInitParams(
    val amount: Double,
    val investmentInProgress: Double = 0.0,
    val isin: String,
    val folio: String?,
    val bankAccountNumber: String = "",
    val bankAccountIfscCode: String = "",
    val schemeName: String? = null,
    val canWithdraw: Boolean? = true,
    val redemptionInProgress: Double = 0.0,
    val redeemableAmount: Double = 0.0,
    val instantRedemptionValue: Double? = null
)

enum class WithdrawMode {
    REGULAR, INSTANT
}

data class WithdrawState(
    val currentBalance: Double = 0.0,
    val investmentInProgress: Double = 0.0,
    val withdrawalInProgress: Double = 0.0,
    val availableToWithdraw: Double = 0.0,
    val schemes: List<WithdrawScheme> = emptyList(),
    val selectedSchemeId: String? = null,
    val selectedWithdrawMode: WithdrawMode = WithdrawMode.REGULAR,
    val isInstantAvailable: Boolean = false,
    val instantRedemptionValue: Double? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true
)

data class WithdrawScheme(
    val id: String,
    val schemeName: String,
    val folioNo: String?,
    val isin: String,
    val investedAmount: Double,
    val currentValue: Double,
    val canWithdraw: Boolean,
    val redeemableAmount: Double = 0.0,
    val redemptionInProgress: Double = 0.0,
    val instantRedemptionValue: Double? = null
)

object WithdrawParamsManager {
    private var params: WithdrawInitParams? = null
    fun set(p: WithdrawInitParams) { params = p }
    fun get(): WithdrawInitParams? = params
    fun clear() { params = null }

    fun toJson(params: WithdrawInitParams): String {
        return try {
            Json.encodeToString(params)
        } catch (e: Exception) {
            ""
        }
    }

    fun fromJson(json: String?): WithdrawInitParams? {
        if (json.isNullOrBlank()) return null
        return try {
            Json.decodeFromString<WithdrawInitParams>(json)
        } catch (e: Exception) {
            null
        }
    }
}

object WithdrawSchemeManager {
    private var scheme: WithdrawScheme? = null
    private var mode: String? = null
    fun set(s: WithdrawScheme) { scheme = s }
    fun get(): WithdrawScheme? = scheme
    fun setMode(m: String?) { mode = m }
    fun getMode(): String? = mode
    fun clear() { 
        scheme = null 
        mode = null
    }
}

class WithdrawViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _withdrawState = MutableStateFlow(WithdrawState())
    val withdrawState: StateFlow<WithdrawState> = _withdrawState.asStateFlow()

    fun loadWithdrawData(userId: String, selectedGoal: InvestmentGoal? = null) {
        viewModelScope.launch {
            platformLog("Loading withdraw data for userId: $userId")
            _withdrawState.value = _withdrawState.value.copy(isLoading = true)

            try {
                dashboardRepository.getDashboardV2(userId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val response = result.data
                            if (response != null) {
                                val transactions = fetchAllTransactions(userId, response.currentInvestments.orEmpty())
                                val withdrawalInProgress = 0.0
                                val state = mapResponseToWithdrawState(response, transactions, selectedGoal)
                                val adjustedAvailableToWithdraw = (state.availableToWithdraw - withdrawalInProgress)
                                    .let { if (it > 0) it else 0.0 }
                                
                                _withdrawState.value = state.copy(
                                    withdrawalInProgress = withdrawalInProgress,
                                    availableToWithdraw = adjustedAvailableToWithdraw,
                                    isLoading = false
                                )
                            } else {
                                _withdrawState.value = _withdrawState.value.copy(isLoading = false)
                            }
                        }
                        is Resource.Error -> {
                            platformLog("Error loading withdraw data: ${result.message}")
                            _withdrawState.value = _withdrawState.value.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Error loading data"
                            )
                        }
                        is Resource.Loading -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("Exception loading withdraw data: ${e.message}")
                _withdrawState.value = _withdrawState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadWithdrawDataWithParams(userId: String, params: WithdrawInitParams?) {
        viewModelScope.launch {
            try {
                _withdrawState.value = _withdrawState.value.copy(isLoading = true)

                val currentValue = params?.amount ?: 0.0
                val investmentInProgress = params?.investmentInProgress ?: 0.0
                val withdrawalInProgress = params?.redemptionInProgress ?: 0.0
                val availableToWithdraw = ((params?.redeemableAmount ?: 0.0) - withdrawalInProgress).coerceAtLeast(0.0)

                val state = WithdrawState(
                    currentBalance = currentValue,
                    investmentInProgress = investmentInProgress,
                    withdrawalInProgress = withdrawalInProgress,
                    availableToWithdraw = availableToWithdraw,
                    schemes = listOf(
                        WithdrawScheme(
                            id = "default",
                            schemeName = params?.schemeName ?: "Current Investment",
                            folioNo = params?.folio,
                            isin = params?.isin ?: "",
                            investedAmount = currentValue,
                            currentValue = currentValue,
                            canWithdraw = true,
                            redemptionInProgress = withdrawalInProgress,
                            redeemableAmount = params?.redeemableAmount ?: 0.0,
                            instantRedemptionValue = params?.instantRedemptionValue
                        )
                    ),
                    isLoading = false,
                    selectedSchemeId = "default",
                    isInstantAvailable = (params?.instantRedemptionValue ?: 0.0) > 0.0,
                    instantRedemptionValue = params?.instantRedemptionValue,
                    selectedWithdrawMode = if ((params?.instantRedemptionValue ?: 0.0) > 0.0) WithdrawMode.INSTANT else WithdrawMode.REGULAR
                )
                platformLog("WithdrawViewModel: 📊 State from params. isInstantAvailable=${state.isInstantAvailable}, value=${state.instantRedemptionValue}")


                _withdrawState.value = state
            } catch (e: Exception) {
                platformLog("❌ Error loading withdraw data")
                _withdrawState.value = _withdrawState.value.copy(isLoading = false)
            }
        }
    }

    fun selectScheme(schemeId: String) {
        _withdrawState.value = _withdrawState.value.copy(
            selectedSchemeId = schemeId
        )
    }

    fun selectWithdrawMode(mode: WithdrawMode) {
        _withdrawState.value = _withdrawState.value.copy(selectedWithdrawMode = mode)
    }

    fun clearErrorMessage() {
        _withdrawState.value = _withdrawState.value.copy(errorMessage = null)
    }

    private suspend fun fetchAllTransactions(
        userId: String,
        currentInvestments: List<CurrentInvestmentDto>
    ): List<RecentTransactionDto> {
        val allTransactions = mutableListOf<RecentTransactionDto>()
        val purposeToIdMap = mutableMapOf<String, String>()

        for (investment in currentInvestments) {
            val purpose = investment.purpose ?: continue
            if (purposeToIdMap.containsKey(purpose)) continue

            try {
                var localUserPurposeId: String? = null
                dashboardRepository.initGoalTxn(GoalSelectionRequest(userId = userId, goal = purpose))
                    .collectLatest { result ->
                        if (result is Resource.Success) {
                            localUserPurposeId = result.data?.userPurposeId
                        }
                    }

                localUserPurposeId?.let { userPurposeId ->
                    if (userPurposeId.isNotBlank()) {
                        purposeToIdMap[purpose] = userPurposeId
                    }
                }
            } catch (e: Exception) {
                platformLog("⚠️ Exception getting userInvestmentPurposeId for purpose '$purpose': ${e.message}")
            }
        }

        if (purposeToIdMap.isNotEmpty()) {
            coroutineScope {
                val transactionResults = purposeToIdMap.values.map { userPurposeId ->
                    async {
                        var transactionsForPurpose = emptyList<RecentTransactionDto>()
                        try {
                            dashboardRepository.getTransactions(
                                TransactionDetailsRequest(
                                    userId = userId,
                                    userInvestmentPurposeId = userPurposeId
                                )
                            ).collectLatest { txResult ->
                                if (txResult is Resource.Success) {
                                    transactionsForPurpose = txResult.data?.purchasePlans?.flatMap { planWithTx ->
                                        planWithTx.transactions.orEmpty().mapNotNull { purchaseTx ->
                                            convertPurchaseTransactionToRecentTransaction(purchaseTx, planWithTx.purchasePlan?.scheme)
                                        }
                                    } ?: emptyList()
                                }
                            }
                        } catch (e: Exception) {
                            platformLog("⚠️ Exception fetching transactions for userPurposeId '$userPurposeId': ${e.message}")
                        }
                        transactionsForPurpose
                    }
                }

                val results = awaitAll(*transactionResults.toTypedArray())
                results.forEach { transactions ->
                    allTransactions.addAll(transactions)
                }
            }
        }
        
        return allTransactions
    }
    
    private fun convertPurchaseTransactionToRecentTransaction(
        purchaseTx: PurchaseTransactionDto,
        schemeName: String?
    ): RecentTransactionDto {
        val transactionDate = purchaseTx.tradedOn ?: purchaseTx.scheduledOn
        
        val status = when (purchaseTx.state?.uppercase()) {
            "SUCCESSFUL", "SUCCESS", "COMPLETED" -> "COMPLETED"
            "SUBMITTED", "PENDING", "IN_PROGRESS" -> "PENDING"
            "FAILED", "CANCELLED" -> "FAILED"
            else -> purchaseTx.state?.uppercase() ?: "PENDING"
        }
        
        return RecentTransactionDto(
            transactionId = purchaseTx.transactionId,
            fundId = null,
            fundName = schemeName,
            transactionType = purchaseTx.transactionType,
            amount = purchaseTx.amount,
            units = purchaseTx.units ?: purchaseTx.allottedUnits,
            nav = purchaseTx.purchasedPrice,
            transactionDate = transactionDate,
            status = status,
            remarks = null
        )
    }

    private fun mapResponseToWithdrawState(
        response: InvestorDashboardResponseV2Dto,
        transactions: List<RecentTransactionDto>,
        selectedGoal: InvestmentGoal? = null
    ): WithdrawState {
        val investments = response.currentInvestments.orEmpty()

        val successfulCredits = transactions.filter {
            it.transactionType?.equals("PURCHASE", ignoreCase = true) == true &&
                    it.status?.equals("COMPLETED", ignoreCase = true) == true
        }

        val totalInvestedFromTransactions = successfulCredits.sumOf { it.amount ?: 0.0 }
        val totalInvestedFromResponse = investments.sumOf { it.investedAmount ?: 0.0 }
        val totalValue = investments.sumOf { it.currentValue ?: 0.0 }
        val withdrawalInProgress = investments.sumOf { it.redemptionInProgress ?: 0.0 }
        val availableToWithdraw = investments.sumOf { (it.redeemableAmount ?: 0.0) - (it.redemptionInProgress ?: 0.0) }.coerceAtLeast(0.0)

        val investmentInProgress = investments.sumOf { it.amountUnderProcessing ?: 0.0 }

        val schemes = investments.flatMap { investment ->
            val canWithdraw = investment.canWithdraw ?: true
            val folioSchemes = investment.folioDetails.orEmpty().map { folio ->
                val investedAmountFromTransactions = successfulCredits
                    .filter { tx -> tx.fundName?.equals(folio.fundName, ignoreCase = true) == true }
                    .sumOf { it.amount ?: 0.0 }

                val investedAmount = when {
                    investedAmountFromTransactions > 0 -> investedAmountFromTransactions
                    folio.investmentAmount != null -> folio.investmentAmount
                    investment.investedAmount != null -> investment.investedAmount
                    else -> 0.0
                } ?: 0.0

                val currentValue = folio.currentValue ?: investment.currentValue ?: investedAmount ?: 0.0

                WithdrawScheme(
                    id = folio.isin ?: "${investment.purpose}-${folio.folioNumber ?: folio.fundName}",
                    schemeName = folio.fundName ?: (investment.purpose ?: "Investment"),
                    folioNo = folio.folioNumber,
                    isin = folio.isin ?: "",
                    investedAmount = investedAmount,
                    currentValue = currentValue,
                    canWithdraw = canWithdraw,
                    redemptionInProgress = investment.redemptionInProgress ?: 0.0,
                    redeemableAmount = investment.redeemableAmount ?: 0.0,
                    instantRedemptionValue = investment.instantRedemptionValue
                )
            }

            if (folioSchemes.isNotEmpty()) {
                folioSchemes
            } else {
                val investedAmountFallback = investment.investedAmount ?: 0.0
                val currentValueFallback = investment.currentValue ?: investedAmountFallback
                if (investedAmountFallback > 0 || currentValueFallback > 0) {
                    val displayName = investment.purpose
                        ?.replace("_", " ")
                        ?.split(" ")
                        ?.joinToString(" ") { word ->
                            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        } ?: "Investment"

                    listOf(
                        WithdrawScheme(
                            id = investment.purpose ?: "investment-${investment.hashCode()}",
                            schemeName = displayName,
                            folioNo = null,
                            isin = "",
                            investedAmount = investedAmountFallback,
                            currentValue = currentValueFallback,
                            canWithdraw = canWithdraw,
                            redemptionInProgress = investment.redemptionInProgress ?: 0.0,
                            redeemableAmount = investment.redeemableAmount ?: 0.0,
                            instantRedemptionValue = investment.instantRedemptionValue
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }.filter { it.investedAmount > 0 || it.currentValue > 0 }

        val filteredSchemes = if (selectedGoal != null) {
            schemes.filter { scheme ->
                scheme.schemeName == selectedGoal.name ||
                        scheme.folioNo == selectedGoal.folioNo
            }
        } else {
            schemes
        }

        val instantRedemptionValue = if (selectedGoal != null) {
            selectedGoal.instantRedemptionValue ?: 0.0
        } else {
            investments.sumOf { it.instantRedemptionValue ?: 0.0 }
        }
        val isInstantAvailable = instantRedemptionValue > 0.0

        return WithdrawState(
            currentBalance = if (selectedGoal != null) selectedGoal.currentValue else totalValue,
            investmentInProgress = if (selectedGoal != null) 0.0 else investmentInProgress,
            withdrawalInProgress = withdrawalInProgress,
            availableToWithdraw = if (selectedGoal != null) (selectedGoal.redeemableAmount - selectedGoal.redemptionInProgress).coerceAtLeast(0.0) else availableToWithdraw,
            schemes = filteredSchemes,
            isInstantAvailable = isInstantAvailable,
            instantRedemptionValue = instantRedemptionValue,
            selectedWithdrawMode = if (isInstantAvailable) WithdrawMode.INSTANT else WithdrawMode.REGULAR,
            isLoading = false
        )
    }
}
