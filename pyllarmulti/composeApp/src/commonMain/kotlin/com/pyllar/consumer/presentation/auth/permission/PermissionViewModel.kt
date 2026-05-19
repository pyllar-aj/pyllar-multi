package com.pyllar.consumer.presentation.auth.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.domain.models.UpdateEmailResponse
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.platform.PermissionManager
import com.pyllar.consumer.platform.PermissionStatus
import com.pyllar.consumer.platform.PlatformActions
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
    private val permissionManager: PermissionManager,
    private val platformActions: PlatformActions
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
        com.pyllar.consumer.util.Log.d("PermissionFlow", "onGrantPermissionsTapped called - userId: $userId")
        val current = _state.value
        if (current.email.isBlank()) {
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Email is blank")
            _state.value = current.copy(showEmailError = true)
            return
        }
        if (!current.isConsentChecked) {
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Consent not checked")
            return
        }

        val status = permissionManager.checkStatus()
        com.pyllar.consumer.util.Log.d("PermissionFlow", "Current status: $status")
        if (status.locationGranted && status.gpsEnabled) {
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Permissions already granted, calling updateEmail")
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.Completed,
                permissionStatus = status
            )
            callUpdateEmailApi(userId)
            return
        }
        if (current.permissionFlow is PermissionFlowState.Completed) {
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Flow completed but required permissions still missing, opening settings")
            platformActions.openAppSettings()
        } else {
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Starting permission flow")
            startPermissionFlow(userId)
        }
    }

    private fun startPermissionFlow(userId: String) {
        viewModelScope.launch {
            // Step 1: Notifications
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Requesting notifications...")
            _state.value = _state.value.copy(permissionFlow = PermissionFlowState.RequestingNotifications)
            permissionManager.requestNotifications() // result ignored — flow continues regardless

            // Step 2: Location
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Requesting location...")
            val statusAfterNotif = permissionManager.checkStatus()
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.RequestingLocation,
                permissionStatus = statusAfterNotif
            )
            permissionManager.requestLocation() // result ignored — flow continues regardless

            // Step 3: GPS (synchronous read inside checkStatus)
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Checking GPS...")
            val statusAfterLocation = permissionManager.checkStatus()
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.CheckingGps,
                permissionStatus = statusAfterLocation
            )

            // Flow complete
            val finalStatus = permissionManager.checkStatus()
            com.pyllar.consumer.util.Log.d("PermissionFlow", "Flow complete, final status: $finalStatus")
            
            _state.value = _state.value.copy(
                permissionFlow = PermissionFlowState.Completed,
                permissionStatus = finalStatus
            )

            if (finalStatus.locationGranted && finalStatus.gpsEnabled) {
                com.pyllar.consumer.util.Log.d("PermissionFlow", "Permissions granted, calling updateEmail")
                callUpdateEmailApi(userId)
            } else {
                com.pyllar.consumer.util.Log.d("PermissionFlow", "Permissions NOT fully granted, staying on permission screen")
            }
        }
    }

    private fun callUpdateEmailApi(userId: String) {
        com.pyllar.consumer.util.Log.d("PermissionFlow", "callUpdateEmailApi called for email: ${_state.value.email}")
        _state.value = _state.value.copy(isProcessing = true, serverErrorMessage = null)
        viewModelScope.launch {
            authRepository.updateEmail(_state.value.email, userId).collect { result ->
                com.pyllar.consumer.util.Log.d("PermissionFlow", "updateEmail result: $result")
                when (result) {
                    is Resource.Loading -> {
                        _state.value = _state.value.copy(updateEmailResult = result)
                    }
                    is Resource.Success -> {
                        val response = result.data
                        val navigation = result.navigation
                        
                        if (response?.isMismatch == true || navigation?.action == NavigationAction.STAY || navigation?.action == NavigationAction.RETRY) {
                             val errorMsg = navigation?.getMessage() ?: response?.message ?: "The mobile number and email you entered do not belong to the same account. Please try again with a different email."
                             _state.value = _state.value.copy(
                                 updateEmailResult = result,
                                 isProcessing = false,
                                 serverErrorMessage = errorMsg
                             )
                        } else {
                            _state.value = _state.value.copy(
                                updateEmailResult = result,
                                isProcessing = false,
                                serverErrorMessage = null
                            )
                        }
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

    fun setServerErrorMessage(message: String?) {
        _state.value = _state.value.copy(serverErrorMessage = message)
    }
}
