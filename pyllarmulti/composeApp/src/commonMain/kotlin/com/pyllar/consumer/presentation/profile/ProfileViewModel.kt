package com.pyllar.consumer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.AccountDeletionResponseDto
import com.pyllar.consumer.data.local.LocalOnboardingStore
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val dob: String = "",
    val gender: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeletionRequestInProgress: Boolean = false,
    val deletionRequestMessage: String? = null,
    val deletionRequestError: String? = null,
    val hasPendingDeletionRequest: Boolean = false,
    val lastDeletionRequest: AccountDeletionResponseDto? = null
)

/**
 * ViewModel for the user profile screen.
 *
 * Migrated from Android-only (Hilt + Context data-wiping) to:
 *  - Koin-injected commonMain ViewModel
 *  - StateFlow; no LiveData
 *  - Data wipe is delegated to platform via [LocalOnboardingStore.logout]
 *    (the full file-system wipe is only needed on Android and can be triggered
 *    in androidMain when Koin injects the right [LocalOnboardingStore] impl)
 */
class ProfileViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val localStore: LocalOnboardingStore
) : ViewModel() {

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isLoading = true, error = null)
            try {
                val name = localStore.getCurrentFullName()
                val email = localStore.getCurrentEmail()
                val phone = localStore.getCurrentPhone()
                _profileState.value = _profileState.value.copy(
                    name = name,
                    email = email,
                    phoneNumber = phone,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile: ${e.message}")
                _profileState.value = _profileState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun requestAccountDeletion(userId: String) {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(
                isDeletionRequestInProgress = true,
                deletionRequestMessage = null,
                deletionRequestError = null
            )
            try {
                onboardingRepository.requestAccountDeletion(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _profileState.value = _profileState.value.copy(
                                isDeletionRequestInProgress = false,
                                hasPendingDeletionRequest = true,
                                lastDeletionRequest = result.data,
                                deletionRequestMessage = "Your account deletion request has been submitted."
                            )
                        }
                        is Resource.Error -> {
                            _profileState.value = _profileState.value.copy(
                                isDeletionRequestInProgress = false,
                                deletionRequestError = result.message ?: "Failed to submit request."
                            )
                        }
                        is Resource.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                _profileState.value = _profileState.value.copy(
                    isDeletionRequestInProgress = false,
                    deletionRequestError = "Failed to submit request: ${e.message}"
                )
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                localStore.logout()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error during logout: ${e.message}")
            } finally {
                onComplete()
            }
        }
    }

    fun clearDeletionMessages() {
        _profileState.value = _profileState.value.copy(
            deletionRequestMessage = null,
            deletionRequestError = null
        )
    }
}
