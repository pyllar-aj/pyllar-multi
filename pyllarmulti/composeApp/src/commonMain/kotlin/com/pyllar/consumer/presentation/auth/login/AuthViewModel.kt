package com.pyllar.consumer.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the login / OTP-registration flow.
 *
 * Migrated from Android-only (Hilt + LiveData) to:
 *  - Koin-injected (registered in [com.pyllar.consumer.di.SharedModules])
 *  - StateFlow instead of LiveData
 *  - commonMain — no Android-specific imports
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginResult = MutableStateFlow<Resource<AuthToken>?>(null)
    val loginResult: StateFlow<Resource<AuthToken>?> = _loginResult

    fun sendOtpParams(request: OtpRegistrationRequest) {
        viewModelScope.launch {
            authRepository.sendOtp(request).collect { result ->
                _loginResult.value = result
            }
        }
    }
}
