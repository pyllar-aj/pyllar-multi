package com.pyllar.consumer.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.local.KeyValueConstants
import com.pyllar.consumer.data.local.LocalOnboardingStore
import com.pyllar.consumer.data.remote.requests.HelperCodeRequest
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HelperCodeState(
    val helperCode: String = "",
    val isSubmitted: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the helper/referral code feature.
 *
 * Migrated from Android-only (Hilt + old ApiClient) to:
 *  - Koin commonMain ViewModel
 *  - Uses [OnboardingRepository] for the API call
 *  - Uses [LocalOnboardingStore] for local persistence
 */
class HelperCodeViewModel(
    private val repository: OnboardingRepository,
    private val localStore: LocalOnboardingStore
) : ViewModel() {

    private val _helperCodeState = MutableStateFlow(HelperCodeState())
    val helperCodeState: StateFlow<HelperCodeState> = _helperCodeState.asStateFlow()

    init {
        loadHelperCodeFromBackend()
    }

    fun loadHelperCodeFromBackend() {
        viewModelScope.launch {
            try {
                val userId = localStore.getCurrentUserId()
                if (userId.isBlank()) {
                    Log.d("HelperCodeViewModel", "UserId is blank, skipping backend fetch")
                    loadFromLocalStorage()
                    return@launch
                }
                repository.getHelperCode(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val code = result.data?.helperCode
                            if (!code.isNullOrBlank()) {
                                localStore.saveValue(KeyValueConstants.HELPER_CODE, code)
                                localStore.saveValue(KeyValueConstants.HELPER_CODE_SUBMITTED, "true")
                                _helperCodeState.value = _helperCodeState.value.copy(
                                    helperCode = code, isSubmitted = true
                                )
                            } else {
                                localStore.saveValue(KeyValueConstants.HELPER_CODE_SUBMITTED, "false")
                                _helperCodeState.value = _helperCodeState.value.copy(helperCode = "", isSubmitted = false)
                            }
                        }
                        is Resource.Error -> {
                            Log.e("HelperCodeViewModel", "Error fetching helper code: ${result.message}")
                            loadFromLocalStorage()
                        }
                        is Resource.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                Log.e("HelperCodeViewModel", "Exception: ${e.message}")
                loadFromLocalStorage()
            }
        }
    }

    private fun loadFromLocalStorage() {
        viewModelScope.launch {
            val code = localStore.getValue(KeyValueConstants.HELPER_CODE)
            val submitted = localStore.getValue(KeyValueConstants.HELPER_CODE_SUBMITTED) == "true"
            if (!code.isNullOrBlank() && submitted) {
                _helperCodeState.value = _helperCodeState.value.copy(helperCode = code, isSubmitted = true)
            }
        }
    }

    fun submitHelperCode(userId: String, code: String) {
        if (_helperCodeState.value.isSubmitted) return

        viewModelScope.launch {
            _helperCodeState.value = _helperCodeState.value.copy(isSubmitting = true, errorMessage = null)
            try {
                repository.submitHelperCode(HelperCodeRequest(userId = userId, helperCode = code.uppercase())).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            localStore.saveValue(KeyValueConstants.HELPER_CODE, code.uppercase())
                            localStore.saveValue(KeyValueConstants.HELPER_CODE_SUBMITTED, "true")
                            _helperCodeState.value = _helperCodeState.value.copy(
                                helperCode = code.uppercase(), isSubmitted = true,
                                isSubmitting = false, errorMessage = null
                            )
                        }
                        is Resource.Error -> {
                            _helperCodeState.value = _helperCodeState.value.copy(
                                isSubmitting = false,
                                errorMessage = result.message ?: "Failed to submit helper code."
                            )
                        }
                        is Resource.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                _helperCodeState.value = _helperCodeState.value.copy(
                    isSubmitting = false, errorMessage = "Failed to submit helper code."
                )
            }
        }
    }

    fun clearError() {
        _helperCodeState.value = _helperCodeState.value.copy(errorMessage = null)
    }
}
