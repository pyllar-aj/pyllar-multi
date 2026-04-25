package com.pyllar.consumer.presentation.mutualfund.upi

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpiAccountInfo(
    val vpa: String,
    val accountName: String? = null,
    val ifsc: String? = null
)

data class UpiAccountLinkingUiState(
    val isLoading: Boolean = false,
    val isUpiAvailable: Boolean? = null,
    val availableUpiApps: List<String> = emptyList(),
    val linkedAccount: UpiAccountInfo? = null,
    val error: String? = null
)

/**
 * ViewModel for the UPI account linking screen.
 *
 * Stays in androidMain — UPI is an Android-only payment Rail.
 * Migrated from Hilt @HiltViewModel to plain ViewModel for Koin.
 */
class UpiAccountLinkingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UpiAccountLinkingUiState())
    val uiState: StateFlow<UpiAccountLinkingUiState> = _uiState.asStateFlow()

    fun checkUpiAvailability(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Check for installed UPI apps
                val pm = context.packageManager
                val upiApps = listOf("com.phonepe.app", "net.one97.paytm", "com.google.android.apps.nbu.paisa.user")
                    .filter { pkg ->
                        try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
                    }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUpiAvailable = upiApps.isNotEmpty(),
                    availableUpiApps = upiApps
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUpiAvailable = false,
                    error = "Error checking UPI availability: ${e.message}"
                )
            }
        }
    }

    fun initiateAccountDiscovery(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val activity = context as? Activity
                if (activity == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Unable to initiate account discovery")
                }
                // Actual UPI SDK call would go here
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error initiating account discovery: ${e.message}"
                )
            }
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (resultCode == Activity.RESULT_OK) {
                // Parse UPI result here with the chosen SDK
            } else {
                _uiState.value = _uiState.value.copy(error = "Account discovery was cancelled")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearLinkedAccount() { _uiState.value = _uiState.value.copy(linkedAccount = null) }
}
