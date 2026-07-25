package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.requests.DoubtsSurveyRequestDto
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Fire-and-forget submission for the doubts-survey bottom sheet. A failed submission must
 * never block the user from leaving the screen, so errors are logged only, not surfaced.
 */
class DoubtsSurveyViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    fun submit(
        screenName: String,
        goalId: String?,
        selectedOption: String,
        freeText: String? = null,
        requestCallback: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                onboardingRepository.submitDoubtsSurvey(
                    DoubtsSurveyRequestDto(
                        screenName = screenName,
                        goalId = goalId,
                        selectedOption = selectedOption,
                        freeText = freeText,
                        requestCallback = requestCallback
                    )
                ).collect()
            } catch (e: Exception) {
                platformLog("DoubtsSurveyViewModel: Failed to submit doubts survey: ${e.message}")
            }
        }
    }
}
