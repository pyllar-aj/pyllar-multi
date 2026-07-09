package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpiFetchUiState(
    val upi: String = "",
    val isFetching: Boolean = false,
    val fetchError: Boolean = false,
    val fetchSuccess: Boolean = false,
    val errorMessage: String? = null,
    val resolvedName: String? = null,
    val resolvedDob: String? = null,
    val resolvedPan: String? = null
)

class UpiFetchViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpiFetchUiState())
    val uiState: StateFlow<UpiFetchUiState> = _uiState.asStateFlow()

    private val upiPattern = Regex("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$")

    fun onUpiInputChanged(input: String) {
        _uiState.update {
            it.copy(
                upi = input,
                fetchError = false,
                fetchSuccess = false,
                errorMessage = null
            )
        }
    }

    fun selectExample(example: String) {
        _uiState.update {
            it.copy(
                upi = example,
                fetchError = false,
                fetchSuccess = false,
                errorMessage = null
            )
        }
    }

    fun fetchDetails() {
        val currentUpi = _uiState.value.upi.trim()
        if (currentUpi.isBlank() || _uiState.value.isFetching) return

        if (!upiPattern.matches(currentUpi)) {
            _uiState.update {
                it.copy(
                    isFetching = false,
                    fetchSuccess = false,
                    fetchError = true,
                    errorMessage = "Enter a valid UPI ID, e.g. yourname@okicici"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isFetching = true,
                fetchError = false,
                fetchSuccess = false,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val userId = sessionStore.getCurrentUserId()

            onboardingRepository.lookupUpiVpa(userId, currentUpi).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data
                        if (data?.nameAsPerBank.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(
                                    isFetching = false,
                                    fetchSuccess = false,
                                    fetchError = true,
                                    errorMessage = "Could not verify the UPI ID. Please check and try again"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isFetching = false,
                                    fetchSuccess = true,
                                    fetchError = false,
                                    errorMessage = null,
                                    resolvedName = data.nameAsPerBank,
                                    resolvedDob = data.dob,
                                    resolvedPan = data.panNumber
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        // The lookup runs synchronously server-side; if this call timed out or failed,
                        // recover the result via the status endpoint instead.
                        onboardingRepository.pollUpiVpaLookupStatus(userId, currentUpi).collect { pollResult ->
                            when (pollResult) {
                                is Resource.Success -> {
                                    val pollData = pollResult.data
                                    if (pollData?.nameAsPerBank.isNullOrBlank()) {
                                        _uiState.update {
                                            it.copy(
                                                isFetching = false,
                                                fetchSuccess = false,
                                                fetchError = true,
                                                errorMessage = "Could not verify the UPI ID. Please check and try again"
                                            )
                                        }
                                    } else {
                                        _uiState.update {
                                            it.copy(
                                                isFetching = false,
                                                fetchSuccess = true,
                                                fetchError = false,
                                                errorMessage = null,
                                                resolvedName = pollData.nameAsPerBank,
                                                resolvedDob = pollData.dob,
                                                resolvedPan = pollData.panNumber
                                            )
                                        }
                                    }
                                }
                                is Resource.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            isFetching = false,
                                            fetchSuccess = false,
                                            fetchError = true,
                                            errorMessage = pollResult.message ?: "Could not verify the UPI ID. Please check and try again"
                                        )
                                    }
                                }
                                is Resource.Loading -> {}
                            }
                        }
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
}
