package com.pyllar.consumer.presentation.mutualfund.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpiMandateSetupUiState(
    val isLoading: Boolean = false,
    val linkedAccount: UpiAccountInfo? = null,
    val sipAmount: String = "",
    val fundName: String = "",
    val startDate: String? = null,
    val mandateId: String? = null,
    val error: String? = null
)

/**
 * KMP-compatible UPI Mandate Setup ViewModel.
 *
 * The mandate is set up via server API (not via Android UPI SDK),
 * making this work identically on iOS and Android.
 */
class UpiMandateSetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UpiMandateSetupUiState())
    val uiState: StateFlow<UpiMandateSetupUiState> = _uiState.asStateFlow()

    fun initializeMandate(linkedAccount: UpiAccountInfo, sipAmount: String, fundName: String) {
        // Approximate "next month" label — KMP-safe (no java.util.Calendar)
        val startDate = "Next Month"
        _uiState.value = _uiState.value.copy(
            linkedAccount = linkedAccount,
            sipAmount = sipAmount,
            fundName = fundName,
            startDate = startDate
        )
    }

    fun setupMandate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val linkedAccount = _uiState.value.linkedAccount
                if (linkedAccount == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No linked account found")
                    return@launch
                }

                // TODO: Replace with actual repository call to create UPI mandate via server
                // e.g. mandateRepository.createMandate(linkedAccount.vpa, sipAmount, fundName)
                delay(2000) // simulate network

                val mandateId = "MANDATE_${(100000..999999).random()}_${(1000..9999).random()}"
                _uiState.value = _uiState.value.copy(isLoading = false, mandateId = mandateId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error setting up mandate: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetMandate() {
        _uiState.value = _uiState.value.copy(mandateId = null, error = null)
    }
}
