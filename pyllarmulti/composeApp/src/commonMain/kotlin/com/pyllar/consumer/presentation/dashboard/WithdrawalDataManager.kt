package com.pyllar.consumer.presentation.dashboard

data class WithdrawalData(
    val amount: Double,
    val schemeName: String,
    val bankName: String,
    val bankAccountLast4: String,
    val bankAccountNumber: String,
    val bankAccountIfscCode: String,
    val transactionId: String,
    val userId: String,
    val schemeId: String,
    val isin: String,
    val folio: String?
)

object WithdrawalDataManager {
    private var withdrawalData: WithdrawalData? = null

    fun setWithdrawalData(data: WithdrawalData) {
        this.withdrawalData = data
    }

    fun getWithdrawalData(): WithdrawalData? {
        return withdrawalData
    }

    fun clearWithdrawalData() {
        withdrawalData = null
    }
}
