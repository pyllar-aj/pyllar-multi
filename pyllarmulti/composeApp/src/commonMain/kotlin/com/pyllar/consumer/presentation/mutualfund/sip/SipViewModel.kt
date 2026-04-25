package com.pyllar.consumer.presentation.mutualfund.sip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.models.LumpsumPurchaseRequest
import com.pyllar.consumer.domain.models.SipCreationRequest
import com.pyllar.consumer.domain.models.SipFormData
import com.pyllar.consumer.domain.models.SipResponse
import com.pyllar.consumer.domain.repository.MutualFundRepository
import com.pyllar.consumer.data.remote.parser.ErrorType
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the SIP creation / lumpsum purchase flow.
 *
 * Migrated from Android-only (Hilt + java.text.*) to:
 *  - Koin-injected (registered in SharedModules)
 *  - StateFlow instead of LiveData
 *  - commonMain — no Android-specific imports
 */
class SipViewModel(
    private val mutualFundRepository: MutualFundRepository
) : ViewModel() {

    private val _sipResult = MutableStateFlow<Resource<SipResponse>?>(null)
    val sipResult: StateFlow<Resource<SipResponse>?> = _sipResult

    private val _lumpsumResult = MutableStateFlow<Resource<SipResponse>?>(null)
    val lumpsumResult: StateFlow<Resource<SipResponse>?> = _lumpsumResult

    private val _formData = MutableStateFlow(SipFormData())
    val formData: StateFlow<SipFormData> = _formData

    fun updateFormData(newData: SipFormData) {
        _formData.value = newData
    }

    fun createSip(userId: String) {
        val currentFormData = _formData.value

        if (!isSipFormValid(currentFormData)) {
            _sipResult.value = Resource.Error(
                "Please fill all required fields",
                errorType = ErrorType.VALIDATION_ERROR
            )
            return
        }

        viewModelScope.launch {
            _sipResult.value = Resource.Loading()
            val request = SipCreationRequest(
                investmentAccountId = currentFormData.investmentAccountId,
                isin = currentFormData.isin,
                amount = currentFormData.amount.toDoubleOrNull() ?: 0.0,
                frequency = currentFormData.frequency,
                startDate = currentFormData.startDate,
                fundSchemeName = currentFormData.fundSchemeName
            )
            mutualFundRepository.createSip(userId, request).collect {
                _sipResult.value = it
            }
        }
    }

    fun createLumpsumPurchase(
        userId: String,
        investmentAccountId: Int,
        isin: String,
        amount: Double,
        investmentDate: String,
        fundSchemeName: String = "HDFC Equity Fund"
    ) {
        if (amount <= 0) {
            _lumpsumResult.value = Resource.Error("Please enter a valid amount", errorType = ErrorType.VALIDATION_ERROR)
            return
        }

        viewModelScope.launch {
            _lumpsumResult.value = Resource.Loading()
            val request = LumpsumPurchaseRequest(
                investmentAccountId = investmentAccountId,
                isin = isin,
                amount = amount,
                investmentDate = investmentDate,
                fundSchemeName = fundSchemeName
            )
            mutualFundRepository.createLumpsumPurchase(userId, request).collect {
                _lumpsumResult.value = it
            }
        }
    }

    private fun isSipFormValid(formData: SipFormData): Boolean {
        return formData.amount.isNotBlank() &&
            formData.amount.toDoubleOrNull() != null &&
            (formData.amount.toDoubleOrNull() ?: 0.0) > 0 &&
            formData.frequency.isNotBlank() &&
            formData.startDate.isNotBlank()
    }
}
