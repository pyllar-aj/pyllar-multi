package com.pyllar.consumer.presentation.auth.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.domain.models.UpdateEmailResponse
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.platform.PermissionStatus
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PermissionFlowState {
    object Idle : PermissionFlowState()
    object RequestingNotifications : PermissionFlowState()
    object RequestingLocation : PermissionFlowState()
    object CheckingGps : PermissionFlowState()
    object Completed : PermissionFlowState()
}

data class PermissionScreenState(
    val email: String = "",
    val isConsentChecked: Boolean = false,
    val showEmailError: Boolean = false,
    val permissionFlow: PermissionFlowState = PermissionFlowState.Idle,
    val permissionStatus: PermissionStatus = PermissionStatus(
        notificationsGranted = false,
        locationGranted = false,
        gpsEnabled = false
    ),
    val isProcessing: Boolean = false,
    val serverErrorMessage: String? = null,
    val updateEmailResult: Resource<UpdateEmailResponse>? = null
)

class PermissionViewModel(
    private val authRepository: AuthRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionScreenState())
    val state: StateFlow<PermissionScreenState> = _state.asStateFlow()

    /** Call once on screen entry to sync current OS permission state. */
    fun refreshPermissionStatus() {
        _state.value = _state.value.copy(
            permissionStatus = permissionManager.checkStatus()
        )
    }

    fun updateEmail(newEmail: String) {
        _state.value = _state.value.copy(email = newEmail, showEmailError = false)
    }

    fun toggleConsent(checked: Boolean) {
        _state.value = _state.value.copy(isConsentChecked = checked)
    }

    /** Main entry point — called when user taps the CTA button. */
    fun onGrantPermissionsTapped(userId: String) {
        val current = _state.value
        if (current.email.isBlank()) {
            _state.value = current.copy(showEmailError = true)
            return
        }
        if (!current.isConsentChecked) return

        val status = permissionManager.checkStatus()
        if (status.notificationsGranted && status.locationGranted && status.gpsEnabled) {
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.Completed,
                permissionStatus = status
            )
            callUpdateEmailApi(userId)
            return
        }
        startPermissionFlow(userId)
    }

    private fun startPermissionFlow(userId: String) {
        viewModelScope.launch {
            // Step 1: Notifications
            _state.value = _state.value.copy(permissionFlow = PermissionFlowState.RequestingNotifications)
            permissionManager.requestNotifications() // result ignored — flow continues regardless

            // Step 2: Location
            val statusAfterNotif = permissionManager.checkStatus()
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.RequestingLocation,
                permissionStatus = statusAfterNotif
            )
            permissionManager.requestLocation() // result ignored — flow continues regardless

            // Step 3: GPS (synchronous read inside checkStatus)
            val statusAfterLocation = permissionManager.checkStatus()
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.CheckingGps,
                permissionStatus = statusAfterLocation
            )

            // Flow complete
            val finalStatus = permissionManager.checkStatus()
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.Completed,
                permissionStatus = finalStatus
            )
            callUpdateEmailApi(userId)
        }
    }

    private fun callUpdateEmailApi(userId: String) {
        _state.value = _state.value.copy(isProcessing = true, serverErrorMessage = null)
        viewModelScope.launch {
            authRepository.updateEmail(_state.value.email, userId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(updateEmailResult = result)
                    }
                    is Resource.Success -> {
                        _state.value = _state.value.copy(
                            updateEmailResult = result,
                            isProcessing = false
                        )
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(
                            updateEmailResult = result,
                            isProcessing = false,
                            serverErrorMessage = result.message,
                            permissionFlow = PermissionFlowState.Idle
                        )
                    }
                }
            }
        }
    }

    /** Call when the app returns from background (e.g. user changed settings). */
    fun onResumed() {
        refreshPermissionStatus()
    }

    fun clearResult() {
        _state.value = _state.value.copy(updateEmailResult = null)
    }

    /** Allow retrying after a STAY/RETRY response from the server. */
    fun resetForRetry() {
        _state.value = _state.value.copy(
            isProcessing = false,
            serverErrorMessage = null,
            updateEmailResult = null,
            permissionFlow = PermissionFlowState.Idle
        )
    }
}
