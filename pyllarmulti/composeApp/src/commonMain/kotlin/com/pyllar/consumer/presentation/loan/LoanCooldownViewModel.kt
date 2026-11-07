package com.pyllar.consumer.presentation.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.requests.LoanEntryDto
import com.pyllar.consumer.domain.repository.LoanRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoanCooldownUiState(
    val isLoading: Boolean = true,
    val entry: LoanEntryDto? = null,
    val hasError: Boolean = false
)

/** Reads the same entry endpoint the loans card uses, so the screen always shows the server's current cooldown. */
class LoanCooldownViewModel(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanCooldownUiState())
    val uiState: StateFlow<LoanCooldownUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = LoanCooldownUiState(isLoading = true)
        viewModelScope.launch {
            val result = loanRepository.getLoanEntrySync()
            val entry = (result as? Resource.Success)?.data
            _uiState.value = LoanCooldownUiState(isLoading = false, entry = entry, hasError = entry == null)
        }
    }
}

