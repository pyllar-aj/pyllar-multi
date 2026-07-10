package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.data.remote.model.dto.CreditBureauLookupResponseDto
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.filterEnglishName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditBureauFetchViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditBureauFetchUiState())
    val uiState: StateFlow<CreditBureauFetchUiState> = _uiState.asStateFlow()

    fun loadPanHolderName() {
        viewModelScope.launch {
            val name = sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.PAN_HOLDER_NAME) ?: ""
            _uiState.update { it.copy(name = name.filterEnglishName()) }
        }
    }

    fun onNameChanged(name: String) {
        val filtered = name.filterEnglishName()
        _uiState.update { it.copy(name = filtered, fetchError = false, fetchSuccess = false, errorMessage = null) }
    }

    fun fetchDetails() {
        if (_uiState.value.isFetching) return
        val name = _uiState.value.name.trim()
        if (name.isBlank()) return

        _uiState.update {
            it.copy(isFetching = true, fetchError = false, fetchSuccess = false, errorMessage = null)
        }

        viewModelScope.launch {
            val userId = sessionStore.getCurrentUserId()
            val mobile = sessionStore.getCurrentPhone().takeLast(10)

            onboardingRepository.lookupCreditBureau(userId, name, mobile).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data
                        if (data?.panNumber.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(
                                    isFetching = false,
                                    fetchSuccess = false,
                                    fetchError = true,
                                    errorMessage = "Could not fetch your PAN. Please check your name and try again"
                                )
                            }
                        } else {
                            val fullName = data.fullName
                            val dob = data.dob
                            val panNumber = data.panNumber

                            sessionStore.saveValue("prefilledName", fullName ?: name)
                            sessionStore.saveValue("prefilledDob", dob ?: "")
                            sessionStore.saveValue("prefilledPan", panNumber ?: "")

                            _uiState.update {
                                it.copy(
                                    isFetching = false,
                                    fetchSuccess = true,
                                    fetchError = false,
                                    errorMessage = null,
                                    resolvedName = fullName ?: it.name,
                                    resolvedDob = dob,
                                    resolvedPan = panNumber
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
                                errorMessage = result.message ?: "Could not fetch your PAN. Please check your name and try again"
                            )
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }
}

data class CreditBureauFetchUiState(
    val name: String = "",
    val isFetching: Boolean = false,
    val fetchSuccess: Boolean = false,
    val fetchError: Boolean = false,
    val errorMessage: String? = null,
    val resolvedName: String? = null,
    val resolvedDob: String? = null,
    val resolvedPan: String? = null
)
