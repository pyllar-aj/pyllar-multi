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
    val lastDeletionRequest: AccountDeletionResponseDto? = null,
    val referredByCode: String? = null,
    val referralEnabled: Boolean = false
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
            
            // 1. Initial load from local store for immediate UI
            try {
                val name = localStore.getCurrentFullName()
                val email = localStore.getCurrentEmail()
                val phone = localStore.getCurrentPhone()
                _profileState.value = _profileState.value.copy(
                    name = name,
                    email = email,
                    phoneNumber = phone
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading local profile: ${e.message}")
            }

            // 2. Fetch full details from server (including deletion status)
            if (userId.isNotBlank()) {
                onboardingRepository.getProfileDetails(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            val latestDeletion = data?.let {
                                if (it.deletionRequestId != null || it.deletionStatus != null || it.deletionRequestedAt != null) {
                                    AccountDeletionResponseDto(
                                        requestId = it.deletionRequestId,
                                        userId = userId,
                                        status = it.deletionStatus,
                                        requestedAt = it.deletionRequestedAt,
                                        message = it.deletionMessage
                                    )
                                } else null
                            }
                            _profileState.value = _profileState.value.copy(
                                name = data?.name ?: _profileState.value.name,
                                email = data?.email ?: _profileState.value.email,
                                phoneNumber = data?.phoneNumber ?: _profileState.value.phoneNumber,
                                dob = data?.dob ?: _profileState.value.dob,
                                gender = data?.gender ?: _profileState.value.gender,
                                hasPendingDeletionRequest = data?.deletionRequested == true,
                                deletionRequestMessage = data?.deletionMessage,
                                lastDeletionRequest = latestDeletion,
                                referredByCode = data?.referredByCode,
                                referralEnabled = data?.referralEnabled ?: false,
                                isLoading = false
                            )
                        }
                        is Resource.Error -> {
                            _profileState.value = _profileState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        is Resource.Loading -> Unit
                    }
                }
            } else {
                _profileState.value = _profileState.value.copy(isLoading = false)
            }
        }
    }

    fun requestAccountDeletion(userId: String, notes: String? = null) {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(
                isDeletionRequestInProgress = true,
                deletionRequestMessage = null,
                deletionRequestError = null
            )
            try {
                onboardingRepository.requestAccountDeletion(userId, notes).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val response = result.data
                            _profileState.value = _profileState.value.copy(
                                isDeletionRequestInProgress = false,
                                hasPendingDeletionRequest = response?.status?.equals("PENDING", ignoreCase = true) == true || response?.status?.equals("SUCCESS", ignoreCase = true) == true,
                                lastDeletionRequest = response,
                                deletionRequestMessage = response?.message ?: "Your account deletion request has been submitted."
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
