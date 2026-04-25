package com.pyllar.consumer.presentation.mutualfund.upi

import android.app.Activity
import android.content.Context
import android.content.Intent
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
 * ViewModel for the UPI mandate setup screen.
 *
 * Stays in androidMain — UPI is Android-only.
 * Migrated from Hilt @HiltViewModel to plain ViewModel for Koin.
 * Replaced java.text.SimpleDateFormat / java.util.Calendar with
 * a simpler epoch-based approach (no JVM date APIs in KMP).
 */
class UpiMandateSetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UpiMandateSetupUiState())
    val uiState: StateFlow<UpiMandateSetupUiState> = _uiState.asStateFlow()

    /** Link account info and SIP parameters before showing the setup UI. */
    fun initializeMandate(linkedAccount: UpiAccountInfo, sipAmount: String, fundName: String) {
        viewModelScope.launch {
            // Approximate "next month" label without java.util.Calendar
            val nowMs = System.currentTimeMillis()
            val nextMonthMs = nowMs + 30L * 24 * 60 * 60 * 1000
            val startDate = "Next month" // Replace with kotlinx-datetime when added to project
            _uiState.value = _uiState.value.copy(
                linkedAccount = linkedAccount,
                sipAmount = sipAmount,
                fundName = fundName,
                startDate = startDate
            )
        }
    }

    fun setupMandate(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val linkedAccount = _uiState.value.linkedAccount
                if (linkedAccount != null) {
                    val mandateId = generateMandateId()
                    val activity = context as? Activity
                    if (activity != null) {
                        // Actual UPI SDK call (e.g. NPCI UPI SDK) would go here
                        simulateMandateCreation(mandateId)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Unable to setup mandate")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "No linked account found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Error setting up mandate: ${e.message}")
            }
        }
    }

    private fun simulateMandateCreation(mandateId: String) {
        viewModelScope.launch {
            delay(2000)
            _uiState.value = _uiState.value.copy(isLoading = false, mandateId = mandateId)
        }
    }

    private fun generateMandateId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "MANDATE_${timestamp}_$random"
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (resultCode == Activity.RESULT_OK) {
                val mandateId = generateMandateId()
                _uiState.value = _uiState.value.copy(mandateId = mandateId, error = null)
            } else {
                _uiState.value = _uiState.value.copy(error = "Mandate setup was cancelled")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun resetMandate() { _uiState.value = _uiState.value.copy(mandateId = null, error = null) }
}
