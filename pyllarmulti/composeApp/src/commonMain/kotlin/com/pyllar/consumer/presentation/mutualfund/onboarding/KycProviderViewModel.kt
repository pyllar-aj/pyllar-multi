package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch

/**
 * Backend calls behind the "Start KYC" provider sheet. The backend decides which providers are offered and builds the
 * CAMS URL (it has the PAN), so the app never assembles that URL itself.
 */
class KycProviderViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    /** The CAMS KYC URL to open in a browser, or null when CAMS is unavailable (disabled, no PAN, or a failed call). */
    suspend fun fetchCamsKycUrl(): String? {
        return try {
            when (val result = onboardingRepository.getKycOptions()) {
                is Resource.Success -> result.data?.options
                    ?.firstOrNull { it.provider == PROVIDER_CAMS && it.enabled && !it.url.isNullOrBlank() }
                    ?.url
                is Resource.Error -> {
                    Log.e(TAG, "Failed to load KYC options: ${result.message}")
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load KYC options", e)
            null
        }
    }

    /** Tells the backend the user opened CAMS KYC. Best effort: a failure must never block opening the browser. */
    fun recordCamsStarted() {
        viewModelScope.launch {
            try {
                onboardingRepository.recordCamsKycStarted()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record CAMS KYC start", e)
            }
        }
    }

    companion object {
        private const val TAG = "KycProviderViewModel"
        const val PROVIDER_CAMS = "CAMS_AB"
    }
}

