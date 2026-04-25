package com.pyllar.consumer.presentation.mutualfund.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.models.PortfolioResponse
import com.pyllar.consumer.domain.repository.MutualFundRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the portfolio screen.
 *
 * Migrated from Android-only (Hilt + LiveData) to:
 *  - Koin-injected (registered in SharedModules)
 *  - StateFlow instead of LiveData
 *  - commonMain — no Android-specific imports
 */
class PortfolioViewModel(
    private val mutualFundRepository: MutualFundRepository
) : ViewModel() {

    private val _portfolioResult = MutableStateFlow<Resource<PortfolioResponse>?>(null)
    val portfolioResult: StateFlow<Resource<PortfolioResponse>?> = _portfolioResult

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun loadPortfolio(userId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            mutualFundRepository.getPortfolio(userId).collect { resource ->
                _portfolioResult.value = resource
                _isRefreshing.value = false
            }
        }
    }

    fun refreshPortfolio(userId: String) {
        loadPortfolio(userId)
    }
}
