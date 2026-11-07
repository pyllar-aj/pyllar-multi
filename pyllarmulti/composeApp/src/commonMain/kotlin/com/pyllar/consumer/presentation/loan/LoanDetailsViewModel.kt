package com.pyllar.consumer.presentation.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.requests.LoanDetailsDto
import com.pyllar.consumer.domain.repository.LoanRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoanDetailsUiState(
    val isLoading: Boolean = true,
    val info: LoanDetailsInfo? = null,
    val hasError: Boolean = false
)

class LoanDetailsViewModel(
    private val loanRepository: LoanRepository,
    initialApplicationId: String = ""
) : ViewModel() {

    private var _applicationId: String = initialApplicationId

    private val _uiState = MutableStateFlow(LoanDetailsUiState())
    val uiState: StateFlow<LoanDetailsUiState> = _uiState.asStateFlow()

    init {
        if (_applicationId.isNotBlank()) {
            load(_applicationId)
        }
    }

    fun initApplicationId(appId: String) {
        if (_applicationId == appId && _applicationId.isNotBlank()) return
        _applicationId = appId
        load(appId)
    }

    fun load(appId: String = _applicationId) {
        if (appId.isBlank()) return
        _uiState.value = LoanDetailsUiState(isLoading = true)
        viewModelScope.launch {
            val result = loanRepository.getLoanDetailsSync(appId)
            val info = (result as? Resource.Success)?.data?.let(LoanDetailsInfo::from)
            _uiState.value = LoanDetailsUiState(isLoading = false, info = info, hasError = info == null)
        }
    }
}

