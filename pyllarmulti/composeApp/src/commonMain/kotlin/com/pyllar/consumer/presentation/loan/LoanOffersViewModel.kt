package com.pyllar.consumer.presentation.loan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.requests.ConsentTextDto
import com.pyllar.consumer.data.remote.requests.LoanApplicationViewDto
import com.pyllar.consumer.data.remote.requests.LoanContinueDto
import com.pyllar.consumer.data.remote.requests.LoanOfferDto
import com.pyllar.consumer.domain.repository.LoanRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoanOffersUiState(
    val applicationId: String = "",
    val status: String = "STARTING",
    val offers: List<LoanOfferDto> = emptyList(),
    val pendingLenders: Int = 0,
    val accountAggregatorUrl: String? = null,
    val selectedOfferId: String? = null,
    val isFirstLoad: Boolean = true,
    val isContinuing: Boolean = false,
    /** Set when polling keeps failing; the last good data stays on screen. */
    val connectionProblem: Boolean = false,
    val errorMessage: String? = null,
    /** The lender disclaimer, fetched from the same versioned wording as the consent texts. */
    val disclaimer: String? = null
)

sealed interface LoanOffersEvent {
    /** The lender's hosted journey, to be shown in the in-app WebView. */
    data class OpenHosted(val url: String) : LoanOffersEvent
    data object AssistedCallback : LoanOffersEvent
}

class LoanOffersViewModel(
    private val loanRepository: LoanRepository,
    initialApplicationId: String = ""
) : ViewModel() {

    private var _applicationId: String = initialApplicationId

    private val _uiState = MutableStateFlow(LoanOffersUiState(applicationId = initialApplicationId))
    val uiState: StateFlow<LoanOffersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoanOffersEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoanOffersEvent> = _events.asSharedFlow()

    private var pollJob: Job? = null

    init {
        if (_applicationId.isNotBlank()) {
            loadDisclaimer()
        }
    }

    fun initApplicationId(appId: String) {
        if (_applicationId == appId && _applicationId.isNotBlank()) return
        _applicationId = appId
        _uiState.value = _uiState.value.copy(applicationId = appId)
        loadDisclaimer()
        startPolling()
    }

    private fun loadDisclaimer() {
        viewModelScope.launch {
            val result = loanRepository.getConsentTextsSync("LOAN_DISCLAIMER")
            val text = (result as? Resource.Success)?.data?.firstOrNull()?.body
            _uiState.value = _uiState.value.copy(disclaimer = text)
        }
    }

    /** Safe to call repeatedly (e.g. on every screen start): only one loop runs at a time. */
    fun startPolling() {
        if (_applicationId.isBlank()) return
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            var failures = 0
            while (true) {
                val result = loanRepository.getLoanApplicationSync(_applicationId)
                val view = (result as? Resource.Success)?.data
                if (view != null) {
                    failures = 0
                    apply(view)
                    val wait = nextDelay(view)
                    if (wait == null) return@launch
                    delay(wait)
                } else {
                    failures++
                    _uiState.value = _uiState.value.copy(isFirstLoad = false, connectionProblem = failures >= 2)
                    delay((POLL_DEFAULT_MS * (1L shl failures.coerceAtMost(3))).coerceAtMost(POLL_MAX_MS))
                }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun select(offerId: String) {
        _uiState.value = _uiState.value.copy(selectedOfferId = offerId, errorMessage = null)
    }

    fun continueWithSelected() {
        val state = _uiState.value
        val offerId = state.selectedOfferId ?: return
        if (state.isContinuing) return
        _uiState.value = state.copy(isContinuing = true, errorMessage = null)
        viewModelScope.launch {
            val result = loanRepository.continueLoanOfferSync(_applicationId, offerId)
            val data = (result as? Resource.Success)?.data
            _uiState.value = _uiState.value.copy(isContinuing = false)
            when {
                data == null -> _uiState.value = _uiState.value.copy(
                    errorMessage = "Could not continue with this offer. Please try again."
                )
                // Only https links go into the WebView; anything else from the provider is treated as a failure.
                data.mode == "HOSTED" && data.url?.startsWith("https://", ignoreCase = true) == true ->
                    _events.tryEmit(LoanOffersEvent.OpenHosted(data.url))
                data.mode == "ASSISTED" -> _events.tryEmit(LoanOffersEvent.AssistedCallback)
                else -> _uiState.value = _uiState.value.copy(errorMessage = "Could not continue with this offer. Please try again.")
            }
        }
    }

    private fun apply(view: LoanApplicationViewDto) {
        val current = _uiState.value
        // Keep the user's pick only while that offer is still in the list.
        val selected = current.selectedOfferId?.takeIf { id -> view.offers.any { it.offerId == id } }
        _uiState.value = current.copy(
            status = view.status,
            offers = view.offers,
            pendingLenders = view.pendingLenders,
            accountAggregatorUrl = view.accountAggregatorUrl,
            selectedOfferId = selected,
            isFirstLoad = false,
            connectionProblem = false
        )
    }

    /** Null means stop polling. */
    private fun nextDelay(view: LoanApplicationViewDto): Long? {
        val server = view.pollAfterMillis.takeIf { it > 0 }?.coerceIn(POLL_MIN_MS, POLL_MAX_MS)
        return when (view.status) {
            "OFFERS_PENDING", "STARTING", "AA_REQUIRED" -> server ?: POLL_DEFAULT_MS
            "OFFERS_AVAILABLE" -> if (view.pendingLenders > 0) server ?: POLL_DEFAULT_MS else null
            else -> null // NO_OFFERS, NOT_PROFILED, REJECTED, EXPIRED, FAILED, CLOSED, DISBURSED, HANDED_OFF
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    private companion object {
        const val POLL_DEFAULT_MS = 5_000L
        const val POLL_MIN_MS = 2_000L
        const val POLL_MAX_MS = 30_000L
    }
}

