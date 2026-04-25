package com.pyllar.consumer.presentation.mutualfund.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.UpiRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Minimal account info needed for UPI linking — KMP-safe (no android.* types).
 */
data class UpiAccountInfo(
    val vpa: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val ifscCode: String = "",
    val accountHolderName: String = ""
)

data class UpiAccountLinkingUiState(
    val isLoading: Boolean = false,
    val linkedAccount: UpiAccountInfo? = null,
    val vpaInput: String = "",
    val vpaError: String? = null,
    val error: String? = null,
    val isVerified: Boolean = false
)

/**
 * KMP-compatible UPI Account Linking ViewModel.
 */
class UpiAccountLinkingViewModel(
    private val upiRepository: UpiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpiAccountLinkingUiState())
    val uiState: StateFlow<UpiAccountLinkingUiState> = _uiState.asStateFlow()

    fun onVpaInputChanged(vpa: String) {
        _uiState.value = _uiState.value.copy(vpaInput = vpa.trim(), vpaError = null)
    }

    fun verifyVpa() {
        val vpa = _uiState.value.vpaInput
        if (vpa.isBlank()) {
            _uiState.value = _uiState.value.copy(vpaError = "Please enter your UPI VPA (e.g. name@upi)")
            return
        }
        if (!vpa.contains("@")) {
            _uiState.value = _uiState.value.copy(vpaError = "Enter a valid VPA (e.g. name@upi)")
            return
        }

        viewModelScope.launch {
            upiRepository.verifyVpa(vpa).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null, vpaError = null)
                    }
                    is Resource.Success -> {
                        val data = result.data
                        if (data != null && data.isVerified) {
                            val accountInfo = UpiAccountInfo(
                                vpa = data.vpa,
                                accountNumber = data.accountNumber ?: "N/A",
                                bankName = data.bankName ?: "N/A",
                                ifscCode = data.ifscCode ?: "N/A",
                                accountHolderName = data.accountHolderName ?: "N/A"
                            )
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                linkedAccount = accountInfo,
                                isVerified = true
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "VPA could not be verified. Please check and try again."
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message ?: "Verification failed"
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, vpaError = null)
    }

    fun reset() {
        _uiState.value = UpiAccountLinkingUiState()
    }
}
